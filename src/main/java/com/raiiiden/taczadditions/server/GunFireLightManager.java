package com.raiiiden.taczadditions.server;

import atomicstryker.dynamiclights.server.DynamicLights;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// NOT annotated with @Mod.EventBusSubscriber — registered manually in TaczAdditions
// only when the "dynamiclights" (Atomicstryker) mod is present, so this class is
// never loaded (and its DynamicLights import never resolved) unless that mod is installed.
public class GunFireLightManager {
    private static final Map<Entity, GunFireLightSource> activeLights = new HashMap<>();

    public static void addLight(Entity entity, int lightLevel) {
        GunFireLightSource existingLight = activeLights.get(entity);

        if (existingLight != null) {
            existingLight.updateLight(lightLevel);
            return;
        }
        // Skip if another dynamic light already occupies this entity's block position.
        // AtomicStryker tracks one lit_air/lit_cave_air/lit_water block per source, and
        // adding a second source here means our removeLightSource() reverts the shared
        // block — wiping out a torch/lantern light that would otherwise stay lit.
        if (hasExistingDynamicLight(entity)) return;

        GunFireLightSource newLight = new GunFireLightSource(entity, lightLevel);
        activeLights.put(entity, newLight);
        DynamicLights.addLightSource(newLight);
    }

    private static boolean hasExistingDynamicLight(Entity entity) {
        Level level = entity.level();
        if (level == null) return false;
        BlockPos pos = entity.blockPosition();
        Block block = level.getBlockState(pos).getBlock();
        return block == DynamicLights.LIT_AIR_BLOCK.get()
                || block == DynamicLights.LIT_CAVE_AIR_BLOCK.get()
                || block == DynamicLights.LIT_WATER_BLOCK.get();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Iterator<Map.Entry<Entity, GunFireLightSource>> it = activeLights.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Entity, GunFireLightSource> entry = it.next();
            GunFireLightSource light = entry.getValue();

            light.tick();

            if (light.getLightLevel() == 0) {
                DynamicLights.removeLightSource(light);
                it.remove();
            }
        }
    }
}
