package dev.pulse;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

public final class PulseClient implements ClientModInitializer {
    private static boolean prevKey;

    @Override
    public void onInitializeClient() {
        Pulse.init(FabricLoader.getInstance().getConfigDir().resolve("pulse-visual.properties"));

        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            McPlatform.onTick();
            Data.Frame f = McPlatform.frame(); if (f != null) Pulse.tick(f);
            menuKey(c);
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient c = MinecraftClient.getInstance();
            if (c.options.hudHidden) return;
            Data.Frame f = McPlatform.frame(); if (f == null) return;
            Pulse.hud(f, new McPlatform.McGfx(ctx, c.textRenderer));
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            Data.Ent e = McPlatform.ent(entity, 1.0); if (e != null) Pulse.attack(e);
            return ActionResult.PASS;
        });
    }

    /** Right Shift открывает и закрывает меню. */
    private static void menuKey(MinecraftClient c) {
        boolean down = GLFW.glfwGetKey(c.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (down && !prevKey) {
            if (c.currentScreen == null) c.setScreen(new PulseScreen());
            else if (c.currentScreen instanceof PulseScreen) c.setScreen(null);
        }
        prevKey = down;
    }
}
