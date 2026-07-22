package com.raiiiden.taczadditions.network;

import com.raiiiden.taczadditions.client.RemoteLaserDots;
import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LaserDotPacketClientHandler {
    public static void handle(LaserDotSyncPacket msg) {
        if (!TacZAdditionsConfig.CLIENT.enableLaserDot.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (mc.player != null && mc.player.getId() == msg.entityId) return;

        RemoteLaserDots.update(msg.entityId, msg.x, msg.y, msg.z, msg.color);
    }
}