package com.raiiiden.taczadditions.network;

import com.raiiiden.taczadditions.client.ClientGunFireLightManager;
import com.raiiiden.taczadditions.client.SodiumDLAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;

@OnlyIn(Dist.CLIENT)
public class MuzzleFlashPacketClientHandler {

    private static final boolean HAS_SODIUM_DL = ModList.get().isLoaded("sodiumdynamiclights");
    private static final boolean HAS_ATOMIC_DL = ModList.get().isLoaded("dynamiclights");

    public static void handle(MuzzleFlashPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity entity = mc.level.getEntity(msg.entityId);
        if (entity == null) return;

        if (msg.lightLevel <= 0) {
            if (msg.color >= 0) {
                SodiumDLAdapter.addFlash(entity, 0, msg.color);
            }
        } else if (HAS_SODIUM_DL) {
            SodiumDLAdapter.addFlash(entity, msg.lightLevel, msg.color);
        } else if (HAS_ATOMIC_DL) {
            if (msg.color >= 0) {
                SodiumDLAdapter.addFlash(entity, 0, msg.color);
            }
            ClientGunFireLightManager.addLight(entity, msg.lightLevel);
        }
    }
}
