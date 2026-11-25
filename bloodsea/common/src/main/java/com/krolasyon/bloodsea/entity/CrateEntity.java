package com.krolasyon.bloodsea.entity;

import com.krolasyon.bloodsea.story.Story;
import com.krolasyon.bloodsea.world.Waves;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/** A crate of someone's cargo, riding the swell. Use it from the raft, or row into it, to haul it aboard. */
public class CrateEntity extends com.krolasyon.bloodsea.compat.SimpleEntity {
    public CrateEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override public boolean isPickable() { return !isRemoved(); }
    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 160 * 160; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        setPos(getX() + Math.sin(tickCount * 0.01 + getId()) * 0.006, Waves.MEAN - 0.35, getZ() + Math.cos(tickCount * 0.013 + getId()) * 0.006);
        // a raft running into it brings it aboard
        for (RaftEntity r : level().getEntitiesOfClass(RaftEntity.class, getBoundingBox().inflate(3.4))) {
            if (r.getControllingPassenger() instanceof ServerPlayer sp) { haul(sp); return; }
        }
        if (tickCount > 20 * 60 * 15) discard();
    }

    @Override
    public InteractionResult interact(Player p, InteractionHand hand) {
        if (!level().isClientSide && p instanceof ServerPlayer sp) haul(sp);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    void haul(ServerPlayer p) {
        if (isRemoved()) return;
        ItemStack[] loot = switch (random.nextInt(5)) {
            case 0 -> new ItemStack[]{new ItemStack(Items.BREAD, 3)};
            case 1 -> new ItemStack[]{new ItemStack(Items.COOKED_COD, 2), new ItemStack(Items.BREAD, 1)};
            case 2 -> new ItemStack[]{new ItemStack(Items.APPLE, 3)};
            case 3 -> new ItemStack[]{new ItemStack(Items.DRIED_KELP, 6), new ItemStack(Items.BREAD, 1)};
            default -> new ItemStack[]{new ItemStack(Items.BAKED_POTATO, 3)};
        };
        for (ItemStack s : loot) if (!p.getInventory().add(s)) p.drop(s, false);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.BARREL_OPEN, SoundSource.PLAYERS, 1f, 0.8f);
        ((ServerLevel) level()).sendParticles(new DustParticleOptions(new Vector3f(0.45f, 0.02f, 0.03f), 1.5f), getX(), getY() + 0.5, getZ(), 25, 0.4, 0.2, 0.4, 0.1);
        Story.onCrate(p);
        discard();
    }

    @Override protected void readAdditionalSaveData(CompoundTag t) {}
    @Override protected void addAdditionalSaveData(CompoundTag t) {}
}
