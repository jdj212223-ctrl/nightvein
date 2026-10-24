package com.nightvein.client;

import com.mojang.math.Axis;
import com.nightvein.Nightvein;
import com.nightvein.client.gui.SkillScreen;
import com.nightvein.compat.ChromefleshCompat;
import com.nightvein.network.ActionPayload;
import com.nightvein.registry.ModSounds;
import com.nightvein.vamp.Powers;
import com.nightvein.vamp.Skill;
import com.nightvein.vamp.VampData;
import com.nightvein.vamp.Vampires;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = Nightvein.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private static final Map<Integer, Bat> BATS = new HashMap<>();
    /** Entities whose pose we pushed in Pre and must pop in Post. */
    private static final Set<Integer> PUSHED = new HashSet<>();
    private static int nextBeat;

    private ClientEvents() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer me = mc.player;
        if (me == null) {
            BATS.clear();
            return;
        }
        VampData d = Vampires.get(me);
        while (Keys.SKILLS.consumeClick()) mc.setScreen(new SkillScreen());
        if (d.isVampire()) {
            while (Keys.BITE.consumeClick()) send(ActionPayload.BITE);
            while (Keys.DASH.consumeClick()) dash(me, d);
            while (Keys.MIST.consumeClick()) send(ActionPayload.MIST);
            while (Keys.HYPNOSIS.consumeClick()) send(ActionPayload.HYPNOSIS);
            while (Keys.BAT.consumeClick()) send(ActionPayload.BAT);
            while (Keys.BLOOD_SENSE.consumeClick()) send(ActionPayload.BLOOD_SENSE);
            while (Keys.NIGHT_VISION.consumeClick()) send(ActionPayload.NIGHT_VISION);
        } else {
            for (var k : Keys.ALL) if (k != Keys.SKILLS) while (k.consumeClick()) { }
        }
        heartbeat(mc, d);
    }

    private static void send(int action) {
        PacketDistributor.sendToServer(new ActionPayload(action));
    }

    /** Movement is ours to make: throw ourselves forward, then tell the server so it can charge for it. */
    private static void dash(LocalPlayer me, VampData d) {
        if (!Powers.canDash(d)) return;
        double power = Powers.dashPower(d.level(Skill.DASH), ChromefleshCompat.hasCyberlegs(me));
        Vec3 look = me.getLookAngle().multiply(1, 0, 1);
        if (look.lengthSqr() < 1e-4) return;
        look = look.normalize().scale(power);
        me.setDeltaMovement(look.x, me.onGround() ? 0.25 : Math.max(me.getDeltaMovement().y, 0.1), look.z);
        d.dashCooldown = Powers.dashCooldown(d.level(Skill.DASH), ChromefleshCompat.hasCyberlegs(me));
        send(ActionPayload.DASH);
    }

    /** The turning, from the inside: a heartbeat that gets louder and faster. */
    private static void heartbeat(Minecraft mc, VampData d) {
        if (!d.isTurning() && !(d.isVampire() && d.blood() <= 2)) return;
        if (--nextBeat > 0) return;
        float p = d.isTurning() ? Vampires.turningProgress(d) : 0.6f;
        nextBeat = Math.round(34 - 18 * p);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.HEARTBEAT.get(), 1f, 0.25f + 0.65f * p));
    }

    /** Vampires drink, they don't eat: their blood bar replaces the food bar. */
    @SubscribeEvent
    public static void hideFood(RenderGuiLayerEvent.Pre e) {
        if (!e.getName().equals(VanillaGuiLayers.FOOD_LEVEL)) return;
        LocalPlayer me = Minecraft.getInstance().player;
        if (me != null && Vampires.get(me).isVampire()) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void hand(RenderHandEvent e) {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me != null && Vampires.get(me).bat()) e.setCanceled(true);
    }

    /** A player in bat form is drawn as a bat. */
    @SubscribeEvent
    public static void renderPlayer(RenderPlayerEvent.Pre e) {
        Player p = e.getEntity();
        if (!Vampires.get(p).bat()) return;
        e.setCanceled(true);
        Bat bat = BATS.computeIfAbsent(p.getId(), id -> {
            Bat b = EntityType.BAT.create(p.level());
            if (b != null) b.setResting(false);
            return b;
        });
        if (bat == null) return;
        bat.tickCount = p.tickCount;
        bat.setPos(p.getX(), p.getY(), p.getZ());
        bat.yBodyRot = bat.yBodyRotO = p.yBodyRot;
        bat.yHeadRot = bat.yHeadRotO = p.yHeadRot;
        bat.setYRot(p.getYRot());
        bat.setXRot(p.getXRot());
        bat.flyAnimationState.startIfStopped(bat.tickCount);
        e.getPoseStack().pushPose();
        e.getPoseStack().translate(0, -0.3, 0);
        Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(bat)
                .render(bat, p.getYRot(), e.getPartialTick(), e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight());
        e.getPoseStack().popPose();
    }

    /** Tackled flat on its back. */
    @SubscribeEvent
    public static void renderLivingPre(RenderLivingEvent.Pre<?, ?> e) {
        if (!ClientPayloads.downed(e.getEntity())) return;
        var pose = e.getPoseStack();
        pose.pushPose();
        PUSHED.add(e.getEntity().getId());
        pose.translate(0, e.getEntity().getBbWidth() / 2, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-e.getEntity().yBodyRot));
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.mulPose(Axis.YP.rotationDegrees(e.getEntity().yBodyRot));
    }

    @SubscribeEvent
    public static void renderLivingPost(RenderLivingEvent.Post<?, ?> e) {
        if (PUSHED.remove(e.getEntity().getId())) e.getPoseStack().popPose();
    }
}
