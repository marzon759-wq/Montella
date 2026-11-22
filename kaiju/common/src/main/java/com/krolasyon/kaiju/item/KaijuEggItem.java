package com.krolasyon.kaiju.item;

import com.krolasyon.kaiju.KaijuConfig;
import com.krolasyon.kaiju.entity.KaijuEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/** hatches a kaiju where it is used; the one who hatched it owns it and can take control with the controller */
public class KaijuEggItem extends Item {
    private final Supplier<? extends EntityType<? extends KaijuEntity>> type;

    public KaijuEggItem(Supplier<? extends EntityType<? extends KaijuEntity>> type, Properties p) {
        super(p);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        BlockPos at = ctx.getClickedPos().relative(ctx.getClickedFace());
        KaijuEntity k = type.get().create(sl);
        if (k == null) return InteractionResult.FAIL;
        Player p = ctx.getPlayer();
        float yaw = p != null ? p.getYRot() + 180 : 0;
        double y = sl.getHeight(Heightmap.Types.OCEAN_FLOOR, at.getX(), at.getZ());
        k.moveTo(at.getX() + 0.5, y, at.getZ() + 0.5, yaw, 0);
        k.setScale((float) KaijuConfig.scale);
        k.faceYaw = yaw;
        if (p != null) k.owner = p.getUUID();
        sl.addFreshEntity(k);
        k.startAttack("roar");
        sl.playSound(null, at, SoundEvents.TURTLE_EGG_HATCH, SoundSource.HOSTILE, 4f, 0.3f);
        if (p != null && !p.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("item.kaiju.egg.tip"));
    }
}
