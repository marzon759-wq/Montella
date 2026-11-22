package com.krolasyon.kaiju.ai;

import com.krolasyon.kaiju.data.KaijuSpec;
import com.krolasyon.kaiju.entity.KaijuEntity;
import com.krolasyon.kaiju.net.ControlInput;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

/**
 * The fighter. Every few ticks it reads the situation (range, which side the enemy is on, whether either of them is
 * down, staggered, grabbed, charging or firing a breath, its own health and breath readiness, what is around it to
 * pick up) and scores every attack that is legal right now; it then picks among the best with some randomness, so it
 * is not predictable, and learns: attacks that keep missing or being blocked lose weight, ones that land gain it.
 * Between attacks it manages distance: closes in, backs off, circles to the enemy's side, or keeps range for the breath.
 * A rider uses the same attack logic for contextual buttons ("light", "heavy", "tail", ...).
 */
public final class KaijuBrain {
    final KaijuEntity e;
    @Nullable public KaijuEntity target;
    @Nullable LivingEntity provoker;
    int provokeTicks, retarget, wanderTicks, circleDir = 1, circleTimer, thinkTimer = 60;
    float wanderYaw;
    final Random rnd = new Random();
    public boolean enabled = true;
    public int nextBreath = 400, retreatTicks, retreatCd, flyThink = 100;
    final java.util.Map<String, Integer> lastUse = new java.util.HashMap<>();

    public KaijuBrain(KaijuEntity e) { this.e = e; }

    public void provoke(LivingEntity le) {
        provoker = le;
        provokeTicks = 400;
    }

    // ------------------------------------------------------------------ perception
    void acquire() {
        if (--retarget > 0 && target != null && !target.isRemoved() && target.state != KaijuEntity.State.DEAD) return;
        retarget = 20;
        KaijuEntity best = null;
        double bd = Double.MAX_VALUE;
        for (KaijuEntity o : e.level().getEntitiesOfClass(KaijuEntity.class, e.getBoundingBox().inflate(700), x -> x != e && !x.isRemoved() && x.state != KaijuEntity.State.DEAD)) {
            if (e.owner != null && e.owner.equals(o.owner) && e.controller == null && o.controller == null && !com.krolasyon.kaiju.KaijuConfig.sameOwnerFights) continue;
            double d = o.distanceToSqr(e);
            if (o.brain.target == e) d *= 0.6; // whoever is fighting me first
            if (d < bd) { bd = d; best = o; }
        }
        target = best;
    }

