package com.raiiiden.taczadditions.mixin.oculus.newly;

import net.irisshaders.iris.shadows.ShadowRenderTargets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ShadowRenderTargets.class, remap = false)
public interface ShadowRenderTargetsAccessor {
    @Accessor("flipped")
    boolean[] taczadditions$getFlipped();

    @Accessor("fullClearRequired") boolean taczadditions$isFullClearRequired();
    @Accessor("fullClearRequired") void taczadditions$setFullClearRequired(boolean value);
    @Accessor("translucentDepthDirty") boolean taczadditions$isTranslucentDepthDirty();
    @Accessor("translucentDepthDirty") void taczadditions$setTranslucentDepthDirty(boolean value);
}
