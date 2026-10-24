package com.nightvein.client.gui;

import com.nightvein.Nightvein;
import com.nightvein.network.ActionPayload;
import com.nightvein.vamp.Skill;
import com.nightvein.vamp.VampData;
import com.nightvein.vamp.Vampires;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The skill tree (K). Five columns; each skill needs a level of the one above it. Click to spend a point. */
public class SkillScreen extends Screen {
    private static final int NODE = 26, GAP_X = 58, GAP_Y = 46;
    private static final String[] COLUMNS = {"hunt", "night", "blood", "mind", "sun"};

    public SkillScreen() {
        super(Component.translatable("screen.nightvein.skills"));
    }

    private VampData data() {
        return Vampires.get(Minecraft.getInstance().player);
    }

    private int left() {
        return width / 2 - (GAP_X * 4 + NODE) / 2;
    }

    private int top() {
        return height / 2 - (GAP_Y * 2 + NODE) / 2 + 10;
    }

    private int nodeX(Skill s) {
        return left() + s.column * GAP_X;
    }

    private int nodeY(Skill s) {
        return top() + s.row * GAP_Y;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        VampData d = data();
        g.drawCenteredString(font, title.copy().withStyle(ChatFormatting.DARK_RED), width / 2, top() - 44, 0xFFFFFF);
        if (!d.isVampire()) {
            Component msg = d.isTurning()
                    ? Component.translatable("screen.nightvein.turning", Math.round(Vampires.turningProgress(d) * 100))
                    : Component.translatable("screen.nightvein.human");
            g.drawCenteredString(font, msg, width / 2, height / 2, 0xAAAAAA);
            return;
        }
        g.drawCenteredString(font, Component.translatable("screen.nightvein.points", d.points(),
                String.format(Locale.ROOT, "%.1f", d.toNextPoint())), width / 2, top() - 30, 0xE0C0C0);
        for (int c = 0; c < COLUMNS.length; c++) {
            g.drawCenteredString(font, Component.translatable("screen.nightvein.column." + COLUMNS[c]),
                    left() + c * GAP_X + NODE / 2, top() - 14, 0x9A6A6A);
        }
        Skill hovered = null;
        for (Skill s : Skill.values()) {
            int x = nodeX(s), y = nodeY(s);
            if (s.requires != null) {
                int lineColour = d.level(s.requires) > 0 ? 0xFFB0182A : 0xFF3A2A2A;
                g.fill(x + NODE / 2 - 1, nodeY(s.requires) + NODE, x + NODE / 2 + 1, y, lineColour);
            }
            int lv = d.level(s);
            boolean locked = s.requires != null && d.level(s.requires) == 0;
            boolean canBuy = !locked && lv < s.maxLevel && d.points() > 0;
            int border = lv >= s.maxLevel ? 0xFFE0A030 : canBuy ? 0xFFE03040 : lv > 0 ? 0xFF902030 : 0xFF404040;
            g.fill(x - 1, y - 1, x + NODE + 1, y + NODE + 1, border);
            g.fill(x, y, x + NODE, y + NODE, locked ? 0xFF141010 : 0xFF241418);
            ResourceLocation icon = Nightvein.id("textures/gui/skills/" + s.id() + ".png");
            if (locked) g.setColor(0.35f, 0.35f, 0.35f, 1f);
            g.blit(icon, x + 5, y + 5, 0, 0, 16, 16, 16, 16);
            g.setColor(1f, 1f, 1f, 1f);
            for (int i = 0; i < s.maxLevel; i++) {
                int px = x + NODE / 2 - (s.maxLevel * 5) / 2 + i * 5;
                g.fill(px, y + NODE + 3, px + 4, y + NODE + 6, i < lv ? 0xFFE03040 : 0xFF3A3A3A);
            }
            if (mouseX >= x && mouseX < x + NODE && mouseY >= y && mouseY < y + NODE) hovered = s;
        }
        if (hovered != null) g.renderTooltip(font, tooltip(hovered, d), mouseX, mouseY);
    }

    private List<FormattedCharSequence> tooltip(Skill s, VampData d) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        int lv = d.level(s);
        lines.add(s.title().copy().withStyle(ChatFormatting.RED).append(Component.literal("  " + lv + "/" + s.maxLevel).withStyle(ChatFormatting.GRAY)).getVisualOrderText());
        if (lv > 0) lines.addAll(font.split(s.description(lv).copy().withStyle(ChatFormatting.GRAY), 200));
        if (lv < s.maxLevel) {
            lines.add(Component.translatable("screen.nightvein.next").withStyle(ChatFormatting.DARK_RED).getVisualOrderText());
            lines.addAll(font.split(s.description(lv + 1).copy().withStyle(ChatFormatting.WHITE), 200));
            if (s.requires != null && d.level(s.requires) == 0) {
                lines.add(Component.translatable("screen.nightvein.requires", s.requires.title()).withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
            } else if (d.points() > 0) {
                lines.add(Component.translatable("screen.nightvein.click").withStyle(ChatFormatting.YELLOW).getVisualOrderText());
            }
        }
        lines.addAll(font.split(Component.translatable("skill.nightvein." + s.id() + ".key").withStyle(ChatFormatting.DARK_GRAY), 200));
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && data().isVampire()) {
            for (Skill s : Skill.values()) {
                int x = nodeX(s), y = nodeY(s);
                if (mouseX >= x && mouseX < x + NODE && mouseY >= y && mouseY < y + NODE) {
                    PacketDistributor.sendToServer(new ActionPayload(ActionPayload.UPGRADE + s.ordinal()));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
