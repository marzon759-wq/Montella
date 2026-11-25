package com.krolasyon.bloodsea.compat;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** A screen that draws no background of its own (no darkening, no blur): the world shows through (Forge 1.21.1). */
public abstract class CScreen extends Screen {
    protected CScreen() { super(Component.literal("")); }

    @Override public void renderBackground(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {}
}
