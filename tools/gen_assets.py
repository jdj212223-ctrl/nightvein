#!/usr/bin/env python3
"""
Regenerates every Nightvein asset that isn't code: textures, sounds, and data/asset JSON.

    python3 -m venv .venv && .venv/bin/pip install numpy soundfile
    .venv/bin/python tools/gen_assets.py

Everything is procedural and seeded, so re-running produces identical files.
Item and skill icons are hand-drawn as ASCII pixel art below; everything else is painted with numpy.
soundfile is needed because Minecraft only positions *mono* Ogg Vorbis.
"""
import json
import os
import struct
import zlib

import numpy as np
import soundfile as sf

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
NS = "nightvein"
ASSETS = os.path.join(ROOT, "assets", NS)
DATA = os.path.join(ROOT, "data", NS)
MC_DATA = os.path.join(ROOT, "data", "minecraft")
SR = 22050

SKILLS = ["speed", "dash", "mist_step", "night_vision", "bat_form", "fangs", "regen", "blood_sense", "hypnosis", "sun_ward"]
ITEMS = ["blood_bottle", "hawthorn_tincture", "sunlit_tincture"]


def path(*p):
    full = os.path.join(*p)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    return full


def write_json(obj, *p):
    with open(path(*p), "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


# =============================================================================== png

def write_png(img, *p):
    img = np.clip(img, 0, 255).astype(np.uint8)
    h, w, c = img.shape
    if c == 3:
        img = np.concatenate([img, np.full((h, w, 1), 255, np.uint8)], axis=2)
    raw = b"".join(b"\x00" + img[y].tobytes() for y in range(h))

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    with open(path(*p), "wb") as f:
        f.write(png)


def rng(seed):
    return np.random.default_rng(seed)


def hexc(h):
    return np.array([(h >> 16) & 255, (h >> 8) & 255, h & 255], float)


# =============================================================================== icons

PALETTE = {
    "k": 0x120A0E,  # outline
    "w": 0xF4EEE8,  # highlight / fang
    "s": 0xB8B0B4,  # silver
    "S": 0x6E6468,  # dark silver
    "r": 0xC0182C,  # blood
    "R": 0x6A0A16,  # dark blood
    "p": 0xF0707C,  # pink
    "g": 0xA8A4B0,  # mist
    "G": 0x5C5866,  # dark mist
    "e": 0xCFE6EE,  # glass
    "E": 0x7FA0AE,  # dark glass
    "c": 0x8A5A3A,  # cork
    "l": 0x4C9A3E,  # leaf
    "L": 0x235A22,  # dark leaf
    "n": 0xFFE45C,  # sun
    "N": 0xF09A20,  # sun dark
    "v": 0xB45CFF,  # violet
    "V": 0x5A1E8C,  # dark violet
    "b": 0x2A1E26,  # bat
    "B": 0x4A3442,  # bat light
    "y": 0xFFD23F,  # eye glow
}

ICONS = {
    "blood_bottle": [
        "",
        "      kkkk",
        "      kcck",
        "      kcck",
        "     keeeek",
        "     keEEek",
        "    keeEEeek",
        "   keerrrrEek",
        "   kerrrrrrEk",
        "   krrpprrrRk",
        "   krprrrrrRk",
        "   krrrrrrrRk",
        "   kRrrrrrRRk",
        "    kRRRRRRk",
        "     kkkkkk",
    ],
    "hawthorn_tincture": [
        "         Ll",
        "      kkkkll",
        "      kcckL",
        "      kcck",
        "     keeeek",
        "     keEEek",
        "    keeEEeek",
        "   keeppppEek",
        "   kepprrppEk",
        "   kppwpppprk",
        "   kpwpprpprk",
        "   kppprrpppk",
        "   kRppppprRk",
        "    kRRRRRRk",
        "     kkkkkk",
    ],
    "sunlit_tincture": [
        "  n         n",
        "   n  kkkk n",
        "      kcck",
        "      kcck",
        "     keeeek",
        "  n  keEEek  n",
        "    keeEEeek",
        "   keennnnEek",
        "   kennwnnnEk",
        "   knwnnnnnNk",
        "   knnnnnnnNk",
        "   knnnnnnNNk",
        "   kNnnnnnNNk",
        "    kNNNNNNk",
        "     kkkkkk",
    ],
    "speed": [
        "",
        "",
        "  kk     kk",
        "  krk    krk",
        "   krk    krk",
        "    krk    krk",
        "     krk    krk",
        "      krk    krk",
        "      krk    krk",
        "     krk    krk",
        "    krk    krk",
        "   krk    krk",
        "  krk    krk",
        "  kk     kk",
    ],
    "dash": [
        "",
        "",
        "",
        "         kk",
        "  RRRR   krk",
        "          krk",
        " rrrrrrrrrrrrk",
        " rrrrrrrrrrrrrk",
        " rrrrrrrrrrrrk",
        "          krk",
        "  RRRR   krk",
        "         kk",
    ],
    "mist_step": [
        "",
        "     gggg",
        "   ggwwwggg",
        "  gwwggggggg",
        "  gwgGGGgggG",
        "   gGG  GggG",
        "        gggG",
        "   ggggggGG",
        "  gwwwgGG",
        " gwggggg   g",
        " gwgGGGGggggG",
        "  gG    GGgG",
        "",
        "   rr  rr",
        "    r   r",
    ],
    "night_vision": [
        "",
        "",
        "",
        "     kkkkkk",
        "   kkwwwwwwkk",
        "  kwwwrrrrwwwk",
        " kwwwrRkkRrwwwk",
        " kwwwrkyykrwwwk",
        " kwwwrkyykrwwwk",
        " kwwwrRkkRrwwwk",
        "  kwwwrrrrwwwk",
        "   kkwwwwwwkk",
        "     kkkkkk",
    ],
    "bat_form": [
        "",
        "",
        "",
        "      b  b",
        " b    bbbb    b",
        " bb   bybyb  bb",
        " bBb  bbbbb bBb",
        " bBBbbbbbbbbBBb",
        " bBBBbbbbbbBBBb",
        "  bBBbbbbbbBBb",
        "  bBb bbbb bBb",
        "   bb  bb  bb",
        "   b        b",
    ],
    "fangs": [
        "",
        "",
        " kkkkkkkkkkkkk",
        " kRrrrrrrrrrRk",
        " krrprrrrrprrk",
        " krwwkrrrkwwrk",
        " kkwwk   kwwkk",
        "   kwwk kwwk",
        "   kwwk kwwk",
        "    kwk kwk",
        "    kwk kwk",
        "     k   k",
        "     r   r",
        "     r",
        "",
    ],
    "regen": [
        "",
        "",
        "   kkk   kkk",
        "  krrrk krrrk",
        " krppwrkrrrrrk",
        " krpwrrrrrrrrk",
        " krrrrrwwrrrrk",
        " krrrrrwwrrrRk",
        "  krrwwwwwwRk",
        "  krrwwwwwwRk",
        "   krrrwwrRk",
        "    krrwwRk",
        "     krRRk",
        "      kRk",
        "       k",
    ],
    "blood_sense": [
        "       R",
        "  R   kRk   R",
        "   R  krk  R",
        "     krrrk",
        "    krrrrrk",
        "R  krrprrrrk  R",
        "   krpprrrrk",
        "  krrprrrrrrk",
        "  krrrrrrrrRk",
        "  krrrrrrrrRk",
        "   krrrrrrRk",
        "   kRrrrrRRk",
        "R   kRRRRRk   R",
        "     kkkkk",
        "  R         R",
    ],
    "hypnosis": [
        "",
        "    vvvvvvv",
        "   v       v",
        "  v  VVVVV  v",
        " v  V     V  v",
        " v V  vvv  V v",
        " v V v   v V v",
        " v V v k v V v",
        " v V v  v  V v",
        " v V  vv   V v",
        " v  V     V  v",
        "  v  VVVVV  v",
        "   v       v",
        "    vvvvvvv",
    ],
    "sun_ward": [
        "  N   n   N",
        "   N  n  N",
        "    nnnnn",
        " N nnkkkkkn N",
        "   nkSssssSk",
        "nnnnkSsrrsSknnnn",
        "   nkSrrrrSk",
        " N nkSrrrrSk  N",
        "    kSsrrsSk",
        "    kkSssSkk",
        "     kkSSkk",
        "   N  kkkk  N",
        "  N    nn    N",
        "       n",
    ],
}


def icon(name, seed):
    rows = ICONS[name]
    assert len(rows) <= 16, name
    img = np.zeros((16, 16, 4), float)
    r = rng(seed)
    top = (16 - len(rows)) // 2
    for y, row in enumerate(rows):
        row = row.rstrip()
        assert len(row) <= 16, (name, row)
        for x, ch in enumerate(row):
            if ch in " .":
                continue
            c = hexc(PALETTE[ch])
            if ch not in "kyn":  # a little grain on surfaces, none on outlines and lights
                c = c + r.normal(0, 4)
            img[top + y, x, :3] = c
            img[top + y, x, 3] = 255
    return img


def drop(fill):
    """9x9 blood drop for the HUD: 'full', 'half' (left half filled), or 'empty' (dark socket)."""
    rows = [
        "    k",
        "   krk",
        "   krk",
        "  krrrk",
        " krprrrk",
        " krprrrk",
        " krrrrRk",
        "  kRRRk",
        "   kkk",
    ]
    img = np.zeros((9, 9, 4), float)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == " ":
                continue
            if fill == "empty" and ch != "k":
                c = (40, 14, 18)
            elif fill == "half" and ch != "k" and x > 4:
                c = (40, 14, 18)
            else:
                c = hexc(PALETTE[ch])
            img[y, x, :3] = c
            img[y, x, 3] = 255
    return img


# =============================================================================== painted textures

def noise(h, w, seed, amp):
    return rng(seed).normal(0, amp, (h, w, 1))


def cloth(h, w, seed, base, amp=4):
    return np.concatenate([np.array(base, float) + noise(h, w, seed, amp), np.full((h, w, 1), 255.0)], axis=2)


def nightwalker():
    """64x64 zombie layout: grey-white skin, black high-collared coat lined in red, eyes left for the glow layer."""
    img = np.zeros((64, 64, 4), float)
    skin = (196, 190, 196)
    img[0:16, 0:32] = cloth(16, 32, 11, skin, 5)
    img[0:8, 8:24] = cloth(8, 16, 12, (24, 18, 22), 3)       # hair on top
    img[8:10, 8:16, :3] = (24, 18, 22)                        # fringe
    img[8:16, 0:8, :3] = img[8:16, 0:8, :3] * 0.94            # right side
    img[8:16, 24:32, :3] = (26, 20, 24)                       # back of head: hair
    img[8:11, 16:24, :3] = (24, 18, 22)                       # left side top
    img[12, 9:11, :3] = (20, 6, 8)                            # eye sockets (lit by the glow layer)
    img[12, 13:15, :3] = (20, 6, 8)
    img[11, 9:11, :3] = (120, 112, 120)                       # heavy brow shadow
    img[11, 13:15, :3] = (120, 112, 120)
    img[14, 10:14, :3] = (110, 24, 32)                        # mouth
    img[15, 10, :3] = (240, 236, 230)                         # fangs
    img[15, 13, :3] = (240, 236, 230)
    # Coat: black, high red-lined collar, silver buttons.
    img[16:32, 16:40] = cloth(16, 24, 13, (20, 14, 18), 3)
    img[20:22, 20:28, :3] = (120, 14, 28)                     # collar lining
    img[20:32, 23:25, :3] = (60, 8, 16)
    for y in (23, 26, 29):
        img[y, 22, :3] = (190, 184, 190)
        img[y, 25, :3] = (190, 184, 190)
    # Arms: coat sleeves, pale hands.
    img[16:32, 40:56] = cloth(16, 16, 14, (22, 16, 20), 3)
    img[29:32, 40:56, :3] = np.array(skin) * 0.95
    img[16:20, 48:56, :3] = np.array(skin) * 0.95             # hand bottoms
    # Legs: dark trousers, boots.
    img[16:32, 0:16] = cloth(16, 16, 15, (30, 24, 30), 3)
    img[28:32, 0:16, :3] = (14, 10, 12)
    img[..., 3] = np.where(img[..., :3].sum(axis=2) > 0, 255, 0)
    write_png(img, ASSETS, "textures", "entity", "nightwalker.png")

    eyes = np.zeros((64, 64, 4), float)
    eyes[12, 9:11] = (255, 40, 50, 255)
    eyes[12, 13:15] = (255, 40, 50, 255)
    write_png(eyes, ASSETS, "textures", "entity", "nightwalker_eyes.png")


def logo():
    n = 128
    yy, xx = np.mgrid[0:n, 0:n]
    img = np.zeros((n, n, 4), float)
    img[..., :3] = (12, 6, 10)
    img[..., 3] = 255
    r = rng(5)
    img[..., :3] += r.normal(0, 3, (n, n, 1))
    # A moon behind it all.
    moon = np.sqrt((xx - 64) ** 2 + (yy - 56) ** 2)
    img[moon < 44, :3] = (64, 24, 34)
    img[moon < 40, :3] = (98, 30, 44)
    # A vein running down through the middle, branching.
    def vein(x0, y0, dx, length, width, seed):
        rr = rng(seed)
        x = x0
        for y in range(y0, min(n, y0 + length)):
            x += dx + rr.normal(0, 0.6)
            xi = int(round(x))
            img[y, max(0, xi - width):min(n, xi + width + 1), :3] = (200, 20, 40)
            img[y, max(0, xi - width - 1):max(0, xi - width), :3] = (90, 8, 20)
    vein(64, 0, 0, 128, 2, 7)
    vein(64, 40, -0.6, 50, 1, 8)
    vein(64, 60, 0.7, 45, 1, 9)
    # Two fangs at the bottom.
    for fx in (46, 82):
        for i in range(22):
            w = max(0, 6 - i // 4)
            img[100 - 22 + i, fx - w:fx + w + 1, :3] = (236, 230, 226)
    write_png(img, ROOT, "nightvein_logo.png")


# =============================================================================== sounds

def t(sec):
    return np.arange(int(sec * SR)) / SR


def env(n, attack=0.01, release=0.2):
    e = np.ones(n)
    a = max(1, int(attack * SR))
    r = max(1, int(release * SR))
    e[:a] = np.linspace(0, 1, a)
    e[-r:] *= np.linspace(1, 0, r)
    return e


def lowpass(x, k):
    """One-pole low pass, k in (0, 1]: smaller is darker."""
    y = np.zeros_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc += k * (v - acc)
        y[i] = acc
    return y


def highpass(x, k):
    return x - lowpass(x, k)


def sweep(f0, f1, sec, shape="sine"):
    f = np.geomspace(f0, f1, int(sec * SR))
    ph = 2 * np.pi * np.cumsum(f) / SR
    if shape == "saw":
        return 2 * ((ph / (2 * np.pi)) % 1) - 1
    return np.sin(ph)


def norm(x, peak=0.8):
    m = np.max(np.abs(x)) or 1
    return x / m * peak


def save(name, x, peak=0.8):
    sf.write(path(ASSETS, "sounds", name + ".ogg"), norm(x, peak).astype(np.float32), SR, format="OGG", subtype="VORBIS")


def thump(f0, sec, decay):
    tt = t(sec)
    return sweep(f0, f0 * 0.6, sec) * np.exp(-tt * decay)


def pad(x, sec):
    return np.concatenate([x, np.zeros(int(sec * SR))])


def mix(*parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[:len(p)] += p
    return out


def sounds():
    r = rng(100)
    # Lub-dub.
    beat = pad(thump(62, 0.16, 26), 0.06)
    save("heartbeat", np.concatenate([beat, thump(54, 0.2, 22), np.zeros(int(0.1 * SR))]), 0.9)

    # Teeth going in: a sharp crunch, then something wet.
    crunch = highpass(r.normal(0, 1, int(0.07 * SR)), 0.4) * env(int(0.07 * SR), 0.002, 0.05)
    wet = lowpass(r.normal(0, 1, int(0.35 * SR)), 0.05) * np.sin(2 * np.pi * 7 * t(0.35)) ** 2 * env(int(0.35 * SR), 0.03, 0.2)
    save("bite", np.concatenate([crunch, wet * 0.8]))

    # Turned: a swell from nothing that ends in one heavy heartbeat that doesn't come back.
    sec = 2.2
    drone = (sweep(40, 80, sec) + 0.5 * sweep(60, 121, sec, "saw") * 0.3) * np.linspace(0, 1, int(sec * SR)) ** 2
    swell = lowpass(r.normal(0, 1, int(sec * SR)), 0.08) * np.linspace(0, 1, int(sec * SR)) ** 3
    save("turned", np.concatenate([drone * 0.7 + swell * 0.6, thump(48, 0.9, 5) * 1.4]))

    # Cure: a bright chord blooming open.
    tt = t(1.6)
    chord = sum(np.sin(2 * np.pi * f * tt) for f in (523, 659, 784, 1046)) * env(len(tt), 0.3, 0.9)
    save("cure", chord + 0.2 * highpass(r.normal(0, 1, len(tt)), 0.5) * env(len(tt), 0.5, 0.8), 0.6)

    # Dash: a band of air sweeping past.
    n = int(0.35 * SR)
    air = r.normal(0, 1, n)
    sweep_k = np.geomspace(0.02, 0.5, n)
    acc, out = 0.0, np.zeros(n)
    for i in range(n):
        acc += sweep_k[i] * (air[i] - acc)
        out[i] = acc
    save("dash", out * env(n, 0.02, 0.2))

    # Mist: a long soft hiss.
    n = int(0.9 * SR)
    save("mist", highpass(r.normal(0, 1, n), 0.3) * env(n, 0.15, 0.6), 0.5)

    # Hypnotize: two sines beating slowly against each other, sinking.
    tt = t(1.4)
    save("hypnotize", (sweep(330, 220, 1.4) + sweep(334, 223, 1.4)) * env(len(tt), 0.2, 0.6), 0.6)

    # Bat: wings, fast.
    tt = t(0.5)
    save("bat", lowpass(r.normal(0, 1, len(tt)), 0.2) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 22 * tt))) * env(len(tt), 0.01, 0.2))

    # Tackle: a body hitting a body.
    click = highpass(r.normal(0, 1, int(0.02 * SR)), 0.5)
    save("tackle", mix(np.concatenate([click, np.zeros(int(0.3 * SR))]) * 0.5, thump(90, 0.32, 12) * 1.2), 0.9)

    # Bloodrush: chrome spinning up on a heartbeat.
    tt = t(0.8)
    spin = sweep(80, 640, 0.8, "saw") * 0.3 * env(len(tt), 0.05, 0.3)
    save("bloodrush", mix(lowpass(spin, 0.2), pad(thump(60, 0.16, 26), 0.6) * 0.8), 0.7)

    # Crash: everything winding down at once.
    tt = t(1.1)
    save("crash", lowpass(sweep(400, 40, 1.1, "saw"), 0.15) * env(len(tt), 0.01, 0.6) + thump(50, 1.1, 4) * 0.6, 0.8)

    # Nightwalker: breath through teeth.
    def breath(sec, seed, f):
        rr = rng(seed)
        n = int(sec * SR)
        x = lowpass(highpass(rr.normal(0, 1, n), 0.15), 0.35) * (0.5 + 0.5 * np.sin(np.pi * np.linspace(0, 1, n)))
        return x + 0.25 * np.sin(2 * np.pi * f * t(sec)) * env(n, sec * 0.3, sec * 0.4)
    save("nightwalker_ambient", breath(1.3, 201, 90), 0.5)
    save("nightwalker_hurt", np.concatenate([highpass(r.normal(0, 1, int(0.06 * SR)), 0.5) * 0.6, breath(0.35, 202, 140)]), 0.7)
    save("nightwalker_death", breath(1.8, 203, 70) * np.linspace(1, 0.2, int(1.8 * SR)) + sweep(160, 50, 1.8) * 0.3 * env(int(1.8 * SR), 0.1, 1), 0.7)


