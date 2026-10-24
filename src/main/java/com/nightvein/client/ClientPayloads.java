package com.nightvein.client;

import com.nightvein.network.StunPayload;
import com.nightvein.network.SyncVampPayload;
import com.nightvein.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/** Client side. */
public final class ClientPayloads {
    /** Tackled mobs: entity id to the game time they get back up. */
    static final Map<Integer, Long> DOWNED = new HashMap<>();

    private ClientPayloads() {}

    public static void sync(SyncVampPayload p) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity e = level.getEntity(p.entityId());
        if (e instanceof Player player) player.setData(ModAttachments.VAMP, p.decode());
    }

    public static void stun(StunPayload p) {
        var level = Minecraft.getInstance().level;
        if (level == null || !p.downed()) return;
        DOWNED.put(p.entityId(), level.getGameTime() + p.ticks());
    }

    public static boolean downed(Entity e) {
        Long until = DOWNED.get(e.getId());
        if (until == null) return false;
        if (e.level().getGameTime() >= until) {
            DOWNED.remove(e.getId());
            return false;
        }
        return true;
    }
}
