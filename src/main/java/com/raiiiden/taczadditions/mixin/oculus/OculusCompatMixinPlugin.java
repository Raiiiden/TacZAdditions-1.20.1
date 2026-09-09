package com.raiiiden.taczadditions.mixin.oculus;

import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

// Applies the FinalPassRenderer swap-skip mixin only when Oculus is present, picking the package by
// version: 1.7.0 and up uses net.irisshaders, older uses net.coderbot. Absent, neither applies.
public class OculusCompatMixinPlugin implements IMixinConfigPlugin {
    private static final DefaultArtifactVersion NEWLY = new DefaultArtifactVersion("1.7.0");

    private final boolean loaded;
    private final boolean isNewly;

    public OculusCompatMixinPlugin() {
        boolean isLoaded = false;
        boolean newly = false;
        IModFileInfo file = LoadingModList.get().getModFileById("oculus");
        if (file != null) {
            isLoaded = true;
            for (IModInfo mod : file.getMods()) {
                if ("oculus".equals(mod.getModId())) {
                    ArtifactVersion version = mod.getVersion();
                    newly = version.compareTo(NEWLY) >= 0;
                    break;
                }
            }
        }
        this.loaded = isLoaded;
        this.isNewly = newly;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!loaded) {
            return false;
        }
        if (mixinClassName.contains(".newly.")) {
            return isNewly;
        }
        if (mixinClassName.contains(".legacy.")) {
            return !isNewly;
        }
        return false;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
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

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}