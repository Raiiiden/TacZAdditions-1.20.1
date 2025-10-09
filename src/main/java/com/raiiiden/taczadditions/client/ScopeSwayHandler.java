package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = "taczadditions", value = Dist.CLIENT)
public class ScopeSwayHandler {

    private static float swayTimer = 0f;
    private static long crouchStartTime = 0;
    private static long crouchCooldownEnd = 0;
    private static boolean wasAiming = false;
    private static final Random rand = new Random();

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!TacZAdditionsConfig.CLIENT.enableScopeSway.get()) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack main = player.getMainHandItem();
        if (!(main.getItem() instanceof IGun iGun)) return;

        float zoom = iGun.getAimingZoom(main);
        float minZoom = TacZAdditionsConfig.CLIENT.scopeSwayMinZoom.get().floatValue();
        if (zoom < minZoom) return;

        IClientPlayerGunOperator op = IClientPlayerGunOperator.fromLocalPlayer(player);
        float aim = op.getClientAimingProgress(Minecraft.getInstance().getFrameTime());

        boolean isAiming = aim >= 0.95f;

        // Reset crouchStartTime when player stops aiming (unscopes)
        if (!isAiming && wasAiming) {
            // Player just unscoped, but don't reset crouchStartTime or cooldown
            // This prevents cheating by unscoping and rescoping
        }

        wasAiming = isAiming;

        if (!isAiming) return;

        float baseStrength = TacZAdditionsConfig.CLIENT.scopeSwayStrength.get().floatValue();
        float baseSpeed = TacZAdditionsConfig.CLIENT.scopeSwaySpeed.get().floatValue();

        float delta = Minecraft.getInstance().getDeltaFrameTime();
        swayTimer += delta;

        long now = System.currentTimeMillis();
        boolean crouching = player.isCrouching();

        float swayMult = 1f;
        float speedMult = 1f;

        long stabilizeMs = TacZAdditionsConfig.CLIENT.crouchStabilizeTime.get().longValue();
        long sporadicMs = TacZAdditionsConfig.CLIENT.crouchSporadicTime.get().longValue();
        long cooldownMs = TacZAdditionsConfig.CLIENT.crouchCooldownTime.get().longValue();

        double sporadicStrength = TacZAdditionsConfig.CLIENT.sporadicSwayStrength.get();
        double sporadicSpeed = TacZAdditionsConfig.CLIENT.sporadicSwaySpeed.get();

        // Check if we're in cooldown period
        if (now < crouchCooldownEnd) {
            // Still in cooldown, use normal sway
            crouching = false; // Override crouch check during cooldown
        }

        if (crouching) {
            if (crouchStartTime == 0) {
                // Just started crouching
                crouchStartTime = now;
            }

            long crouchDuration = now - crouchStartTime;

            if (crouchDuration <= stabilizeMs) {
                // Stabilization phase
                swayMult = 0.25f;
            } else if (crouchDuration <= stabilizeMs + sporadicMs) {
                // Sporadic phase
                swayMult = (float) sporadicStrength;
                speedMult = (float) sporadicSpeed;
            } else {
                // Exceeded max hold time, enter cooldown
                crouchCooldownEnd = now + cooldownMs;
                crouchStartTime = 0;
            }
        } else {
            // Not crouching and not in cooldown - reset everything
            if (now >= crouchCooldownEnd) {
                crouchStartTime = 0;
            }
        }

        float time = (swayTimer / (baseSpeed * speedMult)) * (float) Math.PI * 2f;

        float pitch = (float) Math.sin(time * 0.7f) * baseStrength * swayMult;
        float yaw   = (float) Math.sin(time * 0.45f + 1.3f) * baseStrength * swayMult;

        player.setXRot(player.getXRot() + pitch);
        player.setYRot(player.getYRot() + yaw);
    }
}