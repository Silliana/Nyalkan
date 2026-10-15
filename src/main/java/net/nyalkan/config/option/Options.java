package net.nyalkan.config.option;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.*;
import net.minecraft.network.chat.Component;
import net.nyalkan.Initializer;
import net.nyalkan.config.Config;
import net.nyalkan.config.gui.OptionBlock;
import net.nyalkan.config.video.VideoModeManager;
import net.nyalkan.config.video.VideoModeSet;
import net.nyalkan.render.chunk.build.light.LightMode;
import net.nyalkan.vulkan.Renderer;
import net.nyalkan.vulkan.device.DeviceManager;

import java.util.stream.IntStream;

public abstract class Options {
    public static boolean fullscreenDirty = false;
    private static final int WINDOWED = 0;
    private static final int BORDERLESS = 1;
    private static final int FULLSCREEN = 2;
    static Config config = Initializer.CONFIG;
    static Minecraft minecraft = Minecraft.getInstance();
    static Window window = minecraft.getWindow();
    static net.minecraft.client.Options minecraftOptions = minecraft.options;

    public static OptionBlock[] getVideoOpts() {
        var videoMode = config.videoMode;
        var videoModeSet = VideoModeManager.getFromVideoMode(videoMode);

        // saved mode may be stale
        if (videoModeSet == null) {
            videoModeSet = VideoModeManager.getFirstAvailable();
            videoMode = videoModeSet.getVideoMode();
        }

        VideoModeManager.selectedVideoMode = videoMode;
        var refreshRates = videoModeSet.getRefreshRates();

        CyclingOption<Integer> RefreshRate = (CyclingOption<Integer>) new CyclingOption<>(
                Component.translatable("nyalkan.options.refreshRate"),
                refreshRates.toArray(new Integer[0]),
                (value) -> {
                    VideoModeManager.selectedVideoMode.refreshRate = value;
                    VideoModeManager.applySelectedVideoMode();

                    if (minecraftOptions.fullscreen().get())
                        fullscreenDirty = true;
                },
                () -> VideoModeManager.selectedVideoMode.refreshRate)
                .setTranslator(refreshRate -> Component.nullToEmpty(refreshRate.toString()));

        Option<VideoModeSet> resolutionOption = new CyclingOption<>(
                Component.translatable("options.fullscreen.resolution"),
                VideoModeManager.getVideoResolutions(),
                (value) -> {
                    VideoModeManager.selectedVideoMode = value.getVideoMode(RefreshRate.getNewValue());
                    VideoModeManager.applySelectedVideoMode();

                    if (minecraftOptions.fullscreen().get())
                        fullscreenDirty = true;
                },
                () -> {
                    var selectedVideoMode = VideoModeManager.selectedVideoMode;
                    var selectedVideoModeSet = VideoModeManager.getFromVideoMode(selectedVideoMode);

                    return selectedVideoModeSet != null ? selectedVideoModeSet : VideoModeSet.getDummy();
                })
                .setTranslator(resolution -> Component.nullToEmpty(resolution.toString()));

        resolutionOption.setOnChange(() -> {
            var newVideoMode = resolutionOption.getNewValue();
            var newRefreshRates = newVideoMode.getRefreshRates().toArray(new Integer[0]);

            RefreshRate.setValues(newRefreshRates);
            RefreshRate.setNewValue(newRefreshRates[newRefreshRates.length - 1]);
        });

        return new OptionBlock[]{
                new OptionBlock("", new Option<?>[]{
                        resolutionOption,
                        RefreshRate,
                        new CyclingOption<>(Component.translatable("nyalkan.options.windowMode"),
                                new Integer[]{WINDOWED, BORDERLESS, FULLSCREEN},
                                value -> {
                                    // f11 returns to windowed
                                    config.windowedFullscreen = value == BORDERLESS;
                                    minecraftOptions.fullscreen().set(value != WINDOWED);
                                    fullscreenDirty = true;
                                },
                                () -> !minecraftOptions.fullscreen().get() ? WINDOWED
                                        : config.windowedFullscreen ? BORDERLESS : FULLSCREEN)
                                .setTranslator(value -> Component.translatable(switch (value) {
                                    case BORDERLESS -> "nyalkan.options.windowMode.borderless";
                                    case FULLSCREEN -> "nyalkan.options.windowMode.fullscreen";
                                    default -> "nyalkan.options.windowMode.windowed";
                                })),
                        new RangeOption(Component.translatable("options.framerateLimit"),
                                10, 260, 10,
                                value -> Component.nullToEmpty(value == 260 ?
                                        Component.translatable("options.framerateLimit.max").getString() :
                                        String.valueOf(value)),
                                value -> {
                                    minecraftOptions.framerateLimit().set(value);
                                    window.setFramerateLimit(value);
                                },
                                () -> minecraftOptions.framerateLimit().get()),
                        new SwitchOption(Component.translatable("options.vsync"),
                                value -> {
                                    minecraftOptions.enableVsync().set(value);
                                    window.updateVsync(value);
                                },
                                () -> minecraftOptions.enableVsync().get()),
                }),
                new OptionBlock("", new Option<?>[]{
                        new RangeOption(Component.translatable("options.guiScale"),
                                0, window.calculateScale(0, minecraft.isEnforceUnicode()), 1,
                                value -> Component.translatable((value == 0)
                                        ? "options.guiScale.auto"
                                        : String.valueOf(value)),
                                value -> {
                                    minecraftOptions.guiScale().set(value);
                                    minecraft.resizeDisplay();
                                },
                                () -> (minecraftOptions.guiScale().get())),
                        new RangeOption(Component.translatable("options.gamma"),
                                0, 100, 1,
                                value -> Component.translatable(switch (value) {
                                    case 0 -> "options.gamma.min";
                                    case 50 -> "options.gamma.default";
                                    case 100 -> "options.gamma.max";
                                    default -> String.valueOf(value);
                                }),
                                value -> minecraftOptions.gamma().set(value * 0.01),
                                () -> (int) (minecraftOptions.gamma().get() * 100.0)),
                }),
                new OptionBlock("", new Option<?>[]{
                        new SwitchOption(Component.translatable("options.viewBobbing"),
                                (value) -> minecraftOptions.bobView().set(value),
                                () -> minecraftOptions.bobView().get()),
                        new CyclingOption<>(Component.translatable("options.attackIndicator"),
                                AttackIndicatorStatus.values(),
                                value -> minecraftOptions.attackIndicator().set(value),
                                () -> minecraftOptions.attackIndicator().get())
                                .setTranslator(value -> Component.translatable(value.getKey())),
                        new SwitchOption(Component.translatable("options.autosaveIndicator"),
                                value -> minecraftOptions.showAutosaveIndicator().set(value),
                                () -> minecraftOptions.showAutosaveIndicator().get()),
                })
        };
    }

