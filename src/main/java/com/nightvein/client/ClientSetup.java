package com.nightvein.client;

import com.nightvein.Nightvein;
import com.nightvein.client.gui.VampHud;
import com.nightvein.client.render.NightwalkerRenderer;
import com.nightvein.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = Nightvein.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.NIGHTWALKER.get(), NightwalkerRenderer::new);
    }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent e) {
        for (var k : Keys.ALL) e.register(k);
    }

    @SubscribeEvent
    public static void gui(RegisterGuiLayersEvent e) {
        e.registerBelowAll(Nightvein.id("red_vision"), VampHud::tint);
        e.registerBelow(VanillaGuiLayers.FOOD_LEVEL, Nightvein.id("blood"), VampHud::bar);
    }
}
