package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.mixin.BedrockGunModelAccessor;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Keeps the render-time visibility of each gun's laser_beam bone path.
public final class LaserVisibilityCache {
    private static final Map<Integer, Entry> ENTITY_VISIBILITY = new HashMap<>();

    private static BedrockGunModel localModel;
    private static boolean localVisible = true;

    private LaserVisibilityCache() {}

    private record Entry(BedrockGunModel model, boolean visible) {}

    public static void captureLocal(BedrockGunModel model) {
        localModel = model;
        localVisible = isLaserPathVisible(model);
    }

    public static void captureEntity(int entityId, BedrockGunModel model) {
        ENTITY_VISIBILITY.put(entityId, new Entry(model, isLaserPathVisible(model)));
    }

    public static boolean isLocalVisible(ItemStack gunStack) {
        if (!ClientLaserToggleState.isLaserEnabled(gunStack)) return false;

        return TimelessAPI.getGunDisplay(gunStack)
                .map(display -> display.getGunModel() != localModel || localVisible)
                .orElse(true);
    }

    public static boolean isEntityVisible(int entityId, ItemStack gunStack) {
        if (!ClientLaserToggleState.isLaserEnabled(gunStack)) return false;

        Entry entry = ENTITY_VISIBILITY.get(entityId);
        if (entry == null) return true;

        return TimelessAPI.getGunDisplay(gunStack)
                .map(display -> display.getGunModel() != entry.model || entry.visible)
                .orElse(true);
    }

    public static void removeEntity(int entityId) {
        ENTITY_VISIBILITY.remove(entityId);
    }

    public static void clear() {
        ENTITY_VISIBILITY.clear();
        localModel = null;
        localVisible = true;
    }

    private static boolean isLaserPathVisible(BedrockGunModel model) {
        List<BedrockPart> path = ((BedrockGunModelAccessor) model).taczadditions$getLaserBeamPaths();

        // Models without a laser_beam bone retain the old attachment-based behavior.
        if (path == null || path.isEmpty()) return true;

        for (BedrockPart part : path) {
            if (!part.visible) return false;
        }
        return true;
    }
}
