package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.client.sound.SoundSourceTracker;
import com.tacz.guns.client.sound.GunSoundInstance;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractTickableSoundInstance.class)
public abstract class GunSoundInstanceStopPatch {
    @Inject(method = "isStopped", at = @At("HEAD"), cancellable = true)
    private void taczadditions$overrideIfGunInstance(CallbackInfoReturnable<Boolean> cir) {
        SoundInstance self = (SoundInstance) (Object) this;

        if (self instanceof GunSoundInstance) {
            int source = SoundSourceTracker.get(self);
            if (source > 0) {
                int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
                if (state == AL10.AL_STOPPED) {
                    cir.setReturnValue(true);
                }
            }
        }
    }
}
