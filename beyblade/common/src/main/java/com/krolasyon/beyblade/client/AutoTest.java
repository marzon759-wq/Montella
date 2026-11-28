package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.parts.BeyPart;
import com.krolasyon.beyblade.parts.Loadout;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.RenderTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

public final class AutoTest {
   private static final Logger LOG = LogUtils.getLogger();
   private int tick;
   private boolean worldRequested;
   private int inWorld = -1;
   private final List<AutoTest.Step> steps = new ArrayList<>();
   private int stepIndex;
   private int stepStart;
   private boolean showGui;
   private boolean allowScreen;
   private static final String A = "summon librabeyblade:leone 0 -60 -6 {Spin:3000f,MaxSpin:3000f,Tags:[\"a\"]}";
   private static final String B = "summon librabeyblade:eagle 3.5 -60 -9 {Spin:3000f,MaxSpin:3000f,Frozen:1b,Tags:[\"b\"]}";
   private static final String ST = "summon librabeyblade:stadium 0.5 -60 -14.5";
   private static final String CLEAR = "kill @e[type=!player,type=!librabeyblade:stadium]";
   private boolean recording;
   private int frameNo;
   private long lastFrameNanos;
   private final StringBuilder frameList = new StringBuilder();
   private final ExecutorService writer = Executors.newFixedThreadPool(3);

   public static void init() {
      LOG.info("[AUTOTEST] enabled");
      BeybladeEntity.rivalAbilities = false;
      MinecraftForge.EVENT_BUS.register(new AutoTest());
   }

   private void showcase(String kind, int ability, String cam, int ticks, String... shots) {
      this.cmd(
         4,
         null,
         "kill @e[type=!player,type=!librabeyblade:stadium]",
         "weather clear",
         cam,
         "summon librabeyblade:" + kind + " 0.5 -58 -12 {Tags:[\"a\"]}",
         "summon librabeyblade:common -2.5 -58 -17 {Variant:5}",
         "summon librabeyblade:common 3.5 -58 -17.5 {Variant:0}"
      );
      this.wait(18, null);
      this.force(ability);
      int step = ticks / (shots.length + 1);

      for (String sh : shots) {
         this.wait(step, sh);
      }

      this.wait(step, null);
   }

   private AutoTest() {
      this.cmd(
         0,
         null,
         "gamerule doDaylightCycle false",
         "gamerule doWeatherCycle false",
         "gamerule doMobSpawning false",
         "time set 5000",
         "weather clear",
         "difficulty peaceful",
         "gamemode spectator @a",
         "kill @e[type=!player]",
         "summon librabeyblade:stadium 0.5 -60 -14.5"
      );
      // the whole line up, standing still
      this.cmd(
         10,
         null,
         "tp @a 0.5 -57.0 -10.6 180 28",
         "summon librabeyblade:leone -2.9 -58 -14.5 {Frozen:1b}",
         "summon librabeyblade:eagle -1.6 -58 -14.5 {Frozen:1b}",
         "summon librabeyblade:ldrago -0.3 -58 -14.5 {Frozen:1b}",
         "summon librabeyblade:pegasus 1.0 -58 -14.5 {Frozen:1b}",
         "summon librabeyblade:libra 2.3 -58 -14.5 {Frozen:1b}",
         "summon librabeyblade:tempo 3.6 -58 -14.5 {Frozen:1b}"
      );
      this.wait(40, "lineup");
      // custom bottoms: rubber, bearing and metal tips turned on the lathe
      this.cmd(2, null, CLEAR, "tp @a 0.5 -60.9 -12.4 180 6");
      this.server(4, null, s -> {
         launchCustom(s, com.krolasyon.beyblade.registry.ModEntities.PEGASUS.get(), -1.0,
            custom(com.krolasyon.beyblade.registry.ModItems.PEGASUS.get(), "track_df145:1", "tip_rs:1"), true);
         launchCustom(s, com.krolasyon.beyblade.registry.ModEntities.LIBRA.get(), 0.5,
            custom(com.krolasyon.beyblade.registry.ModItems.LIBRA.get(), "track_ad145:1", "tip_bd:1"), true);
         launchCustom(s, com.krolasyon.beyblade.registry.ModEntities.LEONE.get(), 2.0,
            custom(com.krolasyon.beyblade.registry.ModItems.LEONE.get(), "track_lw160:1", "tip_q:1"), true);
      });
      this.server(30, "tips", s -> {
      });
      // clash: two beys thrown into each other
      this.cmd(
         2,
         null,
         CLEAR,
         "tp @a 0.5 -57.6 -10.9 180 34",
         "summon librabeyblade:pegasus -2.0 -58 -14.5 {Motion:[0.32,0.0,0.0],Tags:[\"a\"]}",
         "summon librabeyblade:leone 3.0 -58 -14.5 {Motion:[-0.32,0.0,0.0]}"
      );
      for (int k = 0; k < 6; k++) {
         this.wait(3, "clash_" + k);
      }
      this.wait(30, null);
      // special moves
      String[] kinds = {"ldrago", "pegasus", "libra", "tempo"};
      int[] abil = {3, 3, 3, 1};
      for (int i = 0; i < kinds.length; i++) {
         int ab = abil[i];
         this.cmd(2, null, CLEAR, "tp @a 0.5 -57.9 -11.4 180 32", "summon librabeyblade:" + kinds[i] + " 0.5 -58 -14.5 {Tags:[\"a\"]}",
            "summon librabeyblade:common 3.5 -58 -16 {Variant:3}");
         this.wait(16, null);
         this.force(ab);
         this.wait(8, "special_" + kinds[i] + "_a");
         this.wait(8, "special_" + kinds[i] + "_b");
         this.wait(10, "special_" + kinds[i] + "_c");
         this.wait(30, null);
      }
      // burst finish
      this.cmd(2, null, CLEAR, "tp @a 0.5 -58.4 -12.7 180 40", "summon librabeyblade:eagle 0.5 -58 -14.5 {Tags:[\"a\"]}");
      this.wait(20, null);
      this.server(1, null, s -> each(s, "a", BeybladeEntity::debugBurst));
      this.wait(4, "burst_a");
      this.wait(6, "burst_b");
      this.wait(10, "burst_c");
      this.wait(30, "burst_d");
      // sleep out
      this.cmd(2, null, CLEAR, "summon librabeyblade:libra 0.5 -58 -14.5 {Tags:[\"a\"]}");
      this.wait(20, null);
      this.server(1, null, s -> each(s, "a", BeybladeEntity::debugSleepOut));
      this.wait(10, "sleep_a");
      this.wait(30, "sleep_b");
      this.wait(20, "END");
   }

