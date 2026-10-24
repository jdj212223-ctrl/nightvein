package com.nightvein;

import com.nightvein.registry.ModAttachments;
import com.nightvein.registry.ModCreativeTabs;
import com.nightvein.registry.ModEntities;
import com.nightvein.registry.ModItems;
import com.nightvein.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(Nightvein.MODID)
public class Nightvein {
    public static final String MODID = "nightvein";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Nightvein(IEventBus modBus, ModContainer container) {
        ModItems.ITEMS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, NightveinConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
