package com.krolasyon.bloodsea.game;

import com.krolasyon.bloodsea.entity.ActorEntity;
import com.krolasyon.bloodsea.entity.RaftEntity;
import com.krolasyon.bloodsea.net.Msg;
import com.krolasyon.bloodsea.net.Net;
import com.krolasyon.bloodsea.registry.ModEntities;
import com.krolasyon.bloodsea.registry.ModSounds;
import com.krolasyon.bloodsea.story.Story;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;

/**
 * Between the chapters the sea is not empty. Every minute or two something happens that the story never explains:
 * a ship's bell far off, blood falling as rain, a moan from below, a whisper with a woman's voice, lanterns on the
 * horizon that go out when looked at, the sun forgetting to be a sun for a moment - and a raft like yours, with a
 * man like you standing on it, watching.
 */
public final class Omens {
    private Omens() {}

    static final class State {
        int t, next = 60;
        final Deque<String> recent = new ArrayDeque<>();
        RaftEntity ghost;
        ActorEntity double_;
        int ghostT;
        List<Vec3> lights = new ArrayList<>();
        int lightsT;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    /** stages in which the sea may do this: not the waking, not the last stretch, not after the end */
    private static final Set<String> CALM = Set.of("rowing", "hungry", "day1", "dusk", "night1", "night1b", "north", "north2", "escape", "east0", "east", "west", "fog");
    private static final Set<String> AFTER_EYE = Set.of("north", "north2", "escape", "east0", "east", "west", "fog");

    /** every second, for a player in the story */
    public static void tick(ServerPlayer p, ServerLevel lvl, RaftEntity raft) {
        State s = STATES.computeIfAbsent(p.getUUID(), k -> new State());
        tickGhost(p, lvl, s);
        tickLights(p, lvl, s);
        if (raft == null || Story.inCut(p) || !CALM.contains(Story.stage(p))) return;
        if (++s.t < s.next) return;
        s.t = 0;
        s.next = 70 + lvl.random.nextInt(90);
        boolean day = com.krolasyon.bloodsea.world.SeaTime.isDay(lvl);
        List<String> pool = new ArrayList<>(List.of("bell", "moan", "whisper", "rain", "ghost"));
        if (day && AFTER_EYE.contains(Story.stage(p))) pool.add("blink");
        if (!day) { pool.add("lights"); pool.add("lights"); }
        pool.removeAll(s.recent);
        if (s.ghost != null) pool.remove("ghost");
        if (pool.isEmpty()) return;
        String o = pool.get(lvl.random.nextInt(pool.size()));
        s.recent.addLast(o);
        while (s.recent.size() > 3) s.recent.removeFirst();
        com.krolasyon.bloodsea.BloodSea.LOG.info("[OMEN] {} for {}", o, p.getName().getString());
        run(o, p, lvl, raft, s);
    }

    /** development: make one happen now */
    public static void force(String o, ServerPlayer p, ServerLevel lvl, RaftEntity raft) {
        run(o, p, lvl, raft, STATES.computeIfAbsent(p.getUUID(), k -> new State()));
    }

    static Vec3 around(ServerPlayer p, double dist, double ang) {
        return new Vec3(p.getX() + Math.cos(ang) * dist, RaftEntity.Y + 0.5, p.getZ() + Math.sin(ang) * dist);
    }

    static void sound(ServerLevel lvl, Vec3 at, String name, float vol, float pitch) {
        lvl.playSound(null, at.x, at.y, at.z, ModSounds.get(name), SoundSource.AMBIENT, vol, pitch);
    }

