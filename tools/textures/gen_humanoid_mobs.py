"""T06 textures: humanoid Vestige mobs (zombie 64x64 skin layout, skeleton 64x32) and the glyph bolt sprite.

Each body part is painted on the vanilla UV boxes (head 0,0 8x8x8; body 16,16 8x12x4; right arm 40,16 4x12x4;
right leg 0,16 4x12x4; left arm 32,48 and left leg 16,48 for 64x64). The second-layer regions (hat 32,0 and the
jacket/sleeve/pants overlays) are left transparent.
"""
import random
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from vestige_palette import *  # noqa: F401,F403
from PIL import Image

SKIN = [(88, 70, 64), (120, 98, 88), (150, 124, 108), (176, 150, 130), (200, 176, 154)]


def box(img, ox, oy, w, h, d, painter):
    """Paint the six faces of a Minecraft cube. painter(face, x, y, fw, fh) -> color; x,y are face-local pixels."""
    faces = {
        "top": (ox + d, oy, w, d),
        "bottom": (ox + d + w, oy, w, d),
        "right": (ox, oy + d, d, h),
        "front": (ox + d, oy + d, w, h),
        "left": (ox + d + w, oy + d, d, h),
        "back": (ox + 2 * d + w, oy + d, w, h),
    }
    for face, (fx, fy, fw, fh) in faces.items():
        for y in range(fh):
            for x in range(fw):
                img.putpixel((fx + x, fy + y), rgba(painter(face, x, y, fw, fh)))


class Style:
    """Palette and decoration choices for one mob."""

    def __init__(self, seed, skin, cloth, trim, eyes, hood=False, helmet=None, belt=True, tabard=None, bare_arms=False,
                 glow=None, cracks=False, tattered=False):
        self.rng = random.Random(seed)
        self.skin, self.cloth, self.trim, self.eyes = skin, cloth, trim, eyes
        self.hood, self.helmet, self.belt, self.tabard = hood, helmet, belt, tabard
        self.bare_arms, self.glow, self.cracks, self.tattered = bare_arms, glow, cracks, tattered

    def pick(self, ramp, lo=1, hi=3):
        return ramp[self.rng.randint(lo, hi)]


def head_painter(s):
    def paint(face, x, y, fw, fh):
        covered = s.hood and face != "front"
        if face == "front":
            if s.hood and (y < 2 or x < 1 or x > fw - 2):
                return s.pick(s.cloth, 0, 2)
            if y == 3 and x in (1, 2, 5, 6):
                return s.eyes
            if y == 4 and x in (1, 2, 5, 6) and s.glow:
                return s.glow
            if y == 6 and 2 <= x <= 5:
                return s.skin[0]
            if s.helmet and y < 2:
                return s.pick(s.helmet, 2, 4)
            return s.pick(s.skin, 1, 3)
        if s.helmet and (face == "top" or y < 3):
            return s.pick(s.helmet, 1, 3)
        if covered or face == "top":
            return s.pick(s.cloth, 0, 2)
        return s.pick(s.skin, 0, 2)
    return paint


def body_painter(s):
    def paint(face, x, y, fw, fh):
        if s.belt and face in ("front", "back", "left", "right") and y == fh - 4:
            return s.pick(s.trim, 2, 3)
        if face == "front" and s.tabard and fw >= 6 and 2 <= x <= fw - 3 and y > 0 and y < fh - 2:
            return s.pick(s.tabard, 1, 3)
        if face == "front" and s.cracks and (x + y * 3) % 7 == 0:
            return s.trim[4]
        if s.tattered and y > fh - 3 and (x + y) % 2 == 0:
            return CLEAR
        if face in ("front", "back") and (y == 0):
            return s.pick(s.trim, 1, 2)
        return s.pick(s.cloth, 1, 3)
    return paint


def limb_painter(s, sleeve_rows):
    def paint(face, x, y, fw, fh):
        if face == "bottom":
            return s.pick(s.skin, 0, 1)
        if y >= fh - 2 and not s.bare_arms:
            return s.pick(s.trim, 1, 2) if y == fh - 2 else s.pick(s.skin, 1, 3)
        if y < sleeve_rows:
            return s.pick(s.trim, 1, 3)
        return s.pick(s.cloth, 1, 3)
    return paint


