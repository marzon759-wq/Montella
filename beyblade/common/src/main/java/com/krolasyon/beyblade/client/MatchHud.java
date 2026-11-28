package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.StadiumEntity;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class MatchHud {
   public void render(GuiGraphics g, float partial, int width, int height) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null && !mc.options.hideGui) {
         List<StadiumEntity> list = mc.level.getEntitiesOfClass(StadiumEntity.class, mc.player.getBoundingBox().inflate(48.0), sx -> sx.matchState() != 0);
         if (!list.isEmpty()) {
            StadiumEntity s = list.get(0);
            Font f = mc.font;
            String a = s.nameA();
            String b = s.nameB();
            String score = s.scoreA() + "  -  " + s.scoreB();
            int cx = width / 2;
            int y = 6;
            int w = Math.max(220, f.width(a) + f.width(b) + f.width(score) + 60);
            g.fill(cx - w / 2, y - 3, cx + w / 2, y + 26, -1610612736);
            g.fill(cx - w / 2, y - 3, cx - w / 2 + 3, y + 26, -12615681);
            g.fill(cx + w / 2 - 3, y - 3, cx + w / 2, y + 26, -49104);
            g.drawString(f, a, cx - f.width(score) / 2 - 12 - f.width(a), y + 2, -8408833, true);
            g.drawString(f, b, cx + f.width(score) / 2 + 12, y + 2, -32656, true);
            g.drawCenteredString(f, score, cx, y + 2, -8080);

            String state = switch (s.matchState()) {
               case 1 -> Component.translatable("hud.librabeyblade.match.countdown", new Object[]{s.round()}).getString();
               case 2 -> Component.translatable("hud.librabeyblade.match.battle", new Object[]{s.round()}).getString();
               case 3 -> Component.translatable("hud.librabeyblade.match.round_end").getString();
               default -> Component.translatable("hud.librabeyblade.match.end").getString();
            };
            g.drawCenteredString(f, state, cx, y + 14, -3355444);
            g.drawCenteredString(f, Component.translatable("hud.librabeyblade.match.goal", new Object[]{4}).getString(), cx, y + 30, -7303024);
         }
      }
   }
}
