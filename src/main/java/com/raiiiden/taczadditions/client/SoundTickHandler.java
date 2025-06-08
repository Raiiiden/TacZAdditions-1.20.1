package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.client.sound.FilteredGunSoundInstance;
import com.raiiiden.taczadditions.client.sound.SoundFilterUtil;
import com.raiiiden.taczadditions.client.sound.SoundFilterRegistry;
import com.raiiiden.taczadditions.client.sound.SoundSourceTracker;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class SoundTickHandler {
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;

        List<FilteredGunSoundInstance> currentTrackedSounds = SoundFilterRegistry.getAll();
        for (FilteredGunSoundInstance sound : currentTrackedSounds) {
            if (sound.isFilterApplied()) {
                continue;
            }

            int source = SoundSourceTracker.get(sound);
            if (source == -1) {
                if (tickCounter % 20 == 0) {
                    //System.out.println("[SoundTickHandler] Still waiting for source for " + sound.getLocation());
                }
                continue;
            }

            boolean success = SoundFilterUtil.applyLowPassFilter(sound, sound.getMuffleAmount());
            if (success) {
                sound.setFilterApplied(true);
            }
        }

        if (tickCounter % 20 == 0) {
            SoundFilterRegistry.clearStopped();
        }
    }
}