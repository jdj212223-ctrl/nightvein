package com.nightvein.vamp;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum Stage implements StringRepresentable {
    HUMAN, TURNING, VAMPIRE;

    public static final com.mojang.serialization.Codec<Stage> CODEC = StringRepresentable.fromEnum(Stage::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
