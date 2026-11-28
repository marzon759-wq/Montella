package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.BeffalEntity;
import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.entity.CommonBeybladeEntity;
import com.krolasyon.beyblade.entity.EagleEntity;
import com.krolasyon.beyblade.entity.LDragoEntity;
import com.krolasyon.beyblade.entity.LeoneEntity;
import com.krolasyon.beyblade.entity.PegasusEntity;
import com.krolasyon.beyblade.entity.TempoEntity;
import com.krolasyon.beyblade.entity.VariaresEntity;
import com.krolasyon.beyblade.parts.BeyPart;
import com.krolasyon.beyblade.parts.Loadout;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;

/**
 * The procedural half of the beyblade rig: precession wobble, lean into the direction of travel, hit shake,
 * special move tilts and the spin itself. These are computed every frame from the entity and written onto the
 * GeckoLib bones (tilt, spin, tip) after the keyframed animations have been applied, so keyframes (hits, specials,
 * bursts) stack on top of the physics.
 *
 * Bone tree (geo/beyblade.geo.json):
 * root > tilt > motion > spin > { tip_lag > tip, track, wheel, ring, bolt }
 */
public final class BeyRig {
   /** Size of the mesh in the world: mesh units to blocks. */
   public static final float SCALE = 0.45F;

   public final BeybladeEntity e;
   public float partial;
   public float t;
   public int state;
   public float ratio;
   public float spin;
   public int dir;
   public int ab;
   public float at;
   public float tx;
   public float tz;
   public float lift;
   public float jitter;
   public float glow;
   public float flash;
   public int overlay;
   public float[] glowCol;
   public float[] streak;
   public double ex;
   public double ey;
   public double ez;
   public float speed;
   public Loadout parts;
   public BeyMesh mesh;
   public float partLift;

   private BeyRig(BeybladeEntity e) {
      this.e = e;
   }

   private static BeyRig last;

   /** Rig of this entity for this frame (computed once, shared by the model, the renderer and the layers). */
   public static BeyRig of(BeybladeEntity e, float partial) {
      BeyRig r = last;
      if (r != null && r.e == e && r.partial == partial && r.t == (float)e.tickCount + partial) {
         return r;
      }
      r = new BeyRig(e);
      r.compute(partial);
      last = r;
      return r;
   }

