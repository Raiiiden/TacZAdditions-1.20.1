package com.raiiiden.taczadditions.mixin;

import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.sound.SoundManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Guns forced silenced through silencedGunIds go mute when their display JSON has no "silence"
// entry, because TaCZ drops the null lookup. Fall back to the unsilenced shot instead.
@Mixin(value = GunDisplayInstance.class, remap = false)
public class GunDisplaySilenceSoundFallbackMixin {
    // Mirrors TaCZ's own SoundPlayManager lister, which is private.
    private static final FileToIdConverter TACZADDITIONS$SOUND_LISTER =
            new FileToIdConverter("tacz_sounds", ".ogg");

    @Inject(method = "getSounds", at = @At("RETURN"), cancellable = true, remap = false)
    private void taczadditions$fallBackToShootSound(
            String name, CallbackInfoReturnable<ResourceLocation> cir) {
        String fallback;
        if (SoundManager.SILENCE_SOUND.equals(name)) {
            fallback = SoundManager.SHOOT_SOUND;
        } else if (SoundManager.SILENCE_3P_SOUND.equals(name)) {
            fallback = SoundManager.SHOOT_3P_SOUND;
        } else {
            return;
        }

        // The entry can also point at an ogg that the pack never shipped, which is just as silent.
        ResourceLocation silenceSound = cir.getReturnValue();
        if (silenceSound != null && taczadditions$soundExists(silenceSound)) return;

        // Re-entrant call resolves "shoot"/"shoot_3p", which this injection ignores.
        ResourceLocation shootSound = ((GunDisplayInstance) (Object) this).getSounds(fallback);
        if (shootSound != null) {
            cir.setReturnValue(shootSound);
        }
    }

    private static boolean taczadditions$soundExists(ResourceLocation sound) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return true;
        return minecraft.getResourceManager()
                .getResource(TACZADDITIONS$SOUND_LISTER.idToFile(sound))
                .isPresent();
    }
}