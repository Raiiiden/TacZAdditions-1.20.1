package com.raiiiden.taczadditions.mixin;

import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = BedrockGunModel.class, remap = false)
public interface BedrockGunModelAccessor {
    @Accessor("laserBeamPaths")
    List<BedrockPart> taczadditions$getLaserBeamPaths();
}
