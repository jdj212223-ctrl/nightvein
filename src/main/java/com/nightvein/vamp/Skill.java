package com.nightvein.vamp;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * The skill tree. Every level costs one point, and points come from blood drunk ({@link VampData#points()}).
 * {@code column}/{@code row} place it in the skill screen; a skill needs one level of the one above it in its column.
 */
public enum Skill implements StringRepresentable {
    SPEED(3, 0, 0, null),
    DASH(2, 0, 1, SPEED),
    MIST_STEP(2, 0, 2, DASH),

    NIGHT_VISION(1, 1, 0, null),
    BAT_FORM(2, 1, 1, NIGHT_VISION),

    FANGS(3, 2, 0, null),
    REGEN(3, 2, 1, FANGS),

    BLOOD_SENSE(2, 3, 0, null),
    HYPNOSIS(2, 3, 1, BLOOD_SENSE),

    SUN_WARD(3, 4, 0, null);

    public static final com.mojang.serialization.Codec<Skill> CODEC = StringRepresentable.fromEnum(Skill::values);

    public final int maxLevel;
    public final int column;
    public final int row;
    @Nullable
    public final Skill requires;

    Skill(int maxLevel, int column, int row, @Nullable Skill requires) {
        this.maxLevel = maxLevel;
        this.column = column;
        this.row = row;
        this.requires = requires;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getSerializedName() {
        return id();
    }

    public Component title() {
        return Component.translatable("skill.nightvein." + id());
    }

    /** What the next level (1-based) gives you. */
    public Component description(int level) {
        return Component.translatable("skill.nightvein." + id() + "." + Math.max(1, Math.min(level, maxLevel)));
    }
}
