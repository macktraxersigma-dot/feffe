package dev.pulse;

/** Всё, что нужно модулям для рисования. Реализация для игры — в McPlatform. */
interface Gfx {
    void fill(int x1, int y1, int x2, int y2, int argb);
    void text(String s, int x, int y, int argb);
    int textWidth(String s);
    void item(Object stack, int x, int y);
}
