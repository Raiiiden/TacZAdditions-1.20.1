package com.raiiiden.taczadditions.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TacZAdditionsConfig {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final Server SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        Pair<Server, ForgeConfigSpec> serverConfig = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER_SPEC = serverConfig.getRight();
        SERVER = serverConfig.getLeft();

        Pair<Client, ForgeConfigSpec> clientConfig = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = clientConfig.getRight();
        CLIENT = clientConfig.getLeft();
    }

    public static class Server {
        public final ForgeConfigSpec.BooleanValue enableMuzzleFlash;

        public Server(ForgeConfigSpec.Builder builder) {
            builder.comment("TacZ Additions - Server Config").push("server");

            enableMuzzleFlash = builder
                    .comment("If false, muzzle flash will not be sent to clients.")
                    .define("enableMuzzleFlash", true);

            builder.pop();
        }
    }

    public static class Client {
        public final ForgeConfigSpec.BooleanValue enableMuzzleFlash;
        public final ForgeConfigSpec.BooleanValue enableRecoilRecovery;
        public final ForgeConfigSpec.BooleanValue enableGunMovement;
        public final ForgeConfigSpec.BooleanValue enableStrafeMovement;
        public final ForgeConfigSpec.BooleanValue enableScopeSway;

        // Strafing - Hipfire
        public final ForgeConfigSpec.DoubleValue strafeYawMultiplier;
        public final ForgeConfigSpec.DoubleValue strafeRollMultiplier;

        // Strafing - Aiming
        public final ForgeConfigSpec.DoubleValue aimStrafeYawMultiplier;
        public final ForgeConfigSpec.DoubleValue aimStrafeRollMultiplier;

        // Strafing - Limits
        public final ForgeConfigSpec.DoubleValue maxStrafeYaw;
        public final ForgeConfigSpec.DoubleValue maxStrafeRoll;
        public final ForgeConfigSpec.DoubleValue strafeSmoothing;

        // Scope Sway
        public final ForgeConfigSpec.DoubleValue scopeSwayStrength;
        public final ForgeConfigSpec.DoubleValue scopeSwaySpeed;
        public final ForgeConfigSpec.DoubleValue scopeSwayMinZoom;

        public final ForgeConfigSpec.DoubleValue crouchStabilizeTime;
        public final ForgeConfigSpec.DoubleValue crouchSporadicTime;
        public final ForgeConfigSpec.DoubleValue crouchCooldownTime;

        public final ForgeConfigSpec.DoubleValue sporadicSwayStrength;
        public final ForgeConfigSpec.DoubleValue sporadicSwaySpeed;

        // Hipfire
        public final ForgeConfigSpec.DoubleValue hipfireYawMultiplier;
        public final ForgeConfigSpec.DoubleValue hipfirePitchMultiplier;
        public final ForgeConfigSpec.DoubleValue hipfireRollFactor;

        // Aiming
        public final ForgeConfigSpec.DoubleValue aimingYawMultiplier;
        public final ForgeConfigSpec.DoubleValue aimingRollFactor;

        // Recoil
        public final ForgeConfigSpec.DoubleValue recoilVisualX;
        public final ForgeConfigSpec.DoubleValue recoilVisualY;
        public final ForgeConfigSpec.DoubleValue recoilVisualZ;
        public final ForgeConfigSpec.DoubleValue recoilCameraMultiplier;

        // Misc
        public final ForgeConfigSpec.DoubleValue dragSmoothing;
        public final ForgeConfigSpec.DoubleValue decayFactor;
        public final ForgeConfigSpec.DoubleValue momentumFactor;
        public final ForgeConfigSpec.DoubleValue rollSensitivity;
        public final ForgeConfigSpec.DoubleValue maxTiltAngle;

        public Client(ForgeConfigSpec.Builder builder) {
            builder.comment("TacZ Additions - Client Config").push("client");

            enableMuzzleFlash = builder
                    .comment("If true, enables muzzle flash light rendering.")
                    .define("enableMuzzleFlash", true);

            enableRecoilRecovery = builder
                    .comment("If true, recoil recovery is enabled (Default in TaCZ), disable to make recoil harder to control.")
                    .define("enableRecoilRecovery", true);

            enableGunMovement = builder
                    .comment("If false, disables all gun movement (sway, roll, etc).")
                    .define("enableGunMovement", true);

            enableStrafeMovement = builder
                    .comment("If false, disables sway/roll from strafing movement.")
                    .define("enableStrafeMovement", true);

            enableScopeSway = builder
                    .comment("Enable subtle camera sway when aiming with high-magnification scopes (4x+)")
                    .define("enableScopeSway", true);

            builder.push("hipfire");
            hipfireYawMultiplier = builder
                    .comment("Yaw multiplier when hip-firing.")
                    .defineInRange("yawMultiplier", 1.25, 0.0, 10.0);
            hipfirePitchMultiplier = builder
                    .comment("Pitch multiplier when hip-firing.")
                    .defineInRange("pitchMultiplier", 1.2, 0.0, 10.0);
            hipfireRollFactor = builder
                    .comment("Roll factor when hip-firing.")
                    .defineInRange("rollFactor", 2.75, 0.0, 10.0);
            builder.pop();

            builder.push("aim");
            aimingYawMultiplier = builder
                    .comment("Yaw multiplier when aiming.")
                    .defineInRange("yawMultiplier", 0.6, 0.0, 10.0);
            aimingRollFactor = builder
                    .comment("Roll factor when aiming.")
                    .defineInRange("rollFactor", 2.75, 0.0, 10.0);
            builder.pop();

            builder.push("strafe");
            strafeYawMultiplier = builder
                    .comment("Yaw sway from strafing while hip-firing.")
                    .defineInRange("hipfireYawMultiplier", 0.0, 0.0, 40.0);
            strafeRollMultiplier = builder
                    .comment("Roll tilt from strafing while hip-firing.")
                    .defineInRange("hipfireRollMultiplier", 20.0, 0.0, 40.0);
            aimStrafeYawMultiplier = builder
                    .comment("Yaw sway from strafing while aiming.")
                    .defineInRange("aimingYawMultiplier", 0.0, 0.0, 40.0);
            aimStrafeRollMultiplier = builder
                    .comment("Roll tilt from strafing while aiming.")
                    .defineInRange("aimingRollMultiplier", 20.0, 0.0, 40.0);
            maxStrafeYaw = builder
                    .comment("Maximum yaw offset from strafing (degrees)")
                    .defineInRange("maxStrafeYaw", 6.0, 0.0, 20.0);
            maxStrafeRoll = builder
                    .comment("Maximum roll angle from strafing (degrees)")
                    .defineInRange("maxStrafeRoll", 20.0, 0.0, 40.0);
            strafeSmoothing = builder
                    .comment("Smoothing factor for strafe movement (higher = more responsive, lower = smoother)")
                    .defineInRange("strafeSmoothing", 0.15, 0.01, 1.0);
            builder.pop();

            builder.push("recoil");
            recoilVisualX = builder
                    .comment("Visual recoil X multiplier (left-right shake)")
                    .defineInRange("visualX", 0.0, 0.0, 10.0);
            recoilVisualY = builder
                    .comment("Visual recoil Y multiplier (vertical bounce)")
                    .defineInRange("visualY", 1.0, 0.0, 10.0);
            recoilVisualZ = builder
                    .comment("Visual recoil Z multiplier (kickback)")
                    .defineInRange("visualZ", 1.0, 0.0, 10.0);
            recoilCameraMultiplier = builder
                    .comment("Camera kick recoil multiplier (pitch/yaw)")
                    .defineInRange("cameraMultiplier", 1.0, 0.0, 10.0);
            builder.pop();

            builder.push("scopeSway");
            scopeSwayStrength = builder
                    .comment("Maximum sway arc when scoped (degrees)")
                    .defineInRange("strength", 0.01, 0.0, 1.0);
            scopeSwaySpeed = builder
                    .comment("Seconds per full sway cycle")
                    .defineInRange("speed", 40.2, 1.0, 120.0);
            scopeSwayMinZoom = builder
                    .comment("Minimum zoom level required before scope sway activates")
                    .defineInRange("minZoom", 4.0, 1.0, 100.0);
            crouchStabilizeTime = builder
                    .comment("Milliseconds to hold crouch to stabilize sway")
                    .defineInRange("crouchStabilizeTime", 3000.0, 0.0, 10000.0);
            crouchSporadicTime = builder
                    .comment("Milliseconds of sporadic sway after stabilizing")
                    .defineInRange("crouchSporadicTime", 3000.0, 0.0, 10000.0);
            crouchCooldownTime = builder
                    .comment("Milliseconds cooldown after sporadic phase before you can stabilize again")
                    .defineInRange("crouchCooldownTime", 8000.0, 0.0, 20000.0);
            sporadicSwayStrength = builder
                    .comment("Multiplier for sway strength during sporadic phase")
                    .defineInRange("sporadicSwayStrength", 7.0, 1.0, 10.0);
            sporadicSwaySpeed = builder
                    .comment("Multiplier for sway speed during sporadic phase")
                    .defineInRange("sporadicSwaySpeed", 0.3, 0.1, 5.0);
            builder.pop();

            dragSmoothing = builder
                    .comment("Drag smoothing factor (lower = more inertia).")
                    .defineInRange("dragSmoothing", 0.2, 0.0, 1.0);
            decayFactor = builder
                    .comment("Decay factor for motion smoothing.")
                    .defineInRange("decayFactor", 0.85, 0.0, 1.0);
            momentumFactor = builder
                    .comment("Velocity influence on final position.")
                    .defineInRange("momentumFactor", 0.45, 0.0, 1.0);
            rollSensitivity = builder
                    .comment("Roll rotation sensitivity.")
                    .defineInRange("rollSensitivity", 1.2, 0.0, 10.0);
            maxTiltAngle = builder
                    .comment("Maximum roll angle (degrees).")
                    .defineInRange("maxTiltAngle", 20.0, 0.0, 180.0);

            builder.pop();
        }
    }

    public static void registerConfigs() {
        LOGGER.info("Registering TacZ Additions config files");
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.CLIENT, CLIENT_SPEC);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.SERVER, SERVER_SPEC);
    }
}