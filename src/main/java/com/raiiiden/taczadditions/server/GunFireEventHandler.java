package com.raiiiden.taczadditions.server;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.raiiiden.taczadditions.network.ModNetworking;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class GunFireEventHandler {

    private static final boolean HAS_SODIUM_DL = ModList.get().isLoaded("sodiumdynamiclights");
    private static final boolean HAS_ATOMIC_DL = ModList.get().isLoaded("dynamiclights");

    @SubscribeEvent
    public static void onGunFire(GunFireEvent event) {
        if (!TacZAdditionsConfig.COMMON.enableMuzzleFlash.get()) return;
        if (!(event.getShooter().level() instanceof ServerLevel serverLevel)) return;

        LivingEntity shooter = event.getShooter();
        ItemStack gun = event.getGunItemStack();
        if (gun.getOrCreateTag().getByte("Jammed") != 0) return;
        if (isMuzzleFlashBlacklisted(gun)) return;

        int lightLevel = isSilenced(gun) ? 6 : 15;
        int color = getMuzzleFlashColor(gun);

        boolean useBlockLight = shouldUseBlockLight(gun);

        if (!useBlockLight && (HAS_SODIUM_DL || HAS_ATOMIC_DL)) {
            ModNetworking.sendMuzzleFlash(shooter, lightLevel, color);
        } else {
            // Send color-only metadata so Colorful Lighting can tint the temporary light block.
            if (color >= 0) {
                ModNetworking.sendMuzzleFlash(shooter, 0, color);
            }
            BlockPos muzzlePos = BlockPos.containing(shooter.getEyePosition());
            ServerMuzzleFlashManager.placeFlash(serverLevel, muzzlePos, lightLevel);
        }
    }

    private static boolean isMuzzleFlashBlacklisted(ItemStack gun) {
        if (gun.isEmpty() || !(gun.getItem() instanceof IGun igun)) return false;
        String gunId = igun.getGunId(gun).toString();
        return TacZAdditionsConfig.COMMON.muzzleFlashWeaponBlacklist.get().contains(gunId);
    }

    private static boolean shouldUseBlockLight(ItemStack gun) {
        if (!TacZAdditionsConfig.COMMON.forceBlockLightForFastGuns.get()) return false;
        if (!(gun.getItem() instanceof IGun igun)) return false;
        int rpm = TimelessAPI.getCommonGunIndex(igun.getGunId(gun))
                .map(index -> index.getGunData().getRoundsPerMinute())
                .orElse(0);
        return rpm >= TacZAdditionsConfig.COMMON.fastGunRpmThreshold.get();
    }

    private static int getMuzzleFlashColor(ItemStack gun) {
        if (gun.isEmpty() || !(gun.getItem() instanceof IGun igun)) return -1;
        String gunId = igun.getGunId(gun).toString();
        for (String entry : TacZAdditionsConfig.COMMON.coloredMuzzleFlashGunColors.get()) {
            int separator = entry.lastIndexOf('=');
            if (separator <= 0 || !entry.substring(0, separator).trim().equals(gunId)) continue;
            try {
                return Integer.parseInt(entry.substring(separator + 1).trim().substring(1), 16);
            } catch (RuntimeException ignored) {
                // Forge validates configured entries; tolerate a malformed synchronized value.
                return -1;
            }
        }
        return -1;
    }

    private static boolean isSilenced(ItemStack gun) {
        if (gun.isEmpty() || !(gun.getItem() instanceof IGun igun)) return false;
        ResourceLocation gunId = igun.getGunId(gun);
        var gunIndexOpt = TimelessAPI.getCommonGunIndex(gunId);
        if (gunIndexOpt.isEmpty()) return false;
        var gunData = gunIndexOpt.get().getGunData();
        AttachmentCacheProperty cache = new AttachmentCacheProperty();
        cache.eval(gun, gunData);
        Object silenceData = cache.getCache(SilenceModifier.ID);
        return silenceData instanceof Pair<?, ?> pair && pair.right() instanceof Boolean b && b;
    }
}