SUBTITLES = {
    "bite": "Teeth sink in", "heartbeat": "Heart pounds", "turned": "Something dies and doesn't", "cure": "Blood runs warm again",
    "dash": "Vampire dashes", "mist": "Mist hisses", "hypnotize": "A will gives way", "bat": "Wings flutter",
    "tackle": "Something hits the floor", "bloodrush": "Chrome drinks", "crash": "Chrome runs dry",
    "nightwalker_ambient": "Nightwalker breathes", "nightwalker_hurt": "Nightwalker hisses", "nightwalker_death": "Nightwalker crumbles",
}


def sounds_json():
    write_json({k: {"subtitle": f"subtitles.{NS}.{k}", "sounds": [f"{NS}:{k}"]} for k in SUBTITLES}, ASSETS, "sounds.json")


# =============================================================================== models

def models():
    for name in ITEMS:
        write_json({"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}}, ASSETS, "models", "item", name + ".json")
    write_json({"parent": "minecraft:item/template_spawn_egg"}, ASSETS, "models", "item", "nightwalker_spawn_egg.json")


# =============================================================================== lang

SKILL_TEXT = {
    "speed": ("Unnatural Speed", "Passive", ["+12% speed.", "+24% speed.", "+40% speed. Fast enough to tackle things you run into."]),
    "dash": ("Dash", "Press R", ["Throw yourself forward. Anything you hit gets tackled. 1 blood.", "Further, and back sooner."]),
    "mist_step": ("Mist Step", "Press G", ["Become mist for a moment and reappear up to 8 blocks ahead. 2 blood.", "14 blocks, and you drift straight through walls."]),
    "night_vision": ("Night Eyes", "Press N to toggle", ["See in the dark. Free."]),
    "bat_form": ("Bat Form", "Press B to toggle", ["Become a bat: small and flying, but you can't fight or dig. Costs blood the whole time. The sun knocks you out of it.", "Faster flight, half the cost."]),
    "fangs": ("Fangs", "Bite with V", ["Bites hit harder, drink more, and heal you a little.", "Harder, more, more.", "Your bite is a weapon now."]),
    "regen": ("Regeneration", "Passive, costs blood", ["Heal every 4 seconds while you have blood.", "Every 2.5 seconds.", "Every 1.5 seconds."]),
    "blood_sense": ("Blood Sense", "Press C to toggle", ["Feel every heartbeat within 16 blocks, through walls. Red: blood. Grey: nothing inside at all. Purple: your own kind.", "32 blocks."]),
    "hypnosis": ("Hypnosis", "Press X while looking at something", ["Hold a mob still for 5 seconds; players get dizzy and slow. 2 blood.", "10 seconds, shorter cooldown."]),
    "sun_ward": ("Sun Ward", "Passive", ["The sun burns you less often.", "The sun no longer burns, it only weakens. Bat form survives it.", "The sun means nothing to you now."]),
}


def lang():
    L = {
        "itemGroup.nightvein": "Nightvein",
        "item.nightvein.blood_bottle": "Bottle of Blood",
        "item.nightvein.blood_bottle.desc": "Vampires: +5 blood. Anyone else: regret.",
        "item.nightvein.hawthorn_tincture": "Hawthorn Tincture",
        "item.nightvein.hawthorn_tincture.desc": "Stops the turning, if you drink it in time.",
        "item.nightvein.sunlit_tincture": "Sunlit Tincture",
        "item.nightvein.sunlit_tincture.desc": "Cures a vampire. Drink it standing in full sun, and hold on.",
        "item.nightvein.nightwalker_spawn_egg": "Nightwalker Spawn Egg",
        "entity.nightvein.nightwalker": "Nightwalker",
        "death.attack.nightvein.bite": "%1$s was drained dry",
        "death.attack.nightvein.bite.player": "%1$s was drained dry by %2$s",
        "death.attack.nightvein.thirst": "%1$s died of thirst. The other kind",
        "death.attack.nightvein.thirst.player": "%1$s died of thirst while fighting %2$s",
        "key.categories.nightvein": "Nightvein",
        "key.nightvein.bite": "Bite",
        "key.nightvein.dash": "Dash",
        "key.nightvein.mist_step": "Mist Step",
        "key.nightvein.hypnosis": "Hypnosis",
        "key.nightvein.bat_form": "Bat Form",
        "key.nightvein.blood_sense": "Blood Sense",
        "key.nightvein.night_vision": "Night Eyes",
        "key.nightvein.skills": "Vampire skills",
        "hud.nightvein.bloodrush": "BLOODRUSH",
        "hud.nightvein.crash": "CHROME DRY",
        "screen.nightvein.skills": "The Thirst",
        "screen.nightvein.human": "Your blood is still warm. (Get bitten on the neck first.)",
        "screen.nightvein.turning": "Turning... %s%%",
        "screen.nightvein.points": "Points: %s   (next in %s blood)",
        "screen.nightvein.column.hunt": "Hunt",
        "screen.nightvein.column.night": "Night",
        "screen.nightvein.column.blood": "Blood",
        "screen.nightvein.column.mind": "Mind",
        "screen.nightvein.column.sun": "Sun",
        "screen.nightvein.next": "Next level:",
        "screen.nightvein.requires": "Needs %s first",
        "screen.nightvein.click": "Click to spend a point",
        "msg.nightvein.bitten": "Something bit your neck. Your heart is doing something strange. (A Hawthorn Tincture might still stop this.)",
        "msg.nightvein.turned": "Your heart stops. You don't.",
        "msg.nightvein.turned.hint": "You're a vampire. Press V to bite, K for your skills. Drink to earn skill points. Stay out of the sun.",
        "msg.nightvein.cured": "The sun goes through you, and you're warm again. Human.",
        "msg.nightvein.too_late": "Too late for hawthorn. You need the sun now.",
        "msg.nightvein.needs_sun": "It burns! It only works in full sunlight.",
        "msg.nightvein.sun_sick": "The sun feels wrong on your skin...",
        "msg.nightvein.sun_bat": "The sun tears you out of bat form",
        "msg.nightvein.thirst": "THIRSTY",
        "msg.nightvein.crash": "The chrome drank you dry. CRASH.",
        "msg.nightvein.no_blood": "Nothing to drink in that",
        "msg.nightvein.no_blood.backrooms": "There's no blood in it. There never was.",
        "msg.nightvein.no_target": "Nothing to look into",
        "msg.nightvein.cant_bat": "Can't become a bat right now",
        "msg.nightvein.mist_blocked": "Nowhere to reform",
        "msg.nightvein.cooldown": "Not yet: %ss",
        "msg.nightvein.hypnosis.immune": "Its will is too strong",
        "msg.nightvein.hypnosis.backrooms": "There's nothing behind its eyes to reach",
        "msg.nightvein.tackle": "TACKLED. It's down.",
        "msg.nightvein.tackle.freed": "TACKLED. It let go!",
        "msg.nightvein.upgrade.not_a_vampire": "Only vampires have skills",
        "msg.nightvein.upgrade.maxed": "Already maxed",
        "msg.nightvein.upgrade.locked": "Learn the one above it first",
        "msg.nightvein.upgrade.no_points": "Drink more blood to earn a point",
        "commands.nightvein.done": "Done (%s players)",
        "commands.nightvein.no_skill": "No such skill",
        "commands.nightvein.status": "%s: %s, %s blood, %s points, skills %s",
    }
    for skill, (title, key, levels) in SKILL_TEXT.items():
        L[f"skill.{NS}.{skill}"] = title
        L[f"skill.{NS}.{skill}.key"] = key
        for i, text in enumerate(levels, 1):
            L[f"skill.{NS}.{skill}.{i}"] = text
    for k, v in SUBTITLES.items():
        L[f"subtitles.{NS}.{k}"] = v
    write_json(dict(sorted(L.items())), ASSETS, "lang", "en_us.json")


# =============================================================================== data

def item(i):
    return i if ":" in i else f"{NS}:{i}"


def shapeless(name, ingredients, count=1, result=None):
    write_json({
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": [{"item": item(i)} for i in ingredients],
        "result": {"id": item(result or name), "count": count},
    }, DATA, "recipe", name + ".json")


def recipes():
    shapeless("hawthorn_tincture", ["minecraft:glass_bottle", "minecraft:sweet_berries", "minecraft:golden_carrot", "minecraft:sugar"])
    shapeless("sunlit_tincture", ["hawthorn_tincture", "minecraft:sunflower", "minecraft:golden_apple", "minecraft:glowstone_dust"])


def damage_types():
    write_json({"message_id": "nightvein.bite", "exhaustion": 0.1, "scaling": "when_caused_by_living_non_player"}, DATA, "damage_type", "bite.json")
    write_json({"message_id": "nightvein.thirst", "exhaustion": 0.0, "scaling": "never"}, DATA, "damage_type", "thirst.json")
    write_json({"values": [f"{NS}:thirst"]}, MC_DATA, "tags", "damage_type", "bypasses_armor.json")
    write_json({"values": [f"{NS}:thirst"]}, MC_DATA, "tags", "damage_type", "no_knockback.json")


def loot_and_spawns():
    write_json({
        "type": "minecraft:entity",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"{NS}:blood_bottle"}],
            "conditions": [{"condition": "minecraft:killed_by_player"},
                           {"condition": "minecraft:random_chance_with_enchanted_bonus", "unenchanted_chance": 0.35,
                            "enchanted_chance": {"type": "minecraft:linear", "base": 0.45, "per_level_above_first": 0.1},
                            "enchantment": "minecraft:looting"}],
        }, {
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": "minecraft:bone",
                         "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 2}}]}],
        }],
    }, DATA, "loot_table", "entities", "nightwalker.json")
    write_json({
        "type": "neoforge:add_spawns",
        "biomes": "#minecraft:is_overworld",
        "spawners": {"type": f"{NS}:nightwalker", "weight": 10, "minCount": 1, "maxCount": 2},
    }, DATA, "neoforge", "biome_modifier", "nightwalker_spawns.json")


