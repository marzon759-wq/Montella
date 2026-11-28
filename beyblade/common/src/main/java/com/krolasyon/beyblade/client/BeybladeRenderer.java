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
import com.krolasyon.beyblade.parts.Loadout;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class BeybladeRenderer extends EntityRenderer<BeybladeEntity> {
   public BeybladeRenderer(Context ctx) {
      super(ctx);
      this.shadowRadius = 0.42F;
      this.shadowStrength = 0.8F;
   }

   public void render(BeybladeEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
      float t = (float)e.tickCount + partial;
      int state = e.getState();
      float ratio = e.spinRatio();
      float spin = Mth.lerp(partial, e.spinAngleO, e.spinAngle);
      float prec = Mth.lerp(partial, e.precessO, e.precess) * (float) (Math.PI / 180.0);
      int ab = e.clientAbility;
      float at = e.abilityAge(partial);
      boolean drago = e instanceof LDragoEntity;
      boolean leone = e instanceof LeoneEntity;
      boolean eagle = e instanceof EagleEntity;
      boolean pegasus = e instanceof PegasusEntity;
      CommonBeybladeEntity common = e instanceof CommonBeybladeEntity c ? c : null;
      boolean tempo = e instanceof TempoEntity;
      boolean variares = e instanceof VariaresEntity;
      boolean beffal = e instanceof BeffalEntity;
      Loadout parts = e.parts();
      BeyMesh mesh = BeyMesh.forLoadout(parts, BeyMesh.of(e));
      float partLift = mesh.lift(parts);
      int dir = e.spinDir();
      double ex = Mth.lerp((double)partial, e.xo, e.getX());
      double ey = Mth.lerp((double)partial, e.yo, e.getY());
      double ez = Mth.lerp((double)partial, e.zo, e.getZ());
      double vx = e.getX() - e.xo;
      double vz = e.getZ() - e.zo;
      float sp = (float)Math.sqrt(vx * vx + vz * vz);
      float wob;
      if (state == 1) {
         float k = Mth.clamp((t - (float)e.clientStateStart) / 22.0F, 0.0F, 1.0F);
         wob = Mth.lerp(k * k, 10.0F, 76.0F);
      } else if (state == 2) {
         wob = 20.0F;
      } else {
         wob = ratio < 0.4F ? 2.5F + (0.4F - ratio) / 0.4F * 17.0F : 1.2F + (1.0F - ratio) * 2.5F;
         float lw = e.getWobble();
         if (lw > 0.0F) {
            wob += lw * (16.0F + 6.0F * Mth.sin(t * 0.9F));
         }
      }

      float tx = Mth.cos(prec) * wob;
      float tz = Mth.sin(prec) * wob;
      if (sp > 0.02F && state != 2) {
         float lean = Math.min(17.0F, sp * 48.0F);
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
               Entity tgt = e.level().getEntity(e.getTargetId());
               if (tgt != null) {
                  double dx = tgt.getX() - e.getX();
                  double dz = tgt.getZ() - e.getZ();
                  double dl = Math.sqrt(dx * dx + dz * dz);
                  if (dl > 0.001) {
                     tx = (float)(dz / dl) * 40.0F;
                     tz = (float)(-dx / dl) * 40.0F;
                  }
               }
            }
         } else if (leone) {
            if (ab == 3 && at < 12.0F) {
               lift = -0.18F * Mth.clamp(at / 4.0F, 0.0F, 1.0F);
               jitter = Mth.sin(t * 14.0F) * 0.02F;
            } else if ((ab == 2 || ab == 5) && at > 2.0F) {
               Entity tgt = e.level().getEntity(e.getTargetId());
               if (tgt != null) {
                  double dx = tgt.getX() - e.getX();
                  double dz = tgt.getZ() - e.getZ();
                  double dl = Math.sqrt(dx * dx + dz * dz);
                  if (dl > 0.001) {
                     float k = Mth.clamp((at - 2.0F) / 6.0F, 0.0F, 1.0F) * 22.0F;
                     tx = (float)(dz / dl) * k;
                     tz = (float)(-dx / dl) * k;
                  }
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
               Entity tgt = e.level().getEntity(e.getTargetId());
               if (tgt != null) {
                  double dx = tgt.getX() - e.getX();
                  double dz = tgt.getZ() - e.getZ();
                  double dl = Math.sqrt(dx * dx + dz * dz);
                  if (dl > 0.001) {
                     float k = Mth.clamp((at - 3.0F) / 10.0F, 0.0F, 1.0F) * 30.0F;
                     tx = (float)(dz / dl) * k;
                     tz = (float)(-dx / dl) * k;
                  }
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
            Entity tgtx = e.level().getEntity(e.getTargetId());
            if (tgtx != null) {
               double dx = tgtx.getX() - e.getX();
               double dz = tgtx.getZ() - e.getZ();
               double dl = Math.sqrt(dx * dx + dz * dz);
               if (dl > 0.001) {
                  float k = Mth.clamp((at - 4.0F) / 8.0F, 0.0F, 1.0F) * 34.0F;
                  tx = (float)(dz / dl) * k;
                  tz = (float)(-dx / dl) * k;
               }
            }

            if (at > 32.0F) {
               jitter = Mth.sin(t * 17.0F) * 0.03F;
            }
         }
      } else if (ab != 0) {
         jitter = Mth.sin(t * 13.0F) * 0.015F;
      }

      float tilt = (float)Math.sqrt((double)(tx * tx + tz * tz));
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
         if (pegasus) {
            if (ab != 0) {
               glow = 0.8F + 0.2F * Mth.sin(t * 1.0F);
            } else if (e.isShielded()) {
               glow = 0.65F + 0.25F * Mth.sin(t * 0.9F);
            }
         } else if (eagle) {
            if (ab != 0) {
               glow = 0.8F + 0.2F * Mth.sin(t * 1.0F);
            } else if (e.isShielded()) {
               glow = 0.6F + 0.25F * Mth.sin(t * 0.9F);
            }
         } else if (leone) {
            if (ab != 0) {
               glow = 0.8F + 0.2F * Mth.sin(t * 1.0F);
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
               case 3:
                  glowCol = new float[]{1.0F, 0.55F, 0.15F};
                  break;
               case 4:
                  glowCol = new float[]{0.5F, 1.0F, 0.95F};
                  break;
               case 5:
                  glowCol = new float[]{1.0F, 0.85F, 0.4F};
            }
         } else if (e.isShielded()) {
            glow = 0.45F + 0.15F * Mth.sin(t * 0.5F);
         }
      } else if (ab != 0) {
         glow = 0.85F + 0.15F * Mth.sin(t * 1.1F);
      } else if (e.isShielded()) {
         glow = 0.55F + 0.2F * Mth.sin(t * 0.8F);
      }

      if (state == 1) {
         glow = 0.0F;
      }

      float flash = hitAge >= 0.0F && hitAge < 4.0F ? 1.0F - hitAge / 4.0F : 0.0F;
      int overlay = OverlayTexture.pack(OverlayTexture.u(flash * 0.7F), OverlayTexture.v(false));
      if (sp > 0.17F || state == 2) {
         this.renderTrail(e, ex, ey, ez, pose, buf, light, sp);
      }

      pose.pushPose();
      pose.translate(0.0F, lift + jitter, 0.0F);
      if (tilt > 0.01F) {
         pose.mulPose(new Quaternionf().rotateAxis(tilt * (float) (Math.PI / 180.0), tx / tilt, 0.0F, tz / tilt));
      }

      pose.scale(0.45F, 0.45F, 0.45F);
      pose.pushPose();
      pose.mulPose(Axis.YP.rotationDegrees(-spin * (float)dir));
      mesh.renderAssembled(pose, buf, light, overlay, parts);
      float g = glow + flash * 0.8F;
      mesh.renderGlowAssembled(pose, buf, glowCol[0] * g, glowCol[1] * g, glowCol[2] * g, parts);
      pose.popPose();
      if (state == 0 && ratio > 0.08F) {
         float speed = BeybladeEntity.spinSpeedFor(ratio);
         float a = 0.4F * Math.min(1.0F, ratio * 1.6F);

         for (int k = 1; k <= 6; k++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees((-spin + speed * 0.09F * (float)k) * (float)dir));
            mesh.renderGhostDisc(pose, buf, light, a * (1.0F - (float)k * 0.14F), mesh.TOP_Y + partLift + 0.006F * (float)k);
            pose.popPose();
         }

         VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
         Matrix4f m = pose.last().pose();
         float k = 0.35F * ratio;
         float[] streak = common != null
            ? common.variant().rgb(true)
            : (
               tempo
                  ? new float[]{0.75F, 0.85F, 1.0F}
                  : (
                     variares
                        ? new float[]{1.0F, 0.3F, 0.25F}
                        : (
                           beffal
                              ? new float[]{1.0F, 0.45F, 0.8F}
                              : (
                                 drago
                                    ? new float[]{1.0F, 0.45F, 0.5F}
                                    : (
                                       leone
                                          ? new float[]{0.6F, 1.0F, 0.6F}
                                          : (eagle ? new float[]{0.8F, 0.6F, 1.0F} : (pegasus ? new float[]{0.6F, 0.85F, 1.0F} : new float[]{0.8F, 1.0F, 0.8F}))
                                    )
                              )
                        )
                  )
            );

         for (int i = 0; i < 3; i++) {
            float a0 = -spin * (float)dir * (float) (Math.PI / 180.0) * 0.5F + (float)i * (float) (Math.PI * 2) / 3.0F;
            Fx.helix(
               vc,
               m,
               1.08F,
               1.08F,
               1.06F,
               1.06F,
               a0,
               -0.16F * (float)dir,
               0.22F,
               8,
               Fx.c(streak[0], streak[1], streak[2], k),
               Fx.c(streak[0], streak[1], streak[2], 0.0F),
               t * 0.1F
            );
         }
      }

      pose.popPose();
      float gr = t - (float)e.grindTime;
      if (gr >= 0.0F && gr < 5.0F) {
         this.renderSparks(e, gr, t, pose, buf);
      }

      float ev = t - (float)e.evadeTime;
      if (ev >= 0.0F && ev < 10.0F) {
         float f = 1.0F - ev / 10.0F;
         VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
         Fx.ring(
            vc,
            pose.last().pose(),
            0.25F,
            0.3F + ev * 0.12F,
            0.6F + ev * 0.18F,
            32,
            new float[]{0.0F, 0.0F, 0.0F},
            Fx.c(0.8F, 0.95F, 1.0F, 1.2F * f),
            3.0F,
            0.0F
         );
         vc = Fx.buf(buf, Fx.SOFT);
         Fx.band(
            vc,
            pose.last().pose(),
            0.5F,
            0.0F,
            0.7F,
            1.1F * f,
            24,
            Fx.c(glowCol[0], glowCol[1], glowCol[2], 0.7F * f),
            new float[]{0.0F, 0.0F, 0.0F},
            1.0F,
            0.0F,
            1.0F,
            0.0F
         );
      }

      float sh = t - (float)e.shockTime;
      if (sh >= 0.0F && sh < 14.0F) {
         this.renderShock(e, sh, ex, ey, ez, pose, buf);
      }

      float sa = t - (float)e.spiritTime;
      if (e.isSpiritActive() || sa >= 0.0F && sa < 24.0F) {
         this.renderSpirit(e, t, sa, pose, buf, glowCol);
      }

      if (beffal) {
         BeffalFx.render((BeffalEntity)e, partial, t, ex, ey, ez, pose, buf, this.entityRenderDispatcher.cameraOrientation());
      } else if (variares) {
         VariaresFx.render((VariaresEntity)e, partial, t, ex, ey, ez, pose, buf, this.entityRenderDispatcher.cameraOrientation());
      } else if (tempo) {
         TempoFx.render((TempoEntity)e, partial, t, ex, ey, ez, pose, buf, this.entityRenderDispatcher.cameraOrientation());
      } else if (common != null) {
         CommonFx.render(common, partial, t, ex, ey, ez, pose, buf, this.entityRenderDispatcher.cameraOrientation());
      } else if (pegasus) {
         PegasusFx.render((PegasusEntity)e, partial, t, ex, ey, ez, pose, buf, this.entityRenderDispatcher.cameraOrientation());
      } else if (eagle) {
         EagleFx.render((EagleEntity)e, partial, t, ex, ey, ez, pose, buf, this.entityRenderDispatcher.cameraOrientation());
      } else if (leone) {
         LeoneFx.render((LeoneEntity)e, partial, t, ex, ey, ez, pose, buf, this.entityRenderDispatcher.cameraOrientation(), light);
      } else if (drago) {
         LDragoFx.render((LDragoEntity)e, partial, t, ex, ey, ez, pose, buf, this.entityRenderDispatcher.cameraOrientation());
      } else {
         LibraFx.render(e, partial, t, ex, ey, ez, pose, buf, this.entityRenderDispatcher.cameraOrientation());
      }

      SpecialClashFx.render(e, partial, t, ex, ey, ez, pose, buf, this.entityRenderDispatcher.cameraOrientation());
      XtremeFx.render(e, partial, t, ex, ey, ez, pose, buf, this.entityRenderDispatcher.cameraOrientation());
      super.render(e, yaw, partial, pose, buf, light);
   }

   private void renderSparks(BeybladeEntity e, float age, float t, PoseStack pose, MultiBufferSource buf) {
      BeybladeEntity other = null;
      double best = 2.5;

      for (BeybladeEntity b : e.level().getEntitiesOfClass(BeybladeEntity.class, e.getBoundingBox().inflate(1.6), bx -> bx != e)) {
         double d = b.distanceToSqr(e);
         if (d < best) {
            best = d;
            other = b;
         }
      }

      if (other != null) {
         Vec3 dir = new Vec3(other.getX() - e.getX(), 0.0, other.getZ() - e.getZ());
         if (!(dir.lengthSqr() < 1.0E-4)) {
            dir = dir.normalize();
            Vec3 c = dir.scale(0.45F).add(0.0, 0.38, 0.0);
            Vec3 side = new Vec3(-dir.z, 0.0, dir.x).scale((double)e.spinDir());
            VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
            Matrix4f m = pose.last().pose();
            float f = 1.0F - age / 5.0F;
            int seed = (int)(t * 3.0F) + e.getId() * 31;

            for (int i = 0; i < 14; i++) {
               float h1 = hash(seed + i * 17);
               float h2 = hash(seed + i * 29);
               float h3 = hash(seed + i * 43);
               Vec3 d = side.scale(0.6 + (double)h1).add(dir.scale((double)h2 - 0.5)).add(0.0, 0.2 + (double)h3 * 0.6, 0.0).normalize();
               float len = 0.25F + h2 * 0.6F;
               Vec3 a = c.add(d.scale((double)age * 0.12));
               Vec3 bx = a.add(d.scale((double)len)).add(0.0, (double)(-age * age) * 0.01, 0.0);
               float k = f * (0.6F + 0.4F * h3);
               Fx.beam(vc, m, a, bx, 0.07F, Fx.c(1.0F, 0.95F, 0.7F, 1.4F * k), Fx.c(1.0F, 0.45F, 0.1F, 0.6F * k), 1.0F, 0.0F);
            }

            vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(
               vc, pose, Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation(), c.x, c.y, c.z, 0.9F * f, t * 20.0F, Fx.c(1.0F, 0.8F, 0.4F, 1.2F * f)
            );
         }
      }
   }

   private static float hash(int n) {
      n = n << 13 ^ n;
      return (float)(n * (n * n * 15731 + 789221) + 1376312589 & 2147483647) / 2.1474836E9F;
   }

   private void renderShock(BeybladeEntity e, float age, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf) {
      float f = 1.0F - age / 14.0F;
      float k = 1.0F - (1.0F - Math.min(1.0F, age / 11.0F)) * (1.0F - Math.min(1.0F, age / 11.0F)) * (1.0F - Math.min(1.0F, age / 11.0F));
      float r = 0.5F + k * 5.5F;
      float base = (float)Math.atan2(e.shockDirZ, e.shockDirX);
      float cx = (float)(e.shockX - ex);
      float cy = (float)(e.shockY - ey) + 0.3F;
      float cz = (float)(e.shockZ - ez);
      Matrix4f m = pose.last().pose();
      VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
      int N = 18;
      float span = 1.15F;
      float[] cIn = Fx.c(0.75F, 0.9F, 1.0F, 0.0F);
      float[] cOut = Fx.c(0.8F, 0.92F, 1.0F, 1.3F * f);
      float[] wall = Fx.c(0.85F, 0.95F, 1.0F, 0.9F * f);
      float[] top = Fx.c(0.0F, 0.0F, 0.0F, 0.0F);
      float h = 1.1F * f + 0.2F;

      for (int i = 0; i < N; i++) {
         float a0 = base - span + 2.0F * span * (float)i / (float)N;
         float a1 = base - span + 2.0F * span * (float)(i + 1) / (float)N;
         float w0 = 1.0F - Math.abs((float)i / (float)N * 2.0F - 1.0F);
         float w1 = 1.0F - Math.abs((float)(i + 1) / (float)N * 2.0F - 1.0F);
         float ri = Math.max(0.0F, r - 1.3F);
         float[] o0 = Fx.c(cOut[0], cOut[1], cOut[2], w0);
         float[] o1 = Fx.c(cOut[0], cOut[1], cOut[2], w1);
         Fx.quad(
            vc,
            m,
            new float[]{cx + Mth.cos(a0) * ri, cy, cz + Mth.sin(a0) * ri},
            new float[]{cx + Mth.cos(a0) * r, cy, cz + Mth.sin(a0) * r},
            new float[]{cx + Mth.cos(a1) * r, cy, cz + Mth.sin(a1) * r},
            new float[]{cx + Mth.cos(a1) * ri, cy, cz + Mth.sin(a1) * ri},
            (float)i / (float)N,
            0.0F,
            (float)(i + 1) / (float)N,
            1.0F,
            cIn,
            Fx.lerp(0.5F, o0, o1)
         );
         float[] wc = Fx.c(wall[0], wall[1], wall[2], (w0 + w1) * 0.5F);
         Fx.quad(
            vc,
            m,
            new float[]{cx + Mth.cos(a0) * r, cy - 0.2F, cz + Mth.sin(a0) * r},
            new float[]{cx + Mth.cos(a0) * r, cy + h, cz + Mth.sin(a0) * r},
            new float[]{cx + Mth.cos(a1) * r, cy + h, cz + Mth.sin(a1) * r},
            new float[]{cx + Mth.cos(a1) * r, cy - 0.2F, cz + Mth.sin(a1) * r},
            (float)i / (float)N,
            1.0F,
            (float)(i + 1) / (float)N,
            0.0F,
            wc,
            top
         );
      }
   }

   private void renderSpirit(BeybladeEntity e, float t, float sa, PoseStack pose, MultiBufferSource buf, float[] col) {
      float in = sa >= 0.0F && sa < 24.0F ? 1.0F - sa / 24.0F : 0.0F;
      float on = e.isSpiritActive() ? Math.min(1.0F, (float)e.spiritRemaining() / 20.0F) : 0.0F;
      Matrix4f m = pose.last().pose();
      VertexConsumer vc = Fx.buf(buf, Fx.WISP);
      float[] gold = new float[]{1.0F, 0.8F, 0.3F};
      float a = on * (0.6F + 0.2F * Mth.sin(t * 0.8F));
      if (a > 0.01F) {
         Fx.band(vc, m, 0.55F, 0.0F, 0.9F, 2.2F, 24, Fx.c(gold[0], gold[1], gold[2], a), new float[]{0.0F, 0.0F, 0.0F}, 2.0F, t * 0.15F, 1.0F, -t * 0.08F);
         Fx.band(vc, m, 0.5F, 0.0F, 0.6F, 1.4F, 24, Fx.c(col[0], col[1], col[2], a * 0.8F), new float[]{0.0F, 0.0F, 0.0F}, 3.0F, -t * 0.2F, 1.0F, -t * 0.1F);
      }

      if (in > 0.01F) {
         vc = Fx.buf(buf, Fx.WAVE);
         float r = 0.6F + (1.0F - in) * 4.0F;
         Fx.ring(vc, m, 0.1F, r - 0.9F, r, 48, new float[]{0.0F, 0.0F, 0.0F}, Fx.c(gold[0], gold[1], gold[2], 1.4F * in), 4.0F, 0.0F);
         vc = Fx.buf(buf, Fx.SOFT);
         Fx.band(vc, m, 0.3F, 0.0F, 0.1F, 8.0F * in, 16, Fx.c(gold[0], gold[1], gold[2], in), new float[]{0.0F, 0.0F, 0.0F}, 1.0F, 0.0F, 1.0F, 0.0F);
      }
   }

   private void renderTrail(BeybladeEntity e, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, int light, float sp) {
      int n = 5;
      float base = e.getState() == 2 ? 0.25F : Mth.clamp((sp - 0.17F) * 2.5F, 0.0F, 0.3F);
      if (!(base <= 0.01F)) {
         for (int i = 1; i <= n; i++) {
            float k = 1.0F - (float)i / (float)(n + 1);
            pose.pushPose();
            pose.translate(e.trailX[i] - ex, e.trailY[i] - ey, e.trailZ[i] - ez);
            pose.scale(0.45F, 0.45F, 0.45F);
            pose.mulPose(Axis.YP.rotationDegrees((-e.spinAngle + (float)i * 40.0F) * (float)e.spinDir()));
            BeyMesh mesh = BeyMesh.of(e);
            mesh.renderGhostDisc(pose, buf, light, base * k, mesh.TOP_Y - 0.15F);
            pose.popPose();
         }

         VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
         Matrix4f m = pose.last().pose();
         float[] col = e instanceof CommonBeybladeEntity c
            ? c.variant().rgb(true)
            : (
               e instanceof TempoEntity
                  ? new float[]{0.7F, 0.82F, 1.0F}
                  : (
                     e instanceof VariaresEntity
                        ? new float[]{1.0F, 0.15F, 0.1F}
                        : (
                           e instanceof BeffalEntity
                              ? new float[]{1.0F, 0.35F, 0.75F}
                              : (
                                 e instanceof PegasusEntity
                                    ? new float[]{0.4F, 0.75F, 1.0F}
                                    : (
                                       e instanceof EagleEntity
                                          ? new float[]{0.7F, 0.45F, 1.0F}
                                          : (
                                             e instanceof LeoneEntity
                                                ? new float[]{0.4F, 1.0F, 0.5F}
                                                : (
                                                   e instanceof LDragoEntity
                                                      ? new float[]{1.0F, 0.15F, 0.25F}
                                                      : (e.isRival() ? new float[]{1.0F, 0.3F, 0.2F} : new float[]{0.5F, 1.0F, 0.5F})
                                                )
                                          )
                                    )
                              )
                        )
                  )
            );

         for (int i = 0; i < n; i++) {
            Vec3 a = new Vec3(e.trailX[i] - ex, e.trailY[i] - ey + 0.32, e.trailZ[i] - ez);
            Vec3 b = new Vec3(e.trailX[i + 1] - ex, e.trailY[i + 1] - ey + 0.32, e.trailZ[i + 1] - ez);
            if (i == 0) {
               a = new Vec3(0.0, 0.32, 0.0);
            }

            float k0 = (1.0F - (float)i / (float)n) * base * 2.2F;
            float k1 = (1.0F - (float)(i + 1) / (float)n) * base * 2.2F;
            Fx.beam(vc, m, a, b, 0.55F, Fx.c(col[0], col[1], col[2], k0), Fx.c(col[0], col[1], col[2], k1), 0.5F, 0.0F);
         }
      }
   }

   public ResourceLocation getTextureLocation(BeybladeEntity e) {
      return BeyMesh.of(e).FACE;
   }
}
