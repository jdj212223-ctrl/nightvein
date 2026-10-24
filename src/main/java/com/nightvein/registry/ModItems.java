package com.nightvein.registry;

import com.nightvein.Nightvein;
import com.nightvein.item.BloodBottleItem;
import com.nightvein.item.TinctureItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nightvein.MODID);

    public static final DeferredItem<BloodBottleItem> BLOOD_BOTTLE = ITEMS.register("blood_bottle",
            () -> new BloodBottleItem(new Item.Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE)));
    public static final DeferredItem<TinctureItem> HAWTHORN_TINCTURE = ITEMS.register("hawthorn_tincture",
            () -> new TinctureItem(TinctureItem.Kind.HAWTHORN, new Item.Properties().stacksTo(8)));
    public static final DeferredItem<TinctureItem> SUNLIT_TINCTURE = ITEMS.register("sunlit_tincture",
            () -> new TinctureItem(TinctureItem.Kind.SUNLIT, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<DeferredSpawnEggItem> NIGHTWALKER_SPAWN_EGG = ITEMS.register("nightwalker_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.NIGHTWALKER, 0x1A1418, 0xB0182A, new Item.Properties()));

    private ModItems() {}
}
