package com.krolasyon.beyblade.parts;

/**
 * How the performance tip touches the stadium floor. Metal Fight rules of thumb:
 * rubber grips (fast, aggressive, burns stamina, the rubber loses bite as the bey slows),
 * flat drives the bey around the bowl, sharp sits still and saves stamina, ball rolls and recovers from tilt,
 * defense / wide tips resist knock outs, free spinning tips lose less spin to hits and the floor,
 * metal tips slide fast but have little grip, quake bounces erratically.
 */
public final class TipPhysics {
   private TipPhysics() {
   }

   /** Fraction of horizontal speed kept per tick while touching the floor (vanilla bey: 0.955). */
   public static double groundFriction(BeyPart.Tip t) {
      if (t.rubber()) {
         return t.sharp() ? 0.957 : 0.966;
      }
      if (t.metal()) {
         return t.sharp() ? 0.944 : 0.969;
      }
      if (t == BeyPart.Tip.QUAKE) {
         return 0.962;
      }
      if (t.sharp()) {
         return t.freeSpin() ? 0.938 : 0.942;
      }
      if (t.ball()) {
         return 0.952;
      }
      if (t.defense()) {
         return 0.947;
      }
      if (t.flat()) {
         return 0.962;
      }
      return 0.955;
   }

   /** Self propulsion along the flower pattern, per tick at full spin. */
   public static double drive(BeyPart.Tip t, float ratio, int spinDir) {
      double d;
      if (t.rubber()) {
         d = t.sharp() ? 0.0018 : (t.ball() ? 0.0024 : 0.0058);
         // the rubber wears: less bite as the spin drops
         d *= 0.55 + 0.45 * ratio;
         if (t == BeyPart.Tip.LEFT_RUBBER_FLAT && spinDir < 0) {
            d *= 1.3;
         }
      } else if (t == BeyPart.Tip.EXTREME_FLAT) {
         d = 0.0062;
      } else if (t.metal() && !t.sharp()) {
         d = t.ball() ? 0.0016 : 0.0048;
      } else if (t.flat()) {
         d = 0.0038;
      } else if (t == BeyPart.Tip.SEMI_FLAT || t == BeyPart.Tip.WIDE_SEMI_FLAT || t == BeyPart.Tip.RUBBER_SEMI_FLAT) {
         d = 0.0022;
      } else if (t.ball()) {
         d = 0.0010;
      } else {
         d = 0.0;
      }
      return d * Math.max(0.2, ratio);
   }

   /** Multiplier on spin decay. */
   public static float decay(BeyPart.Tip t) {
      if (t.freeSpin()) {
         return 0.86F;
      }
      if (t.rubber()) {
         return t.sharp() ? 1.04F : 1.12F;
      }
      if (t == BeyPart.Tip.QUAKE) {
         return 1.15F;
      }
      if (t == BeyPart.Tip.EXTREME_FLAT) {
         return 1.08F;
      }
      if (t.sharp()) {
         return t.metal() ? 0.88F : 0.92F;
      }
      if (t.metal()) {
         return 1.03F;
      }
      return 1.0F;
   }

   /** Multiplier on knock back taken. */
   public static double knock(BeyPart.Tip t) {
      if (t == BeyPart.Tip.RUBBER_DEFENSE_FLAT) {
         return 0.86;
      }
      if (t.wide() || t.defense()) {
         return 0.9;
      }
      if (t == BeyPart.Tip.GRAVITY_BALL || t == BeyPart.Tip.METAL_BALL) {
         return 0.92;
      }
      if (t.ball()) {
         return 0.96;
      }
      if (t.rubber()) {
         return 0.95;
      }
      return t.sharp() ? 1.05 : 1.0;
   }

   /** Grippy tips launch onto the stadium rail at lower speed and accelerate harder on it. */
   public static boolean grippy(BeyPart.Tip t) {
      return t.rubber() || t.flat() || t == BeyPart.Tip.FINAL_DRIVE;
   }

   public static boolean point(BeyPart.Tip t) {
      return t.sharp() || t.ball();
   }
}
