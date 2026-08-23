package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.TaczAdditions;
import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.raiiiden.taczadditions.network.ModNetworking;
import com.tacz.guns.api.item.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Tells the server where the gun is pointing, since only this client knows: it comes from the
// player's own mouse and their own tuning, not from anything the server can work out for itself.
@Mod.EventBusSubscriber(modid = TaczAdditions.MODID, value = Dist.CLIENT)
public final class ClientFreeAimSync {

    // Angles smaller than this change nothing a bullet can hit, so they are not worth a packet.
    private static final float EPSILON = 0.05f;

    private static float sentYaw;
    private static float sentPitch;
    private static boolean hasSent;

    private ClientFreeAimSync() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            hasSent = false;
            return;
        }

        boolean active = FreeAimHandler.isEnabled()
                && TacZAdditionsConfig.COMMON.freeAimAffectsBulletAngle.get()
                && IGun.mainHandHoldGun(player);

        float yaw = active ? FreeAimHandler.yawOffset() : 0f;
        float pitch = active ? FreeAimHandler.pitchOffset() : 0f;

        // Nothing has been claimed yet and there is nothing to claim.
        if (!hasSent && yaw == 0f && pitch == 0f) return;
        if (hasSent && Math.abs(yaw - sentYaw) < EPSILON && Math.abs(pitch - sentPitch) < EPSILON) return;

        ModNetworking.sendFreeAim(yaw, pitch);
        sentYaw = yaw;
        sentPitch = pitch;
        hasSent = true;
    }
}
