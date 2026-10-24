package com.nightvein.compat;

import com.nightvein.Nightvein;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

/**
 * Reads Chromeflesh implants by reflection so it stays optional. The implants that make you faster
 * (leg muscle, cyberlegs, cyberheart) are what Bloodrush feeds on.
 */
public final class ChromefleshCompat {
    public static final boolean LOADED = ModList.get() != null && ModList.get().isLoaded("chromeflesh");
    /** Each of these counts once towards Bloodrush. */
    public static final String[] SPEED_IMPLANTS = {"LEG_MUSCLE", "CYBERLEGS", "CYBERHEART"};

    private static Method has;
    private static Object[] speedImplants;
    private static Object cyberlegs;
    private static boolean broken;

    private ChromefleshCompat() {}

    private static boolean ready() {
        if (!LOADED || broken) return false;
        if (has != null) return true;
        try {
            Class<?> implant = Class.forName("com.chromeflesh.implant.Implant");
            has = Class.forName("com.chromeflesh.implant.Implants").getMethod("has", Player.class, implant);
            speedImplants = new Object[SPEED_IMPLANTS.length];
            for (int i = 0; i < SPEED_IMPLANTS.length; i++) speedImplants[i] = implant.getField(SPEED_IMPLANTS[i]).get(null);
            cyberlegs = implant.getField("CYBERLEGS").get(null);
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            Nightvein.LOGGER.warn("Couldn't reach Chromeflesh's implants; Bloodrush is off", e);
            broken = true;
            has = null;
            return false;
        }
    }

    private static boolean has(Player player, Object implant) {
        try {
            return (boolean) has.invoke(null, player, implant);
        } catch (ReflectiveOperationException | ClassCastException e) {
            Nightvein.LOGGER.warn("Chromeflesh implant check failed; Bloodrush is off", e);
            broken = true;
            return false;
        }
    }

    /** How many speed implants this player has installed (0 without Chromeflesh). */
    public static int speedImplants(Player player) {
        if (!ready()) return 0;
        int n = 0;
        for (Object implant : speedImplants) if (has(player, implant)) n++;
        return n;
    }

    /** Chrome legs throw a dash further and recover from it sooner. */
    public static boolean hasCyberlegs(Player player) {
        return ready() && has(player, cyberlegs);
    }
}
