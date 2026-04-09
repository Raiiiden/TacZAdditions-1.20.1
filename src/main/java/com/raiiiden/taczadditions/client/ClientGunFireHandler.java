package com.raiiiden.taczadditions.client;

import it.unimi.dsi.fastutil.Pair;
import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientGunFireHandler {

    private static final boolean USE_ATOMIC_DL =
            ModList.get().isLoaded("dynamiclights") &&
            !ModList.get().isLoaded("sodiumdynamiclights");
    private static final boolean USE_SODIUM_DL =
            ModList.get().isLoaded("sodiumdynamiclights");

    @SubscribeEvent
    public static void onGunFire(GunFireEvent event) {
        if (!(event.getShooter() instanceof LocalPlayer)) return;

        float x = TacZAdditionsConfig.CLIENT.recoilVisualX.get().floatValue();
        float y = TacZAdditionsConfig.CLIENT.recoilVisualY.get().floatValue();
        float z = TacZAdditionsConfig.CLIENT.recoilVisualZ.get().floatValue();

        float xActual = (float) ((Math.random() * 2.0 - 1.0) * x);

        GunRecoilHandler.trigger(xActual, y, z);

        if (TacZAdditionsConfig.SERVER.enableMuzzleFlash.get()) {
            ItemStack gun = event.getGunItemStack();
            int lightLevel = isSilenced(gun) ? 6 : 15;

            if (USE_SODIUM_DL) {
                com.raiiiden.taczadditions.TaczAdditions.LOGGER.info(
                        "[TacZAdditions] ClientGunFireHandler → SodiumDL addFlash, level={}", lightLevel);
                SodiumDLAdapter.addFlash(event.getShooter(), lightLevel);
            } else if (USE_ATOMIC_DL) {
                ClientGunFireLightManager.addLight(event.getShooter(), lightLevel);
            }
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
