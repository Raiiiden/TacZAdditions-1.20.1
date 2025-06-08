package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.client.sound.SoundSourceTracker;
import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Map;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {
    private static Field channelsField;
    private static boolean fieldSearched = false;

    private static Field findChannelsField() {
        if (fieldSearched) return channelsField;
        fieldSearched = true;

        try {
            // Common field names across different versions/mappings
            String[] possibleNames = {
                    "instanceToChannel", "soundChannels", "channels", "channelAccess",
                    "f_120351_", "f_120352_", "playingSounds"
            };

            Class<?> soundEngineClass = SoundEngine.class;

            for (String name : possibleNames) {
                try {
                    Field field = soundEngineClass.getDeclaredField(name);
                    field.setAccessible(true);
                    if (Map.class.isAssignableFrom(field.getType())) {
                        System.out.println("[SoundEngineMixin] Found channels field: " + name);
                        channelsField = field;
                        return field;
                    }
                } catch (NoSuchFieldException ignored) {
                }
            }

            // If named search fails, search by type
            Field[] fields = soundEngineClass.getDeclaredFields();
            System.out.println("[SoundEngineMixin] Searching through " + fields.length + " fields by type...");

            for (Field field : fields) {
                if (Map.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    System.out.println("[SoundEngineMixin] Found potential Map field: " + field.getName() + " of type " + field.getType());

                    // Try to access it to see if it's the right one
                    try {
                        Object testAccess = field.get(null); // This might fail, but that's okay
                        channelsField = field;
                        System.out.println("[SoundEngineMixin] Successfully set channels field to: " + field.getName());
                        return field;
                    } catch (Exception e) {
                        // This field might require an instance, continue searching
                        channelsField = field; // Set it anyway, we'll try with instance later
                        System.out.println("[SoundEngineMixin] Field " + field.getName() + " requires instance access");
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("[SoundEngineMixin] Error searching for channels field: " + e.getMessage());
            e.printStackTrace();
        }

        if (channelsField == null) {
            System.err.println("[SoundEngineMixin] Could not find channels field");
        }
        return channelsField;
    }

    @Inject(method = "play", at = @At("TAIL"))
    private void taczadditions$trackSourceId(SoundInstance instance, CallbackInfo ci) {
        System.out.println("[SoundEngineMixin] Attempting to track sound: " + instance.getLocation());

        try {
            Field field = findChannelsField();
            if (field == null) {
                System.err.println("[SoundEngineMixin] No channels field available");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<SoundInstance, ?> map = (Map<SoundInstance, ?>) field.get(this);

            if (map == null) {
                System.err.println("[SoundEngineMixin] Channels map is null");
                return;
            }

            System.out.println("[SoundEngineMixin] Channels map size: " + map.size());

            Object handle = map.get(instance);
            if (handle == null) {
                System.out.println("[SoundEngineMixin] No handle found for " + instance.getLocation());
                System.out.println("[SoundEngineMixin] Available keys in map:");
                for (SoundInstance key : map.keySet()) {
                    System.out.println("  - " + key.getLocation() + " (class: " + key.getClass().getSimpleName() + ")");
                }
                return;
            }

            System.out.println("[SoundEngineMixin] Found handle of type: " + handle.getClass().getName());

            // Try to find the channel field in the handle
            try {
                Field channelField = handle.getClass().getDeclaredField("channel");
                channelField.setAccessible(true);
                Channel channel = (Channel) channelField.get(handle);
                SoundSourceTracker.put(instance, channel);
                System.out.println("[SoundEngineMixin] Successfully tracked channel for " + instance.getLocation());
            } catch (NoSuchFieldException e) {
                // Try alternative field names
                String[] channelFieldNames = {"channel", "source", "audioChannel"};
                boolean found = false;

                for (String fieldName : channelFieldNames) {
                    try {
                        Field channelField = handle.getClass().getDeclaredField(fieldName);
                        channelField.setAccessible(true);
                        Object channelObj = channelField.get(handle);

                        if (channelObj instanceof Channel) {
                            Channel channel = (Channel) channelObj;
                            SoundSourceTracker.put(instance, channel);
                            System.out.println("[SoundEngineMixin] Successfully tracked channel via field: " + fieldName);
                            found = true;
                            break;
                        }
                    } catch (Exception ignored) {}
                }

                if (!found) {
                    System.err.println("[SoundEngineMixin] Could not find channel field in handle");
                    System.err.println("[SoundEngineMixin] Handle fields:");
                    for (Field f : handle.getClass().getDeclaredFields()) {
                        System.err.println("  - " + f.getName() + " : " + f.getType().getSimpleName());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("[SoundEngineMixin] Failed to track sound: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Inject(method = "stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"))
    private void taczadditions$cleanupTracking(SoundInstance instance, CallbackInfo ci) {
        SoundSourceTracker.remove(instance);
    }
}