package com.raiiiden.taczadditions.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class TacZAdditionsConfig {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        Pair<Common, ForgeConfigSpec> commonConfig = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = commonConfig.getRight();
        COMMON = commonConfig.getLeft();

        Pair<Client, ForgeConfigSpec> clientConfig = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = clientConfig.getRight();
        CLIENT = clientConfig.getLeft();
    }

    // Gameplay settings use local values until a connected server supplies its snapshot.
    public static class Common {
        public final SyncedValue<Boolean> enableRecoilRecovery;
        public final SyncedValue<Boolean> enableMuzzleFlash;
        public final SyncedValue<List<? extends String>> muzzleFlashWeaponBlacklist;
        public final SyncedValue<List<? extends String>> silencedGunIds;
        public final SyncedValue<List<? extends String>> coloredMuzzleFlashGunColors;
        public final SyncedValue<Double> laserDotMaxDistance;
        public final SyncedValue<Boolean> laserPassThroughNonCollidingBlocks;
        public final SyncedValue<List<? extends String>> laserPassThroughBlocks;
        public final SyncedValue<List<? extends String>> laserBlockingBlocks;
        public final SyncedValue<Boolean> enableLaserToggle;

        public final SyncedValue<Boolean> forceBlockLightForFastGuns;
        public final SyncedValue<Integer> fastGunRpmThreshold;

        // Gun tuck, visual and gameplay alike, so the drawn barrel matches the enforced bullet angle
        public final SyncedValue<Boolean> enableGunTuck;
        public final SyncedValue<Double> tuckDistance;
        public final SyncedValue<Double> tuckMaxAngle;
        public final SyncedValue<Double> tuckMaxTranslate;
        public final SyncedValue<Boolean> tuckAffectsBulletAngle;
        public final SyncedValue<Double> tuckBulletMaxAngle;
        public final SyncedValue<Boolean> blockFireWhenTucked;
        public final SyncedValue<Double> tuckFireBlockThreshold;

        // Aiming
        public final SyncedValue<Boolean> disableAimingWhileAirborne;

        // Variable scope zoom
        public final SyncedValue<Boolean> enableVariableZoom;
        public final SyncedValue<Boolean> variableZoomLockHotbarScroll;
        public final SyncedValue<Boolean> variableZoomAllowBuiltInScopes;
        public final SyncedValue<Double> variableZoomMaxMagnification;
        public final SyncedValue<List<? extends String>> variableZoomForceVariableScopes;
        public final SyncedValue<List<? extends String>> variableZoomForceFixedScopes;

        // Scope sway
        public final SyncedValue<Boolean> enableScopeSway;
        public final SyncedValue<Double> scopeSwayStrength;
        public final SyncedValue<Double> scopeSwaySpeed;
        public final SyncedValue<Double> scopeSwayMinZoom;
        public final SyncedValue<Double> crouchStabilizeTime;
        public final SyncedValue<Double> crouchSporadicTime;
        public final SyncedValue<Double> crouchCooldownTime;
        public final SyncedValue<Double> sporadicSwayStrength;
        public final SyncedValue<Double> sporadicSwaySpeed;

        // Category-based vertical recoil
        public final SyncedValue<Double> recoilPistolVertical;
        public final SyncedValue<Double> recoilRifleVertical;
        public final SyncedValue<Double> recoilSniperVertical;
        public final SyncedValue<Double> recoilSMGVertical;
        public final SyncedValue<Double> recoilShotgunVertical;
        public final SyncedValue<Double> recoilRPGVertical;
        public final SyncedValue<Double> recoilMGVertical;

        // Category-based horizontal recoil
        public final SyncedValue<Double> recoilPistolHorizontal;
        public final SyncedValue<Double> recoilRifleHorizontal;
        public final SyncedValue<Double> recoilSniperHorizontal;
        public final SyncedValue<Double> recoilSMGHorizontal;
        public final SyncedValue<Double> recoilShotgunHorizontal;
        public final SyncedValue<Double> recoilRPGHorizontal;
        public final SyncedValue<Double> recoilMGHorizontal;

        public Common(ForgeConfigSpec.Builder builder) {
            builder.comment("TacZ Additions - Common Config",
                            "On a server these values are the server's: they are sent to each player on login",
                            "and override that player's own file until they disconnect.")
                    .push("common");

            enableRecoilRecovery = ConfigSync.bool("enableRecoilRecovery", builder
                    .comment("If true, recoil recovery is enabled (Default in TaCZ). Disable to make recoil harder to control.")
                    .define("enableRecoilRecovery", true));

            enableMuzzleFlash = ConfigSync.bool("enableMuzzleFlash", builder
                    .comment("If false, muzzle flash will not be sent to clients.")
                    .define("enableMuzzleFlash", true));

            muzzleFlashWeaponBlacklist = ConfigSync.stringList("muzzleFlashWeaponBlacklist", builder
                    .comment(
                            "Gun IDs that must not produce muzzle-flash light. This is enforced by the server.",
                            "Use TaCZ gun IDs such as \"tacz:ak47\". Invalid resource locations are rejected.")
                    .defineListAllowEmpty(
                            "muzzleFlashWeaponBlacklist",
                            List.of("example:gun_id"),
                            value -> value instanceof String id && ResourceLocation.tryParse(id) != null));

            silencedGunIds = ConfigSync.stringList("silencedGunIds", builder
                    .comment(
                            "Gun IDs that TaCZ should always treat as silenced.",
                            "Use this for integrated suppressors that do not expose TaCZ's silence modifier.",
                            "This enables the gun's silenced sound and reduced muzzle-flash light.",
                            "Guns without a silenced sound keep using their normal shot sound.",
                            "Example: \"tacz:example_integrally_suppressed_gun\".")
                    .defineListAllowEmpty(
                            "silencedGunIds",
                            List.of("example:gun_id"),
                            value -> value instanceof String id && ResourceLocation.tryParse(id) != null));

            coloredMuzzleFlashGunColors = ConfigSync.stringList("coloredMuzzleFlashGunColors", builder
                    .comment(
                            "Optional per-gun muzzle-flash colors.",
                            "Requires the Colorful Lighting: Sodium/Embeddium Edition mod on the client.",
                            "Format: \"gun_id=#RRGGBB\". Example: \"tacz:ak47=#FF8A33\".")
                    .defineListAllowEmpty(
                            "coloredMuzzleFlashGunColors",
                            List.of("example:gun_id=#FF8A33"),
                            TacZAdditionsConfig::isValidGunColorEntry));

            laserDotMaxDistance = ConfigSync.dbl("laserDotMaxDistance", builder
                    .comment("Maximum ray trace distance for laser dot in blocks.")
                    .defineInRange("laserDotMaxDistance", 100.0, 1.0, 500.0));

            laserPassThroughNonCollidingBlocks = ConfigSync.bool("laserPassThroughNonCollidingBlocks", builder
                    .comment(
                            "If true, the laser passes through blocks you can walk through, such as tall grass and flowers.",
                            "Their selection box is far wider than the visible model, so the dot would otherwise",
                            "hang in the empty gaps around the plant.")
                    .define("laserPassThroughNonCollidingBlocks", true));

            laserPassThroughBlocks = ConfigSync.stringList("laserPassThroughBlocks", builder
                    .comment(
                            "Blocks the laser passes straight through instead of stopping on.",
                            "Accepts block IDs such as \"minecraft:glass\" and block tags prefixed with \"#\",",
                            "for example \"#forge:glass\".")
                    .defineListAllowEmpty(
                            "laserPassThroughBlocks",
                            List.of("#forge:glass", "#forge:glass_panes"),
                            TacZAdditionsConfig::isValidBlockOrTagEntry));

            laserBlockingBlocks = ConfigSync.stringList("laserBlockingBlocks", builder
                    .comment(
                            "Blocks that always stop the laser, overriding both settings above.",
                            "Defaults cover snow, which has no collision at one layer, and powder snow,",
                            "which has none at all, so the laser would otherwise pass through solid-looking snow.",
                            "Accepts block IDs and \"#\"-prefixed block tags.")
                    .defineListAllowEmpty(
                            "laserBlockingBlocks",
                            List.of("minecraft:snow", "minecraft:powder_snow"),
                            TacZAdditionsConfig::isValidBlockOrTagEntry));

            enableLaserToggle = ConfigSync.bool("enableLaserToggle", builder
                    .comment("Allow players to toggle equipped laser attachments with a keybind. If false, lasers are always on.")
                    .define("enableLaserToggle", true));

            forceBlockLightForFastGuns = ConfigSync.bool("forceBlockLightForFastGuns", builder
                    .comment("If true, guns firing above the RPM threshold will use light blocks instead of dynamic lights for more accurate muzzle flash timing.")
                    .define("forceBlockLightForFastGuns", true));

            fastGunRpmThreshold = ConfigSync.integer("fastGunRpmThreshold", builder
                    .comment("Guns firing at or above this RPM will use light blocks for muzzle flash when forceBlockLightForFastGuns is enabled.")
                    .defineInRange("fastGunRpmThreshold", 600, 1, 6000));

            builder.push("gunTuck");
            enableGunTuck = ConfigSync.bool("gunTuck.enableGunTuck", builder
                    .comment("If true, the gun pitches up when close to a wall.")
                    .define("enableGunTuck", true));
            tuckDistance = ConfigSync.dbl("gunTuck.tuckDistance", builder
                    .comment("Distance in blocks at which gun tuck begins. Drives both the visual tuck on",
                            "clients and the bullet angle / fire block below, so every player uses this value.")
                    .defineInRange("tuckDistance", 1.0, 0.1, 3.0));
            tuckMaxAngle = ConfigSync.dbl("gunTuck.maxAngle", builder
                    .comment("Maximum pitch angle when fully tucked (degrees). Negative values invert the movement.")
                    .defineInRange("maxAngle", 60.0, -90.0, 90.0));
            tuckMaxTranslate = ConfigSync.dbl("gunTuck.maxTranslate", builder
                    .comment("Maximum Z pullback when fully tucked.")
                    .defineInRange("maxTranslate", 1.0, -5.0, 5.0));
            tuckAffectsBulletAngle = ConfigSync.bool("gunTuck.tuckAffectsBulletAngle", builder
                    .comment("If true, bullets leave the barrel at an angle that follows the gun tuck.")
                    .define("tuckAffectsBulletAngle", true));
            tuckBulletMaxAngle = ConfigSync.dbl("gunTuck.tuckBulletMaxAngle", builder
                    .comment("Pitch offset applied to bullets at full tuck (degrees, upward).",
                            "Keep this equal to maxAngle above so shots follow the barrel players see.",
                            "Negative values aim the shot downward instead.")
                    .defineInRange("tuckBulletMaxAngle", 60.0, -90.0, 90.0));
            blockFireWhenTucked = ConfigSync.bool("gunTuck.blockFireWhenTucked", builder
                    .comment("If true, the gun cannot be fired once it is tucked past the threshold below.")
                    .define("blockFireWhenTucked", true));
            tuckFireBlockThreshold = ConfigSync.dbl("gunTuck.tuckFireBlockThreshold", builder
                    .comment("Tuck amount (0 = not tucked, 1 = fully tucked) at which firing is blocked.",
                            "1.0 is never reached in practice, so it effectively disables the block.")
                    .defineInRange("tuckFireBlockThreshold", 0.5, 0.05, 1.0));
            builder.pop();

            builder.push("aiming");
            disableAimingWhileAirborne = ConfigSync.bool("aiming.disableAimingWhileAirborne", builder
                    .comment(
                            "If true, jumping drops the player out of aim and aiming cannot be started mid-air.",
                            "Ladders, water, vehicles, elytra flight and creative flight are not treated as airborne.",
                            "With hold-to-aim the player re-aims automatically on landing if the key is still held.")
                    .define("disableAimingWhileAirborne", false));
            builder.pop();

            builder.push("variableZoom");
            enableVariableZoom = ConfigSync.bool("variableZoom.enabled", builder
                    .comment(
                            "Let scopes with several zoom levels be swept smoothly with the scroll wheel while aiming,",
                            "instead of only toggling between the levels with the zoom key.",
                            "Each authored level acts as a soft detent that briefly resists but can be scrolled past.")
                    .define("enabled", true));
            variableZoomLockHotbarScroll = ConfigSync.bool("variableZoom.lockHotbarScroll", builder
                    .comment(
                            "While aiming a variable scope, the scroll wheel changes magnification instead of the hotbar slot.",
                            "If false, magnification can only be changed with the zoom key.",
                            "Fixed scopes, iron sights and hip fire always keep normal hotbar scrolling.")
                    .define("lockHotbarScroll", true));
            variableZoomAllowBuiltInScopes = ConfigSync.bool("variableZoom.allowBuiltInScopes", builder
                    .comment(
                            "Also apply variable zoom to guns whose scope is built into the gun rather than attached.",
                            "TaCZ pins those to their first zoom level and its zoom key does nothing on them,",
                            "so this makes integrated variable optics usable. Set false for stock TaCZ behaviour.")
                    .define("allowBuiltInScopes", true));
            variableZoomMaxMagnification = ConfigSync.dbl("variableZoom.maxMagnification", builder
                    .comment("Upper limit on magnification, whatever a scope asks for. Raise it to disable the cap.")
                    .defineInRange("maxMagnification", 50.0, 1.0, 100.0));
            variableZoomForceVariableScopes = ConfigSync.stringList("variableZoom.forceVariableScopes", builder
                    .comment(
                            "Scope IDs to always treat as one continuous range, merging every zoom level they define.",
                            "Use this when a pack authors a real variable optic without marking its levels as one sight.",
                            "Example: \"tacz:scope_lpvo_1_6\".")
                    .defineListAllowEmpty(
                            "forceVariableScopes",
                            List.of("example:scope_id"),
                            value -> value instanceof String id && ResourceLocation.tryParse(id) != null));
            variableZoomForceFixedScopes = ConfigSync.stringList("variableZoom.forceFixedScopes", builder
                    .comment(
                            "Scope IDs to never sweep, keeping every zoom level a separate step.",
                            "Use this for optics that switch between fixed settings rather than zooming, such as a 1x/4x switch.",
                            "Example: \"tacz:scope_elcan_4x\".")
                    .defineListAllowEmpty(
                            "forceFixedScopes",
                            List.of("example:scope_id"),
                            value -> value instanceof String id && ResourceLocation.tryParse(id) != null));
            builder.pop();

            builder.push("scopeSway");
            enableScopeSway = ConfigSync.bool("scopeSway.enableScopeSway", builder
                    .comment("Enable subtle camera sway when aiming with high-magnification scopes (4x+)")
                    .define("enableScopeSway", true));
            scopeSwayStrength = ConfigSync.dbl("scopeSway.strength", builder
                    .comment("Maximum sway arc when scoped (degrees)")
                    .defineInRange("strength", 0.01, -1.0, 1.0));
            scopeSwaySpeed = ConfigSync.dbl("scopeSway.speed", builder
                    .comment("Seconds per full sway cycle")
                    .defineInRange("speed", 40.2, 1.0, 120.0));
            scopeSwayMinZoom = ConfigSync.dbl("scopeSway.minZoom", builder
                    .comment("Minimum zoom level required before scope sway activates")
                    .defineInRange("minZoom", 4.0, 1.0, 100.0));
            crouchStabilizeTime = ConfigSync.dbl("scopeSway.crouchStabilizeTime", builder
                    .comment("Milliseconds to hold crouch to stabilize sway")
                    .defineInRange("crouchStabilizeTime", 3000.0, 0.0, 10000.0));
            crouchSporadicTime = ConfigSync.dbl("scopeSway.crouchSporadicTime", builder
                    .comment("Milliseconds of sporadic sway after stabilizing")
                    .defineInRange("crouchSporadicTime", 3000.0, 0.0, 10000.0));
            crouchCooldownTime = ConfigSync.dbl("scopeSway.crouchCooldownTime", builder
                    .comment("Milliseconds cooldown after sporadic phase before you can stabilize again")
                    .defineInRange("crouchCooldownTime", 8000.0, 0.0, 20000.0));
            sporadicSwayStrength = ConfigSync.dbl("scopeSway.sporadicSwayStrength", builder
                    .comment("Multiplier for sway strength during sporadic phase")
                    .defineInRange("sporadicSwayStrength", 7.0, -10.0, 10.0));
            sporadicSwaySpeed = ConfigSync.dbl("scopeSway.sporadicSwaySpeed", builder
                    .comment("Multiplier for sway speed during sporadic phase")
                    .defineInRange("sporadicSwaySpeed", 0.3, 0.1, 5.0));
            builder.pop();

            // Vertical multipliers
            recoilPistolVertical = ConfigSync.dbl("recoilPistolVertical", builder
                    .comment("Vertical recoil multiplier for pistols. Negative values invert the recoil direction.")
                    .defineInRange("recoilPistolVertical", 1.0, -10.0, 10.0));
            recoilRifleVertical = ConfigSync.dbl("recoilRifleVertical", builder
                    .comment("Vertical recoil multiplier for rifles. Negative values invert the recoil direction.")
                    .defineInRange("recoilRifleVertical", 1.0, -10.0, 10.0));
            recoilSniperVertical = ConfigSync.dbl("recoilSniperVertical", builder
                    .comment("Vertical recoil multiplier for snipers. Negative values invert the recoil direction.")
                    .defineInRange("recoilSniperVertical", 1.0, -10.0, 10.0));
            recoilSMGVertical = ConfigSync.dbl("recoilSMGVertical", builder
                    .comment("Vertical recoil multiplier for SMGs. Negative values invert the recoil direction.")
                    .defineInRange("recoilSMGVertical", 1.0, -10.0, 10.0));
            recoilShotgunVertical = ConfigSync.dbl("recoilShotgunVertical", builder
                    .comment("Vertical recoil multiplier for shotguns. Negative values invert the recoil direction.")
                    .defineInRange("recoilShotgunVertical", 1.0, -10.0, 10.0));
            recoilRPGVertical = ConfigSync.dbl("recoilRPGVertical", builder
                    .comment("Vertical recoil multiplier for RPGs. Negative values invert the recoil direction.")
                    .defineInRange("recoilRPGVertical", 1.0, -10.0, 10.0));
            recoilMGVertical = ConfigSync.dbl("recoilMGVertical", builder
                    .comment("Vertical recoil multiplier for MGs. Negative values invert the recoil direction.")
                    .defineInRange("recoilMGVertical", 1.0, -10.0, 10.0));

            // Horizontal multipliers
            recoilPistolHorizontal = ConfigSync.dbl("recoilPistolHorizontal", builder
                    .comment("Horizontal recoil multiplier for pistols. Negative values invert the recoil direction.")
                    .defineInRange("recoilPistolHorizontal", 1.0, -10.0, 10.0));
            recoilRifleHorizontal = ConfigSync.dbl("recoilRifleHorizontal", builder
                    .comment("Horizontal recoil multiplier for rifles. Negative values invert the recoil direction.")
                    .defineInRange("recoilRifleHorizontal", 1.0, -10.0, 10.0));
            recoilSniperHorizontal = ConfigSync.dbl("recoilSniperHorizontal", builder
                    .comment("Horizontal recoil multiplier for snipers. Negative values invert the recoil direction.")
                    .defineInRange("recoilSniperHorizontal", 1.0, -10.0, 10.0));
            recoilSMGHorizontal = ConfigSync.dbl("recoilSMGHorizontal", builder
                    .comment("Horizontal recoil multiplier for SMGs. Negative values invert the recoil direction.")
                    .defineInRange("recoilSMGHorizontal", 1.0, -10.0, 10.0));
            recoilShotgunHorizontal = ConfigSync.dbl("recoilShotgunHorizontal", builder
                    .comment("Horizontal recoil multiplier for shotguns. Negative values invert the recoil direction.")
                    .defineInRange("recoilShotgunHorizontal", 1.0, -10.0, 10.0));
            recoilRPGHorizontal = ConfigSync.dbl("recoilRPGHorizontal", builder
                    .comment("Horizontal recoil multiplier for RPGs. Negative values invert the recoil direction.")
                    .defineInRange("recoilRPGHorizontal", 1.0, -10.0, 10.0));
            recoilMGHorizontal = ConfigSync.dbl("recoilMGHorizontal", builder
                    .comment("Horizontal recoil multiplier for MGs. Negative values invert the recoil direction.")
                    .defineInRange("recoilMGHorizontal", 1.0, -10.0, 10.0));
            builder.pop();
        }
    }

    // Cosmetic gun handling only, never sent anywhere: this stays each player's own preference.
    public static class Client {
        public final ForgeConfigSpec.BooleanValue enableGunMovement;
        public final ForgeConfigSpec.BooleanValue enableStrafeMovement;

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

        // Misc
        public final ForgeConfigSpec.DoubleValue dragSmoothing;
        public final ForgeConfigSpec.DoubleValue decayFactor;
        public final ForgeConfigSpec.DoubleValue momentumFactor;
        public final ForgeConfigSpec.DoubleValue rollSensitivity;
        public final ForgeConfigSpec.DoubleValue maxTiltAngle;

        // Variable scope zoom feel
        public final ForgeConfigSpec.DoubleValue variableZoomScrollSensitivity;
        public final ForgeConfigSpec.BooleanValue variableZoomInvertScroll;
        public final ForgeConfigSpec.DoubleValue variableZoomSmoothingSpeed;
        public final ForgeConfigSpec.DoubleValue variableZoomDetentSnapRange;
        public final ForgeConfigSpec.DoubleValue variableZoomDetentBreakout;
        public final ForgeConfigSpec.BooleanValue variableZoomHandReach;
        public final ForgeConfigSpec.DoubleValue variableZoomHandReachHoldSeconds;
        public final ForgeConfigSpec.DoubleValue variableZoomHandReachSpeed;
        public final ForgeConfigSpec.DoubleValue variableZoomHandReachDamping;
        public final ForgeConfigSpec.DoubleValue variableZoomHandReachOffsetX;
        public final ForgeConfigSpec.DoubleValue variableZoomHandReachOffsetY;
        public final ForgeConfigSpec.DoubleValue variableZoomHandReachOffsetZ;
        public final ForgeConfigSpec.DoubleValue variableZoomHandReachTwistX;
        public final ForgeConfigSpec.DoubleValue variableZoomHandReachTwistY;
        public final ForgeConfigSpec.DoubleValue variableZoomHandReachTwistZ;

        // Experimental
        public final ForgeConfigSpec.BooleanValue magazineText;
        public final ForgeConfigSpec.BooleanValue enableLaserDot;

        public Client(ForgeConfigSpec.Builder builder) {
            builder.comment("TacZ Additions - Client Config").push("client");

            builder.push("variableZoom");
            variableZoomScrollSensitivity = builder
                    .comment(
                            "How much of a scope's zoom range one scroll notch covers.",
                            "0.125 means roughly eight notches to sweep from the lowest to the highest magnification.")
                    .defineInRange("scrollSensitivity", 0.125, 0.01, 1.0);
            variableZoomInvertScroll = builder
                    .comment("If true, scrolling down zooms in instead of out.")
                    .define("invertScroll", false);
            variableZoomSmoothingSpeed = builder
                    .comment(
                            "How quickly the magnification catches up to where you scrolled, per tick.",
                            "1.0 snaps instantly. Lower values ease in more gradually.")
                    .defineInRange("smoothingSpeed", 0.35, 0.05, 1.0);
            variableZoomDetentSnapRange = builder
                    .comment(
                            "How close to one of the scope's own zoom levels the detent grabs, as a fraction of the range.",
                            "Larger values make the levels feel wider.")
                    .defineInRange("detentSnapRange", 0.04, 0.0, 0.5);
            variableZoomDetentBreakout = builder
                    .comment(
                            "How much extra scrolling it takes to leave a zoom level, as a fraction of the range.",
                            "This is the small lock felt at each level. 0.0 removes the detents entirely.")
                    .defineInRange("detentBreakout", 0.1, 0.0, 1.0);

            builder.push("handReach");
            variableZoomHandReach = builder
                    .comment("If true, the off hand reaches up to the optic while you scroll the magnification.")
                    .define("enabled", true);
            variableZoomHandReachHoldSeconds = builder
                    .comment("How long the hand stays at the optic after the last scroll notch, in seconds.")
                    .defineInRange("holdSeconds", 0.45, 0.0, 5.0);
            variableZoomHandReachSpeed = builder
                    .comment("How briskly the hand travels. Higher values arrive sooner.")
                    .defineInRange("speed", 12.0, 1.0, 60.0);
            variableZoomHandReachDamping = builder
                    .comment(
                            "How much the travel is damped. 1.0 settles without overshooting,",
                            "lower values let the hand overshoot slightly before it settles.")
                    .defineInRange("damping", 0.8, 0.1, 2.0);
            variableZoomHandReachOffsetX = builder
                    .comment(
                            "How far the hand travels to reach the optic, in model pixels (1/16 of a block).",
                            "X is sideways travel. Zero keeps the hand on its own line up the gun, which is",
                            "normally right, since the support hand already sits under the optic.")
                    .defineInRange("offsetX", 0.0, -32.0, 32.0);
            variableZoomHandReachOffsetY = builder
                    .comment(
                            "Y is vertical, positive is up. The arm is anchored around its middle, so this sits",
                            "below the optic to bring the top of the arm up against it rather than its centre.")
                    .defineInRange("offsetY", -6.0, -32.0, 32.0);
            variableZoomHandReachOffsetZ = builder
                    .comment(
                            "Z runs along the gun, positive is back toward you where the magnification ring sits.",
                            "This adds to the travel the two bones already imply, which is itself backwards.")
                    .defineInRange("offsetZ", 10.0, -32.0, 32.0);
            variableZoomHandReachTwistX = builder
                    .comment(
                            "How far the arm is turned once it arrives, in degrees, pivoting about the hand.",
                            "X pitches the far end of the arm up and down.")
                    .defineInRange("twistX", 0.0, -180.0, 180.0);
            variableZoomHandReachTwistY = builder
                    .comment(
                            "Y swings the far end of the arm sideways. The elbow sits back and to your left,",
                            "so negative carries it away from the gun and positive drives it into the centre.")
                    .defineInRange("twistY", -25.0, -180.0, 180.0);
            variableZoomHandReachTwistZ = builder
                    .comment("Z rolls the arm about its own length.")
                    .defineInRange("twistZ", 0.0, -180.0, 180.0);
            builder.pop();
            builder.pop();

            enableGunMovement = builder
                    .comment("If false, disables all gun movement (sway, roll, etc).")
                    .define("enableGunMovement", true);

            enableStrafeMovement = builder
                    .comment("If false, disables sway/roll from strafing movement.")
                    .define("enableStrafeMovement", true);

            builder.push("hipfire");
            hipfireYawMultiplier = builder
                    .comment("Yaw multiplier when hip-firing. Negative values invert the movement.")
                    .defineInRange("yawMultiplier", 1.25, -10.0, 10.0);
            hipfirePitchMultiplier = builder
                    .comment("Pitch multiplier when hip-firing. Negative values invert the movement.")
                    .defineInRange("pitchMultiplier", 1.2, -10.0, 10.0);
            hipfireRollFactor = builder
                    .comment("Roll factor when hip-firing. Negative values invert the movement.")
                    .defineInRange("rollFactor", 1.75, -10.0, 10.0);
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
                net.minecraftforge.fml.config.ModConfig.Type.COMMON, COMMON_SPEC);
    }

    private static boolean isValidBlockOrTagEntry(Object value) {
        if (!(value instanceof String entry)) return false;
        String id = entry.startsWith("#") ? entry.substring(1) : entry;
        return ResourceLocation.tryParse(id.trim()) != null;
    }

    private static boolean isValidGunColorEntry(Object value) {
        if (!(value instanceof String entry)) return false;
        int separator = entry.lastIndexOf('=');
        if (separator <= 0 || separator == entry.length() - 1) return false;
        if (ResourceLocation.tryParse(entry.substring(0, separator).trim()) == null) return false;
        return entry.substring(separator + 1).trim().matches("#[0-9a-fA-F]{6}");
    }
}
