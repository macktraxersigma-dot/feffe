package dev.pulse;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Реестр модулей, конфиг и главный цикл (не зависит от Minecraft). */
final class Pulse {
    static final List<Module> mods = new ArrayList<>();
    static HudModules.Interface ui; static InfoModules.Esp esp; static Path file;

    static void init(Path cfg) {
        file = cfg; mods.clear();
        ui = new HudModules.Interface(); esp = new InfoModules.Esp();
        Module[] all = {
            new HudModules.Ambience(), esp, new InfoModules.AntiInvisible(), new InfoModules.Prediction(), new InfoModules.TargetEsp(),
            new FxModules.JumpCircles(), new FxModules.Trails(), new FxModules.Particles(), new InfoModules.NameTags(), new InfoModules.PotionTracker(),
            new InfoModules.Arrows(), new HudModules.Crosshair(), new HudModules.ArmorHud(), new HudModules.HotAlerts(), ui};
        mods.addAll(Arrays.asList(all));
        for (Module m : mods) if (Arrays.asList("ESP", "NameTags", "Prediction", "TargetESP", "JumpCircles", "Particles").contains(m.name)) m.on = true;
        load();
    }

    static void tick(Data.Frame f) { Theme.set(ui.accent.idx, f.now); for (Module m : mods) if (m.on) m.tick(f); }

    static void hud(Data.Frame f, Gfx g) {
        Theme.set(ui.accent.idx, f.now); f.proj = new Proj(f.cam, f.sw, f.sh);
        for (Module m : mods) if (m.on) { try { m.render(f, g); } catch (RuntimeException ex) { m.on = false; System.err.println("[Pulse] " + m.name + " выключен: " + ex); } }
    }

    static void attack(Data.Ent e) { for (Module m : mods) if (m.on) m.onAttack(e); }

    static void save() {
        if (file == null) return;
        Properties p = new Properties();
        for (Module m : mods) { p.setProperty(m.name + ".on", String.valueOf(m.on)); for (Setting s : m.settings) p.setProperty(m.name + "." + s.name, s.save()); }
        try (OutputStream o = Files.newOutputStream(file)) { p.store(o, "Pulse Visual"); } catch (IOException ignored) { }
    }

    static void load() {
        if (file == null || !Files.exists(file)) return;
        Properties p = new Properties();
        try (InputStream i = Files.newInputStream(file)) { p.load(i); } catch (IOException e) { return; }
        for (Module m : mods) {
            String on = p.getProperty(m.name + ".on"); if (on != null) m.on = Boolean.parseBoolean(on);
            for (Setting s : m.settings) { String v = p.getProperty(m.name + "." + s.name); if (v != null) s.load(v); }
        }
    }
}
