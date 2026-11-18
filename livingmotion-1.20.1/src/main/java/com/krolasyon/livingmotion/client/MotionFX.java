package com.krolasyon.livingmotion.client;

import com.krolasyon.livingmotion.LMConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayDeque;

/**
 * Procedural motion for every living entity that is NOT one of the GeckoLib remodels (other vanilla mobs and modded
 * mobs): hit recoil, attack lunge, idle breathing and a squash when landing. It only adds a transform around the
 * entity's own renderer, so it works with any model.
 */
public final class MotionFX {
    private MotionFX() {}

    private static final ArrayDeque<Boolean> PUSHED = new ArrayDeque<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void pre(RenderLivingEvent.Pre<?, ?> ev) {
        LivingEntity e = ev.getEntity();
        boolean apply = LMConfig.on(LMConfig.MOTION_ALL) && (!(e instanceof Player) || LMConfig.on(LMConfig.MOTION_PLAYERS))
                && !e.isSleeping();
        PUSHED.push(apply);
        if (!apply) return;
        PoseStack ps = ev.getPoseStack();
        ps.pushPose();
        float pt = ev.getPartialTick();
        float k = (float) LMConfig.strength();
        float age = e.tickCount + pt;

        // hit recoil: snap back, settle (hurtTime counts 10 -> 0)
        float recoil = 0f, squashHit = 0f;
        if (e.hurtTime > 0 && e.deathTime == 0) {
            float p = 1f - Math.max(0f, e.hurtTime - pt) / Math.max(1, e.hurtDuration);
            recoil = Mth.sin(p * Mth.PI) * (1f - p * 0.6f);
            squashHit = Mth.sin(Math.min(1f, p * 2.5f) * Mth.PI);
        }
        // attack lunge
        float atk = e.getAttackAnim(pt);
        float lunge = atk > 0 ? Mth.sin(atk * Mth.PI) : 0f;
        // landing squash
        float land = Effects.landing(e, pt);
        // breathing
        float breath = e.isAlive() ? Mth.sin(age * 0.09f + e.getId()) * 0.012f : 0f;

        float yaw = Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot);
        ps.mulPose(Axis.YP.rotationDegrees(-yaw));          // local +z = facing direction
        ps.translate(0, 0, (lunge * 0.14f - recoil * 0.1f) * k);
        ps.mulPose(Axis.XP.rotationDegrees((lunge * 7f - recoil * 11f) * k));
        float sy = 1f + breath - (land * 0.22f + squashHit * 0.06f) * k;
        float sxz = 1f - breath * 0.5f + (land * 0.14f + squashHit * 0.04f) * k;
        ps.scale(sxz, sy, sxz);
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void post(RenderLivingEvent.Post<?, ?> ev) {
        Boolean pushed = PUSHED.poll();
        if (pushed != null && pushed) ev.getPoseStack().popPose();
    }
}
