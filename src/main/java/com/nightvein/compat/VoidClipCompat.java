package com.nightvein.compat;

import com.nightvein.Nightvein;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

/**
 * Hooks into liminal. (VoidClip) by reflection so it stays optional. Bacteria can be tackled flat by a vampire running
 * fast enough, and knocking it down shakes loose whoever it was holding. The grab itself is blocked by
 * {@link com.nightvein.mixin.compat.GrabManagerMixin} while it's down.
 */
public final class VoidClipCompat {
    public static final boolean LOADED = ModList.get() != null && ModList.get().isLoaded("voidclip");
    public static final String NAMESPACE = "voidclip";

    private static Map<UUID, Object> grabs;
    private static Field grabberId;
    private static Field progress;
    private static Method isGrabbed;
    private static boolean broken;

    private VoidClipCompat() {}

    public static boolean isBacteria(Entity e) {
        return LOADED && BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).equals(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NAMESPACE, "bacteria"));
    }

    /** Anything from the Backrooms. None of them have blood, and none of them have minds you can get into. */
    public static boolean isBackroomsEntity(Entity e) {
        return LOADED && BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace().equals(NAMESPACE);
    }

    @SuppressWarnings("unchecked")
    private static boolean ready() {
        if (!LOADED || broken) return false;
        if (grabs != null) return true;
        try {
            Class<?> manager = Class.forName("com.voidclip.entity.GrabManager");
            Field f = manager.getDeclaredField("GRABS");
            f.setAccessible(true);
            Class<?> grab = Class.forName("com.voidclip.entity.GrabManager$Grab");
            grabberId = grab.getDeclaredField("grabberId");
            grabberId.setAccessible(true);
            progress = grab.getDeclaredField("progress");
            progress.setAccessible(true);
            isGrabbed = manager.getMethod("isGrabbed", Player.class);
            grabs = (Map<UUID, Object>) f.get(null);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            Nightvein.LOGGER.warn("Couldn't reach VoidClip's grabs; tackling Bacteria won't free anyone", e);
            broken = true;
            grabs = null;
            return false;
        }
    }

    public static boolean isGrabbed(Player player) {
        if (!ready()) return false;
        try {
            return (boolean) isGrabbed.invoke(null, player);
        } catch (ReflectiveOperationException e) {
            broken = true;
            return false;
        }
    }

    /** The entity holding this player, if any. */
    @Nullable
    public static Entity grabberOf(ServerPlayer player) {
        if (!ready()) return null;
        Object g = grabs.get(player.getUUID());
        if (g == null) return null;
        try {
            return player.level().getEntity(grabberId.getInt(g));
        } catch (IllegalAccessException e) {
            broken = true;
            return null;
        }
    }

    /**
     * Breaks every grab {@code grabber} has. Done by filling the struggle bar rather than deleting the grab, so on its
     * next tick VoidClip runs its own escape: the client is told, the advancement fires, and Bacteria lets them go.
     * Returns how many were let go.
     */
    public static int releaseGrabsBy(Entity grabber) {
        if (!ready()) return 0;
        int n = 0;
        try {
            for (Object g : grabs.values()) {
                if (grabberId.getInt(g) == grabber.getId()) {
                    progress.setFloat(g, 1f);
                    n++;
                }
            }
        } catch (IllegalAccessException e) {
            Nightvein.LOGGER.warn("Couldn't break VoidClip grab", e);
            broken = true;
        }
        return n;
    }
}
