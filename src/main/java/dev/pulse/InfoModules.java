package dev.pulse;

import dev.pulse.Data.*;
import static dev.pulse.Draw.*;

/** Информация: ESP, AntiInvisible, Arrows, NameTags, PotionTracker, Prediction, TargetESP. */
final class InfoModules {

    static void drawBox(Gfx g, int[] b, int col, int style) {
        if (b[2] - b[0] < 8) { int c = (b[0] + b[2]) / 2; b[0] = c - 4; b[2] = c + 4; }
        if (b[3] - b[1] < 8) { int c = (b[1] + b[3]) / 2; b[1] = c - 4; b[3] = c + 4; }
        if (style == 2) g.fill(b[0], b[1], b[2], b[3], alpha(col, 40));
        if (style == 1) { corners(g, b[0] - 1, b[1] - 1, b[2] + 1, b[3] + 1, 0xCC000000, 8, 3); corners(g, b[0], b[1], b[2], b[3], col, 8, 1); }
        else { outline(g, b[0] - 1, b[1] - 1, b[2] + 1, b[3] + 1, 0xAA000000, 3); outline(g, b[0], b[1], b[2], b[3], col, 1); }
    }

    // ───────────── ESP ─────────────
    static final class Esp extends Module {
        final Setting players = bool("Игроки", true), mobs = bool("Мобы", false), items = bool("Предметы", true),
            blocks = bool("Контейнеры", true), style = mode("Стиль", 1, "Box", "Corners", "Filled"),
            range = num("Дальность", 64, 8, 128, 4), hp = bool("Полоса HP", true);
        Esp() { super("ESP", "Info", "Подсветка игроков, мобов, предметов и контейнеров сквозь стены"); add(players, mobs, items, blocks, style, range, hp); }

        static int markColor(String t) {
            switch (t) { case "ender": return 0xFFB26BFF; case "shulker": return 0xFFFF7BD0; case "barrel": return 0xFFC49A6C; default: return 0xFFFFA94D; }
        }

        @Override void render(Frame f, Gfx g) {
            double r = range.num();
            for (Ent e : f.ents) {
                if (e.self) continue;
                boolean show = e.kind.equals("player") ? players.on() : e.kind.equals("mob") ? mobs.on() : e.kind.equals("item") && items.on();
                if (!show || dist(f.cam, e.x, e.y, e.z) > r) continue;
                int[] b = box(f, e.x - e.w / 2, e.y, e.z - e.w / 2, e.x + e.w / 2, e.y + e.h, e.z + e.w / 2);
                if (b == null) continue;
                int col = e.kind.equals("player") ? (e.invisible ? 0xFFFFFFFF : Theme.c()) : e.kind.equals("item") ? 0xFFFFD166 : e.hostile ? 0xFFFF5566 : 0xFF55FF7F;
                drawBox(g, b, col, style.idx);
                if (hp.on() && !e.kind.equals("item") && e.maxHp > 0) {
                    double fr = Math.max(0, Math.min(1, e.hp / e.maxHp)); int h = b[3] - b[1];
                    g.fill(b[0] - 5, b[1] - 1, b[0] - 2, b[3] + 1, 0xAA000000);
                    g.fill(b[0] - 4, (int) (b[3] - h * fr), b[0] - 3, b[3], mix(0xFFFF4D4D, 0xFF4DFF88, fr));
                }
                if (e.kind.equals("item")) textC(g, e.itemName + (e.count > 1 ? " x" + e.count : ""), (b[0] + b[2]) / 2.0, b[3] + 3, 0xFFFFE9A8);
            }
            if (blocks.on()) {
                int n = 0;
                for (Mark m : f.marks) {
                    if (n > 160 || dist(f.cam, m.x + .5, m.y + .5, m.z + .5) > r) continue;
                    int[] b = box(f, m.x, m.y, m.z, m.x + 1, m.y + 1, m.z + 1); if (b == null) continue;
                    drawBox(g, b, markColor(m.type), style.idx == 1 ? 1 : 0); n++;
                }
            }
        }
    }

