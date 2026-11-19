package com.krolasyon.mimic.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Base for the story screens: never blurs or dims the world behind it. */
public abstract class HScreen extends Screen {
    protected int age = 0;

    protected HScreen() { super(Component.empty()); }

    /** Overrides the vanilla background on versions where render() calls it; an ordinary method on older ones. */
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public void tick() { age++; }

    @Override
    public boolean isPauseScreen() { return false; }
}
