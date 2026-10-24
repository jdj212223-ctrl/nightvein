package com.nightvein.vamp;

import com.nightvein.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/** Small, quick, flying, and unable to hit anything. Costs blood the whole time. */
public final class BatForm {
    private BatForm() {}

    public static float flySpeed(int level) {
        return level >= 2 ? 0.075f : 0.045f;
    }

    /** Ticks per point of blood. */
    public static int drainInterval(int level) {
        return level >= 2 ? 400 : 200;
    }

    public static boolean enter(ServerPlayer player) {
        VampData d = Vampires.get(player);
        int lv = Vampires.level(player, Skill.BAT_FORM);
        if (lv == 0 || d.bat() || d.blood() < 1) return false;
        d.setBat(true);
        apply(player, lv);
        poof(player);
        Vampires.refresh(player);
        Vampires.sync(player);
        return true;
    }

    /** Puts flight back on after login or respawn while still a bat. */
    public static void apply(ServerPlayer player, int level) {
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.getAbilities().setFlyingSpeed(flySpeed(level));
        player.onUpdateAbilities();
    }

    public static void leave(ServerPlayer player) {
        VampData d = Vampires.get(player);
        d.setBat(false);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
        }
        player.getAbilities().setFlyingSpeed(0.05f);
        player.onUpdateAbilities();
        player.resetFallDistance();
        poof(player);
        Vampires.refresh(player);
        Vampires.sync(player);
    }

    private static void poof(ServerPlayer player) {
        player.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 0.5, player.getZ(), 16, 0.3, 0.4, 0.3, 0.02);
        player.level().playSound(null, player.blockPosition(), ModSounds.BAT.get(), SoundSource.PLAYERS, 0.8f, 1f);
    }
}