    // ───────────── AntiInvisible ─────────────
    static final class AntiInvisible extends Module {
        final Setting range = num("Дальность", 64, 8, 128, 4);
        AntiInvisible() { super("AntiInvisible", "Info", "Показывает игроков под невидимостью"); add(range); }
        @Override void render(Frame f, Gfx g) {
            for (Ent e : f.ents) {
                if (e.self || !e.kind.equals("player") || !e.invisible || dist(f.cam, e.x, e.y, e.z) > range.num()) continue;
                int[] b = box(f, e.x - e.w / 2, e.y, e.z - e.w / 2, e.x + e.w / 2, e.y + e.h, e.z + e.w / 2); if (b == null) continue;
                double pulse = 0.55 + 0.45 * Math.sin(f.now / 220.0);
                g.fill(b[0], b[1], b[2], b[3], alpha(0xFFFFFF, (int) (30 + 40 * pulse)));
                outline(g, b[0], b[1], b[2], b[3], alpha(0xFFFFFF, (int) (150 + 100 * pulse)), 1);
                textC(g, "INVISIBLE · " + e.name, (b[0] + b[2]) / 2.0, b[1] - 11, 0xFFFFFFFF);
            }
        }
    }

    // ───────────── Arrows ─────────────
    static final class Arrows extends Module {
        final Setting radius = num("Радиус", 62, 30, 140, 2), range = num("Дальность", 80, 10, 200, 5), naked = bool("Игнорировать голых", true),
            dist = bool("Дистанция", true);
        Arrows() { super("Arrows", "Info", "Стрелки вокруг прицела на ближайших игроков"); add(radius, range, naked, dist); }
        @Override void render(Frame f, Gfx g) {
            double yaw = Math.toRadians(f.cam.yaw), cx = f.sw / 2.0, cy = f.sh / 2.0, R = radius.num();
            for (Ent e : f.ents) {
                if (e.self || !e.kind.equals("player")) continue;
                double d = dist(f.cam, e.x, e.y, e.z); if (d > range.num() || (naked.on() && e.naked())) continue;
                double dx = e.x - f.cam.x, dz = e.z - f.cam.z;
                double ang = Math.atan2(dx * -Math.cos(yaw) + dz * -Math.sin(yaw), dx * -Math.sin(yaw) + dz * Math.cos(yaw));
                double ax = Math.sin(ang), ay = -Math.cos(ang), bx = cx + ax * R, by = cy + ay * R, px = -ay, py = ax;
                int col = mix(0xFFFF4D4D, Theme.c(), Math.min(1, d / 40));
                double tx = bx + ax * 11, ty = by + ay * 11;
                for (int i = -5; i <= 5; i++) line(g, tx, ty, bx + px * i, by + py * i, alpha(col, 200), 1);
                line(g, tx, ty, bx + px * 6, by + py * 6, alpha(0x000000, 160), 1); line(g, tx, ty, bx - px * 6, by - py * 6, alpha(0x000000, 160), 1);
                if (dist.on()) textC(g, (int) d + "m", bx - ax * 10, (int) (by - ay * 10) - 4, 0xFFFFFFFF);
            }
        }
    }

    // ───────────── NameTags ─────────────
    static final class NameTags extends Module {
        final Setting range = num("Дальность", 96, 8, 200, 4), armor = bool("Броня", true), ping = bool("Пинг", true);
        NameTags() { super("NameTags", "Info", "Теги над игроками: ник, HP, броня, пинг"); add(range, armor, ping); }
        @Override void render(Frame f, Gfx g) {
            for (Ent e : f.ents) {
                if (e.self || !e.kind.equals("player") || dist(f.cam, e.x, e.y, e.z) > range.num()) continue;
                double[] p = f.proj.p(e.x, e.y + e.h + 0.6, e.z); if (p == null) continue;
                double fr = Math.max(0, Math.min(1, e.hp / Math.max(1, e.maxHp)));
                String hp = String.format(java.util.Locale.ROOT, "%.1f", e.hp), pg = ping.on() && e.ping >= 0 ? "  " + e.ping + "ms" : "";
                int w = g.textWidth(e.name + " " + hp + pg) + 10, x = (int) p[0] - w / 2, y = (int) p[1] - 12;
                pill(g, x, y, x + w, y + 13, 0xB0101015); g.fill(x + 1, y + 12, x + 1 + (int) ((w - 2) * fr), y + 13, mix(0xFFFF4D4D, 0xFF4DFF88, fr));
                int tx = x + 5; g.text(e.name, tx, y + 3, 0xFFFFFFFF); tx += g.textWidth(e.name + " ");
                g.text(hp, tx, y + 3, mix(0xFFFF4D4D, 0xFF4DFF88, fr)); tx += g.textWidth(hp);
                if (!pg.isEmpty()) g.text(pg, tx, y + 3, 0xFF9A9AA6);
                if (armor.on()) { int n = 0; for (Object o : e.armor) if (o != null) n++; int ax = (int) p[0] - n * 9;
                    for (Object o : e.armor) if (o != null) { g.item(o, ax, y - 18); ax += 18; } }
            }
        }
    }

