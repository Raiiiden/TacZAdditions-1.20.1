package com.raiiiden.taczadditions.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.raiiiden.taczadditions.TaczAdditions;
import com.raiiiden.taczadditions.network.ModNetworking;
import com.raiiiden.taczadditions.util.LaserToggleData;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = TaczAdditions.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LaserToggleKeybind {
    private static final KeyMapping TOGGLE_LASER = new KeyMapping(
            "key.taczadditions.toggle_laser",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.taczadditions"
    );

    private LaserToggleKeybind() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_LASER);
        MinecraftForge.EVENT_BUS.register(ForgeEvents.class);
    }

    public static final class ForgeEvents {
        private ForgeEvents() {}

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            Minecraft mc = Minecraft.getInstance();
            while (TOGGLE_LASER.consumeClick()) {
                if (mc.player == null || mc.level == null) continue;
                if (!ClientLaserToggleState.isToggleAllowed()) continue;
                if (!ClientNetworkState.isServerModPresent()) continue;
                if (!LaserToggleData.hasLaser(mc.player.getMainHandItem())) continue;

                ModNetworking.sendLaserToggleRequest();
            }
        }

        @SubscribeEvent
        public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            ClientLaserToggleState.setToggleAllowed(ClientNetworkState.isServerModPresent());
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientLaserToggleState.reset();
        }
    }
}
