package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.world.WorldImpact;
import com.krolasyon.beyblade.world.WorldImpact.Style;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What each special move does to the world around it, following how the moves look in Metal Fight Beyblade:
 *
 * Storm Pegasus     Star Blast Attack: into the sky, dives like a shooting star, star shaped crater (PegasusEntity)
 *                   Storm Bringer: a tornado that sucks the opponent up and drops it (PegasusEntity)
 *                   Star Rocket: rocket dive, crater
 * L-Drago           Spin steal vortex pulls everything in, Dragon Emperor dive leaves a burning crater,
 *                   Wild Fire scorches the ground to magma, Life Destructor blasts the area
 * Rock Leone        Lion Gale Force Wall: a wind wall that throws everything out and strips the plants,
 *                   Rock crusher: quake rings and a crater, King Lion Tearing Blast / giant tornado
 * Earth Eagle       wind grinder tornado, Diving Crush crater, Shining Tornado Buster
 * Flame Libra       Sonic Wave rings, Inferno Blast: digs in, the ground turns to sand and erupts in a sand
 *                   tornado, sand crater; Final Judgment shock
 * Variares          lightning sword crater, Evil Befall feather storm / requiem shock, Tempo time stop freezes mobs.
 *
 * All block changes go through {@link WorldImpact} and come back by themselves.
 */
public final class AnimeMoves {
   private AnimeMoves() {
   }

   private static Vec3 pos(Entity e) {
      return e.position();
   }

   /** Every ability tick (server). */
   static void tick(BeybladeEntity e, int ab, int t) {
      if (!(e.level() instanceof ServerLevel l)) {
         return;
      }
      if (t == 1) {
         e.prevDiving = false;
      }
      // any dive that just landed makes a crater
      if (e.prevDiving && !e.diving) {
         landed(e, l);
      }
      e.prevDiving = e.diving;
      Vec3 c = pos(e);
      if (e.diving) {
         Px.send(l, ParticleTypes.FIREWORK, c.x, c.y + 0.4, c.z, 3, 0.15, 0.3, 0.15, 0.02);
      }
      if (e instanceof CommonBeybladeEntity) {
         if (ab == 3 && t == 6) {
            WorldImpact.shockwave(l, e, c, 5.0);
         }
         return;
      }
      switch (e.kindId()) {
         case "ldrago" -> {
            if (ab == 2 && t > 10 && t < 90) {
               WorldImpact.tornado(l, e, c, 4.5, 6.0, 0.9, false, t);
            } else if (ab == 4) {
               WorldImpact.field(l, c, 3.0, Style.FIRE, 3);
               WorldImpact.pillar(l, c, 1.2, 3.0, Style.FIRE, t);
            } else if (ab == 5 && t == 40) {
               WorldImpact.shockwave(l, e, c, 10.0);
               WorldImpact.crater(l, c, 3.0, 1.5, Style.FIRE);
            }
         }
         case "leone" -> {
            if (ab == 3 && t < 30) {
               WorldImpact.tornado(l, e, c, 3.5, 5.0, 1.0, true, t);
            } else if (ab == 4 && t > 4 && (t - 5) % 20 == 0 && t < 80) {
               WorldImpact.shockwave(l, e, c, 5.0 + (t - 5) / 20 * 2.0);
               if (t == 5) {
                  WorldImpact.crater(l, c, 3.0, 2.0, Style.EARTH);
               }
            } else if (ab == 5) {
               WorldImpact.tornado(l, e, c, 5.5, 14.0, 1.1, false, t);
            } else if (ab == 6) {
               WorldImpact.tornado(l, e, c, 8.0, 22.0, 1.3, false, t);
            }
         }
         case "eagle" -> {
            if (ab == 2) {
               WorldImpact.tornado(l, e, c, 3.0, 6.0, 0.8, false, t);
            } else if (ab == 5) {
               WorldImpact.tornado(l, e, c, 6.0, 16.0, 1.2, false, t);
            }
         }
         case "beffal" -> {
            if (ab == 3) {
               WorldImpact.tornado(l, e, c, 4.0, 8.0, 0.7, false, t);
            }
         }
         case "tempo" -> {
            if (ab == 1) {
               timeStop(e, l);
            }
         }
         case "variares", "pegasus" -> {
         }
         default -> {
            // Flame Libra
            if (ab == 1) {
               for (int p : BeybladeEntity.WAVE_PULSES) {
                  if (t == p) {
                     WorldImpact.shockwave(l, e, c, 4.0 + p * 0.25);
                  }
               }
            } else if (ab == 3) {
               if (t < 24) {
                  // digging in: the ground around turns to sand and starts to swirl
                  WorldImpact.field(l, c, 4.0, Style.SAND, 4);
                  WorldImpact.pillar(l, c, 1.0, 2.0 + t * 0.15, Style.SAND, t);
               } else if (t < 52) {
                  Vec3 base = new Vec3(c.x, e.abilityBaseY, c.z);
                  WorldImpact.pillar(l, base, 2.4, 9.0, Style.SAND, t);
                  WorldImpact.tornado(l, e, base, 3.5, 9.0, 0.8, false, t);
               }
            } else if (ab == 5 && t == 86) {
               WorldImpact.shockwave(l, e, c, 12.0);
               WorldImpact.crater(l, c, 2.5, 1.2, Style.STAR);
            }
         }
      }
   }

