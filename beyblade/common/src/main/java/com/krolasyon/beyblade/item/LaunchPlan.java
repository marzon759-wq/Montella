package com.krolasyon.beyblade.item;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class LaunchPlan {
   public static final float IDEAL_MIN = 15.0F;
   public static final float IDEAL_MAX = 50.0F;
   public static final int OVERCHARGE = 65;
   private static final Map<Player, LaunchPlan.Aim> SERVER = new WeakHashMap<>();
   private static final Map<Player, LaunchPlan.Aim> CLIENT = new WeakHashMap<>();
   public final float power;
   public final float pitch;
   public final float jitter;
   public final float wobble;
   public final double moving;
   public final boolean perfect;
   public final boolean powerLaunch;
   public final boolean overcharged;
   public final String hint;

   private static Map<Player, LaunchPlan.Aim> map(Level l) {
      return l.isClientSide() ? CLIENT : SERVER;
   }

   public static void reset(Player p) {
      map(p.level()).remove(p);
   }

   public static void track(Player p) {
      LaunchPlan.Aim a = map(p.level()).computeIfAbsent(p, k -> new LaunchPlan.Aim());
      if (!a.init) {
         a.init = true;
         a.yaw = p.getYRot();
         a.pitch = p.getXRot();
         a.x = p.getX();
         a.z = p.getZ();
      } else {
         float d = Math.abs(Mth.wrapDegrees(p.getYRot() - a.yaw)) + Math.abs(p.getXRot() - a.pitch);
         a.jitter = a.jitter * 0.88F + d;
         a.yaw = p.getYRot();
         a.pitch = p.getXRot();
         double sp = Math.hypot(p.getX() - a.x, p.getZ() - a.z);
         a.speed = a.speed * 0.7 + sp * 0.3;
         a.x = p.getX();
         a.z = p.getZ();
      }
   }

   private LaunchPlan(Player p, int used) {
      LaunchPlan.Aim a = map(p.level()).get(p);
      this.power = Mth.clamp((float)used / 40.0F, 0.25F, 1.0F);
      this.pitch = p.getXRot();
      this.jitter = a == null ? 0.0F : a.jitter;
      this.moving = a == null ? 0.0 : a.speed;
      this.perfect = used >= 40 && used <= 48;
      this.powerLaunch = p.isShiftKeyDown();
      this.overcharged = used > 65;
      float err = this.pitch < 15.0F ? 15.0F - this.pitch : (this.pitch > 50.0F ? this.pitch - 50.0F : 0.0F);
      float tilt = Mth.clamp(err / 35.0F, 0.0F, 1.0F) * 0.55F;
      float shake = Mth.clamp((this.jitter - 6.0F) / 45.0F, 0.0F, 1.0F) * 0.45F;
      float run = (float)Mth.clamp(this.moving / 0.25, 0.0, 1.0) * 0.3F;
      float weak = this.power < 0.45F ? (0.45F - this.power) / 0.45F * 0.25F : 0.0F;
      float tired = this.overcharged ? Math.min(0.4F, (float)(used - 65) * 0.01F) : 0.0F;
      float w = tilt + shake + run + weak + tired + (this.powerLaunch ? 0.1F : 0.0F);
      if (this.perfect) {
         w *= 0.35F;
      }

      this.wobble = Mth.clamp(w, 0.0F, 1.0F);
      float worst = Math.max(Math.max(tilt, shake), Math.max(Math.max(run, weak), tired));
      if (worst < 0.08F) {
         this.hint = this.perfect ? "perfect" : null;
      } else if (worst == tilt) {
         this.hint = this.pitch < 15.0F ? "too_flat" : "too_steep";
      } else if (worst == shake) {
         this.hint = "shaky";
      } else if (worst == run) {
         this.hint = "moving";
      } else if (worst == weak) {
         this.hint = "weak";
      } else {
         this.hint = "tired";
      }
   }

   public static LaunchPlan of(Player p, int used) {
      return new LaunchPlan(p, used);
   }

   public boolean idealAngle() {
      return this.pitch >= 15.0F && this.pitch <= 50.0F;
   }

   public Vec3 velocity(Player p) {
      Vec3 look = p.getLookAngle();
      Vec3 flat = new Vec3(look.x, 0.0, look.z);
      if (flat.lengthSqr() < 1.0E-4) {
         flat = new Vec3(0.0, 0.0, 1.0);
      }

      flat = flat.normalize();
      double arc = Mth.clamp((double)((25.0F - this.pitch) / 60.0F), -0.6, 1.0);
      double speed = (0.35 + 0.55 * (double)this.power) * (this.pitch > 60.0F ? 0.7 : 1.0);
      Vec3 v = flat.scale(speed).add(0.0, 0.12 + 0.22 * arc, 0.0);
      return v.add(this.curve(p));
   }

   public Vec3 curve(Player p) {
      Vec3 look = p.getLookAngle();
      Vec3 side = new Vec3(-look.z, 0.0, look.x);
      if (side.lengthSqr() < 1.0E-4) {
         return Vec3.ZERO;
      } else {
         side = side.normalize();
         LaunchPlan.Aim a = map(p.level()).get(p);
         if (a == null) {
            return Vec3.ZERO;
         } else {
            Vec3 move = new Vec3(p.getX() - p.xo, 0.0, p.getZ() - p.zo);
            double s = Mth.clamp(move.dot(side) * 2.0, -0.25, 0.25);
            return side.scale(s);
         }
      }
   }

   private static final class Aim {
      float yaw;
      float pitch;
      float jitter;
      boolean init;
      double x;
      double z;
      double speed;
   }
}
