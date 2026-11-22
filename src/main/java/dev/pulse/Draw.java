package dev.pulse;

/** Мелкие помощники рисования поверх Gfx.fill. */
final class Draw {
    static int argb(int a, int r, int g, int b) { return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255); }
    static int alpha(int c, int a) { return (Math.max(0, Math.min(255, a)) << 24) | (c & 0xFFFFFF); }
    static int mulA(int c, double f) { return alpha(c, (int) (((c >>> 24) & 255) * Math.max(0, Math.min(1, f)))); }

    static int mix(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = 0;
        for (int s = 24; s >= 0; s -= 8) r |= ((int) (((a >>> s) & 255) * (1 - t) + ((b >>> s) & 255) * t) & 255) << s;
        return r;
    }

    static int hsv(double h, double s, double v) {
        h = ((h % 1) + 1) % 1; double c = v * s, x = c * (1 - Math.abs((h * 6) % 2 - 1)), m = v - c, r, g, b;
        int i = (int) (h * 6);
        switch (i) { case 0: r = c; g = x; b = 0; break; case 1: r = x; g = c; b = 0; break; case 2: r = 0; g = c; b = x; break;
            case 3: r = 0; g = x; b = c; break; case 4: r = x; g = 0; b = c; break; default: r = c; g = 0; b = x; }
        return argb(255, (int) ((r + m) * 255), (int) ((g + m) * 255), (int) ((b + m) * 255));
    }

    static boolean ok(double v) { return !Double.isNaN(v) && Math.abs(v) < 20000; }

    static void outline(Gfx g, int x1, int y1, int x2, int y2, int c, int t) {
        g.fill(x1, y1, x2, y1 + t, c); g.fill(x1, y2 - t, x2, y2, c);
        g.fill(x1, y1 + t, x1 + t, y2 - t, c); g.fill(x2 - t, y1 + t, x2, y2 - t, c);
    }

    static void corners(Gfx g, int x1, int y1, int x2, int y2, int c, int len, int t) {
        len = Math.max(3, Math.min(len, Math.min(x2 - x1, y2 - y1) / 2));
        g.fill(x1, y1, x1 + len, y1 + t, c); g.fill(x1, y1, x1 + t, y1 + len, c);
        g.fill(x2 - len, y1, x2, y1 + t, c); g.fill(x2 - t, y1, x2, y1 + len, c);
        g.fill(x1, y2 - t, x1 + len, y2, c); g.fill(x1, y2 - len, x1 + t, y2, c);
        g.fill(x2 - len, y2 - t, x2, y2, c); g.fill(x2 - t, y2 - len, x2, y2, c);
    }

    static void line(Gfx g, double x1, double y1, double x2, double y2, int c, int t) {
        if (!ok(x1) || !ok(y1) || !ok(x2) || !ok(y2)) return;
        double dx = x2 - x1, dy = y2 - y1; int n = (int) Math.max(1, Math.max(Math.abs(dx), Math.abs(dy)));
        if (n > 1500) return;
        for (int i = 0; i <= n; i++) { int x = (int) Math.round(x1 + dx * i / n), y = (int) Math.round(y1 + dy * i / n); g.fill(x, y, x + t, y + t, c); }
    }

    static void dot(Gfx g, double x, double y, int s, int c) {
        if (!ok(x) || !ok(y)) return;
        int ix = (int) Math.round(x) - s / 2, iy = (int) Math.round(y) - s / 2; g.fill(ix, iy, ix + s, iy + s, c);
    }

    /** Скруглённая плашка (срезанные углы). */
    static void pill(Gfx g, int x1, int y1, int x2, int y2, int c) {
        g.fill(x1 + 1, y1, x2 - 1, y2, c); g.fill(x1, y1 + 1, x2, y2 - 1, c);
    }

    static void textC(Gfx g, String s, double cx, int y, int c) { g.text(s, (int) cx - g.textWidth(s) / 2, y, c); }

    /** Экранный прямоугольник вокруг 3D-бокса или null, если часть бокса за камерой. */
    static int[] box(Data.Frame f, double x1, double y1, double z1, double x2, double y2, double z2) {
        double[] xs = {x1, x2}, ys = {y1, y2}, zs = {z1, z2};
        double minx = 1e9, miny = 1e9, maxx = -1e9, maxy = -1e9;
        for (double x : xs) for (double y : ys) for (double z : zs) {
            double[] p = f.proj.p(x, y, z); if (p == null) return null;
            minx = Math.min(minx, p[0]); maxx = Math.max(maxx, p[0]); miny = Math.min(miny, p[1]); maxy = Math.max(maxy, p[1]);
        }
        if (!ok(minx) || !ok(maxx) || !ok(miny) || !ok(maxy)) return null;
        return new int[]{(int) minx, (int) miny, (int) maxx, (int) maxy};
    }

    static double dist(Data.Cam c, double x, double y, double z) {
        double dx = c.x - x, dy = c.y - y, dz = c.z - z; return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    static String time(int ticks) { int s = ticks / 20; return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60); }
}
