"""Rough preview of the Oddlands density: Minecraft's noise differs in detail, but scales and proportions match."""
import numpy as np, opensimplex
from PIL import Image

def octave_noise(seed, base_octave, mods, amp, x, y=None, z=None):
    total = np.zeros_like(x, dtype=float)
    for k, m in enumerate(mods):
        if m == 0: continue
        f = 2.0 ** (base_octave + k)
        g = opensimplex.OpenSimplex(seed + k)
        if y is None:
            v = np.vectorize(lambda a, b: g.noise2(a * f, b * f))(x, z)
        else:
            v = np.vectorize(lambda a, b, c: g.noise3(a * f, b * f, c * f))(x, y, z)
        total += m * v / (2 ** k) * 1.0
    return total * amp * 1.4

def density(X, Y, Z):
    cont = octave_noise(1, -9, [1, 1, 1, .5], .94, X, None, Z)
    land = np.clip((cont + 0.05) * 3, -1, 1)
    hills = 30 * np.clip(octave_noise(2, -7, [1, .5, .25], .85, X, None, Z) * 2.5, -1, 1) + 20 * np.clip(octave_noise(3, -8, [1, .5], .9, X, None, Z) * 3, -1, 1) + 9 * np.clip(octave_noise(8, -6, [1, .5], .9, X, None, Z) * 4, -1, 1)
    surf = 150 + hills
    canyon = np.clip(1 - np.abs(octave_noise(4, -8, [1, .6], .9, X, None, Z)) / 0.07, 0, 1)
    top = surf - 58 * canyon
    th = 10 + 70 * land
    bottom = surf - th - 36 * np.abs(octave_noise(5, -4, [1, 1], .9, X, None, Z))
    rough = octave_noise(6, -5, [1, 1, .5], .85, X, Y, Z)
    isl = np.minimum((top - Y) / 8 + .35 * rough, (Y - bottom) / 5 + 1.2 * rough)
    islets = octave_noise(7, -6, [1, .6], .9, X, Y, Z) - .52 - np.abs(Y - 150) / 80
    d = np.maximum(isl, islets)
    d = np.minimum(d, (Y - 10) / 4); d = np.minimum(d, (300 - Y) / 6)
    return d

# side slice along x at z=0
W = 768
xs = np.arange(0, W, 2); ys = np.arange(0, 320, 2)
X, Y = np.meshgrid(xs, ys); Z = np.zeros_like(X)
d = density(X, Y, Z)
img = np.zeros((len(ys), len(xs), 3), np.uint8); img[:] = (190, 160, 225)
img[d > 0] = (120, 110, 140)
Image.fromarray(img[::-1]).resize((W, 320), Image.NEAREST).save('/tmp/side.png')
# top view: highest solid y
xs = np.arange(0, 512, 4); zs = np.arange(0, 512, 4)
X2, Z2 = np.meshgrid(xs, zs)
top = np.zeros(X2.shape)
for yy in range(300, 20, -6):
    d = density(X2, np.full(X2.shape, yy), Z2)
    top = np.where((top == 0) & (d > 0), yy, top)
v = np.clip((top - 60) / 160, 0, 1)
im = np.zeros(top.shape + (3,), np.uint8)
im[...] = (60, 40, 90)
im[top > 0] = np.stack([60 + 150 * v, 140 + 90 * v, 120 + 60 * v], -1)[top > 0]
Image.fromarray(im).resize((512, 512), Image.NEAREST).save('/tmp/top.png')
print("land fraction", (top > 0).mean())
