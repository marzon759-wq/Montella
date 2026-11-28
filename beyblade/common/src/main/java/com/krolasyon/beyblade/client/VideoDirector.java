package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.compat.VClient;
import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.parts.BeyPart;
import com.krolasyon.beyblade.parts.Loadout;
import com.krolasyon.beyblade.registry.ModEntities;
import com.krolasyon.beyblade.registry.ModItems;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

/**
 * Records a scripted gameplay video (-Dlibra.video=true): a list of shots, each with a camera path, server actions
 * and a caption. The game runs at a reduced tick rate so a software renderer still gets enough frames; frames are
 * timed by game time, so the video plays at real game speed (or slow motion where a shot asks for it).
 * Output in run/video/: frames, frames.csv (video time, game time, camera), sounds.csv, hum.csv, captions.csv.
 */
public final class VideoDirector {
   private static final Logger LOG = LogUtils.getLogger();
   private static final double CX = 0.5;
   private static final double CY = -59.6;
   private static final double CZ = -14.5;
   private static final String CLEAR = "kill @e[type=!player,type=!librabeyblade:stadium]";
   private static final double FPS = 30.0;
   private static final float BASE_RATE = Float.parseFloat(System.getProperty("libra.video.rate", "5"));

   private interface CamPath {
      double[] at(BeyRigView v, double t);
   }

   /** What a camera path may look at. */
   private static final class BeyRigView {
      List<BeybladeEntity> beys = new ArrayList<>();
      float partial;
   }

   private record Shot(int ticks, float slow, Consumer<MinecraftServer> start, CamPath cam, String caption, List<Object[]> subs) {
   }

   private final List<Shot> shots = new ArrayList<>();
   private int tick;
   private boolean worldRequested;
   private int readyTick = -1;
   private int shotIndex = -1;
   private double shotStartGt;
   private double lastGt = -1.0;
   private double videoTime;
   private double lastFrameVt = -1.0;
   private int frameNo;
   private boolean done;
   private final StringBuilder framesCsv = new StringBuilder("vt,gt,x,y,z\n");
   private final StringBuilder humCsv = new StringBuilder("vt,level\n");
   private final StringBuilder capCsv = new StringBuilder();
   private final List<String> sounds = Collections.synchronizedList(new ArrayList<>());
   private final ExecutorService writer = Executors.newFixedThreadPool(3);
   private File dir;

   public static void init() {
      LOG.info("[VIDEO] enabled, base tick rate {}", BASE_RATE);
      BeybladeEntity.rivalAbilities = false;
      MinecraftForge.EVENT_BUS.register(new VideoDirector());
   }

   // ------------------------------------------------------------------ camera helpers
   private static double[] look(double x, double y, double z, double tx, double ty, double tz) {
      double dx = tx - x;
      double dy = ty - y;
      double dz = tz - z;
      double h = Math.sqrt(dx * dx + dz * dz);
      return new double[]{x, y, z, Math.toDegrees(Math.atan2(-dx, dz)), -Math.toDegrees(Math.atan2(dy, h))};
   }

   private static CamPath orbit(double cx, double cy, double cz, double r, double h, double a0, double degPerTick) {
      return (v, t) -> {
         double a = Math.toRadians(a0 + degPerTick * t);
         return look(cx + Math.sin(a) * r, cy + h, cz - Math.cos(a) * r, cx, cy, cz);
      };
   }

   /** Orbit around the beys in play (their centre, smoothed). */
   private CamPath follow(double r, double h, double a0, double degPerTick) {
      double[] c = {CX, CY, CZ};
      return (v, t) -> {
         double sx = 0.0;
         double sz = 0.0;
         double sy = 0.0;
         int n = 0;
         for (BeybladeEntity b : v.beys) {
            if (b.getState() != BeybladeEntity.STATE_RECALL && b.distanceToSqr(CX, CY, CZ) < 196.0) {
               sx += b.getX();
               sy += b.getY();
               sz += b.getZ();
               n++;
            }
         }
         if (n > 0) {
            c[0] += (sx / n - c[0]) * 0.04;
            c[1] += (Math.min(sy / n, CY + 3.0) - c[1]) * 0.04;
            c[2] += (sz / n - c[2]) * 0.04;
         }
         double a = Math.toRadians(a0 + degPerTick * t);
         return look(c[0] + Math.sin(a) * r, c[1] + h, c[2] - Math.cos(a) * r, c[0], c[1] + 0.25, c[2]);
      };
   }

