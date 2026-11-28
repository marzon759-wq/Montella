package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.network.ModNetwork;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CommandScreen extends Screen {
   private static final int BW = 104;
   private static final int BH = 20;
   private static final int GAP = 4;

   public CommandScreen() {
      super(Component.translatable("screen.librabeyblade.commands"));
   }

   @Nullable
   private static BeybladeEntity active() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         List<BeybladeEntity> list = mc.level
            .getEntitiesOfClass(BeybladeEntity.class, mc.player.getBoundingBox().inflate(96.0), b -> mc.player.getUUID().equals(b.getOwnerId()));
         return list.isEmpty() ? null : list.get(0);
      } else {
         return null;
      }
   }

   protected void init() {
      int cols = 3;
      int rows = (14 + cols - 1) / cols;
      int totalW = cols * 104 + (cols - 1) * 4;
      int x0 = (this.width - totalW) / 2;
      int y0 = Math.max(30, this.height / 2 - (rows * 24 + 50) / 2);

      for (int i = 0; i < 14; i++) {
         int id = i;
         int cx = x0 + i % cols * 108;
         int cy = y0 + i / cols * 24;
         String key = "order.librabeyblade." + BeybladeEntity.ORDER_IDS[i];
         this.addRenderableWidget(Button.builder(Component.translatable(key), b -> {
            ModNetwork.sendCommand(20 + id);
            this.onClose();
         }).bounds(cx, cy, 104, 20).tooltip(Tooltip.create(Component.translatable(key + ".desc"))).build());
      }

      int my = y0 + rows * 24 + 22;
      int mw = (totalW - 12) / 4;

      for (int m = 0; m < 4; m++) {
         int id = m;
         String key = "mode.librabeyblade." + BeybladeEntity.MODE_IDS[m];
         this.addRenderableWidget(
            Button.builder(Component.translatable(key), b -> ModNetwork.sendCommand(40 + id))
               .bounds(x0 + m * (mw + 4), my, mw, 20)
               .tooltip(Tooltip.create(Component.translatable(key + ".desc")))
               .build()
         );
      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      int cols = 3;
      int rows = (14 + cols - 1) / cols;
      int totalW = cols * 104 + (cols - 1) * 4;
      int x0 = (this.width - totalW) / 2;
      int y0 = Math.max(30, this.height / 2 - (rows * 24 + 50) / 2);
      g.drawCenteredString(this.font, this.title, this.width / 2, y0 - 22, -12224);
      BeybladeEntity b = active();
      if (b == null) {
         g.drawCenteredString(this.font, Component.translatable("msg.librabeyblade.none_active"), this.width / 2, y0 - 11, -36752);
      } else {
         int o = b.getOrder();
         int ox = x0 + o % cols * 108;
         int oy = y0 + o / cols * 24;
         g.fill(ox - 2, oy - 2, ox + 104 + 2, oy + 20 + 2, -12224);
         int my = y0 + rows * 24 + 22;
         int mw = (totalW - 12) / 4;
         int mx = x0 + b.getMode() * (mw + 4);
         g.fill(mx - 2, my - 2, mx + mw + 2, my + 20 + 2, -10432257);
         g.drawCenteredString(
            this.font,
            Component.translatable("screen.librabeyblade.level", new Object[]{b.getLevel(), b.getXp(), BeybladeEntity.xpNeeded(b.getLevel())}),
            this.width / 2,
            y0 - 11,
            -5177424
         );
      }

      int my = y0 + rows * 24 + 10;
      g.drawCenteredString(this.font, Component.translatable("screen.librabeyblade.modes"), this.width / 2, my, -6498049);
      super.render(g, mouseX, mouseY, partial);
   }

   public boolean keyPressed(int key, int scan, int mods) {
      if (ClientSetup.COMMAND_KEY.matches(key, scan)) {
         this.onClose();
         return true;
      } else {
         return super.keyPressed(key, scan, mods);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }
}
