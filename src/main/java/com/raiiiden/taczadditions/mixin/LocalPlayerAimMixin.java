package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.client.AirborneAimHandler;
import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.tacz.guns.client.gameplay.LocalPlayerAim;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Refuses a mid-air scope-in when disableAimingWhileAirborne is on. Every aim entry point, key,
// toggle and controller alike, funnels through LocalPlayerAim#aim.
@Mixin(value = LocalPlayerAim.class, remap = false)
public class LocalPlayerAimMixin {

    @Inject(method = "aim", at = @At("HEAD"), cancellable = true, remap = false)
    private void taczadditions$blockAirborneAim(boolean isAim, CallbackInfo ci) {
        // Never block releasing an aim, or the player could be stuck scoped after taking off.
        if (!isAim) return;
        if (!TacZAdditionsConfig.COMMON.disableAimingWhileAirborne.get()) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && AirborneAimHandler.isAirborne(player)) {
            ci.cancel();
        }
    }
}