   /** Orbit around the first bey of this type. */
   private static CamPath track(java.util.function.Supplier<?> type, double r, double h, double a0, double degPerTick) {
      double[] c = {CX, CY, CZ};
      boolean[] init = {false};
      return (v, t) -> {
         for (BeybladeEntity b : v.beys) {
            if (b.getType() == type.get()) {
               double bx = b.getX();
               double by = Math.min(b.getY(), CY + 6.0);
               double bz = b.getZ();
               if (!init[0]) {
                  c[0] = bx;
                  c[1] = by;
                  c[2] = bz;
                  init[0] = true;
               }
               c[0] += (bx - c[0]) * 0.08;
               c[1] += (by - c[1]) * 0.08;
               c[2] += (bz - c[2]) * 0.08;
               break;
            }
         }
         double a = Math.toRadians(a0 + degPerTick * t);
         return look(c[0] + Math.sin(a) * r, c[1] + h, c[2] - Math.cos(a) * r, c[0], c[1] + 0.3, c[2]);
      };
   }

   private static ItemStack custom(Item bey, String... parts) {
      ItemStack st = new ItemStack(bey);
      for (String p : parts) {
         BeyPart part = BeyPart.get(p);
         Loadout.write(st, part.type(), part, 3);
      }
      return st;
   }

   private static void place(MinecraftServer s, EntityType<? extends BeybladeEntity> type, ItemStack st, double x, double z, Vec3 vel, boolean frozen) {
      BeybladeEntity b = BeybladeEntity.launch(type, s.overworld(), null, st, new Vec3(x, CY + 0.3, z), vel, 1.0F);
      b.setFrozen(frozen);
   }

