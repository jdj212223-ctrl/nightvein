package com.nightvein.vamp;

import com.nightvein.compat.ChromefleshCompat;
import com.nightvein.compat.VoidClipCompat;
import com.nightvein.network.ActionPayload;
import com.nightvein.registry.ModDamageTypes;
import com.nightvein.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/** What the keybinds do once they reach the server. */
public final class Powers {
    public static final double BITE_RANGE = 3.5;
    public static final double HYPNOSIS_RANGE = 16;
    public static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.55f, 0.02f, 0.06f), 1f);

    private Powers() {}

    public static void handle(ServerPlayer player, int action) {
        if (action >= ActionPayload.UPGRADE && action < ActionPayload.UPGRADE + Skill.values().length) {
            Skill skill = Skill.values()[action - ActionPayload.UPGRADE];
            Vampires.UpgradeResult r = Vampires.upgrade(player, skill);
            if (r != Vampires.UpgradeResult.UPGRADED) {
                say(player, Component.translatable("msg.nightvein.upgrade." + r.name().toLowerCase(java.util.Locale.ROOT)), ChatFormatting.GRAY);
            }
            return;
        }
        if (!Vampires.get(player).isVampire()) return;
        switch (action) {
            case ActionPayload.BITE -> {
                Aim aim = aim(player, BITE_RANGE);
                if (aim != null) bite(player, aim.target, aim.at);
            }
            case ActionPayload.DASH -> dash(player);
            case ActionPayload.MIST -> mistStep(player);
            case ActionPayload.HYPNOSIS -> {
                Aim aim = aim(player, HYPNOSIS_RANGE);
                if (aim != null && aim.target instanceof LivingEntity le) hypnotize(player, le);
                else say(player, Component.translatable("msg.nightvein.no_target"), ChatFormatting.GRAY);
            }
            case ActionPayload.BAT -> {
                if (Vampires.get(player).bat()) BatForm.leave(player);
                else if (!BatForm.enter(player)) say(player, Component.translatable("msg.nightvein.cant_bat"), ChatFormatting.GRAY);
            }
            case ActionPayload.BLOOD_SENSE -> toggleSense(player);
            case ActionPayload.NIGHT_VISION -> toggleNightVision(player);
            default -> {}
        }
    }

    // ------------------------------------------------------------------ bite

    public enum BiteResult { COOLDOWN, NO_BLOOD, BIT, BIT_NECK, INFECTED }

    /**
     * From behind, or at the neck (aimed at the head), it's a bite to the neck. That's the only bite that turns someone.
     * Hypnotised and sleeping victims can't stop you getting to it.
     */
    public static boolean isNeckBite(Entity biter, LivingEntity victim, Vec3 hitAt) {
        if (victim.isSleeping() || Stuns.isStunned(victim) || victim.hasEffect(MobEffects.DARKNESS) && victim.hasEffect(MobEffects.CONFUSION)) return true;
        if (behind(biter, victim)) return true;
        return hitAt.y >= victim.getEyeY() - 0.45 * victim.getScale();
    }

    /** Is {@code biter} behind where {@code victim}'s body is facing? */
    public static boolean behind(Entity biter, LivingEntity victim) {
        float yaw = victim.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 facing = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 to = biter.position().subtract(victim.position()).multiply(1, 0, 1);
        if (to.lengthSqr() < 1e-4) return false;
        return facing.dot(to.normalize()) < -0.3;
    }

    public static float biteDamage(int fangs) {
        return 2f + 1.5f * fangs;
    }

    public static BiteResult bite(ServerPlayer player, LivingEntity victim, Vec3 hitAt) {
        VampData d = Vampires.get(player);
        if (d.biteCooldown > 0) return BiteResult.COOLDOWN;
        d.biteCooldown = 16;
        int fangs = d.level(Skill.FANGS);
        boolean neck = isNeckBite(player, victim, hitAt);
        float damage = biteDamage(fangs) * (neck ? 1.5f : 1f);
        ServerLevel level = player.serverLevel();
        level.playSound(null, victim.blockPosition(), ModSounds.BITE.get(), SoundSource.PLAYERS, 1f, 0.9f + player.getRandom().nextFloat() * 0.2f);
        level.sendParticles(BLOOD, victim.getX(), victim.getEyeY() - 0.3, victim.getZ(), 10, 0.15, 0.15, 0.15, 0);
        victim.hurt(ModDamageTypes.source(level, ModDamageTypes.BITE, player), damage);

        float blood = Vampires.bloodIn(victim) * (1 + 0.25f * fangs) * (neck ? 1.5f : 1f);
        if (blood <= 0) {
            say(player, Component.translatable(VoidClipCompat.isBackroomsEntity(victim) ? "msg.nightvein.no_blood.backrooms" : "msg.nightvein.no_blood"), ChatFormatting.GRAY);
            return BiteResult.NO_BLOOD;
        }
        d.drink(blood);
        if (fangs > 0) player.heal(damage * 0.25f * fangs);
        if (victim instanceof ServerPlayer vp) vp.getFoodData().setFoodLevel(Math.max(0, vp.getFoodData().getFoodLevel() - 2));
        Vampires.sync(player);
        if (neck && victim instanceof ServerPlayer vp && Vampires.infect(vp)) return BiteResult.INFECTED;
        return neck ? BiteResult.BIT_NECK : BiteResult.BIT;
    }

    // ------------------------------------------------------------------ dash

    public static int dashCooldown(int level, boolean chromeLegs) {
        int base = level >= 2 ? 30 : 50;
        return chromeLegs ? base / 2 : base;
    }

    /** How hard the client throws itself forward. Movement is client side; the server only checks it was allowed. */
    public static double dashPower(int level, boolean chromeLegs) {
        double p = level >= 2 ? 1.9 : 1.4;
        return chromeLegs ? p * 1.5 : p;
    }

    public static boolean canDash(VampData d) {
        return d.isVampire() && d.level(Skill.DASH) > 0 && d.dashCooldown == 0 && d.blood() >= 1 && !d.bat();
    }

    public static boolean dash(ServerPlayer player) {
        VampData d = Vampires.get(player);
        if (!canDash(d)) return false;
        boolean chrome = ChromefleshCompat.hasCyberlegs(player);
        d.spend(1);
        d.dashCooldown = dashCooldown(d.level(Skill.DASH), chrome);
        d.dashTicks = 12;
        player.resetFallDistance();
        player.level().playSound(null, player.blockPosition(), ModSounds.DASH.get(), SoundSource.PLAYERS, 0.9f, 1f);
        // Held by something from the Backrooms? A dash is a shove: throw it off.
        Entity grabber = VoidClipCompat.grabberOf(player);
        if (grabber instanceof LivingEntity le) Tackle.hit(player, le, player.getViewVector(1f));
        Vampires.sync(player);
        return true;
    }

    // ------------------------------------------------------------------ mist step

    public static double mistRange(int level) {
        return level >= 2 ? 14 : 8;
    }

    /** Where a mist step would land, or null if there's nowhere to stand. Level 2 drifts through walls. */
    @Nullable
    public static Vec3 mistTarget(ServerPlayer player, int level) {
        Vec3 dir = player.getViewVector(1f).normalize();
        double range = mistRange(level);
        Vec3 feet = player.position();
        if (level < 2) {
            Vec3 eye = player.getEyePosition();
            HitResult hit = player.level().clip(new ClipContext(eye, eye.add(dir.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            range = Math.min(range, hit.getLocation().distanceTo(eye) - 0.6);
        }
        for (double dist = range; dist >= 1.5; dist -= 0.5) {
            Vec3 at = feet.add(dir.scale(dist));
            for (double dy : new double[]{0, 1, -1}) {
                Vec3 spot = at.add(0, dy, 0);
                AABB box = player.getDimensions(player.getPose()).makeBoundingBox(spot);
                if (player.level().noCollision(player, box) && !player.level().containsAnyLiquid(box)) return spot;
            }
        }
        return null;
    }

    public static boolean mistStep(ServerPlayer player) {
        VampData d = Vampires.get(player);
        int lv = d.level(Skill.MIST_STEP);
        if (lv == 0 || d.mistCooldown > 0 || d.blood() < 2 || d.bat()) return false;
        Vec3 to = mistTarget(player, lv);
        if (to == null) {
            say(player, Component.translatable("msg.nightvein.mist_blocked"), ChatFormatting.GRAY);
            return false;
        }
        d.spend(2);
        d.mistCooldown = lv >= 2 ? 60 : 100;
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY() + 1, player.getZ(), 12, 0.3, 0.6, 0.3, 0.01);
        level.sendParticles(BLOOD, player.getX(), player.getY() + 1, player.getZ(), 12, 0.3, 0.6, 0.3, 0);
        level.playSound(null, player.blockPosition(), ModSounds.MIST.get(), SoundSource.PLAYERS, 1f, 1f);
        player.teleportTo(to.x, to.y, to.z);
        Tackle.forget(player);
        player.resetFallDistance();
        player.invulnerableTime = 20;
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, to.x, to.y + 1, to.z, 12, 0.3, 0.6, 0.3, 0.01);
        level.playSound(null, BlockPos.containing(to), ModSounds.MIST.get(), SoundSource.PLAYERS, 0.7f, 1.3f);
        Vampires.sync(player);
        return true;
    }

    // ------------------------------------------------------------------ hypnosis

    public enum HypnosisResult { NO_SKILL, COOLDOWN, NO_BLOOD, IMMUNE, MOB, PLAYER }

    public static int hypnosisTicks(int level) {
        return level >= 2 ? 200 : 100;
    }

    /** Bosses have too much will; things from the Backrooms have nothing behind their eyes to reach. */
    public static boolean immuneToHypnosis(LivingEntity e) {
        return e instanceof WitherBoss || e instanceof EnderDragon || e instanceof Warden || VoidClipCompat.isBackroomsEntity(e)
                || Vampires.isVampire(e);
    }

    public static HypnosisResult hypnotize(ServerPlayer player, LivingEntity target) {
        VampData d = Vampires.get(player);
        int lv = d.level(Skill.HYPNOSIS);
        HypnosisResult r;
        if (lv == 0) r = HypnosisResult.NO_SKILL;
        else if (d.hypnosisCooldown > 0) r = HypnosisResult.COOLDOWN;
        else if (d.blood() < 2) r = HypnosisResult.NO_BLOOD;
        else if (immuneToHypnosis(target)) r = HypnosisResult.IMMUNE;
        else if (target instanceof ServerPlayer victim) {
            int t = hypnosisTicks(lv) / 2;
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, t, 3, false, false));
            victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, t + 40, 0, false, false));
            victim.addEffect(new MobEffectInstance(MobEffects.DARKNESS, t, 0, false, false));
            r = HypnosisResult.PLAYER;
        } else {
            Stuns.hypnotize(target, hypnosisTicks(lv));
            r = HypnosisResult.MOB;
        }
        if (r == HypnosisResult.MOB || r == HypnosisResult.PLAYER) {
            d.spend(2);
            d.hypnosisCooldown = lv >= 2 ? 200 : 300;
            player.level().playSound(null, target.blockPosition(), ModSounds.HYPNOTIZE.get(), SoundSource.PLAYERS, 1f, 1f);
            player.serverLevel().sendParticles(ParticleTypes.WITCH, target.getX(), target.getEyeY(), target.getZ(), 14, 0.3, 0.3, 0.3, 0.05);
            Vampires.sync(player);
        } else if (r == HypnosisResult.IMMUNE) {
            say(player, Component.translatable(VoidClipCompat.isBackroomsEntity(target) ? "msg.nightvein.hypnosis.backrooms" : "msg.nightvein.hypnosis.immune"), ChatFormatting.GRAY);
        } else if (r == HypnosisResult.COOLDOWN) {
            say(player, Component.translatable("msg.nightvein.cooldown", (d.hypnosisCooldown + 19) / 20), ChatFormatting.RED);
        }
        return r;
    }

    // ------------------------------------------------------------------ toggles

    public static boolean toggleSense(ServerPlayer player) {
        VampData d = Vampires.get(player);
        if (d.level(Skill.BLOOD_SENSE) == 0) return false;
        d.setBloodSense(!d.bloodSense());
        player.level().playSound(null, player.blockPosition(), ModSounds.HEARTBEAT.get(), SoundSource.PLAYERS, 0.5f, d.bloodSense() ? 1.2f : 0.8f);
        Vampires.sync(player);
        return true;
    }

    public static boolean toggleNightVision(ServerPlayer player) {
        VampData d = Vampires.get(player);
        if (d.level(Skill.NIGHT_VISION) == 0) return false;
        d.setNightVision(!d.nightVision());
        if (!d.nightVision()) player.removeEffect(MobEffects.NIGHT_VISION);
        Vampires.sync(player);
        return true;
    }

    // ------------------------------------------------------------------ aim

    public record Aim(LivingEntity target, Vec3 at) {}

    @Nullable
    public static Aim aim(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1f).scale(range));
        HitResult block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        AABB box = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                e -> !e.isSpectator() && e.isPickable() && e != player && e instanceof LivingEntity, eye.distanceToSqr(end));
        return hit == null ? null : new Aim((LivingEntity) hit.getEntity(), hit.getLocation());
    }

    static void say(ServerPlayer player, Component msg, ChatFormatting color) {
        player.displayClientMessage(msg.copy().withStyle(color), true);
    }
}
