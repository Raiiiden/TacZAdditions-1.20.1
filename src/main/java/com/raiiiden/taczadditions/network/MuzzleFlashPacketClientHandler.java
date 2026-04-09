package com.raiiiden.taczadditions.network;

import com.raiiiden.taczadditions.client.SodiumDLAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;

@OnlyIn(Dist.CLIENT)
public class MuzzleFlashPacketClientHandler {

    private static final boolean HAS_SODIUM_DL = ModList.get().isLoaded("sodiumdynamiclights");

    public static void handle(MuzzleFlashPacket msg) {
        if (!HAS_SODIUM_DL) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity entity = mc.level.getEntity(msg.entityId);
        if (entity == null) return;

        SodiumDLAdapter.addFlash(entity, msg.lightLevel);
    }
}
