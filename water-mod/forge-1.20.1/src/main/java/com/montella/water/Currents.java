package com.montella.water;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Water currents with drag: an object in a stream is pulled towards the speed of the water, lighter things faster.
 * Falling water (waterfalls) presses everything inside it downwards. Runs wherever the entity is simulated
 * (server for mobs/items/boats, client for the local player and the boat they steer).
 */
public final class Currents {
    private Currents() {}

    /** speed of the water at full flow, blocks per tick (~3.2 m/s) */
    private static final double STREAM_SPEED = 0.16;

    @SubscribeEvent
    public static void onLiving(LivingEvent.LivingTickEvent e) {
        LivingEntity le = e.getEntity();
        if (!le.isControlledByLocalInstance()) return;
        if (le instanceof Player p && (p.getAbilities().flying || p.isSpectator())) return;
        if (le.isPassenger()) return;
        // swimmers fight the current, big animals are heavier
        double coupling = le instanceof Player ? 0.045 : 0.06 / Math.max(1.0, le.getBbWidth() * le.getBbHeight());
        apply(le, coupling, 1.0);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Level level = e.level;
        if (level.isClientSide) return; // the boat steered by the local player is handled in client.WaterFx
        for (Entity en : ((ServerLevel) level).getAllEntities()) {
            if (!en.isInWater() || !en.isControlledByLocalInstance()) continue;
            if (en instanceof ItemEntity || en instanceof ExperienceOrb) apply(en, 0.12, 1.2);
            else if (en instanceof Boat) apply(en, 0.06, 0.4);
        }
    }

    /** @param coupling how quickly the object takes on the water's speed (drag / mass) */
    public static void apply(Entity en, double coupling, double fallScale) {
        double strength = Config.CURRENT_STRENGTH.get();
        double push = Config.WATERFALL_PUSH.get();
        if (strength <= 0 && push <= 0) return;
        Level level = en.level();
        AABB box = en.getBoundingBox().deflate(0.001);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        Vec3 flow = Vec3.ZERO;
        int n = 0, falling = 0;
        for (int x = (int) Math.floor(box.minX); x <= (int) Math.floor(box.maxX); x++)
            for (int y = (int) Math.floor(box.minY); y <= (int) Math.floor(box.maxY); y++)
                for (int z = (int) Math.floor(box.minZ); z <= (int) Math.floor(box.maxZ); z++) {
                    m.set(x, y, z);
                    FluidState fs = level.getFluidState(m);
                    if (fs.isEmpty() || !fs.is(FluidTags.WATER)) continue;
                    if (y + fs.getHeight(level, m) < box.minY) continue;
                    flow = flow.add(fs.getFlow(level, m));
                    n++;
                    if (fs.hasProperty(FlowingFluid.FALLING) && fs.getValue(FlowingFluid.FALLING)) falling++;
                }
        if (n == 0) return;
        Vec3 v = en.getDeltaMovement();
        double fx = flow.x / n, fz = flow.z / n;
        double flen = Math.sqrt(fx * fx + fz * fz);
        if (flen > 0.01 && strength > 0) {
            // drag towards the water velocity; never slows down something already faster than the stream
            double tx = fx / flen * STREAM_SPEED * Math.min(1.0, flen) * strength;
            double tz = fz / flen * STREAM_SPEED * Math.min(1.0, flen) * strength;
            double along = (v.x * tx + v.z * tz) / Math.max(1e-6, Math.sqrt(tx * tx + tz * tz));
            if (along < Math.sqrt(tx * tx + tz * tz)) {
                v = new Vec3(v.x + (tx - v.x) * coupling, v.y, v.z + (tz - v.z) * coupling);
            }
        }
        if (falling > 0 && push > 0) {
            // the weight of falling water: hard to climb, things get washed down
            double share = (double) falling / n;
            double dy = -0.045 * push * share * fallScale;
            v = new Vec3(v.x * 0.97, Math.max(v.y + dy, -0.55), v.z * 0.97);
        }
        en.setDeltaMovement(v);
    }
}