   private void compute(float partial) {
      BeybladeEntity e = this.e;
      this.partial = partial;
      float t = (float)e.tickCount + partial;
      this.t = t;
      this.state = e.getState();
      this.ratio = e.spinRatio();
      this.spin = e.getState() == BeybladeEntity.STATE_DEFEATED ? e.frozenSpin : Mth.lerp(partial, e.spinAngleO, e.spinAngle);
      float prec = Mth.lerp(partial, e.precessO, e.precess) * (float) (Math.PI / 180.0);
      this.ab = e.clientAbility;
      this.at = e.abilityAge(partial);
      int ab = this.ab;
      float at = this.at;
      boolean drago = e instanceof LDragoEntity;
      boolean leone = e instanceof LeoneEntity;
      boolean eagle = e instanceof EagleEntity;
      boolean pegasus = e instanceof PegasusEntity;
      CommonBeybladeEntity common = e instanceof CommonBeybladeEntity c ? c : null;
      boolean tempo = e instanceof TempoEntity;
      boolean variares = e instanceof VariaresEntity;
      boolean beffal = e instanceof BeffalEntity;
      this.parts = e.parts();
      this.mesh = BeyMesh.forLoadout(this.parts, BeyMesh.of(e));
      this.partLift = this.mesh.lift(this.parts);
      this.dir = e.spinDir();
      this.ex = Mth.lerp((double)partial, e.xo, e.getX());
      this.ey = Mth.lerp((double)partial, e.yo, e.getY());
      this.ez = Mth.lerp((double)partial, e.zo, e.getZ());
      double vx = e.getX() - e.xo;
      double vz = e.getZ() - e.zo;
      float sp = (float)Math.sqrt(vx * vx + vz * vz);
      this.speed = sp;
      int state = this.state;
      float ratio = this.ratio;
      BeyPart.Tip tip = this.parts.tip();
      float wob;
      if (state == 1) {
         // sleep out / burst: the body angle is keyframed, only a little residual precession here
         float k = Mth.clamp((t - (float)e.clientStateStart) / 22.0F, 0.0F, 1.0F);
         wob = Mth.lerp(k, 6.0F, 0.0F);
      } else if (state == 2) {
         wob = 20.0F;
      } else {
         wob = ratio < 0.4F ? 2.5F + (0.4F - ratio) / 0.4F * 17.0F : 1.2F + (1.0F - ratio) * 2.5F;
         // the tip decides how well the bey keeps itself upright
         wob *= tipSteadiness(tip);
         float lw = e.getWobble();
         if (lw > 0.0F) {
            wob += lw * (16.0F + 6.0F * Mth.sin(t * 0.9F));
         }
      }

      float tx = Mth.cos(prec) * wob;
      float tz = Mth.sin(prec) * wob;
      if (sp > 0.02F && state != 2) {
         // rubber tips bite into the floor and lean harder into their turns
         float lean = Math.min(tip.rubber() ? 21.0F : 17.0F, sp * (tip.rubber() ? 58.0F : 48.0F));
         tx += (float)(vz / (double)sp) * lean;
         tz += (float)(-vx / (double)sp) * lean;
      }

      float hitAge = t - (float)e.hitTime;
      if (hitAge >= 0.0F && hitAge < 9.0F) {
         float k = 1.0F - hitAge / 9.0F;
         float amp = (t - (float)e.heavyHitTime < 9.0F ? 22.0F : 12.0F) * k;
         tx += Mth.sin(t * 2.7F) * amp;
         tz += Mth.cos(t * 3.1F) * amp;
      }

      float lift = 0.0F;
      float jitter = 0.0F;
      if (common == null && !tempo && !variares && !beffal) {
         if (pegasus) {
            if (ab == 2 || ab == 3) {
               jitter = Mth.sin(t * 12.0F) * 0.015F;
            }
         } else if (eagle) {
            if (ab == 2) {
               jitter = Mth.sin(t * 13.0F) * 0.02F;
            } else if (ab == 3 && at >= 10.0F && t - (float)((EagleEntity)e).grabTime > 40.0F) {
               float[] d = towardTarget(e, 40.0F);
               if (d != null) {
                  tx = d[0];
                  tz = d[1];
               }
            }
         } else if (leone) {
            if (ab == 3 && at < 12.0F) {
               lift = -0.18F * Mth.clamp(at / 4.0F, 0.0F, 1.0F);
               jitter = Mth.sin(t * 14.0F) * 0.02F;
            } else if ((ab == 2 || ab == 5) && at > 2.0F) {
               float[] d = towardTarget(e, Mth.clamp((at - 2.0F) / 6.0F, 0.0F, 1.0F) * 22.0F);
               if (d != null) {
                  tx = d[0];
                  tz = d[1];
               }
               jitter = Mth.sin(t * 16.0F) * 0.015F;
            }
         } else if (drago) {
            if (ab == 2) {
               jitter = Mth.sin(t * 12.0F) * 0.02F;
               lift = -0.08F;
            } else if (ab == 3 && at < 8.0F) {
               lift = -0.22F * Mth.clamp(at / 5.0F, 0.0F, 1.0F);
               jitter = Mth.sin(t * 15.0F) * 0.025F;
            } else if (ab == 5 && at > 3.0F && at < 62.0F) {
               float[] d = towardTarget(e, Mth.clamp((at - 3.0F) / 10.0F, 0.0F, 1.0F) * 30.0F);
               if (d != null) {
                  tx = d[0];
                  tz = d[1];
               }
               if (at > 20.0F) {
                  jitter = Mth.sin(t * 19.0F) * 0.03F;
               }
            }
         } else if (ab == 1) {
            jitter = Mth.sin(t * 11.0F) * 0.025F;
         } else if (ab == 3 && at < 24.0F) {
            lift = -0.24F * Mth.clamp(at / 5.0F, 0.0F, 1.0F);
            jitter = Mth.sin(t * 13.0F) * 0.02F;
         } else if (ab == 4 && at > 4.0F && at < 52.0F) {
            float[] d = towardTarget(e, Mth.clamp((at - 4.0F) / 8.0F, 0.0F, 1.0F) * 34.0F);
            if (d != null) {
               tx = d[0];
               tz = d[1];
            }
            if (at > 32.0F) {
               jitter = Mth.sin(t * 17.0F) * 0.03F;
            }
         }
      } else if (ab != 0) {
         jitter = Mth.sin(t * 13.0F) * 0.015F;
      }

      this.tx = tx;
      this.tz = tz;
      this.lift = lift;
      this.jitter = jitter;
      this.computeGlow(drago, leone, eagle, pegasus, common, tempo, variares, beffal, t, ab);
      this.flash = hitAge >= 0.0F && hitAge < 4.0F ? 1.0F - hitAge / 4.0F : 0.0F;
      this.overlay = OverlayTexture.pack(OverlayTexture.u(this.flash * 0.7F), OverlayTexture.v(false));
      this.streak = common != null
         ? common.variant().rgb(true)
         : (tempo ? new float[]{0.75F, 0.85F, 1.0F}
         : (variares ? new float[]{1.0F, 0.3F, 0.25F}
         : (beffal ? new float[]{1.0F, 0.45F, 0.8F}
         : (drago ? new float[]{1.0F, 0.45F, 0.5F}
         : (leone ? new float[]{0.6F, 1.0F, 0.6F}
         : (eagle ? new float[]{0.8F, 0.6F, 1.0F}
         : (pegasus ? new float[]{0.6F, 0.85F, 1.0F} : new float[]{0.8F, 1.0F, 0.8F})))))));
   }

