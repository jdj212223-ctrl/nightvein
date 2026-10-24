package com.nightvein.vamp;

import com.nightvein.Nightvein;
import com.nightvein.network.Net;
import com.nightvein.network.StunPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mobs that can't act for a while: knocked flat by a tackle, or hypnotised. Their speed is zeroed with a transient
 * modifier (so nothing is left behind if the world saves mid-stun) and their target is wiped every tick.
 * Physics still apply, so a tackled mob still flies.
 */
public final class Stuns {
    private static final ResourceLocation FROZEN = Nightvein.id("stunned");
    private static final Map<UUID, Stun> STUNS = new ConcurrentHashMap<>();

    private record Stun(ServerLevel level, int entityId, long until, boolean downed) {}

    private Stuns() {}

    public static void down(LivingEntity e, int ticks) {
        apply(e, ticks, true);
    }

    public static void hypnotize(LivingEntity e, int ticks) {
        apply(e, ticks, false);
    }

    private static void apply(LivingEntity e, int ticks, boolean downed) {
        if (!(e.level() instanceof ServerLevel level)) return;
        STUNS.put(e.getUUID(), new Stun(level, e.getId(), level.getGameTime() + ticks, downed));
        AttributeInstance speed = e.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(FROZEN)) {
            speed.addTransientModifier(new AttributeModifier(FROZEN, -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        calm(e);
        Net.sendAround(e, new StunPayload(e.getId(), ticks, downed));
    }

    public static boolean isStunned(LivingEntity e) {
        Stun s = STUNS.get(e.getUUID());
        return s != null && s.level.getGameTime() < s.until;
    }

    public static boolean isDowned(LivingEntity e) {
        Stun s = STUNS.get(e.getUUID());
        return s != null && s.downed && s.level.getGameTime() < s.until;
    }

    private static void calm(LivingEntity e) {
        if (e instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.setAggressive(false);
        }
    }

    public static void release(LivingEntity e) {
        STUNS.remove(e.getUUID());
        AttributeInstance speed = e.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(FROZEN);
    }

    /** Once per server tick. */
    public static void tick() {
        Iterator<Map.Entry<UUID, Stun>> it = STUNS.entrySet().iterator();
        while (it.hasNext()) {
            Stun s = it.next().getValue();
            if (!(s.level.getEntity(s.entityId) instanceof LivingEntity e) || !e.isAlive()) {
                it.remove();
                continue;
            }
            if (s.level.getGameTime() >= s.until) {
                it.remove();
                AttributeInstance speed = e.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null) speed.removeModifier(FROZEN);
            } else {
                calm(e);
            }
        }
    }
}
