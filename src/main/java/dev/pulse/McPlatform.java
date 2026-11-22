
package dev.pulse;

import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ЕДИНСТВЕННЫЙ файл (вместе с PulseClient и PulseScreen),
 * который обращается к классам Minecraft.
 */
final class McPlatform {
    private static Map<Integer, double[]> prev = new HashMap<>(), cur = new HashMap<>();
    private static long tickNs = System.nanoTime();
    private static List<Data.Mark> marks = new ArrayList<>();
    private static int scanTimer;

    // ───── рисование ─────
    static final class McGfx implements Gfx {
        private final DrawContext c;
        private final TextRenderer t;

        McGfx(DrawContext c, TextRenderer t) {
            this.c = c;
            this.t = t;
        }

        @Override
        public void fill(int x1, int y1, int x2, int y2, int argb) {
            c.fill(x1, y1, x2, y2, argb);
        }

        @Override
        public void text(String s, int x, int y, int argb) {
            c.drawText(t, s, x, y, argb, true);
        }

        @Override
        public int textWidth(String s) {
            return t.getWidth(s);
        }

        @Override
        public void item(Object stack, int x, int y) {
            c.drawItem((ItemStack) stack, x, y);
        }
    }

    // ───── тик: позиции для интерполяции и скан контейнеров ─────
    static void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld w = mc.world;

        if (w == null || mc.player == null) {
            prev.clear();
            cur.clear();
            marks = new ArrayList<>();
            return;
        }

        prev = cur;
        cur = new HashMap<>();
        tickNs = System.nanoTime();

        for (Entity e : w.getEntities()) {
            cur.put(e.getId(), new double[]{
                    e.getX(), e.getY(), e.getZ()
            });
        }

