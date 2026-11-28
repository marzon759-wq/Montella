package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.compat.V;
import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.entity.CommonBeybladeEntity;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class LibraHud {
   private static final ResourceLocation ICON = V.rl("librabeyblade", "textures/item/libra_icon.png");
   private static final ResourceLocation ICON_LDRAGO = V.rl("librabeyblade", "textures/item/ldrago_icon.png");
   private static final ResourceLocation ICON_LEONE = V.rl("librabeyblade", "textures/item/leone_icon.png");
   private static final int[] LE_COLORS = new int[]{-6488144, -9633942, -12525440, -2047888, -5177392, -2565928};
   private static final ResourceLocation ICON_EAGLE = V.rl("librabeyblade", "textures/item/eagle_icon.png");
   private static final int[] EA_COLORS = new int[]{-2047745, -4153089, -36752, -3092256, -12192};
   private static final ResourceLocation ICON_PEGASUS = V.rl("librabeyblade", "textures/item/pegasus_icon.png");
   private static final int[] PG_COLORS = new int[]{-6235905, -9391873, -3968, -10436353, -1};
   private static final int[] DR_COLORS = new int[]{-49056, -2080608, -30166, -53168, -20416};
   private static final int[] MODE_COLORS = new int[]{-40880, -10444545, -9371760, -12208};
   private static final int[] AB_COLORS = new int[]{-9633894, -10682448, -30166, -8716304, -11686};

   public void render(GuiGraphics g, float partial, int width, int height) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null && !mc.options.hideGui) {
         BeybladeEntity bey = null;
         List<BeybladeEntity> list = mc.level
            .getEntitiesOfClass(BeybladeEntity.class, mc.player.getBoundingBox().inflate(96.0), b -> mc.player.getUUID().equals(b.getOwnerId()));
         if (!list.isEmpty()) {
            bey = list.get(0);
         }

         if (bey != null) {
            Font font = mc.font;
            int x = 8;
            int y = height / 2 - 66;
            g.fill(x - 4, y - 4, x + 168, y + 174, -2013265920);
            boolean tempo = bey.kindId().equals("tempo");
            boolean variares = bey.kindId().equals("variares");
            boolean beffal = bey.kindId().equals("beffal");
            boolean drago = bey.kindId().equals("ldrago");
            boolean leone = bey.kindId().equals("leone");
            boolean eagle = bey.kindId().equals("eagle");
            boolean pegasus = bey.kindId().equals("pegasus");
            CommonBeybladeEntity common = bey instanceof CommonBeybladeEntity c ? c : null;
            if (common != null) {
               g.blit(V.rl("librabeyblade", "textures/item/common_" + common.getVariant() + "_icon.png"), x, y, 0.0F, 0.0F, 24, 24, 24, 24);
            } else if (beffal) {
               g.blit(V.rl("librabeyblade", "textures/item/beffal_icon.png"), x, y, 0.0F, 0.0F, 24, 24, 24, 24);
            } else if (variares) {
               g.blit(V.rl("librabeyblade", "textures/item/variares_icon.png"), x, y, 0.0F, 0.0F, 24, 24, 24, 24);
            } else if (tempo) {
               g.blit(V.rl("librabeyblade", "textures/item/tempo_icon.png"), x, y, 0.0F, 0.0F, 24, 24, 24, 24);
            } else {
               g.blit(
                  drago ? ICON_LDRAGO : (leone ? ICON_LEONE : (eagle ? ICON_EAGLE : (pegasus ? ICON_PEGASUS : ICON))), x, y, 0.0F, 0.0F, 24, 24, 24, 24
               );
            }

            g.drawString(
               font,
               Component.translatable(bey.titleKey()),
               x + 29,
               y + 2,
               common != null
                  ? 0xFF000000 | common.variant().primary()
                  : (
                     tempo
                        ? -3087105
                        : (
                           variares
                              ? -49088
                              : (beffal ? -36664 : (drago ? -44944 : (leone ? -10420336 : (eagle ? -3632897 : (pegasus ? -9389825 : -4784278)))))
                        )
                  ),
               true
            );

            String state = switch (bey.getState()) {
               case 1 -> "hud.librabeyblade.state.defeated";
               case 2 -> "hud.librabeyblade.state.recall";
               default -> bey.getAbility() != 0 ? "hud.librabeyblade.state.ability" : "hud.librabeyblade.state.battle";
            };
            g.drawString(font, Component.translatable(state), x + 29, y + 13, -3158065, true);
            String lv = "Lv " + bey.getLevel();
            g.drawString(font, lv, x + 160 - font.width(lv), y + 13, -12224, true);
            float ratio = bey.spinRatio();
            int by = y + 29;
            int bw = 160;
            g.fill(x, by, x + bw, by + 7, -15066598);
            int col = ratio > 0.5F ? -8585398 : (ratio > 0.2F ? -12224 : -46534);
            g.fill(x + 1, by + 1, x + 1 + (int)((float)(bw - 2) * ratio), by + 6, col);
            g.drawString(
               font,
               Component.translatable("hud.librabeyblade.spin", new Object[]{Mth.ceil(bey.getSpin()), Mth.ceil(bey.getMaxSpin())}),
               x,
               by + 9,
               -1,
               true
            );
            float xr = bey.getLevel() >= 30 ? 1.0F : Mth.clamp((float)bey.getXp() / (float)BeybladeEntity.xpNeeded(bey.getLevel()), 0.0F, 1.0F);
            g.fill(x, by + 18, x + bw, by + 20, -14671840);
            g.fill(x, by + 18, x + (int)((float)bw * xr), by + 20, -10432257);
            int ay = by + 24;

            for (int i = 1; i <= bey.abilityCount(); i++) {
               KeyMapping key = ClientSetup.ABILITY_KEYS[i - 1];
               int cd = bey.cooldownRemaining(i);
               boolean active = bey.getAbility() == i || i == bey.buffAbility() && bey.isShielded();
               boolean locked = !bey.abilityAvailable(i);
               int cx = active
                  ? -1
                  : (
                     cd > 0 || locked
                        ? -8355712
                        : (
                           common != null
                              ? 0xFF000000 | common.variant().secondary()
                              : (
                                 tempo
                                    ? -3613441
                                    : (
                                       variares
                                          ? -38320
                                          : (
                                             beffal
                                                ? -1535745
                                                : (drago ? DR_COLORS : (leone ? LE_COLORS : (eagle ? EA_COLORS : (pegasus ? PG_COLORS : AB_COLORS))))[i - 1]
                                          )
                                    )
                              )
                        )
                  );
               String keyName = key.getTranslatedKeyMessage().getString();
               g.drawString(font, "[" + keyName + "]", x, ay, -8080, true);
               g.drawString(font, Component.translatable(bey.abilityKey(i)), x + 22, ay, cx, true);
               if (locked && common != null && i == 3) {
                  String s = "Lv5";
                  g.drawString(font, s, x + 160 - font.width(s), ay, -7303024, true);
               } else if (cd > 0 && !locked) {
                  String s = (cd + 19) / 20 + "s";
                  g.drawString(font, s, x + 160 - font.width(s), ay, -36752, true);
               } else if (active) {
                  g.drawString(font, "✦", x + 154, ay, -1, true);
               }

               ay += 10;
            }

            g.drawString(font, "[" + ClientSetup.RECALL_KEY.getTranslatedKeyMessage().getString() + "] ", x, ay + 2, -8080, true);
            g.drawString(font, Component.translatable("hud.librabeyblade.recall"), x + 22, ay + 2, -5185281, true);
            ay += 12;
            g.drawString(font, "[" + ClientSetup.MODE_KEY.getTranslatedKeyMessage().getString() + "]", x, ay, -8080, true);
            boolean nat = bey.getMode() == bey.nativeMode();
            g.drawString(
               font,
               Component.translatable("mode.librabeyblade." + BeybladeEntity.MODE_IDS[bey.getMode()]).append(nat ? " ★" : ""),
               x + 22,
               ay,
               MODE_COLORS[bey.getMode()],
               true
            );
            ay += 10;
            g.drawString(font, "[" + ClientSetup.COMMAND_KEY.getTranslatedKeyMessage().getString() + "]", x, ay, -8080, true);
            g.drawString(font, Component.translatable("order.librabeyblade." + BeybladeEntity.ORDER_IDS[bey.getOrder()]), x + 22, ay, -2039584, true);
            ay += 12;
            g.drawString(font, "[" + ClientSetup.SPIRIT_KEY.getTranslatedKeyMessage().getString() + "]", x, ay, -8080, true);
            boolean sp = bey.isSpiritActive();
            float sr = sp ? (float)bey.spiritRemaining() / 180.0F : (float)bey.getSpirit() / 100.0F;
            g.fill(x + 22, ay + 1, x + 160, ay + 8, -14671840);
            int scol = sp ? (mc.level.getGameTime() / 3L % 2L == 0L ? -8096 : -28640) : (bey.getSpirit() >= 100 ? -12224 : -5214176);
            g.fill(x + 23, ay + 2, x + 23 + (int)(136.0F * Mth.clamp(sr, 0.0F, 1.0F)), ay + 7, scol);
            g.drawString(
               font,
               Component.translatable(
                  sp ? "hud.librabeyblade.spirit_on" : (bey.getSpirit() >= 100 ? "hud.librabeyblade.spirit_ready" : "hud.librabeyblade.spirit")
               ),
               x + 26,
               ay,
               -1,
               true
            );
            ay += 11;
            Component passive = bey.passiveLine();
            if (passive != null) {
               g.drawString(font, passive, x, ay, -40848, true);
               ay += 10;
            }

            if (bey.isFinalDrive()) {
               g.drawString(font, Component.translatable("hud.librabeyblade.final_drive"), x, ay, -40896, true);
            } else if (bey.isLifeAfterDeath()) {
               g.drawString(font, Component.translatable("hud.librabeyblade.lad"), x, ay, -10420336, true);
            }
         }
      }
   }
}
