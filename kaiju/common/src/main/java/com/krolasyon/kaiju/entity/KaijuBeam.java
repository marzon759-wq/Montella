package com.krolasyon.kaiju.entity;

import com.google.gson.JsonObject;
import com.krolasyon.kaiju.data.KaijuSpec;
import com.krolasyon.kaiju.net.Net;
import com.krolasyon.kaiju.world.WorldSmash;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;

/**
 * The atomic breath, server side. While the charge clip runs the plates come out tail-to-head (the client lights them
 * from their slide-out); the beam exists only in the fire phase. Each tick it starts at the mouth node, points where the
 * head points (plus the attack's pitch, the AI's lead or the rider's crosshair, limited to what the neck can reach),
 * and its front advances at 45 blocks/tick through air but has to burn its way through terrain. A kaiju in the path is
 * burnt where it is hit (the region's heat rises and the skin cracks orange), pushed along the beam and staggered
 * once its poise breaks. Two beams that meet lock into a clash: the meeting point slides toward the weaker breath
 * until one overpowers the other and drives the blast into its owner.
 */
public final class KaijuBeam {
    static final Set<KaijuBeam> ACTIVE = Collections.newSetFromMap(new WeakHashMap<>());

    final KaijuEntity e;
    KaijuSpec.Attack atk;
    boolean firing;
    int cooldown;
    float power, width, length;
    Vec3 start = Vec3.ZERO, end = Vec3.ZERO, dir = new Vec3(0, 0, 1);
    float pitchExtra;
    boolean sweep;
    // clash
    KaijuBeam clashWith;
    float clashS = 0.5f;
    int fireTicks, blastTimer, chargeSoundTick;

    KaijuBeam(KaijuEntity e) { this.e = e; }

    public boolean firing() { return firing; }

    void begin(KaijuSpec.Attack a) {
        atk = a;
        JsonObject b = a.beam;
        power = b.get("power").getAsFloat();
        width = b.get("width").getAsFloat();
        pitchExtra = b.has("pitch") ? b.get("pitch").getAsFloat() : 0;
        sweep = b.has("sweep");
        length = 0;
        fireTicks = 0;
        e.level().playSound(null, e.getX(), e.getY() + 40, e.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 10f, 0.5f);
    }

