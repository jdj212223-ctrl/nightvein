package com.nightvein.client.gui;

import com.nightvein.Nightvein;
import com.nightvein.vamp.VampData;
import com.nightvein.vamp.Vampires;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** The blood bar (where food would be), the red of the turning, and the Bloodrush readout. */
public final class VampHud {
    private static final ResourceLocation DROP_FULL = Nightvein.id("hud/blood_full");
    private static final ResourceLocation DROP_HALF = Nightvein.id("hud/blood_half");
    private static final ResourceLocation DROP_EMPTY = Nightvein.id("hud/blood_empty");

    private VampHud() {}

    /** Drawn below everything else: a red that creeps in from the edges as the turning takes hold. */
    public static void tint(GuiGraphics g, DeltaTracker delta) {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null) return;
        VampData d = Vampires.get(me);
        float strength;
        if (d.isTurning()) strength = 0.12f + 0.38f * Vampires.turningProgress(d);
        else if (d.isVampire() && d.blood() <= 2) strength = 0.25f;
        else return;
        // A slow pulse, in time with nothing in particular.
        strength *= 0.85f + 0.15f * Mth.sin((me.tickCount + delta.getGameTimeDeltaPartialTick(true)) * 0.25f);
        int w = g.guiWidth(), h = g.guiHeight();
        int edge = (int) (strength * 255) << 24 | 0x6A0008;
        int mid = (int) (strength * 90) << 24 | 0x6A0008;
        g.fillGradient(0, 0, w, h / 2, edge, mid);
        g.fillGradient(0, h / 2, w, h, mid, edge);
    }

    public static void bar(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer me = mc.player;
        if (me == null || mc.options.hideGui || !mc.gameMode.canHurtPlayer()) return;
        VampData d = Vampires.get(me);
        if (!d.isVampire()) return;
        int right = g.guiWidth() / 2 + 91;
        int y = g.guiHeight() - mc.gui.rightHeight;
        mc.gui.rightHeight += 10;
        int units = Mth.ceil(d.blood());
        for (int i = 0; i < 10; i++) {
            int x = right - i * 8 - 9;
            int yy = y + (d.blood() <= 4 && me.tickCount % (i + 7) == 0 ? me.getRandom().nextInt(2) : 0);
            g.blitSprite(DROP_EMPTY, x, yy, 9, 9);
            if (i * 2 + 1 < units) g.blitSprite(DROP_FULL, x, yy, 9, 9);
            else if (i * 2 + 1 == units) g.blitSprite(DROP_HALF, x, yy, 9, 9);
        }
        if (d.rushing || d.crash > 0) {
            Component label = Component.translatable(d.rushing ? "hud.nightvein.bloodrush" : "hud.nightvein.crash");
            int colour = d.rushing ? 0xFF2E4A : 0x7A7A7A;
            g.drawString(mc.font, label, right - mc.font.width(label), y - 10, colour, true);
        }
    }
}
