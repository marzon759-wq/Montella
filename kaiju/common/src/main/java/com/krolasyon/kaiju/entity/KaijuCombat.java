package com.krolasyon.kaiju.entity;

import com.google.gson.JsonObject;
import com.krolasyon.kaiju.KaijuMod;
import com.krolasyon.kaiju.data.KaijuSpec;
import com.krolasyon.kaiju.net.Net;
import com.krolasyon.kaiju.world.WorldSmash;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Contact physics between kaiju. Hits are swept spheres (the weapon node's path this tick against the victim's node
 * spheres), so even a tail tip moving ten blocks a tick cannot tunnel through a body. A hit transfers momentum:
 *   reduced mass mu = m_part*M / (m_part + M), closing speed v along the contact normal,
 *   impulse J = (1 + e) * mu * v,   victim dv = eta * J / M_victim   (eta: energy lost into flesh and bone)
 * dv decides the reaction (flinch, stagger, knockdown) together with the victim's poise; damage grows with the
 * momentum delivered and the kind of weapon. Both bodies freeze for a few frames (hit-stop) so impacts land.
 */
public final class KaijuCombat {
    private KaijuCombat() {}

    static final float RESTITUTION = 0.25f, ETA = 0.10f, DMG = 46f;

    static float partFraction(KaijuEntity e, int node) {
        String n = e.spec.nodes.get(node).name();
        if (n.startsWith("tail")) return 0.16f;
        if (n.startsWith("hand") || n.startsWith("elbow")) return 0.05f;
        if (n.startsWith("foot") || n.startsWith("shin") || n.startsWith("knee") || n.startsWith("thigh")) return 0.1f;
        if (n.equals("head") || n.equals("snout") || n.equals("jaw")) return 0.07f;
        if (n.equals("held")) return 0.05f;
        return 0.5f;
    }

    static float kindMul(String kind) {
        return switch (kind) { case "cut" -> 2.4f; case "bite" -> 2.6f; case "stomp" -> 1.6f; case "crush" -> 1.5f; default -> 1.0f; };
    }

    static List<KaijuEntity> others(KaijuEntity self, double range) {
        return self.level().getEntitiesOfClass(KaijuEntity.class, self.getBoundingBox().inflate(range), o -> o != self && !o.isRemoved() && o.state != KaijuEntity.State.DEAD);
    }

    /** closest point parameter on segment a->b to point p */
    static double segT(Vec3 a, Vec3 b, Vec3 p) {
        Vec3 ab = b.subtract(a);
        double l = ab.lengthSqr();
        return l < 1e-9 ? 0 : Mth.clamp(p.subtract(a).dot(ab) / l, 0, 1);
    }

    public static void sweepHits(KaijuEntity a, KaijuSpec.Attack atk, KaijuSpec.Hit h, int hitIndex) {
        float k = a.k();
        for (KaijuEntity v : others(a, 200 * k)) {
            if (v == a.holding && !atk.tags.contains("grapple")) continue;
            long key = ((long) hitIndex << 32) | v.getId();
            if (a.hitOnce.contains(key)) continue;
            double best = Double.MAX_VALUE;
            int bw = -1, bv = -1;
            Vec3 bestPoint = null;
            for (int wi : h.nodes()) {
                if (!a.partOk(a.spec.nodes.get(wi).sever())) continue;
                Vec3 p0 = a.prevNodes[wi], p1 = a.nodes[wi];
                if (p0 == null || p1 == null) continue;
                float rw = a.nodeR(wi) + 0.5f;
                for (int vi = 0; vi < v.nodes.length; vi++) {
                    if (v.nodes[vi] == null || v.spec.nodes.get(vi).r() <= 0) continue;
                    if (!v.partOk(v.spec.nodes.get(vi).sever())) continue;
                    Vec3 c = v.nodes[vi];
                    double t = segT(p0, p1, c);
                    Vec3 q = p0.add(p1.subtract(p0).scale(t));
                    double d = q.distanceTo(c) - rw - v.nodeR(vi);
                    if (d < 0 && d < best) { best = d; bw = wi; bv = vi; bestPoint = q; }
                }
            }
            if (bw >= 0) {
                a.hitOnce.add(key);
                resolve(a, v, bw, bv, bestPoint, h.kind(), h.power(), atk);
            }
        }
        // small things caught by the strike
        for (int wi : h.nodes()) {
            Vec3 p1 = a.nodes[wi];
            if (p1 == null) continue;
            float r = a.nodeR(wi) + 1;
            for (LivingEntity le : a.level().getEntitiesOfClass(LivingEntity.class, new AABB(p1, p1).inflate(r), x -> !(x instanceof KaijuEntity) && x.getVehicle() != a)) {
                Vec3 vel = p1.subtract(a.prevNodes[wi] == null ? p1 : a.prevNodes[wi]);
                le.hurt(a.damageSources().mobAttack(a), 30 + 40 * h.power());
                le.setDeltaMovement(le.getDeltaMovement().add(vel.scale(0.8)).add(0, 0.6, 0));
                le.hurtMarked = true;
            }
        }
    }