   private static Consumer<MinecraftServer> cmds(String... c) {
      return s -> {
         for (String x : c) {
            s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), x);
         }
      };
   }

   private static Consumer<MinecraftServer> then(Consumer<MinecraftServer> a, Consumer<MinecraftServer> b) {
      return s -> {
         a.accept(s);
         b.accept(s);
      };
   }

   /** Rival beys may use their special moves on their own only in shots that ask for it. */
   private boolean fight;

   private void shot(int ticks, float slow, Consumer<MinecraftServer> start0, CamPath cam, String caption, Object[]... subs) {
      boolean f = this.fight;
      this.fight = false;
      Consumer<MinecraftServer> start = s -> {
         BeybladeEntity.rivalAbilities = f;
         start0.accept(s);
      };
      List<Object[]> l = new ArrayList<>();
      Collections.addAll(l, subs);
      this.shots.add(new Shot(ticks, slow, start, cam, caption, l));
   }

   private static Object[] sub(int atTick, Consumer<MinecraftServer> action) {
      return new Object[]{atTick, action};
   }

   // ------------------------------------------------------------------ the script
   private VideoDirector() {
      // 1. intro: the stadium
      this.fight = true;
      this.shot(300, 1.0F, then(cmds(CLEAR), s -> {
         place(s, ModEntities.PEGASUS.get(), new ItemStack(ModItems.PEGASUS.get()), CX - 3.0, CZ, new Vec3(0.0, 0.0, 0.35), false);
         place(s, ModEntities.LDRAGO.get(), new ItemStack(ModItems.LDRAGO.get()), CX + 3.0, CZ, new Vec3(0.0, 0.0, -0.35), false);
      }), orbit(CX, CY, CZ, 15.0, 7.5, 200.0, 0.12), "BEYBLADE METAL FIGHT 2.0|GeckoLib animasyonları · Forge 1.20.1 & 1.21.1");
      // 2. line up
      String[] kinds = {"leone", "eagle", "ldrago", "pegasus", "beffal", "variares", "libra", "tempo"};
      String[] names = {"Rock Leone 145WB", "Earth Eagle 145WD", "L-Drago Destruction F:S", "Storm Pegasus 105RF", "Evil Befall UW145EWD",
         "Variares D:D", "Flame Libra T125ES", "Twisted Tempo 145WD"};
      List<String> lineup = new ArrayList<>();
      for (int i = 0; i < kinds.length; i++) {
         lineup.add(String.format(Locale.ROOT, "summon librabeyblade:%s %.2f %.2f %.2f {Frozen:1b}", kinds[i], -6.2 + 1.8 * i, CY + 0.3, CZ));
      }
      List<String> lc = new ArrayList<>();
      lc.add(CLEAR);
      lc.addAll(lineup);
      this.shot(440, 1.0F, cmds(lc.toArray(new String[0])), (v, t) -> {
         double x = -7.4 + 14.8 * Math.min(1.0, t / 440.0);
         return look(x, CY + 1.0, CZ + 2.3, x + 0.2, CY + 0.2, CZ);
      }, "Kadro: 8 Metal Fight beyi");
      for (int i = 0; i < kinds.length; i++) {
         this.capAt(-1, 20 + i * 52, names[i]);
      }
      // 3. parts: the new tips and tracks
      this.shot(420, 1.0F, then(cmds(CLEAR), s -> {
         place(s, ModEntities.PEGASUS.get(), custom(ModItems.PEGASUS.get(), "track_df145", "tip_rs"), CX - 1.3, CZ, Vec3.ZERO, true);
         place(s, ModEntities.LIBRA.get(), custom(ModItems.LIBRA.get(), "track_ad145", "tip_bd"), CX, CZ, Vec3.ZERO, true);
         place(s, ModEntities.LEONE.get(), custom(ModItems.LEONE.get(), "track_lw160", "tip_rb"), CX + 1.3, CZ, Vec3.ZERO, true);
      }), (v, t) -> {
         double a = Math.toRadians(-35.0 + 70.0 * t / 420.0);
         return look(CX + Math.sin(a) * 3.0, CY + 0.35, CZ + Math.cos(a) * 3.0, CX, CY + 0.25, CZ);
      }, "Yeni parçalar: 14 Performance Tip · 9 Spin Track");
      this.capAt(-1, 30, "DF145 + RS (Lastik Sivri)");
      this.capAt(-1, 160, "AD145 + B:D (Rulman Sürüş) — uç gövdeden yavaş döner");
      this.capAt(-1, 290, "LW160 + RB (Lastik Top)");
      // 4. first battle
      this.fight = true;
      this.shot(800, 1.0F, then(cmds(CLEAR), s -> {
         place(s, ModEntities.PEGASUS.get(), new ItemStack(ModItems.PEGASUS.get()), CX - 6.0, CZ - 2.0, new Vec3(0.45, 0.25, 0.1), false);
         place(s, ModEntities.LDRAGO.get(), new ItemStack(ModItems.LDRAGO.get()), CX + 6.0, CZ + 2.0, new Vec3(-0.45, 0.25, -0.1), false);
      }), this.follow(5.0, 2.6, 160.0, 0.18), "Storm Pegasus (RF lastik uç) vs L-Drago Destruction (sola dönüş)");
      this.capAt(-1, 260, "Lastik uç zemini kavrar: çiçek deseninde hızlı saldırı");
      this.capAt(-1, 520, "Her vuruş: GeckoLib geri tepme + parça titremesi");
      // 5. special moves
      Object[][] sp = {
         {"ldrago", ModEntities.LDRAGO, 3}, {"pegasus", ModEntities.PEGASUS, 3}, {"leone", ModEntities.LEONE, 5}, {"eagle", ModEntities.EAGLE, 3},
         {"libra", ModEntities.LIBRA, 3}, {"variares", ModEntities.VARIARES, 5}, {"beffal", ModEntities.BEFFAL, 3}, {"tempo", ModEntities.TEMPO, 1}
      };
      for (int i = 0; i < sp.length; i++) {
         String kind = (String)sp[i][0];
         java.util.function.Supplier<?> type = (java.util.function.Supplier<?>)sp[i][1];
         int ab = (Integer)sp[i][2];
         String abilityKey = "ability.librabeyblade." + (kind.equals("libra") ? "" + ab : kind + "." + ab);
         this.shot(250, 1.0F, cmds(CLEAR,
               String.format(Locale.ROOT, "summon librabeyblade:%s %.2f %.2f %.2f {Tags:[\"a\"]}", kind, CX - 1.5, CY + 0.3, CZ + 1.0),
               String.format(Locale.ROOT, "summon librabeyblade:common %.2f %.2f %.2f {Variant:%d}", CX + 2.5, CY + 0.3, CZ - 1.5, i),
               String.format(Locale.ROOT, "summon librabeyblade:common %.2f %.2f %.2f {Variant:%d}", CX - 3.5, CY + 0.3, CZ - 2.5, i + 3)),
            track(type, 4.2, 2.2, 140.0 + 45.0 * i, 0.22), "@" + abilityKey,
            sub(30, s -> each(s, b -> b.debugForceAbility(ab))));
      }
      // 6. rubber vs sharp
      this.shot(560, 1.0F, then(cmds(CLEAR), s -> {
         place(s, ModEntities.PEGASUS.get(), custom(ModItems.PEGASUS.get(), "track_105", "tip_rf"), CX - 5.0, CZ, new Vec3(0.3, 0.2, 0.25), false);
         place(s, ModEntities.LIBRA.get(), custom(ModItems.LIBRA.get(), "track_145", "tip_es"), CX + 0.5, CZ + 0.3, new Vec3(0.0, 0.2, 0.0), false);
      }), orbit(CX, CY, CZ, 8.5, 5.0, 30.0, 0.1), "Lastik düz (RF) saldırır · Sonsuz sivri (ES) merkezde dayanır");
      this.capAt(-1, 300, "Lastik yavaşladıkça aşınır, sivri uç dönüşünü korur");
      // 7. burst finish (slow motion)
      this.shot(50, 1.0F, cmds(CLEAR, String.format(Locale.ROOT, "summon librabeyblade:eagle %.2f %.2f %.2f", CX, CY + 0.3, CZ)),
         orbit(CX, CY, CZ, 2.0, 0.9, 20.0, 0.4), "BURST FINISH!");
      this.shot(70, 0.3F, s -> each(s, BeybladeEntity::debugBurst), orbit(CX, CY, CZ, 2.0, 0.9, 40.0, 0.4), "BURST FINISH!|Face Bolt · Energy Ring · Fusion Wheel · Spin Track · Performance Tip");
      // 8. sleep out
      this.shot(50, 1.0F, cmds(CLEAR, String.format(Locale.ROOT, "summon librabeyblade:libra %.2f %.2f %.2f", CX, CY + 0.3, CZ)),
         orbit(CX, CY, CZ, 2.2, 0.7, 100.0, 0.3), "Sleep-out");
      this.shot(80, 0.5F, s -> each(s, BeybladeEntity::debugSleepOut), orbit(CX, CY, CZ, 2.2, 0.7, 115.0, 0.3), "Sleep-out: dengesini kaybeder, yan yatar");
      // 9. battle royale
      this.fight = true;
      this.shot(1250, 1.0F, then(cmds(CLEAR), s -> {
         place(s, ModEntities.PEGASUS.get(), new ItemStack(ModItems.PEGASUS.get()), CX - 6.0, CZ, new Vec3(0.35, 0.2, 0.2), false);
         place(s, ModEntities.LDRAGO.get(), new ItemStack(ModItems.LDRAGO.get()), CX + 6.0, CZ, new Vec3(-0.35, 0.2, -0.2), false);
         place(s, ModEntities.LEONE.get(), custom(ModItems.LEONE.get(), "track_ad145", "tip_rdf"), CX, CZ - 6.0, new Vec3(0.2, 0.2, 0.35), false);
         place(s, ModEntities.VARIARES.get(), custom(ModItems.VARIARES.get(), "track_c145", "tip_lrf"), CX, CZ + 6.0, new Vec3(-0.2, 0.2, -0.35), false);
      }), this.follow(7.0, 4.0, 0.0, 0.12), "Battle Royale: Pegasus · L-Drago · Leone (RDF) · Variares (LRF)");
      this.capAt(-1, 400, "");
      // 10. outro
      this.shot(240, 1.0F, s -> {
      }, orbit(CX, CY, CZ, 16.0, 9.0, 120.0, 0.12), "Beyblade Metal Fight 2.0|GeckoLib · 31 uç · 24 track · 1.20.1 & 1.21.1");
   }

   /** extra caption inside shot {@code shot} (index, -1 = last added) starting at a tick of that shot */
   private void capAt(int shot, int atTick, String text) {
      Shot s = this.shots.get(shot < 0 ? this.shots.size() - 1 : shot);
      s.subs().add(new Object[]{atTick, text});
   }

   private static void each(MinecraftServer s, Consumer<BeybladeEntity> c) {
      for (BeybladeEntity e : s.overworld().getEntitiesOfClass(BeybladeEntity.class, new AABB(-40.0, -80.0, -60.0, 40.0, 20.0, 40.0))) {
         if (e.getTags().contains("a") || e.getType() == ModEntities.EAGLE.get() || e.getType() == ModEntities.LIBRA.get()) {
            c.accept(e);
         }
      }
   }

   // ------------------------------------------------------------------ events
   @SubscribeEvent
   public void onSound(PlayLevelSoundEvent e) {
      if (e.getLevel().isClientSide() || this.shotIndex < 0 || this.done) {
         return;
      }
      Vec3 p = null;
      if (e instanceof PlayLevelSoundEvent.AtPosition ap) {
         p = ap.getPosition();
      } else if (e instanceof PlayLevelSoundEvent.AtEntity ae) {
         p = ae.getEntity().position();
      }
      if (p != null && e.getSound() != null) {
         String id = e.getSound().value().getLocation().toString();
         this.sounds.add(String.format(Locale.ROOT, "%d,%s,%.2f,%.2f,%.2f,%.3f,%.3f", e.getLevel().getGameTime(), id, p.x, p.y, p.z, e.getNewVolume(), e.getNewPitch()));
      }
   }

   @SubscribeEvent
   public void onClientTick(TickEvent.ClientTickEvent event) {
      if (event.phase != TickEvent.Phase.END) {
         return;
      }
      Minecraft mc = Minecraft.getInstance();
      this.tick++;
      mc.options.pauseOnLostFocus = false;
      if (mc.getOverlay() != null) {
         return;
      }
      if (!this.worldRequested && this.tick > 40 && mc.level == null) {
         this.worldRequested = true;
         LevelSettings settings = new LevelSettings("lvideo", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
         VClient.createFlatTestWorld(mc, "lvideo", settings, new WorldOptions(7L, false, false));
         return;
      }
      if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) {
         return;
      }
      if (mc.screen != null) {
         mc.setScreen(null);
      }
      mc.options.hideGui = true;
      if (this.readyTick < 0) {
         this.readyTick = this.tick;
         cmds("gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false", "time set 6000", "weather clear",
               "difficulty peaceful", "gamemode spectator @a", "kill @e[type=!player]", "summon librabeyblade:stadium 0.5 -60 -14.5",
               "tp @a 0.5 -50 0").accept(mc.getSingleplayerServer());
      }
   }

   @SubscribeEvent
   public void onRender(TickEvent.RenderTickEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null || mc.player == null || this.readyTick < 0 || this.tick - this.readyTick < 120 || this.done) {
         return;
      }
      float partial = VClient.partialTick();
      double gt = (double)mc.level.getGameTime() + (double)partial;
      if (event.phase == TickEvent.Phase.START) {
         if (this.shotIndex < 0 || gt - this.shotStartGt >= (double)this.shots.get(this.shotIndex).ticks()) {
            this.nextShot(mc, gt);
            if (this.done) {
               return;
            }
         }
         Shot s = this.shots.get(this.shotIndex);
         double st = gt - this.shotStartGt;
         // sub actions / captions of this shot
         for (Object[] sub : s.subs()) {
            int at = (Integer)sub[0];
            if (sub.length == 2 && st >= (double)at && !(sub[1] instanceof Boolean)) {
               if (sub[1] instanceof String text) {
                  this.caption(text);
               } else {
                  @SuppressWarnings("unchecked")
                  Consumer<MinecraftServer> a = (Consumer<MinecraftServer>)sub[1];
                  MinecraftServer srv = mc.getSingleplayerServer();
                  srv.execute(() -> a.accept(srv));
               }
               sub[1] = Boolean.TRUE;
            }
         }
         BeyRigView v = new BeyRigView();
         v.partial = partial;
         for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof BeybladeEntity b) {
               v.beys.add(b);
            }
         }
         double[] c = s.cam().at(v, st);
         Player p = mc.player;
         double eye = p.getEyeHeight();
         p.setPos(c[0], c[1] - eye, c[2]);
         p.xo = p.xOld = c[0];
         p.yo = p.yOld = c[1] - eye;
         p.zo = p.zOld = c[2];
         p.setYRot((float)c[3]);
         p.setXRot((float)c[4]);
         p.yRotO = (float)c[3];
         p.xRotO = (float)c[4];
         p.setYHeadRot((float)c[3]);
         p.yHeadRotO = (float)c[3];
         p.setDeltaMovement(Vec3.ZERO);
         // advance video time by game time
         if (this.lastGt >= 0.0 && gt > this.lastGt) {
            this.videoTime += (gt - this.lastGt) / 20.0 / (double)s.slow();
         }
         this.lastGt = gt;
         // spin hum level: spinning beys near the camera
         double hum = 0.0;
         for (BeybladeEntity b : v.beys) {
            if (b.getState() == BeybladeEntity.STATE_BATTLE) {
               double d = Math.sqrt(b.distanceToSqr(c[0], c[1], c[2]));
               hum += (double)b.spinRatio() * Math.max(0.0, 1.0 - d / 18.0);
            }
         }
         if (this.videoTime - this.lastFrameVt >= 1.0 / FPS - 1.0E-4) {
            this.humCsv.append(String.format(Locale.ROOT, "%.4f,%.3f%n", this.videoTime, hum));
         }
      } else if (event.phase == TickEvent.Phase.END && this.shotIndex >= 0) {
         // capture at most FPS frames per video second
         if (this.lastFrameVt < 0.0 || this.videoTime - this.lastFrameVt >= 1.0 / FPS - 1.0E-4) {
            this.lastFrameVt = this.videoTime;
            this.capture(mc, gt);
         }
      }
   }

   private void nextShot(Minecraft mc, double gt) {
      this.shotIndex++;
      if (this.shotIndex >= this.shots.size()) {
         this.finish(mc);
         return;
      }
      Shot s = this.shots.get(this.shotIndex);
      this.shotStartGt = gt;
      MinecraftServer srv = mc.getSingleplayerServer();
      float rate = BASE_RATE * s.slow();
      srv.execute(() -> {
         cmds(String.format(Locale.ROOT, "tick rate %.2f", rate)).accept(srv);
         s.start().accept(srv);
      });
      this.caption(s.caption());
      LOG.info("[VIDEO] shot {} / {} at video {}s, {} frames", this.shotIndex + 1, this.shots.size(), String.format(Locale.ROOT, "%.1f", this.videoTime), this.frameNo);
   }

   private void caption(String text) {
      if (text.startsWith("@")) {
         text = I18n.get(text.substring(1));
      }
      this.capCsv.append(String.format(Locale.ROOT, "%.3f\t%s%n", this.videoTime, text));
   }

   private File dir(Minecraft mc) {
      if (this.dir == null) {
         this.dir = new File(mc.gameDirectory, "video");
         new File(this.dir, "frames").mkdirs();
      }
      return this.dir;
   }

   private void capture(Minecraft mc, double gt) {
      NativeImage img = Screenshot.takeScreenshot(mc.getMainRenderTarget());
      String name = String.format("f_%06d.jpg", this.frameNo++);
      Player p = mc.player;
      this.framesCsv.append(String.format(Locale.ROOT, "%.4f,%.3f,%.2f,%.2f,%.2f%n", this.videoTime, gt, p.getX(), p.getEyeY(), p.getZ()));
      File out = new File(new File(this.dir(mc), "frames"), name);
      this.writer.submit(() -> {
         try {
            BufferedImage bi = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < img.getHeight(); y++) {
               for (int x = 0; x < img.getWidth(); x++) {
                  int abgr = img.getPixelRGBA(x, y);
                  bi.setRGB(x, y, (abgr & 0xFF) << 16 | abgr & 0xFF00 | abgr >> 16 & 0xFF);
               }
            }
            ImageWriter w = ImageIO.getImageWritersByFormatName("jpg").next();
            ImageWriteParam prm = w.getDefaultWriteParam();
            prm.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            prm.setCompressionQuality(0.93F);
            try (ImageOutputStream os = ImageIO.createImageOutputStream(out)) {
               w.setOutput(os);
               w.write(null, new IIOImage(bi, null, null), prm);
            }
            w.dispose();
         } catch (Exception ex) {
            LOG.error("[VIDEO] frame write failed", ex);
         } finally {
            img.close();
         }
      });
   }

   private void finish(Minecraft mc) {
      this.done = true;
      try {
         File d = this.dir(mc);
         this.writer.shutdown();
         this.writer.awaitTermination(5L, TimeUnit.MINUTES);
         Files.writeString(new File(d, "frames.csv").toPath(), this.framesCsv.toString());
         Files.writeString(new File(d, "hum.csv").toPath(), this.humCsv.toString());
         Files.writeString(new File(d, "captions.tsv").toPath(), this.capCsv.toString() + String.format(Locale.ROOT, "%.3f\t%n", this.videoTime));
         synchronized (this.sounds) {
            Files.writeString(new File(d, "sounds.csv").toPath(), "gt,id,x,y,z,vol,pitch\n" + String.join("\n", this.sounds) + "\n");
         }
         LOG.info("[VIDEO] done: {} frames, {}s", this.frameNo, String.format(Locale.ROOT, "%.1f", this.videoTime));
      } catch (Exception ex) {
         LOG.error("[VIDEO] finish failed", ex);
      }
      mc.stop();
   }
}