        if (--scanTimer <= 0) {
            scanTimer = 20;
            scan(mc, w);
        }
    }

    private static void scan(MinecraftClient mc, ClientWorld w) {
        int r = Math.min(32, (int) Pulse.esp.range.num());
        List<Data.Mark> out = new ArrayList<>();

        if (!Pulse.esp.on || !Pulse.esp.blocks.on()) {
            marks = out;
            return;
        }

        BlockPos o = mc.player.getBlockPos();
        BlockPos.Mutable m = new BlockPos.Mutable();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    m.set(
                            o.getX() + x,
                            o.getY() + y,
                            o.getZ() + z
                    );

                    Block b = w.getBlockState(m).getBlock();

                    if (b == Blocks.AIR) continue;

                    String t =
                            b instanceof EnderChestBlock ? "ender" :
                            b instanceof ChestBlock ? "chest" :
                            b instanceof ShulkerBoxBlock ? "shulker" :
                            b instanceof BarrelBlock ? "barrel" :
                            null;

                    if (t == null) continue;

                    Data.Mark k = new Data.Mark();
                    k.x = m.getX();
                    k.y = m.getY();
                    k.z = m.getZ();
                    k.type = t;
                    out.add(k);
                }
            }
        }

        marks = out;
    }

    // ───── снимок кадра ─────
    static Data.Frame frame() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity pl = mc.player;
        ClientWorld w = mc.world;

        if (pl == null || w == null) return null;

        Data.Frame f = new Data.Frame();
        f.sw = mc.getWindow().getScaledWidth();
        f.sh = mc.getWindow().getScaledHeight();
        f.now = System.currentTimeMillis();

        double t = Math.max(
                0,
                Math.min(1, (System.nanoTime() - tickNs) / 50_000_000.0)
        );

        // Исправлено для Minecraft 1.21.11
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d cp = cam.getCameraPos();

        f.cam.x = cp.x;
        f.cam.y = cp.y;
        f.cam.z = cp.z;
        f.cam.yaw = cam.getYaw();
        f.cam.pitch = cam.getPitch();
        f.cam.fov = mc.options.getFov().getValue();

        Data.Self m = f.me;

        m.name = pl.getName().getString();
        m.x = pl.getX();
        m.y = pl.getY();
        m.z = pl.getZ();
        m.eyeY = pl.getEyeY();

        m.yaw = pl.getYaw();
        m.pitch = pl.getPitch();
        m.hp = pl.getHealth();
        m.maxHp = pl.getMaxHealth();
        m.absorb = pl.getAbsorptionAmount();

        m.onGround = pl.isOnGround();
        m.vy = pl.getVelocity().y;
        m.useTicks = pl.getItemUseTime();
        m.cooldown = pl.getAttackCooldownProgress(0.5f);

        m.fps = mc.getCurrentFps();

        PlayerListEntry me = mc.getNetworkHandler() == null
                ? null
                : mc.getNetworkHandler().getPlayerListEntry(pl.getUuid());

        m.ping = me == null ? 0 : me.getLatency();

        m.held = held(pl.getMainHandStack());

        if (m.held.isEmpty()) {
            m.held = held(pl.getOffHandStack());
        }

        armor(pl, m.armor, m.armorFrac);
        effects(pl, m.eff);

        int totems = 0;

        for (int i = 0; i < pl.getInventory().size(); i++) {
            ItemStack s = pl.getInventory().getStack(i);

            if (s.isOf(Items.TOTEM_OF_UNDYING)) {
                totems += s.getCount();
            }
        }

        m.totems = totems;

        for (Entity e : w.getEntities()) {
            if (e == pl || e.isSpectator() || e instanceof ArmorStandEntity) {
                continue;
            }

            Data.Ent d = ent(e, t);

            if (d != null) {
                f.ents.add(d);

                if (mc.targetedEntity == e) {
                    f.target = d;
                }
            }
        }

        f.marks = marks;

        f.ray = (x1, y1, z1, x2, y2, z2) -> {
            HitResult h = w.raycast(
                    new RaycastContext(
                            new Vec3d(x1, y1, z1),
                            new Vec3d(x2, y2, z2),
                            RaycastContext.ShapeType.COLLIDER,
                            RaycastContext.FluidHandling.NONE,
                            pl
                    )
            );

            if (h.getType() == HitResult.Type.MISS) {
                return null;
            }

            Vec3d p = h.getPos();

            return new double[]{p.x, p.y, p.z};
        };

        return f;
    }

    /** Снимок одной сущности (для события удара и для кадра). */
    static Data.Ent ent(Entity e, double t) {
        Data.Ent d = new Data.Ent();

        if (e instanceof PlayerEntity) {
            d.kind = "player";
        } else if (e instanceof ItemEntity) {
            d.kind = "item";
        } else if (e instanceof LivingEntity) {
            d.kind = "mob";
        } else {
            return null;
        }

        d.id = e.getId();
        d.name = e.getName().getString();
        d.invisible = e.isInvisible();
        d.w = e.getWidth();
        d.h = e.getHeight();

        double[] a = prev.get(d.id);
        double[] b = cur.get(d.id);

        if (a != null && b != null) {
            d.x = a[0] + (b[0] - a[0]) * t;
            d.y = a[1] + (b[1] - a[1]) * t;
            d.z = a[2] + (b[2] - a[2]) * t;
        } else {
            d.x = e.getX();
            d.y = e.getY();
            d.z = e.getZ();
        }

        if (e instanceof LivingEntity le) {
            d.hp = le.getHealth();
            d.maxHp = le.getMaxHealth();
            d.hostile = e instanceof HostileEntity;

            armor(le, d.armor, d.armorFrac);
            effects(le, d.eff);

            if (e instanceof PlayerEntity) {
                MinecraftClient mc = MinecraftClient.getInstance();

                PlayerListEntry p = mc.getNetworkHandler() == null
                        ? null
                        : mc.getNetworkHandler().getPlayerListEntry(e.getUuid());

                d.ping = p == null ? -1 : p.getLatency();
            }
        }

        if (e instanceof ItemEntity ie) {
            d.itemName = ie.getStack().getName().getString();
            d.count = ie.getStack().getCount();
            d.maxHp = 0;
        }

        return d;
    }

    private static void armor(
            LivingEntity e,
            Object[] out,
            double[] frac
    ) {
        EquipmentSlot[] s = {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        };

        for (int i = 0; i < 4; i++) {
            ItemStack st = e.getEquippedStack(s[i]);

            out[i] = st.isEmpty() ? null : st;

            frac[i] = st.isEmpty()
                    ? -1
                    : st.isDamageable()
                        ? 1.0 - st.getDamage() / (double) st.getMaxDamage()
                        : 1.0;
        }
    }

    private static void effects(
            LivingEntity e,
            List<Data.Eff> out
    ) {
        for (StatusEffectInstance i : e.getStatusEffects()) {
            out.add(
                    new Data.Eff(
                            i.getEffectType().value().getName().getString(),
                            i.getDuration(),
                            i.getAmplifier()
                    )
            );
        }
    }

    private static String held(ItemStack s) {
        if (s.isEmpty()) return "";

        if (s.isOf(Items.ENDER_PEARL)) return "pearl";
        if (s.isOf(Items.SNOWBALL)) return "snowball";
        if (s.isOf(Items.EGG)) return "egg";
        if (s.isOf(Items.SPLASH_POTION)) return "splash";
        if (s.isOf(Items.LINGERING_POTION)) return "lingering";
        if (s.isOf(Items.EXPERIENCE_BOTTLE)) return "xp";
        if (s.isOf(Items.TRIDENT)) return "trident";
        if (s.isOf(Items.BOW)) return "bow";
        if (s.isOf(Items.CROSSBOW)) return "crossbow";

        return "";
    }
}
