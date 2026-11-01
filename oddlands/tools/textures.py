"""Procedural 16x16 textures for the Oddlands. Deterministic: every texture is seeded by its name."""
import math
import random
import zlib

from PIL import Image

S = 16


def rng(name):
    return random.Random(zlib.crc32(name.encode()))


def img(alpha=False):
    return Image.new("RGBA", (S, S), (0, 0, 0, 0) if alpha else (0, 0, 0, 255))


def jitter(c, r, amt=8):
    return tuple(max(0, min(255, v + r.randint(-amt, amt))) for v in c[:3]) + ((c[3],) if len(c) > 3 else (255,))


def value_noise(r, scale=4):
    """Smooth-ish value noise on a 16x16 torus, 0..1."""
    g = [[r.random() for _ in range(scale)] for _ in range(scale)]

    def at(x, y):
        fx, fy = x / S * scale, y / S * scale
        x0, y0 = int(fx) % scale, int(fy) % scale
        x1, y1 = (x0 + 1) % scale, (y0 + 1) % scale
        tx, ty = fx - int(fx), fy - int(fy)
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        a = g[y0][x0] * (1 - tx) + g[y0][x1] * tx
        b = g[y1][x0] * (1 - tx) + g[y1][x1] * tx
        return a * (1 - ty) + b * ty

    return at


def noisy(name, palette, scale=4, grain=0.35, amt=6):
    r = rng(name)
    n = value_noise(r, scale)
    im = img()
    px = im.load()
    for y in range(S):
        for x in range(S):
            v = n(x, y) * (1 - grain) + r.random() * grain
            px[x, y] = jitter(palette[min(len(palette) - 1, int(v * len(palette)))], r, amt)
    return im


def eye(im, x, y, size=2, pupil=(30, 20, 40), white=(245, 240, 230), look=(0, 0)):
    px = im.load()
    for dy in range(size):
        for dx in range(size):
            if 0 <= x + dx < S and 0 <= y + dy < S:
                px[x + dx, y + dy] = white + (255,)
    if size == 1:
        px[x, y] = pupil + (255,)
        return
    cx = x + (size - 1) // 2 + (1 if look[0] > 0 and size > 2 else 0)
    cy = y + size // 2 - (1 if size == 2 else 0) + (1 if look[1] > 0 else 0)
    if 0 <= cx < S and 0 <= cy < S:
        px[cx, cy] = pupil + (255,)
    if size >= 3:
        px[min(S - 1, cx + 1), cy] = pupil + (255,)


def speckle(im, name, colors, chance=0.08):
    r = rng(name + "speckle")
    px = im.load()
    for y in range(S):
        for x in range(S):
            if r.random() < chance:
                px[x, y] = jitter(r.choice(colors), r, 6)
    return im


