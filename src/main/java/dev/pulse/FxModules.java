package dev.pulse;

import dev.pulse.Data.*;
import java.util.*;
import static dev.pulse.Draw.*;

/** Эффекты: JumpCircles, Trails, Particles. */
final class FxModules {

    // ───────────── JumpCircles ─────────────
    static final class JumpCircles extends Module {
        final Setting life = num("Время, мс", 900, 300, 2500, 100), size = num("Размер", 1.8, 0.8, 4, 0.2);
        final List<double[]> list = new ArrayList<>(); boolean was = true;
        JumpCircles() { super("JumpCircles", "Effects", "Круги под ногами при прыжке"); add(life, size); }

        @Override void tick(Frame f) {
            Self m = f.me;
            if (was && !m.onGround && m.vy > 0.1) list.add(new double[]{m.x, m.y + 0.02, m.z, f.now});
            was = m.onGround; if (list.size() > 20) list.remove(0);
        }

        @Override void render(Frame f, Gfx g) {
            for (Iterator<double[]> it = list.iterator(); it.hasNext(); ) {
                double[] c = it.next(); double pr = (f.now - c[3]) / life.num();
                if (pr >= 1) { it.remove(); continue; }
                double r = 0.3 + (1 - Math.pow(1 - pr, 2)) * size.num(), a = Math.pow(1 - pr, 1.3);
                for (int ring = 0; ring < 2; ring++) for (int i = 0; i < 56; i++) {
                    double ang = i * 2 * Math.PI / 56, rr = ring == 0 ? r : r * 0.62;
                    double[] p = f.proj.p(c[0] + Math.cos(ang) * rr, c[1], c[2] + Math.sin(ang) * rr); if (p == null) continue;
                    dot(g, p[0], p[1], ring == 0 ? 3 : 2, alpha(Theme.c(i / 56.0), (int) (240 * a * (ring == 0 ? 1 : 0.55))));
                }
            }
        }
    }

    // ───────────── Trails ─────────────
    static final class Trails extends Module {
        final Setting length = num("Длина", 60, 10, 200, 10), width = num("Толщина", 3, 1, 6, 1);
        final List<double[]> pts = new ArrayList<>(); double lx = Double.NaN, ly, lz;
        Trails() { super("Trails", "Effects", "Светящийся шлейф за игроком"); add(length, width); }

        @Override void tick(Frame f) {
            Self m = f.me; double d = Double.isNaN(lx) ? 1 : Math.abs(m.x - lx) + Math.abs(m.y - ly) + Math.abs(m.z - lz);
            if (d > 0.03) pts.add(new double[]{m.x, m.y + 0.1, m.z, f.now}); else if (!pts.isEmpty()) pts.remove(0);
            lx = m.x; ly = m.y; lz = m.z; while (pts.size() > length.i()) pts.remove(0);
        }

        @Override void render(Frame f, Gfx g) {
            double[] prev = null; int n = pts.size();
            for (int i = 0; i < n; i++) {
                double[] c = pts.get(i), p = f.proj.p(c[0], c[1], c[2]); double k = (i + 1) / (double) n;
                if (p != null) {
                    int col = alpha(Theme.c(i / (double) Math.max(1, n)), (int) (230 * k));
                    if (prev != null) line(g, prev[0], prev[1], p[0], p[1], col, Math.max(1, (int) Math.round(width.num() * k)));
                    dot(g, p[0], p[1], Math.max(2, (int) Math.round(width.num() * k + 1)), alpha(0xFFFFFF, (int) (90 * k)));
                }
                prev = p;
            }
        }
    }

    // ───────────── Particles ─────────────
    static final class Particles extends Module {
        static final class P { double x, y, z, vx, vy, vz, size; int life, max, col; }
        final Setting hit = bool("При ударе", true), kill = bool("При убийстве", true), totem = bool("Тотем", true), amount = num("Количество", 14, 4, 40, 2);
        final List<P> list = new ArrayList<>(); final Random rnd = new Random(); final Map<Integer, double[]> seen = new HashMap<>(); int lastTotems = -1;
        Particles() { super("Particles", "Effects", "Частицы при ударах, убийствах и тотеме"); add(hit, kill, totem, amount); }

        void burst(double x, double y, double z, int n, int col, double sp) {
            for (int i = 0; i < n && list.size() < 700; i++) {
                P p = new P(); p.x = x; p.y = y; p.z = z; double a = rnd.nextDouble() * Math.PI * 2, u = rnd.nextDouble() * 2 - 1, r = Math.sqrt(1 - u * u);
                p.vx = Math.cos(a) * r * sp * (0.4 + rnd.nextDouble()); p.vy = (u * 0.6 + 0.35) * sp; p.vz = Math.sin(a) * r * sp * (0.4 + rnd.nextDouble());
                p.max = p.life = 14 + rnd.nextInt(14); p.col = rnd.nextInt(4) == 0 ? 0xFFFFFFFF : mix(col, 0xFFFFFFFF, rnd.nextDouble() * 0.5); p.size = 0.045 + rnd.nextDouble() * 0.04; list.add(p);
            }
        }

        @Override void onAttack(Ent e) { if (hit.on()) burst(e.x, e.y + e.h * 0.6, e.z, amount.i(), Theme.c(), 0.12); }

        @Override void tick(Frame f) {
            for (Iterator<P> it = list.iterator(); it.hasNext(); ) {
                P p = it.next(); if (--p.life <= 0) { it.remove(); continue; }
                p.x += p.vx; p.y += p.vy; p.z += p.vz; p.vx *= 0.94; p.vz *= 0.94; p.vy = p.vy * 0.94 - 0.012;
            }
            Set<Integer> now = new HashSet<>();
            for (Ent e : f.ents) { if (e.self || e.kind.equals("item")) continue; now.add(e.id); seen.put(e.id, new double[]{e.x, e.y + e.h / 2, e.z, e.hp}); }
            for (Iterator<Map.Entry<Integer, double[]>> it = seen.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Integer, double[]> en = it.next(); if (now.contains(en.getKey())) continue; double[] s = en.getValue();
                if (kill.on() && s[3] <= 3 && dist(f.cam, s[0], s[1], s[2]) < 16) burst(s[0], s[1], s[2], amount.i() * 2, Theme.c(), 0.18);
                it.remove();
            }
            int t = f.me.totems; if (lastTotems > t && totem.on()) burst(f.me.x, f.me.y + 1, f.me.z, 50, 0xFFFFD84D, 0.22); lastTotems = t;
        }

        @Override void render(Frame f, Gfx g) {
            for (P p : list) {
                double[] s = f.proj.p(p.x, p.y, p.z); if (s == null) continue; double k = p.life / (double) p.max;
                int px = Math.max(2, (int) Math.min(6, p.size * f.proj.scale / s[2]));
                dot(g, s[0], s[1], px + 2, alpha(p.col, (int) (50 * k))); dot(g, s[0], s[1], px, mulA(p.col, k));
            }
        }
    }
}
