package com.raiiiden.taczadditions.client.sound;

import com.tacz.guns.client.sound.GunSoundInstance;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SoundFilterRegistry {
    private static final List<FilteredGunSoundInstance> trackedSounds = new CopyOnWriteArrayList<>();

    public static void register(FilteredGunSoundInstance sound) {
        trackedSounds.add(sound);
    }

    public static List<FilteredGunSoundInstance> getAll() {
        return Collections.unmodifiableList(trackedSounds);
    }

    public static void remove(FilteredGunSoundInstance sound) {
        trackedSounds.remove(sound);
    }

    public static void clearStopped() {
        trackedSounds.removeIf(GunSoundInstance::isStopped);
    }
}