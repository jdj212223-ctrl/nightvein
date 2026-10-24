package com.nightvein.test;

import com.mojang.authlib.GameProfile;
import com.nightvein.Nightvein;
import com.nightvein.NightveinConfig;
import com.nightvein.compat.ChromefleshCompat;
import com.nightvein.compat.VoidClipCompat;
import com.nightvein.entity.Nightwalker;
import com.nightvein.item.BloodBottleItem;
import com.nightvein.item.TinctureItem;
import com.nightvein.registry.ModEntities;
import com.nightvein.registry.ModItems;
import com.nightvein.vamp.BatForm;
import com.nightvein.vamp.Powers;
import com.nightvein.vamp.Skill;
import com.nightvein.vamp.Stage;
import com.nightvein.vamp.Stuns;
import com.nightvein.vamp.Tackle;
import com.nightvein.vamp.VampData;
import com.nightvein.vamp.VampTicker;
import com.nightvein.vamp.Vampires;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

/**
 * Run with ./gradlew runGameTestServer (alone) and runGameTestServerCompat (beside VoidClip and Chromeflesh); both are
 * part of ./gradlew build. The compat tests check whichever case they find themselves in.
 */
@GameTestHolder(Nightvein.MODID)
@PrefixGameTestTemplate(false)
public class NightveinGameTests {

    /** Like GameTestHelper#makeMockServerPlayerInLevel, but genuinely in survival (that one always claims creative). */
    private static ServerPlayer player(GameTestHelper helper, Vec3 at, float yaw) {
        ServerLevel level = helper.getLevel();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-vamp"), false);
        ServerPlayer p = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
        p.setGameMode(GameType.SURVIVAL);
        p.moveTo(helper.absoluteVec(at), yaw, 0);
        p.setYBodyRot(yaw);
        p.setYHeadRot(yaw);
        return p;
    }

    private static ServerPlayer player(GameTestHelper helper) {
        return player(helper, new Vec3(1.5, 1, 1.5), 0);
    }

    private static ServerPlayer vampire(GameTestHelper helper, Vec3 at, float yaw) {
        ServerPlayer p = player(helper, at, yaw);
        Vampires.get(p).setStage(Stage.VAMPIRE);
        Vampires.get(p).setBlood(VampData.MAX_BLOOD);
        Vampires.refresh(p);
        return p;
    }

    private static void giveSkill(ServerPlayer p, Skill skill, int level) {
        Vampires.get(p).setLevel(skill, level);
        Vampires.refresh(p);
    }

    // ------------------------------------------------------------------ data

    @GameTest(template = "arena")
    public static void dataSurvivesSaveAndLoad(GameTestHelper helper) {
        VampData data = new VampData(Stage.VAMPIRE, 0, 12.5f, 40f, Map.of(Skill.SPEED, 2, Skill.FANGS, 1), true, false, true);
        Tag tag = VampData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        VampData back = VampData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
        helper.assertTrue(back.stage() == Stage.VAMPIRE, "stage lost");
        helper.assertTrue(back.blood() == 12.5f && back.drunk() == 40f, "blood lost");
        helper.assertTrue(back.level(Skill.SPEED) == 2 && back.level(Skill.FANGS) == 1, "skills lost: " + back.skills());
        helper.assertTrue(back.nightVision() && back.bat() && !back.bloodSense(), "toggles lost");
        VampData empty = VampData.CODEC.parse(NbtOps.INSTANCE, new CompoundTag()).getOrThrow();
        helper.assertTrue(empty.stage() == Stage.HUMAN && empty.skills().isEmpty(), "an empty tag should load as a plain human");
        helper.succeed();
    }

    // ------------------------------------------------------------------ infection

