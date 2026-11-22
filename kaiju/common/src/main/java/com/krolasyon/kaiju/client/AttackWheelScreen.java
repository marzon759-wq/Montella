package com.krolasyon.kaiju.client;

import com.krolasyon.kaiju.data.KaijuSpec;
import com.krolasyon.kaiju.entity.KaijuEntity;
import com.krolasyon.kaiju.net.Net;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

/** Z: every attack grouped by category. Click = perform now and keep it on the special key (Y). */
public class AttackWheelScreen extends Screen {
    final KaijuEntity k;

    public AttackWheelScreen(KaijuEntity k) {
        super(Component.translatable("screen.kaiju.wheel"));
        this.k = k;
    }

    @Override
    protected void init() {
        Map<String, List<KaijuSpec.Attack>> cats = new LinkedHashMap<>();
        for (KaijuSpec.Attack a : k.spec.attacks) cats.computeIfAbsent(a.cat, c -> new ArrayList<>()).add(a);
        int cols = cats.size();
        int colW = Math.min(118, (width - 20) / Math.max(1, cols));
        int x = (width - colW * cols) / 2;
        for (Map.Entry<String, List<KaijuSpec.Attack>> e : cats.entrySet()) {
            int y = 34;
            for (KaijuSpec.Attack a : e.getValue()) {
                String id = a.id;
                addRenderableWidget(Button.builder(Component.translatable("attack.kaiju." + id), b -> {
                    Net.sendSelect(id, true);
                    onClose();
                }).bounds(x + 1, y, colW - 2, 13).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(id))).build());
                y += 14;
            }
            x += colW;
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 8, 0x9FE8FF);
        Map<String, Integer> seen = new LinkedHashMap<>();
        for (KaijuSpec.Attack a : k.spec.attacks) seen.putIfAbsent(a.cat, seen.size());
        int cols = seen.size();
        int colW = Math.min(118, (width - 20) / Math.max(1, cols));
        int x = (width - colW * cols) / 2;
        for (String c : seen.keySet()) {
            g.drawCenteredString(font, Component.translatable("cat.kaiju." + c), x + colW / 2, 22, 0xFFD27A);
            x += colW;
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
