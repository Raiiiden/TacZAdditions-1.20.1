package com.raiiiden.taczadditions.client.event;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "taczadditions", value = Dist.CLIENT)
public class GunFireListener {
    @SubscribeEvent
    public static void onChat(ClientChatEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        if (event.getMessage().equalsIgnoreCase("checkshoot")) {
            IClientPlayerGunOperator gunOperator = IClientPlayerGunOperator.fromLocalPlayer(player);
            if (gunOperator != null) {
                System.out.println("[DEBUG] shoot() is being called from class: " + gunOperator.getClass().getName());
            } else {
                System.out.println("[DEBUG] No implementation found for shoot().");
            }
        }
    }
}
