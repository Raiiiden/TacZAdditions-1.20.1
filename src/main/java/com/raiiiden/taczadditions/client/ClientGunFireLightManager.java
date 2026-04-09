package com.raiiiden.taczadditions.client;

import atomicstryker.dynamiclights.server.DynamicLights;
import net.minecraft.world.entity.Entity;
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
        } else {
            ClientGunFireLightSource newLight = new ClientGunFireLightSource(entity, lightLevel);
            activeLights.put(entity, newLight);
            DynamicLights.addLightSource(newLight);
        }
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
