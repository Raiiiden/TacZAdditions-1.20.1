package com.raiiiden.taczadditions.registry;

import com.raiiiden.taczadditions.TaczAdditions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TaczAdditions.MODID);

    public static final RegistryObject<SoundEvent> LASER_CLICK = SOUND_EVENTS.register(
            "laserclick",
            () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(TaczAdditions.MODID, "laserclick")
            )
    );

    private ModSounds() {}
}