   /** How much of the precession wobble reaches the body (lower is steadier). */
   public static float tipSteadiness(BeyPart.Tip tip) {
      if (tip.freeSpin()) {
         return 0.7F;
      }
      if (tip.sharp()) {
         return 0.8F;
      }
      if (tip.ball()) {
         return 0.85F;
      }
      if (tip.wide()) {
         return 0.75F;
      }
      return tip.rubber() ? 1.15F : 1.0F;
   }

   private static float[] towardTarget(BeybladeEntity e, float deg) {
      Entity tgt = e.level().getEntity(e.getTargetId());
      if (tgt == null) {
         return null;
      }
      double dx = tgt.getX() - e.getX();
      double dz = tgt.getZ() - e.getZ();
      double dl = Math.sqrt(dx * dx + dz * dz);
      return dl > 0.001 ? new float[]{(float)(dz / dl) * deg, (float)(-dx / dl) * deg} : null;
   }

   private void computeGlow(
      boolean drago, boolean leone, boolean eagle, boolean pegasus, CommonBeybladeEntity common, boolean tempo, boolean variares, boolean beffal, float t, int ab
   ) {
      BeybladeEntity e = this.e;
      float[] glowCol = drago
         ? (e.isRival() ? new float[]{0.65F, 0.2F, 1.0F} : new float[]{1.0F, 0.15F, 0.28F})
         : (e.isRival() ? new float[]{1.0F, 0.25F, 0.15F} : new float[]{0.55F, 1.0F, 0.45F});
      if (pegasus) {
         glowCol = e.isRival() ? new float[]{1.0F, 0.4F, 0.2F} : new float[]{0.4F, 0.75F, 1.0F};
      }
      if (eagle) {
         glowCol = e.isRival() ? new float[]{1.0F, 0.3F, 0.3F} : new float[]{0.7F, 0.4F, 1.0F};
      }
      if (leone) {
         glowCol = e.isRival() ? new float[]{1.0F, 0.6F, 0.1F} : new float[]{0.3F, 1.0F, 0.45F};
      }
      if (common != null) {
         glowCol = common.variant().rgb(true);
      }
      if (variares) {
         glowCol = e.isRival() ? new float[]{0.7F, 0.3F, 1.0F} : new float[]{1.0F, 0.15F, 0.1F};
      }
      if (beffal) {
         glowCol = e.isRival() ? new float[]{0.6F, 0.3F, 1.0F} : new float[]{1.0F, 0.35F, 0.75F};
      }
      if (tempo) {
         glowCol = e.isRival() ? new float[]{1.0F, 0.4F, 0.3F} : new float[]{0.6F, 0.8F, 1.0F};
      }

      float glow = 0.12F;
      if (common == null && !tempo && !variares && !beffal) {
         if (pegasus || eagle) {
            if (ab != 0) {
               glow = 0.8F + 0.2F * Mth.sin(t);
            } else if (e.isShielded()) {
               glow = (pegasus ? 0.65F : 0.6F) + 0.25F * Mth.sin(t * 0.9F);
            }
         } else if (leone) {
            if (ab != 0) {
               glow = 0.8F + 0.2F * Mth.sin(t);
               if (ab == 4) {
                  glowCol = new float[]{1.0F, 0.8F, 0.35F};
               }
            } else if (e.isShielded()) {
               glow = 0.55F + 0.2F * Mth.sin(t * 0.7F);
            }
         } else if (drago) {
            if (ab != 0) {
               glow = 0.8F + 0.2F * Mth.sin(t * 1.1F);
               if (ab == 3 || ab == 5) {
                  glowCol = new float[]{1.0F, 0.45F, 0.1F};
               }
            } else if (e.isShielded()) {
               glow = 0.7F + 0.3F * Mth.sin(t * 0.8F);
            }
         } else if (ab != 0) {
            glow = 0.75F + 0.25F * Mth.sin(t * 0.9F);
            switch (ab) {
               case 3 -> glowCol = new float[]{1.0F, 0.55F, 0.15F};
               case 4 -> glowCol = new float[]{0.5F, 1.0F, 0.95F};
               case 5 -> glowCol = new float[]{1.0F, 0.85F, 0.4F};
               default -> {
               }
            }
         } else if (e.isShielded()) {
            glow = 0.45F + 0.15F * Mth.sin(t * 0.5F);
         }
      } else if (ab != 0) {
         glow = 0.85F + 0.15F * Mth.sin(t * 1.1F);
      } else if (e.isShielded()) {
         glow = 0.55F + 0.2F * Mth.sin(t * 0.8F);
      }
      if (this.state == 1) {
         glow = 0.0F;
      }
      this.glow = glow;
      this.glowCol = glowCol;
   }