def leg_painter(s):
    def paint(face, x, y, fw, fh):
        if face == "bottom":
            return s.pick(s.cloth, 0, 0)
        if y >= fh - 3:
            return s.pick(s.trim, 0, 2)
        return s.pick(s.cloth, 0, 2)
    return paint


def humanoid(name, s):
    img = Image.new("RGBA", (64, 64), CLEAR)
    box(img, 0, 0, 8, 8, 8, head_painter(s))
    box(img, 16, 16, 8, 12, 4, body_painter(s))
    box(img, 40, 16, 4, 12, 4, limb_painter(s, 3))
    box(img, 0, 16, 4, 12, 4, leg_painter(s))
    box(img, 32, 48, 4, 12, 4, limb_painter(s, 3))
    box(img, 16, 48, 4, 12, 4, leg_painter(s))
    save(img, "entity/" + name)


def skeleton(name, s):
    img = Image.new("RGBA", (64, 32), CLEAR)
    bone = s.skin

    def head(face, x, y, fw, fh):
        if face == "front":
            if y == 3 and x in (1, 2, 5, 6):
                return s.eyes
            if y == 3 and x in (3, 4):
                return bone[0]
            if y == 5 and x % 2 == 0 and 1 <= x <= 6:
                return bone[0]
            if y < 1:
                return s.pick(s.trim, 2, 4)
        if face == "top":
            return s.pick(s.trim, 1, 3)
        return s.pick(bone, 2, 4)

    def torso(face, x, y, fw, fh):
        if y % 3 == 2 and face in ("front", "back"):
            return s.pick(bone, 0, 1)
        if face == "front" and s.tabard and 2 <= x <= fw - 3:
            return s.pick(s.tabard, 1, 3)
        return s.pick(s.cloth, 1, 3) if y < 2 or y > fh - 4 else s.pick(bone, 2, 4)

    def arm(face, x, y, fw, fh):
        if y < 2:
            return s.pick(s.trim, 1, 3)
        return s.pick(bone, 2, 4) if y % 4 else s.pick(bone, 0, 1)

    def leg(face, x, y, fw, fh):
        if y >= fh - 2:
            return s.pick(s.trim, 0, 2)
        return s.pick(s.cloth, 0, 2)

    box(img, 0, 0, 8, 8, 8, head)
    box(img, 16, 16, 8, 12, 4, torso)
    box(img, 40, 16, 2, 12, 2, arm)
    box(img, 0, 16, 2, 12, 2, leg)
    save(img, "entity/" + name)


def glyph_bolt():
    img = new(16)
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d < 2.2:
                img.putpixel((x, y), rgba(OPAL[4]))
            elif d < 3.6:
                img.putpixel((x, y), rgba(TEAL[4]))
            elif d < 5.0:
                img.putpixel((x, y), rgba(TEAL[2]))
            elif d < 7.0:
                img.putpixel((x, y), rgba(TEAL[1], 120))
    # Four glyph ticks.
    for i in range(2, 5):
        for px, py in ((8, 1 + i // 2), (8, 14 - i // 2), (1 + i // 2, 8), (14 - i // 2, 8)):
            img.putpixel((px, py), rgba(TEAL[3]))
    save(img, "entity/glyph_bolt")


def main():
    humanoid("reliquary_sentinel", Style(601, SKIN, IVORY, GOLD, GOLD[4], helmet=IVORY, tabard=TEAL))
    humanoid("scriptbound_scribe", Style(602, SKIN, INK, TEAL, TEAL[4], hood=True, tabard=VIOLET, glow=TEAL[3]))
    humanoid("gilded_husk", Style(603, MOSS, GOLD, IVORY, EMBER[3], cracks=True, tattered=True, bare_arms=True))
    humanoid("mirebound_cantor", Style(604, MOSS, MOSS, OPAL, OPAL[4], hood=True, tattered=True, glow=OPAL[3]))
    humanoid("oathbreaker_knight", Style(605, INK, INK, VIOLET, VIOLET[4], helmet=INK, tabard=VIOLET, cracks=True))
    humanoid("vestige_drifter", Style(606, SKIN, OPAL, TEAL, INK[1], hood=True, belt=False, tattered=True))
    skeleton("rift_archer", Style(607, OPAL, VIOLET, INK, VIOLET[4], tabard=INK))
    humanoid("echo_weaver", Style(608, VIOLET, VIOLET, OPAL, OPAL[4], hood=True, glow=TEAL[4], tattered=True, belt=False))
    glyph_bolt()


if __name__ == "__main__":
    main()
