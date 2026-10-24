package com.nightvein.registry;

import com.nightvein.Nightvein;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Nightvein.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.nightvein"))
            .icon(() -> ModItems.BLOOD_BOTTLE.get().getDefaultInstance())
            .displayItems((params, out) -> {
                out.accept(ModItems.BLOOD_BOTTLE.get());
                out.accept(ModItems.HAWTHORN_TINCTURE.get());
                out.accept(ModItems.SUNLIT_TINCTURE.get());
                out.accept(ModItems.NIGHTWALKER_SPAWN_EGG.get());
            })
            .build());

    private ModCreativeTabs() {}
}
