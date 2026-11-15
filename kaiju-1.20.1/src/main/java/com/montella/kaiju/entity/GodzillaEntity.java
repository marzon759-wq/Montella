package com.montella.kaiju.entity;

import com.montella.kaiju.rig.Skeleton.Joint;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public class GodzillaEntity extends TitanEntity {
    public static final int TAIL = 14;
    public static final float[] TAIL_R = new float[TAIL + 1];
    static {
        for (int i = 0; i <= TAIL; i++) { float f = i / (float) TAIL; TAIL_R[i] = 3.3f * (1 - f) * (1 - 0.35f * f) + 0.22f; }
    }

    Joint pelvis, spine, chest, neck0, neck1, neck2, head, jaw, armL0, armL1, handL, armR0, armR1, handR,
            thighL, shinL, footL, thighR, shinR, footR;
    final Joint[] tail = new Joint[TAIL + 1];
    private float swingDir = 1;
    private float swingTurned;
    private int heldHead = -1;

    public GodzillaEntity(EntityType<? extends GodzillaEntity> type, Level level) { super(type, level); buildSkeleton(); }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1200).add(Attributes.ARMOR, 20)
                .add(Attributes.ATTACK_DAMAGE, 40).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 200)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void buildSkeleton() {
        var s = skel;
        s.add("root", null, 0, 0, 0);
        pelvis = s.add("pelvis", "root", 0, 10.2f, -0.6f);
        spine = s.add("spine", "pelvis", 0, 2.6f, 0.4f).rest(0.12f, 0, 0);
        chest = s.add("chest", "spine", 0, 4.2f, 0.2f).rest(0.08f, 0, 0);
        neck0 = s.add("neck0", "chest", 0, 4.4f, 0.9f).rest(-1.05f, 0, 0);
        neck1 = s.add("neck1", "neck0", 0, 0, 1.9f).rest(0.15f, 0, 0);
        neck2 = s.add("neck2", "neck1", 0, 0, 1.8f).rest(0.2f, 0, 0);
        head = s.add("head", "neck2", 0, 0, 1.5f).rest(0.72f, 0, 0);
        jaw = s.add("jaw", "head", 0, -0.45f, 0.55f).rest(0.05f, 0, 0);
        armL0 = s.add("armL0", "chest", 3.7f, 2.5f, 2.2f).rest(0.95f, 0.3f, 0);
        armL1 = s.add("armL1", "armL0", 0, 0, 2.8f).rest(-1.0f, 0, 0);
        handL = s.add("handL", "armL1", 0, 0, 2.4f).rest(0.6f, 0, 0);
        armR0 = s.add("armR0", "chest", -3.7f, 2.5f, 2.2f).rest(0.95f, -0.3f, 0);
        armR1 = s.add("armR1", "armR0", 0, 0, 2.8f).rest(-1.0f, 0, 0);
        handR = s.add("handR", "armR1", 0, 0, 2.4f).rest(0.6f, 0, 0);
        thighL = s.add("thighL", "pelvis", 3.4f, -0.6f, 0.2f).rest(1.3f, 0, 0);
        shinL = s.add("shinL", "thighL", 0, 0, 5.0f).rest(0.75f, 0, 0);
        footL = s.add("footL", "shinL", 0, 0, 4.3f).rest(-2.05f, 0, 0);
        thighR = s.add("thighR", "pelvis", -3.4f, -0.6f, 0.2f).rest(1.3f, 0, 0);
        shinR = s.add("shinR", "thighR", 0, 0, 5.0f).rest(0.75f, 0, 0);
        footR = s.add("footR", "shinR", 0, 0, 4.3f).rest(-2.05f, 0, 0);
        tail[0] = s.add("tail0", "pelvis", 0, 0.3f, -3.4f).rest(0.62f, Mth.PI, 0);
        for (int i = 1; i <= TAIL; i++) tail[i] = s.add("tail" + i, "tail" + (i - 1), 0, 0, 2.25f).rest(i < 9 ? -0.045f : -0.07f, 0, 0);
    }

    @Override public float mass() { return 1.0f; }
    @Override public float radius() { return 5.0f; }
    @Override protected float beamBasePower() { return 1.0f; }
    @Override protected float beamDamage() { return 6f; }
    @Override protected SoundEvent beamSound() { return SoundEvents.WARDEN_SONIC_BOOM; }
    @Override protected SoundEvent hurtRoar() { return SoundEvents.RAVAGER_ROAR; }
    @Override protected int roarLength() { return 74; }
    @Override public boolean isEnemyTitan(Entity e) { return e instanceof GhidorahEntity; }
    @Override protected float regenPerSecond() { return 2f; }

    @Override
    protected List<Vec3> beamOrigins() { return List.of(jointWorld("head", 0, -0.35f, 3.4f)); }

    public float swingDir() { return (aux() & 1) == 0 ? 1 : -1; }

    // =================================================================== animation
    @Override
    public void pose(float pt) {
        skel.resetPose();
        int st = state();
        float t = stateTime(pt);
        float age = tickCount + pt;
        float ph = Mth.lerp(pt, prevWalkPhase, walkPhase);
        float wa = Mth.lerp(pt, prevWalkAmt, walkAmt);
        float ly = Mth.lerp(pt, prevLookYaw, lookYaw), lp = Mth.lerp(pt, prevLookPitch, lookPitch);
        float fallAmt = Mth.lerp(pt, prevFall, fall);
        poseRoot(pt, 3.6f);

        // breathing & idle life
        float breath = Mth.sin(age * 0.06f);
        spine.rx += breath * 0.015f;
        chest.scale = 1f + breath * 0.012f;
        neck0.rx += breath * 0.02f;
        for (int i = 0; i <= TAIL; i++) {
            float f = i / (float) TAIL;
            tail[i].ry += Mth.sin(age * 0.045f - i * 0.38f) * 0.05f * f;
            tail[i].rx += Mth.sin(age * 0.03f - i * 0.3f) * 0.012f;
        }
        jaw.rx += Math.max(0, Mth.sin(age * 0.021f)) * 0.06f;

        // walking: heavy plantigrade gait with hip sway and tail counter-swing
        boolean run = st == S.RAM;
        float amp = run ? 1.25f : 1f;
        if (wa > 0.01f) {
            float s = Mth.sin(ph), c = Mth.cos(ph);
            thighL.rx += -s * 0.42f * wa * amp; thighR.rx += s * 0.42f * wa * amp;
            shinL.rx += Math.max(0, c) * 0.55f * wa * amp; shinR.rx += Math.max(0, -c) * 0.55f * wa * amp;
            footL.rx += (s * 0.42f - Math.max(0, c) * 0.55f) * wa * amp * 0.8f;
            footR.rx += (-s * 0.42f - Math.max(0, -c) * 0.55f) * wa * amp * 0.8f;
            pelvis.ty += -Math.abs(c) * 0.38f * wa * amp + 0.2f * wa;
            pelvis.rz += s * 0.06f * wa;
            pelvis.ry += s * 0.05f * wa;
            spine.rz -= s * 0.04f * wa;
            neck0.rz += s * 0.03f * wa;
            head.rx += Mth.sin(ph * 2) * 0.03f * wa;
            armL0.rx += s * 0.12f * wa; armR0.rx -= s * 0.12f * wa;
            for (int i = 0; i <= TAIL; i++) tail[i].ry += Mth.sin(ph - i * 0.42f) * 0.045f * wa * (0.3f + i / (float) TAIL);
        }
        if (run) {
            spine.rx += 0.32f; chest.rx += 0.1f; neck0.rx += 0.35f; head.rx -= 0.25f;
            armL0.rx -= 0.4f; armR0.rx -= 0.4f;
            for (int i = 0; i < 6; i++) tail[i].rx -= 0.05f;
        }

        // head tracks the target, spread over the neck
        float trackW = S.down(st) ? 0 : 1;
        neck0.ry += ly * 0.3f * trackW; neck1.ry += ly * 0.3f * trackW; neck2.ry += ly * 0.2f * trackW; head.ry += ly * 0.2f * trackW;
        neck0.rx += lp * 0.25f * trackW; neck1.rx += lp * 0.25f * trackW; head.rx += lp * 0.35f * trackW;

        switch (st) {
            case S.ROAR -> poseRoar(t, roarLength());
            case S.CHARGE_BEAM -> {
                float e = smooth(Mth.clamp(t / chargeTime(), 0, 1));
                float snap = Mth.clamp((t - chargeTime() + 6) / 6f, 0, 1);
                neck0.rx -= 0.35f * e * (1 - snap); neck1.rx -= 0.2f * e * (1 - snap);
                head.rx -= 0.3f * e * (1 - snap);
                jaw.rx += 0.25f * e + 0.5f * snap;
                spine.rx -= 0.12f * e;
                armL0.rz += 0.25f * e; armR0.rz -= 0.25f * e;
                handL.rx += 0.4f * e; handR.rx += 0.4f * e;
                float tr = Mth.sin(age * 2.1f) * 0.012f * e;
                head.ry += tr; neck2.rx += tr;
                for (int i = 0; i <= TAIL; i++) tail[i].rx -= 0.02f * e;
            }
            case S.FIRE_BEAM -> {
                float e = Math.min(1, t / 4f);
                jaw.rx += 0.9f * e + Mth.sin(age * 1.7f) * 0.04f;
                head.rx += -0.1f * e + Mth.sin(age * 2.3f) * 0.015f;
                neck0.rx += 0.18f * e; spine.rx += 0.12f * e;
                thighL.rx += 0.25f; thighR.rx -= 0.3f; shinL.rx += 0.2f; shinR.rx += 0.25f;
                footL.rx -= 0.45f; footR.rx += 0.05f;
                armL0.rz += 0.3f; armR0.rz -= 0.3f; armL0.rx -= 0.3f; armR0.rx -= 0.3f;
                for (int i = 0; i <= TAIL; i++) tail[i].rx -= 0.025f;
            }
            case S.BEAM_END -> {
                float e = 1 - smooth(Mth.clamp(t / 26f, 0, 1));
                jaw.rx += 0.8f * e; neck0.rx += 0.15f * e; head.rx -= 0.05f * e;
            }
            case S.TAIL_WINDUP -> {
                float e = smooth(Mth.clamp(t / 14f, 0, 1));
                float d = swingDir();
                spine.ry -= d * 0.32f * e; chest.ry -= d * 0.22f * e;
                neck0.ry += d * 0.35f * e; head.ry += d * 0.3f * e;
                spine.rx += 0.18f * e; pelvis.ty -= 0.6f * e;
                thighL.rx -= 0.25f * e; thighR.rx -= 0.25f * e; shinL.rx += 0.45f * e; shinR.rx += 0.45f * e;
                footL.rx -= 0.2f * e; footR.rx -= 0.2f * e;
                for (int i = 0; i <= TAIL; i++) tail[i].ry += d * 0.05f * e * (i / (float) TAIL);
            }
            case S.TAIL_SWING -> {
                float d = swingDir();
                float e = Mth.clamp(t / 4f, 0, 1);
                spine.ry += d * 0.25f * e; chest.ry += d * 0.15f * e;
                neck0.ry -= d * 0.25f; head.ry -= d * 0.3f;
                spine.rx += 0.25f; pelvis.ty -= 0.6f;
                shinL.rx += 0.45f; shinR.rx += 0.45f; thighL.rx -= 0.25f; thighR.rx -= 0.25f;
                // the tail lags behind the hips and then whips
                for (int i = 0; i <= TAIL; i++) {
                    float f = i / (float) TAIL;
                    tail[i].ry -= d * (0.09f * (1 - e) - 0.03f * e) * f;
                    tail[i].rx += 0.012f;
                }
            }
            case S.TAIL_RECOVER -> {
                float d = swingDir();
                float e = 1 - smooth(Mth.clamp(t / 18f, 0, 1));
                spine.ry += d * 0.25f * e; spine.rx += 0.25f * e; pelvis.ty -= 0.6f * e;
                shinL.rx += 0.45f * e; shinR.rx += 0.45f * e; thighL.rx -= 0.25f * e; thighR.rx -= 0.25f * e;
                float wob = Mth.sin(t * 0.9f) * e;
                for (int i = 0; i <= TAIL; i++) tail[i].ry += d * 0.06f * wob * (i / (float) TAIL);
            }
            case S.BITE -> {
                float lunge = t < 7 ? smooth(t / 7f) : t < 12 ? 1 : 1 - smooth((t - 12) / 8f);
                float open = t < 7 ? smooth(t / 7f) : t < 9 ? 1 - (t - 7) / 2f : 0;
                spine.rx += 0.2f * lunge; chest.rx += 0.1f * lunge; neck0.rx += 0.55f * lunge; neck1.rx += 0.1f * lunge;
                head.rx -= 0.35f * lunge; jaw.rx += 0.95f * open;
                armL0.rx -= 0.6f * lunge; armR0.rx -= 0.6f * lunge; armL1.rx -= 0.3f * lunge; armR1.rx -= 0.3f * lunge;
                thighL.rx += 0.3f * lunge; shinR.rx += 0.3f * lunge;
            }
            case S.HOLD, S.TEAR -> {
                spine.rx += 0.2f; neck0.rx += 0.5f; head.rx -= 0.3f; jaw.rx += 0.15f;
                float shake = Mth.sin(t * 0.9f) * (st == S.TEAR ? 0.1f : 0.22f);
                neck0.ry += shake; neck1.ry += shake * 0.6f; head.rz += shake;
                armL0.rx -= 0.9f; armR0.rx -= 0.9f; armL0.rz -= 0.3f; armR0.rz += 0.3f;
                armL1.rx -= 0.4f; armR1.rx -= 0.4f;
                thighL.rx += 0.35f; shinL.rx += 0.2f; thighR.rx -= 0.25f;
                if (st == S.TEAR) {
                    float pull = smooth(Mth.clamp((t - 6) / 8f, 0, 1));
                    neck0.rx -= 0.9f * pull; head.rx -= 0.5f * pull; spine.rx -= 0.35f * pull;
                    neck0.ry -= 0.6f * pull; jaw.rx += t > 16 ? 0.7f : 0;
                }
            }
            case S.THROW -> {
                float e = smooth(Mth.clamp(t / 12f, 0, 1));
                float r = 1 - smooth(Mth.clamp((t - 14) / 10f, 0, 1));
                spine.ry += 0.5f * e * r; chest.ry += 0.3f * e * r; neck0.ry += 0.4f * e * r;
                spine.rx += 0.2f * r; neck0.rx += 0.4f * r; jaw.rx += t < 12 ? 0.15f : 0.6f * r;
                armL0.rx -= 0.8f * r; armR0.rx -= 0.8f * r;
                thighR.rx -= 0.3f * r; shinL.rx += 0.3f * r;
            }
            case S.SHOVE -> {
                float e = t < 8 ? smooth(t / 8f) : 1 - smooth(Mth.clamp((t - 8) / 10f, 0, 1));
                armL0.rx -= 1.3f * e; armR0.rx -= 1.3f * e; armL1.rx += 0.6f * e; armR1.rx += 0.6f * e;
                handL.rx -= 0.6f * e; handR.rx -= 0.6f * e;
                spine.rx += 0.25f * e; neck0.rx += 0.2f * e; jaw.rx += 0.5f * e;
                thighR.rx -= 0.4f * e; shinR.rx += 0.3f * e;
            }
            case S.PULSE -> {
                float e = smooth(Mth.clamp(t / 26f, 0, 1));
                float blast = t > 28 ? 1 - smooth(Mth.clamp((t - 28) / 14f, 0, 1)) : 0;
                if (fallAmt < 0.5f) {
                    spine.rx -= 0.25f * e - 0.3f * blast; neck0.rx -= 0.4f * e; head.rx -= 0.5f * e;
                    armL0.rz += 0.6f * e; armR0.rz -= 0.6f * e; armL0.rx -= 0.4f * e; armR0.rx -= 0.4f * e;
                } else {
                    neck0.rx -= 0.5f * e; head.rx -= 0.4f * e;
                }
                jaw.rx += 1.0f * Math.max(e * 0.4f, blast);
                for (int i = 0; i <= TAIL; i++) tail[i].rx -= 0.04f * e;
            }
            case S.STAGGER, S.CLASH_RECOIL -> {
                float r = Mth.lerp(pt, prevRecoil, recoil);
                neck0.rx -= 0.3f * r; head.rx -= 0.4f * r; jaw.rx += 0.6f * r;
                armL0.rz += 0.5f * r; armR0.rz -= 0.5f * r; armL0.rx -= 0.6f * r; armR0.rx -= 0.6f * r;
                float wob = Mth.sin(t * 0.7f) * Math.max(0, 1 - t / 22f);
                spine.rz += wob * 0.08f; thighL.rx += wob * 0.2f; thighR.rx -= wob * 0.2f;
                for (int i = 0; i <= TAIL; i++) tail[i].ry += wob * 0.04f;
            }
            case S.HELD -> {
                float w = Mth.sin(age * 0.5f);
                neck0.rx -= 0.4f; head.rx -= 0.4f; jaw.rx += 0.7f;
                armL0.rx -= 0.9f + w * 0.3f; armR0.rx -= 0.9f - w * 0.3f; armL0.rz += 0.4f; armR0.rz -= 0.4f;
                thighL.rx += w * 0.4f; thighR.rx -= w * 0.4f;
                for (int i = 0; i <= TAIL; i++) tail[i].ry += Mth.sin(age * 0.4f - i * 0.5f) * 0.08f;
            }
            default -> {}
        }

        // lying down: struggle, push up, thrash the tail; getting up uses the arms
        if (fallAmt > 0.01f) {
            float f = fallAmt;
            boolean pinned = st == S.PINNED;
            float struggle = pinned ? 1f : st == S.FALLEN ? 0.4f : 0.15f;
            float w = Mth.sin(age * (pinned ? 0.5f : 0.25f));
            neck0.rx -= 0.5f * f; head.rx -= 0.3f * f; neck0.ry += w * 0.2f * struggle * f;
            jaw.rx += (0.3f + 0.4f * Math.max(0, w)) * struggle * f;
            armL0.rx -= (0.8f + w * 0.4f * struggle) * f; armR0.rx -= (0.8f - w * 0.4f * struggle) * f;
            armL0.rz += 0.5f * f; armR0.rz -= 0.5f * f;
            thighL.rx -= (0.6f + w * 0.3f * struggle) * f; thighR.rx -= (0.3f - w * 0.3f * struggle) * f;
            shinL.rx += 0.5f * f; shinR.rx += 0.7f * f;
            for (int i = 0; i <= TAIL; i++) {
                tail[i].rx -= 0.035f * f;
                tail[i].ry += Mth.sin(age * 0.3f - i * 0.5f) * 0.06f * struggle * f;
            }
            if (st == S.GET_UP) {
                float push = Mth.sin(Mth.clamp(t / 40f, 0, 1) * Mth.PI);
                armL0.rx -= 0.7f * push; armR0.rx -= 0.7f * push; armL1.rx += 0.8f * push; armR1.rx += 0.8f * push;
                thighL.rx -= 0.8f * push; shinL.rx += 1.0f * push; footL.rx -= 0.2f * push;
            }
        }
        if (st == S.DEATH) { jaw.rx += 0.5f; neck0.ry += 0.3f; }
        skel.compute();
    }

    private void poseRoar(float t, int len) {
        float e = smooth(Mth.clamp(t / 16f, 0, 1)) * (1 - smooth(Mth.clamp((t - len + 14) / 14f, 0, 1)));
        float tremble = Mth.sin(t * 2.4f) * 0.02f * e;
        spine.rx -= 0.2f * e; chest.rx -= 0.12f * e;
        neck0.rx -= 0.55f * e; neck1.rx -= 0.25f * e; neck2.rx -= 0.15f * e;
        head.rx -= 0.55f * e + tremble; head.rz += tremble;
        jaw.rx += 1.0f * e + tremble * 3;
        armL0.rz += 0.55f * e; armR0.rz -= 0.55f * e; armL0.rx -= 0.5f * e; armR0.rx -= 0.5f * e;
        handL.rx -= 0.5f * e; handR.rx -= 0.5f * e;
        for (int i = 0; i <= TAIL; i++) tail[i].rx -= 0.025f * e * (1 - i / (float) TAIL);
    }

    /** Dorsal plates light up in a wave from the tail to the head (0..1 per tail/back index; index 0 = neck). */
    public float spineGlow(int seg, int total, float pt) {
        int st = state();
        float t = stateTime(pt);
        float beam = beamPower();
        if (st == S.CHARGE_BEAM) {
            float front = t / chargeTime() * (total + 4);
            float my = total - seg;
            float g = Mth.clamp((front - my) / 3f, 0, 1);
            return g * (0.75f + 0.25f * Mth.sin((tickCount + pt) * 1.3f + seg));
        }
        if (st == S.FIRE_BEAM) return 0.9f + 0.1f * Mth.sin((tickCount + pt) * 2f + seg * 0.7f);
        if (st == S.PULSE) {
            float front = t / 26f * (total + 4);
            float g = Mth.clamp((front - (total - seg)) / 3f, 0, 1);
            return t > 28 ? Math.max(0, 1 - (t - 28) / 20f) : g;
        }
        return Math.max(beam, 0.06f + 0.04f * Mth.sin((tickCount + pt) * 0.05f));
    }

    // =================================================================== behaviour
    @Override
    protected void titanAi(LivingEntity target, double gap) {
        boolean titan = target instanceof TitanEntity;
        if (titan && roarCooldown <= 0 && gap > 25 && random.nextFloat() < 0.02f) { startRoar(); return; }
        if (titan && S.down(((TitanEntity) target).state()) && gap < 8 && attackCooldown <= 0) {
            if (random.nextFloat() < 0.5f) { startBite(); return; }
            if (beamCooldown <= 0) { startBeam(false); return; }
        }
        if (beamCooldown <= 0 && gap > 10 && gap < 95 && random.nextFloat() < 0.035f && hasLineOfSight(target)) { startBeam(false); return; }
        if (gap < 4 && attackCooldown <= 0) {
            float r = random.nextFloat();
            if (r < 0.32f) startBite();
            else if (r < 0.64f) startTail();
            else if (r < 0.82f) { setState(S.SHOVE); attackCooldown = 30; }
            else if (beamCooldown < 60) startBeam(true);
            else startTail();
            return;
        }
        if (gap > 12 && gap < 32 && attackCooldown <= 0 && random.nextFloat() < 0.012f) { setState(S.RAM); attackCooldown = 60; return; }
        if (gap > 2.5) moveTowards(target.position(), gap > 30 ? 0.32f : 0.24f, 2.5f);
        else turnTo(yawTo(target.position()), 2.5f);
    }

    private void startBite() { setState(S.BITE); attackCooldown = 40; }

    private void startTail() {
        setAux(random.nextBoolean() ? 0 : 1);
        setState(S.TAIL_WINDUP);
        attackCooldown = 70;
        playSound(SoundEvents.RAVAGER_ATTACK, 5f, 0.5f);
    }

    @Override
    protected void tickRoar() {
        if (stateTime == 10) playSound(SoundEvents.ENDER_DRAGON_GROWL, 10f, 0.45f);
        if (stateTime == 12) playSound(SoundEvents.RAVAGER_ROAR, 10f, 0.4f);
        if (stateTime == 30) playSound(SoundEvents.ENDER_DRAGON_GROWL, 10f, 0.38f);
        if (stateTime > 12 && stateTime < 60 && stateTime % 8 == 0) level().broadcastEntityEvent(this, EV_SHAKE_SMALL);
    }

    @Override
    protected void tickState(LivingEntity target) {
        int st = state();
        ServerLevel sl = (ServerLevel) level();
        switch (st) {
            case S.IDLE -> {
                if (walkAmt > 0.2f && tickCount % 13 == 0) {
                    playSound(SoundEvents.RAVAGER_STEP, 4f, 0.35f);
                    level().broadcastEntityEvent(this, EV_SHAKE_SMALL);
                }
            }
            case S.RAM -> {
                if (target == null || stateTime > 60) { setState(S.IDLE); return; }
                if (stateTime % 9 == 0) { playSound(SoundEvents.RAVAGER_STEP, 5f, 0.4f); level().broadcastEntityEvent(this, EV_SHAKE_SMALL); }
                moveTowards(target.position(), 0.62f, 3f);
                if (gapTo(target) < 1.0) onRamContact(target instanceof TitanEntity t ? t : null);
                if (target != null && !(target instanceof TitanEntity) && gapTo(target) < 1.5) {
                    fling(target, Vec3.directionFromRotation(-20, getYRot()).scale(2.5), 30);
                    setState(S.IDLE);
                }
            }
            case S.TAIL_WINDUP -> {
                turnTo(getYRot() - swingDir() * 40, 3.2f);
                if (stateTime >= 14) { setState(S.TAIL_SWING); swingTurned = 0; playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 8f, 0.3f); }
            }
            case S.TAIL_SWING -> tickTailSwing(sl);
            case S.TAIL_RECOVER -> {
                if (target != null && stateTime > 6) turnTo(yawTo(target.position()), 4f);
                if (stateTime > 20) setState(S.IDLE);
            }
            case S.BITE -> {
                if (target != null) turnTo(yawTo(target.position()), 3f);
                if (stateTime == 3) playSound(SoundEvents.RAVAGER_ATTACK, 5f, 0.6f);
                if (stateTime == 9 && target != null) doBite(target);
                if (stateTime > 20) setState(S.IDLE);
            }
            case S.HOLD -> tickHold(target);
            case S.TEAR -> tickTear();
            case S.THROW -> tickThrow();
            case S.SHOVE -> {
                if (stateTime == 8 && target != null) {
                    Vec3 hand = jointWorld("handL", 0, 0, 1.5f);
                    if (gapTo(target) < 3.5) {
                        Vec3 f = Vec3.directionFromRotation(0, getYRot());
                        fling(target, f.scale(0.75).add(0, 0.1, 0), 25);
                        playSound(SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 6f, 0.4f);
                        level().broadcastEntityEvent(this, EV_SHAKE_SMALL);
                        smash(hand, 2.5, 30);
                    }
                }
                if (stateTime > 20) setState(S.IDLE);
            }
            case S.PULSE -> tickPulse(sl);
            case S.PINNED -> {
                if (stateTime > 50 + random.nextInt(60) && random.nextFloat() < 0.05f) setState(S.PULSE);
            }
            case S.HELD -> {
                if (stateTime > 30 && random.nextFloat() < 0.02f) setState(S.PULSE);
            }
            default -> {}
        }
    }

    private void tickTailSwing(ServerLevel sl) {
        float d = swingDir();
        float total = 250f;
        float stepDeg = 20f;
        int sub = 4;
        // model-space tail points do not change while only the yaw turns, compute them once
        Vector3f[] pts = new Vector3f[TAIL + 1];
        for (int i = 0; i <= TAIL; i++) pts[i] = tail[i].origin();
        for (int k = 0; k < sub; k++) {
            float yaw = getYRot() + d * stepDeg / sub;
            setYRot(yaw); yBodyRot = yaw; yHeadRot = yaw;
            swingTurned += stepDeg / sub;
            for (int i = 4; i <= TAIL; i++) {
                Vec3 p = toWorld(pts[i], getX(), getY(), getZ(), yaw);
                double r = TAIL_R[i] + 0.6;
                for (Entity e : entitiesNear(p, r)) {
                    if (e instanceof TitanEntity t && t.state() == S.HELD) continue;
                    // tangential direction of the sweep
                    Vec3 rel = p.subtract(position()).multiply(1, 0, 1);
                    double dist = rel.length();
                    Vec3 tan = new Vec3(-rel.z, 0, rel.x).normalize().scale(-d);
                    double speed = dist * (stepDeg * Mth.DEG_TO_RAD);
                    float power = (float) Mth.clamp(speed * 0.24, 0.5, 2.2);
                    Vec3 imp = tan.scale(power * 0.85).add(0, 0.25 + power * 0.15, 0).add(rel.normalize().scale(0.3));
                    if (e instanceof TitanEntity t) {
                        t.receiveImpact(imp, power, 55f * power, this);
                        playSound(SoundEvents.GENERIC_EXPLODE, 7f, 0.4f);
                        playSound(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 7f, 0.3f);
                        level().broadcastEntityEvent(this, EV_SHAKE_BIG);
                        sl.sendParticles(ParticleTypes.EXPLOSION, p.x, p.y, p.z, 6, 1.5, 1.5, 1.5, 0);
                        // the tail stops on the body it hit: it never passes through
                        setState(S.TAIL_RECOVER);
                        return;
                    }
                    if (!(e instanceof TitanEntity)) fling(e, imp.scale(2.0), 30f);
                }
                if (i % 2 == 0 && p.y - getY() < 5) {
                    smash(p, TAIL_R[i] + 0.4, 25);
                    if (k == 0 && i % 4 == 0) dust(sl, p, 3, 14);
                }
            }
            if (swingTurned >= total) { setState(S.TAIL_RECOVER); return; }
        }
        if (stateTime % 4 == 0) playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 6f, 0.4f);
    }

    private void doBite(LivingEntity target) {
        Vec3 mouth = jointWorld("head", 0, -0.4f, 3.2f);
        if (target instanceof GhidorahEntity g) {
            int h = g.closestNeck(mouth, 6.0);
            if (h >= 0) {
                heldHead = h;
                setGrabbed(g);
                g.setState(S.HELD);
                g.hurt(damageSources().mobAttack(this), 30);
                g.damageHead(h, 40);
                setState(S.HOLD);
                playSound(SoundEvents.PLAYER_ATTACK_CRIT, 8f, 0.4f);
                playSound(SoundEvents.ENDER_DRAGON_HURT, 8f, 0.5f);
                return;
            }
        }
        if (target instanceof TitanEntity t ? gapTo(t) < 3.5 : target.distanceTo(this) < radius() + 5) {
            Vec3 pull = position().subtract(target.position()).multiply(1, 0, 1).normalize().scale(0.6);
            fling(target, pull.add(0, 0.1, 0), 35);
            playSound(SoundEvents.PLAYER_ATTACK_CRIT, 6f, 0.5f);
        }
    }

    private void tickHold(LivingEntity target) {
        Entity g = grabbed();
        if (!(g instanceof TitanEntity t) || !t.isAlive() || t.state() != S.HELD) { releaseGrabs(); setState(S.IDLE); return; }
        Vec3 f = Vec3.directionFromRotation(0, getYRot());
        double dist = radius() + t.radius() - 1.5;
        float shake = Mth.sin(stateTime * 0.9f) * 2.5f;
        Vec3 side = new Vec3(-f.z, 0, f.x);
        Vec3 want = position().add(f.scale(dist)).add(side.scale(shake));
        Vec3 np = t.position().add(want.subtract(t.position()).multiply(0.3, 0, 0.3));
        t.setPos(np.x, t.getY(), np.z);
        t.setYRot(yawTo(np) + 180); t.yBodyRot = t.getYRot();
        if (stateTime % 5 == 0) { t.hurt(damageSources().mobAttack(this), 4); playSound(SoundEvents.ENDER_DRAGON_HURT, 6f, 0.6f); }
        if (stateTime % 8 == 0) level().broadcastEntityEvent(this, EV_SHAKE_SMALL);
        if (stateTime > 36) {
            boolean canTear = t instanceof GhidorahEntity gh && heldHead >= 0 && gh.headsAlive() >= 2;
            setState(canTear && random.nextFloat() < 0.4f ? S.TEAR : S.THROW);
        }
    }

    private void tickTear() {
        Entity g = grabbed();
        if (!(g instanceof GhidorahEntity gh)) { releaseGrabs(); setState(S.IDLE); return; }
        if (stateTime < 14) {
            Vec3 back = position().subtract(gh.position()).multiply(1, 0, 1).normalize().scale(0.25);
            gh.setPos(gh.getX() + back.x, gh.getY(), gh.getZ() + back.z);
        }
        if (stateTime == 14) {
            gh.severHead(heldHead, Vec3.directionFromRotation(-30, getYRot() + 160).scale(0.9));
            playSound(SoundEvents.PLAYER_ATTACK_CRIT, 10f, 0.3f);
            playSound(SoundEvents.ENDER_DRAGON_HURT, 10f, 0.4f);
            level().broadcastEntityEvent(this, EV_SHAKE_BIG);
            setGrabbed(null);
            gh.setState(S.STAGGER);
            gh.receiveImpact(gh.position().subtract(position()).multiply(1, 0, 1).normalize().scale(0.6), 0.6f, 0, this);
            heldHead = -1;
        }
        if (stateTime > 34) { setState(S.IDLE); if (random.nextFloat() < 0.7f) startRoar(); }
    }

    private void tickThrow() {
        Entity g = grabbed();
        if (stateTime <= 12 && g instanceof TitanEntity t) {
            turnTo(getYRot() + 110, 9f);
            Vec3 f = Vec3.directionFromRotation(0, getYRot());
            Vec3 want = position().add(f.scale(radius() + t.radius() - 1.5));
            t.setPos(want.x, t.getY() + (stateTime > 6 ? 0.4 : 0), want.z);
            if (stateTime == 12) {
                Vec3 side = new Vec3(-f.z, 0, f.x).scale(-1);
                setGrabbed(null);
                t.setState(S.STAGGER);
                t.receiveImpact(side.scale(1.5).add(f.scale(0.6)).add(0, 0.65, 0), 2.0f, 45, this);
                playSound(SoundEvents.PLAYER_ATTACK_STRONG, 8f, 0.3f);
                heldHead = -1;
            }
        }
        if (stateTime > 26) setState(S.IDLE);
    }

    private void tickPulse(ServerLevel sl) {
        if (stateTime == 1) playSound(SoundEvents.BEACON_POWER_SELECT, 8f, 0.5f);
        if (stateTime == 28) {
            Vec3 c = position().add(0, 6, 0);
            playSound(SoundEvents.GENERIC_EXPLODE, 12f, 0.35f);
            playSound(SoundEvents.WARDEN_SONIC_BOOM, 12f, 0.4f);
            level().broadcastEntityEvent(this, EV_SHAKE_HUGE);
            for (int i = 0; i < 64; i++) {
                double a = i / 64.0 * Math.PI * 2;
                for (int r = 4; r <= 28; r += 6)
                    sl.sendParticles(r < 10 ? ParticleTypes.SONIC_BOOM : ParticleTypes.EXPLOSION, c.x + Math.cos(a) * r, getY() + 2, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
            sl.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 3, 2, 2, 2, 0);
            releaseGrabs();
            for (Entity e : entitiesNear(c, 30)) {
                Vec3 away = e.position().subtract(position()).multiply(1, 0, 1);
                double d = Math.max(1, away.length());
                float power = (float) Mth.clamp(2.4 - d / 20, 0.6, 2.4);
                fling(e, away.normalize().scale(power).add(0, 0.5 + power * 0.25, 0), 60 * power);
            }
            if (griefing()) crater(blockPosition(), 7);
            if (!onGround()) setState(S.FLUNG);
            else if (fall > 0.5f) setState(S.GET_UP);
            else setState(S.IDLE);
            beamCooldown = Math.max(beamCooldown, 100);
        }
    }

    @Override
    protected void onRamContact(TitanEntity other) {
        if (state() != S.RAM) return;
        Vec3 f = Vec3.directionFromRotation(0, getYRot());
        setState(S.STAGGER);
        recoil = 0.6f;
        playSound(SoundEvents.GENERIC_EXPLODE, 8f, 0.4f);
        playSound(SoundEvents.ANVIL_LAND, 8f, 0.3f);
        level().broadcastEntityEvent(this, EV_SHAKE_BIG);
        setDeltaMovement(f.scale(-0.3));
        if (other != null) {
            boolean both = other.state() == S.RAM;
            other.receiveImpact(f.scale(both ? 0.7 : 1.25).add(0, 0.25, 0), both ? 0.8f : 1.3f, 50, this);
            Vec3 mid = position().add(other.position()).scale(0.5).add(0, 8, 0);
            ((ServerLevel) level()).sendParticles(ParticleTypes.EXPLOSION_EMITTER, mid.x, mid.y, mid.z, 1, 0, 0, 0, 0);
        }
    }
}
