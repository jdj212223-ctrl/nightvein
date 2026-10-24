package com.nightvein.vamp;

import com.nightvein.NightveinConfig;
import com.nightvein.compat.ChromefleshCompat;
import com.nightvein.registry.ModDamageTypes;
import com.nightvein.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Everything that happens on its own, every tick, to the turning and the turned. */
public final class VampTicker {
    /** Vampires never get hungry, and never heal from food: the bar is held just below natural regeneration. */
    public static final int PINNED_FOOD = 17;
    public static final int CRASH_TICKS = 200;

    private VampTicker() {}

    public static void tick(ServerPlayer p) {
        VampData d = Vampires.get(p);
        if (d.biteCooldown > 0) d.biteCooldown--;
        if (d.dashCooldown > 0) d.dashCooldown--;
        if (d.mistCooldown > 0) d.mistCooldown--;
        if (d.hypnosisCooldown > 0) d.hypnosisCooldown--;
        if (d.dashTicks > 0) d.dashTicks--;
        if (d.crash > 0) d.crash--;
        switch (d.stage()) {
            case TURNING -> turning(p, d);
            case VAMPIRE -> vampire(p, d);
            default -> {}
        }
        if (d.stage() != Stage.HUMAN && p.tickCount % 20 == 0) Vampires.sync(p);
    }

    // ------------------------------------------------------------------ turning

    /** Thirst, sun sickness, and near the end, the body starting to give up. The red and the heartbeat are client side. */
    public static void turning(ServerPlayer p, VampData d) {
        d.setTurning(d.turning() + 1);
        float progress = Vampires.turningProgress(d);
        if (p.tickCount % 100 == 0) {
            effect(p, MobEffects.HUNGER, 120, progress > 0.5f ? 1 : 0);
        }
        if (p.tickCount % 40 == 0 && Vampires.inSunlight(p)) {
            effect(p, MobEffects.CONFUSION, 120, 0);
            effect(p, MobEffects.WEAKNESS, 80, 0);
            p.displayClientMessage(Component.translatable("msg.nightvein.sun_sick").withStyle(ChatFormatting.GOLD), true);
        }
        if (progress > 0.7f && p.tickCount % 200 == 0) effect(p, MobEffects.MOVEMENT_SLOWDOWN, 80, 0);
        if (d.turning() >= NightveinConfig.turningTicks()) Vampires.completeTurning(p);
    }

    // ------------------------------------------------------------------ vampire

    public static void vampire(ServerPlayer p, VampData d) {
        p.getFoodData().setFoodLevel(PINNED_FOOD);
        p.getFoodData().setSaturation(0);
        p.getFoodData().setExhaustion(0);

        if (p.tickCount % 1200 == 0) d.drain(1);
        sun(p, d, Vampires.inSunlight(p));
        bloodrush(p, d);
        regen(p, d);

        if (d.nightVision() && d.level(Skill.NIGHT_VISION) > 0) {
            MobEffectInstance nv = p.getEffect(MobEffects.NIGHT_VISION);
            if (nv == null || nv.getDuration() < 220) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
        }
        if (d.bloodSense() && p.tickCount % 600 == 0) d.drain(1);

        if (d.bat()) {
            int lv = d.level(Skill.BAT_FORM);
            if (lv == 0 || d.blood() <= 0) BatForm.leave(p);
            else {
                if (!p.getAbilities().mayfly) BatForm.apply(p, lv);
                if (p.tickCount % BatForm.drainInterval(lv) == 0) d.drain(1);
            }
        }

        if (d.blood() <= 0) {
            effect(p, MobEffects.WEAKNESS, 60, 1);
            effect(p, MobEffects.MOVEMENT_SLOWDOWN, 60, 0);
            if (p.tickCount % 80 == 0 && p.getHealth() > 1) {
                p.hurt(ModDamageTypes.source(p.level(), ModDamageTypes.THIRST), Math.min(1f, p.getHealth() - 1));
                if (p.tickCount % 400 == 0) p.displayClientMessage(Component.translatable("msg.nightvein.thirst").withStyle(ChatFormatting.DARK_RED), true);
            }
        }

        Tackle.tick(p, d);
    }

    /** Ticks between catching fire in the sun, or 0 if it doesn't burn you at all. */
    public static int burnInterval(int ward, boolean helmet) {
        if (!NightveinConfig.sunBurns() || ward >= 2) return 0;
        int base = ward == 0 ? 40 : 100;
        return helmet ? base * 2 : base;
    }

