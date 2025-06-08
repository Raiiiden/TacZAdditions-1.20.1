package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.client.sound.FilteredGunSoundInstance;
import com.raiiiden.taczadditions.client.sound.SoundFilterRegistry;
import com.tacz.guns.client.sound.GunSoundInstance;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.init.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundPlayManager.class)
public class SoundPlayManagerMixin {
    @Inject(
            method = "playClientSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/resources/ResourceLocation;FFIZ)Lcom/tacz/guns/client/sound/GunSoundInstance;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void taczadditions$injectFilteredGunSound(
            Entity entity, @Nullable ResourceLocation name, float volume, float pitch, int distance, boolean mono,
            CallbackInfoReturnable<GunSoundInstance> cir) {

        try {
            System.out.println("[SoundPlayManagerMixin] Playing sound: " + name);

            if (name != null && (
                    name.getPath().contains("fire") ||
                            name.getPath().contains("shoot") ||
                            name.getPath().contains("shot")
            )) {
                float muffle = 0.8f;

                FilteredGunSoundInstance filteredInstance = new FilteredGunSoundInstance(
                        ModSounds.GUN.get(),
                        SoundSource.PLAYERS,
                        volume,
                        pitch,
                        entity,
                        distance,
                        name,
                        mono,
                        muffle
                );

                SoundFilterRegistry.register(filteredInstance);
                Minecraft.getInstance().getSoundManager().play(filteredInstance);
                cir.setReturnValue(filteredInstance);
            }
        } catch (Exception ignored) {
            // Let TACZ handle fallback sound logic
        }
    }
}