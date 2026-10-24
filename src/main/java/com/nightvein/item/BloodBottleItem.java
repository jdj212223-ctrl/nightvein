package com.nightvein.item;

import com.nightvein.vamp.Vampires;
import com.nightvein.vamp.VampData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Blood to go. A vampire fills one by sneaking and using a glass bottle on something with blood in it.
 * Nightwalkers sometimes drop them.
 */
public class BloodBottleItem extends DrinkItem {
    public static final float BLOOD = 5;

    public BloodBottleItem(Properties props) {
        super(props);
    }

    @Override
    protected void drink(ServerPlayer player, ItemStack stack) {
        VampData d = Vampires.get(player);
        if (d.isVampire()) {
            d.drink(BLOOD);
            Vampires.sync(player);
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 1));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.nightvein.blood_bottle.desc").withStyle(ChatFormatting.DARK_RED));
    }
}
