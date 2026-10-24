package com.nightvein.network;

import com.nightvein.Nightvein;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** A key the player pressed, or a skill they clicked in the skill screen ({@code UPGRADE + skill ordinal}). */
public record ActionPayload(int action) implements CustomPacketPayload {
    public static final int BITE = 0, DASH = 1, MIST = 2, HYPNOSIS = 3, BAT = 4, BLOOD_SENSE = 5, NIGHT_VISION = 6;
    public static final int UPGRADE = 100;
    public static final Type<ActionPayload> TYPE = new Type<>(Nightvein.id("action"));
    public static final StreamCodec<ByteBuf, ActionPayload> CODEC = ByteBufCodecs.VAR_INT.map(ActionPayload::new, ActionPayload::action);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
