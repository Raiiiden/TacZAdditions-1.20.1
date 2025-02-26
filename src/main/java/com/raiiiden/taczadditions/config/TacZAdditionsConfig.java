package com.raiiiden.taczadditions.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TacZAdditionsConfig {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        // Build the config using Forge's Pair method.
        Pair<Client, ForgeConfigSpec> clientConfig = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = clientConfig.getRight();
        CLIENT = clientConfig.getLeft();
    }

    // Client-side configuration options.
    public static class Client {
        // Recoil Recovery: if true, recoil recovery is enabled (normal behavior).
        // If false, no recovery (i.e. only kick is applied).
        public final ForgeConfigSpec.BooleanValue enableRecoilRecovery;

        // Camera Sway toggling and parameters:
        public final ForgeConfigSpec.BooleanValue enableCameraSway;
        public final ForgeConfigSpec.DoubleValue defaultMultiplier; // For translation sway
        public final ForgeConfigSpec.DoubleValue aimMultiplier;
        public final ForgeConfigSpec.DoubleValue smoothingFactor;   // For multiplier smoothing

        // Extra Roll (sideways tilt) parameters.
        public final ForgeConfigSpec.DoubleValue hipfireRollFactor;
        public final ForgeConfigSpec.DoubleValue aimingRollFactor;
        public final ForgeConfigSpec.DoubleValue rollSensitivity;
        public final ForgeConfigSpec.DoubleValue rollSmoothing;
        public final ForgeConfigSpec.DoubleValue maxTiltAngle;

        // Sway strength and speed (if used elsewhere).
        public final ForgeConfigSpec.DoubleValue swayStrength;
        public final ForgeConfigSpec.DoubleValue swaySpeed;

        public Client(ForgeConfigSpec.Builder builder) {
            builder.comment("TacZ Additions client configuration settings").push("client");

            // Recoil config
            enableRecoilRecovery = builder
                    .comment("If true, recoil recovery is enabled (normal behavior). If false, no negative deltas (recovery) are applied.")
                    .define("enableRecoilRecovery", true);

            // Camera sway toggle
            enableCameraSway = builder
                    .comment("If true, camera sway is enabled.")
                    .define("enableCameraSway", true);

            // Translation sway multipliers and smoothing
            defaultMultiplier = builder
                    .comment("Default multiplier for translation sway (When Not Aiming).")
                    .defineInRange("defaultMultiplier", 1.5, 0.0, 100.0);
            aimMultiplier = builder
                    .comment("Multiplier for translation sway when aiming (1-10 Recommended).")
                    .defineInRange("aimMultiplier", 6.0, 0.0, 100.0);
            smoothingFactor = builder
                    .comment("Smoothing factor for interpolating the translation sway multiplier (Small Values Recommended).")
                    .defineInRange("smoothingFactor", 0.045, 0.0, 1.0);

            // Extra Roll (sideways tilt) parameters
            hipfireRollFactor = builder
                    .comment("Roll factor when hip-firing (Not Aiming).")
                    .defineInRange("hipfireRollFactor", 0.5, 0.0, 10.0);
            aimingRollFactor = builder
                    .comment("Roll factor when aiming.")
                    .defineInRange("aimingRollFactor", 1.5, 0.0, 10.0);
            rollSensitivity = builder
                    .comment("Sensitivity for roll changes.")
                    .defineInRange("rollSensitivity", 15.0, 0.0, 100.0);
            rollSmoothing = builder
                    .comment("Smoothing factor for roll interpolation.")
                    .defineInRange("rollSmoothing", 0.04, 0.0, 1.0);
            maxTiltAngle = builder
                    .comment("Maximum tilt angle (in degrees) for roll.")
                    .defineInRange("maxTiltAngle", 30.0, 0.0, 180.0);

            // Additional sway values (if used elsewhere)
            swayStrength = builder
                    .comment("Multiplier for gun sway strength")
                    .defineInRange("swayStrength", 1.0, 0.0, 10.0);
            swaySpeed = builder
                    .comment("Multiplier for gun sway speed")
                    .defineInRange("swaySpeed", 1.0, 0.0, 10.0);

            builder.pop();
        }
    }

    // register the configuration
    public static void registerConfigs() {
        LOGGER.info("Registering TacZ Additions configuration");
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT, CLIENT_SPEC);
    }
}
