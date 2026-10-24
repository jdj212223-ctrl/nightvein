package com.nightvein.network;

import com.nightvein.Nightvein;
import com.nightvein.vamp.VampData;
import com.nightvein.vamp.Vampires;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

/**
 * One player's vampire state, plus the cooldowns the HUD shows (only meaningful for the player themselves).
 * {@code rush} is -1 while Bloodrushing, otherwise the ticks left of a crash.
 */
public record SyncVampPayload(int entityId, CompoundTag data, int dashCooldown, int mistCooldown, int hypnosisCooldown,
                              int rush) implements CustomPacketPayload {
    public static final Type<SyncVampPayload> TYPE = new Type<>(Nightvein.id("sync"));
    public static final StreamCodec<ByteBuf, SyncVampPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncVampPayload::entityId,
            ByteBufCodecs.COMPOUND_TAG, SyncVampPayload::data,
            ByteBufCodecs.VAR_INT, SyncVampPayload::dashCooldown,
            ByteBufCodecs.VAR_INT, SyncVampPayload::mistCooldown,
            ByteBufCodecs.VAR_INT, SyncVampPayload::hypnosisCooldown,
            ByteBufCodecs.VAR_INT, SyncVampPayload::rush,
            SyncVampPayload::new);

    public static SyncVampPayload of(Player player) {
        VampData d = Vampires.get(player);
        Tag tag = VampData.CODEC.encodeStart(NbtOps.INSTANCE, d).getOrThrow();
        return new SyncVampPayload(player.getId(), (CompoundTag) tag, d.dashCooldown, d.mistCooldown, d.hypnosisCooldown, d.rushing ? -1 : d.crash);
    }

    public VampData decode() {
        VampData d = VampData.CODEC.parse(NbtOps.INSTANCE, data).result().orElseGet(VampData::new);
        d.dashCooldown = dashCooldown;
        d.mistCooldown = mistCooldown;
        d.hypnosisCooldown = hypnosisCooldown;
        d.rushing = rush < 0;
        d.crash = Math.max(0, rush);
        return d;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
