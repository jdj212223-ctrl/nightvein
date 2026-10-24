package com.nightvein.vamp;

import com.nightvein.compat.VoidClipCompat;
import com.nightvein.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Run into something fast enough (a dash, or a top-speed sprint) and you hit it like a truck. Bacteria from liminal.
 * goes down flat for a few seconds and lets go of whoever it was holding.
 */
public final class Tackle {
    /** Blocks per tick. A plain sprint is about 0.28; speed 3 or a chrome build gets you past this. */
    public static final double TACKLE_SPEED = 0.42;
    public static final int DOWNED_TICKS = 100;

    private static final Map<UUID, Vec3> LAST = new HashMap<>();
    /** Who each player last hit, and when, so one run-through counts once. */
    private static final Map<UUID, Map<Integer, Long>> RECENT = new HashMap<>();

    private Tackle() {}

    /** How far the player moved sideways since last tick (movement is client side, so the server measures it). */
    public static Vec3 moved(ServerPlayer player) {
        Vec3 now = player.position();
        Vec3 last = LAST.put(player.getUUID(), now);
        return last == null ? Vec3.ZERO : now.subtract(last).multiply(1, 0, 1);
    }

    public static void forget(ServerPlayer player) {
        LAST.remove(player.getUUID());
        RECENT.remove(player.getUUID());
    }

    /** Called every tick for vampires. */
    public static void tick(ServerPlayer player, VampData d) {
        Vec3 dir = moved(player);
        boolean fast = d.dashTicks > 0 || (player.isSprinting() && dir.length() >= TACKLE_SPEED);
        if (!fast || d.bat()) return;
        if (dir.lengthSqr() < 1e-4) dir = player.getViewVector(1f).multiply(1, 0, 1);
        AABB reach = player.getBoundingBox().inflate(0.4).expandTowards(dir.normalize().scale(0.6));
        long now = player.level().getGameTime();
        Map<Integer, Long> recent = RECENT.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        recent.values().removeIf(t -> now - t > 20);
        for (Entity e : player.level().getEntities(player, reach, e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator())) {
            if (recent.containsKey(e.getId())) continue;
            recent.put(e.getId(), now);
            hit(player, (LivingEntity) e, dir);
        }
    }

    /** Slam into {@code target}. Returns true if it was knocked down (only Bacteria goes down). */
    public static boolean hit(ServerPlayer player, LivingEntity target, Vec3 dir) {
        Vec3 push = dir.multiply(1, 0, 1);
        push = push.lengthSqr() < 1e-4 ? target.position().subtract(player.position()).multiply(1, 0, 1) : push;
        push = push.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : push.normalize();
        boolean bacteria = VoidClipCompat.isBacteria(target);
        int speed = Vampires.level(player, Skill.SPEED);
        float damage = 3 + speed;
        target.hurt(player.damageSources().playerAttack(player), bacteria ? damage + 3 : damage);
        // Set the motion directly: Bacteria shrugs off knockback entirely, but not this.
        double strength = bacteria ? 1.6 : 1.1;
        target.setDeltaMovement(push.x * strength, 0.45, push.z * strength);
        target.hurtMarked = true;
        player.level().playSound(null, target.blockPosition(), ModSounds.TACKLE.get(), SoundSource.PLAYERS, 1.1f, bacteria ? 0.7f : 1f);
        player.serverLevel().sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 14, 0.3, 0.3, 0.3, 0.3);
        if (!bacteria) return false;
        Stuns.down(target, DOWNED_TICKS);
        int freed = VoidClipCompat.releaseGrabsBy(target);
        Powers.say(player, net.minecraft.network.chat.Component.translatable(freed > 0 ? "msg.nightvein.tackle.freed" : "msg.nightvein.tackle"),
                net.minecraft.ChatFormatting.DARK_RED);
        return true;
    }
}
