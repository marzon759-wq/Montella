package com.krolasyon.bloodsea.story;

import com.krolasyon.bloodsea.BloodSea;
import com.krolasyon.bloodsea.compat.CreatureBase;
import com.krolasyon.bloodsea.entity.*;
import com.krolasyon.bloodsea.game.Endings;
import com.krolasyon.bloodsea.game.SeaEvents;
import com.krolasyon.bloodsea.net.Msg;
import com.krolasyon.bloodsea.net.Net;
import com.krolasyon.bloodsea.registry.ModEntities;
import com.krolasyon.bloodsea.registry.ModItems;
import com.krolasyon.bloodsea.registry.ModSounds;
import com.krolasyon.bloodsea.world.Regions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Consumer;

/**
 * The story of the man on the raft, run on the server: a chain of stages, each waiting for something (a place, a
 * time, an action) and then playing a cutscene, putting a choice, setting an objective. Choices weigh four ways -
 * hope, acceptance, courage, fury - and the heaviest decides where the voyage ends.
 */
public final class Story {
    private Story() {}

    // ================================================================== state, kept on the player
    public static CompoundTag st(ServerPlayer p) {
        CompoundTag d = p.getPersistentData();
        if (!d.contains("bloodsea_story")) d.put("bloodsea_story", new CompoundTag());
        return d.getCompound("bloodsea_story");
    }

    public static String stage(ServerPlayer p) { return st(p).getString("stage"); }

    static void stage(ServerPlayer p, String s) {
        CompoundTag t = st(p);
        t.putString("stage", s);
        t.putInt("t", 0);
        t.putDouble("x0", p.getX());
        t.putDouble("z0", p.getZ());
        BloodSea.LOG.info("[STORY] {} -> {}", p.getName().getString(), s);
    }

    static void score(ServerPlayer p, String k, int n) { st(p).putInt("s_" + k, st(p).getInt("s_" + k) + n); }

    public static boolean active(ServerPlayer p) { String s = stage(p); return !s.isEmpty() && !s.equals("done"); }

    public static boolean pipGone(ServerPlayer p) { return st(p).getBoolean("pip_gone"); }

    static void obj(ServerPlayer p, String key) {
        st(p).putString("obj", key);
        Net.toPlayer(p, Msg.of("obj").s("key", key));
    }

    public static void say(ServerPlayer p, String key) { Net.toPlayer(p, Msg.of("say").s("key", key)); }

    static void eye(ServerPlayer p, int ticks) {
        st(p).putLong("eye_until", p.level().getGameTime() + ticks);
        Net.toPlayer(p, Msg.of("eye").i("ticks", ticks));
    }

    public static boolean eyeOpen(ServerPlayer p) { return !active(p) || st(p).getLong("eye_until") > p.level().getGameTime(); }

