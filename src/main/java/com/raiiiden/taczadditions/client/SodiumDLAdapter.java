package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.TaczAdditions;
import dev.lambdaurora.lambdynlights.api.DynamicLightHandler;
import dev.lambdaurora.lambdynlights.api.DynamicLightHandlers;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side adapter for Dynamic Lights Reforged (sodiumdynamiclights).
 *
 * The DL mod does not automatically call our registered entity handler for the local
 * player — we must force-call lambdynlights$dynamicLightTick() ourselves, which in turn
 * calls DynamicLightHandlers.getLuminanceFrom() → our handler → FLASH_MAP value →
 * schedules the chunk rebuild.
 *
 * This class is ONLY loaded when "sodiumdynamiclights" is detected — never reference it
 * except inside a guarded block in TaczAdditions.
 */
public class SodiumDLAdapter {

    // entityId → [lightLevel, ticksRemaining]
    private static final Map<Integer, int[]> FLASH_MAP = new ConcurrentHashMap<>();

    // Cached reflection method for lambdynlights$dynamicLightTick on Player
    private static Method dlTickMethod = null;
    private static boolean dlTickResolved = false;

    /** Call once from clientSetup when sodiumdynamiclights is confirmed loaded. */
    public static void init() {
        DynamicLightHandlers.registerDynamicLightHandler(
                EntityType.PLAYER,
                (DynamicLightHandler<Player>) player -> {
                    int[] state = FLASH_MAP.get(player.getId());
                    return state != null ? state[0] : 0;
                }
        );
        MinecraftForge.EVENT_BUS.register(SodiumDLAdapter.class);
        TaczAdditions.LOGGER.info("[TacZAdditions] SodiumDLAdapter initialized");
    }

    /** Called from the client-side GunFireEvent and from the server packet. */
    public static void addFlash(Entity entity, int lightLevel) {
        FLASH_MAP.put(entity.getId(), new int[]{lightLevel, 6});
        forceDLTick(entity);
    }

    /**
     * Force-calls lambdynlights$dynamicLightTick() on the entity via reflection.
     * This is necessary because the DL mod does not automatically poll our registered
     * entity handler for the local player — we must trigger it ourselves.
     */
    private static void forceDLTick(Entity entity) {
        if (!dlTickResolved) {
            dlTickResolved = true;
            // Try known mixin method name variants
            for (String name : new String[]{
                    "lambdynlights$dynamicLightTick",
                    "sodiumdynamiclights$dynamicLightTick"
            }) {
                try {
                    Method m = entity.getClass().getMethod(name);
                    dlTickMethod = m;
                    TaczAdditions.LOGGER.info("[TacZAdditions] Found DL tick method: {}", name);
                    break;
                } catch (NoSuchMethodException ignored) {}
            }
            if (dlTickMethod == null) {
                TaczAdditions.LOGGER.warn("[TacZAdditions] No DL tick method found on entity — dynamic lights may not update");
            }
        }
        if (dlTickMethod != null) {
            try {
                dlTickMethod.invoke(entity);
            } catch (Exception e) {
                TaczAdditions.LOGGER.warn("[TacZAdditions] DL tick invoke failed: {}", e.getMessage());
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getInstance().isPaused()) return;

        Minecraft mc = Minecraft.getInstance();
        Iterator<Map.Entry<Integer, int[]>> it = FLASH_MAP.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, int[]> entry = it.next();
            if (--entry.getValue()[1] <= 0) {
                it.remove();
                // Force a tick so the DL mod sees luminance drop back to 0
                if (mc.level != null) {
                    Entity entity = mc.level.getEntity(entry.getKey());
                    if (entity != null) forceDLTick(entity);
                }
            }
        }
    }
}
