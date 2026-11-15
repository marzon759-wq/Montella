package com.montella.kaiju.entity;

import com.montella.kaiju.KaijuMod;
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

import java.util.ArrayList;
import java.util.List;

public class GhidorahEntity extends TitanEntity {
    public static final int NECK = 10, TAIL = 12;
    public static final float[] NECK_R = new float[NECK + 1], TAIL_R = new float[TAIL + 1];
    static {
        for (int i = 0; i <= NECK; i++) NECK_R[i] = 1.45f - 0.5f * i / NECK;
        for (int i = 0; i <= TAIL; i++) { float f = i / (float) TAIL; TAIL_R[i] = 1.9f * (1 - f) + 0.3f; }
    }
    public static final float[] FINGER_LEN = {10f, 11f, 10f, 8.5f};

    Joint pelvis, spine, chest, thighL, shinL, footL, thighR, shinR, footR;
    final Joint[][] neck = new Joint[3][NECK + 1];
    final Joint[] head = new Joint[3], jaw = new Joint[3];
    final Joint[] wing0 = new Joint[2], wing1 = new Joint[2], wing2 = new Joint[2];
    final Joint[][] finger = new Joint[2][4], fingerB = new Joint[2][4];
    final Joint[][] tails = new Joint[2][TAIL + 1];

    public float spread, prevSpread;
    private final float[] headHp = {220, 220, 220};
    private final int[] regrowDelay = new int[3];
    private int biteHits;

