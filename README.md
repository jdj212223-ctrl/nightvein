# Nightvein

A vampire mod for **NeoForge 1.21.1**. It only takes one bite to the neck.

Third in the family with [liminal.](https://github.com/jdj212223-ctrl/voidclip) (VoidClip) and Chromeflesh. Works fine
without either; with them, it plugs in (see below).

## Becoming one

- A **vampire player** (press **V**) or a **Nightwalker** (a pale mob that walks at night and tries to get behind you)
  bites you **on the neck**: from behind, or aimed at your head. Anywhere else is just a bite.
- It isn't instant. You start **turning** (3 minutes by default, `turningSeconds` in the server config): thirst,
  sun sickness, red creeping into your vision, a heartbeat getting louder and faster.
- When it finishes, or if you die before it does, you wake up a vampire.

## The cure

- **Hawthorn Tincture** (glass bottle + sweet berries + golden carrot + sugar) stops the turning any time before it ends.
- **Sunlit Tincture** (hawthorn tincture + sunflower + golden apple + glowstone dust) cures a full vampire, but only
  if you drink it **standing in full sun**. Anywhere else it just burns. You lose every skill.

## Being one

- Blood replaces hunger. Bite things (V) to drink: villagers and players have the most, animals some, the undead and
  golems none. Sneak + glass bottle on a mob fills a **Bottle of Blood**.
- The sun burns, but you can survive it (a helmet helps). No blood means weak, slow, and slowly dying.
- Blood you drink earns **skill points**. Open the tree with **K**:

| Hunt | Night | Blood | Mind | Sun |
|---|---|---|---|---|
| Speed (3) | Night Eyes (1), N | Fangs (3): bite damage + life steal | Blood Sense (2), C: see heartbeats through walls | Sun Ward (3) |
| Dash (2), R | Bat Form (2), B: fly | Regeneration (3) | Hypnosis (2), X | |
| Mist Step (2), G: lv2 goes through walls | | | | |

Each skill needs a level of the one above it. `/nightvein` (ops) has `infect`, `turn`, `cure`, `blood`, `points`,
`skill`, `maxskills`, `status`.

## With Chromeflesh: Bloodrush

Vampire Speed + Chromeflesh speed implants (Leg Muscle, Cyberlegs, Cyberheart) = **Bloodrush** while sprinting:
the fastest thing in the game. Over 3x normal sprint with everything maxed. Cyberlegs also throw your dash 50% further
and halve its cooldown. **The catch: chrome drinks your blood.** Bloodrush burns a point every 2 seconds, and running dry
**crashes** you (slowness and nausea, Bloodrush off until you drink).

## With liminal. (VoidClip)

Go fast enough (a dash, or a Speed 3 / Bloodrush sprint) and you **tackle** whatever you run into. **Bacteria goes down
flat** for 5 seconds, can't grab anyone while it's down, and lets go of whoever it was holding. Dash while it's holding
you to shove it off. Backrooms things have no blood, and nothing behind their eyes to hypnotise. Blood Sense outlines
them grey, whatever they look like.

## Building

```
./gradlew build
```

Runs the GameTests twice: alone, and beside VoidClip + Chromeflesh (their jars from `../voidclip/build/libs` and
`../chromeflesh/build/libs`, set in `gradle.properties`; skipped if missing). `./gradlew runClientCompat` plays with all three.

Assets are generated: `.venv/bin/python tools/gen_assets.py` (needs numpy + soundfile).

MIT, by jdj212223-ctrl.
