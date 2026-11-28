package com.krolasyon.beyblade.compat;

import com.krolasyon.beyblade.client.BeyAnims;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/** GeckoLib plumbing for every beyblade (Forge 1.21.1 / GeckoLib 4.8). The animation choices live in {@link BeyAnims}. */
public abstract class GeoBeyBase extends DataEntity implements GeoEntity {
   private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

   protected GeoBeyBase(EntityType<?> type, Level level) {
      super(type, level);
   }

   /** Plays a one shot animation on the action controller. Safe on both sides (client plays it directly). */
   public void playAction(String name) {
      this.triggerAnim(BeyAnims.ACTION, name);
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar reg) {
      reg.add(new AnimationController<>(this, BeyAnims.BODY, 4, st -> {
         String a = BeyAnims.bodyLoop((com.krolasyon.beyblade.entity.BeybladeEntity)(Object)this);
         return st.setAndContinue(BeyAnims.hold(a) ? RawAnimation.begin().thenPlayAndHold(BeyAnims.full(a)) : RawAnimation.begin().thenLoop(BeyAnims.full(a)));
      }));
      reg.add(new AnimationController<>(this, BeyAnims.TIP, 3, st ->
         st.setAndContinue(RawAnimation.begin().thenLoop(BeyAnims.full(BeyAnims.tipLoop((com.krolasyon.beyblade.entity.BeybladeEntity)(Object)this))))));
      AnimationController<GeoBeyBase> act = new AnimationController<>(this, BeyAnims.ACTION, 1, st -> PlayState.STOP);
      for (String a : BeyAnims.ACTIONS) {
         act.triggerableAnim(a, RawAnimation.begin().thenPlay(BeyAnims.full(a)));
      }
      reg.add(act);
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.geoCache;
   }

   @Override
   public double getTick(Object object) {
      return this.tickCount;
   }

   /** Client position interpolation (the bey smooths server positions itself). */
   protected void lerpToImpl(double x, double y, double z) {
      this.setPos(x, y, z);
   }

   @Override
   public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
      this.lerpToImpl(x, y, z);
   }
}
