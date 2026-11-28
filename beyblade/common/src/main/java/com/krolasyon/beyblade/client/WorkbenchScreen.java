package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.item.PartItem;
import com.krolasyon.beyblade.parts.BeyPart;
import com.krolasyon.beyblade.parts.Loadout;
import com.krolasyon.beyblade.parts.PartType;
import com.krolasyon.beyblade.workbench.WorkbenchMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class WorkbenchScreen extends AbstractContainerScreen<WorkbenchMenu> {
   private static final int W = 258;
   private static final int H = 250;
   private static final int[][] BTN = new int[][]{{19, 4}, {-1, 19}, {6, 19}, {-1, 19}, {6, 19}};
   private final Button[] upgrade = new Button[5];
   private static final String[] STATS = new String[]{"atk", "def", "sta", "spd"};
   private static final int[] STAT_COL = new int[]{-42422, -10839297, -10821510, -10819344};

   public WorkbenchScreen(WorkbenchMenu menu, Inventory inv, Component title) {
      super(menu, inv, title);
      this.imageWidth = 258;
      this.imageHeight = 250;
      this.inventoryLabelX = 48;
      this.inventoryLabelY = 155;
      this.titleLabelX = 8;
      this.titleLabelY = 6;
   }

   protected void init() {
      super.init();

      for (PartType t : PartType.values()) {
         int i = t.ordinal();
         int[] p = WorkbenchMenu.POS[1 + i];
         this.upgrade[i] = (Button)this.addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
               this.minecraft.gameMode.handleInventoryButtonClick(((WorkbenchMenu)this.menu).containerId, i);
            }
         }).bounds(this.leftPos + p[0] + BTN[i][0], this.topPos + p[1] + BTN[i][1], 11, 11).build());
      }
   }

   protected void containerTick() {
      super.containerTick();

      for (PartType t : PartType.values()) {
         Button b = this.upgrade[t.ordinal()];
         ItemStack s = ((WorkbenchMenu)this.menu).partStack(t);
         b.visible = s.getItem() instanceof PartItem;
         b.active = ((WorkbenchMenu)this.menu).canUpgrade(t);
         if (b.visible) {
            int lvl = PartItem.level(s);
            b.setTooltip(
               Tooltip.create(
                  lvl >= 5
                     ? Component.translatable("screen.librabeyblade.workbench.maxed")
                     : Component.translatable(
                        "screen.librabeyblade.workbench.upgrade",
                        new Object[]{lvl + 1, WorkbenchMenu.UPGRADE_COUNT[lvl], WorkbenchMenu.UPGRADE_ITEM[lvl].getDescription()}
                     )
               )
            );
         }
      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      super.render(g, mouseX, mouseY, partial);
      this.renderTooltip(g, mouseX, mouseY);
   }

   protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
      int x = this.leftPos;
      int y = this.topPos;
      g.fill(x, y, x + 258, y + 250, -15068120);
      g.fill(x + 1, y + 1, x + 258 - 1, y + 250 - 1, -14016192);
      g.fillGradient(x + 4, y + 16, x + 128, y + 152, -12964262, -14476744);
      g.fillGradient(x + 131, y + 16, x + 258 - 4, y + 152, -13884346, -14804944);
      int cx = x + WorkbenchMenu.POS[0][0] + 8;
      int cy = y + WorkbenchMenu.POS[0][1] + 8;

      for (int i = 1; i <= 5; i++) {
         line(g, cx, cy, x + WorkbenchMenu.POS[i][0] + 8, y + WorkbenchMenu.POS[i][1] + 8, 1722843391);
      }

      for (Slot s : ((WorkbenchMenu)this.menu).slots) {
         boolean special = s.index < 7;
         int col = s.index == 0 ? -16320 : (special ? -6653216 : -11911072);
         g.fill(x + s.x - 2, y + s.y - 2, x + s.x + 18, y + s.y + 18, col);
         g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, -15593954);
      }

      g.pose().pushPose();
      g.pose().scale(0.6F, 0.6F, 1.0F);

      for (PartType t : PartType.values()) {
         int[] p = WorkbenchMenu.POS[1 + t.ordinal()];
         Component c = Component.translatable(t.nameKey());
         g.drawString(
            this.font, c, (int)((float)(x + p[0] + 8) / 0.6F) - this.font.width(c) / 2, (int)((float)(y + p[1] - 8) / 0.6F), -2569985, false
         );
      }

      int[] mp = WorkbenchMenu.POS[6];
      Component mc = Component.translatable("screen.librabeyblade.workbench.material");
      g.drawString(this.font, mc, (int)((float)(x + mp[0] + 8) / 0.6F) - this.font.width(mc) / 2, (int)((float)(y + mp[1] - 8) / 0.6F), -8064, false);
      g.pose().popPose();
   }

   private static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int col) {
      int n = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));

      for (int i = 0; i <= n; i += 2) {
         int px = x0 + (x1 - x0) * i / Math.max(1, n);
         int py = y0 + (y1 - y0) * i / Math.max(1, n);
         g.fill(px, py, px + 1, py + 1, col);
      }
   }

   protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
      g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, -10144, false);
      g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -3620640, false);
      int px = 134;
      int py = 20;
      ItemStack bey = ((WorkbenchMenu)this.menu).bey();
      Loadout l = ((WorkbenchMenu)this.menu).loadout();
      if (l == null) {
         g.drawWordWrap(this.font, Component.translatable("screen.librabeyblade.workbench.empty"), px, py + 30, 118, -5199664);
      } else {
         g.pose().pushPose();
         g.pose().translate(232.0F, (float)(py - 2), 100.0F);
         g.pose().scale(1.3F, 1.3F, 1.3F);
         g.renderItem(bey, 0, 0);
         g.pose().popPose();
         g.drawString(this.font, this.trim(bey.getHoverName(), 94), px, py, -1, false);
         String[] lean = new String[]{"attack", "defense", "stamina", "balance"};
         g.drawString(
            this.font,
            Component.translatable("screen.librabeyblade.workbench.type", new Object[]{Component.translatable("mode.librabeyblade." + lean[l.leaning()])}),
            px,
            py + 11,
            -14224,
            false
         );
         if (!l.complete()) {
            g.drawString(this.font, Component.translatable("screen.librabeyblade.workbench.incomplete"), px, py + 22, -40864, false);
         }

         float[] vals = new float[]{l.attack(), l.defense(), l.stamina(), l.speed()};

         for (int i = 0; i < 4; i++) {
            int by = py + 34 + i * 11;
            g.drawString(this.font, Component.translatable("stat.librabeyblade." + STATS[i] + ".short"), px, by, STAT_COL[i], false);
            int bw = 70;
            g.fill(px + 26, by + 1, px + 26 + bw, by + 7, -15593954);
            int fill = Math.min(bw, Math.round(vals[i] / 90.0F * (float)bw));
            g.fill(px + 26, by + 1, px + 26 + fill, by + 7, STAT_COL[i]);
            g.drawString(this.font, String.valueOf(Math.round(vals[i])), px + 100, by, -1, false);
         }

         int iy = py + 80;
         g.drawString(
            this.font,
            Component.translatable("stat.librabeyblade.weight", new Object[]{String.format(Locale.ROOT, "%.1f", l.weight())}),
            px,
            iy,
            -3092264,
            false
         );
         g.drawString(
            this.font,
            Component.translatable("stat.librabeyblade.height", new Object[]{String.format(Locale.ROOT, "%.1f", (float)l.height() / 10.0F)}),
            px,
            iy + 10,
            -3092264,
            false
         );
         g.pose().pushPose();
         g.pose().translate((float)px, (float)(iy + 21), 0.0F);
         g.pose().scale(0.7F, 0.7F, 1.0F);
         int ty = 0;
         Component tip = Component.translatable("tip.librabeyblade." + l.tip().name().toLowerCase(Locale.ROOT));
         g.drawWordWrap(this.font, tip, 0, ty, 168, -8331024);
         ty += this.font.wordWrapHeight(tip, 168) + 2;
         if (l.trackTrait() != BeyPart.Trait.NONE) {
            g.drawWordWrap(this.font, Component.translatable("trait.librabeyblade." + l.trackTrait().name().toLowerCase(Locale.ROOT)), 0, ty, 168, -20256);
         }

         g.pose().popPose();
      }
   }

   private Component trim(Component c, int w) {
      String s = c.getString();
      return (Component)(this.font.width(s) <= w ? c : Component.literal(this.font.plainSubstrByWidth(s, w - 6) + "…"));
   }

   protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
      if (this.hoveredSlot != null
         && this.hoveredSlot.index > 0
         && this.hoveredSlot.index < 6
         && this.hoveredSlot.hasItem()
         && this.hoveredSlot.getItem().getItem() instanceof PartItem pi) {
         List<Component> lines = new ArrayList<>();
         lines.add(this.hoveredSlot.getItem().getHoverName().copy().withStyle(ChatFormatting.WHITE));
         PartItem.appendPartLines(pi.part(), PartItem.level(this.hoveredSlot.getItem()), lines);
         g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
      } else {
         super.renderTooltip(g, mouseX, mouseY);
      }
   }
}
