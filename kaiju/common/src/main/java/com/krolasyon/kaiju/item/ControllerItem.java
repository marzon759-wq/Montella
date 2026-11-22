package com.krolasyon.kaiju.item;

import com.krolasyon.kaiju.entity.KaijuEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/** right-click: take control of the nearest kaiju you own (any kaiju in creative); again to let go */
public class ControllerItem extends Item {
    public ControllerItem(Properties p) { super(p); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(st);
        if (sp.getVehicle() instanceof KaijuEntity k) {
            k.releaseControl();
            sp.displayClientMessage(Component.translatable("msg.kaiju.released"), true);
            return InteractionResultHolder.consume(st);
        }
        take(sp);
        return InteractionResultHolder.consume(st);
    }

    public static boolean take(ServerPlayer sp) {
        KaijuEntity best = sp.level().getEntitiesOfClass(KaijuEntity.class, sp.getBoundingBox().inflate(600),
                        k -> k.state != KaijuEntity.State.DEAD && (k.owner == null || k.owner.equals(sp.getUUID()) || sp.getAbilities().instabuild))
                .stream().min(Comparator.comparingDouble(k -> k.distanceToSqr(sp))).orElse(null);
        if (best == null) {
            sp.displayClientMessage(Component.translatable("msg.kaiju.none"), true);
            return false;
        }
        best.takeControl(sp);
        sp.displayClientMessage(Component.translatable("msg.kaiju.controlling"), true);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("item.kaiju.controller.tip"));
    }
}
