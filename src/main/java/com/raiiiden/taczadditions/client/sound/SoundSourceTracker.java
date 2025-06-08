package com.raiiiden.taczadditions.client.sound;

import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.resources.sounds.SoundInstance;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SoundSourceTracker {
    private static final Map<SoundInstance, Integer> sourceMap = new ConcurrentHashMap<>();
    private static final Field sourceField;

    static {
        Field field;
        try {
            field = Channel.class.getDeclaredField("source");
            field.setAccessible(true);
            System.out.println("[SoundSourceTracker] Successfully accessed Channel.source field");
        } catch (Exception e) {
            field = null;
            System.err.println("[SoundSourceTracker] Failed to access Channel.source field: " + e.getMessage());
            e.printStackTrace();
        }
        sourceField = field;
    }

    public static void put(SoundInstance instance, Channel channel) {
        if (sourceField == null) return;

        try {
            int sourceId = (int) sourceField.get(channel);
            sourceMap.put(instance, sourceId);
            System.out.println("[SoundSourceTracker] Tracked source " + sourceId + " for sound: " + instance.getLocation());
        } catch (Exception e) {
            System.err.println("[SoundSourceTracker] Failed to get source ID: " + e.getMessage());
        }
    }

    public static int get(SoundInstance instance) {
        Integer sourceId = sourceMap.get(instance);
        if (sourceId == null) {
            return -1;
        }
        return sourceId;
    }

    public static void remove(SoundInstance instance) {
        Integer removed = sourceMap.remove(instance);
        if (removed != null) {
            System.out.println("[SoundSourceTracker] Removed tracking for source " + removed);
        }
    }

    public static void cleanup() {
        sourceMap.clear();
    }
}