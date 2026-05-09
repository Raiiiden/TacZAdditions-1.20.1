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
        ClientGunFireLightSource newLight = new ClientGunFireLightSource(entity, lightLevel);
        activeLights.put(entity, newLight);
        DynamicLights.addLightSource(newLight); // register once, never remove
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (ClientGunFireLightSource light : activeLights.values()) {
            light.tick(); // just advance the timer, getLightLevel() returns 0 when expired
        }
        // Prune dead entities only (player disconnect, dimension change, etc.)
        activeLights.entrySet().removeIf(e -> !e.getKey().isAlive());
    }
}
