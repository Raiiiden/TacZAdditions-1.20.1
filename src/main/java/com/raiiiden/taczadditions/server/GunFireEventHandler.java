package com.raiiiden.taczadditions.server;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class GunFireEventHandler {

    @SubscribeEvent
    public static void onGunFire(GunFireEvent event) {
        if (!TacZAdditionsConfig.SERVER.enableMuzzleFlash.get()) return;
        if (!(event.getShooter().level() instanceof ServerLevel serverLevel)) return;

        LivingEntity shooter = event.getShooter();
        ItemStack gun = event.getGunItemStack();

        int lightLevel = isSilenced(gun) ? 6 : 15;

        Vec3 muzzleVec = shooter.getEyePosition().add(shooter.getLookAngle().scale(1.0));
        BlockPos muzzlePos = BlockPos.containing(muzzleVec);

        ServerMuzzleFlashManager.placeFlash(serverLevel, muzzlePos, lightLevel);
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
