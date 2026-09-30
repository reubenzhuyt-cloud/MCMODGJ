package com.example.weather_realm.client.gui;

import com.example.weather_realm.network.SetWeatherPayload;
import com.example.weather_realm.network.WeatherMode;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 天象调控仪 / Weather Harmonizer.
 *
 * <p>Client-only screen opened by right-clicking a weather altar core that sits on top of a tuning
 * pedestal. Provides three buttons, each of which sends a {@link SetWeatherPayload} to the server
 * and closes the screen. Reuses the vanilla semi-transparent gradient background and the default
 * button click sound.</p>
 */
public class WeatherControlScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24;
    private static final int TITLE_Y = 40;

    public WeatherControlScreen() {
        super(Component.translatable("screen.weather_realm.weather_control.title"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - BUTTON_SPACING;
        addModeButton(WeatherMode.CLEAR, "screen.weather_realm.weather_control.clear", centerX, startY);
        addModeButton(WeatherMode.RAIN, "screen.weather_realm.weather_control.rain", centerX, startY + BUTTON_SPACING);
        addModeButton(WeatherMode.THUNDER, "screen.weather_realm.weather_control.thunder", centerX,
                startY + BUTTON_SPACING * 2);
    }

    private void addModeButton(WeatherMode mode, String labelKey, int centerX, int y) {
        this.addRenderableWidget(Button.builder(Component.translatable(labelKey), button -> selectMode(mode))
                .bounds(centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private void selectMode(WeatherMode mode) {
        PacketDistributor.sendToServer(new SetWeatherPayload(mode));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, TITLE_Y, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