    void phase(int phase, int t, int len) {
        // plate clicks: the charge pushes the plates out one by one from the tail; play a crackle as the wave passes
        if (phase == 0 && e.level() instanceof ServerLevel sl) {
            float prog = t / (float) Math.max(1, len);
            if (t % 3 == 0) {
                int tail0 = e.spec.node("tail16"), neck = e.spec.node("neck");
                int from = tail0, to = neck;
                // walk the spine nodes from the tail tip toward the neck
                List<Integer> spine = new ArrayList<>();
                for (int i = 16; i >= 1; i--) spine.add(e.spec.node("tail" + i));
                spine.add(e.spec.node("hips"));
                spine.add(e.spec.node("belly"));
                spine.add(e.spec.node("chest"));
                spine.add(neck);
                int idx = Mth.clamp((int) (prog * spine.size()), 0, spine.size() - 1);
                Vec3 p = e.nodePos(spine.get(idx)).add(0, e.nodeR(spine.get(idx)) * 1.2, 0);
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 14, 2, 2, 2, 0.4);
                sl.sendParticles(new DustParticleOptions(new Vector3f(0.4f, 0.85f, 1f), 3f), p.x, p.y, p.z, 10, 2, 2, 2, 0.05);
                e.level().playSound(null, p.x, p.y, p.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 4f, 0.6f + prog * 0.8f);
            }
            if (t == len - 8) e.level().playSound(null, e.getX(), e.getY() + 40, e.getZ(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 14f, 0.5f);
        }
        boolean fire = phase == 1;
        if (fire && !firing) {
            firing = true;
            ACTIVE.add(this);
            e.level().playSound(null, e.getX(), e.getY() + 40, e.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 24f, 0.5f);
            e.level().playSound(null, e.getX(), e.getY() + 40, e.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 24f, 0.6f);
            if (e.level() instanceof ServerLevel sl) {
                Vec3 m = e.nodePos(e.spec.node("mouth"));
                sl.sendParticles(ParticleTypes.SONIC_BOOM, m.x, m.y, m.z, 3, 1, 1, 1, 0);
                Net.shake(sl, m, 2f, 300 * e.k());
            }
        }
        if (!fire && firing) {
            stopFiring();
            if (phase == 2 && e.level() instanceof ServerLevel sl) {
                // plates slam back in: steam vents along the back
                for (int i = 1; i <= 16; i += 2) {
                    Vec3 p = e.nodePos(e.spec.node("tail" + i)).add(0, e.nodeR(e.spec.node("tail" + i)), 0);
                    sl.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 8, 1.5, 1, 1.5, 0.1);
                }
                Vec3 c = e.nodePos(e.spec.node("chest"));
                sl.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 10 * e.k(), c.z, 40, 4, 3, 4, 0.15);
                e.level().playSound(null, c.x, c.y, c.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 10f, 0.4f);
            }
        }
    }

    void stopFiring() {
        if (firing && length > 20 && e.level() instanceof ServerLevel sl) nuclearBlast(sl, end, power);
        firing = false;
        ACTIVE.remove(this);
        if (clashWith != null) { clashWith.clashWith = null; clashWith = null; }
        e.syncBeam(Vec3.ZERO, Vec3.ZERO, false, false);
    }

    void stop() {
        if (firing) stopFiring();
        if (atk != null) cooldown = e.tickCount + 20 * 6;
        atk = null;
    }

    void tick() {
        if (!firing) return;
        fireTicks++;
        if (e.controller == null && fireTicks > 6) {
            KaijuEntity t = e.brain.target;
            if (t == null || t.isRemoved() || t.state == KaijuEntity.State.DEAD) { if (e.attack != null) e.endAttack(); return; }
        }
        ServerLevel sl = (ServerLevel) e.level();
        float k = e.k();
        Vec3 mouth = e.nodePos(e.spec.node("mouth"));
        Vec3 aim = e.nodePos(e.spec.node("aim"));
        Vec3 headDir = aim.subtract(mouth).normalize();
        Vec3 want = e.brain.beamDirection(headDir, pitchExtra);
        // the neck limits the beam to a cone around where the head is pointing; turn smoothly
        Vec3 d = e.isDown() ? want : limitCone(headDir, want, 55);  // lying down the head lifts to aim; the neck cone does not apply
        dir = dir.lengthSqr() < 0.5 ? d : dir.add(d.subtract(dir).scale(0.35)).normalize();
        start = mouth;
        float maxLen = 360 * (k / 0.5f) * (0.6f + 0.4f * power);
        float radius = 2.2f * width * (k / 0.5f);
        // advance the front: fast through air, slow where it has to melt through
        float target = Math.min(maxLen, length + 45);
        float reach = 0;
        Vec3 p = start;
        KaijuEntity hitK = null;
        int hitNode = -1;
        int budget = 180;
        List<KaijuEntity> foes = KaijuCombat.others(e, maxLen);
        for (float s = 0; s <= target; s += 1.5f) {
            p = start.add(dir.scale(s));
            reach = s;
            if (clashWith != null && clashPoint() != null && s >= clashPoint().distanceTo(start)) break;
            // kaiju bodies
            for (KaijuEntity o : foes) {
                if (o == e || o.dodgeTicks > 0) continue;
                for (int i = 0; i < o.nodes.length; i++) {
                    float r = o.nodeR(i);
                    if (r <= 0 || o.nodes[i] == null || !o.partOk(o.spec.nodes.get(i).sever())) continue;
                    if (o.nodes[i].distanceToSqr(p) < (r + radius) * (r + radius)) { hitK = o; hitNode = i; break; }
                }
                if (hitK != null) break;
            }
            if (hitK != null) break;
            if (s > length) {
                BlockPos bp = BlockPos.containing(p);
                BlockState bs = sl.getBlockState(bp);
                if (!bs.isAir() && bs.getFluidState().isEmpty()) {
                    if (bs.getDestroySpeed(sl, bp) < 0) break;
                    budget -= WorldSmash.vaporize(sl, p, radius * 1.2f, Math.max(10, budget), e);
                    target = Math.min(target, s + 3); // has to dig through
                }
            }
        }
        length = Math.max(length, Math.min(reach, target));
        if (hitK != null) length = reach;
        end = start.add(dir.scale(length));
        // burn what is hit
        if (hitK != null) burn(hitK, hitNode, sl);
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(radius), x -> !(x instanceof KaijuEntity) && x.getVehicle() != e)) {
            Vec3 c = le.position();
            double t = KaijuCombat.segT(start, end, c);
            if (start.add(end.subtract(start).scale(t)).distanceTo(c) < radius + 1) {
                le.hurt(e.damageSources().indirectMagic(e, e), 40);
                le.setSecondsOnFire(10);
            }
        }
        // the blast where it lands
        if (++blastTimer % 5 == 0 && (hitK != null || length < maxLen - 2)) {
            sl.explode(e, end.x, end.y, end.z, 4.5f * power * Math.min(1.5f, k / 0.5f), true, Level.ExplosionInteraction.MOB);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, end.x, end.y, end.z, 1, 0, 0, 0, 0);
        }
        if (fireTicks % 2 == 0) {
            for (int i = 0; i < 6; i++) {
                Vec3 q = start.add(dir.scale(length * e.getRandom().nextFloat()));
                sl.sendParticles(ParticleTypes.END_ROD, q.x, q.y, q.z, 2, radius * 0.5, radius * 0.5, radius * 0.5, 0.2);
            }
            sl.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 1, 0, 0, 0, 0);
        }
        if (fireTicks % 20 == 0) e.level().playSound(null, start.x, start.y, start.z, SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 20f, 0.5f);
        // the beam pushes its owner back a little
        e.knock = e.knock.subtract(new Vec3(dir.x, 0, dir.z).scale(0.004 * power));
        clash(sl);
        e.syncBeam(start, end, true, clashWith != null);
    }

    void burn(KaijuEntity o, int node, ServerLevel sl) {
        float k = e.k();
        float dps = 5.5f * power * width * (k / 0.5f);
        if (o.guardTicks > 0) dps *= 0.45f;
        o.damage(dps, node, "beam");
        int region = Math.max(0, o.spec.nodes.get(node).region());
        o.heat[region] = Math.min(1f, o.heat[region] + 0.03f * power);
        // heat spreads to the neighbouring regions a little
        for (int r = 0; r < o.heat.length; r++) if (r != region) o.heat[r] = Math.min(1f, o.heat[r] + 0.004f * power);
        o.poise -= 3.5f * power;
        o.knock = o.knock.add(new Vec3(dir.x, 0, dir.z).scale(0.02 * power));
        if (o.poise <= 0 && o.state != KaijuEntity.State.DOWN && o.state != KaijuEntity.State.REACT) {
            if (o.attack != null) o.endAttack();
            o.state = KaijuEntity.State.REACT;
            o.play(o.heat[region] > 0.6f ? "burn_writhe" : "beam_recoil");
            o.poise = 30;
        }
        if (o.poise < -60 || o.heat[region] > 0.95f && o.getRandom().nextFloat() < 0.02f) o.knockdown(new Vec3(dir.x, 0, dir.z).scale(0.25), KaijuCombat.dirFrom(o, start));
        Vec3 at = o.nodePos(node);
        sl.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 12, 2, 2, 2, 0.1);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 6, 2, 2, 2, 0.05);
        if (fireTicks % 6 == 0) e.level().playSound(null, at.x, at.y, at.z, SoundEvents.ENDER_DRAGON_HURT, SoundSource.HOSTILE, 10f, 0.4f);
    }

    static Vec3 limitCone(Vec3 axis, Vec3 want, float deg) {
        double cos = axis.dot(want);
        double lim = Math.cos(Math.toRadians(deg));
        if (cos >= lim) return want.normalize();
        Vec3 perp = want.subtract(axis.scale(cos));
        if (perp.lengthSqr() < 1e-6) return axis;
        perp = perp.normalize();
        double s = Math.sin(Math.toRadians(deg));
        return axis.scale(lim).add(perp.scale(s)).normalize();
    }

    // ------------------------------------------------------------------ beam clash
    Vec3 clashPoint() {
        if (clashWith == null) return null;
        Vec3 a = start, b = clashWith.start;
        return a.add(b.subtract(a).scale(e.getId() < clashWith.e.getId() ? clashS : 1 - clashS));
    }

    void clash(ServerLevel sl) {
        float k = e.k();
        if (clashWith == null) {
            for (KaijuBeam o : ACTIVE) {
                if (o == this || o.e == e || !o.firing || o.e.level() != e.level()) continue;
                double[] st = closest(start, end, o.start, o.end);
                Vec3 pa = start.add(end.subtract(start).scale(st[0]));
                Vec3 pb = o.start.add(o.end.subtract(o.start).scale(st[1]));
                if (pa.distanceTo(pb) < 5 * k / 0.5 * (width + o.width)) {
                    clashWith = o;
                    o.clashWith = this;
                    com.krolasyon.kaiju.KaijuMod.LOG.info("[KAIJU] beam clash #{} vs #{}", e.getId(), o.e.getId());
                    // where they met, as a fraction of the mouth-to-mouth line
                    double dm = start.distanceTo(o.start);
                    float s = (float) Mth.clamp(start.distanceTo(pa) / Math.max(1, dm), 0.1, 0.9);
                    clashS = e.getId() < o.e.getId() ? s : 1 - s;
                    o.clashS = clashS;
                    sl.playSound(null, pa.x, pa.y, pa.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 30f, 0.3f);
                    break;
                }
            }
            return;
        }
        if (!clashWith.firing) { clashWith = null; return; }
        if (e.getId() > clashWith.e.getId()) return; // the lower id drives the struggle for both
        float pa = strength(), pb = clashWith.strength();
        float drift = (pa - pb) * 0.012f + (e.getRandom().nextFloat() - 0.5f) * 0.01f;
        clashS = Mth.clamp(clashS + drift, 0.02f, 0.98f);
        clashWith.clashS = clashS;
        Vec3 cp = clashPoint();
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, cp.x, cp.y, cp.z, 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.END_ROD, cp.x, cp.y, cp.z, 40, 6 * k, 6 * k, 6 * k, 0.6);
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, cp.x, cp.y, cp.z, 40, 8 * k, 8 * k, 8 * k, 0.8);
        if (fireTicks % 3 == 0) WorldSmash.vaporize(sl, cp, 8 * k, 120, e);
        if (fireTicks % 8 == 0) sl.explode(e, cp.x, cp.y, cp.z, 5f, true, Level.ExplosionInteraction.MOB);
        Net.shake(sl, cp, 2.2f, 500 * k);
        // overpowered: the stronger beam punches through into the other's face
        if (clashS > 0.9f || clashS < 0.1f) {
            KaijuBeam loser = clashS > 0.9f ? clashWith : this;
            KaijuBeam winner = loser == this ? clashWith : this;
            KaijuEntity l = loser.e;
            com.krolasyon.kaiju.KaijuMod.LOG.info("[KAIJU] clash won by #{} over #{}", winner.e.getId(), l.getId());
            l.damage(260 * winner.power, l.spec.node("head"), "beam");
            l.heat[0] = Math.min(1, l.heat[0] + 0.5f);
            l.heat[2] = Math.min(1, l.heat[2] + 0.4f);
            if (l.attack != null) l.endAttack();
            l.knockdown(new Vec3(winner.dir.x, 0, winner.dir.z).scale(0.6), KaijuCombat.dirFrom(l, winner.start));
            nuclearBlast(sl, l.nodePos(l.spec.node("chest")), 1.6f);
            winner.clashWith = null;
            loser.clashWith = null;
        }
    }

    float strength() {
        float hpf = 0.6f + 0.4f * e.hp / e.spec.maxHp;
        float mash = e.controller != null && e.input.down(com.krolasyon.kaiju.net.ControlInput.BREATH) ? 1.15f : 1f;
        return power * width * hpf * mash * (0.9f + e.getRandom().nextFloat() * 0.2f);
    }

    static double[] closest(Vec3 p1, Vec3 q1, Vec3 p2, Vec3 q2) {
        Vec3 d1 = q1.subtract(p1), d2 = q2.subtract(p2), r = p1.subtract(p2);
        double a = d1.dot(d1), e = d2.dot(d2), f = d2.dot(r);
        double s, t;
        if (a < 1e-9 && e < 1e-9) return new double[]{0, 0};
        if (a < 1e-9) { s = 0; t = Mth.clamp(f / e, 0, 1); }
        else {
            double c = d1.dot(r);
            if (e < 1e-9) { t = 0; s = Mth.clamp(-c / a, 0, 1); }
            else {
                double b = d1.dot(d2), den = a * e - b * b;
                s = den != 0 ? Mth.clamp((b * f - c * e) / den, 0, 1) : 0;
                t = (b * s + f) / e;
                if (t < 0) { t = 0; s = Mth.clamp(-c / a, 0, 1); }
                else if (t > 1) { t = 1; s = Mth.clamp((b - c) / a, 0, 1); }
            }
        }
        return new double[]{s, t};
    }

    // ------------------------------------------------------------------ blasts
    static void nuclearBlast(ServerLevel sl, Vec3 at, float power) {
        float r = 10 * power;
        sl.explode(null, at.x, at.y, at.z, Math.min(14f, 7f * power), true, Level.ExplosionInteraction.MOB);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 6, r * 0.5, r * 0.3, r * 0.5, 0);
        // the mushroom: a rising stem and a spreading cap
        for (int i = 0; i < 14; i++) sl.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, at.x, at.y + i * 3, at.z, 10, 2 + i * 0.2, 1, 2 + i * 0.2, 0.02);
        sl.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, at.x, at.y + 44, at.z, 160, r * 1.4, 4, r * 1.4, 0.03);
        sl.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 200, r, 2, r, 0.3);
        sl.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 40f, 0.2f);
        Net.flash(sl, at, 1.0f, 700);
        Net.shake(sl, at, 3.5f, 700);
    }

    static void nuclearPulse(KaijuEntity a, float radius, float power) {
        if (!(a.level() instanceof ServerLevel sl)) return;
        Vec3 c = a.nodePos(a.spec.node("chest"));
        if (a.heldBy != null) { KaijuEntity h = a.heldBy; KaijuCombat.releaseVictim(h, Vec3.ZERO); }
        if (a.holding != null) KaijuCombat.releaseVictim(a, Vec3.ZERO);
        WorldSmash.vaporize(sl, c, radius * 0.25f, 3000, a);
        WorldSmash.shockwave(sl, new Vec3(c.x, a.groundAt(c), c.z), radius, power, a);
        for (KaijuEntity o : KaijuCombat.others(a, radius)) {
            double d = o.position().distanceTo(a.position());
            if (d > radius) continue;
            float f = (float) (1 - d / radius);
            o.damage(380 * power * f, o.spec.node("chest"), "beam");
            for (int r = 0; r < o.heat.length; r++) o.heat[r] = Math.min(1, o.heat[r] + 0.35f * f);
            Vec3 out = o.position().subtract(a.position()).multiply(1, 0, 1).normalize();
            o.knockdown(out.scale(1.1 * f * power), KaijuCombat.dirFrom(o, a.position()));
            o.vy += 0.4 * f;
        }
        sl.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 3, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 30, radius * 0.4, radius * 0.2, radius * 0.4, 0);
        sl.sendParticles(new DustParticleOptions(new Vector3f(0.5f, 0.9f, 1f), 4f), c.x, c.y, c.z, 600, radius * 0.5, radius * 0.3, radius * 0.5, 0.5);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 40f, 0.3f);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 40f, 0.2f);
        Net.flash(sl, c, 1.0f, 900);
        Net.shake(sl, c, 4f, 900);
        a.beam.cooldown = a.tickCount + 20 * 20;
    }
}
