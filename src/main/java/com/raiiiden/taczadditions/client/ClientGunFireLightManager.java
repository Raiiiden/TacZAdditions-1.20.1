package com.raiiiden.taczadditions.client;

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

// NOT annotated with @Mod.EventBusSubscriber — registered manually in TaczAdditions.clientSetup()
// only when "dynamiclights" (Atomicstryker) is loaded, so this class and its imports are
// never resolved unless that mod is actually installed.
public class ClientGunFireLightManager {
    private static final Map<Entity, ClientGunFireLightSource> activeLights = new HashMap<>();

    public static void addLight(Entity entity, int lightLevel) {
        ClientGunFireLightSource existing = activeLights.get(entity);
        if (existing != null) {
            existing.updateLight(lightLevel);
            return;
        }
        // Skip if another dynamic light already occupies this entity's block position.
        // AtomicStryker tracks one lit_air/lit_cave_air/lit_water block per source, and
        // adding a second source here means our removeLightSource() reverts the shared
        // block — wiping out a torch/lantern light that would otherwise stay lit.
        if (hasExistingDynamicLight(entity)) return;

        ClientGunFireLightSource newLight = new ClientGunFireLightSource(entity, lightLevel);
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
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Iterator<Map.Entry<Entity, ClientGunFireLightSource>> it = activeLights.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Entity, ClientGunFireLightSource> entry = it.next();
            ClientGunFireLightSource light = entry.getValue();
            light.tick();
            if (light.getLightLevel() == 0) {
                DynamicLights.removeLightSource(light);
                it.remove();
            }
        }
    }
}
