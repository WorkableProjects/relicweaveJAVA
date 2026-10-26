"""T16 textures: the Archivist's Mark mob effect icon (18x18, gold disc with a crosshair mark)."""
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from vestige_palette import *  # noqa: F401,F403


def archivists_mark():
    img = new(18)
    for y in range(18):
        for x in range(18):
            if (x - 8.5) ** 2 + (y - 8.5) ** 2 <= 8.5 ** 2:
                img.putpixel((x, y), rgba(GOLD[1 if (x + y) % 5 else 0]))
    # Crosshair ring with ticks and a bright centre.
    for y in range(18):
        for x in range(18):
            d = (x - 8.5) ** 2 + (y - 8.5) ** 2
            if 14 <= d <= 22:
                img.putpixel((x, y), rgba(GOLD[4]))
    for i in (2, 3, 4, 13, 14, 15):
        img.putpixel((8, i), rgba(GOLD[4]))
        img.putpixel((9, i), rgba(GOLD[4]))
        img.putpixel((i, 8), rgba(GOLD[4]))
        img.putpixel((i, 9), rgba(GOLD[4]))
    for x in (8, 9):
        for y in (8, 9):
            img.putpixel((x, y), rgba(OPAL[4]))
    save(img, "mob_effect/archivists_mark")


if __name__ == "__main__":
    archivists_mark()
