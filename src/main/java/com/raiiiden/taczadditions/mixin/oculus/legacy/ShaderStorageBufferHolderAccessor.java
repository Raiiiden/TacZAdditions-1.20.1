package com.raiiiden.taczadditions.mixin.oculus.legacy;

import net.coderbot.iris.gl.buffer.ShaderStorageBuffer;
import net.coderbot.iris.gl.buffer.ShaderStorageBufferHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ShaderStorageBufferHolder.class, remap = false)
public interface ShaderStorageBufferHolderAccessor {
    @Accessor("buffers")
    ShaderStorageBuffer[] taczadditions$getBuffers();
}
