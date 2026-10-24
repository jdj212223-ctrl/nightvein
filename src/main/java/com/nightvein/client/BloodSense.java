package com.nightvein.client;

import com.nightvein.vamp.Skill;
import com.nightvein.vamp.VampData;
import com.nightvein.vamp.Vampires;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Client side. Which entities the local player can feel through walls, and in what colour. */
public final class BloodSense {
    public static final int RED = 0xD0142C;
    public static final int KIN = 0x9B30FF;
    /** Things with no blood in them: undead, golems, and everything from the Backrooms (whatever it's pretending to be). */
    public static final int EMPTY = 0x9A9A9A;

    private BloodSense() {}

    public static double range(int level) {
        return level >= 2 ? 32 : 16;
    }

    public static boolean senses(Entity e) {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null || e == me || !(e instanceof LivingEntity) || !e.isAlive()) return false;
        VampData d = Vampires.get(me);
        if (!d.isVampire() || !d.bloodSense()) return false;
        double r = range(d.level(Skill.BLOOD_SENSE));
        return d.level(Skill.BLOOD_SENSE) > 0 && e.distanceToSqr(me) < r * r;
    }

    public static int colour(Entity e) {
        if (Vampires.isVampire(e)) return KIN;
        return e instanceof LivingEntity le && Vampires.bloodIn(le) > 0 ? RED : EMPTY;
    }
}
