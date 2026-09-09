package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.client.FreeAimHandler;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// Free aim steers the gun before it steers the player, so the movement is taken out of the turn
// here and only what the deadzone could not hold is passed on to the camera.
@Mixin(MouseHandler.class)
public class FreeAimTurnMixin {

    // Entity.turn scales both arguments by this before adding them to the player's rotation.
    private static final double DEGREES_PER_UNIT = 0.15;

    @ModifyArg(
            method = "turnPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"
            ),
            index = 0
    )
    private double taczadditions$divertYawToGun(double yaw) {
        if (yaw == 0.0) return yaw;
        return FreeAimHandler.absorbYaw(yaw * DEGREES_PER_UNIT) / DEGREES_PER_UNIT;
    }

    @ModifyArg(
            method = "turnPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"
            ),
            index = 1
    )
    private double taczadditions$divertPitchToGun(double pitch) {
        if (pitch == 0.0) return pitch;
        return FreeAimHandler.absorbPitch(pitch * DEGREES_PER_UNIT) / DEGREES_PER_UNIT;
    }
}
