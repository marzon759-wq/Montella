package com.krolasyon.beyblade.item;

import com.krolasyon.beyblade.entity.StadiumEntity;
import com.krolasyon.beyblade.registry.ModEntities;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class StadiumItem extends com.krolasyon.beyblade.compat.BeyItem {
   public StadiumItem(Properties props) {
      super(props);
   }

   public InteractionResult useOn(UseOnContext ctx) {
      Level level = ctx.getLevel();
      BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
      if (!level.isClientSide()) {
         Player player = ctx.getPlayer();
         double x = (double)pos.getX() + 0.5;
         double y = (double)pos.getY();
         double z = (double)pos.getZ() + 0.5;
         if (!level.getEntitiesOfClass(StadiumEntity.class, new AABB(x - 18.0, y - 4.0, z - 18.0, x + 18.0, y + 4.0, z + 18.0)).isEmpty()) {
            if (player != null) {
               com.krolasyon.beyblade.Msg.say(player, Component.translatable("msg.librabeyblade.stadium_near"), true);
            }

            return InteractionResult.FAIL;
         }

         StadiumEntity st = (StadiumEntity)((EntityType)ModEntities.STADIUM.get()).create(level);
         if (st == null) {
            return InteractionResult.FAIL;
         }

         st.moveTo(x, y, z, 0.0F, 0.0F);
         level.addFreshEntity(st);
         level.playSound(null, x, y, z, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.6F, 1.4F);
         if (player == null || !player.getAbilities().instabuild) {
            ctx.getItemInHand().shrink(1);
         }
      }

      return InteractionResult.sidedSuccess(level.isClientSide());
   }

   @Override
   protected void tooltip(ItemStack stack, List<Component> tip, TooltipFlag flag) {
      tip.add(Component.translatable("item.librabeyblade.stadium.desc").withStyle(ChatFormatting.GRAY));
      tip.add(Component.translatable("item.librabeyblade.stadium.desc2").withStyle(ChatFormatting.DARK_GRAY));
   }
}
