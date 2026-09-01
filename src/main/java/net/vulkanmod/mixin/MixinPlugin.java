package net.vulkanmod.mixin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class MixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogManager.getLogger("VulkanMod");
    private static final String MERGED = "Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;";
    private static final Pattern HANDLER = Pattern.compile("(?:handler|redirect|modify|localvar|constant|args)\\$[a-z0-9]{3}\\d{3}\\$.+");

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    // report unmatched injectors
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        Set<String> called = new HashSet<>();
        for (MethodNode method : targetClass.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call) {
                    called.add(call.name);
                } else if (insn instanceof InvokeDynamicInsnNode indy) {
                    for (Object arg : indy.bsmArgs)
                        if (arg instanceof Handle handle)
                            called.add(handle.getName());
                }
            }
        }

        for (MethodNode method : targetClass.methods) {
            if (HANDLER.matcher(method.name).matches() && !called.contains(method.name) && mergedFrom(method, mixinClassName))
                LOGGER.warn("Compat check: {} injector {} matched nothing in {}",
                        mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1), method.name, targetClassName);
        }
    }

    private static boolean mergedFrom(MethodNode method, String mixinClassName) {
        if (method.visibleAnnotations == null)
            return false;

        for (AnnotationNode annotation : method.visibleAnnotations) {
            if (!MERGED.equals(annotation.desc) || annotation.values == null)
                continue;
            for (int i = 0; i + 1 < annotation.values.size(); i += 2)
                if ("mixin".equals(annotation.values.get(i)) && mixinClassName.equals(annotation.values.get(i + 1)))
                    return true;
        }
        return false;
    }
}