    // mods may widen these
    private static int maxOf(OptionInstance<Integer> option, int fallback) {
        return option.values() instanceof OptionInstance.IntRange range ? range.maxInclusive() : fallback;
    }

    public static OptionBlock[] getGraphicsOpts() {
        return new OptionBlock[]{
                new OptionBlock("", new Option<?>[]{
                        new RangeOption(Component.translatable("options.renderDistance"),
                                2, maxOf(minecraftOptions.renderDistance(), 32), 1,
                                (value) -> minecraftOptions.renderDistance().set(value),
                                () -> minecraftOptions.renderDistance().get()),
                        new RangeOption(Component.translatable("options.simulationDistance"),
                                5, maxOf(minecraftOptions.simulationDistance(), 32), 1,
                                (value) -> minecraftOptions.simulationDistance().set(value),
                                () -> minecraftOptions.simulationDistance().get()),
                        new CyclingOption<>(Component.translatable("options.prioritizeChunkUpdates"),
                                PrioritizeChunkUpdates.values(),
                                value -> minecraftOptions.prioritizeChunkUpdates().set(value),
                                () -> minecraftOptions.prioritizeChunkUpdates().get())
                                .setTranslator(value -> Component.translatable(value.getKey())),
                }),
                new OptionBlock("", new Option<?>[]{
                        new CyclingOption<>(Component.translatable("options.graphics"),
                                new GraphicsStatus[]{GraphicsStatus.FAST, GraphicsStatus.FANCY},
                                value -> minecraftOptions.graphicsMode().set(value),
                                () -> minecraftOptions.graphicsMode().get())
                                .setTranslator(graphicsMode -> Component.translatable(graphicsMode.getKey())),
                        new CyclingOption<>(Component.translatable("options.particles"),
                                new ParticleStatus[]{ParticleStatus.MINIMAL, ParticleStatus.DECREASED, ParticleStatus.ALL},
                                value -> minecraftOptions.particles().set(value),
                                () -> minecraftOptions.particles().get())
                                .setTranslator(particlesMode -> Component.translatable(particlesMode.getKey())),
                        new CyclingOption<>(Component.translatable("options.renderClouds"),
                                CloudStatus.values(),
                                value -> minecraftOptions.cloudStatus().set(value),
                                () -> minecraftOptions.cloudStatus().get())
                                .setTranslator(value -> Component.translatable(value.getKey())),
                        new CyclingOption<>(Component.translatable("options.ao"),
                                new Integer[]{LightMode.FLAT, LightMode.SMOOTH, LightMode.SUB_BLOCK},
                                (value) -> {
                                    if (value > LightMode.FLAT)
                                        minecraftOptions.ambientOcclusion().set(true);
                                    else
                                        minecraftOptions.ambientOcclusion().set(false);

                                    config.ambientOcclusion = value;

                                    minecraft.levelRenderer.allChanged();
                                },
                                () -> config.ambientOcclusion)
                                .setTranslator(value -> Component.translatable(switch (value) {
                                    case LightMode.FLAT -> "options.off";
                                    case LightMode.SMOOTH -> "options.on";
                                    case LightMode.SUB_BLOCK -> "nyalkan.options.ao.subBlock";
                                    default -> "nyalkan.options.unknown";
                                }))
                                .setTooltip(Component.translatable("nyalkan.options.ao.subBlock.tooltip")),
                        new RangeOption(Component.translatable("options.biomeBlendRadius"),
                                0, 7, 1,
                                value -> {
                                    int v = value * 2 + 1;
                                    return Component.nullToEmpty("%d x %d".formatted(v, v));
                                },
                                (value) -> {
                                    minecraftOptions.biomeBlendRadius().set(value);
                                    minecraft.levelRenderer.allChanged();
                                },
                                () -> minecraftOptions.biomeBlendRadius().get()),
                }),
                new OptionBlock("", new Option<?>[]{
                        new SwitchOption(Component.translatable("options.entityShadows"),
                                value -> minecraftOptions.entityShadows().set(value),
                                () -> minecraftOptions.entityShadows().get()),
                        new RangeOption(Component.translatable("options.entityDistanceScaling"),
                                50, 500, 25,
                                value -> minecraftOptions.entityDistanceScaling().set(value * 0.01),
                                () -> minecraftOptions.entityDistanceScaling().get().intValue() * 100),
                        new CyclingOption<>(Component.translatable("options.mipmapLevels"),
                                new Integer[]{0, 1, 2, 3, 4},
                                value -> {
                                    minecraftOptions.mipmapLevels().set(value);
                                    minecraft.updateMaxMipLevel(value);
                                    minecraft.delayTextureReload();
                                },
                                () -> minecraftOptions.mipmapLevels().get())
                                .setTranslator(value -> Component.nullToEmpty(value.toString()))
                })
        };
    }

