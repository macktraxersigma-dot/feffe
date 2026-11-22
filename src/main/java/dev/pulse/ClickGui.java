package dev.pulse;

import static dev.pulse.Draw.*;

/** Меню на правый Shift. Рисуется через Gfx, мышь передаётся снаружи (опрос GLFW), поэтому не зависит от версии игры. */
final class ClickGui {
    static final String[] CATS = {"Info", "Effects", "HUD"}, TITLES = {"ИНФОРМАЦИЯ", "ЭФФЕКТЫ", "ИНТЕРФЕЙС"};
    static final int PW = 138, GAP = 10, HEAD = 22, ROW = 18, SROW = 16;
    Module open; Setting drag; boolean pl, pr;

    int height(String cat) {
        int h = HEAD + 4;
        for (Module m : Pulse.mods) if (m.cat.equals(cat)) { h += ROW; if (m == open) h += m.settings.size() * SROW + 3; }
        return h + 3;
    }

    void draw(Gfx g, int sw, int sh, int mx, int my, boolean L, boolean R) {
        boolean pressL = L && !pl, pressR = R && !pr; pl = L; pr = R; if (!L) drag = null;
        g.fill(0, 0, sw, sh, 0x8A05050A);
        int total = 3 * PW + 2 * GAP, x0 = (sw - total) / 2, y0 = Math.max(26, sh / 2 - 130);
        textC(g, "Pulse Visual", sw / 2.0, y0 - 20, Theme.c()); textC(g, open != null ? open.name + " — " + open.desc : "ЛКМ — вкл/выкл · ПКМ — настройки · Right Shift — закрыть", sw / 2.0, y0 - 10, open != null ? 0xFF9A9AA8 : 0xFF6E6E7A);
        for (int c = 0; c < 3; c++) {
            int x = x0 + c * (PW + GAP), y = y0, h = height(CATS[c]);
            pill(g, x, y, x + PW, y + h, 0xE0101015); outline(g, x, y, x + PW, y + h, 0x22FFFFFF, 1); g.fill(x + 1, y, x + PW - 1, y + 2, Theme.c(c * 0.3));
            g.text(TITLES[c], x + 8, y + 8, 0xFFB8B8C4); y += HEAD + 2;
            for (Module m : Pulse.mods) {
                if (!m.cat.equals(CATS[c])) continue;
                boolean hov = mx >= x + 3 && mx < x + PW - 3 && my >= y && my < y + ROW;
                if (hov) { g.fill(x + 3, y, x + PW - 3, y + ROW, 0x18FFFFFF); if (pressL) m.on = !m.on; if (pressR && !m.settings.isEmpty()) open = open == m ? null : m; }
                if (m.on) g.fill(x + 3, y + 2, x + 5, y + ROW - 2, Theme.c());
                g.text(m.name, x + 10, y + 5, m.on ? 0xFFFFFFFF : 0xFF8A8A96);
                if (!m.settings.isEmpty()) g.text(m == open ? "-" : "+", x + PW - 13, y + 5, 0xFF5E5E6A);
                y += ROW;
                if (m == open) {
                    g.fill(x + 6, y, x + PW - 6, y + m.settings.size() * SROW + 3, 0x40000000); y += 2;
                    for (Setting s : m.settings) { settingRow(g, s, x + 9, y, PW - 18, mx, my, pressL, pressR, L); y += SROW; }
                    y += 1;
                }
            }
        }
    }

    void settingRow(Gfx g, Setting s, int x, int y, int w, int mx, int my, boolean pressL, boolean pressR, boolean L) {
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + SROW;
        g.text(s.name, x, y + 4, 0xFFA8A8B4);
        if (s.type == Setting.NUM) {
            int bx = x + w * 5 / 12, bw = w - w * 5 / 12 - 30; double fr = (s.v - s.min) / (s.max - s.min);
            g.fill(bx, y + 7, bx + bw, y + 9, 0x30FFFFFF); g.fill(bx, y + 7, bx + (int) (bw * fr), y + 9, Theme.c()); g.fill(bx + (int) (bw * fr) - 1, y + 5, bx + (int) (bw * fr) + 2, y + 11, 0xFFFFFFFF);
            g.text(s.show(), x + w - g.textWidth(s.show()), y + 4, 0xFFFFFFFF);
            if (pressL && mx >= bx - 3 && mx <= bx + bw + 3 && my >= y && my < y + SROW) drag = s;
            if (drag == s && L) s.setNum(s.min + (mx - bx) / (double) bw * (s.max - s.min));
        } else {
            String v = s.show(); int tw = g.textWidth(v);
            if (s.type == Setting.BOOL) { g.fill(x + w - 22, y + 3, x + w, y + 13, s.b ? Theme.c() : 0xFF2A2A32); g.fill(x + w - (s.b ? 10 : 20), y + 4, x + w - (s.b ? 2 : 12), y + 12, 0xFFFFFFFF); }
            else g.text(v, x + w - tw, y + 4, Theme.c());
            if (hov && pressL) { if (s.type == Setting.BOOL) s.b = !s.b; else s.cycle(1); }
            if (hov && pressR && s.type == Setting.MODE) s.cycle(-1);
        }
    }
}
