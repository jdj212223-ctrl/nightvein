package com.nightvein.entity;

import com.nightvein.registry.ModDamageTypes;
import com.nightvein.registry.ModSounds;
import com.nightvein.vamp.Powers;
import com.nightvein.vamp.Vampires;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * A pale thing in a long coat that walks at night. It doesn't come at you head on: it circles until it's behind you,
 * and a bite from behind goes into the neck. It leaves other vampires alone and burns in the sun.
 */
public class Nightwalker extends Monster {
    public Nightwalker(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.ARMOR, 2);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new RestrictSunGoal(this));
        goalSelector.addGoal(2, new FleeSunGoal(this, 1.2));
        goalSelector.addGoal(3, new CircleBehindGoal());
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                e -> e instanceof Player p && !Vampires.get(p).isVampire()));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
    }

    @Override
    public void aiStep() {
        if (isAlive() && isSunBurnTick()) igniteForSeconds(8);
        super.aiStep();
    }

    /** Behind you, it bites; anywhere else it claws. */
    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof LivingEntity victim && Powers.behind(this, victim)) return bite(victim);
        return super.doHurtTarget(target);
    }

    public boolean bite(LivingEntity victim) {
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        level().playSound(null, victim.blockPosition(), ModSounds.BITE.get(), SoundSource.HOSTILE, 1f, 0.8f);
        boolean hurt = victim.hurt(ModDamageTypes.source(level(), ModDamageTypes.BITE, this), 5);
        if (hurt) heal(3);
        if (victim instanceof ServerPlayer p && !p.isBlocking()) Vampires.infect(p);
        return hurt;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Fire hurts it twice as much; it is mostly dead already.
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) amount *= 2;
        return super.hurt(source, amount);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.NIGHTWALKER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.NIGHTWALKER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.NIGHTWALKER_DEATH.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    /** Close in, but swing round to its back before going for the throat. Gives up and attacks if it takes too long. */
    private class CircleBehindGoal extends Goal {
        private int ticks;
        private int restUntil;

        CircleBehindGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return tickCount >= restUntil && t instanceof Player && t.isAlive() && distanceToSqr(t) < 10 * 10
                    && !Powers.behind(Nightwalker.this, t);
        }

        @Override
        public void start() {
            ticks = 0;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() && ticks < 120;
        }

        @Override
        public void stop() {
            if (ticks >= 120) restUntil = tickCount + 100;
        }

        @Override
        public void tick() {
            ticks++;
            LivingEntity t = getTarget();
            if (t == null) return;
            float yaw = t.yBodyRot * Mth.DEG_TO_RAD;
            Vec3 back = t.position().add(Mth.sin(yaw) * 1.6, 0, -Mth.cos(yaw) * 1.6);
            // Swing wide, so it goes round you rather than through you.
            Vec3 side = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw)).scale(position().subtract(t.position()).dot(new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw))) >= 0 ? 2.5 : -2.5);
            Vec3 aim = distanceToSqr(back) > 9 ? back.add(side) : back;
            getNavigation().moveTo(aim.x, aim.y, aim.z, 1.3);
            getLookControl().setLookAt(t, 30, 30);
        }
    }
}