    // ───────────── PotionTracker ─────────────
    static final class PotionTracker extends Module {
        final Setting range = num("Дальность", 48, 8, 128, 4);
        PotionTracker() { super("PotionTracker", "Info", "Активные зелья противников"); add(range); }
        @Override void render(Frame f, Gfx g) {
            for (Ent e : f.ents) {
                if (e.self || !e.kind.equals("player") || e.eff.isEmpty() || dist(f.cam, e.x, e.y, e.z) > range.num()) continue;
                int[] b = box(f, e.x - e.w / 2, e.y, e.z - e.w / 2, e.x + e.w / 2, e.y + e.h, e.z + e.w / 2); if (b == null) continue;
                int y = b[1], n = 0;
                for (Eff x : e.eff) { if (n++ >= 5) break;
                    String s = x.name + (x.amp > 0 ? " " + (x.amp + 1) : "") + "  " + time(x.ticks);
                    int w = g.textWidth(s) + 8; pill(g, b[2] + 5, y, b[2] + 5 + w, y + 12, 0xA0101015);
                    g.fill(b[2] + 5, y + 1, b[2] + 7, y + 11, Theme.c(n * 0.2)); g.text(s, b[2] + 10, y + 2, x.ticks < 200 ? 0xFFFFA0A0 : 0xFFFFFFFF); y += 14; }
            }
        }
    }

    // ───────────── Prediction ─────────────
    static final class Prediction extends Module {
        final Setting steps = num("Длина", 120, 30, 240, 10), marker = bool("Точка падения", true);
        Prediction() { super("Prediction", "Info", "Траектория жемчуга, зелий, стрел и трезубца"); add(steps, marker); }

        @Override void render(Frame f, Gfx g) {
            Self m = f.me; double speed = 1.5, grav = 0.03, off = 0; String h = m.held;
            if (h.isEmpty()) return;
            if (h.equals("pearl") || h.equals("snowball") || h.equals("egg")) { speed = 1.5; grav = 0.03; }
            else if (h.equals("splash") || h.equals("lingering")) { speed = 0.5; grav = 0.05; off = -20; }
            else if (h.equals("xp")) { speed = 0.7; grav = 0.07; off = -20; }
            else if (h.equals("trident")) { speed = 2.5; grav = 0.05; }
            else if (h.equals("crossbow")) { speed = 3.15; grav = 0.05; }
            else if (h.equals("bow")) { if (m.useTicks <= 0) return; double p = m.useTicks / 20.0; p = (p * p + p * 2) / 3; if (p < 0.1) return; speed = Math.min(1, p) * 3; grav = 0.05; }
            else return;
            double yaw = Math.toRadians(m.yaw), pit = Math.toRadians(m.pitch + off);
            double x = m.x - Math.cos(yaw) * 0.16, y = m.eyeY - 0.1, z = m.z - Math.sin(yaw) * 0.16;
            double vx = -Math.sin(yaw) * Math.cos(pit) * speed, vy = -Math.sin(pit) * speed, vz = Math.cos(yaw) * Math.cos(pit) * speed;
            double[] prev = null; double[] end = null;
            for (int i = 0; i < steps.i(); i++) {
                double nx = x + vx, ny = y + vy, nz = z + vz; double[] hit = f.ray == null ? null : f.ray.cast(x, y, z, nx, ny, nz);
                if (hit != null) { nx = hit[0]; ny = hit[1]; nz = hit[2]; }
                double[] a = f.proj.p(x, y, z), b = f.proj.p(nx, ny, nz); double k = 1 - i / (double) steps.i() * 0.5;
                if (a != null && b != null) line(g, a[0], a[1], b[0], b[1], alpha(Theme.c(i / 40.0), (int) (230 * k)), 2);
                if (b != null && i % 4 == 0) dot(g, b[0], b[1], 4, alpha(0xFFFFFF, (int) (120 * k)));
                end = new double[]{nx, ny, nz}; if (hit != null) break;
                x = nx; y = ny; z = nz; vx *= 0.99; vy = vy * 0.99 - grav; vz *= 0.99;
            }
            if (marker.on() && end != null) {
                double[] p = f.proj.p(end[0], end[1], end[2]);
                if (p != null) {
                    int c = Theme.c();
                    for (int i = 0; i < 24; i++) { double a = i * Math.PI / 12; dot(g, p[0] + Math.cos(a) * 7, p[1] + Math.sin(a) * 7, 2, alpha(c, 230)); }
                    dot(g, p[0], p[1], 3, 0xFFFFFFFF);
                    textC(g, String.format(java.util.Locale.ROOT, "%.1fm", dist(f.cam, end[0], end[1], end[2])), p[0], (int) p[1] + 11, 0xFFFFFFFF);
                }
            }
        }
    }

