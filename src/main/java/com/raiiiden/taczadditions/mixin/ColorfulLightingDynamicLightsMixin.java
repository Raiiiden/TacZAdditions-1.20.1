package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.client.ColorfulLightingBridge;
import com.raiiiden.taczadditions.client.SodiumDLAdapter;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Supplies configured RGB colors to the optional Colorful Lighting resolver.
@Pseudo
@Mixin(
        targets = "me.erykczy.colorfullighting.compat.dynamiclights.DynamicLightsCompat",
        remap = false
)
public class ColorfulLightingDynamicLightsMixin {
    @Inject(method = "resolveSourceColor", at = @At("HEAD"), cancellable = true, remap = false)
    private static void taczadditions$useMuzzleFlashColor(
            Object source, CallbackInfoReturnable<Object> cir) {
        if (!(source instanceof Entity entity)) return;
        taczadditions$setColor(entity, cir);
    }

    @Inject(method = "resolveEntityColor", at = @At("HEAD"), cancellable = true, remap = false)
    private static void taczadditions$colorFallbackLightBlock(
            Entity entity, CallbackInfoReturnable<Object> cir) {
        taczadditions$setColor(entity, cir);
    }

    private static void taczadditions$setColor(
            Entity entity, CallbackInfoReturnable<Object> cir) {
        int color = SodiumDLAdapter.getFlashColor(entity.getId());
        if (color < 0) return;

        Object colorfulLightingColor = ColorfulLightingBridge.createColor(color);
        if (colorfulLightingColor != null) {
            cir.setReturnValue(colorfulLightingColor);
        }
    }
}
