package com.relicweave.vestige.client.boss.a;

/**
 * Shelf-packs cube texture rectangles left-to-right, top-to-bottom in declaration order. Mirrors {@code layout()} in
 * {@code tools/textures/gen_bosses_a.py}; keep the box tables of both in sync.
 */
final class BossTex {
    private BossTex() {}

    /** Returns {u, v} for each {w, h, d} box in order. */
    static int[][] layout(int texW, int[][] boxes) {
        int[][] out = new int[boxes.length][];
        int x = 0;
        int y = 0;
        int row = 0;
        for (int i = 0; i < boxes.length; i++) {
            int bw = 2 * (boxes[i][2] + boxes[i][0]);
            int bh = boxes[i][2] + boxes[i][1];
            if (x + bw > texW) {
                x = 0;
                y += row;
                row = 0;
            }
            out[i] = new int[]{x, y};
            x += bw;
            row = Math.max(row, bh);
        }
        return out;
    }
}
