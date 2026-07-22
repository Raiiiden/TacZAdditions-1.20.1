package com.raiiiden.taczadditions.server;

import com.raiiiden.taczadditions.TaczAdditions;
import com.raiiiden.taczadditions.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TaczAdditions.MODID)
public final class LaserToggleServerEvents {
    private LaserToggleServerEvents() {}

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetworking.sendLaserToggleConfig(player);
        }
    }
}
