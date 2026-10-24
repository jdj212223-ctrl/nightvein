package com.nightvein.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** A bottle you drink; leaves the glass behind. */
public abstract class DrinkItem extends Item {
    protected DrinkItem(Properties props) {
        super(props);
    }

    /** What drinking it does. Server side. */
    protected abstract void drink(ServerPlayer player, ItemStack stack);

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) drink(sp, stack);
        if (entity instanceof Player p && p.hasInfiniteMaterials()) return stack;
        stack.shrink(1);
        if (stack.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
        if (entity instanceof Player p && !p.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) p.drop(new ItemStack(Items.GLASS_BOTTLE), false);
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }
}
