package dev.pulse;

/** Проекция точки мира на экран (GUI-координаты). Работает в любом виде камеры и сквозь стены. */
final class Proj {
    final double cx, cy, cz, fx, fy, fz, rx, ry, rz, ux, uy, uz, scale;
    final int sw, sh;

    Proj(Data.Cam c, int sw, int sh) {
        this.sw = sw; this.sh = sh; cx = c.x; cy = c.y; cz = c.z;
        double yaw = Math.toRadians(c.yaw), pit = Math.toRadians(c.pitch);
        fx = -Math.sin(yaw) * Math.cos(pit); fy = -Math.sin(pit); fz = Math.cos(yaw) * Math.cos(pit);
        rx = -Math.cos(yaw); ry = 0; rz = -Math.sin(yaw);
        ux = ry * fz - rz * fy; uy = rz * fx - rx * fz; uz = rx * fy - ry * fx;
        scale = (sh / 2.0) / Math.tan(Math.toRadians(Math.max(10, Math.min(170, c.fov))) / 2);
    }

    /** {sx, sy, depth} или null, если точка за камерой. */
    double[] p(double x, double y, double z) {
        double dx = x - cx, dy = y - cy, dz = z - cz;
        double d = dx * fx + dy * fy + dz * fz;
        if (d < 0.05) return null;
        return new double[]{
            sw / 2.0 + (dx * rx + dy * ry + dz * rz) / d * scale,
            sh / 2.0 - (dx * ux + dy * uy + dz * uz) / d * scale, d};
    }
}
