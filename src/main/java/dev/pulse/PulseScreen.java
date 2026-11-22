package dev.pulse;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Экран меню. Мышь опрашиваем через GLFW, чтобы не зависеть от сигнатур обработчиков кликов. */
final class PulseScreen extends Screen {
    private final ClickGui gui = new ClickGui();

    PulseScreen() { super(Text.literal("Pulse Visual")); }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MinecraftClient c = MinecraftClient.getInstance(); long h = c.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        gui.draw(new McPlatform.McGfx(ctx, c.textRenderer), width, height, mouseX, mouseY, l, r);
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void removed() { Pulse.save(); }
}