    static void run(String o, ServerPlayer p, ServerLevel lvl, RaftEntity raft, State s) {
        double ang = lvl.random.nextDouble() * Math.PI * 2;
        switch (o) {
            case "bell" -> {
                Vec3 at = around(p, 70, ang);
                sound(lvl, at, "omen_bell", 7f, 0.92f + lvl.random.nextFloat() * 0.1f);
                Story.later(50, () -> Story.say(p, "omen.bell"));
                Story.later(280, () -> Story.say(p, "omen.bell2"));
            }
            case "moan" -> {
                sound(lvl, new Vec3(p.getX(), RaftEntity.Y - 10, p.getZ()), "omen_moan", 5f, 0.85f + lvl.random.nextFloat() * 0.2f);
                Net.toPlayer(p, Msg.of("shake").i("ticks", 70));
                for (int i = 0; i < 8; i++) {
                    int k = i;
                    Story.later(10 + i * 12, () -> {
                        // blood boils up around the raft
                        for (int j = 0; j < 6; j++) {
                            double a = lvl.random.nextDouble() * Math.PI * 2, r = 3.5 + lvl.random.nextDouble() * 5;
                            lvl.sendParticles(new DustParticleOptions(new Vector3f(0.35f, 0.01f, 0.02f), 2.4f),
                                    raft.getX() + Math.cos(a) * r, RaftEntity.Y + 0.1, raft.getZ() + Math.sin(a) * r, 6, 0.3, 0.05, 0.3, 0.02);
                        }
                        if (k % 3 == 0) lvl.playSound(null, raft.getX(), RaftEntity.Y, raft.getZ(), ModSounds.get("blood_splash"), SoundSource.AMBIENT, 0.5f, 0.5f);
                    });
                }
                Story.later(110, () -> Story.say(p, "omen.moan"));
                Net.toPlayer(p, Msg.of("omen").s("kind", "dread").i("ticks", 400));
            }
            case "whisper" -> {
                Vec3 look = p.getLookAngle();
                Vec3 at = new Vec3(p.getX() - look.x * 2.5, p.getEyeY(), p.getZ() - look.z * 2.5);
                sound(lvl, at, "caller_whisper", 1.4f, 0.8f);
                Story.later(10, () -> Story.say(p, "omen.whisper"));
                Story.later(110, () -> Story.say(p, "omen.whisper2"));
            }
            case "rain" -> {
                Net.toPlayer(p, Msg.of("omen").s("kind", "rain").i("ticks", 1100));
                Story.later(80, () -> Story.say(p, "omen.rain"));
            }
            case "blink" -> {
                Net.toPlayer(p, Msg.of("eye").i("ticks", 60));
                Story.later(150, () -> Story.say(p, "omen.blink"));
            }
            case "lights" -> {
                s.lights.clear();
                for (int i = 0; i < 6; i++) {
                    double a = ang + (i - 2.5) * 0.18 + lvl.random.nextGaussian() * 0.05;
                    s.lights.add(around(p, 55 + lvl.random.nextDouble() * 20, a).add(0, 0.6, 0));
                }
                s.lightsT = 70;
                Story.later(40, () -> Story.say(p, "omen.lights"));
            }
            case "ghost" -> {
                Vec3 at = around(p, 22, ang);
                RaftEntity g = ModEntities.RAFT.get().create(lvl);
                if (g == null) return;
                g.addTag(RaftEntity.GHOST);
                float face = (float) (Math.atan2(p.getZ() - at.z, p.getX() - at.x) * 180 / Math.PI) - 90f;
                g.moveTo(at.x, RaftEntity.Y, at.z, Math.round(face / 90f) * 90f, 0);
                lvl.addFreshEntity(g);
                ActorEntity d = ModEntities.ACTOR.get().create(lvl);
                if (d == null) { g.discard(); return; }
                d.addTag(RaftEntity.GHOST);
                Vec3 deck = g.local(0, RaftEntity.TOP + 0.02, 0.4);
                d.moveTo(deck.x, deck.y, deck.z, face, 0);
                d.setAnim("stand");
                lvl.addFreshEntity(d);
                s.ghost = g;
                s.double_ = d;
                s.ghostT = 0;
                Story.later(40, () -> Story.say(p, "omen.ghost"));
                Story.later(200, () -> { if (s.double_ != null) Story.say(p, "omen.ghost2"); });
            }
            default -> {}
        }
    }

    static void tickGhost(ServerPlayer p, ServerLevel lvl, State s) {
        if (s.ghost == null) return;
        s.ghostT++;
        boolean gone = s.ghost.isRemoved() || s.double_ == null || s.double_.isRemoved() || s.ghost.level() != p.level();
        double dist = gone ? 0 : s.ghost.distanceTo(p);
        if (!gone && s.double_ != null) {
            // he watches you, always
            float face = (float) (Math.atan2(p.getZ() - s.double_.getZ(), p.getX() - s.double_.getX()) * 180 / Math.PI) - 90f;
            s.double_.setYRot(face);
            s.double_.setYHeadRot(face);
            s.double_.setYBodyRot(face);
        }
        if (gone || dist < 10 || s.ghostT > 50 || Story.inCut(p)) {
            if (!gone) {
                lvl.sendParticles(ParticleTypes.LARGE_SMOKE, s.ghost.getX(), s.ghost.getY() + 1, s.ghost.getZ(), 60, 1.6, 0.8, 1.6, 0.01);
                sound(lvl, s.ghost.position(), "omen_bell", 3f, 0.6f);
                if (dist < 10) Story.later(30, () -> Story.say(p, "omen.ghost3"));
            }
            if (s.ghost != null) s.ghost.discard();
            if (s.double_ != null) s.double_.discard();
            s.ghost = null;
            s.double_ = null;
        }
    }

    static void tickLights(ServerPlayer p, ServerLevel lvl, State s) {
        if (s.lights.isEmpty()) return;
        if (--s.lightsT <= 0 || Story.inCut(p)) { s.lights.clear(); return; }
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        for (Iterator<Vec3> it = s.lights.iterator(); it.hasNext(); ) {
            Vec3 l = it.next();
            // look straight at one and it goes out
            if (l.subtract(eye).normalize().dot(look) > 0.995) { it.remove(); continue; }
            for (int k = 0; k < 4; k++)
                Story.later(k * 5, () -> lvl.sendParticles(p, new DustParticleOptions(new Vector3f(1f, 0.7f, 0.3f), 4f), true, l.x, l.y, l.z, 8, 0.12, 0.12, 0.12, 0));
        }
    }

    public static void forget(ServerPlayer p) {
        State s = STATES.remove(p.getUUID());
        if (s != null) {
            if (s.ghost != null) s.ghost.discard();
            if (s.double_ != null) s.double_.discard();
        }
    }
}
