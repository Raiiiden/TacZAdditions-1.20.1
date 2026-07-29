package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.client.SodiumDLAdapter;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "toni.sodiumdynamiclights.SodiumDynamicLights", remap = false)
public class SodiumSelfLuminanceMixin {

    @Inject(
            method = "getLivingEntityLuminanceFromItems",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private static void injectFlashLuminance(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        // Any living shooter, not just players — NPCs firing guns light their surroundings too.
        if (entity == null) return;
        int flash = SodiumDLAdapter.getFlashLuminance(entity.getId());
        if (flash > cir.getReturnValue()) {
            cir.setReturnValue(flash);
        }
    }
}
