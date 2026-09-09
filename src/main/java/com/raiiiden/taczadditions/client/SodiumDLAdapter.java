package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.TaczAdditions;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SodiumDLAdapter {

    private static final long FLASH_DURATION_MS = 50;
    // Keep color metadata longer than the flash so Colorful Lighting can sample it reliably.
    private static final long COLOR_METADATA_DURATION_MS = 500;

    private static final Map<Integer, Flash> FLASH_MAP = new ConcurrentHashMap<>();

    public static void init() {
        MinecraftForge.EVENT_BUS.register(SodiumDLAdapter.class);
        TaczAdditions.LOGGER.info("[TacZAdditions] SodiumDLAdapter initialized");
    }

    public static void addFlash(Entity entity, int lightLevel, int color) {
        long now = System.currentTimeMillis();
        FLASH_MAP.put(entity.getId(),
                new Flash(
                        now + FLASH_DURATION_MS,
                        now + COLOR_METADATA_DURATION_MS,
                        lightLevel,
                        color));
    }

    public static int getFlashLuminance(int entityId) {
        Flash flash = FLASH_MAP.get(entityId);
        if (flash == null || System.currentTimeMillis() >= flash.lightExpiry()) return 0;
        return flash.lightLevel();
    }

    // Returns the active RGB888 override, or -1 when none exists.
    public static int getFlashColor(int entityId) {
        Flash flash = FLASH_MAP.get(entityId);
        if (flash == null || System.currentTimeMillis() >= flash.colorExpiry()) return -1;
        return flash.color();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getInstance().isPaused()) return;

        long now = System.currentTimeMillis();
        FLASH_MAP.entrySet().removeIf(entry -> now >= entry.getValue().colorExpiry());
    }

    private record Flash(long lightExpiry, long colorExpiry, int lightLevel, int color) {
    }
}
