package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public class SpinSound extends AbstractTickableSoundInstance {
   private final BeybladeEntity bey;

   public SpinSound(BeybladeEntity bey) {
      super((SoundEvent)ModSounds.SPIN_LOOP.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
      this.bey = bey;
      this.looping = true;
      this.delay = 0;
      this.volume = 0.01F;
      this.pitch = 1.0F;
      this.x = bey.getX();
      this.y = bey.getY();
      this.z = bey.getZ();
   }

   public void tick() {
      if (this.bey.isRemoved()) {
         this.stop();
      } else {
         this.x = this.bey.getX();
         this.y = this.bey.getY();
         this.z = this.bey.getZ();
         float ratio = this.bey.spinRatio();
         float targetVol;
         float targetPitch;
         switch (this.bey.getState()) {
            case 1:
               targetVol = 0.0F;
               targetPitch = 0.5F;
               break;
            case 2:
               targetVol = 0.35F;
               targetPitch = 1.6F;
               break;
            default:
               targetVol = 0.25F + 0.45F * ratio;
               targetPitch = 0.65F + 0.85F * ratio;
               if (this.bey.getAbility() != 0) {
                  targetVol += 0.2F;
                  targetPitch += 0.25F;
               }
         }

         this.volume = Mth.lerp(0.2F, this.volume, targetVol);
         this.pitch = Mth.clamp(Mth.lerp(0.2F, this.pitch, targetPitch), 0.5F, 2.0F);
      }
   }
}
