package com.raiiiden.taczadditions.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

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
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> muzzleFlashWeaponBlacklist;
        public final ForgeConfigSpec.DoubleValue laserDotMaxDistance;
        public final ForgeConfigSpec.BooleanValue enableLaserToggle;

        public final ForgeConfigSpec.BooleanValue forceBlockLightForFastGuns;
        public final ForgeConfigSpec.IntValue fastGunRpmThreshold;
        // Category-based vertical recoil
        public final ForgeConfigSpec.DoubleValue recoilPistolVertical;
        public final ForgeConfigSpec.DoubleValue recoilRifleVertical;
        public final ForgeConfigSpec.DoubleValue recoilSniperVertical;
        public final ForgeConfigSpec.DoubleValue recoilSMGVertical;
        public final ForgeConfigSpec.DoubleValue recoilShotgunVertical;
        public final ForgeConfigSpec.DoubleValue recoilRPGVertical;
        public final ForgeConfigSpec.DoubleValue recoilMGVertical;

        // Category-based horizontal recoil
        public final ForgeConfigSpec.DoubleValue recoilPistolHorizontal;
        public final ForgeConfigSpec.DoubleValue recoilRifleHorizontal;
        public final ForgeConfigSpec.DoubleValue recoilSniperHorizontal;
        public final ForgeConfigSpec.DoubleValue recoilSMGHorizontal;
        public final ForgeConfigSpec.DoubleValue recoilShotgunHorizontal;
        public final ForgeConfigSpec.DoubleValue recoilRPGHorizontal;
        public final ForgeConfigSpec.DoubleValue recoilMGHorizontal;

        public Server(ForgeConfigSpec.Builder builder) {
            builder.comment("TacZ Additions - Server Config").push("server");

            enableMuzzleFlash = builder
                    .comment("If false, muzzle flash will not be sent to clients.")
                    .define("enableMuzzleFlash", true);

            muzzleFlashWeaponBlacklist = builder
                    .comment(
                            "Gun IDs that must not produce muzzle-flash light. This is enforced by the server.",
                            "Use TaCZ gun IDs such as \"tacz:ak47\". Invalid resource locations are rejected.")
                    .defineListAllowEmpty(
                            "muzzleFlashWeaponBlacklist",
                            List.of(),
                            value -> value instanceof String id && ResourceLocation.tryParse(id) != null);

            laserDotMaxDistance = builder
                    .comment("Maximum ray trace distance for laser dot in blocks.")
                    .defineInRange("laserDotMaxDistance", 100.0, 1.0, 500.0);

            enableLaserToggle = builder
                    .comment("Allow players to toggle equipped laser attachments with a keybind. If false, lasers are always on.")
                    .define("enableLaserToggle", true);

            forceBlockLightForFastGuns = builder
                    .comment("If true, guns firing above the RPM threshold will use light blocks instead of dynamic lights for more accurate muzzle flash timing.")
                    .define("forceBlockLightForFastGuns", true);

            fastGunRpmThreshold = builder
                    .comment("Guns firing at or above this RPM will use light blocks for muzzle flash when forceBlockLightForFastGuns is enabled.")
                    .defineInRange("fastGunRpmThreshold", 600, 1, 6000);

            // Vertical multipliers
            recoilPistolVertical = builder
                    .comment("Vertical recoil multiplier for pistols. Negative values invert the recoil direction.")
                    .defineInRange("recoilPistolVertical", 1.0, -10.0, 10.0);
            recoilRifleVertical = builder
                    .comment("Vertical recoil multiplier for rifles. Negative values invert the recoil direction.")
                    .defineInRange("recoilRifleVertical", 1.0, -10.0, 10.0);
            recoilSniperVertical = builder
                    .comment("Vertical recoil multiplier for snipers. Negative values invert the recoil direction.")
                    .defineInRange("recoilSniperVertical", 1.0, -10.0, 10.0);
            recoilSMGVertical = builder
                    .comment("Vertical recoil multiplier for SMGs. Negative values invert the recoil direction.")
                    .defineInRange("recoilSMGVertical", 1.0, -10.0, 10.0);
            recoilShotgunVertical = builder
                    .comment("Vertical recoil multiplier for shotguns. Negative values invert the recoil direction.")
                    .defineInRange("recoilShotgunVertical", 1.0, -10.0, 10.0);
            recoilRPGVertical = builder
                    .comment("Vertical recoil multiplier for RPGs. Negative values invert the recoil direction.")
                    .defineInRange("recoilRPGVertical", 1.0, -10.0, 10.0);
            recoilMGVertical = builder
                    .comment("Vertical recoil multiplier for MGs. Negative values invert the recoil direction.")
                    .defineInRange("recoilMGVertical", 1.0, -10.0, 10.0);

            // Horizontal multipliers
            recoilPistolHorizontal = builder
                    .comment("Horizontal recoil multiplier for pistols. Negative values invert the recoil direction.")
                    .defineInRange("recoilPistolHorizontal", 1.0, -10.0, 10.0);
            recoilRifleHorizontal = builder
                    .comment("Horizontal recoil multiplier for rifles. Negative values invert the recoil direction.")
                    .defineInRange("recoilRifleHorizontal", 1.0, -10.0, 10.0);
            recoilSniperHorizontal = builder
                    .comment("Horizontal recoil multiplier for snipers. Negative values invert the recoil direction.")
                    .defineInRange("recoilSniperHorizontal", 1.0, -10.0, 10.0);
            recoilSMGHorizontal = builder
                    .comment("Horizontal recoil multiplier for SMGs. Negative values invert the recoil direction.")
                    .defineInRange("recoilSMGHorizontal", 1.0, -10.0, 10.0);
            recoilShotgunHorizontal = builder
                    .comment("Horizontal recoil multiplier for shotguns. Negative values invert the recoil direction.")
                    .defineInRange("recoilShotgunHorizontal", 1.0, -10.0, 10.0);
            recoilRPGHorizontal = builder
                    .comment("Horizontal recoil multiplier for RPGs. Negative values invert the recoil direction.")
                    .defineInRange("recoilRPGHorizontal", 1.0, -10.0, 10.0);
            recoilMGHorizontal = builder
                    .comment("Horizontal recoil multiplier for MGs. Negative values invert the recoil direction.")
                    .defineInRange("recoilMGHorizontal", 1.0, -10.0, 10.0);
            builder.pop();
        }
    }

    public static class Client {
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
        public final ForgeConfigSpec.DoubleValue maxHipPitch;
        public final ForgeConfigSpec.DoubleValue maxHipYaw;

        // Aiming
        public final ForgeConfigSpec.DoubleValue aimingYawMultiplier;
        public final ForgeConfigSpec.DoubleValue aimingRollFactor;
        public final ForgeConfigSpec.DoubleValue maxAimPitch;
        public final ForgeConfigSpec.DoubleValue maxAimYaw;

        // Recoil
        public final ForgeConfigSpec.DoubleValue recoilVisualX;
        public final ForgeConfigSpec.DoubleValue recoilVisualY;
        public final ForgeConfigSpec.DoubleValue recoilVisualZ;
        public final ForgeConfigSpec.DoubleValue recoilKickAngle;
        public final ForgeConfigSpec.DoubleValue recoilKickPivot;

        // Gun Tuck
        public final ForgeConfigSpec.BooleanValue enableGunTuck;
        public final ForgeConfigSpec.DoubleValue gunTuckDistance;
        public final ForgeConfigSpec.DoubleValue gunTuckMaxAngle;
        public final ForgeConfigSpec.DoubleValue gunTuckMaxTranslate;

        // Misc
        public final ForgeConfigSpec.DoubleValue dragSmoothing;
        public final ForgeConfigSpec.DoubleValue decayFactor;
        public final ForgeConfigSpec.DoubleValue momentumFactor;
        public final ForgeConfigSpec.DoubleValue rollSensitivity;
        public final ForgeConfigSpec.DoubleValue maxTiltAngle;

        // Experimental
        public final ForgeConfigSpec.BooleanValue magazineText;
        public final ForgeConfigSpec.BooleanValue enableLaserDot;

        public Client(ForgeConfigSpec.Builder builder) {
            builder.comment("TacZ Additions - Client Config").push("client");
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
                    .comment("Yaw multiplier when hip-firing. Negative values invert the movement.")
                    .defineInRange("yawMultiplier", 1.25, -10.0, 10.0);
            hipfirePitchMultiplier = builder
                    .comment("Pitch multiplier when hip-firing. Negative values invert the movement.")
                    .defineInRange("pitchMultiplier", 1.2, -10.0, 10.0);
            hipfireRollFactor = builder
                    .comment("Roll factor when hip-firing. Negative values invert the movement.")
                    .defineInRange("rollFactor", 2.75, -10.0, 10.0);
            maxHipPitch = builder
                    .comment("Maximum pitch offset when hip-firing (degrees)")
                    .defineInRange("maxHipPitch", 6.0, 0.0, 45.0);

            maxHipYaw = builder
                    .comment("Maximum yaw offset when hip-firing (degrees)")
                    .defineInRange("maxHipYaw", 12.0, 0.0, 45.0);
            builder.pop();

            builder.push("aim");
            aimingYawMultiplier = builder
                    .comment("Yaw multiplier when aiming. Negative values invert the movement.")
                    .defineInRange("yawMultiplier", 0.6, -10.0, 10.0);
            aimingRollFactor = builder
                    .comment("Roll factor when aiming. Negative values invert the movement.")
                    .defineInRange("rollFactor", 2.75, -10.0, 10.0);
            maxAimPitch = builder
                    .comment("Maximum pitch offset when aiming (degrees)")
                    .defineInRange("maxAimPitch", 2.0, 0.0, 45.0);

            maxAimYaw = builder
                    .comment("Maximum yaw offset when aiming (degrees)")
                    .defineInRange("maxAimYaw", 10.0, 0.0, 45.0);
            builder.pop();

            builder.push("strafe");
            strafeYawMultiplier = builder
                    .comment("Yaw sway from strafing while hip-firing. Negative values invert the movement.")
                    .defineInRange("hipfireYawMultiplier", 0.0, -40.0, 40.0);
            strafeRollMultiplier = builder
                    .comment("Roll tilt from strafing while hip-firing. Negative values invert the movement.")
                    .defineInRange("hipfireRollMultiplier", 20.0, -40.0, 40.0);
            aimStrafeYawMultiplier = builder
                    .comment("Yaw sway from strafing while aiming. Negative values invert the movement.")
                    .defineInRange("aimingYawMultiplier", 0.0, -40.0, 40.0);
            aimStrafeRollMultiplier = builder
                    .comment("Roll tilt from strafing while aiming. Negative values invert the movement.")
                    .defineInRange("aimingRollMultiplier", 20.0, -40.0, 40.0);
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
                    .defineInRange("visualX", 0.0, -10.0, 10.0);
            recoilVisualY = builder
                    .comment("Visual recoil Y multiplier (vertical bounce). Negative values invert the movement.")
                    .defineInRange("visualY", 0.0, -10.0, 10.0);
            recoilVisualZ = builder
                    .comment("Visual recoil Z multiplier (kickback). Negative values invert the movement.")
                    .defineInRange("visualZ", 5.0, -20.0, 20.0);
            recoilKickAngle = builder
                    .comment("Visual recoil kick angle in degrees (negative rotates downward, 0 = disabled)")
                    .defineInRange("kickAngle", 1.0, -45.0, 45.0);
            recoilKickPivot = builder
                    .comment("Pivot point offset along Z axis for barrel kick rotation (distance from grip)")
                    .defineInRange("kickPivot", 0.0, -2.0, 2.0);
            builder.pop();

            builder.push("gunTuck");
            enableGunTuck = builder
                    .comment("If true, the gun pitches up when close to a wall.")
                    .define("enableGunTuck", true);
            gunTuckDistance = builder
                    .comment("Distance in blocks at which gun tuck begins.")
                    .defineInRange("tuckDistance", 1.0, 0.1, 3.0);
            gunTuckMaxAngle = builder
                    .comment("Maximum pitch angle when fully tucked (degrees). Negative values invert the movement.")
                    .defineInRange("maxAngle", 60.0, -90.0, 90.0);
            gunTuckMaxTranslate = builder
                    .comment("Maximum Z pullback when fully tucked.")
                    .defineInRange("maxTranslate", 1.0, -5.0, 5.0);
            builder.pop();

            builder.push("scopeSway");
            scopeSwayStrength = builder
                    .comment("Maximum sway arc when scoped (degrees)")
                    .defineInRange("strength", 0.01, -1.0, 1.0);
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
                    .defineInRange("sporadicSwayStrength", 7.0, -10.0, 10.0);
            sporadicSwaySpeed = builder
                    .comment("Multiplier for sway speed during sporadic phase")
                    .defineInRange("sporadicSwaySpeed", 0.3, 0.1, 5.0);
            builder.pop();

            dragSmoothing = builder
                    .comment("Drag smoothing factor (lower = more inertia).")
                    .defineInRange("dragSmoothing", 0.1, 0.0, 1.0);
            decayFactor = builder
                    .comment("Decay factor for motion smoothing.")
                    .defineInRange("decayFactor", 0.5, 0.0, 1.0);
            momentumFactor = builder
                    .comment("Velocity influence on final position.")
                    .defineInRange("momentumFactor", 0.45, 0.0, 1.0);
            rollSensitivity = builder
                    .comment("Roll rotation sensitivity. Negative values invert the movement.")
                    .defineInRange("rollSensitivity", 1.2, -10.0, 10.0);
            maxTiltAngle = builder
                    .comment("Maximum roll angle (degrees).")
                    .defineInRange("maxTiltAngle", 20.0, 0.0, 180.0);

            builder.pop();

            builder.push("experimental");
            magazineText = builder
                    .comment("If true, shows floating magazine ammo text (experimental).")
                    .define("magazineText", false);
            enableLaserDot = builder
                    .comment("If true, renders laser dot particles when using laser attachments")
                    .define("enableLaserDot", true);
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
