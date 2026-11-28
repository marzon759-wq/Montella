package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.item.LaunchPlan;
import com.krolasyon.beyblade.item.LauncherItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class LaunchHud {
   public void render(GuiGraphics g, float partial, int width, int height) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && !mc.options.hideGui && mc.player.isUsingItem() && mc.player.getUseItem().getItem() instanceof LauncherItem) {
         int used = mc.player.getTicksUsingItem();
         LaunchPlan plan = LaunchPlan.of(mc.player, used);
         Font f = mc.font;
         int x = width / 2 + 18;
         int y = height / 2 - 34;
         int w = 128;
         g.fill(x - 4, y - 4, x + w + 4, y + 66, -1879048192);
         g.drawString(f, Component.translatable("launch.librabeyblade.power"), x, y, -1, true);
         int bx = x + 44;
         int bw = w - 44;
         float pw = Mth.clamp((float)used / 40.0F, 0.0F, 1.0F);
         g.fill(bx, y + 1, bx + bw, y + 8, -14671840);
         int col = plan.overcharged ? -49088 : (plan.perfect ? -12224 : -11476880);
         g.fill(bx, y + 1, bx + Math.round((float)bw * pw), y + 8, col);
         if (plan.perfect && used / 2 % 2 == 0) {
            g.fill(bx - 1, y, bx + bw + 1, y + 9, 1627381888);
         }

         g.drawString(
            f, Component.translatable("launch.librabeyblade.angle", new Object[]{Math.round(plan.pitch)}), x, y + 14, plan.idealAngle() ? -9371760 : -36768, true
         );
         int ay = y + 24;
         g.fill(bx, ay, bx + bw, ay + 5, -12574688);
         int z0 = bx + Math.round((float)bw * 15.0F / 90.0F);
         int z1 = bx + Math.round((float)bw * 50.0F / 90.0F);
         g.fill(z0, ay, z1, ay + 5, -13727170);
         int mk = bx + Math.round((float)bw * Mth.clamp(plan.pitch, 0.0F, 90.0F) / 90.0F);
         g.fill(mk - 1, ay - 2, mk + 1, ay + 7, -1);
         float steady = 1.0F - plan.wobble;
         g.drawString(f, Component.translatable("launch.librabeyblade.balance"), x, y + 32, -1, true);
         g.fill(bx, y + 33, bx + bw, y + 40, -14671840);
         int sc = steady > 0.7F ? -11476880 : (steady > 0.4F ? -995264 : -49088);
         g.fill(bx, y + 33, bx + Math.round((float)bw * steady), y + 40, sc);
         g.drawString(f, Math.round(steady * 100.0F) + "%", bx + bw - f.width(Math.round(steady * 100.0F) + "%") - 1, y + 32, -1, true);
         Component hint = plan.hint == null
            ? Component.translatable("launch.librabeyblade.hint.good")
            : Component.translatable("launch.librabeyblade.hint." + plan.hint);
         int hc = plan.hint == null ? -7274592 : ("perfect".equals(plan.hint) ? -12224 : -24464);
         g.drawString(f, hint, x, y + 46, hc, true);
         if (plan.powerLaunch) {
            g.drawString(f, Component.translatable("launch.librabeyblade.power_launch"), x, y + 56, -32640, true);
         }
      }
   }
}
