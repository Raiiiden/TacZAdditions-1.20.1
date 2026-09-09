package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.TaczAdditions;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

// Reflection adapter for the optional AtomicStryker Dynamic Lights API.
public final class ClientGunFireLightManager {
    private static final String DYNAMIC_LIGHTS_CLASS =
            "atomicstryker.dynamiclights.server.DynamicLights";
    private static final String LIGHT_SOURCE_INTERFACE =
            "atomicstryker.dynamiclights.server.IDynamicLightSource";

    private static final Map<Entity, LightState> activeLights = new HashMap<>();
    private static Method addLightSource;
    private static Class<?> lightSourceInterface;
    private static boolean available;

    private ClientGunFireLightManager() {
    }

    public static void init() {
        try {
            ClassLoader loader = ClientGunFireLightManager.class.getClassLoader();
            Class<?> dynamicLights = Class.forName(DYNAMIC_LIGHTS_CLASS, false, loader);
            lightSourceInterface = Class.forName(LIGHT_SOURCE_INTERFACE, false, loader);
            addLightSource = dynamicLights.getMethod("addLightSource", lightSourceInterface);
            available = true;
            MinecraftForge.EVENT_BUS.register(ClientGunFireLightManager.class);
            TaczAdditions.LOGGER.info(
                    "[TacZAdditions] Atomic Dynamic Lights reflection adapter initialized");
        } catch (ReflectiveOperationException | LinkageError error) {
            available = false;
            TaczAdditions.LOGGER.warn(
                    "[TacZAdditions] Dynamic Lights was detected but its API is unavailable; "
                            + "muzzle flash dynamic lights are disabled",
                    error);
        }
    }

    public static void addLight(Entity entity, int lightLevel) {
        if (!available) return;

        LightState existing = activeLights.get(entity);
        if (existing != null) {
            existing.updateLight(lightLevel);
            return;
        }

        LightState newLight = new LightState(entity, lightLevel);
        Object proxy = Proxy.newProxyInstance(
                lightSourceInterface.getClassLoader(),
                new Class<?>[]{lightSourceInterface},
                (proxyInstance, method, args) -> switch (method.getName()) {
                    case "getAttachmentEntity" -> newLight.entity;
                    case "getLightLevel" -> newLight.getLightLevel();
                    case "toString" -> "TacZAdditionsDynamicLight[" + newLight.entity + "]";
                    case "hashCode" -> System.identityHashCode(proxyInstance);
                    case "equals" -> proxyInstance == (args == null ? null : args[0]);
                    default -> throw new UnsupportedOperationException(method.toString());
                });

        try {
            addLightSource.invoke(null, proxy);
            activeLights.put(entity, newLight);
        } catch (ReflectiveOperationException | LinkageError error) {
            available = false;
            TaczAdditions.LOGGER.warn(
                    "[TacZAdditions] Could not register a reflected Dynamic Lights source; "
                            + "disabling the adapter",
                    error);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (LightState light : activeLights.values()) {
            light.tick();
        }
        // Let dead-entity lights reach zero before dropping their state.
        activeLights.entrySet().removeIf(
                entry -> !entry.getKey().isAlive() && entry.getValue().isExpired());
    }

    private static final class LightState {
        private static final int LIGHT_DURATION_TICKS = 2;

        private final Entity entity;
        private int lightLevel;
        private int ticksAlive;

        private LightState(Entity entity, int lightLevel) {
            this.entity = entity;
            updateLight(lightLevel);
        }

        private void updateLight(int newLightLevel) {
            lightLevel = newLightLevel;
            ticksAlive = 0;
        }

        private void tick() {
            ticksAlive++;
        }

        private int getLightLevel() {
            return ticksAlive < LIGHT_DURATION_TICKS ? lightLevel : 0;
        }

        private boolean isExpired() {
            return ticksAlive >= LIGHT_DURATION_TICKS;
        }
    }
}
