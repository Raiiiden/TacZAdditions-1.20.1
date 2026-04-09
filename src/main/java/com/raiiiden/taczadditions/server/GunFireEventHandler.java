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

    /**
     * Detected once at class-load time via Forge's ModList (no reflection needed).
     *
     * Priority order:
     *  1. sodiumdynamiclights — client-side dynamic lights shared via packet; no server blocks.
     *  2. dynamiclights (Atomicstryker) — server-side entity lighting; registered separately.
     *  3. Neither — fall back to placing a transient light block via ServerMuzzleFlashManager.
     */
    private static final boolean HAS_SODIUM_DL    = ModList.get().isLoaded("sodiumdynamiclights");
    private static final boolean HAS_ATOMIC_DL    = ModList.get().isLoaded("dynamiclights");

    @SubscribeEvent
    public static void onGunFire(GunFireEvent event) {
        if (!TacZAdditionsConfig.SERVER.enableMuzzleFlash.get()) return;
        if (!(event.getShooter().level() instanceof ServerLevel serverLevel)) return;

        LivingEntity shooter = event.getShooter();
        ItemStack gun = event.getGunItemStack();
        if (gun.getOrCreateTag().getByte("Jammed") != 0) return;

        int lightLevel = isSilenced(gun) ? 6 : 15;

        if (HAS_SODIUM_DL) {
            // Send packet — each nearby client will create its own client-side dynamic light.
            // GunFireLightManager (Atomicstryker) is intentionally NOT called here; both
            // approaches would double-apply lighting.
            ModNetworking.sendMuzzleFlash(shooter, lightLevel);

        } else if (HAS_ATOMIC_DL) {
            // GunFireLightManager is registered on the Forge bus only when this mod is
            // present (see TaczAdditions.commonSetup), so it's safe to call here.
            GunFireLightManager.addLight(shooter, lightLevel);

        } else {
            // No dynamic-light mod — place a transient light block at the muzzle position.
            BlockPos muzzlePos = BlockPos.containing(shooter.getEyePosition());
            ServerMuzzleFlashManager.placeFlash(serverLevel, muzzlePos, lightLevel);
        }
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
