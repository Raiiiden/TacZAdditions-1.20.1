package com.raiiiden.taczadditions.mixin;

import com.tacz.guns.client.event.FirstPersonRenderGunEvent;
import com.tacz.guns.client.model.BedrockGunModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FirstPersonRenderGunEvent.class)
public interface FirstPersonRenderGunEventInvoker {
    @Invoker(value = "applyShootSwayAndRotation", remap = false)
    static void callApplyShootSwayAndRotation(BedrockGunModel model, float aimingProgress) {
        throw new AssertionError();
    }
}