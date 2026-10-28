"""0.0.4.5.b dread HUD textures: a black radial vignette and white vein tendrils creeping in from the screen edges
(tinted at runtime). Both 256x256, drawn stretched over the screen with blur on."""
import json
import math
import random
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from vestige_palette import TEXTURES  # noqa: E402
from PIL import Image, ImageDraw, ImageFilter

SIZE = 256
OUT = TEXTURES / "gui" / "vestige"


def blur_meta(name):
    (OUT / f"{name}.png.mcmeta").write_text(json.dumps({"texture": {"blur": True, "clamp": True}}, indent=2) + "\n")


def vignette():
    img = Image.new("RGBA", (SIZE, SIZE))
    c = (SIZE - 1) / 2
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot((x - c) / c, (y - c) / c) / math.sqrt(2)  # 0 centre, 1 corner
            t = max(0.0, min(1.0, (d - 0.38) / 0.62))
            a = t * t * (3 - 2 * t)
            img.putpixel((x, y), (0, 0, 0, int(255 * a)))
    img.save(OUT / "dread_vignette.png")
    blur_meta("dread_vignette")


def veins():
    rng = random.Random(31337)
    layer = Image.new("L", (SIZE, SIZE), 0)
    draw = ImageDraw.Draw(layer)
    c = SIZE / 2

    def grow(x, y, angle, width, length, depth):
        for _ in range(length):
            nx = x + math.cos(angle) * 2.2
            ny = y + math.sin(angle) * 2.2
            draw.line((x, y, nx, ny), fill=255, width=max(1, int(width)))
            x, y = nx, ny
            angle += rng.uniform(-0.35, 0.35)
            width *= 0.985
            if math.hypot(x - c, y - c) < SIZE * 0.30:
                return
            if depth < 3 and rng.random() < 0.06:
                grow(x, y, angle + rng.choice((-1, 1)) * rng.uniform(0.5, 1.0), width * 0.7, int(length * 0.5), depth + 1)

    for _ in range(26):
        side = rng.randrange(4)
        p = rng.uniform(0, SIZE)
        x, y = [(p, 0), (SIZE, p), (p, SIZE), (0, p)][side]
        angle = math.atan2(c - y, c - x) + rng.uniform(-0.6, 0.6)
        grow(x, y, angle, rng.uniform(2.5, 4.5), rng.randint(20, 45), 0)

    layer = layer.filter(ImageFilter.GaussianBlur(0.8))
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot((x - c) / c, (y - c) / c) / math.sqrt(2)
            edge = max(0.0, min(1.0, (d - 0.35) / 0.45))
            a = int(layer.getpixel((x, y)) * edge)
            img.putpixel((x, y), (255, 255, 255, a))
    img.save(OUT / "dread_veins.png")
    blur_meta("dread_veins")


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    vignette()
    veins()
    print("dread textures written")
