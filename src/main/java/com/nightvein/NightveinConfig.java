package com.nightvein;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Per-world server config (serverconfig/nightvein-server.toml), so whoever hosts the LAN world sets it. */
public final class NightveinConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue TURNING_SECONDS;
    public static final ModConfigSpec.BooleanValue SUN_BURNS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        TURNING_SECONDS = b.comment("How long the turning lasts after a bite to the neck, in seconds. A Hawthorn Tincture cures it any time before the end.")
                .defineInRange("turningSeconds", 180, 10, 60 * 60);
        SUN_BURNS = b.comment("Whether daylight sets young vampires on fire. Off: the sun only weakens them.")
                .define("sunBurns", true);
        SPEC = b.build();
    }

    private NightveinConfig() {}

    /** Config values throw before the world loads (and in some tests), so fall back to the defaults. */
    public static int turningTicks() {
        return SPEC.isLoaded() ? TURNING_SECONDS.get() * 20 : 180 * 20;
    }

    public static boolean sunBurns() {
        return !SPEC.isLoaded() || SUN_BURNS.get();
    }
}