    public float distPx(KaijuEntity t) {
        Vec3 a = e.position(), b = t.position();
        return (float) (Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.z - b.z) * (a.z - b.z)) / e.k());
    }

    /** -180..180, 0 = straight ahead, + = to the left */
    public float bearing(Vec3 p) {
        Vec3 d = p.subtract(e.position());
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
        return Mth.wrapDegrees(e.getYRot() - yaw) * -1f;
    }

    static float yawTo(Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        return (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
    }

    String side(float bearing) {
        float b = Math.abs(bearing);
        if (b < 50) return "front";
        if (b > 130) return "back";
        return bearing > 0 ? "left" : "right";
    }

    boolean arcOk(KaijuSpec.Attack a, float bearing) {
        if (a.arc.equals("any")) return true;
        return a.arc.equals(side(bearing)) || a.arc.equals("front") && Math.abs(bearing) < 70;
    }

    // ------------------------------------------------------------------ scoring
    float score(KaijuSpec.Attack a, @Nullable KaijuEntity t, String only) {
        if (!e.ready(a) || !e.meets(a, t)) return 0;
        if (a.cat.equals("fly") && !e.flying) return 0;   // take-off is decided by tickFlyer, not scored
        if (a.id.equals("land")) return 0;
        if (e.controller == null && e.stamina < e.cost(a) * 0.8f) return 0;  // too tired for this one
        Integer used = lastUse.get(a.id);
        if (only != null && !only.contains(a.cat)) return 0;
        float s = a.weight;
        float[] st = e.stats.get(a.id);
        if (st != null && st[0] >= 2) s *= 0.55f + Math.min(1f, st[1] / st[0]);  // learning
        if (used != null && e.tickCount - used < 300) s *= 0.25f;  // vary: not the same move again so soon
        if (t == null) {
            return a.tags.contains("world") || a.cat.equals("intimidate") ? s * 0.2f : 0;
        }
        float d = distPx(t);
        float bear = bearing(t.position());
        boolean inRange = d >= a.minRange && d <= a.maxRange;
        if (!inRange) return 0;
        if (!arcOk(a, bear)) return 0;
        boolean tDown = t.isDown();
        boolean tStag = t.state == KaijuEntity.State.REACT && !tDown;
        boolean tCharging = t.flag(KaijuEntity.F_CHARGING);
        boolean tFiring = t.flag(KaijuEntity.F_FIRING);
        boolean meDown = e.state == KaijuEntity.State.DOWN;
        float hp = e.hp / e.spec.maxHp;
        if (meDown) {
            if (!a.cat.equals("ground") && !a.id.equals("atomic_ground")) return 0;
            s *= a.tags.contains("getup") ? (d < 60 ? 2.2f : 0.6f) : 1.4f;
            if (a.id.equals("roll_away") && t.attack != null) s *= 3;
            return s;
        }
        if (a.tags.contains("mount")) s *= tDown ? 7f : 0f;
        else if (tDown && !a.isBreath()) s *= 0.35f;
        if (tDown && a.id.equals("atomic_ground_pin")) s *= 2.5f;
        if (a.tags.contains("grapple")) s *= tStag ? 3.5f : (t.guardTicks > 0 ? 2.5f : 0.6f);
        if (a.tags.contains("heavy")) s *= tStag ? 2.2f : 1f;
        if (a.isBreath()) {
            // like the film: the breath is a rare, deliberate act at range, never sprayed into the sky
            if (e.controller == null && (a.id.equals("atomic_aerial") || a.id.equals("atomic_point_blank") || a.id.equals("atomic_sweep")
                    || a.id.equals("atomic_ground"))) return 0;  // lying on its back the head points at the sky: get up first
            boolean clash = (tCharging || tFiring) && d > 140;
            if (e.tickCount < nextBreath && !clash) return 0;
            if (a.id.equals("atomic_ground_pin")) { if (!(tDown && d < 90)) return 0; s *= 0.6f; }
            else if (a.id.equals("atomic_ground")) { if (d < 130) return 0; }
            else if (d < e.spec.breathMinPx && !a.has("air")) return 0;
            s *= d > 320 ? 3f : 1.2f;
            if (clash) s *= 6f;
            if (a.id.equals("atomic_charged_max")) s *= hp < 0.4f && d > 300 ? 1.5f : 0f;
            if (a.id.equals("atomic_burst")) s *= 0.5f;
        }
        if (a.id.equals("nuclear_pulse")) {
            s *= (e.state == KaijuEntity.State.HELD || e.heldBy != null) ? 30f : (hp < 0.35f && d < 90 ? 5f : 0.08f);
        }
        if (a.cat.equals("defense")) {
            boolean threat = (t.attack != null && t.attack.hits.size() > 0 && d <= t.attack.maxRange + 12) || tFiring;
            if (!threat) return 0;
            s *= tFiring ? 3f : 1.6f;
            if (a.id.equals("parry_counter")) s *= t.attack != null && t.attack.cat.equals("claw") ? 1.8f : 0.4f;
            if (a.id.equals("block_arms") && hp < 0.5f) s *= 1.6f;
        }
        if (a.cat.equals("world")) {
            if (a.id.equals("throw_held")) s *= d > 70 ? 6f : 0.2f;
            else if (a.id.equals("swing_held")) s *= d < 90 ? 4f : 0.1f;
            else if (a.id.equals("kick_debris")) s *= d > 120 ? 0.8f : 0f;
            else s *= d > 220 ? 0.9f : 0.12f;   // uproot / rip only when there is distance to use it
        }
        if (a.cat.equals("intimidate")) s *= d > 250 && e.tickCount % 400 < 60 ? 0.6f : 0.03f;
        if (a.cat.equals("tail")) s *= side(bear).equals("front") ? (d < 70 ? 0.6f : 0.25f) : 1.8f;
        if (a.cat.equals("claw") || a.cat.equals("bite") || a.cat.equals("leg") || a.cat.equals("grapple")) s *= side(bear).equals("front") ? 2.2f : 0.3f;
        if (a.cat.equals("body")) s *= d < 70 ? 1.6f : 1f;
        if (a.tags.contains("gap_close")) s *= d > 110 ? 2.2f : 0.4f;
        if (a.tags.contains("surround") && e.level().getEntitiesOfClass(KaijuEntity.class, e.getBoundingBox().inflate(120 * e.k()), x -> x != e).size() > 1) s *= 3f;
        // out of reach: close the distance first (walk in, or a gap-closing charge) instead of fighting the air
        if (d > 60 && !a.tags.contains("gap_close") && !a.isBreath() && !a.cat.equals("world") && !a.cat.equals("defense")
                && !a.tags.contains("mount") && !(a.cat.equals("tail") && !side(bear).equals("front"))) s *= 0.3f;
        // combos: after landing a hit, keep pressing with fast follow-ups
        if (e.tickCount - e.lastLanded < 30 && a.len < 2.0f) s *= 1.8f;
        return s;
    }

    @Nullable
    KaijuSpec.Attack pick(@Nullable KaijuEntity t, String only, float threshold) {
        List<KaijuSpec.Attack> list = new ArrayList<>();
        List<Float> w = new ArrayList<>();
        float best = 0;
        for (KaijuSpec.Attack a : e.spec.attacks) {
            float s = score(a, t, only);
            if (s <= 0) continue;
            list.add(a);
            w.add(s * s);
            best = Math.max(best, s);
        }
        if (list.isEmpty() || best < threshold) return null;
        float tot = 0;
        for (float x : w) tot += x;
        float r = rnd.nextFloat() * tot;
        KaijuSpec.Attack chosen = list.get(list.size() - 1);
        for (int i = 0; i < list.size(); i++) {
            r -= w.get(i);
            if (r <= 0) { chosen = list.get(i); break; }
        }
        lastUse.put(chosen.id, e.tickCount);
        return chosen;
    }

    // ------------------------------------------------------------------ AI
    public void tick() {
        if (!enabled) { e.wantSpeed = 0; return; }
        acquire();
        if (provokeTicks > 0) provokeTicks--;
        e.wantSpeed = 0;
        e.moveFwd = 1;
        e.moveSide = 0;
        if (e.state != KaijuEntity.State.IDLE) return;
        KaijuEntity t = target;
        if (t == null) { wander(); return; }
        float d = distPx(t);
        float bear = bearing(t.position());
        e.faceYaw = yawTo(e.position(), t.position());
        if (tickFlyer(t, d)) return;
        // tired or badly hurt: back off for a while, catch breath, then come again
        float hpr = e.hp / e.spec.maxHp;
        if (retreatCd > 0) retreatCd--;
        if (retreatTicks <= 0 && retreatCd <= 0 && (e.stamina < 22 || hpr < 0.35f && e.tickCount - e.lastHurtTick() < 40 && rnd.nextFloat() < 0.08f)) {
            retreatTicks = 70 + rnd.nextInt(90);
            retreatCd = 420;
            com.krolasyon.kaiju.KaijuMod.LOG.info("[KAIJU] #{} backs off (stamina={} hp={})", e.getId(), (int) e.stamina, (int) e.hp);
        }
        if (retreatTicks > 0) {
            retreatTicks--;
            if (d < 45 && --thinkTimer <= 0) {  // cornered: a quick defensive move
                thinkTimer = 10;
                KaijuSpec.Attack a = pick(t, "defense,tail,claw", 0.2f);
                if (a != null && e.startAttack(a.id)) return;
            }
            if (d > 140 && --thinkTimer <= 0) {  // at range again: maybe the breath
                thinkTimer = 20;
                KaijuSpec.Attack a = pick(t, "atomic,beam", 0.5f);
                if (a != null && e.startAttack(a.id)) { nextBreath = e.tickCount + e.spec.breathPacingMin + rnd.nextInt(Math.max(1, e.spec.breathPacingMax - e.spec.breathPacingMin)); retreatTicks = 0; return; }
            }
            e.wantSpeed = d < 240 ? -e.spec.walkPx * 0.85f : 0;
            if (retreatTicks == 0 && rnd.nextFloat() < 0.35f) e.startAttack("roar");
            return;
        }
        if (--thinkTimer <= 0) {
            thinkTimer = d > 60 ? 12 + rnd.nextInt(14) : 3 + rnd.nextInt(5);
            KaijuSpec.Attack a = pick(t, null, d > 60 ? 0.9f : 0.25f);
            if (a != null && e.startAttack(a.id)) {
                if (a.isBreath()) nextBreath = e.tickCount + e.spec.breathPacingMin + rnd.nextInt(Math.max(1, e.spec.breathPacingMax - e.spec.breathPacingMin));
                return;
            }
        }
        // footwork
        float want = e.heldDebris != null ? 160 : (e.beam.firing() ? d : 38);
        if (--circleTimer <= 0) { circleTimer = 60 + rnd.nextInt(80); circleDir = rnd.nextBoolean() ? 1 : -1; }
        if (d > want + 15) {
            e.wantSpeed = d > 170 ? e.spec.fastPx : e.spec.walkPx;
            if (Math.abs(bear) > 35) e.wantSpeed *= 0.4f;
        } else if (d < want - 18) {
            e.wantSpeed = -e.spec.walkPx * 0.7f;
        } else if (rnd.nextFloat() < 0.5f) {
            // circle toward the enemy's flank
            e.faceYaw += circleDir * 25;
            e.wantSpeed = e.spec.walkPx * 0.6f;
        }
    }

    void wander() {
        if (provoker != null && provokeTicks > 0 && provoker.isAlive()) {
            e.faceYaw = yawTo(e.position(), provoker.position());
            double d = provoker.distanceTo(e) / e.k();
            if (d > 60) e.wantSpeed = e.spec.walkPx;
            else if (e.canAct() && rnd.nextInt(30) == 0) e.startAttack(rnd.nextBoolean() ? "stomp_r" : "tail_spin_360");
            return;
        }
        if (--wanderTicks <= 0) {
            wanderTicks = 160 + rnd.nextInt(240);
            wanderYaw = e.getYRot() + rnd.nextFloat() * 160 - 80;
            if (rnd.nextInt(8) == 0) e.startAttack("roar");
        }
        e.faceYaw = wanderYaw;
        e.wantSpeed = wanderTicks % 200 < 120 ? e.spec.walkPx * 0.8f : 0;
    }

    public void tickDown() {
        if (e.state != KaijuEntity.State.DOWN) return;
        if (e.controller != null && !e.flag(KaijuEntity.F_AI_ASSIST)) return;
        if (e.pinTicks > 0 || e.downTicks < 16) return;
        acquire();
        KaijuEntity t = target;
        float d = t == null ? 9999 : distPx(t);
        if (t != null && d < 160 && rnd.nextFloat() < 0.12f) {
            KaijuSpec.Attack a = pick(t, "ground,atomic", 0.2f);
            if (a != null && e.startAttack(a.id)) { if (a.isBreath()) nextBreath = e.tickCount + e.spec.breathPacingMin + rnd.nextInt(Math.max(1, e.spec.breathPacingMax - e.spec.breathPacingMin)); return; }
        }
        if (e.downTicks > 30 + rnd.nextInt(30)) e.getUp();
    }

    // ------------------------------------------------------------------ rider
    public void tickControlled() {
        ControlInput in = e.input;
        acquire();
        KaijuEntity t = crosshairTarget();
        if (t != null) target = t;
        if (e.state == KaijuEntity.State.DOWN) {
            if (in.pressed(ControlInput.ATTACK)) e.startAttack("ground_kick");
            else if (in.pressed(ControlInput.HEAVY)) e.startAttack("ground_tail_sweep");
            else if (in.pressed(ControlInput.BREATH)) e.startAttack(e.spec.key("breath_ground", "atomic_ground"));
            else if (in.pressed(ControlInput.DODGE)) e.startAttack("roll_away");
            else if (in.pressed(ControlInput.GRAB)) e.startAttack("ground_claw_grab");
            else if (in.pressed(ControlInput.JUMP) || in.forward > 0.5f && e.downTicks > 10) {
                if (t != null && distPx(t) < 60 && e.startAttack("kip_up_strike")) return;
                e.getUp();
            }
            in.consume();
            return;
        }
        if (e.flying) { tickControlledFlight(in, t); in.consume(); return; }
        // movement relative to the camera
        e.wantSpeed = 0;
        e.moveFwd = 1;
        e.moveSide = 0;
        boolean sprint = in.down(ControlInput.SPRINT);
        if (Math.abs(in.forward) > 0.1f || Math.abs(in.strafe) > 0.1f) {
            if (in.forward < -0.1f && Math.abs(in.strafe) < 0.1f) {
                e.faceYaw = in.yaw;
                e.wantSpeed = -e.spec.walkPx * 0.7f;
            } else {
                float ang = (float) (Mth.atan2(in.strafe, Math.max(0.0001f, in.forward)) * Mth.RAD_TO_DEG);
                if (in.forward < -0.1f) ang = in.strafe > 0 ? 135 : -135;
                e.faceYaw = in.yaw - ang;
                e.wantSpeed = sprint ? e.spec.fastPx : e.spec.walkPx;
            }
        } else if (e.beam.firing() || in.down(ControlInput.GUARD)) {
            e.faceYaw = in.yaw;
        }
        // busy (a hit-stop frame): keep the press buffered for a moment instead of dropping it
        if (!e.canAct()) { if (e.tickCount - in.latchedAt > 12) in.consume(); return; }
        if (e.tickCount - in.latchedAt > 12) in.consume();  // a press from long ago is stale
        String id = null;
        if (in.pressed(ControlInput.ATTACK)) id = bestOf(t, "claw,bite,leg,body,mount", t != null && t.isDown() ? "stomp_downed" : (rnd.nextBoolean() ? "claw_swipe_l" : "claw_swipe_r"));
        else if (in.pressed(ControlInput.HEAVY)) id = bestOf(t, "tail,body,grapple,mount", "tail_spin_360");
        else if (in.pressed(ControlInput.BREATH)) {
            if (e.flying) id = e.spec.key("breath_air", "atomic_breath");
            else if (t != null && t.isDown() && distPx(t) < 90) id = e.spec.key("breath_down", "atomic_ground_pin");
            else if (in.pitch < -25) id = e.spec.key("breath_up", "atomic_aerial");
            else if (sprint) id = e.spec.key("breath_sprint", "atomic_sweep");
            else if (in.down(ControlInput.GUARD)) id = e.spec.key("breath_guard", "atomic_charged_max");
            else id = e.spec.key("breath", "atomic_breath");
        } else if (in.pressed(ControlInput.GRAB)) {
            if (e.heldDebris != null) id = t != null && distPx(t) < 80 ? "swing_held" : "throw_held";
            else if (t != null && distPx(t) < 58) id = in.down(ControlInput.GUARD) ? "grab_bodyslam" : "grab_throw";
            else id = sprint ? "rip_ground_throw" : "uproot_tree";
        } else if (in.pressed(ControlInput.TAIL)) {
            String sd = t == null ? "none" : side(bearing(t.position()));
            id = switch (sd) {
                case "left" -> "tail_whip_l";
                case "right" -> "tail_whip_r";
                case "back" -> rnd.nextBoolean() ? "tail_ground_slam_back" : "tail_stab_back";
                case "front" -> "tail_spin_360";
                default -> "tail_spin_360";
            };
        } else if (in.pressed(ControlInput.BITE)) id = e.spec.keys.containsKey("bite") ? e.spec.key("bite", "bite_lunge") : bestOf(t, "bite,grapple", "bite_lunge");
        else if (in.pressed(ControlInput.PULSE)) id = e.spec.key("pulse", "nuclear_pulse");
        else if (in.pressed(ControlInput.ROAR)) id = "roar";
        else if (in.pressed(ControlInput.DODGE)) id = in.strafe > 0.1f ? "sidestep_l" : in.strafe < -0.1f ? "sidestep_r" : "backstep";
        else if (in.pressed(ControlInput.SPECIAL)) id = e.special;
        else if (in.pressed(ControlInput.JUMP)) id = e.spec.canFly ? "takeoff" : sprint ? "ground_pound" : (rnd.nextBoolean() ? "stomp_l" : "stomp_r");
        else if (in.down(ControlInput.GUARD) && e.wantSpeed == 0) id = "block_arms";
        if (in.pressed(ControlInput.AI)) e.input.aiToggle = true;
        in.consume();
        if (id != null) {
            KaijuSpec.Attack a = e.spec.attackById.get(id);
            if (a != null && !e.ready(a)) return;
            e.startAttack(id);
        }
    }

    /** a rider on the wing: W flies where the camera looks (up and down too), Space lands, the buttons are air moves */
    void tickControlledFlight(ControlInput in, @Nullable KaijuEntity t) {
        e.faceYaw = in.yaw;
        e.wantSpeed = in.forward > 0.1f ? e.spec.flyPx * (in.down(ControlInput.SPRINT) ? 1.4f : 1f) : 0;
        if (in.forward > 0.1f) e.flyAlt += -Math.sin(Math.toRadians(in.pitch)) * e.spec.flyPx * e.k() / 20f * 0.8f;
        double ground = e.groundAt(e.position());
        e.flyAlt = Mth.clamp(e.flyAlt, ground + 16 * e.k(), ground + 260 * e.k());
        if (!e.canAct()) return;
        String id = null;
        if (in.pressed(ControlInput.JUMP)) id = "land";
        else if (in.pressed(ControlInput.ATTACK)) id = e.spec.key("air_attack", "land");
        else if (in.pressed(ControlInput.GRAB)) id = e.spec.key("air_grab", "land");
        else if (in.pressed(ControlInput.BREATH)) id = e.spec.key("breath_air", "land");
        else if (in.pressed(ControlInput.ROAR)) id = "roar";
        if (id != null) e.startAttack(id);
    }

    /** the AI on the wing: climb over the enemy, then stoop, snatch or breathe, and come down again */
    boolean tickFlyer(KaijuEntity t, float d) {
        if (!e.spec.canFly) return false;
        if (!e.flying) {
            // far away it takes wing to close in; in the thick of it, now and then it breaks off into the air
            if (--flyThink <= 0) {
                flyThink = d > 230 ? 60 : 240;
                float chance = d > 230 ? 0.35f : 0.18f;
                if (retreatTicks > 0) chance = 0.5f;
                if (rnd.nextFloat() < chance && e.stamina > 30 && e.startAttack("takeoff")) return true;
            }
            return false;
        }
        e.flyAlt = Math.max(e.flyAlt, t.getY() + 45 * e.k());
        if (e.flyTicks > 900 || e.stamina < 18) { e.startAttack("land"); return true; }
        if (--thinkTimer <= 0) {
            thinkTimer = 15 + rnd.nextInt(20);
            KaijuSpec.Attack a = pick(t, "fly,beam", 0.2f);
            if (a != null && e.startAttack(a.id)) { if (a.isBreath()) nextBreath = e.tickCount + e.spec.breathPacingMin; return true; }
        }
        float horiz = d;
        e.wantSpeed = horiz > 70 ? e.spec.flyPx : (horiz > 30 ? e.spec.flyPx * 0.4f : 0);
        return true;
    }

    String bestOf(@Nullable KaijuEntity t, String cats, String fallback) {
        KaijuSpec.Attack a = pick(t, cats, 0.01f);
        return a != null ? a.id : fallback;
    }

    @Nullable
    KaijuEntity crosshairTarget() {
        if (e.controller == null) return target;
        Vec3 eye = e.controller.getEyePosition();
        Vec3 look = e.controller.getLookAngle();
        KaijuEntity best = null;
        double bd = Double.MAX_VALUE;
        for (KaijuEntity o : e.level().getEntitiesOfClass(KaijuEntity.class, e.getBoundingBox().inflate(700), x -> x != e && x.state != KaijuEntity.State.DEAD)) {
            Vec3 c = o.nodePos(o.spec.node("chest"));
            Vec3 rel = c.subtract(eye);
            double along = rel.dot(look);
            if (along < 0) continue;
            double off = rel.subtract(look.scale(along)).length() / Math.max(1, along);
            if (off < 0.5 && off < bd) { bd = off; best = o; }
        }
        return best != null ? best : target;
    }

    // ------------------------------------------------------------------ aiming
    /** where thrown things should go: the enemy's chest, led by its velocity (or the rider's crosshair) */
    public Vec3 aimPoint() {
        if (e.controller != null && !e.flag(KaijuEntity.F_AI_ASSIST)) {
            Vec3 eye = e.controller.getEyePosition();
            Vec3 end = eye.add(e.controller.getLookAngle().scale(700));
            KaijuEntity t = crosshairTarget();
            if (t != null) return t.nodePos(t.spec.node("chest"));
            BlockHitResult hit = e.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e.controller));
            return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : end;
        }
        if (target == null) return e.position().add(e.forward().scale(200));
        Vec3 c = target.nodePos(target.spec.node("chest"));
        double dist = c.distanceTo(e.position());
        return c.add(target.knock.scale(Math.min(40, dist / 20)));
    }

    public Vec3 beamDirection(Vec3 headDir, float pitchExtra) {
        Vec3 mouth = e.nodePos(e.spec.node("mouth"));
        if (e.controller != null && !e.flag(KaijuEntity.F_AI_ASSIST)) {
            Vec3 p = aimPoint();
            return p.subtract(mouth).normalize();
        }
        Vec3 d;
        if (target != null) {
            Vec3 c = target.nodePos(target.spec.node("chest")).add(target.knock.scale(6));
            d = c.subtract(mouth).normalize();
        } else {
            d = new Vec3(headDir.x, 0, headDir.z).normalize();
        }
        // never into the sky: at most 15 degrees above the horizon, at most 50 below
        double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
        double pitch = Math.atan2(d.y, horiz);
        pitch = Mth.clamp(pitch, Math.toRadians(-50), Math.toRadians(15));
        return new Vec3(d.x / Math.max(1e-6, horiz) * Math.cos(pitch), Math.sin(pitch), d.z / Math.max(1e-6, horiz) * Math.cos(pitch));
    }
}