    public static OptionBlock[] getOptimizationOpts() {
        return new OptionBlock[]{
                new OptionBlock("", new Option[]{
                        new CyclingOption<>(Component.translatable("nyalkan.options.advCulling"),
                                new Integer[]{1, 2, 3, 10},
                                value -> config.advCulling = value,
                                () -> config.advCulling)
                                .setTranslator(value -> Component.translatable(switch (value) {
                                    case 1 -> "nyalkan.options.advCulling.aggressive";
                                    case 2 -> "nyalkan.options.advCulling.normal";
                                    case 3 -> "nyalkan.options.advCulling.conservative";
                                    case 10 -> "options.off";
                                    default -> "nyalkan.options.unknown";
                                }))
                                .setTooltip(Component.translatable("nyalkan.options.advCulling.tooltip")),
                        new SwitchOption(Component.translatable("nyalkan.options.entityCulling"),
                                value -> config.entityCulling = value,
                                () -> config.entityCulling)
                                .setTooltip(Component.translatable("nyalkan.options.entityCulling.tooltip")),
                        new SwitchOption(Component.translatable("nyalkan.options.animateOnlyVisible"),
                                value -> config.animateOnlyVisible = value,
                                () -> config.animateOnlyVisible)
                                .setTooltip(Component.translatable("nyalkan.options.animateOnlyVisible.tooltip")),
                        new SwitchOption(Component.translatable("nyalkan.options.poseReuse"),
                                value -> config.poseReuse = value,
                                () -> config.poseReuse)
                                .setTooltip(Component.translatable("nyalkan.options.poseReuse.tooltip")),
                        new SwitchOption(Component.translatable("nyalkan.options.modelReplay"),
                                value -> config.modelReplay = value,
                                () -> config.modelReplay)
                                .setTooltip(Component.translatable("nyalkan.options.modelReplay.tooltip")),
                        new SwitchOption(Component.translatable("nyalkan.options.blockEntityReplay"),
                                value -> config.blockEntityReplay = value,
                                () -> config.blockEntityReplay)
                                .setTooltip(Component.translatable("nyalkan.options.blockEntityReplay.tooltip")),
                        new SwitchOption(Component.translatable("nyalkan.options.entityReplay"),
                                value -> config.entityReplay = value,
                                () -> config.entityReplay)
                                .setTooltip(Component.translatable("nyalkan.options.entityReplay.tooltip")),
                        new SwitchOption(Component.translatable("nyalkan.options.occlusionCulling"),
                                value -> config.occlusionCulling = value,
                                () -> config.occlusionCulling)
                                .setTooltip(Component.translatable("nyalkan.options.occlusionCulling.tooltip")),
                        new SwitchOption(Component.translatable("nyalkan.options.tickSpreading"),
                                value -> config.tickSpreading = value,
                                () -> config.tickSpreading)
                                .setTooltip(Component.translatable("nyalkan.options.tickSpreading.tooltip")),
                        new SwitchOption(Component.translatable("nyalkan.options.entityBackfaceCulling"),
                                value -> config.entityBackfaceCulling = value,
                                () -> config.entityBackfaceCulling)
                                .setTooltip(Component.translatable("nyalkan.options.entityBackfaceCulling.tooltip")),
                        new SwitchOption(Component.translatable("nyalkan.options.animationLod"),
                                value -> config.animationLod = value,
                                () -> config.animationLod)
                                .setTooltip(Component.translatable("nyalkan.options.animationLod.tooltip")),
                        new SwitchOption(Component.translatable("nyalkan.options.hudBatching"),
                                value -> config.hudBatching = value,
                                () -> config.hudBatching)
                                .setTooltip(Component.translatable("nyalkan.options.hudBatching.tooltip")),
                        new SwitchOption(Component.translatable("nyalkan.options.hudCaching"),
                                value -> config.hudCaching = value,
                                () -> config.hudCaching)
                                .setTooltip(Component.translatable("nyalkan.options.hudCaching.tooltip")),
                        new SwitchOption(Component.translatable("nyalkan.options.smoothHudUpdates"),
                                value -> config.smoothHudUpdates = value,
                                () -> config.smoothHudUpdates)
                                .setTooltip(Component.translatable("nyalkan.options.smoothHudUpdates.tooltip")),
                        new RangeOption(Component.translatable("nyalkan.options.hudLowFpsRate"),
                                0, 120, 10,
                                value -> value == 0 ? Component.translatable("options.off") : Component.literal(value + " Hz"),
                                value -> config.hudLowFpsRate = value,
                                () -> config.hudLowFpsRate)
                                .setTooltip(Component.translatable("nyalkan.options.hudLowFpsRate.tooltip")),
                        new SwitchOption(Component.translatable("nyalkan.options.gpuParticles"),
                                value -> config.gpuParticles = value,
                                () -> config.gpuParticles)
                                .setTooltip(Component.translatable("nyalkan.options.gpuParticles.tooltip")),
                        new SwitchOption(Component.translatable("nyalkan.options.mipmapSpriteFix"),
                                value -> config.mipmapSpriteFix = value,
                                () -> config.mipmapSpriteFix)
                                .setTooltip(Component.translatable("nyalkan.options.mipmapSpriteFix.tooltip")),
                        new SwitchOption(Component.translatable("nyalkan.options.bakedBlockEntities"),
                                value -> {
                                    config.bakedBlockEntities = value;
                                    minecraft.levelRenderer.allChanged();
                                },
                                () -> config.bakedBlockEntities)
                                .setTooltip(Component.translatable("nyalkan.options.bakedBlockEntities.tooltip")),
                        new SwitchOption(Component.translatable("nyalkan.options.uniqueOpaqueLayer"),
                                value -> {
                                    config.uniqueOpaqueLayer = value;
                                    minecraft.levelRenderer.allChanged();
                                },
                                () -> config.uniqueOpaqueLayer)
                                .setTooltip(Component.translatable("nyalkan.options.uniqueOpaqueLayer.tooltip")),
                        new SwitchOption(Component.translatable("nyalkan.options.indirectDraw"),
                                value -> config.indirectDraw = value,
                                () -> config.indirectDraw)
                                .setTooltip(Component.translatable("nyalkan.options.indirectDraw.tooltip")),
                })
        };

    }

