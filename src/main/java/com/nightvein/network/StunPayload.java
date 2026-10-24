package com.nightvein.network;

import com.nightvein.Nightvein;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** A mob was knocked flat (tackled) or put under (hypnotised), so clients can draw it that way. */
public record StunPayload(int entityId, int ticks, boolean downed) implements CustomPacketPayload {
    public static final Type<StunPayload> TYPE = new Type<>(Nightvein.id("stun"));
    public static final StreamCodec<ByteBuf, StunPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StunPayload::entityId,
            ByteBufCodecs.VAR_INT, StunPayload::ticks,
            ByteBufCodecs.BOOL, StunPayload::downed,
            StunPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
