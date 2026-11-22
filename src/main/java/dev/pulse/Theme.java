package dev.pulse;

/** Цветовая тема. Акцент выбирается в модуле Interface. */
final class Theme {
    static final String[] NAMES = {"Violet", "Blue", "Teal", "Pink", "Orange", "Lime", "Red", "White", "Rainbow"};
    static final int[] COLORS = {0xFF8B7CF6, 0xFF4F8CFF, 0xFF2DD4BF, 0xFFF472B6, 0xFFFB923C, 0xFFA3E635, 0xFFFF5566, 0xFFEDEDF2};
    static int idx = 0; static long now = System.currentTimeMillis();

    static void set(int i, long t) { idx = Math.max(0, Math.min(NAMES.length - 1, i)); now = t; }

    /** Цвет акцента; offset сдвигает оттенок в режиме Rainbow. */
    static int c(double offset) {
        if (idx >= COLORS.length) return Draw.hsv(now / 4000.0 + offset * 0.3, 0.6, 1.0);
        return COLORS[idx];
    }
    static int c() { return c(0); }
}
