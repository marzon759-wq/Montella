package com.krolasyon.beyblade.item;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.entity.Px;
import com.krolasyon.beyblade.entity.StadiumEntity;
import com.krolasyon.beyblade.parts.Loadout;
import com.krolasyon.beyblade.registry.ModSounds;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class LauncherItem extends Item {
   public static final int FULL_CHARGE = 40;
   private final Supplier<? extends Item> beyItem;
   private final Supplier<? extends EntityType<? extends BeybladeEntity>> beyType;

   public LauncherItem(Properties props, Supplier<? extends Item> beyItem, Supplier<? extends EntityType<? extends BeybladeEntity>> beyType) {
      super(props);
      this.beyItem = beyItem;
      this.beyType = beyType;
   }

   public boolean isStandard() {
      return this.beyItem == null;
   }

   public int findLibra(Player p) {
      int off = p.getInventory().getContainerSize() - 1;
      ItemStack offStack = p.getInventory().getItem(off);
      if (this.isStandard() && offStack.getItem() instanceof LibraItem && complete(offStack)) {
         return off;
      } else {
         for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if ((this.isStandard() ? s.getItem() instanceof LibraItem : s.is(this.beyItem.get())) && complete(s)) {
               return i;
            }
         }

         return -1;
      }
   }

   private static boolean complete(ItemStack s) {
      return Loadout.of(s).complete();
   }

   private boolean hasIncomplete(Player p) {
      for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
         ItemStack s = p.getInventory().getItem(i);
         if ((this.isStandard() ? s.getItem() instanceof LibraItem : s.is(this.beyItem.get())) && !complete(s)) {
            return true;
         }
      }

      return false;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      BeybladeEntity active = BeybladeEntity.findActive(player);
      if (active != null) {
         if (!level.isClientSide()) {
            if (player.isShiftKeyDown()) {
               active.recall();
            } else {
               player.displayClientMessage(Component.translatable("msg.librabeyblade.already"), true);
            }
         }

         return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
      } else if (this.findLibra(player) >= 0) {
         player.startUsingItem(hand);
         LaunchPlan.reset(player);
         if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), (SoundEvent)ModSounds.CHARGE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
         }

         return InteractionResultHolder.consume(stack);
      } else {
         if (!level.isClientSide() && this.hasIncomplete(player)) {
            player.displayClientMessage(Component.translatable("msg.librabeyblade.incomplete").withStyle(ChatFormatting.RED), true);
         } else if (!level.isClientSide()) {
            player.displayClientMessage(
               this.isStandard()
                  ? Component.translatable("msg.librabeyblade.no_bey_any")
                  : Component.translatable("msg.librabeyblade.no_bey", new Object[]{this.beyItem.get().getDescription()}),
               true
            );
         }

         return InteractionResultHolder.fail(stack);
      }
   }

   public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
      int used = this.getUseDuration(stack) - remaining;
      if (entity instanceof Player pl) {
         LaunchPlan.track(pl);
      }

      if (entity instanceof Player p && !level.isClientSide()) {
         if (used == 65) {
            level.playSound(null, p.getX(), p.getY(), p.getZ(), (SoundEvent)ModSounds.GRIND.get(), SoundSource.PLAYERS, 0.6F, 0.7F);
         }

         if (used == 40) {
            level.playSound(null, p.getX(), p.getY(), p.getZ(), (SoundEvent)ModSounds.LAND.get(), SoundSource.PLAYERS, 0.8F, 1.6F);
            Px.send((ServerLevel)level, ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getEyeY() - 0.3, p.getZ(), 10, 0.3, 0.2, 0.3, 0.1);
         }
      }
   }

   public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
      if (entity instanceof Player player && !level.isClientSide()) {
         int used = this.getUseDuration(stack) - remaining;
         if (used < 4) {
            return;
         }

         int slot = this.findLibra(player);
         if (slot >= 0 && BeybladeEntity.findActive(player) == null) {
            LaunchPlan plan = LaunchPlan.of(player, used);
            float power = plan.power;
            ItemStack libra = player.getInventory().getItem(slot);
            ItemStack libraType = libra.copy();
            ItemStack launched = libra.copy();
            launched.setCount(1);
            libra.shrink(1);
            Vec3 look = player.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0.0, look.z);
            if (flat.lengthSqr() < 1.0E-4) {
               flat = new Vec3(0.0, 0.0, 1.0);
            }

            flat = flat.normalize();
            Vec3 pos = player.getEyePosition().add(flat.scale(1.0)).add(0.0, -0.5, 0.0);
            Vec3 vel = plan.velocity(player);

            for (StadiumEntity st : level.getEntitiesOfClass(StadiumEntity.class, player.getBoundingBox().inflate(24.0))) {
               double r = Math.hypot(player.getX() - st.getX(), player.getZ() - st.getZ());
               if (r > 10.1F) {
                  Vec3 toC = new Vec3(st.getX() - pos.x, 0.0, st.getZ() - pos.z);
                  double d = toC.length();
                  if (!(d < 0.001) && !(toC.scale(1.0 / d).dot(flat) < 0.5)) {
                     double h = Math.max(0.5, 3.95 - (pos.y - st.getY()));
                     double vy = Math.sqrt(0.12 * h) + 0.05;
                     double ticks = vy / 0.06 * 2.0;
                     double vh = Math.min(0.95, Math.max(0.3, (d - 3.0) / ticks)) * (0.85 + 0.15 * (double)power);
                     vel = flat.scale(vh).add(0.0, vy, 0.0).add(plan.curve(player));
                     break;
                  }
               }
            }

            EntityType<? extends BeybladeEntity> type = this.isStandard() && libraType.getItem() instanceof LibraItem li ? li.spawnType() : this.beyType.get();
            boolean perfect = plan.perfect;
            boolean powerLaunch = plan.powerLaunch;
            if (powerLaunch) {
               vel = vel.multiply(1.35, 1.0, 1.35);
            }

            BeybladeEntity bey = BeybladeEntity.launch(type, level, player, launched, pos, vel, 0.55F + 0.45F * power);
            bey.launchBonus(perfect, powerLaunch);
            bey.setLaunchWobble(plan.wobble);
            int steady = Math.round((1.0F - plan.wobble) * 100.0F);
            if (plan.wobble >= 0.6F) {
               player.displayClientMessage(
                  Component.translatable(
                        "msg.librabeyblade.launch_bad",
                        new Object[]{steady, Component.translatable("launch.librabeyblade.hint." + (plan.hint == null ? "shaky" : plan.hint))}
                     )
                     .withStyle(ChatFormatting.RED),
                  false
               );
            } else if (plan.wobble >= 0.3F) {
               player.displayClientMessage(
                  Component.translatable(
                        "msg.librabeyblade.launch_shaky",
                        new Object[]{steady, Component.translatable("launch.librabeyblade.hint." + (plan.hint == null ? "shaky" : plan.hint))}
                     )
                     .withStyle(ChatFormatting.YELLOW),
                  false
               );
            }

            if (perfect) {
               player.displayClientMessage(
                  Component.translatable("msg.librabeyblade.perfect_launch").withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}), false
               );
            }

            if (powerLaunch) {
               player.displayClientMessage(Component.translatable("msg.librabeyblade.power_launch").withStyle(ChatFormatting.RED), false);
            }

            level.playSound(
               null,
               player.getX(),
               player.getY(),
               player.getZ(),
               (SoundEvent)ModSounds.LAUNCH.get(),
               SoundSource.PLAYERS,
               1.2F,
               0.95F + power * 0.1F
            );
            if (!perfect && !powerLaunch) {
               player.displayClientMessage(Component.translatable("msg.librabeyblade.launch", new Object[]{Math.round(power * 100.0F)}), true);
            }

            player.getCooldowns().addCooldown(this, 10);
            return;
         }

         return;
      }
   }

   public int getUseDuration(ItemStack stack) {
      return 72000;
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.BOW;
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
      tip.add(Component.translatable(this.getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
      tip.add(Component.translatable("item.librabeyblade.libra_launcher.desc2").withStyle(ChatFormatting.DARK_GRAY));
   }
}
