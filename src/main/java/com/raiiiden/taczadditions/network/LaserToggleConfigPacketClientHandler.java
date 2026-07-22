package com.raiiiden.taczadditions.network;

import com.raiiiden.taczadditions.client.ClientLaserToggleState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class LaserToggleConfigPacketClientHandler {
    private LaserToggleConfigPacketClientHandler() {}

    public static void handle(LaserToggleConfigPacket packet) {
        ClientLaserToggleState.setToggleAllowed(packet.toggleAllowed);
    }
}
