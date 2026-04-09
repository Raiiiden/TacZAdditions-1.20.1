package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.client.SodiumDLAdapter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import toni.sodiumdynamiclights.SodiumDynamicLights;

@Mixin(value = SodiumDynamicLights.class, remap = false)
public class SodiumSelfLuminanceMixin {

    @Inject(
            method = "getLivingEntityLuminanceFromItems",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private static void injectFlashLuminance(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (!(entity instanceof Player player)) return;
        int flash = SodiumDLAdapter.getFlashLuminance(player.getId());
        if (flash > cir.getReturnValue()) {
            cir.setReturnValue(flash);
        }
    }
}