   private static ItemStack custom(Item bey, String... parts) {
      ItemStack st = new ItemStack(bey);

      for (String p : parts) {
         String[] a = p.split(":");
         BeyPart part = BeyPart.get(a[0]);
         Loadout.write(st, part.type(), part, Integer.parseInt(a[1]));
      }

      return st;
   }

   private static void launchCustom(MinecraftServer s, EntityType<? extends BeybladeEntity> type, double x, ItemStack st, boolean frozen) {
      BeybladeEntity b = BeybladeEntity.launch(
         type, s.overworld(), null, st, new Vec3(x, -57.5, frozen ? -14.5 : -14.5 + (double)(x > 0.0 ? -2 : 2)), Vec3.ZERO, 1.0F
      );
      b.setFrozen(frozen);
   }

   private void record(boolean on) {
      this.client(0, null, mc -> this.recording = on);
   }

   @SubscribeEvent
   public void onRender(RenderTickEvent event) {
      if (event.phase == Phase.END) {
         Minecraft mc = Minecraft.getInstance();
         long now = System.nanoTime();
         if (this.recording && mc.level != null) {
            if (this.lastFrameNanos != 0L) {
               this.frameList.append(String.format(Locale.ROOT, "duration %.4f%n", Math.min(0.25, (double)(now - this.lastFrameNanos) / 1.0E9)));
            }

            this.lastFrameNanos = now;
            String name = String.format("f_%06d.jpg", this.frameNo++);
            this.frameList.append("file '").append(name).append("'\n");
            NativeImage img = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            File dir = new File(mc.gameDirectory, "frames");
            dir.mkdirs();
            this.writer.submit(() -> {
               try {
                  BufferedImage bi = new BufferedImage(img.getWidth(), img.getHeight(), 1);

                  for (int y = 0; y < img.getHeight(); y++) {
                     for (int x = 0; x < img.getWidth(); x++) {
                        int abgr = img.getPixelRGBA(x, y);
                        bi.setRGB(x, y, (abgr & 0xFF) << 16 | abgr & 0xFF00 | abgr >> 16 & 0xFF);
                     }
                  }

                  ImageIO.write(bi, "jpg", new File(dir, name));
               } catch (Exception var10) {
                  LOG.error("[AUTOTEST] frame write failed", var10);
               } finally {
                  img.close();
               }
            });
         } else {
            this.lastFrameNanos = 0L;
         }
      }
   }

   private void cmd(int wait, String shot, String... commands) {
      this.steps.add(new AutoTest.Step(wait, shot, s -> {
         for (String c : commands) {
            s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
         }
      }, null));
   }

   private void wait(int ticks, String shot) {
      this.steps.add(new AutoTest.Step(ticks, shot, null, null));
   }

