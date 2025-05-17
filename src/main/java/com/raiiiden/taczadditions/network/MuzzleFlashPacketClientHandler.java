package com.raiiiden.taczadditions.network;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MuzzleFlashPacketClientHandler {
    public static void handle(MuzzleFlashPacket msg) {
        // Some muzzle smoke here later maybe
    }
}
