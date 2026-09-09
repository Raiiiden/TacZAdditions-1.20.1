package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.raiiiden.taczadditions.util.ScopeZoomBands;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Takes the scroll wheel away from the hotbar while aiming a variable optic and feeds it to
// VariableZoomState instead. Anything else, fixed scopes, iron sights, hipfire, scrolls as usual.
@Mod.EventBusSubscriber(modid = "taczadditions", value = Dist.CLIENT)
public final class VariableZoomScrollHandler {

    private VariableZoomScrollHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        VariableZoomState.tick();
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!TacZAdditionsConfig.COMMON.enableVariableZoom.get()) return;
        if (!TacZAdditionsConfig.COMMON.variableZoomLockHotbarScroll.get()) return;

        Minecraft mc = Minecraft.getInstance();
        // A container or menu owns its own scrolling.
        if (mc.player == null || mc.screen != null) return;

        if (VariableZoomState.onScroll(event.getScrollDelta())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        VariableZoomState.reset();
        // Attachment indexes are rebuilt per connection, so the band cache must not outlive one.
        ScopeZoomBands.clearCache();
    }
}
