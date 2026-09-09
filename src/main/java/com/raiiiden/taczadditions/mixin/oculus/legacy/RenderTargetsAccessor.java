package com.raiiiden.taczadditions.mixin.oculus.legacy;

import net.coderbot.iris.rendertarget.RenderTargets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RenderTargets.class, remap = false)
public interface RenderTargetsAccessor {
    @Accessor("fullClearRequired") boolean taczadditions$isFullClearRequired();
    @Accessor("fullClearRequired") void taczadditions$setFullClearRequired(boolean value);
    @Accessor("translucentDepthDirty") boolean taczadditions$isTranslucentDepthDirty();
    @Accessor("translucentDepthDirty") void taczadditions$setTranslucentDepthDirty(boolean value);
    @Accessor("handDepthDirty") boolean taczadditions$isHandDepthDirty();
    @Accessor("handDepthDirty") void taczadditions$setHandDepthDirty(boolean value);
}
