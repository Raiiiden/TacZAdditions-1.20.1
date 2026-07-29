package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.raiiiden.taczadditions.util.CurrentGunStack;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.pojo.data.gun.GunRecoil;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GunRecoil.class)
public class GunRecoilMixin {

    @ModifyVariable(method = "genPitchSplineFunction", at = @At("HEAD"), ordinal = 0, argsOnly = true, remap = false)
    private float applyVerticalMultiplier(float modifier) {
        IGun gun = IGun.getIGunOrNull(CurrentGunStack.get());
        GunTabType type = TimelessAPI.getCommonGunIndex(gun.getGunId(CurrentGunStack.get()))
                .map(index -> mapStringToGunTabType(index.getType()))
                .orElse(GunTabType.RIFLE);
        return modifier * getVerticalMultiplier(type);
    }

    @ModifyVariable(method = "genYawSplineFunction", at = @At("HEAD"), ordinal = 0, argsOnly = true, remap = false)
    private float applyHorizontalMultiplier(float modifier) {
        IGun gun = IGun.getIGunOrNull(CurrentGunStack.get());
        GunTabType type = TimelessAPI.getCommonGunIndex(gun.getGunId(CurrentGunStack.get()))
                .map(index -> mapStringToGunTabType(index.getType()))
                .orElse(GunTabType.RIFLE);
        return modifier * getHorizontalMultiplier(type);
    }

    private GunTabType mapStringToGunTabType(String type) {
        return switch (type.toLowerCase()) {
            case "pistol" -> GunTabType.PISTOL;
            case "rifle" -> GunTabType.RIFLE;
            case "sniper" -> GunTabType.SNIPER;
            case "smg" -> GunTabType.SMG;
            case "shotgun" -> GunTabType.SHOTGUN;
            case "rpg" -> GunTabType.RPG;
            case "mg" -> GunTabType.MG;
            default -> GunTabType.RIFLE;
        };
    }

    private float getVerticalMultiplier(GunTabType type) {
        return switch (type) {
            case PISTOL -> TacZAdditionsConfig.COMMON.recoilPistolVertical.get().floatValue();
            case RIFLE -> TacZAdditionsConfig.COMMON.recoilRifleVertical.get().floatValue();
            case SNIPER -> TacZAdditionsConfig.COMMON.recoilSniperVertical.get().floatValue();
            case SMG -> TacZAdditionsConfig.COMMON.recoilSMGVertical.get().floatValue();
            case SHOTGUN -> TacZAdditionsConfig.COMMON.recoilShotgunVertical.get().floatValue();
            case RPG -> TacZAdditionsConfig.COMMON.recoilRPGVertical.get().floatValue();
            case MG -> TacZAdditionsConfig.COMMON.recoilMGVertical.get().floatValue();
        };
    }

    private float getHorizontalMultiplier(GunTabType type) {
        return switch (type) {
            case PISTOL -> TacZAdditionsConfig.COMMON.recoilPistolHorizontal.get().floatValue();
            case RIFLE -> TacZAdditionsConfig.COMMON.recoilRifleHorizontal.get().floatValue();
            case SNIPER -> TacZAdditionsConfig.COMMON.recoilSniperHorizontal.get().floatValue();
            case SMG -> TacZAdditionsConfig.COMMON.recoilSMGHorizontal.get().floatValue();
            case SHOTGUN -> TacZAdditionsConfig.COMMON.recoilShotgunHorizontal.get().floatValue();
            case RPG -> TacZAdditionsConfig.COMMON.recoilRPGHorizontal.get().floatValue();
            case MG -> TacZAdditionsConfig.COMMON.recoilMGHorizontal.get().floatValue();
        };
    }
}
