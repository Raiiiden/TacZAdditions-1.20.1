package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.TaczAdditions;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SodiumDLAdapter {

    private static final long FLASH_DURATION_MS = 50; // adjust this

    private static final Map<Integer, Long> FLASH_MAP = new ConcurrentHashMap<>();

    public static void init() {
        MinecraftForge.EVENT_BUS.register(SodiumDLAdapter.class);
        TaczAdditions.LOGGER.info("[TacZAdditions] SodiumDLAdapter initialized");
    }

    public static void addFlash(Entity entity, int lightLevel) {
        FLASH_MAP.put(entity.getId(), System.currentTimeMillis() + FLASH_DURATION_MS);
    }

    public static int getFlashLuminance(int entityId) {
        Long expiry = FLASH_MAP.get(entityId);
        if (expiry == null) return 0;
        return System.currentTimeMillis() < expiry ? 15 : 0;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getInstance().isPaused()) return;

        long now = System.currentTimeMillis();
        FLASH_MAP.entrySet().removeIf(entry -> now >= entry.getValue());
    }
}