    public GhidorahEntity(EntityType<? extends GhidorahEntity> type, Level level) {
        super(type, level);
        buildSkeleton();
        setAux(100 | 100 << 8 | 100 << 16);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1100).add(Attributes.ARMOR, 16)
                .add(Attributes.ATTACK_DAMAGE, 35).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 200)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void buildSkeleton() {
        var s = skel;
        s.add("root", null, 0, 0, 0);
        pelvis = s.add("pelvis", "root", 0, 8.0f, 0);
        spine = s.add("spine", "pelvis", 0, 2.4f, 0.3f).rest(0.06f, 0, 0);
        chest = s.add("chest", "spine", 0, 3.8f, 0.3f).rest(-0.04f, 0, 0);
        float[][] base = {{0, 3.5f, 0.7f}, {2.5f, 2.8f, 0.5f}, {-2.5f, 2.8f, 0.5f}};
        float[] baseYaw = {0, 0.38f, -0.38f};
        for (int h = 0; h < 3; h++) {
            neck[h][0] = s.add("n" + h + "_0", "chest", base[h][0], base[h][1], base[h][2]).rest(h == 0 ? -1.35f : -1.22f, baseYaw[h], 0);
            for (int i = 1; i <= NECK; i++)
                neck[h][i] = s.add("n" + h + "_" + i, "n" + h + "_" + (i - 1), 0, 0, 1.02f).rest(i <= 4 ? -0.05f : 0.235f, i == 5 ? -baseYaw[h] * 0.6f : 0, 0);
            head[h] = s.add("h" + h, "n" + h + "_" + NECK, 0, 0, 0.9f).rest(0.28f, -baseYaw[h] * 0.3f, 0);
            jaw[h] = s.add("j" + h, "h" + h, 0, -0.42f, 0.55f).rest(0.05f, 0, 0);
        }
        for (int w = 0; w < 2; w++) {
            float m = w == 0 ? 1 : -1;
            String n = w == 0 ? "L" : "R";
            wing0[w] = s.add("w" + n + "0", "chest", 2.5f * m, 2.3f, -1.4f).rest(0, Mth.HALF_PI * m, 0);
            wing1[w] = s.add("w" + n + "1", "w" + n + "0", 0, 0, 5f);
            wing2[w] = s.add("w" + n + "2", "w" + n + "1", 0, 0, 6f);
            float[] fan = {0.22f, 0.72f, 1.18f, 1.62f};
            for (int k = 0; k < 4; k++) {
                finger[w][k] = s.add("f" + n + k, "w" + n + "2", 0, 0, 0).rest(0, fan[k] * m, 0);
                fingerB[w][k] = s.add("f" + n + k + "b", "f" + n + k, 0, 0, FINGER_LEN[k] * 0.5f);
            }
        }
        thighL = s.add("thighL", "pelvis", 2.8f, -0.5f, 0.3f).rest(1.2f, 0, 0);
        shinL = s.add("shinL", "thighL", 0, 0, 4.2f).rest(0.95f, 0, 0);
        footL = s.add("footL", "shinL", 0, 0, 4.0f).rest(-2.15f, 0, 0);
        thighR = s.add("thighR", "pelvis", -2.8f, -0.5f, 0.3f).rest(1.2f, 0, 0);
        shinR = s.add("shinR", "thighR", 0, 0, 4.2f).rest(0.95f, 0, 0);
        footR = s.add("footR", "shinR", 0, 0, 4.0f).rest(-2.15f, 0, 0);
        for (int k = 0; k < 2; k++) {
            float m = k == 0 ? 1 : -1;
            tails[k][0] = s.add("t" + k + "_0", "pelvis", 1.1f * m, 0.2f, -2.6f).rest(0.5f, Mth.PI - 0.22f * m, 0);
            for (int i = 1; i <= TAIL; i++) tails[k][i] = s.add("t" + k + "_" + i, "t" + k + "_" + (i - 1), 0, 0, 1.7f).rest(-0.035f, 0.012f * -m, 0);
        }
    }

    @Override public float mass() { return 0.9f; }
    @Override public float radius() { return 4.5f; }
    @Override protected float beamBasePower() { return 1.05f * Math.max(1, headsAlive()) / 3f + 0.1f; }
    @Override protected float beamDamage() { return 2.2f * headsAlive(); }
    @Override protected SoundEvent beamSound() { return SoundEvents.LIGHTNING_BOLT_THUNDER; }
    @Override protected SoundEvent hurtRoar() { return SoundEvents.ENDER_DRAGON_HURT; }
    @Override protected int roarLength() { return 80; }
    @Override public boolean isEnemyTitan(Entity e) { return e instanceof GodzillaEntity; }
    @Override protected boolean huntsPlayers() { return true; }
    @Override protected int chargeTime() { return 26; }
    @Override protected int fireTime() { return 80; }

    // ------------------------------------------------------------------ heads
    public int growth(int h) { return (aux() >> (h * 8)) & 0xFF; }

    private void setGrowth(int h, int g) {
        int a = aux() & ~(0xFF << (h * 8));
        setAux(a | (Mth.clamp(g, 0, 100) << (h * 8)));
    }

    public boolean headUp(int h) { return growth(h) >= 100; }

    public int headsAlive() { int n = 0; for (int h = 0; h < 3; h++) if (headUp(h)) n++; return n; }

    public Vec3 mouth(int h) { return jointWorld("h" + h, 0, -0.35f, 3.9f); }

    @Override
    protected List<Vec3> beamOrigins() {
        List<Vec3> out = new ArrayList<>();
        for (int h = 0; h < 3; h++) if (headUp(h)) out.add(mouth(h));
        if (out.isEmpty()) out.add(jointWorld("chest", 0, 4, 1));
        return out;
    }

    public int closestNeck(Vec3 p, double r) {
        int best = -1; double bd = r * r;
        for (int h = 0; h < 3; h++) {
            if (growth(h) < 60) continue;
            for (int i = 3; i <= NECK; i++) {
                double d = toWorld(neck[h][i].origin()).distanceToSqr(p);
                if (d < bd) { bd = d; best = h; }
            }
            double d = toWorld(head[h].origin()).distanceToSqr(p);
            if (d < bd) { bd = d; best = h; }
        }
        return best;
    }

    public void damageHead(int h, float amount) {
        if (!headUp(h)) return;
        headHp[h] -= amount;
        if (headHp[h] <= 0) severHead(h, Vec3.directionFromRotation(-40, getYRot() + (random.nextFloat() - 0.5f) * 120).scale(0.7));
    }

    public void severHead(int h, Vec3 vel) {
        if (!headUp(h)) return;
        if (headsAlive() <= 1) { hurt(damageSources().generic(), 80); headHp[h] = 120; return; }
        Vec3 p = toWorld(neck[h][7].origin());
        SeveredHeadEntity e = new SeveredHeadEntity(KaijuMod.SEVERED_HEAD.get(), level());
        e.moveTo(p.x, p.y, p.z, getYRot(), 0);
        e.setDeltaMovement(vel);
        level().addFreshEntity(e);
        setGrowth(h, 0);
        headHp[h] = 220;
        regrowDelay[h] = 260 + random.nextInt(120);
        hurt(damageSources().generic(), 60);
        ServerLevel sl = (ServerLevel) level();
        sl.sendParticles(ParticleTypes.LAVA, p.x, p.y, p.z, 40, 0.6, 0.6, 0.6, 0.2);
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 60, 0.8, 0.8, 0.8, 0.6);
        playSound(SoundEvents.ENDER_DRAGON_HURT, 10f, 0.35f);
        level().broadcastEntityEvent(this, EV_SHAKE_BIG);
    }

    @Override
    protected void onBeamStruck(TitanEntity from, Vec3 at) {
        super.onBeamStruck(from, at);
        for (int h = 0; h < 3; h++) {
            if (headUp(h) && toWorld(head[h].origin()).distanceToSqr(at) < 4.5 * 4.5) damageHead(h, 5f);
        }
    }

    // ------------------------------------------------------------------ animation
    @Override
    public void tick() {
        prevSpread = spread;
        super.tick();
        int st = state();
        float want = switch (st) {
            case S.FLY, S.TAKEOFF, S.LAND, S.LIFT, S.WING_BUFFET -> 1f;
            case S.ROAR -> stateTime > 10 && stateTime < 66 ? 0.85f : 0.1f;
            case S.FIRE_BEAM, S.CHARGE_BEAM, S.PIN -> 0.45f;
            case S.FLUNG, S.FALLEN, S.DEATH -> 0.6f;
            default -> 0f;
        };
        spread += (want - spread) * 0.08f;
    }

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
        float sp = Mth.lerp(pt, prevSpread, spread);
        boolean flying = isFlying() || st == S.LIFT;
        poseRoot(pt, 3.2f);

        float breath = Mth.sin(age * 0.07f);
        spine.rx += breath * 0.015f;
        chest.scale = 1 + breath * 0.012f;

        // three necks: sinuous, independent, each with its own temper
        float[] phase = {0f, 2.1f, 4.3f};
        for (int h = 0; h < 3; h++) {
            for (int i = 1; i <= NECK; i++) {
                float f = i / (float) NECK;
                neck[h][i].ry += Mth.sin(age * 0.055f + phase[h] - i * 0.45f) * 0.07f * f;
                neck[h][i].rx += Mth.sin(age * 0.043f + phase[h] * 1.3f - i * 0.38f) * 0.045f;
            }
            float snap = Math.max(0, Mth.sin(age * 0.09f + phase[h] * 2.7f) - 0.75f) * 3.5f;
            jaw[h].rx += 0.08f + snap * 0.45f;
            head[h].rz += Mth.sin(age * 0.05f + phase[h]) * 0.08f;
            // looking: spread over the neck, side heads converge on the target
            float yawShare = ly + (h == 1 ? -0.12f : h == 2 ? 0.12f : 0);
            if (!S.down(st)) {
                for (int i = 6; i <= NECK; i++) { neck[h][i].ry += yawShare * 0.11f; neck[h][i].rx += lp * 0.06f; }
                head[h].ry += yawShare * 0.25f; head[h].rx += lp * 0.3f;
            }
        }

        if (wa > 0.01f && !flying) {
            float s = Mth.sin(ph), c = Mth.cos(ph);
            thighL.rx += -s * 0.45f * wa; thighR.rx += s * 0.45f * wa;
            shinL.rx += Math.max(0, c) * 0.6f * wa; shinR.rx += Math.max(0, -c) * 0.6f * wa;
            footL.rx += (s * 0.45f - Math.max(0, c) * 0.6f) * wa * 0.8f;
            footR.rx += (-s * 0.45f - Math.max(0, -c) * 0.6f) * wa * 0.8f;
            pelvis.ty += -Math.abs(c) * 0.32f * wa + 0.18f * wa;
            pelvis.rz += s * 0.07f * wa; spine.rz -= s * 0.05f * wa;
            for (int h = 0; h < 3; h++) neck[h][2].rx += Mth.sin(ph * 2 + h) * 0.03f * wa;
            for (int k = 0; k < 2; k++) for (int i = 0; i <= TAIL; i++) tails[k][i].ry += Mth.sin(ph - i * 0.4f + k) * 0.05f * wa;
        }
        for (int k = 0; k < 2; k++) for (int i = 0; i <= TAIL; i++) {
            float f = i / (float) TAIL;
            tails[k][i].ry += Mth.sin(age * 0.05f - i * 0.4f + k * 1.7f) * 0.06f * f;
            tails[k][i].rx += Mth.sin(age * 0.037f - i * 0.3f + k) * 0.02f;
        }

        poseWings(sp, flying, age, t, st);

        if (flying) {
            float bank = Mth.clamp((float) getDeltaMovement().y * 0.8f, -0.3f, 0.3f);
            spine.rx += 0.35f; pelvis.rx += 0.2f - bank;
            thighL.rx -= 0.9f; thighR.rx -= 0.9f; shinL.rx += 0.9f; shinR.rx += 0.9f; footL.rx += 0.7f; footR.rx += 0.7f;
            for (int h = 0; h < 3; h++) { neck[h][0].rx += 0.35f; for (int i = 5; i <= NECK; i++) neck[h][i].rx -= 0.04f; }
            for (int k = 0; k < 2; k++) for (int i = 0; i <= TAIL; i++) tails[k][i].rx -= 0.03f;
        }

        switch (st) {
            case S.ROAR -> {
                for (int h = 0; h < 3; h++) {
                    float start = h == 0 ? 0 : h == 1 ? 14 : 24;
                    float e = smooth(Mth.clamp((t - start) / 12f, 0, 1)) * (1 - smooth(Mth.clamp((t - 64) / 14f, 0, 1)));
                    neck[h][0].rx -= 0.25f * e;
                    for (int i = 5; i <= NECK; i++) neck[h][i].rx -= 0.16f * e;
                    head[h].rx -= 0.4f * e; jaw[h].rx += 1.0f * e + Mth.sin(t * 2.2f + h) * 0.05f * e;
                    neck[h][3].ry += (h == 1 ? 0.25f : h == 2 ? -0.25f : 0) * e;
                }
                spine.rx -= 0.12f; chest.rx -= 0.08f;
            }
            case S.CHARGE_BEAM -> {
                float e = smooth(Mth.clamp(t / chargeTime(), 0, 1));
                for (int h = 0; h < 3; h++) {
                    for (int i = 4; i <= NECK; i++) neck[h][i].rx -= 0.07f * e;
                    head[h].rx -= 0.2f * e; jaw[h].rx += 0.5f * e;
                    head[h].ry += Mth.sin(age * 1.9f + h) * 0.03f * e;
                }
            }
            case S.FIRE_BEAM -> {
                for (int h = 0; h < 3; h++) {
                    float j = Mth.sin(age * 1.6f + h * 2) * 0.05f;
                    for (int i = 5; i <= NECK; i++) neck[h][i].rx += 0.02f + j * 0.3f;
                    head[h].rx += 0.05f + j; jaw[h].rx += 1.0f + j;
                }
                spine.rx += 0.1f;
                thighL.rx += 0.25f; thighR.rx -= 0.25f; shinL.rx += 0.2f; shinR.rx += 0.2f; footL.rx -= 0.45f;
            }
            case S.BEAM_END -> {
                float e = 1 - smooth(Mth.clamp(t / 26f, 0, 1));
                for (int h = 0; h < 3; h++) jaw[h].rx += 0.9f * e;
            }
            case S.BITE -> {
                for (int h = 0; h < 3; h++) {
                    float s0 = 4 + h * 6;
                    float lt = t - s0;
                    float lunge = lt < 0 ? 0 : lt < 5 ? smooth(lt / 5f) : lt < 8 ? 1 : 1 - smooth(Mth.clamp((lt - 8) / 8f, 0, 1));
                    float open = lt < 0 ? 0 : lt < 4 ? smooth(lt / 4f) : lt < 6 ? 1 - (lt - 4) / 2f : 0;
                    neck[h][0].rx += 0.55f * lunge;
                    for (int i = 5; i <= NECK; i++) neck[h][i].rx -= 0.1f * lunge;
                    head[h].rx += 0.15f * lunge; jaw[h].rx += 1.0f * open;
                }
                spine.rx += 0.15f;
            }
            case S.WING_BUFFET -> {
                for (int h = 0; h < 3; h++) { head[h].rx -= 0.3f; jaw[h].rx += 0.6f; }
            }
            case S.PIN -> {
                spine.rx += 0.5f; chest.rx += 0.2f;
                thighL.rx -= 0.6f; thighR.rx -= 0.6f; shinL.rx += 1.0f; shinR.rx += 1.0f; footL.rx -= 0.4f; footR.rx -= 0.4f;
                thighL.rz += 0.3f; thighR.rz -= 0.3f;
                for (int h = 0; h < 3; h++) {
                    float cyc = ((t + h * 5) % 15f) / 15f;
                    float strike = cyc < 0.35f ? smooth(cyc / 0.35f) : 1 - smooth((cyc - 0.35f) / 0.65f);
                    neck[h][0].rx += 0.6f + 0.5f * strike;
                    for (int i = 5; i <= NECK; i++) neck[h][i].rx += 0.06f * strike;
                    jaw[h].rx += cyc < 0.3f ? 0.9f : 0.2f;
                    neck[h][2].ry += (h == 1 ? 0.15f : h == 2 ? -0.15f : 0);
                }
            }
            case S.LIFT -> {
                for (int h = 0; h < 3; h++) { neck[h][0].rx += 0.7f; for (int i = 5; i <= NECK; i++) neck[h][i].rx += 0.05f; jaw[h].rx += 0.25f; }
                thighL.rx -= 0.4f; thighR.rx -= 0.4f;
            }
            case S.STAGGER, S.CLASH_RECOIL, S.HELD -> {
                float r = Mth.lerp(pt, prevRecoil, recoil);
                float w = st == S.HELD ? Mth.sin(age * 0.6f) : Mth.sin(t * 0.7f) * Math.max(0, 1 - t / 22f);
                for (int h = 0; h < 3; h++) {
                    neck[h][0].rx -= 0.3f * r; head[h].rx -= 0.4f * r; jaw[h].rx += 0.7f * Math.max(r, st == S.HELD ? 0.8f : 0);
                    neck[h][3].ry += w * 0.25f * (h == 2 ? -1 : 1);
                }
                spine.rz += w * 0.08f;
            }
            default -> {}
        }

        if (fallAmt > 0.01f) {
            float w = Mth.sin(age * 0.3f);
            for (int h = 0; h < 3; h++) {
                neck[h][0].rx -= 0.6f * fallAmt;
                for (int i = 1; i <= NECK; i++) neck[h][i].ry += Mth.sin(age * 0.2f + i * 0.5f + h * 2) * 0.12f * fallAmt;
                jaw[h].rx += (0.4f + 0.3f * w) * fallAmt;
            }
            thighL.rx -= (0.6f + 0.3f * w) * fallAmt; thighR.rx -= (0.4f - 0.3f * w) * fallAmt;
            shinL.rx += 0.6f * fallAmt; shinR.rx += 0.6f * fallAmt;
        }

        // severed heads: the neck ends in a stump; regrowth pushes a new neck and head out of it
        for (int h = 0; h < 3; h++) {
            int g = growth(h);
            if (g >= 100) continue;
            float gf = g / 100f;
            neck[h][7].scale = gf <= 0 ? 0.0001f : 0.15f + 0.85f * smooth(gf);
            float writhe = 1 - gf;
            for (int i = 7; i <= NECK; i++) {
                neck[h][i].ry += Mth.sin(age * 0.35f + i) * 0.35f * writhe;
                neck[h][i].rx += Mth.cos(age * 0.3f + i * 1.3f) * 0.2f * writhe;
            }
            jaw[h].rx += 0.6f * writhe;
            neck[h][5].ry += Mth.sin(age * 0.2f) * 0.1f;
        }
        skel.compute();
    }

    private void poseWings(float sp, boolean flying, float age, float t, int st) {
        float fold = 1 - sp;
        float flap = 0, fingerCurl = 0;
        if (flying) {
            float speed = st == S.TAKEOFF || st == S.LIFT ? 0.42f : 0.3f;
            float c = Mth.sin(age * speed);
            flap = c * 0.75f;
            fingerCurl = Math.max(0, -Mth.cos(age * speed)) * 0.35f;
        } else if (st == S.WING_BUFFET) {
            float cyc = Mth.clamp((t - 8) / 14f, 0, 1);
            flap = t < 8 ? -0.6f * smooth(t / 8f) : -0.6f + 1.5f * smooth(cyc);
        } else if (st == S.ROAR) {
            flap = Mth.sin(t * 0.25f) * 0.12f * sp;
        }
        for (int w = 0; w < 2; w++) {
            float m = w == 0 ? 1 : -1;
            // spread: arm sweeps up and out. Folded: humerus back and up, forearm forward, fingers along the body
            wing0[w].rx += -0.35f * sp - flap + fold * -0.5f;
            wing0[w].ry += m * (0.35f * sp + fold * 1.2f);
            wing0[w].rz += m * (fold * 0.3f);
            wing1[w].ry += m * (-0.15f * sp + fold * -2.5f);
            wing1[w].rx += -flap * 0.3f + fold * 0.25f;
            wing2[w].ry += m * (0.2f * sp + fold * 2.45f);
            wing2[w].rx += fold * 0.35f + flap * 0.15f;
            for (int k = 0; k < 4; k++) {
                finger[w][k].ry += m * (fold * (0.15f - k * 0.38f) - fingerCurl * 0.1f * k);
                finger[w][k].rx += fold * (0.25f + k * 0.12f) + fingerCurl * 0.4f;
                fingerB[w][k].rx += fingerCurl * 0.3f + fold * 0.1f;
            }
        }
    }

    // ------------------------------------------------------------------ behaviour
    @Override
    protected void titanAi(LivingEntity target, double gap) {
        boolean titan = target instanceof TitanEntity;
        if (titan && roarCooldown <= 0 && gap > 25 && random.nextFloat() < 0.02f) { startRoar(); return; }
        if (target instanceof TitanEntity tt && tt.state() == S.FALLEN && gap < 10 && attackCooldown <= 0) { startPin(tt); return; }
        if (beamCooldown <= 0 && headsAlive() > 0 && gap > 8 && gap < 95 && random.nextFloat() < 0.04f && hasLineOfSight(target)) { startBeam(false); return; }
        if (gap > 55 && random.nextFloat() < 0.02f) { setState(S.TAKEOFF); return; }
        if (gap < 4 && attackCooldown <= 0) {
            float r = random.nextFloat();
            if (r < 0.55f && headsAlive() > 0) { setState(S.BITE); biteHits = 0; attackCooldown = 40; }
            else if (r < 0.85f) { setState(S.WING_BUFFET); attackCooldown = 50; }
            else if (beamCooldown < 60) startBeam(true);
            else { setState(S.BITE); biteHits = 0; attackCooldown = 40; }
            return;
        }
        if (gap > 2.5) moveTowards(target.position(), gap > 30 ? 0.3f : 0.24f, 2.8f);
        else turnTo(yawTo(target.position()), 2.8f);
    }

    private void startPin(TitanEntity t) {
        setState(S.PIN);
        setGrabbed(t);
        t.setState(S.PINNED);
        attackCooldown = 60;
        playSound(SoundEvents.ENDER_DRAGON_GROWL, 10f, 0.7f);
    }

    @Override
    protected void tickRoar() {
        int[] at = {6, 20, 30};
        float[] pitch = {0.55f, 0.7f, 0.62f};
        for (int h = 0; h < 3; h++) if (stateTime == at[h] && headUp(h)) playSound(SoundEvents.ENDER_DRAGON_GROWL, 9f, pitch[h]);
        if (stateTime == 40) playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 4f, 0.6f);
    }

    @Override
    protected void serverTick() {
        super.serverTick();
        for (int h = 0; h < 3; h++) {
            int g = growth(h);
            if (g >= 100) { headHp[h] = Math.min(220, headHp[h] + 0.05f); continue; }
            if (regrowDelay[h] > 0) { regrowDelay[h]--; continue; }
            if (tickCount % 2 == 0) setGrowth(h, g + 1);
            if (g == 1) playSound(SoundEvents.SLIME_SQUISH, 6f, 0.3f);
            if (g + 1 >= 100) { playSound(SoundEvents.ENDER_DRAGON_GROWL, 10f, 0.75f); }
        }
    }

    @Override
    protected void tickState(LivingEntity target) {
        int st = state();
        ServerLevel sl = (ServerLevel) level();
        switch (st) {
            case S.IDLE -> {
                if (walkAmt > 0.2f && tickCount % 12 == 0) { playSound(SoundEvents.RAVAGER_STEP, 4f, 0.45f); level().broadcastEntityEvent(this, EV_SHAKE_SMALL); }
            }
            case S.BITE -> {
                if (target != null) turnTo(yawTo(target.position()), 3f);
                for (int h = 0; h < 3; h++) {
                    int at = 4 + h * 6 + 5;
                    if (stateTime == at - 3 && headUp(h)) playSound(SoundEvents.ENDER_DRAGON_AMBIENT, 6f, 0.7f + h * 0.1f);
                    if (stateTime == at && target != null && headUp(h)) {
                        Vec3 m = mouth(h);
                        boolean hit = target instanceof TitanEntity tt ? tt.getBoundingBox().inflate(1.5).contains(m) || gapTo(tt) < 2.5 : target.distanceToSqr(m) < 25;
                        if (hit) {
                            biteHits++;
                            Vec3 pull = position().subtract(target.position()).multiply(1, 0, 1).normalize();
                            fling(target, pull.scale(target instanceof TitanEntity ? 0.22 : 0.8).add(0, 0.1, 0), 18);
                            playSound(SoundEvents.PLAYER_ATTACK_CRIT, 6f, 0.6f);
                            sl.sendParticles(ParticleTypes.CRIT, m.x, m.y, m.z, 20, 0.5, 0.5, 0.5, 0.3);
                        }
                    }
                }
                if (stateTime > 30) {
                    if (biteHits >= 3 && target instanceof GodzillaEntity g && random.nextFloat() < 0.45f && g.state() != S.HELD && !S.down(g.state())) startLift(g);
                    else setState(S.IDLE);
                }
            }
            case S.WING_BUFFET -> {
                if (target != null) turnTo(yawTo(target.position()), 2f);
                if (stateTime == 8) playSound(SoundEvents.ENDER_DRAGON_FLAP, 10f, 0.4f);
                if (stateTime == 18) {
                    playSound(SoundEvents.ENDER_DRAGON_FLAP, 10f, 0.3f);
                    Vec3 f = Vec3.directionFromRotation(0, getYRot());
                    level().broadcastEntityEvent(this, EV_SHAKE_SMALL);
                    for (Entity e : entitiesNear(position().add(f.scale(12)).add(0, 6, 0), 16)) {
                        Vec3 rel = e.position().subtract(position());
                        if (rel.normalize().dot(f) < 0.2) continue;
                        double d = rel.length();
                        float power = (float) Mth.clamp(1.25 - d / 40, 0.4, 1.1);
                        fling(e, f.scale(power).add(0, 0.25, 0), 20 * power);
                    }
                    for (int i = 0; i < 6; i++) dust(sl, position().add(f.scale(6 + i * 4)), 6, 16);
                }
                if (stateTime > 32) setState(S.IDLE);
            }
            case S.TAKEOFF -> {
                moveY = 0.45;
                if (stateTime % 8 == 0) { playSound(SoundEvents.ENDER_DRAGON_FLAP, 10f, 0.4f); dust(sl, position(), 14, 30); }
                if (stateTime > 22) setState(S.FLY);
            }
            case S.FLY -> {
                if (stateTime % 9 == 0) playSound(SoundEvents.ENDER_DRAGON_FLAP, 8f, 0.45f);
                double ground = groundBelow();
                if (target == null) { moveY = (ground + 14 - getY()) * 0.05; if (stateTime > 200) setState(S.LAND); return; }
                double alt = Math.max(ground + 10, target.getY() + 14);
                moveY = Mth.clamp((alt - getY()) * 0.05, -0.35, 0.35);
                Vec3 tp = target.position();
                turnTo(yawTo(tp), 3f);
                Vec3 f = Vec3.directionFromRotation(0, getYRot());
                double gap = gapTo(target);
                double spd = gap > 20 ? 0.5 : 0.2;
                setDeltaMovement(getDeltaMovement().add((f.x * spd - getDeltaMovement().x) * 0.08, 0, (f.z * spd - getDeltaMovement().z) * 0.08));
                if (gap < 16 || stateTime > 400) setState(S.LAND);
            }
            case S.LAND -> {
                moveY = -0.3;
                if (stateTime % 9 == 0) playSound(SoundEvents.ENDER_DRAGON_FLAP, 8f, 0.4f);
                if (onGround() && stateTime > 4) {
                    setState(S.IDLE);
                    playSound(SoundEvents.GENERIC_EXPLODE, 6f, 0.5f);
                    level().broadcastEntityEvent(this, EV_SHAKE_BIG);
                    dust(sl, position(), 14, 80);
                    if (griefing()) crater(blockPosition(), 3);
                }
                if (stateTime > 200) setState(S.IDLE);
            }
            case S.LIFT -> tickLift(sl);
            case S.PIN -> tickPin(sl);
            default -> {}
        }
    }

    private double groundBelow() {
        var p = blockPosition().mutable();
        for (int i = 0; i < 64; i++) { p.move(0, -1, 0); if (!level().getBlockState(p).isAir()) return p.getY() + 1; }
        return getY() - 64;
    }

    private void startLift(GodzillaEntity g) {
        setState(S.LIFT);
        setGrabbed(g);
        g.setState(S.HELD);
        playSound(SoundEvents.ENDER_DRAGON_GROWL, 10f, 0.6f);
    }

    @Override
    protected boolean isLiftedVictim() { return false; }

    private void tickLift(ServerLevel sl) {
        Entity e = grabbed();
        if (!(e instanceof TitanEntity t) || t.state() != S.HELD) { releaseGrabs(); setState(S.FLY); return; }
        moveY = stateTime < 55 ? 0.24 : 0;
        if (stateTime % 7 == 0) { playSound(SoundEvents.ENDER_DRAGON_FLAP, 10f, 0.35f); if (stateTime < 20) dust(sl, position(), 16, 40); }
        Vec3 f = Vec3.directionFromRotation(0, getYRot());
        Vec3 want = position().add(f.scale(radius() + t.radius() - 2.5)).add(0, -3.5, 0);
        t.setPos(want.x, want.y, want.z);
        t.setDeltaMovement(Vec3.ZERO);
        t.setYRot(getYRot() + 180); t.yBodyRot = t.getYRot();
        if (stateTime % 10 == 0) t.hurt(damageSources().mobAttack(this), 5);
        if (stateTime > 64) {
            setGrabbed(null);
            t.setState(S.FLUNG);
            t.setDeltaMovement(f.scale(0.4).add(0, -0.3, 0));
            t.hurtMarked = true;
            playSound(SoundEvents.ENDER_DRAGON_GROWL, 10f, 0.5f);
            setState(S.FLY);
        }
    }

    private void tickPin(ServerLevel sl) {
        Entity e = grabbed();
        if (!(e instanceof TitanEntity t) || t.state() != S.PINNED || stateTime > 170) {
            if (e instanceof TitanEntity t2 && t2.state() == S.PINNED) t2.setState(S.GET_UP);
            setGrabbed(null);
            setState(S.IDLE);
            setDeltaMovement(Vec3.directionFromRotation(0, getYRot() + 180).scale(0.4).add(0, 0.2, 0));
            return;
        }
        Vec3 dir = Vec3.directionFromRotation(0, t.hitYaw());
        Vec3 want = t.position().add(dir.scale(6.5)).add(0, 4.5, 0);
        Vec3 p = position().add(want.subtract(position()).scale(stateTime < 15 ? 0.15 : 0.5));
        setPos(p.x, p.y, p.z);
        setDeltaMovement(Vec3.ZERO);
        turnTo(t.hitYaw(), 6f);
        for (int h = 0; h < 3; h++) {
            if (!headUp(h)) continue;
            if ((stateTime + h * 5) % 15 == 6) {
                t.hurt(damageSources().mobAttack(this), 9);
                playSound(SoundEvents.PLAYER_ATTACK_CRIT, 6f, 0.5f + h * 0.1f);
                Vec3 m = mouth(h);
                sl.sendParticles(ParticleTypes.CRIT, m.x, m.y, m.z, 16, 0.6, 0.6, 0.6, 0.3);
                if (h == 0) level().broadcastEntityEvent(this, EV_SHAKE_SMALL);
            }
        }
    }

    /** Lightning crackle across the body while the beams are charged or firing (0..1). */
    public float crackle(float pt) {
        int st = state();
        float t = stateTime(pt);
        if (st == S.CHARGE_BEAM) return Mth.clamp(t / chargeTime(), 0, 1) * (0.6f + 0.4f * Math.abs(Mth.sin(t * 1.7f)));
        if (st == S.FIRE_BEAM) return 0.7f + 0.3f * Math.abs(Mth.sin((tickCount + pt) * 2.3f));
        return beamPower() * 0.8f;
    }
}