   private void server(int wait, String shot, Consumer<MinecraftServer> action) {
      this.steps.add(new AutoTest.Step(wait, shot, action, null));
   }

   private void client(int wait, String shot, Consumer<Minecraft> action) {
      this.steps.add(new AutoTest.Step(wait, shot, null, null, action));
   }

   private void force(int ability) {
      this.server(1, null, s -> each(s, "a", b -> {
            LOG.info("[AUTOTEST] force ability {} spin={}", ability, b.getSpin());
            b.debugForceAbility(ability);
         }));
   }

   private static void each(MinecraftServer s, String tag, Consumer<BeybladeEntity> c) {
      ServerLevel l = s.overworld();

      for (BeybladeEntity e : l.getEntitiesOfClass(BeybladeEntity.class, new AABB(-80.0, -80.0, -80.0, 80.0, 40.0, 80.0))) {
         if (tag == null || e.getTags().contains(tag)) {
            c.accept(e);
         }
      }
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         Minecraft mc = Minecraft.getInstance();
         this.tick++;
         mc.options.pauseOnLostFocus = false;
         if (mc.getOverlay() == null) {
            if (!this.worldRequested && this.tick > 40 && mc.level == null) {
               this.worldRequested = true;
               LOG.info("[AUTOTEST] creating world");
               LevelSettings settings = new LevelSettings(
                  "ltest", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT
               );
               com.krolasyon.beyblade.compat.VClient.createFlatTestWorld(mc, "ltest", settings, new WorldOptions(42L, false, false));
            } else if (mc.level != null && mc.player != null && mc.getSingleplayerServer() != null) {
               if (mc.screen != null && !(mc.screen instanceof CommandScreen) && !(mc.screen instanceof WorkbenchScreen)) {
                  mc.setScreen(null);
               }

               mc.options.hideGui = !this.showGui;
               if (this.inWorld < 0) {
                  this.inWorld = this.tick;
                  this.stepStart = this.tick + 100;
                  LOG.info("[AUTOTEST] in world");
               }

               if (this.tick >= this.stepStart && this.stepIndex < this.steps.size()) {
                  AutoTest.Step st = this.steps.get(this.stepIndex);
                  if (this.tick == this.stepStart) {
                     if (st.gui() != null) {
                        this.showGui = st.gui();
                     }

                     if (st.clientAction() != null) {
                        st.clientAction().accept(mc);
                     }

                     if (st.action() != null) {
                        MinecraftServer s = mc.getSingleplayerServer();
                        s.execute(() -> {
                           try {
                              st.action().accept(s);
                           } catch (Throwable var3x) {
                              LOG.error("[AUTOTEST] step failed", var3x);
                           }
                        });
                     }
                  }

                  if (this.tick - this.stepStart >= st.delay()) {
                     if (st.shot() != null) {
                        MinecraftServer srv = mc.getSingleplayerServer();
                        srv.execute(
                           () -> each(
                                 srv,
                                 null,
                                 b -> LOG.info(
                                       "[AUTOTEST] state pos={} spin={} state={} ability={} rival={} sclash={} shot={}",
                                       new Object[]{
                                          b.position(), b.getSpin(), b.getState(), b.getAbility(), b.isRival(), b.getSpecialClashPartner(), st.shot()
                                       }
                                    )
                              )
                        );
                        if (st.shot().equals("END")) {
                           this.writer.shutdown();

                           try {
                              this.writer.awaitTermination(60L, TimeUnit.SECONDS);
                           } catch (InterruptedException var7) {
                           }

                           LOG.info("[AUTOTEST] finished, {} video frames", this.frameNo);

                           try {
                              File dir = new File(mc.gameDirectory, "frames");
                              dir.mkdirs();
                              Files.writeString(new File(dir, "list.txt").toPath(), this.frameList.toString());
                           } catch (IOException var6) {
                              LOG.error("[AUTOTEST] list write failed", var6);
                           }

                           mc.stop();
                           return;
                        }

                        Screenshot.grab(
                           mc.gameDirectory,
                           "ktest_" + String.format("%02d_", this.stepIndex) + st.shot() + ".png",
                           mc.getMainRenderTarget(),
                           msg -> LOG.info("[AUTOTEST] screenshot {}", st.shot())
                        );
                     }

                     this.stepIndex++;
                     this.stepStart = this.tick + 1;
                  }
               }
            }
         }
      }
   }

   static record Step(int delay, String shot, Consumer<MinecraftServer> action, Boolean gui, Consumer<Minecraft> clientAction) {
      Step(int delay, String shot, Consumer<MinecraftServer> action, Boolean gui) {
         this(delay, shot, action, gui, null);
      }
   }
}
