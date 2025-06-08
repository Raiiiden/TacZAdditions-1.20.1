package com.raiiiden.taczadditions.client.sound;

import com.tacz.guns.client.sound.GunSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class FilteredGunSoundInstance extends GunSoundInstance {
    public final float muffleAmount;
    private boolean filterApplied = false;

    public FilteredGunSoundInstance(SoundEvent sound, SoundSource category, float volume, float pitch,
                                    Entity entity, int attenuationDistance, @Nullable ResourceLocation registryName, boolean mono,
                                    float muffleAmount) {
        super(sound, category, volume, pitch, entity, attenuationDistance, registryName, mono);
        this.muffleAmount = muffleAmount;
    }

    public float getMuffleAmount() {
        return muffleAmount;
    }

    public boolean isFilterApplied() {
        return this.filterApplied;
    }

    public void setFilterApplied(boolean applied) {
        this.filterApplied = applied;
    }
}