    @GameTest(template = "arena")
    public static void biteFromBehindInfects(GameTestHelper helper) {
        // Victim faces south (+Z); the vampire stands north of them, at their back.
        ServerPlayer victim = player(helper, new Vec3(1.5, 1, 2.5), 0);
        ServerPlayer vamp = vampire(helper, new Vec3(1.5, 1, 1.5), 0);
        helper.assertTrue(Powers.behind(vamp, victim), "vampire should count as behind the victim");
        Powers.BiteResult r = Powers.bite(vamp, victim, victim.position().add(0, 1, 0)); // low on the back: still the neck from behind
        helper.assertTrue(r == Powers.BiteResult.INFECTED, "a bite from behind should infect, got " + r);
        helper.assertTrue(Vampires.get(victim).stage() == Stage.TURNING, "victim should be turning");
        helper.assertTrue(Vampires.get(vamp).drunk() > 0, "the vampire should have drunk");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void biteAtTheNeckFromTheFrontInfects(GameTestHelper helper) {
        // Face to face: victim looks north (yaw 180) at the vampire.
        ServerPlayer victim = player(helper, new Vec3(1.5, 1, 2.5), 180);
        ServerPlayer vamp = vampire(helper, new Vec3(1.5, 1, 1.5), 0);
        helper.assertFalse(Powers.behind(vamp, victim), "face to face is not behind");
        Powers.BiteResult r = Powers.bite(vamp, victim, new Vec3(victim.getX(), victim.getEyeY() - 0.2, victim.getZ()));
        helper.assertTrue(r == Powers.BiteResult.INFECTED, "a bite at the neck should infect, got " + r);
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void biteToTheBodyFromTheFrontDoesNotInfect(GameTestHelper helper) {
        ServerPlayer victim = player(helper, new Vec3(1.5, 1, 2.5), 180);
        ServerPlayer vamp = vampire(helper, new Vec3(1.5, 1, 1.5), 0);
        Powers.BiteResult r = Powers.bite(vamp, victim, victim.position().add(0, 0.8, 0));
        helper.assertTrue(r == Powers.BiteResult.BIT, "should be an ordinary bite, got " + r);
        helper.assertTrue(Vampires.get(victim).stage() == Stage.HUMAN, "an arm bite shouldn't turn anyone");
        helper.assertTrue(Vampires.get(vamp).drunk() > 0, "the vampire should still have drunk something");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void turningTakesTimeAndHasSymptoms(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        helper.assertTrue(Vampires.infect(p), "infect failed");
        helper.assertFalse(Vampires.infect(p), "infecting twice should do nothing");
        VampData d = Vampires.get(p);
        for (int i = 0; i < 100; i++) {
            p.tickCount++;
            VampTicker.tick(p);
        }
        helper.assertTrue(d.isTurning(), "should still be turning after 5 seconds: it isn't instant");
        helper.assertTrue(p.hasEffect(MobEffects.HUNGER), "thirst (hunger) should have set in");
        helper.assertTrue(Vampires.turningProgress(d) > 0, "turning should progress");
        d.setTurning(NightveinConfig.turningTicks() - 1);
        p.tickCount++;
        VampTicker.tick(p);
        helper.assertTrue(d.isVampire(), "the turning should finish at the end");
        helper.assertTrue(d.blood() > 0, "a new vampire should wake with some blood");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void hawthornCuresTheTurningButNotAVampire(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        Vampires.infect(p);
        helper.assertTrue(TinctureItem.apply(p, TinctureItem.Kind.HAWTHORN, false) == TinctureItem.Outcome.CURED_TURNING, "hawthorn should stop the turning");
        helper.assertTrue(Vampires.get(p).stage() == Stage.HUMAN, "should be human again");
        Vampires.completeTurning(p);
        helper.assertTrue(TinctureItem.apply(p, TinctureItem.Kind.HAWTHORN, true) == TinctureItem.Outcome.TOO_LATE, "hawthorn is too weak for a full vampire");
        helper.assertTrue(Vampires.get(p).isVampire(), "still a vampire");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void sunlitTinctureOnlyWorksInTheSun(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, new Vec3(1.5, 1, 1.5), 0);
        giveSkill(p, Skill.SPEED, 2);
        helper.assertTrue(TinctureItem.apply(p, TinctureItem.Kind.SUNLIT, false) == TinctureItem.Outcome.NEEDS_SUN, "in the shade it should only burn");
        helper.assertTrue(Vampires.get(p).isVampire(), "still a vampire in the shade");
        helper.assertTrue(TinctureItem.apply(p, TinctureItem.Kind.SUNLIT, true) == TinctureItem.Outcome.CURED_VAMPIRE, "in the sun it should cure");
        VampData d = Vampires.get(p);
        helper.assertTrue(d.stage() == Stage.HUMAN && d.skills().isEmpty() && d.drunk() == 0, "cure should wipe everything");
        helper.assertTrue(p.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(Nightvein.id("speed")) == null, "speed skill outlived the cure");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void dyingWhileTurningFinishesIt(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        Vampires.infect(p);
        // What the respawn handler does with the (copied-on-death) attachment.
        ServerPlayer respawned = helper.getLevel().getServer().getPlayerList().respawn(p, false, Entity.RemovalReason.KILLED);
        helper.assertTrue(Vampires.get(respawned).isVampire(), "dying mid-turn should wake you as a vampire, got " + Vampires.get(respawned).stage());
        helper.succeed();
    }

    // ------------------------------------------------------------------ skills

    @GameTest(template = "arena")
    public static void skillsCostPointsAndFollowTheTree(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, new Vec3(1.5, 1, 1.5), 0);
        VampData d = Vampires.get(p);
        helper.assertTrue(Vampires.upgrade(p, Skill.SPEED) == Vampires.UpgradeResult.NO_POINTS, "no points yet");
        d.drink(VampData.bloodForPoints(3));
        helper.assertTrue(d.points() == 3, "drinking should earn points, have " + d.points());
        helper.assertTrue(Vampires.upgrade(p, Skill.DASH) == Vampires.UpgradeResult.LOCKED, "dash needs speed first");
        helper.assertTrue(Vampires.upgrade(p, Skill.SPEED) == Vampires.UpgradeResult.UPGRADED, "speed 1");
        helper.assertTrue(Vampires.upgrade(p, Skill.DASH) == Vampires.UpgradeResult.UPGRADED, "dash 1 after speed");
        helper.assertTrue(Vampires.upgrade(p, Skill.NIGHT_VISION) == Vampires.UpgradeResult.UPGRADED, "night vision");
        helper.assertTrue(Vampires.upgrade(p, Skill.NIGHT_VISION) == Vampires.UpgradeResult.MAXED, "night vision has one level");
        helper.assertTrue(d.points() == 0, "three points spent");
        helper.assertTrue(Vampires.upgrade(p, Skill.FANGS) == Vampires.UpgradeResult.NO_POINTS, "out of points");
        ServerPlayer human = player(helper);
        helper.assertTrue(Vampires.upgrade(human, Skill.SPEED) == Vampires.UpgradeResult.NOT_A_VAMPIRE, "humans have no skills");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void speedSkillStacksOnceAndRefreshes(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, new Vec3(1.5, 1, 1.5), 0);
        double base = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        giveSkill(p, Skill.SPEED, 3);
        Vampires.refresh(p);
        Vampires.refresh(p);
        double fast = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        helper.assertTrue(Math.abs(fast - base * 1.4) < 1e-6, "speed 3 should be +40%, got " + fast / base);
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void bitingFeedsAndFangsStealLife(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, new Vec3(1.5, 1, 1.5), 0);
        Vampires.get(p).setBlood(0);
        giveSkill(p, Skill.FANGS, 3);
        p.setHealth(10);
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(1, 1, 3));
        Powers.bite(p, cow, cow.position().add(0, 0.5, 0));
        helper.assertTrue(Vampires.get(p).blood() > 0, "cow blood should fill the bar");
        helper.assertTrue(p.getHealth() > 10, "fangs should steal life");
        helper.assertTrue(cow.getHealth() < cow.getMaxHealth(), "the cow should feel it");
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 1, 1));
        Vampires.get(p).biteCooldown = 0;
        helper.assertTrue(Powers.bite(p, zombie, zombie.getEyePosition()) == Powers.BiteResult.NO_BLOOD, "the undead have no blood");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void sunBurnsYoungVampiresAndWardsProtect(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, new Vec3(1.5, 1, 1.5), 0);
        VampData d = Vampires.get(p);
        p.tickCount = 400;
        helper.assertTrue(VampTicker.sun(p, d, true), "a fresh vampire should catch fire in the sun");
        helper.assertTrue(p.hasEffect(MobEffects.WEAKNESS), "and feel weak");
        p.clearFire();
        p.removeAllEffects();
        helper.assertFalse(VampTicker.sun(p, d, false), "no sun, no fire");
        giveSkill(p, Skill.SUN_WARD, 2);
        helper.assertFalse(VampTicker.sun(p, d, true), "ward 2 shouldn't burn");
        helper.assertTrue(p.hasEffect(MobEffects.WEAKNESS), "ward 2 still weakens");
        p.removeAllEffects();
        giveSkill(p, Skill.SUN_WARD, 3);
        VampTicker.sun(p, d, true);
        helper.assertFalse(p.hasEffect(MobEffects.WEAKNESS), "ward 3 is immune");
        helper.assertTrue(VampTicker.burnInterval(0, true) > VampTicker.burnInterval(0, false), "a helmet should help");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void vampiresDrinkInsteadOfEating(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, new Vec3(1.5, 1, 1.5), 0);
        VampData d = Vampires.get(p);
        p.getFoodData().setFoodLevel(3);
        d.setBlood(0);
        VampTicker.vampire(p, d);
        helper.assertTrue(p.getFoodData().getFoodLevel() == VampTicker.PINNED_FOOD, "the food bar should be held still");
        helper.assertTrue(p.hasEffect(MobEffects.WEAKNESS), "no blood should mean weakness");
        ItemStack bottle = new ItemStack(ModItems.BLOOD_BOTTLE.get());
        ItemStack left = bottle.finishUsingItem(p.level(), p);
        helper.assertTrue(d.blood() == BloodBottleItem.BLOOD, "a blood bottle should fill " + BloodBottleItem.BLOOD + ", got " + d.blood());
        helper.assertTrue(left.is(net.minecraft.world.item.Items.GLASS_BOTTLE), "should give the glass back");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void dashChargesBloodAndCoolsDown(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, new Vec3(1.5, 1, 1.5), 0);
        VampData d = Vampires.get(p);
        helper.assertFalse(Powers.dash(p), "no dash without the skill");
        giveSkill(p, Skill.DASH, 1);
        float before = d.blood();
        helper.assertTrue(Powers.dash(p), "dash failed");
        helper.assertTrue(d.blood() == before - 1, "dash should cost a point of blood");
        helper.assertTrue(d.dashTicks > 0 && d.dashCooldown > 0, "dash should open a tackle window and start a cooldown");
        helper.assertFalse(Powers.dash(p), "can't dash again during the cooldown");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void mistStepMovesYouForward(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, new Vec3(1.5, 1, 0.5), 0);
        giveSkill(p, Skill.MIST_STEP, 1);
        Vec3 from = p.position();
        helper.assertTrue(Powers.mistStep(p), "mist step failed in open air: target " + Powers.mistTarget(p, 1) + " from " + from
                + " look " + p.getViewVector(1f) + " block ahead " + helper.getLevel().getBlockState(p.blockPosition().south(2)));
        double moved = p.position().distanceTo(from);
        helper.assertTrue(moved > 3 && moved <= Powers.mistRange(1) + 0.01, "should step forward a few blocks, moved " + moved);
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void mistStepTwoDriftsThroughWalls(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, new Vec3(1.5, 1, 0.5), 0);
        for (int x = 0; x < 3; x++) for (int y = 1; y < 4; y++) helper.setBlock(new BlockPos(x, y, 2), Blocks.STONE);
        giveSkill(p, Skill.MIST_STEP, 1);
        Vec3 l1 = Powers.mistTarget(p, 1);
        helper.assertTrue(l1 == null || helper.relativeVec(l1).z < 2, "level 1 shouldn't pass the wall");
        Vec3 l2 = Powers.mistTarget(p, 2);
        helper.assertTrue(l2 != null && helper.relativeVec(l2).z > 3, "level 2 should come out the other side");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void hypnosisFreezesAMob(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, new Vec3(1.5, 1, 1.5), 0);
        giveSkill(p, Skill.HYPNOSIS, 1);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 4));
        zombie.setTarget(p);
        helper.assertTrue(Powers.hypnotize(p, zombie) == Powers.HypnosisResult.MOB, "hypnosis failed");
        helper.assertTrue(zombie.getTarget() == null, "a hypnotised mob forgets its target");
        helper.assertTrue(zombie.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0, "and stands still");
        helper.assertTrue(Stuns.isStunned(zombie), "should be stunned");
        helper.assertTrue(Powers.hypnotize(p, zombie) == Powers.HypnosisResult.COOLDOWN, "cooldown");
        Stuns.release(zombie);
        helper.assertTrue(zombie.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0, "released mobs move again");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void batFormFliesAndShrinks(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, new Vec3(1.5, 1, 1.5), 0);
        helper.assertFalse(BatForm.enter(p), "no bat without the skill");
        giveSkill(p, Skill.BAT_FORM, 1);
        double height = p.getBbHeight();
        helper.assertTrue(BatForm.enter(p), "bat form failed");
        helper.assertTrue(p.getAbilities().mayfly, "bats fly");
        helper.assertTrue(p.getBbHeight() < height * 0.6, "bats are small, height " + p.getBbHeight());
        BatForm.leave(p);
        helper.assertFalse(p.getAbilities().mayfly, "flight should end with bat form");
        helper.assertTrue(Math.abs(p.getBbHeight() - height) < 1e-4, "full size again");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void tackleThrowsMobs(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, new Vec3(1.5, 1, 1.5), 0);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        float hp = zombie.getHealth();
        helper.assertFalse(Tackle.hit(p, zombie, new Vec3(0, 0, 1)), "only Bacteria gets knocked down");
        helper.assertTrue(zombie.getHealth() < hp, "a tackle should hurt");
        helper.assertTrue(zombie.getDeltaMovement().z > 0.5, "and throw it the way you were going");
        helper.succeed();
    }

    // ------------------------------------------------------------------ vampire mob

    @GameTest(template = "arena")
    public static void nightwalkerBitesFromBehind(GameTestHelper helper) {
        ServerPlayer p = player(helper, new Vec3(1.5, 1, 2.5), 0); // facing south
        Nightwalker n = helper.spawn(ModEntities.NIGHTWALKER.get(), new BlockPos(1, 1, 1)); // to the north, at their back
        n.doHurtTarget(p);
        helper.assertTrue(Vampires.get(p).isTurning(), "a nightwalker bite from behind should infect");
        ServerPlayer q = player(helper, new Vec3(3.5, 1, 2.5), 180); // facing north, toward it
        Nightwalker m = helper.spawn(ModEntities.NIGHTWALKER.get(), new BlockPos(3, 1, 1));
        m.doHurtTarget(q);
        helper.assertTrue(Vampires.get(q).stage() == Stage.HUMAN, "from the front it only claws");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void nightwalkersLeaveVampiresAlone(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, new Vec3(1.5, 1, 1.5), 0);
        Nightwalker n = helper.spawn(ModEntities.NIGHTWALKER.get(), new BlockPos(1, 1, 4));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(n.getTarget() != p, "a nightwalker shouldn't hunt a vampire");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ Chromeflesh

    private static void install(ServerPlayer p, String implant) throws ReflectiveOperationException {
        Class<?> type = Class.forName("com.chromeflesh.implant.Implant");
        Method install = Class.forName("com.chromeflesh.implant.Implants").getMethod("install", ServerPlayer.class, type);
        install.invoke(null, p, type.getField(implant).get(null));
    }

    @GameTest(template = "arena")
    public static void chromefleshBloodrushIsTheFastestThingAlive(GameTestHelper helper) throws ReflectiveOperationException {
        ServerPlayer human = player(helper);
        ServerPlayer vamp = vampire(helper, new Vec3(2.5, 1, 1.5), 0);
        giveSkill(vamp, Skill.SPEED, 3);
        if (!ChromefleshCompat.LOADED) {
            helper.assertTrue(ChromefleshCompat.speedImplants(vamp) == 0, "no Chromeflesh, no implants");
            vamp.setSprinting(true);
            VampTicker.bloodrush(vamp, Vampires.get(vamp));
            helper.assertFalse(Vampires.get(vamp).rushing, "Bloodrush needs Chromeflesh");
            helper.succeed();
            return;
        }
        ServerPlayer chrome = player(helper, new Vec3(3.5, 1, 1.5), 0);
        ServerPlayer hybrid = vampire(helper, new Vec3(4.5, 1, 1.5), 0);
        giveSkill(hybrid, Skill.SPEED, 3);
        for (String implant : ChromefleshCompat.SPEED_IMPLANTS) {
            install(chrome, implant);
            install(hybrid, implant);
        }
        helper.assertTrue(ChromefleshCompat.speedImplants(hybrid) == 3, "should see all three speed implants");
        for (ServerPlayer p : new ServerPlayer[]{human, vamp, chrome, hybrid}) p.setSprinting(true);
        VampTicker.bloodrush(hybrid, Vampires.get(hybrid));
        VampTicker.bloodrush(vamp, Vampires.get(vamp));
        helper.assertTrue(Vampires.get(hybrid).rushing, "the hybrid should be Bloodrushing");
        helper.assertFalse(Vampires.get(vamp).rushing, "no chrome, no Bloodrush");
        double h = human.getAttributeValue(Attributes.MOVEMENT_SPEED), v = vamp.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double c = chrome.getAttributeValue(Attributes.MOVEMENT_SPEED), x = hybrid.getAttributeValue(Attributes.MOVEMENT_SPEED);
        helper.assertTrue(x > v && x > c && v > h && c > h, "hybrid should beat both: human " + h + ", vampire " + v + ", chrome " + c + ", hybrid " + x);
        helper.assertTrue(x >= v * c / h * 1.5, "Bloodrush should be well beyond the two stacked: " + x);

        // The chrome drinks: run it dry and crash.
        VampData d = Vampires.get(hybrid);
        d.setBlood(1);
        hybrid.tickCount = 40;
        VampTicker.bloodrush(hybrid, d);
        helper.assertTrue(d.crash > 0, "running dry should crash");
        helper.assertTrue(hybrid.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "a crash is slow");
        helper.assertTrue(hybrid.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(Vampires.BLOODRUSH) == null, "Bloodrush off after a crash");
        d.setBlood(10);
        VampTicker.bloodrush(hybrid, d);
        helper.assertFalse(d.rushing, "no Bloodrush while crashed");
        helper.assertTrue(Powers.dashPower(1, ChromefleshCompat.hasCyberlegs(hybrid)) > Powers.dashPower(1, false), "cyberlegs throw a dash further");
        helper.succeed();
    }

    // ------------------------------------------------------------------ VoidClip

    @GameTest(template = "arena", timeoutTicks = 200)
    public static void voidclipTackleDownsBacteriaAndFreesItsCatch(GameTestHelper helper) throws ReflectiveOperationException {
        ServerPlayer vamp = vampire(helper, new Vec3(1.5, 1, 1.5), 0);
        if (!VoidClipCompat.LOADED) {
            Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 3));
            helper.assertFalse(VoidClipCompat.isBacteria(zombie) || VoidClipCompat.isBackroomsEntity(zombie), "nothing is from the Backrooms without VoidClip");
            helper.assertTrue(VoidClipCompat.grabberOf(vamp) == null && VoidClipCompat.releaseGrabsBy(zombie) == 0, "grab hooks should be inert");
            helper.succeed();
            return;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("voidclip", "bacteria"));
        LivingEntity bacteria = (LivingEntity) helper.spawn(type, new BlockPos(4, 1, 4));
        helper.assertTrue(VoidClipCompat.isBacteria(bacteria), "should recognise Bacteria");
        helper.assertTrue(Vampires.bloodIn(bacteria) == 0, "Backrooms things have no blood");
        giveSkill(vamp, Skill.HYPNOSIS, 1);
        helper.assertTrue(Powers.hypnotize(vamp, bacteria) == Powers.HypnosisResult.IMMUNE, "nothing behind its eyes to hypnotise");

        ServerPlayer dad = player(helper, new Vec3(4.5, 1, 3.5), 0);
        Class<?> manager = Class.forName("com.voidclip.entity.GrabManager");
        Class<?> monster = Class.forName("com.voidclip.entity.BackroomsMonster");
        Method start = manager.getMethod("start", ServerPlayer.class, monster, float.class, int.class);
        Method tick = manager.getMethod("tick", ServerPlayer.class);
        helper.assertTrue((boolean) start.invoke(null, dad, bacteria, 0.075f, 100), "Bacteria should grab");
        helper.assertTrue(VoidClipCompat.isGrabbed(dad) && VoidClipCompat.grabberOf(dad) == bacteria, "should see the grab");

        float hp = bacteria.getHealth();
        helper.assertTrue(Tackle.hit(vamp, bacteria, new Vec3(1, 0, 1)), "a tackle should knock Bacteria down");
        helper.assertTrue(Stuns.isDowned(bacteria), "Bacteria should be down");
        helper.assertTrue(bacteria.getHealth() < hp, "and hurt");
        helper.assertTrue(bacteria.getDeltaMovement().horizontalDistance() > 1, "knockback resistance shouldn't save it");
        tick.invoke(null, dad);
        helper.assertFalse(VoidClipCompat.isGrabbed(dad), "knocking it down should free who it was holding");
        helper.assertFalse((boolean) start.invoke(null, vamp, bacteria, 0.075f, 100), "a downed Bacteria can't grab");

        // Grabbed yourself? Dash into it.
        LivingEntity other = (LivingEntity) helper.spawn(type, new BlockPos(1, 1, 3));
        ServerPlayer solo = vampire(helper, new Vec3(1.5, 1, 2.5), 0);
        giveSkill(solo, Skill.DASH, 1);
        helper.assertTrue((boolean) start.invoke(null, solo, other, 0.075f, 100), "second grab");
        helper.assertTrue(Powers.dash(solo), "dash while held");
        helper.assertTrue(Stuns.isDowned(other), "dashing while held should shove it down");
        tick.invoke(null, solo);
        helper.assertFalse(VoidClipCompat.isGrabbed(solo), "and get you out");

        helper.runAfterDelay(Tackle.DOWNED_TICKS + 5, () -> {
            helper.assertFalse(Stuns.isDowned(bacteria), "Bacteria gets back up eventually");
            helper.succeed();
        });
    }
}
