package com.relicweave.vestige.client.entity.beast;

/**
 * Shelf-packs cube texture rectangles left-to-right, top-to-bottom in declaration order. Mirrors {@code layout()} in
 * {@code tools/textures/gen_beast_mobs.py}, so the generated skins match the models without hand-written UV offsets.
 */
final class TexLayout {
    private final int texW;
    private int x;
    private int y;
    private int row;

    TexLayout(int texW) {
        this.texW = texW;
    }

    /** Returns {u, v} for the next w*h*d cube. */
    int[] next(int w, int h, int d) {
        int bw = 2 * (d + w);
        int bh = d + h;
        if (x + bw > texW) {
            x = 0;
            y += row;
            row = 0;
        }
        int[] uv = {x, y};
        x += bw;
        row = Math.max(row, bh);
        return uv;
    }
}
