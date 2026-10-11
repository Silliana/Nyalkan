package net.nyalkan.tick;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.nyalkan.Initializer;
import net.nyalkan.interfaces.ExtendedEntity;
import net.nyalkan.interfaces.ExtendedLivingEntity;
import net.nyalkan.mixin.tick.ParticleAccessor;
import net.nyalkan.mixin.tick.ParticleEngineInvoker;
import net.nyalkan.render.DistanceScale;
import net.nyalkan.render.chunk.WorldRenderer;

import java.util.ArrayList;
import java.util.function.Consumer;

public final class TickSpreader {
    private static final double NEAR_ENTITY = 8.0 * 8.0;
    private static final double SOON_ENTITY = 32.0;
    private static final double NEAR_BLOCK = 8.0 * 8.0;
    private static final double NEAR_PARTICLE = 8.0 * 8.0;
    private static final double SOON_PARTICLE = 24.0;
    private static final double MIN_FAR = 48.0;
    private static final int HIDDEN_TICKS = 2;

    private static final TwoTierQueue<Entity> entities = new TwoTierQueue<>();
    private static final TwoTierQueue<TickingBlockEntity> blocks = new TwoTierQueue<>();
    private static final TwoTierQueue<Particle> particles = new TwoTierQueue<>();
    private static final ArrayList<Runnable> tasks = new ArrayList<>();
    private static Consumer<Entity> ticker;
    private static ClientLevel level;
    private static ParticleEngine particleEngine;

    private static int tickIndex;
    private static double farSqr = MIN_FAR * MIN_FAR;
    private static double soonEntitySqr, soonParticleSqr;
    private static int framesSinceTick;
    private static double framesPerTick = 4.0;
    private static boolean tickFrame, active, running;

    private TickSpreader() {}

    public static Consumer<Entity> beginTick(ClientLevel clientLevel, Consumer<Entity> original) {
        finish();
        framesPerTick = framesPerTick * 0.8 + Math.max(1, framesSinceTick) * 0.2;
        framesSinceTick = 0;
        tickFrame = true;
        tickIndex++;

        Minecraft minecraft = Minecraft.getInstance();
        double far = Math.max(MIN_FAR, minecraft.options.getEffectiveRenderDistance() * 16 / 4.0);
        farSqr = far * far;
        soonEntitySqr = DistanceScale.sqr(SOON_ENTITY);
        soonParticleSqr = DistanceScale.sqr(SOON_PARTICLE);
        Player player = minecraft.player;
        active = Initializer.CONFIG.tickSpreading && player != null && framesPerTick >= 1.5;
        if (!active)
            return original;

        ticker = original;
        level = clientLevel;
        Entity camera = minecraft.getCameraEntity();
        return entity -> {
            if (entity instanceof Player || entity == camera || entity.isPassenger() || entity.hasIndirectPassenger(player)) {
                original.accept(entity);
                return;
            }
            double distance = entity.distanceToSqr(player);
            if (distance < NEAR_ENTITY) {
                original.accept(entity);
                return;
            }
            hold(entity);
            entities.add(entity, distance < soonEntitySqr);
        };
    }

    // keep interpolation until ticked
    private static void hold(Entity entity) {
        holdOne(entity);
        if (entity.isVehicle())
            for (Entity passenger : entity.getIndirectPassengers())
                holdOne(passenger);
    }

    private static void holdOne(Entity entity) {
        entity.setOldPosAndRot();
        if (entity instanceof LivingEntity living) {
            living.yBodyRotO = living.yBodyRot;
            living.yHeadRotO = living.yHeadRot;
        }
    }

    // client chunk tickers only
    public static boolean deferBlock(TickingBlockEntity blockTicker) {
        Player player = Minecraft.getInstance().player;
        if (!active || running || player == null)
            return false;
        BlockPos pos = blockTicker.getPos();
        double distance = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (distance < NEAR_BLOCK)
            return false;
        int phase = tickIndex + (int) (pos.asLong() ^ pos.asLong() >>> 32);
        boolean visible = WorldRenderer.getInstance().isSectionVisible(pos.getX(), pos.getY(), pos.getZ());
        int every = !visible ? 3 : distance > farSqr ? 1 : 0;
        if ((phase & every) == 0)
            blocks.add(blockTicker, false);
        return true;
    }