    static void resolve(KaijuEntity a, KaijuEntity v, int wi, int vi, Vec3 point, String kind, float power, KaijuSpec.Attack atk) {
        if (v.dodgeTicks > 0) return;
        if (v.parryTicks > 0 && !kind.equals("stomp")) {
            // caught: the attacker is thrown off balance and the defender's counter (already queued in its clip) lands
            a.hitstop = 6;
            v.hitstop = 3;
            a.poise -= 60;
            a.react(dirFrom(a, v.position()), 0.1f);
            sound(a, point, SoundEvents.ANVIL_LAND, 8f, 0.5f);
            return;
        }
        Vec3 wv = a.nodes[wi].subtract(a.prevNodes[wi]);
        Vec3 vv = v.nodes[vi].subtract(v.prevNodes[vi] == null ? v.nodes[vi] : v.prevNodes[vi]);
        Vec3 rel = wv.subtract(vv);
        Vec3 n = v.nodes[vi].subtract(point);
        if (n.lengthSqr() < 1e-6) n = rel;
        n = n.normalize();
        double closing = Math.max(rel.dot(n), rel.length() * 0.6);
        closing = Mth.clamp(closing, 0.15 * a.k() / 0.5, 9 * a.k() / 0.5); // even a slow shove pushes; nothing exceeds a tail tip
        float mPart = a.mass() * partFraction(a, wi) * power;
        float mV = v.mass();
        double mu = mPart * mV / (mPart + mV);
        double J = (1 + RESTITUTION) * mu * closing;
        double dv = Math.min(0.6, ETA * J / mV);
        Vec3 dir = new Vec3(n.x, 0, n.z);
        if (dir.lengthSqr() < 1e-6) dir = v.position().subtract(a.position()).multiply(1, 0, 1);
        dir = dir.normalize();
        if (v.guardTicks > 0) dv *= 0.4;
        double up = atk.tags.contains("launch") ? dv * 0.6 : 0;
        v.knock = v.knock.add(dir.scale(dv));
        if (up > 0) v.vy += up;
        // the attacker feels it too (Newton's third law), much less because it is braced
        a.knock = a.knock.subtract(dir.scale(dv * mV / (a.mass() * 6)));
        float dmg = (float) (DMG * Math.sqrt(mu / mV) * closing * kindMul(kind) * (a.k() / 0.5f));
        v.damage(dmg, vi, kind);
        v.poise -= (float) (dv * 240);
        v.heat[Math.max(0, v.spec.nodes.get(vi).region())] += kind.equals("crush") ? 0.02f : 0f;
        int stop = Mth.clamp((int) (dv * 22) + 2, 2, 9);
        a.hitstop = Math.max(a.hitstop, stop);
        v.hitstop = Math.max(v.hitstop, stop);
        String vd = dirFrom(v, a.position());
        boolean trip = atk.tags.contains("trip") && v.spec.nodes.get(vi).name().matches("foot.*|shin.*|knee.*");
        if ((trip || v.poise <= 0 || dv > 0.3) && v.kdImmune <= 0) v.knockdown(dir.scale(dv * 0.5), vd);
        else v.react(vd, (float) dv);
        a.lastLanded = a.tickCount;
        KaijuMod.LOG.info("[KAIJU] hit #{} {} -> #{} {} kind={} closing={} dv={} dmg={} poise={} -> {}", a.getId(), atk.id, v.getId(), v.spec.nodes.get(vi).name(), kind,
                String.format("%.2f", closing), String.format("%.3f", dv), (int) dmg, (int) v.poise, v.clip.name);
        a.stats.computeIfAbsent(atk.id, x -> new float[2])[1] += 1;
        // effects scale with momentum
        ServerLevel sl = (ServerLevel) a.level();
        float big = (float) Mth.clamp(dv * 4, 0.2, 2.5);
        sl.sendParticles(ParticleTypes.EXPLOSION, point.x, point.y, point.z, (int) (3 + big * 4), 2 * big, 2 * big, 2 * big, 0);
        if (kind.equals("cut") || kind.equals("bite"))
            sl.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.35f, 0.0f, 0.02f), 3f), point.x, point.y, point.z, (int) (30 * big), 2, 2, 2, 0.3);
        sound(a, point, kind.equals("bite") ? SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR : SoundEvents.GENERIC_EXPLODE, 6 + big * 4, 0.4f + 0.1f / big);
        sound(a, point, SoundEvents.IRON_GOLEM_DAMAGE, 8f, 0.4f);
        Net.shake(sl, point, big * 1.5f, 260 * a.k());
        if (v.state == KaijuEntity.State.DOWN || v.isDown()) WorldSmash.crater((ServerLevel) a.level(), v.nodePos(v.spec.node("hips")), 6 * v.k() * big, 2, a);
    }

    public static String dirFrom(KaijuEntity v, Vec3 from) {
        Vec3 d = from.subtract(v.position());
        double f = d.dot(v.forward()), l = d.dot(v.left());
        if (Math.abs(f) >= Math.abs(l)) return f >= 0 ? "f" : "b";
        return l >= 0 ? "l" : "r";
    }

    static void sound(KaijuEntity a, Vec3 p, net.minecraft.sounds.SoundEvent s, float vol, float pitch) {
        a.level().playSound(null, p.x, p.y, p.z, s, SoundSource.HOSTILE, vol, pitch);
    }

    // ------------------------------------------------------------------ continuous body contact (no interpenetration)
    public static void bodyContacts(KaijuEntity a) {
        for (KaijuEntity b : others(a, 160 * a.k())) {
            if (b.getId() < a.getId()) continue; // each pair once
            if (a.holding == b || b.holding == a) continue;
            Vec3 push = Vec3.ZERO;
            int contacts = 0;
            for (int i = 0; i < a.nodes.length; i++) {
                float ra = a.nodeR(i);
                if (ra <= 0 || a.nodes[i] == null || !a.partOk(a.spec.nodes.get(i).sever())) continue;
                for (int j = 0; j < b.nodes.length; j++) {
                    float rb = b.nodeR(j);
                    if (rb <= 0 || b.nodes[j] == null || !b.partOk(b.spec.nodes.get(j).sever())) continue;
                    Vec3 d = b.nodes[j].subtract(a.nodes[i]);
                    double dist = d.length();
                    double pen = ra + rb - dist;
                    if (pen > 0) {
                        Vec3 nrm = dist > 1e-4 ? d.scale(1 / dist) : new Vec3(1, 0, 0);
                        push = push.add(new Vec3(nrm.x, 0, nrm.z).scale(pen));
                        contacts++;
                    }
                }
            }
            if (contacts == 0) continue;
            push = push.scale(1.0 / Math.max(1, Math.sqrt(contacts)));
            float ma = a.mass(), mb = b.mass();
            double wa = mb / (ma + mb), wb = ma / (ma + mb);
            Vec3 pa = push.scale(-wa * 0.6), pb = push.scale(wb * 0.6);
            if (a.state != KaijuEntity.State.HELD) a.setPos(a.getX() + pa.x, a.getY(), a.getZ() + pa.z);
            if (b.state != KaijuEntity.State.HELD) b.setPos(b.getX() + pb.x, b.getY(), b.getZ() + pb.z);
            // kill the closing velocity along the push direction (bodies cannot move through each other)
            Vec3 n = push.normalize();
            double va = a.knock.dot(n), vb = b.knock.dot(n);
            if (va - vb > 0) {
                double vm = (ma * va + mb * vb) / (ma + mb);
                a.knock = a.knock.subtract(n.scale(va - vm));
                b.knock = b.knock.subtract(n.scale(vb - vm));
            }
        }
    }

    // ------------------------------------------------------------------ clip impacts: footfalls, falls
    public static void impact(KaijuEntity e, String clip, float t) {
        if (!(e.level() instanceof ServerLevel sl)) return;
        float k = e.k();
        if (clip.startsWith("walk") || clip.startsWith("hold_walk") || clip.equals("turn")) {
            int foot = e.spec.node(t < e.clip.len * 0.5f ? "foot_r" : "foot_l");
            Vec3 p = e.nodePos(foot);
            WorldSmash.footprint(sl, p, e.nodeR(foot) * 0.9f, e);
            sound(e, p, SoundEvents.GENERIC_EXPLODE, 3f + k * 2, 0.35f);
            Net.shake(sl, p, 0.35f, 120 * k);
            crushSmall(e, p, e.nodeR(foot) * 1.3f, 20);
            return;
        }
        if (clip.startsWith("knockdown") || clip.startsWith("death") || clip.startsWith("getup")) {
            Vec3 p = e.nodePos(e.spec.node("chest"));
            Vec3 hips = e.nodePos(e.spec.node("hips"));
            WorldSmash.crater(sl, p, 14 * k, 4, e);
            WorldSmash.crater(sl, hips, 14 * k, 4, e);
            for (int i = e.spec.node("tail3"); i >= 0 && i <= e.spec.node("tail12"); i += 3) WorldSmash.crater(sl, e.nodePos(i), 8 * k, 2, e);
            sound(e, p, SoundEvents.GENERIC_EXPLODE, 16f, 0.3f);
            sound(e, p, SoundEvents.ANVIL_LAND, 10f, 0.3f);
            Net.shake(sl, p, 2.2f, 400 * k);
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.x, p.y, p.z, 80, 14 * k * 2, 2, 14 * k * 2, 0.05);
            crushSmall(e, p, 26 * k, 60);
            for (KaijuEntity o : others(e, 60 * k)) if (o.nodePos(o.spec.node("hips")).distanceTo(p) < 30 * k) o.poise -= 25;
        }
    }

    public static void landing(KaijuEntity e, double speed) {
        if (!(e.level() instanceof ServerLevel sl)) return;
        float k = e.k();
        Vec3 p = e.position();
        float big = (float) Mth.clamp(speed, 0.3, 3);
        WorldSmash.crater(sl, p.add(0, 2, 0), 16 * k * big, (int) (3 * big), e);
        sound(e, p, SoundEvents.GENERIC_EXPLODE, 18f, 0.25f);
        Net.shake(sl, p, 2.5f * big, 500 * k);
        crushSmall(e, p, 30 * k, 80);
        if (e.state == KaijuEntity.State.THROWN) {
            e.damage((float) (220 * speed), e.spec.node("hips"), "crush");
            e.state = KaijuEntity.State.IDLE;
            e.knockdown(Vec3.ZERO, "f");
        }
    }

    static void crushSmall(KaijuEntity e, Vec3 p, float r, float dmg) {
        for (Entity x : e.level().getEntities(e, new AABB(p, p).inflate(r, r * 0.6, r))) {
            if (x instanceof KaijuEntity || x.getVehicle() == e || x instanceof ItemEntity) continue;
            if (x instanceof LivingEntity le) {
                le.hurt(e.damageSources().mobAttack(e), dmg);
                Vec3 out = le.position().subtract(p).multiply(1, 0, 1).normalize().scale(1.2).add(0, 0.7, 0);
                le.setDeltaMovement(out);
                le.hurtMarked = true;
            }
        }
    }

    // ------------------------------------------------------------------ attack events
    public static void event(KaijuEntity a, KaijuSpec.Attack atk, KaijuSpec.Event ev) {
        if (!(a.level() instanceof ServerLevel sl)) return;
        JsonObject g = ev.args();
        float k = a.k();
        switch (ev.type()) {
            case "grab" -> grab(a, g.get("node").getAsString(), g.get("target").getAsString());
            case "release" -> {
                if (g.has("aim") && a.heldDebris != null) {
                    a.heldDebris.throwAt(a, a.brain.aimPoint(), g.get("speed").getAsFloat() * k / 20f);
                } else if (a.holding != null) {
                    float kk = k / 20f;
                    Vec3 v = a.forward().scale(g.get("fwd").getAsFloat() * kk).add(a.left().scale(g.get("side").getAsFloat() * kk)).add(0, g.get("up").getAsFloat() * kk, 0);
                    releaseVictim(a, v);
                }
            }
            case "slam" -> {
                KaijuEntity v = a.holding;
                if (v != null) {
                    releaseVictim(a, Vec3.ZERO);
                    float p = g.get("power").getAsFloat();
                    v.damage(160 * p * (k / 0.5f), v.spec.node("chest"), "crush");
                    v.knockdown(a.forward().scale(0.3), "f");
                    Vec3 at = v.nodePos(v.spec.node("hips"));
                    WorldSmash.crater(sl, at, 18 * k, 5, a);
                    sound(a, at, SoundEvents.GENERIC_EXPLODE, 18, 0.3f);
                    Net.shake(sl, at, 3f, 500 * k);
                }
            }
            case "pulse_hit" -> {
                KaijuEntity v = a.holding;
                if (v != null) {
                    v.damage(90 * g.get("power").getAsFloat() * (k / 0.5f), v.heldNode, "bite");
                    v.hitstop = 4;
                    a.hitstop = 3;
                    sl.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.4f, 0f, 0f), 3f), v.nodePos(v.heldNode).x, v.nodePos(v.heldNode).y, v.nodePos(v.heldNode).z, 40, 2, 2, 2, 0.2);
                    sound(a, v.nodePos(v.heldNode), SoundEvents.PLAYER_ATTACK_CRIT, 8, 0.4f);
                }
            }
            case "sever_pull" -> {
                KaijuEntity v = a.holding;
                if (v != null && v.heldNode >= 0) {
                    int grp = v.spec.nodes.get(v.heldNode).sever();
                    if (grp >= 0 && (v.partHp[grp] < v.partMaxHp() * 0.6f || a.getRandom().nextFloat() < 0.35f)) v.sever(grp);
                    else v.damage(140 * (k / 0.5f), v.heldNode, "bite");
                }
            }
            case "pin" -> {
                KaijuEntity t = a.brain.target;
                if (t != null && t.state == KaijuEntity.State.DOWN && t.position().distanceTo(a.position()) < 80 * k) {
                    t.pinTicks = (int) (g.get("secs").getAsFloat() * 20);
                    t.downTicks = 0;
                }
            }
            case "trip" -> {
                KaijuEntity t = a.brain.target;
                if (t != null && t.position().distanceTo(a.nodePos(a.spec.node("hand_r"))) < 50 * k && !t.isDown()) t.knockdown(Vec3.ZERO, "b");
            }
            case "shockwave" -> {
                int n = a.spec.node(g.get("node").getAsString());
                Vec3 at = a.nodePos(n);
                at = new Vec3(at.x, a.groundAt(at), at.z);
                float r = g.get("radius").getAsFloat() * k, p = g.get("power").getAsFloat();
                WorldSmash.shockwave(sl, at, r, p, a);
                for (KaijuEntity o : others(a, r * 2)) {
                    double d = o.position().distanceTo(at);
                    if (d < r) {
                        float f = (float) (1 - d / r) * p;
                        o.poise -= 50 * f;
                        o.damage(60 * f * (k / 0.5f), o.spec.node("shin_l"), "blunt");
                        if (atk.tags.contains("trip") && f > 0.35f || o.poise <= 0) o.knockdown(o.position().subtract(at).normalize().scale(0.2 * f), dirFrom(o, at));
                        else o.react(dirFrom(o, at), 0.08f * f);
                    }
                }
            }
            case "uproot" -> {
                if (a.heldDebris == null) {
                    DebrisEntity d = WorldSmash.capture(sl, a, g.get("kind").getAsString());
                    if (d != null) a.heldDebris = d;
                }
            }
            case "shatter_held" -> {
                if (a.heldDebris != null) { a.heldDebris.shatter(a.heldDebris.position(), 1.0f); a.heldDebris = null; }
            }
            case "kick_ground" -> {
                DebrisEntity d = WorldSmash.capture(sl, a, "kick");
                if (d != null) d.throwAt(a, a.brain.aimPoint(), g.get("speed").getAsFloat() * k / 20f);
            }
            case "pulse" -> KaijuBeam.nuclearPulse(a, g.get("radius").getAsFloat() * k, g.get("power").getAsFloat());
            case "roar" -> {
                float r = g.get("radius").getAsFloat() * k;
                sound(a, a.nodePos(a.spec.node("head")), SoundEvents.ENDER_DRAGON_GROWL, 30f, 0.45f);
                sound(a, a.nodePos(a.spec.node("head")), SoundEvents.RAVAGER_ROAR, 30f, 0.35f);
                for (KaijuEntity o : others(a, r)) o.poise -= 30;
                for (LivingEntity le : a.level().getEntitiesOfClass(LivingEntity.class, a.getBoundingBox().inflate(r * 0.5), x -> !(x instanceof KaijuEntity) && x.getVehicle() != a)) {
                    le.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                    le.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 120, 1));
                }
                Net.shake(sl, a.position(), 1.2f, r);
            }
            case "guard" -> a.guardTicks = (int) (g.get("secs").getAsFloat() * 20);
            case "dodge" -> a.dodgeTicks = (int) (g.get("secs").getAsFloat() * 20);
            case "parry" -> a.parryTicks = (int) (g.get("secs").getAsFloat() * 20);
            default -> KaijuMod.LOG.debug("unhandled event {}", ev.type());
        }
    }

    // ------------------------------------------------------------------ grapples
    static void grab(KaijuEntity a, String nodeName, String target) {
        int gn = a.spec.node(nodeName);
        Vec3 grip = a.nodePos(gn);
        float k = a.k();
        KaijuEntity best = null;
        int bestNode = -1;
        double bd = Double.MAX_VALUE;
        for (KaijuEntity v : others(a, 120 * k)) {
            if (v.state == KaijuEntity.State.HELD || v.dodgeTicks > 0) continue;
            for (String tn : targetNodes(target)) {
                int vi = v.spec.node(tn);
                if (vi < 0 || !v.partOk(v.spec.nodes.get(vi).sever())) continue;
                double d = v.nodePos(vi).distanceTo(grip) - v.nodeR(vi) - a.nodeR(gn);
                if (d < 9 * k && d < bd) { bd = d; best = v; bestNode = vi; }
            }
        }
        if (best == null) return;
        if (best.attack != null) best.endAttack();
        if (best.heldDebris != null) best.heldDebris.drop();
        best.state = KaijuEntity.State.HELD;
        best.heldBy = a;
        best.heldNode = bestNode;
        a.holding = best;
        a.gripNode = gn;
        best.play("held");
        best.hitstop = 4;
        a.hitstop = 3;
        sound(a, grip, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 10, 0.4f);
    }

    static String[] targetNodes(String t) {
        return switch (t) {
            case "neck" -> new String[]{"neck", "head", "chest"};
            case "head" -> new String[]{"head", "neck", "snout"};
            case "legs" -> new String[]{"shin_l", "shin_r", "foot_l", "foot_r", "knee_l", "knee_r"};
            case "arm" -> new String[]{"elbow_l", "elbow_r", "hand_l", "hand_r"};
            default -> new String[]{"chest", "belly", "neck", "shoulder_l", "shoulder_r"};
        };
    }

    public static void carryVictim(KaijuEntity a) {
        KaijuEntity v = a.holding;
        if (v == null || v.isRemoved() || v.state != KaijuEntity.State.HELD) { a.holding = null; return; }
        Vec3 grip = a.nodePos(a.gripNode);
        v.computeNodes();
        Vec3 off = v.nodePos(v.heldNode).subtract(v.position());
        Vec3 np = grip.subtract(off);
        v.setPos(np.x, np.y, np.z);
        v.setYRot(Mth.wrapDegrees(a.getYRot() + 180));
        // the held one struggles
        if (v.tickCount % 20 == 0 && v.poise > 60 && a.getRandom().nextFloat() < 0.18f) {
            releaseVictim(a, a.forward().scale(0.3));
            a.react("f", 0.1f);
            sound(a, grip, SoundEvents.ANVIL_LAND, 6, 0.6f);
        }
    }

    public static void releaseVictim(KaijuEntity a, Vec3 vel) {
        KaijuEntity v = a.holding;
        a.holding = null;
        a.gripNode = -1;
        if (v == null || v.state != KaijuEntity.State.HELD) return;
        v.heldBy = null;
        if (vel.lengthSqr() > 0.04) {
            v.state = KaijuEntity.State.THROWN;
            v.knock = new Vec3(vel.x, 0, vel.z);
            v.vy = Math.max(0.3, vel.y);
            v.play("thrown");
        } else {
            v.state = KaijuEntity.State.IDLE;
            v.knockdown(Vec3.ZERO, "f");
        }
    }

    public static void particlesBlock(ServerLevel sl, Vec3 p, int n, float spread) {
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), p.x, p.y, p.z, n, spread, spread * 0.5, spread, 0.2);
    }
}
