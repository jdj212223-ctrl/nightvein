package com.nightvein.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.nightvein.vamp.Skill;
import com.nightvein.vamp.VampData;
import com.nightvein.vamp.Vampires;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.function.Consumer;

/** /nightvein: cheats for testing and for fixing things in a LAN world. Needs permission level 2. */
public final class NightveinCommand {
    private static final SimpleCommandExceptionType NO_SKILL = new SimpleCommandExceptionType(Component.translatable("commands.nightvein.no_skill"));

    private NightveinCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("nightvein").requires(s -> s.hasPermission(2))
                .then(Commands.literal("status")
                        .executes(c -> status(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> status(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("infect").then(targets().executes(c -> each(c, Vampires::infect))))
                .then(Commands.literal("turn").then(targets().executes(c -> each(c, Vampires::completeTurning))))
                .then(Commands.literal("cure").then(targets().executes(c -> each(c, Vampires::cure))))
                .then(Commands.literal("blood").then(targets()
                        .then(Commands.argument("amount", FloatArgumentType.floatArg(0, VampData.MAX_BLOOD)).executes(c -> each(c, p -> {
                            Vampires.get(p).setBlood(FloatArgumentType.getFloat(c, "amount"));
                            Vampires.sync(p);
                        })))))
                .then(Commands.literal("points").then(targets()
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100)).executes(c -> each(c, p -> {
                            VampData v = Vampires.get(p);
                            int want = v.earnedPoints() + IntegerArgumentType.getInteger(c, "amount");
                            v.setDrunk(VampData.bloodForPoints(want));
                            Vampires.sync(p);
                        })))))
                .then(Commands.literal("skill").then(targets()
                        .then(Commands.argument("skill", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Skill.values()).map(Skill::id), b))
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, 3)).executes(c -> {
                                    Skill skill = skill(c);
                                    return each(c, p -> {
                                        Vampires.get(p).setLevel(skill, IntegerArgumentType.getInteger(c, "level"));
                                        Vampires.refresh(p);
                                        Vampires.sync(p);
                                    });
                                })))))
                .then(Commands.literal("maxskills").then(targets().executes(c -> each(c, p -> {
                    VampData v = Vampires.get(p);
                    for (Skill s : Skill.values()) v.setLevel(s, s.maxLevel);
                    v.setDrunk(Math.max(v.drunk(), VampData.bloodForPoints(v.spentPoints())));
                    Vampires.refresh(p);
                    Vampires.sync(p);
                })))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ?> targets() {
        return Commands.argument("targets", EntityArgument.players());
    }

    private static Skill skill(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        String name = StringArgumentType.getString(c, "skill").toUpperCase(Locale.ROOT);
        try {
            return Skill.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw NO_SKILL.create();
        }
    }

    private static int each(CommandContext<CommandSourceStack> c, Consumer<ServerPlayer> action) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "targets");
        players.forEach(action);
        c.getSource().sendSuccess(() -> Component.translatable("commands.nightvein.done", players.size()), true);
        return players.size();
    }

    private static int status(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        VampData d = Vampires.get(p);
        c.getSource().sendSuccess(() -> Component.translatable("commands.nightvein.status", p.getDisplayName(),
                d.stage().getSerializedName(), String.format(Locale.ROOT, "%.1f", d.blood()), d.points(), d.skills().toString()), false);
        return 1;
    }
}
