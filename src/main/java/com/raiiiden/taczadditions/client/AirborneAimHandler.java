package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Drops the player out of aim once they leave the ground. Blocking a new aim is handled by
// LocalPlayerAimMixin, which shares the airborne test below.
@Mod.EventBusSubscriber(modid = "taczadditions", value = Dist.CLIENT)
public final class AirborneAimHandler {

    private AirborneAimHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!TacZAdditionsConfig.COMMON.disableAimingWhileAirborne.get()) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !isAirborne(player)) return;
        if (!IGun.mainHandHoldGun(player)) return;

        IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
        if (operator.isAim()) {
            operator.aim(false);
        }
    }

    // Only unsupported freefall counts. Everything else here leaves onGround false for reasons that
    // have nothing to do with jumping, and would otherwise lock aiming off indefinitely.
    public static boolean isAirborne(LocalPlayer player) {
        if (player.onGround()) return false;
        if (player.getAbilities().flying || player.isFallFlying()) return false;
        if (player.isPassenger()) return false;
        if (player.onClimbable()) return false;
        return !player.isInWater() && !player.isInLava();
    }
}