    /** development: go straight to a stage */
    public static void jump(ServerPlayer p, String s) {
        if (!active(p)) {
            p.getPersistentData().put("bloodsea_story", new CompoundTag());
            p.server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, p.server);
            SeaEvents.enter(p);
            pip(p.serverLevel(), SeaEvents.raftFor(p.serverLevel(), p));
        }
        if (s.startsWith("dest_")) {
            st(p).putString("dest", s.substring(5));
            st(p).putBoolean("pip_gone", true);
            s = "dest";
        }
        stage(p, s);
        st(p).putInt("t", 100000);
    }

    // ================================================================== starting
    public static void start(ServerPlayer p) {
        ServerLevel sea = p.server.getLevel(SeaEvents.SEA);
        if (sea == null) return;
        CompoundTag t = new CompoundTag();
        p.getPersistentData().put("bloodsea_story", t);
        p.server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, p.server);
        p.server.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, p.server);
        sea.setDayTime(1000);
        SeaEvents.enter(p);
        RaftEntity r = SeaEvents.raftFor(sea, p);
        pip(sea, r);
        p.getFoodData().setFoodLevel(14);
        stage(p, "wake");
        play(p, "wake", () -> {
            if (!p.getInventory().contains(new ItemStack(ModItems.SEA_MAP.get()))) p.getInventory().add(new ItemStack(ModItems.SEA_MAP.get()));
            obj(p, "oar");
            stage(p, "oar");
        });
    }

    static PipEntity pip(ServerLevel lvl, RaftEntity r) {
        var l = lvl.getEntitiesOfClass(PipEntity.class, r.getBoundingBox().inflate(40));
        if (!l.isEmpty()) return l.get(0);
        PipEntity c = ModEntities.PIP.get().create(lvl);
        Vec3 b = r.local(-1.4, RaftEntity.TOP + 0.05, -1.45);
        c.moveTo(b.x, b.y, b.z, r.getYRot(), 0);
        lvl.addFreshEntity(c);
        return c;
    }

    static RaftEntity raft(ServerPlayer p) {
        var l = p.serverLevel().getEntitiesOfClass(RaftEntity.class, p.getBoundingBox().inflate(48));
        return l.isEmpty() ? null : l.get(0);
    }

    // ================================================================== every second, for each player in the story
    public static void tick(ServerPlayer p) {
        if (Boolean.getBoolean("bloodsea.debugai") && p.server.getTickCount() % 400 == 0)
            BloodSea.LOG.info("[STORY] guard active={} cut={} asking={} dim={}", active(p), SESSIONS.containsKey(p.getUUID()), ASKING.containsKey(p.getUUID()), p.level().dimension().location());
        if (!active(p) || SESSIONS.containsKey(p.getUUID()) || ASKING.containsKey(p.getUUID())) return;
        if (!p.level().dimension().equals(SeaEvents.SEA)) return;
        CompoundTag s = st(p);
        int t = s.getInt("t") + 20;
        s.putInt("t", t);
        ServerLevel lvl = p.serverLevel();
        RaftEntity r = raft(p);
        Regions.Region reg = Regions.at(p.getX(), p.getZ());
        double moved = Math.hypot(p.getX() - s.getDouble("x0"), p.getZ() - s.getDouble("z0"));
        String stage = stage(p);
        if (t % 200 == 0 && Boolean.getBoolean("bloodsea.debugai"))
            BloodSea.LOG.info("[STORY] tick {} t={} region={} dist={} raft={}", stage, t, reg, (int) Regions.dist(p.getX(), p.getZ()), r != null);
        switch (stage) {
            case "oar" -> {
                if (p.getVehicle() instanceof RaftEntity) {
                    say(p, "think.row");
                    obj(p, "explore");
                    stage(p, "rowing");
                }
            }
            case "rowing" -> {
                if (t > 300 && r != null) play(p, "rule", () -> {
                    p.getFoodData().setFoodLevel(5);
                    say(p, "think.hungry");
                    obj(p, "food");
                    stage(p, "hungry");
                });
            }
            case "hungry" -> Crates.keep(p, 3);
            case "day1" -> {
                Crates.keep(p, 2);
                if (t > 1400 && r != null) {
                    lvl.setDayTime(10500);
                    play(p, "gift", () -> ask(p, "gift", pick -> {
                        if (pick.equals("share")) { score(p, "courage", 1); bondUp(p, 3); } else score(p, "fury", 1);
                        say(p, "gift." + pick);
                        stage(p, "dusk");
                    }));
                }
            }
            case "dusk" -> {
                Crates.keep(p, 2);
                if (t > 400) play(p, "moon", () -> { obj(p, "night"); stage(p, "night1"); });
            }
            case "night1" -> {
                if (t > 700) play(p, "bump", () -> ask(p, "bump", pick -> {
                    switch (pick) {
                        case "look" -> { score(p, "courage", 1); Creatures2.wakePast(p); }
                        case "quiet" -> score(p, "hope", 1);
                        default -> { score(p, "fury", 1); p.hurt(p.damageSources().generic(), 3f); Net.toPlayer(p, Msg.of("shake").i("ticks", 30)); }
                    }
                    say(p, "bump." + pick);
                    stage(p, "night1b");
                }));
            }
            case "night1b" -> {
                if (t > 900) play(p, "dawn", () -> play(p, "mem1", () -> {
                    say(p, "think.north");
                    obj(p, "north");
                    stage(p, "north");
                }));
            }
            case "north" -> {
                Crates.keep(p, 2);
                if ((moved > 220 || t > 2600) && r != null) {
                    lvl.setDayTime(6000);
                    play(p, "eye", () -> ask(p, "eye", pick -> {
                        score(p, pick.equals("stare") ? "courage" : "hope", 1);
                        say(p, "eye." + pick);
                        eye(p, 0);
                        st(p).putBoolean("eye_free", true);
                        stage(p, "north2");
                    }));
                }
            }
            case "north2" -> {
                Crates.keep(p, 2);
                if (reg == Regions.Region.BONES && r != null) play(p, "bones", () -> {
                    obj(p, "escape");
                    Creatures2.release(p, "gatherer");
                    stage(p, "escape");
                });
            }
            case "escape" -> {
                var g = lvl.getEntitiesOfClass(GathererEntity.class, p.getBoundingBox().inflate(70));
                if (g.isEmpty() || t > 1400) {
                    g.forEach(Entity::discard);
                    say(p, "bones.after");
                    obj(p, "east");
                    stage(p, "east0");
                }
            }
            case "east0" -> { if (t > 160) { say(p, "think.east"); stage(p, "east"); } }
            case "east" -> {
                Crates.keep(p, 2);
                if (reg == Regions.Region.LEVIATHAN && Regions.dist(p.getX(), p.getZ()) > 560 && r != null) {
                    lvl.setDayTime(8000);
                    play(p, "lev", () -> ask(p, "lev", pick -> {
                        score(p, switch (pick) { case "look" -> "courage"; case "close" -> "accept"; default -> "fury"; }, 1);
                        say(p, "lev." + pick);
                        stage(p, "lev_sink");
                    }));
                }
            }
            case "lev_sink" -> {
                if (t == 20) lvl.getEntitiesOfClass(LeviathanEntity.class, p.getBoundingBox().inflate(120)).forEach(l -> l.setAnim("sink"));
                if (t > 120) {
                    lvl.getEntitiesOfClass(LeviathanEntity.class, p.getBoundingBox().inflate(160)).forEach(Entity::discard);
                    play(p, "mem2", () -> { say(p, "think.west"); obj(p, "west"); stage(p, "west"); });
                }
            }
            case "west" -> {
                Crates.keep(p, 2);
                if ((reg == Regions.Region.MIRAGE || moved > 380) && r != null) {
                    lvl.setDayTime(15500);
                    play(p, "voice", () -> ask(p, "voice", pick -> {
                        score(p, switch (pick) { case "answer" -> "hope"; case "silent" -> "courage"; default -> "accept"; }, pick.equals("answer") ? 2 : 1);
                        say(p, "voice." + pick);
                        stage(p, "fog");
                    }));
                }
            }
            case "fog" -> {
                if (t > 400 && r != null) play(p, "taken", () -> {
                    st(p).putBoolean("pip_gone", true);
                    ask(p, "after", pick -> {
                        score(p, pick, 2);
                        String dest = destination(p);
                        st(p).putString("dest", dest);
                        say(p, "dest." + dest);
                        obj(p, "dest_" + dest);
                        lvl.setDayTime(4000);
                        stage(p, "dest");
                    });
                });
            }
            case "dest" -> {
                Crates.keep(p, 1);
                if (t % 1200 == 600) say(p, "think.alone");
                String dest = s.getString("dest");
                Vec3 goal = switch (dest) {
                    case "hope" -> new Vec3(Regions.LIGHTHOUSE_X + 40, 0, Regions.LIGHTHOUSE_Z);
                    case "accept" -> new Vec3(Regions.THRONE_X, 0, Regions.THRONE_Z + 70);
                    case "courage" -> new Vec3(Regions.MAW_X - 80, 0, Regions.MAW_Z);
                    default -> null;
                };
                boolean there = goal != null ? Math.hypot(p.getX() - goal.x, p.getZ() - goal.z) < 60 : Regions.dist(p.getX(), p.getZ()) > Regions.EDGE_R - 40 && p.getZ() > 0;
                if (there && r != null) ending(p, dest);
            }
            default -> {}
        }
        // the Watcher opens its eye now and then, by day, once it has been seen
        if (s.getBoolean("eye_free") && com.krolasyon.bloodsea.world.SeaTime.isDay(lvl) && p.getRandom().nextInt(150) == 0) eye(p, 160);
    }

    /** hope, unless something else outweighs it */
    static String destination(ServerPlayer p) {
        String best = "hope";
        int bv = st(p).getInt("s_hope");
        for (String k : new String[]{"courage", "accept", "fury"}) {
            int v = st(p).getInt("s_" + k);
            if (v > bv) { bv = v; best = k; }
        }
        return best;
    }

    static void bondUp(ServerPlayer p, int n) {
        RaftEntity r = raft(p);
        if (r != null) pip(p.serverLevel(), r).bond += n;
    }

    public static void onGift(ServerPlayer p) {}

    public static void onCrate(ServerPlayer p) {
        if (stage(p).equals("hungry")) {
            say(p, "think.crate");
            obj(p, "explore");
            stage(p, "day1");
        }
    }

    public static void onPet(ServerPlayer p) {
        if (p.getRandom().nextInt(4) == 0) say(p, "think.pet");
    }

    /** which creatures the director may send at this point of the story */
    public static boolean allows(ServerPlayer p, String kind) {
        if (!active(p)) return true;
        if (SESSIONS.containsKey(p.getUUID())) return false;
        String s = stage(p);
        return switch (kind) {
            case "wake" -> s.equals("night1b") || s.equals("dest") || s.equals("fog");
            case "changer" -> !s.equals("wake") && !s.equals("oar") && !s.equals("rowing");
            case "caller" -> s.equals("dest");
            case "gatherer" -> s.equals("dest");
            default -> false;
        };
    }

    // ================================================================== endings
    static void ending(ServerPlayer p, String dest) {
        stage(p, "ending");
        String id = switch (dest) { case "hope" -> "mirage"; case "accept" -> "bone"; case "courage" -> "leviathan"; default -> "edge"; };
        Runnable end = () -> {
            stage(p, "done");
            Endings.begin(p, id);
        };
        switch (id) {
            case "mirage" -> play(p, "mirage", () -> {
                Vec3 h = Sets.hospital(p.serverLevel());
                playAt(p, "hospital", h, 0f, () -> playAt(p, "stomach", Sets.stomach(p.serverLevel()), 0f, end));
            });
            case "bone" -> play(p, "throne", end);
            case "leviathan" -> play(p, "maw", () -> {
                ServerLevel ow = p.server.overworld();
                playAt(p, "rescue", Sets.rescue(ow), 0f, end);
            });
            default -> play(p, "edge", end);
        }
    }

    // ================================================================== choices
    static final Map<UUID, Consumer<String>> ASKING = new HashMap<>();

    static void ask(ServerPlayer p, String id, Consumer<String> then) {
        ASKING.put(p.getUUID(), then);
        Net.toPlayer(p, Msg.of("choice").s("id", id).i("secs", 12));
    }

    public static void onPick(ServerPlayer p, String id, String pick) {
        Consumer<String> c = ASKING.remove(p.getUUID());
        if (c == null) return;
        String[] opts = Lines.CHOICES.get(id);
        if (opts == null) return;
        if (!Arrays.asList(opts).contains(pick)) pick = opts[0];
        BloodSea.LOG.info("[STORY] choice {} -> {}", id, pick);
        c.accept(pick);
    }

    // ================================================================== cutscenes
    static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public static boolean inCut(ServerPlayer p) { return SESSIONS.containsKey(p.getUUID()); }

    /** play a cutscene around the player's raft */
    public static void play(ServerPlayer p, String id, Runnable then) {
        RaftEntity r = raft(p);
        Vec3 o = r != null ? new Vec3(r.getX(), r.getY() + RaftEntity.TOP, r.getZ()) : p.position();
        float yaw = r != null ? r.getYRot() : p.getYRot();
        if (r != null) {
            p.stopRiding();
            r.setDeltaMovement(Vec3.ZERO);
            r.setYRot(Math.round(r.getYRot() / 90f) * 90f);
            yaw = r.getYRot();
        }
        playAt(p, id, o, yaw, then);
    }

    public static void playAt(ServerPlayer p, String id, Vec3 origin, float yaw, Runnable then) {
        Cut c = Cuts.ALL.get(id);
        if (c == null) { then.run(); return; }
        Session old = SESSIONS.remove(p.getUUID());
        if (old != null) old.cleanup(false);
        Session s = new Session(p, c, origin, yaw, then);
        SESSIONS.put(p.getUUID(), s);
        s.begin();
    }

    public static void onCutDone(ServerPlayer p) {
        Session s = SESSIONS.get(p.getUUID());
        if (s != null && s.t > 20) s.end();
    }

    /** every tick */
    private static final List<Object[]> LATER = new ArrayList<>();
    private static int clock;

    /** run something a number of server ticks from now */
    public static void later(int ticks, Runnable r) { LATER.add(new Object[]{clock + ticks, r}); }

    public static void serverTick(net.minecraft.server.MinecraftServer server) {
        clock++;
        for (Object[] o : new ArrayList<>(LATER)) if ((int) o[0] <= clock) { LATER.remove(o); ((Runnable) o[1]).run(); }
        for (Session s : new ArrayList<>(SESSIONS.values())) s.tick();
    }

    /** a running cutscene: its clock, the people and things in it */
    public static final class Session {
        public final ServerPlayer p;
        public final Cut cut;
        public final Vec3 o;
        public final float yaw;
        final Runnable then;
        int t;
        ActorEntity actor;
        PipEntity pip;
        final Map<String, Entity> things = new HashMap<>();
        final List<Mover> movers = new ArrayList<>();
        Vec3 home;
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> homeDim;
        boolean ended;
        long[] timeRamp;

        Session(ServerPlayer p, Cut cut, Vec3 o, float yaw, Runnable then) {
            this.p = p; this.cut = cut; this.o = o; this.yaw = yaw; this.then = then;
        }

        public ServerLevel lvl() { return p.serverLevel(); }

        public Vec3 at(double lx, double ly, double lz) {
            double a = Math.toRadians(yaw), c = Math.cos(a), s = Math.sin(a);
            return new Vec3(o.x + lx * c - lz * s, o.y + ly, o.z + lx * s + lz * c);
        }

        void begin() {
            home = p.position();
            homeDim = p.level().dimension();
            // the set may be far away: the body goes with the camera so the world is loaded there
            if (o.distanceTo(p.position()) > 64 || !p.level().dimension().equals(lvl().dimension())) {
                Vec3 w = at(2.5, 1.0, 2.5);
                p.teleportTo(lvl(), w.x, w.y, w.z, 0, 0);
            }
            p.setInvulnerable(true);
            p.setNoGravity(true);
            p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 999999, 0, false, false));
            SeaCreature.FROZEN = true;
            var pips = lvl().getEntitiesOfClass(PipEntity.class, new AABB(o, o).inflate(48));
            if (!pips.isEmpty()) {
                pip = pips.get(0);
                pip.scripted = true;
            }
            Net.toPlayer(p, Msg.of("cut").s("id", cut.id).d("x", o.x).d("y", o.y).d("z", o.z).f("yaw", yaw));
            BloodSea.LOG.info("[STORY] cut {} begins", cut.id);
        }

        void tick() {
            if (ended) return;
            for (Cut.Ev e : cut.events) if (e.t() == t) {
                try { e.run().accept(this); } catch (Throwable ex) { BloodSea.LOG.error("[STORY] event failed in {} at {}", cut.id, t, ex); }
            }
            for (Iterator<Mover> it = movers.iterator(); it.hasNext(); ) if (it.next().step(t)) it.remove();
            if (timeRamp != null) {
                float k = Mth.clamp((t - timeRamp[2]) / (float) timeRamp[3], 0, 1);
                lvl().setDayTime((long) Mth.lerp(k, timeRamp[0], timeRamp[1]));
            }
            p.setDeltaMovement(Vec3.ZERO);
            t++;
            if (t > cut.length + 80) end();
        }

        void end() {
            if (ended) return;
            ended = true;
            SESSIONS.remove(p.getUUID());
            cleanup(true);
            Net.toPlayer(p, Msg.of("cut_end"));
            BloodSea.LOG.info("[STORY] cut {} ends", cut.id);
            if (cut.choice != null) ask(p, cut.choice, pick -> then.run());
            else then.run();
        }

        void cleanup(boolean restore) {
            if (actor != null) actor.discard();
            for (Entity e : things.values()) if (e != null && !e.getTags().contains("keep")) e.discard();
            if (pip != null && !pip.isRemoved()) {
                pip.scripted = false;
                pip.setAnim("");
            }
            SeaCreature.FROZEN = false;
            p.removeEffect(MobEffects.INVISIBILITY);
            p.setInvulnerable(false);
            p.setNoGravity(false);
            if (restore && home != null && (p.position().distanceTo(home) > 3 || !p.level().dimension().equals(homeDim))) {
                ServerLevel hl = p.server.getLevel(homeDim);
                if (hl != null) p.teleportTo(hl, home.x, home.y, home.z, p.getYRot(), 0);
            }
            // back aboard, at the stern
            RaftEntity r = raft(p);
            if (restore && r != null) {
                Vec3 st = r.local(0, RaftEntity.TOP + 0.02, -1.2);
                p.teleportTo(st.x, st.y, st.z);
            }
        }

        // ------------------------------------------------------------------ what events can do
        public ActorEntity actor() {
            if (actor == null || actor.isRemoved()) {
                actor = ModEntities.ACTOR.get().create(lvl());
                Vec3 w = at(0, 0, 0);
                actor.moveTo(w.x, w.y, w.z, yaw, 0);
                lvl().addFreshEntity(actor);
            }
            return actor;
        }

        public void actor(String anim) { actor().setAnim(anim); }

        public void actorAt(double lx, double ly, double lz, float ly2) { put(actor(), lx, ly, lz, ly2); }

        public void actorWalk(double lx, double ly, double lz, float lyaw, int ticks) { move(actor(), lx, ly, lz, lyaw, ticks); }

        public void pip(String anim) { if (pip != null) pip.setAnim(anim); }

        public void pipAt(double lx, double ly, double lz, float lyaw) {
            if (pip == null) return;
            if (pip.isPassenger()) pip.stopRiding();
            put(pip, lx, ly, lz, lyaw);
        }

        public void pipWalk(double lx, double ly, double lz, float lyaw, int ticks) { if (pip != null) move(pip, lx, ly, lz, lyaw, ticks); }

        public void pipGone() {
            if (pip != null) {
                pip.discard();
                pip = null;
            }
        }

        public void pipMeow() { if (pip != null) pip.meow(); }

        public Entity spawn(String kind, double lx, double ly, double lz, float lyaw) {
            EntityType<?> type = switch (kind) {
                case "pip" -> ModEntities.PIP.get();
                case "wake" -> ModEntities.WAKE.get();
                case "gatherer" -> ModEntities.GATHERER.get();
                case "caller" -> ModEntities.CALLER.get();
                case "leviathan" -> ModEntities.LEVIATHAN.get();
                case "rescuer" -> ModEntities.RESCUER.get();
                default -> ModEntities.CHANGER.get();
            };
            Entity e = type.create(lvl());
            if (e instanceof SeaCreature sc) sc.scripted = true;
            if (e instanceof PipEntity pe) pe.scripted = true;
            Vec3 w = at(lx, ly, lz);
            e.moveTo(w.x, w.y, w.z, yaw + lyaw, 0);
            lvl().addFreshEntity(e);
            things.put(kind, e);
            return e;
        }

        public void keep(String kind) { Entity e = things.get(kind); if (e != null) e.addTag("keep"); }

        public void anim(String kind, String anim) { if (things.get(kind) instanceof CreatureBase c) c.setAnim(anim); }

        public void act(String kind, String action) { if (things.get(kind) instanceof CreatureBase c) c.playAction(action); }

        public void moveThing(String kind, double lx, double ly, double lz, float lyaw, int ticks) {
            Entity e = things.get(kind);
            if (e != null) move(e, lx, ly, lz, lyaw, ticks);
        }

        public void gone(String kind) { Entity e = things.remove(kind); if (e != null) e.discard(); }

        public void sound(String name, float vol, float pitch) {
            lvl().playSound(null, o.x, o.y + 1, o.z, ModSounds.get(name), SoundSource.MASTER, vol, pitch);
        }

        public void soundAt(String name, double lx, double ly, double lz, float vol, float pitch) {
            Vec3 w = at(lx, ly, lz);
            lvl().playSound(null, w.x, w.y, w.z, ModSounds.get(name), SoundSource.MASTER, vol, pitch);
        }

        public void time(long t0, long t1, int ticks) { timeRamp = new long[]{t0, t1, t, Math.max(1, ticks)}; }

        public void eye(int ticks) { Story.eye(p, ticks); }

        public void shake(int ticks) { Net.toPlayer(p, Msg.of("shake").i("ticks", ticks)); }

        public void puff(double lx, double ly, double lz, int n) {
            Vec3 w = at(lx, ly, lz);
            lvl().sendParticles(ParticleTypes.LARGE_SMOKE, w.x, w.y, w.z, n, 0.2, 0.1, 0.2, 0.01);
        }

        public void splash(double lx, double ly, double lz) {
            Vec3 w = at(lx, ly, lz);
            lvl().sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.5f, 0.02f, 0.03f), 2.0f), w.x, w.y, w.z, 40, 0.5, 0.3, 0.5, 0.2);
            soundAt("blood_splash", lx, ly, lz, 1.5f, 0.9f);
        }

        public void drop(ItemStack s, double lx, double ly, double lz) {
            Vec3 w = at(lx, ly, lz);
            ItemEntity ie = new ItemEntity(lvl(), w.x, w.y, w.z, s);
            ie.setDeltaMovement(Vec3.ZERO);
            lvl().addFreshEntity(ie);
        }

        void put(Entity e, double lx, double ly, double lz, float lyaw) {
            Vec3 w = at(lx, ly, lz);
            e.moveTo(w.x, w.y, w.z, yaw + lyaw, 0);
            e.setYHeadRot(yaw + lyaw);
            e.setYBodyRot(yaw + lyaw);
            movers.removeIf(m -> m.e == e);
        }

        void move(Entity e, double lx, double ly, double lz, float lyaw, int ticks) {
            movers.removeIf(m -> m.e == e);
            movers.add(new Mover(e, e.position(), at(lx, ly, lz), e.getYRot(), yaw + lyaw, t, Math.max(1, ticks)));
        }
    }

    record Mover(Entity e, Vec3 a, Vec3 b, float ya, float yb, int t0, int dur) {
        boolean step(int t) {
            float k = Mth.clamp((t - t0) / (float) dur, 0, 1);
            float s = k * k * (3 - 2 * k);
            Vec3 v = a.lerp(b, s);
            float y = ya + Mth.wrapDegrees(yb - ya) * s;
            e.moveTo(v.x, v.y, v.z, y, e.getXRot());
            e.setYHeadRot(y);
            e.setYBodyRot(y);
            return k >= 1;
        }
    }
}