def bricks(name, palette, mortar, rows=4, offset=True, eyes=0):
    im = noisy(name, palette, 4, 0.5)
    px = im.load()
    h = S // rows
    r = rng(name + "b")
    for y in range(S):
        row = y // h
        if y % h == h - 1:
            for x in range(S):
                px[x, y] = jitter(mortar, r, 4)
            continue
        shift = (S // 4) if (offset and row % 2) else 0
        for x in range(S):
            if (x + shift) % (S // 2) == S // 2 - 1:
                px[x, y] = jitter(mortar, r, 4)
    for _ in range(eyes):
        eye(im, r.randint(1, 12), r.randint(0, rows - 1) * h + 1, 2)
    return im


def bevel(im, light=18, dark=-22):
    px = im.load()
    for i in range(S):
        for (x, y, d) in ((i, 0, light), (0, i, light), (i, S - 1, dark), (S - 1, i, dark)):
            c = px[x, y]
            px[x, y] = tuple(max(0, min(255, v + d)) for v in c[:3]) + (c[3],)
    return im


def planks(name, palette, seam):
    r = rng(name)
    im = img()
    px = im.load()
    for y in range(S):
        band = y // 4
        base = palette[band % len(palette)]
        for x in range(S):
            if y % 4 == 3:
                px[x, y] = jitter(seam, r, 4)
            else:
                grain = 10 if (x + band * 5) % 7 == 0 else 0
                px[x, y] = jitter(tuple(max(0, v - grain) for v in base), r, 6)
        if y % 4 == 3:
            continue
    for band in range(4):
        x = (band * 7 + 3) % S
        for y in range(band * 4, band * 4 + 3):
            px[x, y] = jitter(seam, r, 4)
    return im


def bark(name, palette, knot_eye=True):
    r = rng(name)
    im = img()
    px = im.load()
    cols = [r.choice(palette) for _ in range(S)]
    for x in range(S):
        for y in range(S):
            c = cols[x]
            if r.random() < 0.15:
                c = palette[0]
            px[x, y] = jitter(c, r, 8)
    if knot_eye:
        kx, ky = r.randint(4, 9), r.randint(4, 9)
        for dy in range(-1, 3):
            for dx in range(-1, 4):
                px[kx + dx, ky + dy] = jitter(palette[0], r, 4)
        eye(im, kx, ky, 3, look=(1, 0))
    return im


def rings(name, outer, inner_palette):
    r = rng(name)
    im = img()
    px = im.load()
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 6.8:
                c = outer
            else:
                c = inner_palette[int(d) % len(inner_palette)]
            px[x, y] = jitter(c, r, 5)
    return im


def stripes(name, colors, heights):
    r = rng(name)
    im = img()
    px = im.load()
    y = 0
    i = 0
    while y < S:
        h = heights[i % len(heights)]
        for yy in range(y, min(S, y + h)):
            for x in range(S):
                px[x, yy] = jitter(colors[i % len(colors)], r, 7)
        y += h
        i += 1
    return im


def overlay_top(side, top, depth_by_x):
    """Drip the top texture down over the side like grass block sides."""
    out = side.copy()
    sp, tp = out.load(), top.load()
    for x in range(S):
        for y in range(depth_by_x[x]):
            sp[x, y] = tp[x, y]
    return out


def translucent(im, alpha):
    px = im.load()
    for y in range(S):
        for x in range(S):
            c = px[x, y]
            px[x, y] = c[:3] + (alpha if c[3] > 0 else 0,)
    return im


def cross_canvas():
    return img(alpha=True)


def put(px, x, y, c):
    if 0 <= x < S and 0 <= y < S:
        px[x, y] = c if len(c) == 4 else c + (255,)


def blades(name, colors, eye_count=2, height=(7, 13)):
    r = rng(name)
    im = cross_canvas()
    px = im.load()
    for b in range(6):
        x = 1 + b * 2 + r.randint(0, 1)
        h = r.randint(*height)
        lean = r.choice((-1, 0, 1))
        for i in range(h):
            xx = x + (lean * i) // 6
            put(px, xx, S - 1 - i, jitter(colors[min(len(colors) - 1, i * len(colors) // h)], r, 6))
    for _ in range(eye_count):
        ex = r.randint(2, 12)
        ey = r.randint(4, 9)
        eye(im, ex, ey, 2)
    return im


def stem(px, x, y0, y1, color, r):
    for y in range(y0, y1):
        put(px, x, y, jitter(color, r, 5))


def blob(px, cx, cy, rx, ry, palette, r, shade=True):
    for y in range(S):
        for x in range(S):
            d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
            if d <= 1:
                i = 0 if not shade else min(len(palette) - 1, int(d * len(palette)))
                if shade and y < cy - ry * 0.4 and x < cx:
                    i = 0
                put(px, x, y, jitter(palette[i], r, 5))


def save(im, path):
    im.save(path)


# ---------------------------------------------------------------- the roster

LILAC = [(98, 88, 112), (112, 102, 126), (126, 116, 140), (140, 130, 152)]
MORTAR = (72, 64, 86)
MOSS = [(52, 140, 128), (64, 158, 140), (78, 176, 152), (96, 192, 166)]
MULCH = [(70, 46, 60), (84, 56, 70), (98, 66, 82), (110, 76, 92)]
CHALK = [(206, 196, 174), (218, 210, 190), (228, 222, 204), (238, 234, 218)]
BONE = [(176, 166, 142), (192, 184, 160), (206, 198, 176), (220, 212, 190)]
RUST = [(146, 72, 40), (164, 84, 46), (182, 98, 56), (198, 114, 66)]
CRUST = [(26, 28, 48), (34, 36, 60), (44, 48, 76), (56, 60, 92)]
BARK = [(92, 56, 78), (112, 70, 94), (128, 82, 108), (146, 96, 124)]
PLANK = [(168, 116, 142), (182, 128, 154), (156, 106, 132), (176, 122, 148)]
JELLY = [(168, 232, 140), (140, 214, 124), (190, 242, 160), (226, 170, 210)]
CAP = [(118, 66, 150), (136, 80, 170), (154, 96, 186), (104, 56, 132)]


def build(out_dir):
    T = {}

    def add(name, im):
        T[name] = im

    knob = speckle(noisy("knobstone", LILAC, 4, 0.45), "knob", [(84, 76, 98), (150, 142, 164)], 0.10)
    add("knobstone", knob)
    add("knobstone_bricks", bevel(bricks("knobstone_bricks", LILAC[1:], MORTAR)))
    pol = noisy("polished_knobstone", LILAC[2:], 2, 0.2, 4)
    add("polished_knobstone", bevel(pol, 22, -26))
    ch = bevel(noisy("chiseled_knobstone", LILAC[1:3], 2, 0.2, 4), 20, -24)
    px = ch.load()
    for x in range(3, 13):
        put(px, x, 3, MORTAR)
        put(px, x, 12, MORTAR)
    for y in range(3, 13):
        put(px, 3, y, MORTAR)
        put(px, 12, y, MORTAR)
    eye(ch, 5, 6, 2)
    eye(ch, 9, 6, 2)
    for x in range(6, 10):
        put(px, x, 10, (60, 40, 70))
    add("chiseled_knobstone", ch)

    ore = knob.copy()
    r = rng("ore")
    opx = ore.load()
    for _ in range(5):
        cx, cy = r.randint(2, 13), r.randint(2, 13)
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (-1, 0)):
            put(opx, cx + dx, cy + dy, jitter(r.choice([(206, 118, 76), (230, 146, 98), (118, 186, 152)]), r, 6))
    add("copperknob_ore", ore)

    moss_top = speckle(noisy("squishmoss_top", MOSS, 4, 0.4), "moss", [(230, 140, 176), (240, 200, 120)], 0.05)
    add("squishmoss_top", moss_top)
    mulch = speckle(noisy("mulch", MULCH, 4, 0.5), "mulch", [(140, 100, 70), (60, 38, 50)], 0.08)
    add("mulch", mulch)
    r = rng("mossside")
    add("squishmoss_side", overlay_top(mulch, moss_top, [r.randint(3, 6) for _ in range(S)]))
    cushion = noisy("squish_cushion", MOSS[1:], 2, 0.2, 4)
    cpx = cushion.load()
    for i in range(S):
        put(cpx, i, i, (40, 118, 108))
        put(cpx, S - 1 - i, i, (40, 118, 108))
    for (x, y) in ((7, 3), (3, 7), (11, 7), (7, 11)):
        put(cpx, x, y, (240, 200, 120))
    add("squish_cushion", bevel(cushion, 16, -16))

    add("chalkdust", speckle(noisy("chalkdust", CHALK, 4, 0.5, 4), "chalk", [(170, 160, 140), (150, 140, 124)], 0.06))
    bone = noisy("bonestone", BONE, 4, 0.4)
    bpx = bone.load()
    r = rng("pores")
    for _ in range(9):
        x, y = r.randint(0, 15), r.randint(0, 15)
        put(bpx, x, y, (120, 110, 92))
        put(bpx, min(15, x + 1), y, (140, 130, 110))
    add("bonestone", bone)
    add("chalk_bricks", bevel(bricks("chalk_bricks", CHALK[1:], (164, 154, 132), 4)))

    add("rustrock", noisy("rustrock", RUST, 4, 0.45))
    add("banded_rustrock", stripes("banded_rustrock", [RUST[2], (120, 58, 40), RUST[1], (214, 156, 100), RUST[3], (96, 48, 36)], [3, 2, 3, 1, 4, 3]))
    tiles = noisy("rustrock_tiles", RUST[1:], 2, 0.3, 5)
    tpx = tiles.load()
    for i in range(S):
        for j in (0, 8):
            put(tpx, i, j, (112, 54, 34))
            put(tpx, j, i, (112, 54, 34))
    add("rustrock_tiles", bevel(tiles))

    crust = noisy("dripcrust", CRUST, 4, 0.4)
    speckle(crust, "glint", [(150, 200, 255), (200, 160, 255)], 0.04)
    add("dripcrust", crust)

    add("bulbwood_log", bark("bulbwood_log", BARK))
    add("bulbwood_log_top", rings("bulbwood_log_top", BARK[1], [(200, 154, 176), (176, 128, 152)]))
    add("bulbwood_planks", planks("bulbwood_planks", PLANK, (120, 78, 100)))

    jelly = noisy("jellyleaf", JELLY[:3], 4, 0.5)
    speckle(jelly, "jellyb", [JELLY[3]], 0.06)
    jpx = jelly.load()
    r = rng("jellyholes")
    for _ in range(14):
        jpx[r.randint(0, 15), r.randint(0, 15)] = (0, 0, 0, 0)
    add("jellyleaf", translucent(jelly, 190))

    stemt = stripes("gawkcap_stem", [(214, 200, 228), (196, 180, 214), (226, 214, 236)], [1, 2, 1])
    stemt = stemt.rotate(90)
    add("gawkcap_stem", stemt)
    add("gawkcap_stem_top", rings("gawkcap_stem_top", (190, 174, 210), [(226, 214, 236), (204, 190, 222)]))
    cap = noisy("gawkcap_cap", CAP, 4, 0.4)
    speckle(cap, "capglow", [(120, 255, 230), (180, 255, 240)], 0.07)
    eye(cap, 4, 5, 3)
    eye(cap, 10, 10, 2)
    add("gawkcap_cap", cap)

    sap = noisy("sap_glob", [(220, 140, 40), (236, 164, 56), (246, 190, 90)], 3, 0.3)
    speckle(sap, "sapb", [(255, 230, 170)], 0.05)
    add("sap_glob", translucent(sap, 200))

    lantern = noisy("peeper_lantern", LILAC[1:], 2, 0.2, 4)
    lpx = lantern.load()
    for y in range(3, 13):
        for x in range(3, 13):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 5:
                put(lpx, x, y, (180, 255, 230) if d > 3 else (240, 255, 250))
    eye(lantern, 6, 6, 4, pupil=(40, 30, 70))
    add("peeper_lantern", bevel(lantern, 24, -26))

    rib = stripes("ribstone", [(232, 226, 206), (214, 206, 184), (196, 188, 166)], [2, 1, 2, 1]).rotate(90)
    add("ribstone", rib)
    add("ribstone_top", rings("ribstone_top", (200, 192, 170), [(176, 166, 142), (230, 224, 204)]))

    # -------- plants (cutout)
    add("peeper_grass", blades("peeper_grass", [(36, 110, 104), (56, 150, 134), (92, 196, 168)], 2))
    add("ribgrass", blades("ribgrass", [(200, 190, 168), (222, 216, 196), (240, 236, 222)], 0, (5, 10)))
    sulk = blades("sulkweed", [(84, 96, 90), (110, 124, 112), (140, 150, 134)], 1, (8, 14))
    add("sulkweed", sulk)

    def flower(name, petals, center_eye=True):
        r = rng(name)
        im = cross_canvas()
        px = im.load()
        stem(px, 7, 8, 16, (52, 130, 100), r)
        stem(px, 8, 11, 16, (52, 130, 100), r)
        put(px, 9, 12, (70, 160, 120))
        put(px, 6, 13, (70, 160, 120))
        blob(px, 7.5, 5, 5, 4, petals, r)
        if center_eye:
            eye(im, 6, 3, 4)
        return im

    add("blinkbloom", flower("blinkbloom", [(240, 150, 190), (220, 110, 160), (190, 80, 140)]))

    def mushroom(name, cap_palette, eye_size=3):
        r = rng(name)
        im = cross_canvas()
        px = im.load()
        for y in range(9, 16):
            put(px, 7, y, jitter((222, 210, 232), r, 5))
            put(px, 8, y, jitter((206, 194, 220), r, 5))
        blob(px, 7.5, 7, 6, 4, cap_palette, r)
        for x in range(2, 14):
            if px[x, 9][3] > 0 and x % 3 == 0:
                put(px, x, 9, (120, 255, 230))
        eye(im, 6, 5, eye_size)
        return im

    add("gawkcap", mushroom("gawkcap", CAP))

    r = rng("glowgulp")
    gg = cross_canvas()
    gpx = gg.load()
    stem(gpx, 7, 11, 16, (60, 120, 110), r)
    stem(gpx, 8, 12, 16, (60, 120, 110), r)
    blob(gpx, 7.5, 7, 5, 5, [(220, 255, 245), (150, 250, 230), (90, 220, 210), (60, 170, 180)], r)
    for x in range(5, 11):
        put(gpx, x, 9, (40, 90, 100))
    put(gpx, 6, 10, (40, 90, 100))
    put(gpx, 9, 10, (40, 90, 100))
    eye(gg, 5, 4, 2)
    eye(gg, 9, 4, 2)
    add("glowgulp", gg)

    r = rng("dangleroot")
    dr = cross_canvas()
    dpx = dr.load()
    for x in (2, 5, 7, 10, 13):
        length = r.randint(8, 15)
        wob = 0
        for y in range(length):
            if r.random() < 0.25:
                wob = max(-1, min(1, wob + r.choice((-1, 1))))
            put(dpx, x + wob, y, jitter((150, 110, 90), r, 8))
        put(dpx, x + wob, length, (150, 255, 220))
        put(dpx, x + wob, length + 1, (90, 220, 190))
    add("dangleroot", dr)

    r = rng("husk")
    hs = cross_canvas()
    hpx = hs.load()
    for b in range(5):
        x, y = 7 + r.randint(-1, 1), 15
        dx = r.choice((-1, 1))
        for i in range(r.randint(7, 12)):
            put(hpx, x, y, jitter((122, 92, 62), r, 8))
            y -= 1
            if r.random() < 0.4:
                x += dx
    add("husk_shrub", hs)

    r = rng("dustpuff")
    dp = cross_canvas()
    ppx = dp.load()
    blob(ppx, 7.5, 10, 6, 5, [(240, 230, 210), (226, 214, 190), (204, 190, 166), (180, 164, 140)], r)
    eye(dp, 5, 9, 1)
    eye(dp, 9, 9, 1)
    put(ppx, 7, 12, (120, 90, 80))
    put(ppx, 8, 12, (120, 90, 80))
    add("dustpuff", dp)

    r = rng("tooth")
    tc = cross_canvas()
    tpx2 = tc.load()
    for (x, h, w) in ((3, 6, 2), (7, 11, 3), (11, 8, 2)):
        for y in range(h):
            for dx in range(w if y < h - 2 else w - 1):
                put(tpx2, x + dx, 15 - y, jitter((246, 242, 228) if dx else (220, 214, 196), r, 4))
        put(tpx2, x, 15 - h + 1, (255, 255, 255))
    add("tooth_crystal", tc)

    r = rng("sprout")
    sp = cross_canvas()
    spx = sp.load()
    stem(spx, 7, 7, 16, (110, 72, 92), r)
    blob(spx, 7.5, 5, 4, 3, JELLY, r)
    eye(sp, 6, 4, 2)
    add("wobble_sprout", sp)

    # -------- wooden things derived from planks
    door_b = planks("bulbwood_door_bottom", PLANK, (120, 78, 100))
    door_t = planks("bulbwood_door_top", PLANK, (120, 78, 100))
    tpx3 = door_t.load()
    for y in range(4, 11):
        for x in range(4, 12):
            if math.hypot(x - 7.5, y - 7.5) < 3.6:
                put(tpx3, x, y, (0, 0, 0, 0))
    eye(door_t, 7, 7, 1, white=(245, 240, 230))
    add("bulbwood_door_bottom", bevel(door_b))
    add("bulbwood_door_top", bevel(door_t))
    trap = planks("bulbwood_trapdoor", PLANK, (120, 78, 100))
    trpx = trap.load()
    for (x, y) in ((4, 4), (11, 4), (4, 11), (11, 11)):
        for dx in (0, 1):
            for dy in (0, 1):
                put(trpx, x + dx - 1, y + dy - 1, (0, 0, 0, 0))
    add("bulbwood_trapdoor", bevel(trap))
    item_door = cross_canvas()
    idpx = item_door.load()
    dbp, dtp = door_b.load(), door_t.load()
    for y in range(8):
        for x in range(4, 12):
            idpx[x, y] = dtp[x, y * 2]
            idpx[x, y + 8] = dbp[x, y * 2]
    add("item/bulbwood_door", item_door)

    # -------- the portal (animated)
    frames = []
    for f in range(8):
        r = rng("wish%d" % f)
        fr = img()
        fpx = fr.load()
        for y in range(S):
            for x in range(S):
                a = math.atan2(y - 7.5, x - 7.5)
                d = math.hypot(x - 7.5, y - 7.5)
                v = 0.5 + 0.5 * math.sin(a * 3 + d * 0.9 - f * math.pi / 4)
                base = (40 + int(60 * v), 170 + int(70 * v), 170 + int(60 * v))
                if v > 0.86:
                    base = (230, 150 + int(40 * v), 90)
                fpx[x, y] = jitter(base, r, 6)[:3] + (200,)
        frames.append(fr)
    strip = Image.new("RGBA", (S, S * len(frames)))
    for i, fr in enumerate(frames):
        strip.paste(fr, (0, i * S))
    add("wishing_water", strip)

    import os
    for name, im in T.items():
        path = os.path.join(out_dir, name + ".png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        im.save(path)
    return T