def structure(name, size):
    """An empty GameTest template of the given size (gzipped NBT, written by hand: nothing in it but its bounds)."""
    import gzip

    def tag_name(t, n):
        b = n.encode()
        return bytes([t]) + struct.pack(">H", len(b)) + b

    body = tag_name(9, "size") + bytes([3]) + struct.pack(">i", 3) + b"".join(struct.pack(">i", v) for v in size)
    for empty in ("entities", "blocks", "palette"):
        body += tag_name(9, empty) + bytes([10]) + struct.pack(">i", 0)
    body += tag_name(3, "DataVersion") + struct.pack(">i", 3955)
    nbt = tag_name(10, "") + body + b"\x00"
    with open(path(DATA, "structure", name + ".nbt"), "wb") as f:
        f.write(gzip.compress(nbt, mtime=0))


def main():
    structure("arena", (9, 5, 9))
    for i, name in enumerate(ITEMS):
        write_png(icon(name, 10 + i), ASSETS, "textures", "item", name + ".png")
    for i, name in enumerate(SKILLS):
        write_png(icon(name, 40 + i), ASSETS, "textures", "gui", "skills", name + ".png")
    for fill in ("full", "half", "empty"):
        write_png(drop(fill), ASSETS, "textures", "gui", "sprites", "hud", f"blood_{fill}.png")
    nightwalker()
    logo()
    sounds()
    sounds_json()
    models()
    lang()
    recipes()
    damage_types()
    loot_and_spawns()


if __name__ == "__main__":
    main()