    // ───────────── TargetESP ─────────────
    static final class TargetEsp extends Module {
        final Setting style = mode("Стиль", 0, "Ring", "Ghosts", "Square"), info = bool("Инфо о цели", true);
        int lastId = -1; long lastSeen;
        TargetEsp() { super("TargetESP", "Info", "Маркер на цели (кольцо, призраки, квадрат)"); add(style, info); }

        @Override void render(Frame f, Gfx g) {
            if (f.target != null) { lastId = f.target.id; lastSeen = f.now; }
            if (lastId < 0 || f.now - lastSeen > 1500) return;
            Ent e = null; for (Ent x : f.ents) if (x.id == lastId) e = x;
            if (e == null) return;
            double t = f.now / 1000.0, fade = f.target != null ? 1 : Math.max(0, 1 - (f.now - lastSeen) / 1500.0);
            if (style.idx == 0) {
                double r = e.w * 0.8 + 0.15;
                for (int k = 0; k < 3; k++) {
                    double yy = e.y + (0.5 + 0.5 * Math.sin(t * 2.2 - k * 0.55)) * e.h;
                    for (int i = 0; i < 40; i++) { double a = i * 2 * Math.PI / 40 + t; double[] p = f.proj.p(e.x + Math.cos(a) * r, yy, e.z + Math.sin(a) * r);
                        if (p != null) dot(g, p[0], p[1], k == 0 ? 3 : 2, mulA(alpha(Theme.c(i / 40.0), 235 - k * 75), fade)); }
                }
            } else if (style.idx == 1) {
                for (int i = 0; i < 5; i++) for (int tr = 0; tr < 6; tr++) {
                    double a = t * 2.4 + i * 2 * Math.PI / 5 - tr * 0.13, yy = e.y + e.h * (0.5 + 0.42 * Math.sin(t * 1.7 + i * 1.3 - tr * 0.1));
                    double[] p = f.proj.p(e.x + Math.cos(a) * (e.w + 0.2), yy, e.z + Math.sin(a) * (e.w + 0.2)); if (p == null) continue;
                    int c = Theme.c(i / 5.0); double k = 1 - tr / 6.0;
                    if (tr == 0) dot(g, p[0], p[1], 9, mulA(alpha(c, 70), fade));
                    dot(g, p[0], p[1], tr == 0 ? 4 : 3, mulA(alpha(c, (int) (250 * k)), fade));
                }
            } else {
                int[] b = box(f, e.x - e.w / 2, e.y, e.z - e.w / 2, e.x + e.w / 2, e.y + e.h, e.z + e.w / 2); if (b == null) return;
                double cx = (b[0] + b[2]) / 2.0, cy = (b[1] + b[3]) / 2.0, hs = Math.max(b[2] - b[0], b[3] - b[1]) * 0.55 + 4, rot = Math.sin(t * 2) * 0.35;
                for (int i = 0; i < 4; i++) {
                    double a1 = rot + Math.PI / 4 + i * Math.PI / 2, a2 = a1 + Math.PI / 2, s = hs * 1.4142;
                    double x1 = cx + Math.cos(a1) * s, y1 = cy + Math.sin(a1) * s, x2 = cx + Math.cos(a2) * s, y2 = cy + Math.sin(a2) * s;
                    line(g, x1, y1, x1 + (x2 - x1) * 0.3, y1 + (y2 - y1) * 0.3, mulA(Theme.c(i * 0.25), fade), 2);
                    line(g, x2, y2, x2 - (x2 - x1) * 0.3, y2 - (y2 - y1) * 0.3, mulA(Theme.c(i * 0.25), fade), 2);
                }
            }
            if (info.on() && e.maxHp > 0) {
                int w = 120, x = f.sw / 2 - w / 2, y = f.sh / 2 + 46; double fr = Math.max(0, Math.min(1, e.hp / e.maxHp));
                pill(g, x, y, x + w, y + 24, mulA(0xB0101015, fade)); g.text(e.name, x + 6, y + 4, mulA(0xFFFFFFFF, fade));
                String hs = String.format(java.util.Locale.ROOT, "%.1f", e.hp); g.text(hs, x + w - 6 - g.textWidth(hs), y + 4, mulA(mix(0xFFFF4D4D, 0xFF4DFF88, fr), fade));
                g.fill(x + 6, y + 16, x + w - 6, y + 19, mulA(0x40FFFFFF, fade)); g.fill(x + 6, y + 16, x + 6 + (int) ((w - 12) * fr), y + 19, mulA(Theme.c(), fade));
            }
        }
    }
}
