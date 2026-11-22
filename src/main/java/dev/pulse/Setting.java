package dev.pulse;

final class Setting {
    static final int BOOL = 0, NUM = 1, MODE = 2;
    final String name; final int type;
    boolean b; double v, min, max, step; String[] modes; int idx;

    private Setting(String n, int t) { name = n; type = t; }

    static Setting bool(String n, boolean d) { Setting s = new Setting(n, BOOL); s.b = d; return s; }
    static Setting num(String n, double d, double min, double max, double step) {
        Setting s = new Setting(n, NUM); s.v = d; s.min = min; s.max = max; s.step = step; return s;
    }
    static Setting mode(String n, int i, String... modes) { Setting s = new Setting(n, MODE); s.idx = i; s.modes = modes; return s; }

    boolean on() { return b; }
    double num() { return v; }
    int i() { return (int) Math.round(v); }
    String mode() { return modes[idx]; }

    void setNum(double x) {
        if (step > 0) x = Math.round((x - min) / step) * step + min;
        v = Math.max(min, Math.min(max, x));
    }
    void cycle(int d) { idx = ((idx + d) % modes.length + modes.length) % modes.length; }

    String show() {
        switch (type) {
            case BOOL: return b ? "вкл" : "выкл";
            case NUM: return step >= 1 ? String.valueOf((int) Math.round(v)) : String.format(java.util.Locale.ROOT, "%.1f", v);
            default: return modes[idx];
        }
    }
    String save() { return type == BOOL ? String.valueOf(b) : type == NUM ? String.valueOf(v) : String.valueOf(idx); }
    void load(String s) {
        try {
            if (type == BOOL) b = Boolean.parseBoolean(s); else if (type == NUM) setNum(Double.parseDouble(s));
            else idx = Math.max(0, Math.min(modes.length - 1, Integer.parseInt(s)));
        } catch (Exception ignored) { }
    }
}