    public static OptionBlock[] getOtherOpts() {
        return new OptionBlock[]{
                new OptionBlock("", new Option[]{
                        new RangeOption(Component.translatable("nyalkan.options.frameQueue"),
                                2, 5, 1,
                                value -> {
                                    config.frameQueueSize = value;
                                    Renderer.scheduleSwapChainUpdate();
                                }, () -> config.frameQueueSize)
                                .setTooltip(Component.translatable("nyalkan.options.frameQueue.tooltip")),
                        new CyclingOption<>(Component.translatable("nyalkan.options.deviceSelector"),
                                IntStream.range(-1, DeviceManager.suitableDevices.size()).boxed().toArray(Integer[]::new),
                                value -> config.device = value,
                                () -> config.device)
                                .setTranslator(value -> Component.translatable((value == -1)
                                        ? "nyalkan.options.deviceSelector.auto"
                                        : DeviceManager.suitableDevices.get(value).deviceName)
                                )
                                .setTooltip(Component.nullToEmpty("%s: %s".formatted(
                                        Component.translatable("nyalkan.options.deviceSelector.tooltip").getString(),
                                        DeviceManager.device.deviceName
                                ))
                        ),
                        new SwitchOption(Component.translatable("nyalkan.options.fpsOverlay"),
                                value -> config.fpsOverlay = value,
                                () -> config.fpsOverlay)
                                .setTooltip(Component.translatable("nyalkan.options.fpsOverlay.tooltip"))
                })
        };

    }
}
