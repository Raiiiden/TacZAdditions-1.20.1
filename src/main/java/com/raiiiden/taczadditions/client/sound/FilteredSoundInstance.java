package com.raiiiden.taczadditions.client.sound;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class FilteredSoundInstance extends AbstractSoundInstance {
    public final float muffleAmount;
    private boolean manuallyStopped = false;
    private boolean filterApplied = false;

    public FilteredSoundInstance(SoundEvent sound, SoundSource source, float volume, float pitch, double x, double y, double z, float muffleAmount) {
        super(sound, source, RandomSource.create());
        this.volume = volume;
        this.pitch = pitch;
        this.x = x;
        this.y = y;
        this.z = z;
        this.muffleAmount = muffleAmount;
        this.looping = false;
        this.delay = 0;
        this.relative = false;

        System.out.println("[FilteredSoundInstance] Created with muffle: " + muffleAmount + " for sound: " + sound.getLocation());
    }

    public void stopManually() {
        this.manuallyStopped = true;
    }

    public boolean isStoppedManually() {
        return this.manuallyStopped;
    }

    public boolean isFilterApplied() {
        return this.filterApplied;
    }

    public void setFilterApplied(boolean applied) {
        this.filterApplied = applied;
    }
}