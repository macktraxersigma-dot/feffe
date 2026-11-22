package dev.pulse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

abstract class Module {
    final String name, cat, desc; boolean on; final List<Setting> settings = new ArrayList<>();

    Module(String name, String cat, String desc) { this.name = name; this.cat = cat; this.desc = desc; }

    void add(Setting... s) { settings.addAll(Arrays.asList(s)); }
    void tick(Data.Frame f) { }
    void render(Data.Frame f, Gfx g) { }
    void onAttack(Data.Ent e) { }

    static Setting bool(String n, boolean d) { return Setting.bool(n, d); }
    static Setting num(String n, double d, double min, double max, double step) { return Setting.num(n, d, min, max, step); }
    static Setting mode(String n, int i, String... m) { return Setting.mode(n, i, m); }
}
