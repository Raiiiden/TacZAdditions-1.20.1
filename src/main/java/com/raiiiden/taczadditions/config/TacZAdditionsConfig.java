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
        Pair<Client, ForgeConfigSpec> clientConfig = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = clientConfig.getRight();
        CLIENT = clientConfig.getLeft();
    }

    public static class Client {
        public final ForgeConfigSpec.BooleanValue enableRecoilRecovery;
        public final ForgeConfigSpec.BooleanValue enableCameraSway;

        public final ForgeConfigSpec.DoubleValue hipfireRollFactor;
        public final ForgeConfigSpec.DoubleValue aimingRollFactor;
        public final ForgeConfigSpec.DoubleValue rollSensitivity;
        public final ForgeConfigSpec.DoubleValue maxTiltAngle;

        public final ForgeConfigSpec.DoubleValue hipfireYawMultiplier;
        public final ForgeConfigSpec.DoubleValue hipfirePitchMultiplier;

        public final ForgeConfigSpec.DoubleValue dragSmoothing;
        public final ForgeConfigSpec.DoubleValue decayFactor;
        public final ForgeConfigSpec.DoubleValue momentumFactor;

        public Client(ForgeConfigSpec.Builder builder) {
            builder.comment("TacZ Additions client configuration settings").push("client");

            enableRecoilRecovery = builder
                    .comment("If true, recoil recovery is enabled.")
                    .define("enableRecoilRecovery", true);

            enableCameraSway = builder
                    .comment("If true, camera sway is enabled.")
                    .define("enableCameraSway", true);

            hipfireRollFactor = builder
                    .comment("Roll factor when hip-firing.")
                    .defineInRange("hipfireRollFactor", 2.75, 0.0, 10.0);

            aimingRollFactor = builder
                    .comment("Roll factor when aiming.")
                    .defineInRange("aimingRollFactor", 2.75, 0.0, 10.0);

            rollSensitivity = builder
                    .comment("Sensitivity multiplier for roll rotation.")
                    .defineInRange("rollSensitivity", 1.2, 0.0, 10.0);

            maxTiltAngle = builder
                    .comment("Maximum tilt (roll) angle in degrees.")
                    .defineInRange("maxTiltAngle", 20.0, 0.0, 180.0);

            hipfireYawMultiplier = builder
                    .comment("Yaw multiplier when hip-firing.")
                    .defineInRange("hipfireYawMultiplier", 1.25, 0.0, 10.0);

            hipfirePitchMultiplier = builder
                    .comment("Pitch multiplier when hip-firing.")
                    .defineInRange("hipfirePitchMultiplier", 0.7, 0.0, 10.0);

            dragSmoothing = builder
                    .comment("Drag smoothing factor (lower = more inertia).")
                    .defineInRange("dragSmoothing", 0.15, 0.0, 1.0);

            decayFactor = builder
                    .comment("Decay factor for smoothing (higher = more lingering motion).")
                    .defineInRange("decayFactor", 0.84, 0.0, 1.0);

            momentumFactor = builder
                    .comment("How much velocity contributes to position (lower = heavier).")
                    .defineInRange("momentumFactor", 0.45, 0.0, 1.0);

            builder.pop();
        }
    }

    public static void registerConfigs() {
        LOGGER.info("Registering TacZ Additions configuration");
        net.minecraftforge.fml.ModLoadingContext.get()
                .registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT, CLIENT_SPEC);
    }
}
