package com.nightvein.vamp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;

/**
 * Everything Nightvein knows about one player. Saved with the player and copied across death; cooldowns and the
 * Bloodrush state are not saved. The client holds whatever the server last synced.
 */
public final class VampData {
    public static final float MAX_BLOOD = 20f;

    public static final Codec<VampData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Stage.CODEC.optionalFieldOf("stage", Stage.HUMAN).forGetter(VampData::stage),
            Codec.INT.optionalFieldOf("turning", 0).forGetter(VampData::turning),
            Codec.FLOAT.optionalFieldOf("blood", 0f).forGetter(VampData::blood),
            Codec.FLOAT.optionalFieldOf("drunk", 0f).forGetter(VampData::drunk),
            Codec.unboundedMap(Skill.CODEC, Codec.INT).optionalFieldOf("skills", Map.of()).forGetter(d -> d.skills),
            Codec.BOOL.optionalFieldOf("night_vision", false).forGetter(VampData::nightVision),
            Codec.BOOL.optionalFieldOf("blood_sense", false).forGetter(VampData::bloodSense),
            Codec.BOOL.optionalFieldOf("bat", false).forGetter(VampData::bat)
    ).apply(i, VampData::new));

    private Stage stage;
    private int turning;
    private float blood;
    private float drunk;
    private final EnumMap<Skill, Integer> skills = new EnumMap<>(Skill.class);
    private boolean nightVision;
    private boolean bloodSense;
    private boolean bat;

    // Not saved.
    public int biteCooldown;
    public int dashCooldown;
    public int mistCooldown;
    public int hypnosisCooldown;
    /** Ticks left of a dash, during which anything you run into gets tackled. */
    public int dashTicks;
    /** Ticks left of a Bloodrush crash (ran the chrome dry). */
    public int crash;
    public boolean rushing;

    public VampData() {
        this(Stage.HUMAN, 0, 0f, 0f, Map.of(), false, false, false);
    }

    public VampData(Stage stage, int turning, float blood, float drunk, Map<Skill, Integer> skills,
                    boolean nightVision, boolean bloodSense, boolean bat) {
        this.stage = stage;
        this.turning = turning;
        this.blood = blood;
        this.drunk = drunk;
        skills.forEach((s, l) -> {
            if (l > 0) this.skills.put(s, Math.min(l, s.maxLevel));
        });
        this.nightVision = nightVision;
        this.bloodSense = bloodSense;
        this.bat = bat;
    }

    public Stage stage() {
        return stage;
    }

    public boolean isVampire() {
        return stage == Stage.VAMPIRE;
    }

    public boolean isTurning() {
        return stage == Stage.TURNING;
    }

    public int turning() {
        return turning;
    }

    public float blood() {
        return blood;
    }

    public float drunk() {
        return drunk;
    }

    public int level(Skill skill) {
        return skills.getOrDefault(skill, 0);
    }

    public Map<Skill, Integer> skills() {
        return Map.copyOf(skills);
    }

    public boolean nightVision() {
        return nightVision;
    }

    public boolean bloodSense() {
        return bloodSense;
    }

    public boolean bat() {
        return bat;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        if (stage != Stage.TURNING) turning = 0;
    }

    public void setTurning(int ticks) {
        turning = Math.max(0, ticks);
    }

    public void setBlood(float blood) {
        this.blood = Mth.clamp(blood, 0f, MAX_BLOOD);
    }

    /** Adds to the bar and to the lifetime total (which is what earns skill points). Returns what actually fit in the bar. */
    public float drink(float amount) {
        float before = blood;
        setBlood(blood + amount);
        drunk += amount;
        return blood - before;
    }

    /** Takes blood from the bar. Returns false (and takes nothing) if there isn't that much. */
    public boolean spend(float amount) {
        if (blood < amount) return false;
        blood -= amount;
        return true;
    }

    public void drain(float amount) {
        setBlood(blood - amount);
    }

    public void setDrunk(float drunk) {
        this.drunk = Math.max(0f, drunk);
    }

    public void setLevel(Skill skill, int level) {
        level = Mth.clamp(level, 0, skill.maxLevel);
        if (level == 0) skills.remove(skill);
        else skills.put(skill, level);
    }

    public void setNightVision(boolean on) {
        nightVision = on;
    }

    public void setBloodSense(boolean on) {
        bloodSense = on;
    }

    public void setBat(boolean on) {
        bat = on;
    }

    /** Back to plain human: skills, blood and toggles all gone. */
    public void reset() {
        stage = Stage.HUMAN;
        turning = 0;
        blood = 0;
        drunk = 0;
        skills.clear();
        nightVision = bloodSense = bat = false;
        dashTicks = crash = 0;
        rushing = false;
    }

    // ------------------------------------------------------------------ points

    /** Blood needed for the n-th point (0-based): 6, 7, 8, ... so the first few come quickly. */
    public static float costOfPoint(int n) {
        return 6 + n;
    }

    /** Points earned so far from everything ever drunk. */
    public int earnedPoints() {
        int n = 0;
        float left = drunk;
        while (left >= costOfPoint(n)) {
            left -= costOfPoint(n);
            n++;
        }
        return n;
    }

    /** Blood still to drink before the next point. */
    public float toNextPoint() {
        float left = drunk;
        int n = 0;
        while (left >= costOfPoint(n)) {
            left -= costOfPoint(n);
            n++;
        }
        return costOfPoint(n) - left;
    }

    public int spentPoints() {
        int n = 0;
        for (int l : skills.values()) n += l;
        return n;
    }

    public int points() {
        return Math.max(0, earnedPoints() - spentPoints());
    }

    /** Lifetime blood that buys {@code points} points, for commands and tests. */
    public static float bloodForPoints(int points) {
        float total = 0;
        for (int i = 0; i < points; i++) total += costOfPoint(i);
        return total;
    }

    public VampData copy() {
        return new VampData(stage, turning, blood, drunk, skills, nightVision, bloodSense, bat);
    }
}
