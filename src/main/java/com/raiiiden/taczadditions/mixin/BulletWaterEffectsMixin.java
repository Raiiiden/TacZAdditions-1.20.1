package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.server.BulletWaterEffects;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Runs the water effects from the bullet's own tick. A sweep over live entities cannot work here:
// TaCZ discards a bullet during the tick it hits something, so any shot that ended in a wall, an
// entity or the lake bed on the tick it was fired would already be gone by the end of that tick.
@Mixin(value = EntityKineticBullet.class, remap = false)
public abstract class BulletWaterEffectsMixin {

    @Inject(method = "tick()V", at = @At("TAIL"), remap = true)
    private void taczadditions$spawnWaterEffects(CallbackInfo ci) {
        EntityKineticBullet bullet = (EntityKineticBullet) (Object) this;
        if (!(bullet.level() instanceof ServerLevel)) return;
        BulletWaterEffects.onBulletTicked(bullet);
    }
}
