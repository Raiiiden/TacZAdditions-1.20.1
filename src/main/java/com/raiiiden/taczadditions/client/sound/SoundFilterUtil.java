package com.raiiiden.taczadditions.client.sound;

import net.minecraft.client.resources.sounds.SoundInstance;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.EXTEfx;
import org.lwjgl.openal.AL11;

public class SoundFilterUtil {
    public static boolean applyLowPassFilter(SoundInstance instance, float muffleAmount) {
        int source = SoundSourceTracker.get(instance);
        if (source == -1) {
            System.out.println("[SoundFilter] No OpenAL source found for instance: " + instance.getLocation());
            return false;
        }

        // Check if source is valid and playing
        int sourceState = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
        if (sourceState != AL10.AL_PLAYING && sourceState != AL10.AL_PAUSED) {
            //System.out.println("[SoundFilter] Source " + source + " is not playing (state: " + sourceState + ")");
            return false;
        }

        try {
            // low-pass filter
            int filter = EXTEfx.alGenFilters();
            if (filter == 0) {
                System.err.println("[SoundFilter] Failed to generate filter");
                return false;
            }

            EXTEfx.alFilteri(filter, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);

            // aggressive filtering settings
            float gain = Math.max(0.1f, 1.0f - muffleAmount * 0.8f);
            float gainHF = Math.max(0.1f, 1.0f - muffleAmount * 0.9f);

            EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAIN, gain);
            EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAINHF, gainHF);

            // Apply the filter to the source
            AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, filter);

            // Check for OpenAL errors
            int error = AL10.alGetError();
            if (error != AL10.AL_NO_ERROR) {
                System.err.println("[SoundFilter] OpenAL error when applying filter: " + error);
                EXTEfx.alDeleteFilters(filter);
                return false;
            }

            System.out.println("[SoundFilter] Successfully applied filter " + filter +
                    " to source " + source +
                    " | GAIN=" + gain +
                    " | GAINHF=" + gainHF +
                    " | Sound=" + instance.getLocation());

            return true;

        } catch (Exception e) {
            System.err.println("[SoundFilter] Exception while applying filter: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}