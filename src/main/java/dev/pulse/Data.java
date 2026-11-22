package dev.pulse;

import java.util.ArrayList;
import java.util.List;

/** Снимок состояния игры на кадр. Заполняется в McPlatform, модули читают только его. */
final class Data {
    static final class Cam { double x, y, z, yaw, pitch, fov = 70; }

    static final class Eff {
        final String name; final int ticks, amp;
        Eff(String n, int t, int a) { name = n; ticks = t; amp = a; }
    }

    static final class Ent {
        int id; String name = ""; String kind = "other"; // player | mob | item
        boolean self, invisible, hostile;
        double x, y, z, w = 0.6, h = 1.8;
        float hp = 20, maxHp = 20; int ping = -1;
        final Object[] armor = new Object[4];          // head, chest, legs, feet
        final double[] armorFrac = {-1, -1, -1, -1};   // -1 нет брони, 0..1 прочность
        final List<Eff> eff = new ArrayList<>();
        String itemName = ""; int count = 1;
        boolean naked() { for (double d : armorFrac) if (d >= 0) return false; return true; }
    }

    static final class Mark { int x, y, z; String type; }

    static final class Self {
        String name = ""; double x, y, z, eyeY; float yaw, pitch;
        float hp = 20, maxHp = 20, absorb; boolean onGround = true; double vy;
        String held = ""; int useTicks; float cooldown = 1; int totems; int fps, ping;
        final Object[] armor = new Object[4];
        final double[] armorFrac = {-1, -1, -1, -1};
        final List<Eff> eff = new ArrayList<>();
    }

    interface Ray { double[] cast(double x1, double y1, double z1, double x2, double y2, double z2); }

    static final class Frame {
        final Cam cam = new Cam(); final Self me = new Self();
        final List<Ent> ents = new ArrayList<>(); List<Mark> marks = new ArrayList<>();
        Ent target; Ray ray; Proj proj; int sw, sh; long now;
    }
}
