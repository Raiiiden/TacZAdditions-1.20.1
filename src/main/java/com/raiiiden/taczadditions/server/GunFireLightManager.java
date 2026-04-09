package com.raiiiden.taczadditions.server;

import atomicstryker.dynamiclights.server.DynamicLights;
import net.minecraft.world.entity.Entity;
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
        } else {
            GunFireLightSource newLight = new GunFireLightSource(entity, lightLevel);
            activeLights.put(entity, newLight);
            DynamicLights.addLightSource(newLight);
        }
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
