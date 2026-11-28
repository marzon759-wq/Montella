package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.registry.ModSounds;
import com.krolasyon.beyblade.world.WorldImpact;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The gust of wind two beys throw out sideways when they slam into each other, as in the anime: dust and air
 * blasted along the line perpendicular to the hit, everything light nearby blown away, plants laid flat.
 */
public final class ClashWind {
   private ClashWind() {
   }

   static void gust(BeybladeEntity a, BeybladeEntity b, double cx, double cy, double cz, double nx, double nz, boolean heavy, double impact) {
      if (!(a.level() instanceof ServerLevel l)) {
         return;
      }
      double power = heavy ? 1.0 + Math.min(1.0, impact) : 0.45;
      l.playSound(null, cx, cy, cz, ModSounds.CLASH_WIND.get(), SoundSource.NEUTRAL, heavy ? 2.0F : 0.9F, 0.85F + l.random.nextFloat() * 0.3F);
      Vec3 c = new Vec3(cx, cy, cz);
      Vec3 perp = new Vec3(-nz, 0.0, nx);
      Vec3 axis = new Vec3(nx, 0.0, nz);
      // directional air: count 0 makes the deltas a velocity
      int n = heavy ? 8 : 3;
      for (int i = 0; i < n; i++) {
         double side = i % 2 == 0 ? 1.0 : -1.0;
         Vec3 d = perp.scale(side * (0.7 + l.random.nextDouble() * 0.6)).add(axis.scale((l.random.nextDouble() - 0.5) * 0.7));
         double sp = (0.35 + l.random.nextDouble() * 0.45) * power;
         l.sendParticles(ParticleTypes.CLOUD, cx + d.x * 0.3, cy - 0.15 + l.random.nextDouble() * 0.3, cz + d.z * 0.3, 0, d.x, 0.02, d.z, sp);
         if (heavy && i % 4 == 0) {
            l.sendParticles(ParticleTypes.SWEEP_ATTACK, cx + d.x * 1.2, cy, cz + d.z * 1.2, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
      if (heavy) {
         l.sendParticles(ParticleTypes.EXPLOSION, cx, cy, cz, 1, 0.0, 0.0, 0.0, 0.0);
         l.sendParticles(ParticleTypes.POOF, cx, cy - 0.3, cz, 5, 0.6, 0.05, 0.6, 0.12);
         WorldImpact.flatten(l, c, 2.5 + impact * 1.5);
      }
      // blow light things away along the wind
      double r = heavy ? 6.5 : 3.5;
      Player oa = a.getOwner();
      Player ob = b.getOwner();
      for (Entity e : l.getEntities(a, new AABB(c, c).inflate(r, 2.0, r), x -> x != b && !(x instanceof BeybladeEntity))) {
         if (!(e instanceof LivingEntity || e instanceof ItemEntity)) {
            continue;
         }
         Vec3 rel = new Vec3(e.getX() - cx, 0.0, e.getZ() - cz);
         double d = rel.length();
         if (d > r || d < 0.01) {
            continue;
         }
         double side = Math.signum(rel.dot(perp));
         Vec3 push = perp.scale(side == 0.0 ? 1.0 : side).scale(0.6).add(rel.scale(0.4 / d));
         double k = power * (1.0 - d / r) * ((e == oa || e == ob) ? 0.35 : 0.8);
         e.setDeltaMovement(e.getDeltaMovement().add(push.scale(k)).add(0.0, 0.12 * k, 0.0));
         e.hurtMarked = true;
      }
   }
}