    public static boolean deferParticle(ParticleEngine engine, Particle particle) {
        Player player = Minecraft.getInstance().player;
        if (!active || running || player == null)
            return false;
        Vec3 pos = particle.getPos();
        double distance = player.distanceToSqr(pos.x, pos.y, pos.z);
        if (distance < NEAR_PARTICLE)
            return false;
        if (!WorldRenderer.getInstance().isSectionVisible(Mth.floor(pos.x), Mth.floor(pos.y), Mth.floor(pos.z))) {
            age(particle);
            return true;
        }
        particleEngine = engine;
        holdParticle(particle);
        particles.add(particle, distance < soonParticleSqr);
        return true;
    }

    private static void holdParticle(Particle particle) {
        ParticleAccessor accessor = (ParticleAccessor) particle;
        accessor.nyalkan$setXo(accessor.nyalkan$x());
        accessor.nyalkan$setYo(accessor.nyalkan$y());
        accessor.nyalkan$setZo(accessor.nyalkan$z());
    }

    // hidden particles only age
    private static void age(Particle particle) {
        holdParticle(particle);
        ParticleAccessor accessor = (ParticleAccessor) particle;
        int age = accessor.nyalkan$age();
        accessor.nyalkan$setAge(age + 1);
        if (age >= accessor.nyalkan$lifetime())
            particle.remove();
    }

    private static boolean fullTick(Entity entity, Player player) {
        if (!(entity instanceof Mob) || entity.isVehicle())
            return true;
        int phase = tickIndex + entity.getId();
        if (tickIndex - ((ExtendedEntity) entity).nyalkan$drawnTick() >= HIDDEN_TICKS)
            return (phase & 3) == 0;
        return entity.distanceToSqr(player) <= farSqr || (phase & 1) == 0;
    }

    private static void lightTick(Entity entity) {
        if (entity.isRemoved() || entity.isPassenger() || level.tickRateManager().isEntityFrozen(entity))
            return;
        entity.setOldPosAndRot();
        entity.tickCount++;
        ((ExtendedLivingEntity) entity).nyalkan$lightTick();
    }

    public static int tickIndex() {
        return tickIndex;
    }

    public static void deferOrRun(Runnable task) {
        if (!deferTask(task))
            task.run();
    }

    private static boolean deferTask(Runnable task) {
        if (!Initializer.CONFIG.tickSpreading || framesPerTick < 1.5 || Minecraft.getInstance().level == null)
            return false;
        tasks.add(task);
        return true;
    }

    public static void runTasks() {
        if (!tasks.isEmpty())
            FrameBudget.markHeavy();
        for (int i = 0; i < tasks.size(); i++)
            tasks.get(i).run();
        tasks.clear();
    }

    public static void frame() {
        framesSinceTick++;
        if (tickFrame) {
            tickFrame = false;
            return;
        }
        runTasks();
        if (remaining() <= 0)
            return;
        // done before next tick
        int framesLeft = Math.max(1, (int) (framesPerTick * 0.75) - framesSinceTick + 1);
        run(share(entities.remaining(), framesLeft), share(blocks.remaining(), framesLeft),
                share(particles.remaining(), framesLeft));
    }

    private static int share(int remaining, int framesLeft) {
        return (remaining + framesLeft - 1) / framesLeft;
    }

    // leaving the level
    public static void clear() {
        runTasks();
        resetQueues();
    }

    public static void finish() {
        run(entities.remaining(), blocks.remaining(), particles.remaining());
        resetQueues();
    }

    private static void resetQueues() {
        entities.clear();
        blocks.clear();
        particles.clear();
        ticker = null;
        level = null;
        particleEngine = null;
    }

    private static int remaining() {
        return entities.remaining() + blocks.remaining() + particles.remaining();
    }

    private static void run(int entityCount, int blockCount, int particleCount) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean sameLevel = level != null && minecraft.level == level;
        Player player = minecraft.player;
        running = true;
        try {
            for (; entityCount > 0 && entities.remaining() > 0; entityCount--) {
                Entity entity = entities.poll();
                if (!sameLevel || ticker == null || entity.level() != level)
                    continue;
                if (player == null || fullTick(entity, player))
                    ticker.accept(entity);
                else
                    lightTick(entity);
            }
            for (; blockCount > 0 && blocks.remaining() > 0; blockCount--) {
                TickingBlockEntity blockTicker = blocks.poll();
                if (sameLevel && !blockTicker.isRemoved())
                    blockTicker.tick();
            }
            for (; particleCount > 0 && particles.remaining() > 0; particleCount--) {
                Particle particle = particles.poll();
                if (sameLevel && particleEngine != null && particle.isAlive())
                    ((ParticleEngineInvoker) particleEngine).nyalkan$tickParticle(particle);
            }
        } finally {
            running = false;
        }
    }
}