   /** While a buff ability (shield / awakening) is active. */
   static void buff(BeybladeEntity e) {
      if (!(e.level() instanceof ServerLevel l)) {
         return;
      }
      if (e.kindId().equals("leone") && e.isShielded()) {
         // Lion Gale Force Wall
         WorldImpact.tornado(l, e, pos(e), 3.2, 4.0, 0.9, true, e.tickCount);
      }
   }

   /** The moment a diving move hits the ground. */
   static void landed(BeybladeEntity e, ServerLevel l) {
      Vec3 c = pos(e);
      switch (e.kindId()) {
         case "pegasus" -> {
            WorldImpact.crater(l, c, 4.5, 2.5, Style.STAR);
            WorldImpact.shockwave(l, e, c, 9.0);
         }
         case "ldrago" -> {
            WorldImpact.crater(l, c, 4.5, 2.5, Style.FIRE);
            WorldImpact.field(l, c, 6.0, Style.FIRE, 30);
            WorldImpact.shockwave(l, e, c, 8.0);
         }
         case "libra" -> {
            WorldImpact.crater(l, c, 4.0, 2.0, Style.SAND);
            WorldImpact.shockwave(l, e, c, 8.0);
         }
         default -> {
            WorldImpact.crater(l, c, 3.5, 2.0, Style.EARTH);
            WorldImpact.shockwave(l, e, c, 7.0);
         }
      }
   }

   /** Earth Eagle's Diving Crush and other slams that do not use the dive flag. */
   static void slam(BeybladeEntity e, double radius, Style style) {
      if (e.level() instanceof ServerLevel l) {
         WorldImpact.crater(l, pos(e), radius, radius * 0.55, style);
         WorldImpact.shockwave(l, e, pos(e), radius * 2.0);
      }
   }

   /** Variares' Final Lightning Sword lands. */
   static void swordStrike(BeybladeEntity e, Vec3 at) {
      if (e.level() instanceof ServerLevel l) {
         WorldImpact.crater(l, at, 4.0, 2.5, Style.STORM);
         WorldImpact.shockwave(l, e, at, 9.0);
      }
   }

   static void requiem(BeybladeEntity e, Vec3 at) {
      if (e.level() instanceof ServerLevel l) {
         WorldImpact.shockwave(l, e, at, 8.0);
         WorldImpact.crater(l, at, 2.5, 1.2, Style.EARTH);
      }
   }

   /** Tempo's time stop: mobs and items around freeze in place. */
   private static void timeStop(BeybladeEntity e, ServerLevel l) {
      Player owner = e.getOwner();
      for (Entity x : l.getEntities(e, new AABB(e.blockPosition()).inflate(14.0), x -> x != owner)) {
         if (x instanceof BeybladeEntity) {
            continue;
         }
         x.setDeltaMovement(Vec3.ZERO);
         x.hurtMarked = true;
         if (x instanceof LivingEntity le) {
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 10, false, false));
            le.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 10, 10, false, false));
         }
         if (l.getGameTime() % 4 == 0) {
            Px.send(l, ParticleTypes.REVERSE_PORTAL, x.getX(), x.getY() + x.getBbHeight() * 0.5, x.getZ(), 2, 0.2, 0.3, 0.2, 0.0);
         }
      }
   }
}
