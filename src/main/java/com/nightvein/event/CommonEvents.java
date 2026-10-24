package com.nightvein.event;

import com.nightvein.Nightvein;
import com.nightvein.command.NightveinCommand;
import com.nightvein.registry.ModItems;
import com.nightvein.registry.ModSounds;
import com.nightvein.vamp.BatForm;
import com.nightvein.vamp.Skill;
import com.nightvein.vamp.Stage;
import com.nightvein.vamp.Stuns;
import com.nightvein.vamp.Tackle;
import com.nightvein.vamp.VampData;
import com.nightvein.vamp.VampTicker;
import com.nightvein.vamp.Vampires;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Nightvein.MODID)
public final class CommonEvents {
    private CommonEvents() {}

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p) VampTicker.tick(p);
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post e) {
        Stuns.tick();
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) restore(p);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Tackle.forget(p);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        VampData d = Vampires.get(p);
        // Death doesn't cure you. Dying while turning just finishes it.
        if (d.stage() == Stage.TURNING) Vampires.completeTurning(p);
        if (d.bat()) d.setBat(false);
        if (d.isVampire()) d.setBlood(Math.max(d.blood(), 6));
        restore(p);
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) restore(p);
    }

    private static void restore(ServerPlayer p) {
        Vampires.refresh(p);
        VampData d = Vampires.get(p);
        if (d.isVampire() && d.bat()) BatForm.apply(p, d.level(Skill.BAT_FORM));
        Vampires.sync(p);
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking e) {
        if (e.getEntity() instanceof ServerPlayer viewer && e.getTarget() instanceof Player target) Vampires.syncTo(viewer, target);
    }

    /** Bats can't fight or dig. */
    @SubscribeEvent
    public static void attack(AttackEntityEvent e) {
        if (Vampires.get(e.getEntity()).bat()) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed e) {
        if (Vampires.get(e.getEntity()).bat()) e.setNewSpeed(0);
    }

    /** A vampire sneaking with a glass bottle draws blood from whatever they use it on. */
    @SubscribeEvent
    public static void fillBottle(PlayerInteractEvent.EntityInteract e) {
        Player player = e.getEntity();
        ItemStack held = e.getItemStack();
        if (!held.is(Items.GLASS_BOTTLE) || !player.isShiftKeyDown() || !(e.getTarget() instanceof LivingEntity target)) return;
        if (!Vampires.get(player).isVampire() || Vampires.bloodIn(target) < 1) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
        if (player.level().isClientSide) return;
        target.hurt(player.damageSources().playerAttack(player), 3);
        player.level().playSound(null, target.blockPosition(), ModSounds.BITE.get(), SoundSource.PLAYERS, 0.6f, 1.4f);
        if (!player.hasInfiniteMaterials()) held.shrink(1);
        ItemStack blood = new ItemStack(ModItems.BLOOD_BOTTLE.get());
        if (!player.getInventory().add(blood)) player.drop(blood, false);
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        NightveinCommand.register(e.getDispatcher());
    }
}
