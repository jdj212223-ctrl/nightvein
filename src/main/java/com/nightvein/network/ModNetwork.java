package com.nightvein.network;

import com.nightvein.Nightvein;
import com.nightvein.client.ClientPayloads;
import com.nightvein.vamp.Powers;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Nightvein.MODID)
public final class ModNetwork {
    private ModNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        // Client handlers live in nested lambdas so the client class is only linked on the client.
        r.playToClient(SyncVampPayload.TYPE, SyncVampPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> ClientPayloads.sync(p)));
        r.playToClient(StunPayload.TYPE, StunPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> ClientPayloads.stun(p)));
        r.playToServer(ActionPayload.TYPE, ActionPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) Powers.handle(sp, p.action());
        }));
    }
}
