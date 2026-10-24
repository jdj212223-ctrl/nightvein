package com.nightvein.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class Keys {
    public static final String CATEGORY = "key.categories.nightvein";
    public static final KeyMapping BITE = key("bite", GLFW.GLFW_KEY_V);
    public static final KeyMapping DASH = key("dash", GLFW.GLFW_KEY_R);
    public static final KeyMapping MIST = key("mist_step", GLFW.GLFW_KEY_G);
    public static final KeyMapping HYPNOSIS = key("hypnosis", GLFW.GLFW_KEY_X);
    public static final KeyMapping BAT = key("bat_form", GLFW.GLFW_KEY_B);
    public static final KeyMapping BLOOD_SENSE = key("blood_sense", GLFW.GLFW_KEY_C);
    public static final KeyMapping NIGHT_VISION = key("night_vision", GLFW.GLFW_KEY_N);
    public static final KeyMapping SKILLS = key("skills", GLFW.GLFW_KEY_K);
    public static final KeyMapping[] ALL = {BITE, DASH, MIST, HYPNOSIS, BAT, BLOOD_SENSE, NIGHT_VISION, SKILLS};

    private static KeyMapping key(String name, int key) {
        return new KeyMapping("key.nightvein." + name, InputConstants.Type.KEYSYM, key, CATEGORY);
    }

    private Keys() {}
}
