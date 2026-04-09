package com.raiiiden.taczadditions.debug;

import com.raiiiden.taczadditions.TaczAdditions;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.resource.GunDisplayInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = TaczAdditions.MODID, value = Dist.CLIENT)
public class GunBoneDebugger {

    public static boolean enabled = false;

    private static long lastLogTime = 0;
    private static final long LOG_INTERVAL = 2000; // 2 seconds

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!enabled) return;
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogTime < LOG_INTERVAL) {
            return;
        }

        Player player = mc.player;
        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.isEmpty() || !(heldItem.getItem() instanceof IGun)) {
            return;
        }

        IGun iGun = (IGun) heldItem.getItem();
        Optional<GunDisplayInstance> displayOpt = TimelessAPI.getGunDisplay(heldItem);

        if (!displayOpt.isPresent()) {
            return;
        }

        GunDisplayInstance gunDisplay = displayOpt.get();
        BedrockGunModel gunModel = gunDisplay.getGunModel();

        if (gunModel == null || gunModel.getRootNode() == null) {
            return;
        }

        lastLogTime = currentTime;

        TaczAdditions.LOGGER.info("=== Gun Bone Structure for: {} ===", iGun.getGunId(heldItem));
        logBoneHierarchy(gunModel.getRootNode(), 0);
        TaczAdditions.LOGGER.info("=== End of Bone Structure ===");
    }

    private static void logBoneHierarchy(BedrockPart part, int depth) {
        if (part == null) return;

        String indent = "  ".repeat(depth);
        String visibility = part.visible ? "visible" : "HIDDEN";

        TaczAdditions.LOGGER.info("{}|- {} ({})", indent, part.name, visibility);

        // Log children
        for (BedrockPart child : part.children) {
            logBoneHierarchy(child, depth + 1);
        }
    }
}