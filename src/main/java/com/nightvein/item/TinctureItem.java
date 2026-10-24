package com.nightvein.item;

import com.nightvein.vamp.Stage;
import com.nightvein.vamp.VampData;
import com.nightvein.vamp.Vampires;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The cure.
 * <ul>
 *     <li>Hawthorn Tincture stops the turning, any time before it finishes.</li>
 *     <li>Sunlit Tincture undoes a full vampire, but only if you drink it standing in the sun. Anywhere else it just burns.</li>
 * </ul>
 */
public class TinctureItem extends DrinkItem {
    public enum Kind { HAWTHORN, SUNLIT }

    public enum Outcome { NOTHING, CURED_TURNING, TOO_LATE, CURED_VAMPIRE, NEEDS_SUN }

    public final Kind kind;

    public TinctureItem(Kind kind, Properties props) {
        super(props);
        this.kind = kind;
    }

    @Override
    protected void drink(ServerPlayer player, ItemStack stack) {
        apply(player, kind, Vampires.inSunlight(player));
    }

    /** Separated from the sun check so tests can decide whether it's sunny. */
    public static Outcome apply(ServerPlayer player, Kind kind, boolean inSun) {
        VampData d = Vampires.get(player);
        Stage stage = d.stage();
        if (stage == Stage.TURNING) {
            Vampires.cure(player);
            return Outcome.CURED_TURNING;
        }
        if (stage == Stage.HUMAN) {
            if (kind == Kind.SUNLIT) player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0));
            return Outcome.NOTHING;
        }
        if (kind == Kind.HAWTHORN) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
            player.displayClientMessage(Component.translatable("msg.nightvein.too_late").withStyle(ChatFormatting.GRAY), true);
            return Outcome.TOO_LATE;
        }
        if (!inSun) {
            player.setRemainingFireTicks(100);
            player.displayClientMessage(Component.translatable("msg.nightvein.needs_sun").withStyle(ChatFormatting.GOLD), true);
            return Outcome.NEEDS_SUN;
        }
        Vampires.cure(player);
        player.setRemainingFireTicks(60);
        return Outcome.CURED_VAMPIRE;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        String key = kind == Kind.HAWTHORN ? "item.nightvein.hawthorn_tincture.desc" : "item.nightvein.sunlit_tincture.desc";
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }
}
