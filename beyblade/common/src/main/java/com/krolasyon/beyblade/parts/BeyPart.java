package com.krolasyon.beyblade.parts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BeyPart(
   String id,
   PartType type,
   int attack,
   int defense,
   int stamina,
   int speed,
   float weight,
   int rarity,
   String mesh,
   int height,
   BeyPart.Tip tip,
   BeyPart.Trait trait,
   int color,
   int color2
) {
   private static final Map<String, BeyPart> ALL = new LinkedHashMap<>();
   private static final Map<PartType, List<BeyPart>> BY_TYPE = new LinkedHashMap<>();

   public String nameKey() {
      return "part.librabeyblade." + this.id;
   }

   public String itemId() {
      return "part_" + this.id;
   }

   private static BeyPart add(BeyPart p) {
      ALL.put(p.id, p);
      BY_TYPE.computeIfAbsent(p.type, k -> new ArrayList<>()).add(p);
      return p;
   }

   private static void bolt(String id, int atk, int def, int sta, int spd, int rarity, int col, int col2) {
      add(new BeyPart(id, PartType.BOLT, atk, def, sta, spd, 1.0F, rarity, "", 0, BeyPart.Tip.NONE, BeyPart.Trait.SPIRIT, col, col2));
   }

   private static void ring(String id, int atk, int def, int sta, int spd, float w, int rarity, int col, int col2) {
      add(new BeyPart(id, PartType.RING, atk, def, sta, spd, w, rarity, "", 0, BeyPart.Tip.NONE, BeyPart.Trait.NONE, col, col2));
   }

   private static void wheel(String id, String mesh, int atk, int def, int sta, int spd, float w, int rarity, int col, int col2) {
      add(new BeyPart(id, PartType.WHEEL, atk, def, sta, spd, w, rarity, mesh, 0, BeyPart.Tip.NONE, BeyPart.Trait.NONE, col, col2));
   }

   private static void track(String id, int h, BeyPart.Trait trait, int atk, int def, int sta, int spd, float w, int rarity, int col, int col2) {
      add(new BeyPart(id, PartType.TRACK, atk, def, sta, spd, w, rarity, "", h, BeyPart.Tip.NONE, trait, col, col2));
   }

   private static void tip(String id, BeyPart.Tip tip, int atk, int def, int sta, int spd, float w, int rarity, int col, int col2) {
      add(
         new BeyPart(
            id,
            PartType.TIP,
            atk,
            def,
            sta,
            spd,
            w,
            rarity,
            "",
            0,
            tip,
            tip != BeyPart.Tip.FINAL_DRIVE && tip != BeyPart.Tip.FINAL_SURVIVE ? BeyPart.Trait.NONE : BeyPart.Trait.FOUR_D,
            col,
            col2
         )
      );
   }

   public static BeyPart get(String id) {
      return ALL.get(id);
   }

   public static Collection<BeyPart> all() {
      return Collections.unmodifiableCollection(ALL.values());
   }

   public static List<BeyPart> of(PartType t) {
      return BY_TYPE.getOrDefault(t, List.of());
   }

   public static String[] stock(String kind) {
      if (kind.startsWith("common_")) {
         int v = Integer.parseInt(kind.substring(7));
         String[] ids = new String[]{
            "iron_bull",
            "frost_wolf",
            "jade_turtle",
            "thunder_viper",
            "shadow_bat",
            "sky_falcon",
            "crimson_scorpion",
            "golden_phoenix",
            "ocean_shark",
            "storm_fox"
         };
         String[] tips = new String[]{"tip_f", "tip_sf", "tip_d", "tip_f", "tip_s", "tip_sf", "tip_f", "tip_s", "tip_d", "tip_sf"};
         return new String[]{"bolt_standard", "ring_standard", "wheel_" + ids[Math.floorMod(v, 10)], "track_100", tips[Math.floorMod(v, 10)]};
      } else {
         return switch (kind) {
            case "ldrago" -> new String[]{"bolt_ldrago", "ring_ldrago", "wheel_ldrago", "track_145", "tip_fs"};
            case "leone" -> new String[]{"bolt_leone", "ring_leone", "wheel_leone", "track_145", "tip_wb"};
            case "eagle" -> new String[]{"bolt_eagle", "ring_eagle", "wheel_eagle", "track_145", "tip_wd"};
            case "pegasus" -> new String[]{"bolt_pegasus", "ring_pegasus", "wheel_pegasus", "track_105", "tip_rf"};
            case "tempo" -> new String[]{"bolt_tempo", "ring_tempo", "wheel_tempo", "track_145", "tip_wd"};
            case "variares" -> new String[]{"bolt_variares", "ring_variares", "wheel_variares", "track_145", "tip_wb"};
            case "beffal" -> new String[]{"bolt_beffal", "ring_beffal", "wheel_beffal", "track_uw145", "tip_ewd"};
            default -> new String[]{"bolt_libra", "ring_libra", "wheel_libra", "track_t125", "tip_es"};
         };
      }
   }

   static {
      bolt("bolt_libra", 2, 3, 6, 2, 2, 5950282, 15790320);
      bolt("bolt_ldrago", 7, 1, 2, 3, 2, 13114922, 2763328);
      bolt("bolt_leone", 1, 7, 3, 2, 2, 3121738, 15263976);
      bolt("bolt_eagle", 2, 4, 6, 1, 2, 9060560, 15786239);
      bolt("bolt_pegasus", 6, 2, 2, 5, 2, 3043040, 16777215);
      bolt("bolt_tempo", 3, 3, 4, 3, 2, 13687016, 3158080);
      bolt("bolt_variares", 7, 3, 1, 3, 2, 14163486, 16764992);
      bolt("bolt_beffal", 3, 4, 4, 3, 2, 12599472, 16761064);
      bolt("bolt_bigbang", 7, 2, 3, 5, 3, 4235488, 16732272);
      bolt("bolt_phoenix", 3, 5, 6, 2, 3, 6961824, 15259903);
      bolt("bolt_inferno", 6, 3, 3, 4, 3, 15773728, 6295584);
      bolt("bolt_standard", 2, 2, 2, 2, 0, 11580604, 4210760);
      ring("ring_libra", 3, 4, 8, 3, 3.0F, 2, 9101146, 3045930);
      ring("ring_ldrago", 9, 2, 3, 4, 3.2F, 2, 14688304, 6293520);
      ring("ring_leone", 2, 9, 4, 2, 3.4F, 2, 3853146, 1071648);
      ring("ring_eagle", 3, 5, 8, 2, 3.0F, 2, 10113248, 4198538);
      ring("ring_pegasus", 8, 2, 3, 7, 2.8F, 2, 3832560, 666240);
      ring("ring_tempo", 4, 4, 5, 4, 3.0F, 2, 6318200, 14739696);
      ring("ring_variares", 9, 4, 2, 4, 3.2F, 2, 13639712, 3147784);
      ring("ring_beffal", 4, 5, 5, 4, 3.1F, 2, 15755952, 7344208);
      ring("ring_bigbang", 9, 3, 4, 6, 3.3F, 3, 2113728, 15745152);
      ring("ring_phoenix", 3, 6, 9, 2, 3.2F, 3, 9059040, 3149930);
      ring("ring_inferno", 7, 4, 4, 5, 3.2F, 3, 8394800, 15773728);
      ring("ring_standard", 3, 3, 3, 3, 3.0F, 0, 13160672, 6975616);
      wheel("wheel_libra", "libra", 8, 12, 22, 8, 33.0F, 2, 12107464, 5950282);
      wheel("wheel_ldrago", "ldrago", 24, 8, 8, 12, 37.0F, 2, 9476264, 13114922);
      wheel("wheel_leone", "leone", 6, 26, 12, 6, 36.0F, 2, 11055288, 3121738);
      wheel("wheel_eagle", "eagle", 8, 16, 22, 4, 40.0F, 2, 11579584, 9060560);
      wheel("wheel_pegasus", "pegasus", 24, 6, 8, 16, 32.0F, 2, 13159640, 3043040);
      wheel("wheel_tempo", "tempo", 14, 14, 14, 10, 36.0F, 2, 12633292, 13687016);
      wheel("wheel_variares", "variares", 26, 12, 6, 10, 37.0F, 2, 12106948, 14163486);
      wheel("wheel_beffal", "beffal", 14, 16, 16, 8, 39.0F, 2, 13159636, 15755952);
      wheel("wheel_bigbang", "bigbang", 28, 12, 10, 14, 42.0F, 3, 13685980, 2113728);
      wheel("wheel_phoenix", "phoenix", 8, 20, 26, 6, 41.0F, 3, 12633292, 9059040);
      wheel("wheel_inferno", "inferno", 22, 14, 14, 12, 40.0F, 3, 13158608, 15773728);
      int[][] commonStats = new int[][]{
         {22, 8, 6, 12},
         {12, 12, 12, 12},
         {6, 22, 12, 6},
         {20, 6, 8, 14},
         {6, 10, 22, 6},
         {12, 10, 12, 14},
         {22, 8, 8, 12},
         {8, 10, 22, 8},
         {8, 22, 10, 8},
         {14, 12, 10, 14}
      };
      String[] commonIds = new String[]{
         "iron_bull",
         "frost_wolf",
         "jade_turtle",
         "thunder_viper",
         "shadow_bat",
         "sky_falcon",
         "crimson_scorpion",
         "golden_phoenix",
         "ocean_shark",
         "storm_fox"
      };
      int[] commonCol = new int[]{14168616, 7260415, 3121738, 16110110, 7024565, 4099824, 10489898, 16756768, 1724320, 16742938};

      for (int i = 0; i < commonIds.length; i++) {
         int[] s = commonStats[i];
         wheel("wheel_" + commonIds[i], "common_" + i, s[0] - 4, s[1] - 4, s[2] - 4, s[3] - 2, 30.0F, 0, 11580604, commonCol[i]);
      }

      track("track_85", 85, BeyPart.Trait.NONE, 3, 6, 4, 6, 1.4F, 0, 5264476, 9080984);
      track("track_90", 90, BeyPart.Trait.NONE, 3, 6, 4, 5, 1.5F, 0, 5264476, 9080984);
      track("track_100", 100, BeyPart.Trait.NONE, 4, 5, 5, 5, 1.6F, 0, 5264476, 9080984);
      track("track_105", 105, BeyPart.Trait.NONE, 5, 4, 5, 5, 1.6F, 0, 4212304, 8425632);
      track("track_125", 125, BeyPart.Trait.NONE, 5, 4, 6, 4, 1.8F, 0, 4212304, 8425632);
      track("track_145", 145, BeyPart.Trait.NONE, 5, 4, 6, 4, 2.0F, 0, 4212304, 8425632);
      track("track_230", 230, BeyPart.Trait.GRAVITY, 9, 1, 4, 3, 2.8F, 1, 3685444, 10528952);
      track("track_bd145", 145, BeyPart.Trait.BOOST_DISK, 3, 10, 6, 2, 4.0F, 1, 3159100, 12633292);
      track("track_c145", 145, BeyPart.Trait.CLAW, 9, 3, 4, 4, 2.4F, 1, 3159100, 14212324);
      track("track_w105", 105, BeyPart.Trait.WING, 4, 3, 9, 5, 2.0F, 1, 2779850, 13689087);
      track("track_uw145", 145, BeyPart.Trait.UPPER_WING, 8, 4, 5, 4, 2.4F, 1, 12599440, 15773912);
      track("track_t125", 125, BeyPart.Trait.TORNADO, 3, 4, 10, 4, 1.9F, 1, 3828282, 10543264);
      track("track_d125", 125, BeyPart.Trait.DEFENSE_RING, 2, 10, 6, 3, 2.3F, 1, 3159100, 15790320);
      track("track_r145", 145, BeyPart.Trait.RUBBER_RING, 4, 9, 4, 3, 2.2F, 1, 2105380, 13639712);
      track("track_th170", 170, BeyPart.Trait.TWIN_HEIGHT, 7, 5, 5, 3, 2.6F, 2, 3159100, 15777856);
      track("track_120", 120, BeyPart.Trait.NONE, 4, 5, 5, 4, 1.7F, 0, 5264476, 9080984);
      track("track_130", 130, BeyPart.Trait.NONE, 5, 4, 6, 4, 1.8F, 0, 4212304, 8425632);
      track("track_160", 160, BeyPart.Trait.NONE, 6, 4, 5, 3, 2.1F, 0, 4212304, 8425632);
      track("track_ch120", 120, BeyPart.Trait.TWIN_HEIGHT, 6, 5, 5, 4, 2.2F, 1, 3159100, 16764992);
      track("track_ed145", 145, BeyPart.Trait.DEFENSE_RING, 2, 9, 9, 2, 2.6F, 2, 3159100, 13689087);
      track("track_df145", 145, BeyPart.Trait.WING, 3, 5, 10, 3, 2.2F, 1, 2779850, 15790320);
      track("track_ad145", 145, BeyPart.Trait.DEFENSE_RING, 3, 11, 5, 2, 3.0F, 2, 6316128, 16757504);
      track("track_sw145", 145, BeyPart.Trait.CLAW, 8, 6, 4, 4, 2.5F, 1, 3159100, 16737843);
      track("track_lw160", 160, BeyPart.Trait.WING, 5, 4, 8, 4, 2.3F, 1, 10040217, 15790320);
      tip("tip_f", BeyPart.Tip.FLAT, 8, 3, 2, 10, 1.0F, 0, 15263980, 6316136);
      tip("tip_rf", BeyPart.Tip.RUBBER_FLAT, 10, 3, 1, 13, 1.1F, 1, 2763312, 13639712);
      tip("tip_r2f", BeyPart.Tip.RUBBER_FLAT, 9, 4, 2, 12, 1.2F, 1, 3159100, 4243680);
      tip("tip_xf", BeyPart.Tip.EXTREME_FLAT, 11, 2, 1, 15, 1.0F, 2, 15790320, 2121952);
      tip("tip_sf", BeyPart.Tip.SEMI_FLAT, 6, 5, 5, 7, 1.0F, 0, 14737636, 7368824);
      tip("tip_s", BeyPart.Tip.SHARP, 1, 4, 12, 2, 0.9F, 0, 15263980, 6316136);
      tip("tip_es", BeyPart.Tip.ETERNAL_SHARP, 1, 4, 15, 2, 1.2F, 1, 6340704, 14745568);
      tip("tip_cs", BeyPart.Tip.COAT_SHARP, 3, 5, 10, 4, 1.0F, 1, 2763312, 15790320);
      tip("tip_d", BeyPart.Tip.DEFENSE, 2, 10, 8, 2, 1.1F, 0, 15263980, 6316136);
      tip("tip_wd", BeyPart.Tip.WIDE_DEFENSE, 2, 11, 10, 2, 1.3F, 0, 15790324, 7368824);
      tip("tip_ewd", BeyPart.Tip.ETERNAL_WIDE_DEFENSE, 2, 11, 12, 2, 1.5F, 1, 10502320, 15778032);
      tip("tip_b", BeyPart.Tip.BALL, 3, 9, 7, 4, 1.1F, 0, 15263980, 6316136);
      tip("tip_wb", BeyPart.Tip.WIDE_BALL, 3, 12, 6, 3, 1.3F, 0, 2763312, 7368824);
      tip("tip_mf", BeyPart.Tip.METAL_FLAT, 10, 4, 3, 9, 2.4F, 1, 13159636, 7370880);
      tip("tip_rsf", BeyPart.Tip.RUBBER_SEMI_FLAT, 8, 6, 3, 9, 1.2F, 1, 3195088, 1065040);
      tip("tip_fd", BeyPart.Tip.FINAL_DRIVE, 8, 7, 8, 9, 2.6F, 2, 3836640, 14696544);
      tip("tip_fs", BeyPart.Tip.FINAL_SURVIVE, 4, 7, 11, 5, 2.4F, 2, 2763332, 12632272);
      tip("tip_hf", BeyPart.Tip.HOLE_FLAT, 7, 3, 3, 9, 1.0F, 0, 15263980, 4210752);
      tip("tip_wf", BeyPart.Tip.WIDE_FLAT, 8, 4, 2, 9, 1.1F, 0, 14737636, 3170266);
      tip("tip_wsf", BeyPart.Tip.WIDE_SEMI_FLAT, 6, 5, 5, 7, 1.1F, 0, 15790320, 9474192);
      tip("tip_rs", BeyPart.Tip.RUBBER_SHARP, 4, 5, 7, 5, 1.0F, 1, 2763312, 11546150);
      tip("tip_rb", BeyPart.Tip.RUBBER_BALL, 5, 8, 4, 5, 1.1F, 1, 3159100, 3381759);
      tip("tip_rdf", BeyPart.Tip.RUBBER_DEFENSE_FLAT, 7, 8, 3, 7, 1.3F, 1, 2763312, 16750848);
      tip("tip_lrf", BeyPart.Tip.LEFT_RUBBER_FLAT, 10, 3, 1, 13, 1.1F, 1, 2763312, 10040217);
      tip("tip_sd", BeyPart.Tip.SEMI_DEFENSE, 3, 8, 9, 3, 1.1F, 0, 15263980, 7368824);
      tip("tip_ds", BeyPart.Tip.DEFENSE_SHARP, 2, 7, 11, 2, 1.1F, 0, 15790320, 3355647);
      tip("tip_ms", BeyPart.Tip.METAL_SHARP, 2, 4, 13, 2, 1.8F, 1, 13159636, 9474192);
      tip("tip_mb", BeyPart.Tip.METAL_BALL, 4, 9, 8, 4, 2.0F, 1, 13159636, 7370880);
      tip("tip_gb", BeyPart.Tip.GRAVITY_BALL, 4, 10, 7, 3, 1.6F, 1, 3828282, 13684944);
      tip("tip_q", BeyPart.Tip.QUAKE, 9, 4, 1, 8, 1.0F, 1, 15263980, 16737843);
      tip("tip_bd", BeyPart.Tip.BEARING_DRIVE, 1, 5, 16, 2, 1.4F, 2, 13684944, 2121952);
   }

   public static enum Tip {
      NONE(0.3F),
      FLAT(0.22F),
      RUBBER_FLAT(0.26F),
      EXTREME_FLAT(0.3F),
      SEMI_FLAT(0.12F),
      SHARP(0.0F),
      ETERNAL_SHARP(0.0F),
      DEFENSE(0.08F),
      WIDE_DEFENSE(0.1F),
      ETERNAL_WIDE_DEFENSE(0.12F),
      BALL(0.12F),
      WIDE_BALL(0.18F),
      METAL_FLAT(0.2F),
      RUBBER_SEMI_FLAT(0.14F),
      FINAL_DRIVE(0.18F),
      FINAL_SURVIVE(0.06F),
      COAT_SHARP(0.02F),
      HOLE_FLAT(0.2F),
      WIDE_FLAT(0.3F),
      WIDE_SEMI_FLAT(0.16F),
      RUBBER_SHARP(0.03F),
      RUBBER_BALL(0.12F),
      RUBBER_DEFENSE_FLAT(0.2F),
      LEFT_RUBBER_FLAT(0.26F),
      SEMI_DEFENSE(0.1F),
      DEFENSE_SHARP(0.04F),
      METAL_SHARP(0.0F),
      METAL_BALL(0.12F),
      GRAVITY_BALL(0.14F),
      QUAKE(0.18F),
      BEARING_DRIVE(0.02F);

      public final float contact;

      private Tip(float contact) {
         this.contact = contact;
      }

      /** Rubber grips the floor: fast, aggressive, the rubber wears as the bey slows. */
      public boolean rubber() {
         return switch (this) {
            case RUBBER_FLAT, RUBBER_SEMI_FLAT, COAT_SHARP, RUBBER_SHARP, RUBBER_BALL, RUBBER_DEFENSE_FLAT, LEFT_RUBBER_FLAT -> true;
            default -> false;
         };
      }

      /** The tip turns on its own (bearing / eternal / final): hits and the floor steal less spin. */
      public boolean freeSpin() {
         return this == ETERNAL_SHARP || this == ETERNAL_WIDE_DEFENSE || this == FINAL_DRIVE || this == FINAL_SURVIVE || this == BEARING_DRIVE;
      }

      public boolean sharp() {
         return switch (this) {
            case SHARP, ETERNAL_SHARP, COAT_SHARP, RUBBER_SHARP, DEFENSE_SHARP, METAL_SHARP, BEARING_DRIVE -> true;
            default -> false;
         };
      }

      public boolean ball() {
         return switch (this) {
            case BALL, WIDE_BALL, RUBBER_BALL, METAL_BALL, GRAVITY_BALL -> true;
            default -> false;
         };
      }

      public boolean wide() {
         return switch (this) {
            case WIDE_DEFENSE, ETERNAL_WIDE_DEFENSE, WIDE_BALL, WIDE_FLAT, WIDE_SEMI_FLAT, RUBBER_DEFENSE_FLAT -> true;
            default -> false;
         };
      }

      public boolean metal() {
         return this == METAL_FLAT || this == METAL_SHARP || this == METAL_BALL;
      }

      public boolean defense() {
         return switch (this) {
            case DEFENSE, WIDE_DEFENSE, ETERNAL_WIDE_DEFENSE, SEMI_DEFENSE, DEFENSE_SHARP, RUBBER_DEFENSE_FLAT -> true;
            default -> false;
         };
      }

      /** A flat contact surface (drives the bey around the bowl). */
      public boolean flat() {
         return this.contact >= 0.18F && !this.ball() && !this.defense() && this != FINAL_SURVIVE;
      }
   }

   public static enum Trait {
      NONE,
      BOOST_DISK,
      CLAW,
      WING,
      UPPER_WING,
      TORNADO,
      DEFENSE_RING,
      RUBBER_RING,
      TWIN_HEIGHT,
      GRAVITY,
      SPIRIT,
      FOUR_D;
   }
}
