package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.parts.BeyPart;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;

/** Client side floor contact effects of the performance tip. */
public final class TipFx {
   private TipFx() {
   }

   public static void tick(BeybladeEntity e) {
      if (e.getState() != BeybladeEntity.STATE_BATTLE || !e.onGround() || e.tickCount < 4) {
         return;
      }
      double vx = e.getX() - e.xo;
      double vz = e.getZ() - e.zo;
      double sp = Math.sqrt(vx * vx + vz * vz);
      BeyPart.Tip t = e.parts().tip();
      Level lv = e.level();
      double x = e.getX();
      double y = e.getY() + 0.02;
      double z = e.getZ();
      if (t.rubber()) {
         // hot rubber: a thin smoke wake in hard turns
         if (sp > 0.16 && e.tickCount % 2 == 0) {
            lv.addParticle(ParticleTypes.SMOKE, x - vx * 2.0, y, z - vz * 2.0, -vx * 0.1, 0.012, -vz * 0.1);
         }
      } else if (t.metal()) {
         // metal on the floor throws sparks
         if (sp > 0.12 && e.tickCount % 2 == 0) {
            lv.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, -vx * 2.0 + (lv.random.nextDouble() - 0.5) * 0.2, 0.08, -vz * 2.0 + (lv.random.nextDouble() - 0.5) * 0.2);
         }
      } else if (t == BeyPart.Tip.QUAKE) {
         if (e.tickCount % 9 == 0) {
            for (int i = 0; i < 3; i++) {
               lv.addParticle(ParticleTypes.CRIT, x, y, z, (lv.random.nextDouble() - 0.5) * 0.3, 0.12, (lv.random.nextDouble() - 0.5) * 0.3);
            }
         }
      } else if (t.flat() && sp > 0.2 && e.tickCount % 4 == 0) {
         lv.addParticle(ParticleTypes.CLOUD, x - vx, y, z - vz, 0.0, 0.004, 0.0);
      } else if (t.sharp() && sp < 0.03 && e.spinRatio() > 0.5F && e.tickCount % 20 == 0) {
         // a sharp tip "sleeping" in the centre leaves a tiny swirl
         lv.addParticle(ParticleTypes.WHITE_ASH, x, y, z, 0.0, 0.01, 0.0);
      }
   }
}