    /** Returns true if it set you on fire this tick. */
    public static boolean sun(ServerPlayer p, VampData d, boolean exposed) {
        int ward = d.level(Skill.SUN_WARD);
        if (!exposed || ward >= 3) return false;
        effect(p, MobEffects.WEAKNESS, 60, 0);
        if (ward == 0) effect(p, MobEffects.MOVEMENT_SLOWDOWN, 60, 0);
        if (p.tickCount % 200 == 0) d.drain(1);
        if (d.bat() && ward < 2) {
            BatForm.leave(p);
            p.displayClientMessage(Component.translatable("msg.nightvein.sun_bat").withStyle(ChatFormatting.GOLD), true);
        }
        int interval = burnInterval(ward, !p.getItemBySlot(EquipmentSlot.HEAD).isEmpty());
        if (interval > 0 && p.tickCount % interval == 0 && !p.fireImmune() && !p.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            p.setRemainingFireTicks(Math.max(p.getRemainingFireTicks(), 40));
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ Bloodrush (Chromeflesh)

    /** Extra speed (multiplies everything else) for vampire speed riding chrome legs. */
    public static double bloodrushBonus(int implants, int speedLevel) {
        return 0.15 * implants + 0.10 * speedLevel;
    }

    /**
     * Vampire speed stacked on Chromeflesh speed implants, while sprinting: the fastest thing in the game. The chrome
     * runs on your blood, a point every two seconds, and running dry crashes you hard.
     */
    public static void bloodrush(ServerPlayer p, VampData d) {
        int implants = ChromefleshCompat.speedImplants(p);
        int speedLv = d.level(Skill.SPEED);
        boolean can = implants > 0 && speedLv > 0 && p.isSprinting() && d.crash == 0 && d.blood() > 0 && !d.bat();
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        boolean was = d.rushing;
        if (can) {
            double bonus = bloodrushBonus(implants, speedLv);
            AttributeModifier current = speed.getModifier(Vampires.BLOODRUSH);
            if (current == null || current.amount() != bonus) {
                speed.removeModifier(Vampires.BLOODRUSH);
                speed.addTransientModifier(new AttributeModifier(Vampires.BLOODRUSH, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
            d.rushing = true;
            if (p.tickCount % 40 == 0) d.drain(1);
            if (d.blood() <= 0) crash(p, d);
        } else {
            speed.removeModifier(Vampires.BLOODRUSH);
            d.rushing = false;
        }
        if (!was && d.rushing) p.level().playSound(null, p.blockPosition(), ModSounds.BLOODRUSH.get(), SoundSource.PLAYERS, 0.8f, 1f);
        if (was != d.rushing) Vampires.sync(p);
    }

    public static void crash(ServerPlayer p, VampData d) {
        d.crash = CRASH_TICKS;
        d.rushing = false;
        p.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(Vampires.BLOODRUSH);
        effect(p, MobEffects.MOVEMENT_SLOWDOWN, CRASH_TICKS, 1);
        effect(p, MobEffects.CONFUSION, 100, 0);
        p.setSprinting(false);
        p.level().playSound(null, p.blockPosition(), ModSounds.CRASH.get(), SoundSource.PLAYERS, 1f, 1f);
        p.displayClientMessage(Component.translatable("msg.nightvein.crash").withStyle(ChatFormatting.DARK_RED), true);
    }

    // ------------------------------------------------------------------ healing

    /** Ticks per half heart healed. Without Regen it's slow and needs a well-fed vampire. */
    public static int regenInterval(int level) {
        return switch (level) {
            case 1 -> 80;
            case 2 -> 50;
            case 3 -> 30;
            default -> 160;
        };
    }

    public static void regen(ServerPlayer p, VampData d) {
        int lv = d.level(Skill.REGEN);
        if (p.getHealth() >= p.getMaxHealth() || d.blood() < (lv == 0 ? 10 : 2)) return;
        if (p.tickCount % regenInterval(lv) != 0) return;
        p.heal(1);
        d.drain(0.25f);
    }

    private static void effect(ServerPlayer p, Holder<MobEffect> effect, int ticks, int amp) {
        MobEffectInstance cur = p.getEffect(effect);
        if (cur == null || cur.getDuration() < ticks / 2 || cur.getAmplifier() < amp) {
            p.addEffect(new MobEffectInstance(effect, ticks, amp, true, true));
        }
    }
}
