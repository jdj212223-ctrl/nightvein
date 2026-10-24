package com.nightvein.vamp;

import com.nightvein.Nightvein;
import com.nightvein.NightveinConfig;
import com.nightvein.compat.VoidClipCompat;
import com.nightvein.network.Net;
import com.nightvein.network.SyncVampPayload;
import com.nightvein.registry.ModAttachments;
import com.nightvein.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.level.Level;

/** Becoming one, being one, and stopping. Server side unless noted. */
public final class Vampires {
    private static final ResourceLocation SPEED = Nightvein.id("speed");
    public static final ResourceLocation BLOODRUSH = Nightvein.id("bloodrush");
    private static final ResourceLocation BAT_SCALE = Nightvein.id("bat_scale");

    private Vampires() {}

    /** Works on both sides; on the client it holds whatever the server last synced. */
    public static VampData get(Player player) {
        return player.getData(ModAttachments.VAMP);
    }

    public static boolean isVampire(Entity e) {
        return e instanceof Player p && get(p).isVampire() || e instanceof com.nightvein.entity.Nightwalker;
    }

    public static int level(Player player, Skill skill) {
        VampData d = get(player);
        return d.isVampire() ? d.level(skill) : 0;
    }

    // ------------------------------------------------------------------ infection and cure

    /** A bite to the neck. Starts the turning; does nothing to someone already turning or turned. */
    public static boolean infect(ServerPlayer player) {
        VampData d = get(player);
        if (d.stage() != Stage.HUMAN || player.isCreative() || player.isSpectator()) return false;
        d.setStage(Stage.TURNING);
        d.setTurning(0);
        player.displayClientMessage(Component.translatable("msg.nightvein.bitten").withStyle(ChatFormatting.DARK_RED), false);
        player.level().playSound(null, player.blockPosition(), ModSounds.HEARTBEAT.get(), SoundSource.PLAYERS, 1f, 0.8f);
        sync(player);
        return true;
    }

    public static float turningProgress(VampData d) {
        return d.isTurning() ? Math.min(1f, d.turning() / (float) NightveinConfig.turningTicks()) : 0f;
    }

    /** The end of the turning, or dying partway through it. */
    public static void completeTurning(ServerPlayer player) {
        VampData d = get(player);
        d.setStage(Stage.VAMPIRE);
        d.setBlood(8);
        player.removeEffect(MobEffects.HUNGER);
        player.removeEffect(MobEffects.CONFUSION);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
        player.displayClientMessage(Component.translatable("msg.nightvein.turned").withStyle(ChatFormatting.RED), false);
        player.displayClientMessage(Component.translatable("msg.nightvein.turned.hint").withStyle(ChatFormatting.GRAY), false);
        player.level().playSound(null, player.blockPosition(), ModSounds.TURNED.get(), SoundSource.PLAYERS, 1.2f, 1f);
        refresh(player);
        sync(player);
    }

    /** Human again. Every skill and every drop of blood is gone. */
    public static void cure(ServerPlayer player) {
        VampData d = get(player);
        boolean wasBat = d.bat();
        d.reset();
        if (wasBat) BatForm.leave(player);
        player.removeEffect(MobEffects.NIGHT_VISION);
        player.removeEffect(MobEffects.HUNGER);
        player.getFoodData().setFoodLevel(20);
        player.level().playSound(null, player.blockPosition(), ModSounds.CURE.get(), SoundSource.PLAYERS, 1f, 1f);
        player.displayClientMessage(Component.translatable("msg.nightvein.cured").withStyle(ChatFormatting.GOLD), false);
        refresh(player);
        sync(player);
    }

    // ------------------------------------------------------------------ skills

    public enum UpgradeResult { NOT_A_VAMPIRE, MAXED, LOCKED, NO_POINTS, UPGRADED }

    public static UpgradeResult upgrade(ServerPlayer player, Skill skill) {
        VampData d = get(player);
        UpgradeResult r;
        if (!d.isVampire()) r = UpgradeResult.NOT_A_VAMPIRE;
        else if (d.level(skill) >= skill.maxLevel) r = UpgradeResult.MAXED;
        else if (skill.requires != null && d.level(skill.requires) == 0) r = UpgradeResult.LOCKED;
        else if (d.points() <= 0) r = UpgradeResult.NO_POINTS;
        else {
            d.setLevel(skill, d.level(skill) + 1);
            r = UpgradeResult.UPGRADED;
            player.level().playSound(null, player.blockPosition(), ModSounds.TURNED.get(), SoundSource.PLAYERS, 0.4f, 1.6f);
            refresh(player);
            sync(player);
        }
        return r;
    }

    public static double speedBonus(int level) {
        return switch (level) {
            case 1 -> 0.12;
            case 2 -> 0.24;
            case 3 -> 0.40;
            default -> 0;
        };
    }

    /** Re-applies attribute modifiers from scratch. They are transient, so this runs on login and respawn too. */
    public static void refresh(Player player) {
        VampData d = get(player);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        speed.removeModifier(SPEED);
        int lv = d.isVampire() ? d.level(Skill.SPEED) : 0;
        if (lv > 0) speed.addTransientModifier(new AttributeModifier(SPEED, speedBonus(lv), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        if (!d.isVampire()) {
            speed.removeModifier(BLOODRUSH);
            d.rushing = false;
        }
        AttributeInstance scale = player.getAttribute(Attributes.SCALE);
        scale.removeModifier(BAT_SCALE);
        if (d.isVampire() && d.bat()) scale.addTransientModifier(new AttributeModifier(BAT_SCALE, -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        player.refreshDimensions();
    }

    // ------------------------------------------------------------------ blood

    /**
     * How much blood one bite takes out of this, before Fangs. Nothing undead, nothing built, and nothing from the
     * Backrooms has any.
     */
    public static float bloodIn(LivingEntity e) {
        if (e.getType().is(EntityTypeTags.UNDEAD) || e instanceof AbstractGolem || VoidClipCompat.isBackroomsEntity(e)) return 0;
        if (e instanceof Player p) return get(p).isVampire() ? 0.5f : 4;
        if (e instanceof com.nightvein.entity.Nightwalker) return 1;
        if (e instanceof AbstractVillager || e instanceof AbstractIllager || e instanceof net.minecraft.world.entity.monster.Witch) return 4;
        if (e instanceof Animal) return 2;
        return 1;
    }

    /** Standing in daylight: day, nothing overhead, not raining here, not underwater. */
    public static boolean inSunlight(Player player) {
        Level level = player.level();
        if (!level.isDay() || level.dimensionType().hasFixedTime()) return false;
        BlockPos eyes = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        return level.canSeeSky(eyes) && !level.isRainingAt(eyes) && !player.isInWaterOrBubble();
    }

    // ------------------------------------------------------------------ sync

    /** Sends this player's state to themselves and to everyone near enough to see them (for bat form). */
    public static void sync(ServerPlayer player) {
        Net.sendAround(player, SyncVampPayload.of(player));
    }

    public static void syncTo(ServerPlayer viewer, Player target) {
        Net.send(viewer, SyncVampPayload.of(target));
    }
}
