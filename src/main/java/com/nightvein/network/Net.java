package com.nightvein.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

/** Sends only to connections that negotiated our channels: fake players and test players have none. */
public final class Net {
    private Net() {}

    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection != null && player.connection.hasChannel(payload)) PacketDistributor.sendToPlayer(player, payload);
    }

    /** To everyone close enough to see {@code about}, and to {@code about} itself if it's a player. */
    public static void sendAround(Entity about, CustomPacketPayload payload) {
        if (!(about.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p == about || p.distanceToSqr(about) < 160 * 160) send(p, payload);
        }
    }
}