   /**
    * Writes the procedural pose onto the bones. These bones carry no keyframes, and GeckoLib keeps whatever was set
    * on them last frame, so every value here is absolute. The keyframed bones (motion and the parts) sit below them.
    */
   public void apply(GeoBone root, GeoBone tilt, GeoBone spinBone, GeoBone tipLag) {
      if (root != null) {
         root.setPosY((this.lift + this.jitter) * 16.0F);
      }
      if (tilt != null) {
         float tilt0 = (float)Math.sqrt((double)(this.tx * this.tx + this.tz * this.tz));
         if (tilt0 > 0.01F) {
            // exact axis-angle tilt (axis lies in the floor plane) as ZYX euler angles for GeckoLib
            Quaternionf q = new Quaternionf().rotateAxis(tilt0 * (float) (Math.PI / 180.0), this.tx / tilt0, 0.0F, this.tz / tilt0);
            Vector3f eu = q.getEulerAnglesZYX(new Vector3f());
            tilt.setRotX(eu.x);
            tilt.setRotY(eu.y);
            tilt.setRotZ(eu.z);
         } else {
            tilt.setRotX(0.0F);
            tilt.setRotY(0.0F);
            tilt.setRotZ(0.0F);
         }
      }
      float spinRad = Mth.wrapDegrees(-this.spin * (float)this.dir) * (float) (Math.PI / 180.0);
      if (spinBone != null) {
         spinBone.setRotY(spinRad);
      }
      if (tipLag != null) {
         float lagRot = 0.0F;
         if (this.parts.tip().freeSpin() && this.state == 0) {
            // a free spinning tip (bearing / eternal / final) lags behind the body: it turns slower in world space
            float lag = Mth.clamp(0.35F + 0.5F * (1.0F - this.ratio), 0.35F, 0.85F);
            lagRot = -spinRad * lag;
         }
         tipLag.setRotY(lagRot);
      }
   }
}
