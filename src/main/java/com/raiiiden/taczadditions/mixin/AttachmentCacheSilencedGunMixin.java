package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.tacz.guns.api.GunProperties;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Forces TaCZ's silence property for configured integrated-suppressor guns.
@Mixin(value = AttachmentCacheProperty.class, remap = false)
public class AttachmentCacheSilencedGunMixin {
    @Inject(method = "eval", at = @At("RETURN"), remap = false)
    private void taczadditions$forceConfiguredGunSilenced(
            ItemStack gunStack, GunData gunData, CallbackInfo ci) {
        if (gunStack.isEmpty() || !(gunStack.getItem() instanceof IGun gun)) return;
        if (!TacZAdditionsConfig.COMMON.silencedGunIds.get()
                .contains(gun.getGunId(gunStack).toString())) {
            return;
        }

        AttachmentCacheProperty cache = (AttachmentCacheProperty) (Object) this;
        Pair<Integer, Boolean> silence = cache.getCache(GunProperties.SILENCE);
        if (silence != null && !silence.right()) {
            // Preserve TaCZ's computed audible distance and only force its silenced-sound flag.
            cache.setCache(GunProperties.SILENCE, Pair.of(silence.left(), true));
        }
    }
}
