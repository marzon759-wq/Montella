package com.krolasyon.mimic.story;

import com.krolasyon.mimic.MimicMod;
import com.krolasyon.mimic.block.ModeBlock;
import com.krolasyon.mimic.block.OpenableBlock;
import com.krolasyon.mimic.entity.GuestEntity;
import com.krolasyon.mimic.entity.MimicEntity;
import com.krolasyon.mimic.entity.TruckEntity;
import com.krolasyon.mimic.net.Msg;
import com.krolasyon.mimic.net.Net;
import com.krolasyon.mimic.registry.ModEntities;
import com.krolasyon.mimic.registry.ModItems;
import com.krolasyon.mimic.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** The story director: world plumbing, interaction, the flashlight, noise, the dialogue and cutscene engines, the free roam. */
public final class Story {
    public static StoryState S = new StoryState();
    static MinecraftServer server;
    static final Random RND = new Random();
    static MimicEntity hm;
    static GuestEntity lily;
    static TruckEntity truck;
    private static final List<Object[]> timers = new ArrayList<>();
    private static boolean hudDirty = true;
    private static int age = 0;
    // dialogue
    static String dlgId = null;
    private static String dlgNode = null;
    private static int dlgAuto = -1;
    // cutscene
    static String cutId = null;
    static int cutT = 0;
    // hiding
    private static String hiddenId = null;
    private static boolean seenHide = false;
    // quick time events
    private static String qteType = null;
    private static int qteTicks = 0;
    private static boolean breathFailed = false;
    // caught
    private static int respawnIn = -1;
    // the free roam
    static int hauntCd = 400;
    private static int relocations = 0;
    private static int thunderCd = 300;
    private static int safeTicks = 0;
    // the flashlight's light blocks
    private static final List<BlockPos> lights = new ArrayList<>();
    /** the loudest noise of the last second, local position and radius */
    static Vec3 noisePos = null;
    static double noiseRadius = 0;
    static int noiseAge = 0;

    private Story() {}

    // ================================================================== lifecycle
    public static void load(MinecraftServer s) {
        server = s;
        S = StoryState.load(s);
        House.ensureIndexed();
        hm = null;
        lily = null;
        truck = null;
        timers.clear();
        lights.clear();
        dlgId = null;
        cutId = null;
        hiddenId = null;
        qteType = null;
        respawnIn = -1;
        if (S.active) S.vars.put("resume", 1);
    }

    public static void unload(MinecraftServer s) {
        if (S.active) {
            clearLights();
            S.save(s);
        }
    }

    public static void onLogin(ServerPlayer p) {
        if (S.active && p.getUUID().toString().equals(S.player)) {
            S.vars.put("resume", 1);
            hudDirty = true;
        }
    }

    public static ServerPlayer player(MinecraftServer s) {
        if (!S.active || s == null || S.player.isEmpty()) return null;
        try {
            return s.getPlayerList().getPlayer(UUID.fromString(S.player));
        } catch (Exception e) {
            return null;
        }
    }

    static ServerPlayer player() { return player(server); }

    public static boolean isStoryPlayer(ServerPlayer p) {
        return S.active && p.getUUID().toString().equals(S.player);
    }

    public static boolean lightOn(ServerPlayer p) { return isStoryPlayer(p) && S.lightOn; }

    public static String status() {
        if (!S.active) return "MIMIC: no story running. /mimic start";
        return "MIMIC: chapter " + S.chapter + ", step " + S.step + ", notes " + S.notes.size() + "/" + Notes.COUNT + ", caught " + S.caught;
    }

    public static void start(ServerPlayer p) {
        if (S.active) {
            ServerPlayer cur = player();
            if (cur != null && cur != p) {
                p.sendSystemMessage(Component.translatable("msg.mimic.busy"));
                return;
            }
            stop(p, false);
        }
        server = p.getServer();
        ServerLevel lvl = p.serverLevel();
        S = new StoryState();
        S.active = true;
        S.player = p.getUUID().toString();
        BlockPos pp = p.blockPosition();
        int oy = Math.max(lvl.getMinBuildHeight() + 12, Math.min(pp.getY() - 1, lvl.getMaxBuildHeight() - 44));
        S.ox = pp.getX() - 50;
        S.oy = oy;
        S.oz = pp.getZ() - 102;
        S.prevMode = p.gameMode.getGameModeForPlayer().getName();
        GameRules gr = lvl.getGameRules();
        S.prevDaylight = gr.getBoolean(GameRules.RULE_DAYLIGHT);
        S.prevWeather = gr.getBoolean(GameRules.RULE_WEATHER_CYCLE);
        S.prevMobs = gr.getBoolean(GameRules.RULE_DOMOBSPAWNING);
        S.prevFire = gr.getBoolean(GameRules.RULE_DOFIRETICK);
        S.prevTime = lvl.getDayTime();
        applyWorldRules(lvl, true);
        p.sendSystemMessage(Component.translatable("msg.mimic.building"));
        long t0 = System.currentTimeMillis();
        clearEntities(lvl);
        House.build(lvl, origin());
        MimicMod.LOG.info("Harrow Farm built in {} ms at {}", System.currentTimeMillis() - t0, origin());
        p.setGameMode(GameType.ADVENTURE);
        p.getInventory().clearContent();
        Steps.enter("intro");
        S.save(server);
    }

    public static void stop(ServerPlayer p, boolean announce) {
        if (!S.active) {
            if (announce) p.sendSystemMessage(Component.translatable("msg.mimic.not_running"));
            return;
        }
        ServerPlayer sp = player();
        ServerLevel lvl = sp != null ? sp.serverLevel() : p.serverLevel();
        clearLights();
        applyWorldRules(lvl, false);
        removeMimic();
        if (lily != null) lily.discard();
        if (truck != null) truck.discard();
        lily = null;
        truck = null;
        if (sp != null) {
            if (hiddenId != null) unhide(sp);
            sp.setInvulnerable(false);
            sp.setInvisible(false);
            GameType gt = GameType.byName(S.prevMode, GameType.SURVIVAL);
            sp.setGameMode(gt);
            Net.toPlayer(sp, Msg.of("end"));
        }
        for (String id : House.DOORS.keySet()) S.unlocked.add(id);
        S.active = false;
        dlgId = null;
        cutId = null;
        hiddenId = null;
        qteType = null;
        timers.clear();
        S.save(server);
        if (announce) p.sendSystemMessage(Component.translatable("msg.mimic.stopped"));
    }

    private static void applyWorldRules(ServerLevel lvl, boolean story) {
        GameRules gr = lvl.getGameRules();
        gr.getRule(GameRules.RULE_DAYLIGHT).set(!story && S.prevDaylight, server);
        gr.getRule(GameRules.RULE_WEATHER_CYCLE).set(!story && S.prevWeather, server);
        gr.getRule(GameRules.RULE_DOMOBSPAWNING).set(!story && S.prevMobs, server);
        gr.getRule(GameRules.RULE_DOFIRETICK).set(!story && S.prevFire, server);
        if (story) {
            lvl.setDayTime(18000);
            // a dry storm: clouds and thunder, no rain
            lvl.setWeatherParameters(0, 2000000, false, false);
        } else {
            lvl.setWeatherParameters(12000, 0, false, false);
        }
    }

    private static void clearEntities(ServerLevel lvl) {
        AABB box = new AABB(S.ox - 16, S.oy - 12, S.oz - 16, S.ox + 120, S.oy + 40, S.oz + 124);
        for (Entity e : lvl.getEntitiesOfClass(Entity.class, box)) {
            if (e instanceof ServerPlayer) continue;
            e.discard();
        }
    }

    // ================================================================== coordinates
    public static BlockPos origin() { return new BlockPos(S.ox, S.oy, S.oz); }

    static BlockPos wp(int x, int y, int z) { return new BlockPos(S.ox + x, S.oy + y, S.oz + z); }

    static BlockPos wp(BlockPos local) { return local.offset(S.ox, S.oy, S.oz); }

    static Vec3 w(double x, double y, double z) { return new Vec3(S.ox + x, S.oy + y, S.oz + z); }

    public static Vec3 worldCenter(BlockPos local) { return new Vec3(S.ox + local.getX() + 0.5, S.oy + local.getY(), S.oz + local.getZ() + 0.5); }

    static BlockPos local(BlockPos world) { return world.offset(-S.ox, -S.oy, -S.oz); }

    static Vec3 lp(ServerPlayer p) { return new Vec3(p.getX() - S.ox, p.getY() - S.oy, p.getZ() - S.oz); }

    public static Vec3 lpos(Vec3 world) { return world.subtract(S.ox, S.oy, S.oz); }

    static String room(ServerPlayer p) {
        Vec3 l = lp(p);
        return House.roomAt(l.x, l.y, l.z);
    }

    public static String roomAtWorld(Vec3 v) {
        Vec3 l = lpos(v);
        return House.roomAt(l.x, l.y, l.z);
    }

    static boolean in(ServerPlayer p, String room) { return room(p).equals(room); }

    static double distLocal(ServerPlayer p, double x, double y, double z) { return lp(p).distanceTo(new Vec3(x, y, z)); }

    public static String hotspot(BlockPos world) {
        if (!S.active) return null;
        return House.HOT.get(local(world));
    }

    /** crouched in the corn rows */
    public static boolean inCover(ServerPlayer p) {
        Vec3 l = lp(p);
        return p.isCrouching() && House.inCorn(l.x, l.y, l.z);
    }

    // ================================================================== messaging
    static void hud() { hudDirty = true; }

    static void obj(String key) { obj(key, ""); }

    static void obj(String key, String arg) {
        boolean changed = !key.equals(S.objective);
        S.objective = key;
        S.objectiveArg = arg;
        hudDirty = true;
        ServerPlayer p = player();
        if (changed && p != null && !key.isEmpty()) Net.toPlayer(p, Msg.of("objective"));
    }

    static void say(String key, String speaker, int ticks) {
        ServerPlayer p = player();
        if (p != null) Net.toPlayer(p, Msg.of("line").s("key", key).s("speaker", speaker).i("dur", ticks));
    }

    /** an inner-voice line: "line.<key>" */
    static int age() { return age; }

    /** the player struck the mimic: the story decides what that does */
    public static boolean onMimicHit(ServerPlayer p) { return S.active && Steps.onFightHit(p); }

    private static final java.util.Map<String, Integer> THOUGHT_AT = new java.util.HashMap<>();

    /** an inner-voice line: "line.<key>", in Sam's voice when it has one (not again if it was just said) */
    static void think(String key) {
        String n = "v.you." + key;
        Integer last = THOUGHT_AT.get(key);
        boolean repeat = last != null && age - last < 100;
        if (!ModSounds.has(n) || skipping || repeat) {
            if (!repeat) say("line." + key, "you", 90);
            return;
        }
        THOUGHT_AT.put(key, age);
        int d = voiceSlot(n);
        later(Math.max(0, d), () -> {
            sndNow(n, 1f, 1f);
            say("line." + key, "you", Math.max(60, voiceTicks(n) + 12));
        });
    }

    static void hint(String key) {
        ServerPlayer p = player();
        if (p != null) Net.toPlayer(p, Msg.of("hint").s("key", key));
    }

    private static java.util.Map<String, Integer> VOICE_LEN = null;

    /** how long a voice line plays, in ticks (from the generated voice_lengths.json) */
    static int voiceTicks(String sound) {
        if (VOICE_LEN == null) {
            VOICE_LEN = new java.util.HashMap<>();
            try (var in = Story.class.getResourceAsStream("/assets/mimicry/voice_lengths.json")) {
                if (in != null) {
                    var obj = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                    for (var e : obj.entrySet()) VOICE_LEN.put(e.getKey(), e.getValue().getAsInt());
                }
            } catch (Exception ex) {
                MimicMod.LOG.warn("no voice lengths", ex);
            }
        }
        // the server does not know the player's language: wait for the longer of the two recordings
        return Math.max(VOICE_LEN.getOrDefault(sound, 80), VOICE_LEN.getOrDefault("tr." + sound, 0));
    }

    /** a voice somewhere in the world, with its subtitle */
    static void voiceAt(double x, double y, double z, String sound, String speaker) {
        if (skipping) return;
        int d = voiceSlot(sound);
        later(Math.max(0, d), () -> {
            sndAtNow(x, y, z, sound, 1.3f, 1f);
            say("sub." + sound, speaker, voiceTicks(sound) + 10);
        });
    }

    /** a voice on the walkie-talkie in your pocket */
    static void radio(String sound, int ticks) {
        int d = voiceSlot(sound);
        later(Math.max(0, d), () -> snd("fx.radio_click", 0.9f, 1f));
        later(Math.max(0, d) + 6, () -> {
            sndNow(sound, 1f, 1f);
            say("sub." + sound, "lily", Math.max(ticks, voiceTicks(sound) + 10));
        });
        ServerPlayer p = player();
        if (p != null) noiseEvent(local(p.blockPosition()), 12);
    }

    static void msg(String key, Object... args) {
        ServerPlayer p = player();
        if (p != null) p.displayClientMessage(Component.translatable("msg.mimic." + key, args), true);
    }

    static void chapter(int n) {
        S.chapter = n;
        ServerPlayer p = player();
        if (p != null) Net.toPlayer(p, Msg.of("chapter").i("n", n));
        hudDirty = true;
        S.save(server);
    }

    public static void fx(ServerPlayer p, String type, int ticks) {
        if (p != null) Net.toPlayer(p, Msg.of("fx").s("t", type).i("n", ticks));
    }

    static void fx(String type, int ticks) { fx(player(), type, ticks); }

    // ---------------------------------------------------------------- one voice at a time
    /** the tick the last queued voice line finishes; every spoken line waits its turn, nobody talks over anybody */
    private static int voiceBusy = 0;
    /** set while a skipped cutscene runs its remaining cues: they happen, but silently */
    static boolean skipping = false;

    /** reserve the voice channel: returns how many ticks to wait before this line may start */
    static int voiceSlot(String name) {
        int start = Math.max(age, voiceBusy);
        voiceBusy = start + voiceTicks(name) + 6;
        return start - age;
    }

    static void snd(String name, float vol, float pitch) {
        if (name.startsWith("v.")) {
            if (skipping) return;
            int d = voiceSlot(name);
            if (d > 0) {
                later(d, () -> sndNow(name, vol, pitch));
                return;
            }
        }
        sndNow(name, vol, pitch);
    }

    private static void sndNow(String name, float vol, float pitch) {
        logVoice(name);
        ServerPlayer p = player();
        if (p != null) p.playNotifySound(ModSounds.get(name), SoundSource.AMBIENT, vol, pitch);
    }

    static void sndAt(double x, double y, double z, String name, float vol, float pitch) {
        if (name.startsWith("v.")) {
            if (skipping) return;
            int d = voiceSlot(name);
            if (d > 0) {
                later(d, () -> sndAtNow(x, y, z, name, vol, pitch));
                return;
            }
        }
        sndAtNow(x, y, z, name, vol, pitch);
    }

    private static void sndAtNow(double x, double y, double z, String name, float vol, float pitch) {
        logVoice(name);
        ServerPlayer p = player();
        if (p == null) return;
        Vec3 v = w(x, y, z);
        p.serverLevel().playSound(null, v.x, v.y, v.z, ModSounds.get(name), SoundSource.AMBIENT, vol, pitch);
    }

    /** development: every voice line with the tick it starts on and how long it runs, to check nothing talks over anything */
    private static void logVoice(String name) {
        if (name.startsWith("v.") && System.getProperty("mimic.autotest") != null)
            MimicMod.LOG.info("[VOICE] {} {} {} step={}", age, voiceTicks(name), name, S.step);
    }

    static void later(int ticks, Runnable r) { timers.add(new Object[]{age + ticks, r}); }

    static int inc(String k) {
        int v = S.v(k) + 1;
        S.v(k, v);
        return v;
    }

    static void credits(ServerPlayer p) {
        Net.toPlayer(p, Msg.of("credits").s("ending", S.ending).i("notes", S.notes.size()).i("notesMax", Notes.COUNT).i("caught", S.caught).i("lies", learnedCount()));
    }

    static int learnedCount() {
        int n = 0;
        for (String f : S.flags) if (f.startsWith("learned:")) n++;
        return n;
    }

    // ================================================================== noise: it hears everything
    /** a sound the thing can hear, at a local block position, heard up to radius blocks away */
    static void noiseEvent(BlockPos local, double radius) {
        Vec3 v = new Vec3(local.getX() + 0.5, local.getY(), local.getZ() + 0.5);
        if (noisePos == null || noiseAge > 20 || radius >= noiseRadius) {
            noisePos = v;
            noiseRadius = radius;
            noiseAge = 0;
        }
    }

    // ================================================================== the body: breath for running
    static float stamina = 100;
    static boolean tired = false;
    private static int breathCd = 0;
    private static float sentStam = -1;
    private static boolean sentTired = false;

    /** sprinting spends breath (about nine seconds of it), walking and crouching give it back. Out of breath you cannot
     *  run, and you pant: it can hear that. */
    static void tickBody(ServerPlayer p) {
        boolean moving = p.getDeltaMovement().horizontalDistanceSqr() > 0.002;
        if (p.isSprinting() && moving) stamina -= 0.55f;
        else if (p.isCrouching() || !moving) stamina += 0.7f;
        else stamina += 0.35f;
        stamina = Math.max(0, Math.min(100, stamina));
        if (stamina <= 0 && !tired) {
            tired = true;
            if (!S.f("hint_run")) {
                S.flags.add("hint_run");
                hint("run");
            }
        }
        if (tired && stamina >= 35) tired = false;
        if (tired && p.isSprinting()) p.setSprinting(false);
        if (stamina < 30 && hiddenId == null && --breathCd <= 0) {
            breathCd = 24 + (int) stamina;
            p.playNotifySound(ModSounds.get("fx.gasp"), SoundSource.PLAYERS, 0.35f + (30 - stamina) / 60f, 0.8f + RND.nextFloat() * 0.15f);
            noiseEvent(local(p.blockPosition()), p.isCrouching() ? 4 : 9);
        }
        if (Math.abs(stamina - sentStam) >= 1.5f || tired != sentTired || (stamina >= 100 && sentStam < 100)) {
            sentStam = stamina;
            sentTired = tired;
            Net.toPlayer(p, Msg.of("body").f("stam", stamina).b("tired", tired));
        }
    }

    /** how far the player's movement can be heard right now */
    public static double footstepRadius(ServerPlayer p) {
        if (p.isCrouching()) return 1.5;
        double sp = p.getDeltaMovement().horizontalDistance();
        if (sp < 0.02) return 0;
        if (p.isSprinting()) return 17;
        return 7.5;
    }

    /** the loudest thing the mimic could hear from where it stands: a world position, or null */
    public static Vec3 heard(Vec3 from) {
        if (!S.active) return null;
        ServerPlayer p = player();
        if (p == null) return null;
        if (noisePos != null && noiseAge < 20) {
            Vec3 wv = w(noisePos.x, noisePos.y, noisePos.z);
            if (wv.distanceTo(from) < noiseRadius) return wv;
        }
        double r = footstepRadius(p);
        if (hiddenId == null && r > 0 && p.position().distanceTo(from) < r) return p.position();
        return null;
    }

    // ================================================================== inventory
    static boolean has(String id) {
        ServerPlayer p = player();
        if (p == null) return false;
        Item it = ModItems.story(id);
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(it)) return true;
        return false;
    }

    static boolean take(String id) {
        ServerPlayer p = player();
        if (p == null) return false;
        Item it = ModItems.story(id);
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.is(it)) {
                st.shrink(1);
                return true;
            }
        }
        return false;
    }

    static void give(String id) {
        ServerPlayer p = player();
        if (p == null) return;
        ItemStack st = new ItemStack(ModItems.story(id));
        Component name = st.getHoverName();
        if (!p.getInventory().add(st)) p.drop(st, false);
        p.displayClientMessage(Component.translatable("msg.mimic.picked", name), true);
        snd("fx.pickup", 0.9f, 1f);
        hudDirty = true;
    }

    static void note(String id) {
        ServerPlayer p = player();
        if (p == null) return;
        boolean fresh = S.notes.add(id);
        snd("fx.paper", 0.9f, 1f);
        Net.toPlayer(p, Msg.of("note").s("id", id).b("fresh", fresh));
        hudDirty = true;
    }

    /** right click with a story item in hand */
    public static boolean useItem(ServerPlayer p, String id) {
        if (cutId != null || dlgId != null || respawnIn >= 0) return false;
        switch (id) {
            case "flashlight" -> {
                toggleLight(p);
                return true;
            }
            case "battery" -> {
                if (S.battery > 90) {
                    msg("battery_full");
                    return true;
                }
                if (take("battery")) {
                    S.battery = 100;
                    snd("fx.light_on", 0.8f, 1.3f);
                    msg("battery_swap");
                    hudDirty = true;
                }
                return true;
            }
            case "walkie" -> {
                if (S.step.equals("c1_radio") && S.v("sub") == 0) {
                    snd("fx.radio_click", 1f, 1f);
                    noiseEvent(local(p.blockPosition()), 10);
                    startDialogue("radio1", "r1");
                } else {
                    snd("fx.radio_static", 0.8f, 1f);
                    noiseEvent(local(p.blockPosition()), 14);
                    msg("walkie_static");
                }
                return true;
            }
            case "meat" -> {
                // thrown: a distraction anywhere
                if (!S.step.equals("c3_trap")) {
                    Vec3 at = p.position().add(p.getViewVector(1f).scale(8));
                    take("meat");
                    snd("fx.throw", 1f, 1f);
                    noiseEvent(local(BlockPos.containing(at)), 30);
                    msg("threw_meat");
                    return true;
                }
                return false;
            }
            default -> {
                msg("item_" + id);
                return true;
            }
        }
    }

    // ================================================================== the flashlight
    static void toggleLight(ServerPlayer p) {
        if (!S.hasLight) return;
        if (!S.lightOn && S.battery <= 0 && !take("battery")) {
            msg("light_dead");
            snd("fx.light_flicker", 1f, 1f);
            return;
        }
        if (!S.lightOn && S.battery <= 0) S.battery = 100;
        S.lightOn = !S.lightOn;
        snd(S.lightOn ? "fx.light_on" : "fx.light_off", 0.9f, 1f);
        noiseEvent(local(p.blockPosition()), 3);
        if (!S.lightOn) clearLights();
        hudDirty = true;
        sendHud(p);
    }

    /** real light where the beam lands, and a little around you: invisible light blocks, moved every other tick */
    private static void updateLights(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        List<BlockPos> want = new ArrayList<>();
        if (S.lightOn && hiddenId == null && cutId == null) {
            Vec3 eye = p.getEyePosition();
            Vec3 dir = p.getViewVector(1f);
            BlockHitResult hit = lvl.clip(new ClipContext(eye, eye.add(dir.scale(26)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            double dist = hit.getType() == HitResult.Type.MISS ? 26 : hit.getLocation().distanceTo(eye);
            // where the beam lands, and two points along it
            for (double f : new double[]{Math.max(1.5, dist - 0.6), dist * 0.55, Math.min(3.0, dist * 0.3)}) {
                BlockPos bp = BlockPos.containing(eye.add(dir.scale(f)));
                if (!want.contains(bp)) want.add(bp);
            }
            want.add(p.blockPosition().above());
        }
        // remove the old ones that are no longer wanted
        for (Iterator<BlockPos> it = lights.iterator(); it.hasNext(); ) {
            BlockPos bp = it.next();
            if (!want.contains(bp)) {
                if (lvl.getBlockState(bp).is(Blocks.LIGHT)) lvl.setBlock(bp, Blocks.AIR.defaultBlockState(), 3);
                it.remove();
            }
        }
        float flick = S.battery < 12 && RND.nextInt(6) == 0 ? 0.4f : 1f;
        for (int i = 0; i < want.size(); i++) {
            BlockPos bp = want.get(i);
            BlockState cur = lvl.getBlockState(bp);
            if (!cur.isAir() && !cur.is(Blocks.LIGHT)) continue;
            int level = (int) ((i == 0 ? 13 : i == 1 ? 11 : i == 2 ? 9 : 6) * flick);
            BlockState ls = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, Math.max(1, level));
            if (!cur.equals(ls)) lvl.setBlock(bp, ls, 3);
            if (!lights.contains(bp)) lights.add(bp);
        }
    }

    static void clearLights() {
        ServerPlayer p = player();
        if (p != null) {
            ServerLevel lvl = p.serverLevel();
            for (BlockPos bp : lights) if (lvl.getBlockState(bp).is(Blocks.LIGHT)) lvl.setBlock(bp, Blocks.AIR.defaultBlockState(), 3);
        }
        lights.clear();
    }

    /** is the player looking at this point (and the flashlight on it) */
    static boolean playerLooksAt(ServerPlayer p, Vec3 point, double dot) {
        Vec3 eye = p.getEyePosition();
        Vec3 dir = point.subtract(eye).normalize();
        if (dir.dot(p.getViewVector(1f)) < dot) return false;
        return p.serverLevel().clip(new ClipContext(eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS;
    }

    // ================================================================== lights of the farm
    private static int lampMode(House.Lamp l) {
        if (S.f("dark:" + l.room) || S.f("dark:all")) return 0;
        return l.quality == 0 ? 1 : l.quality == 1 ? 2 : 0;
    }

    static void refreshLamps(String roomPrefix) {
        ServerPlayer p = player();
        if (p == null) return;
        ServerLevel lvl = p.serverLevel();
        for (House.Lamp l : House.LAMPS) {
            if (roomPrefix != null && !l.room.startsWith(roomPrefix)) continue;
            setLamp(lvl, l, lampMode(l));
        }
    }

    private static void setLamp(ServerLevel lvl, House.Lamp l, int mode) {
        BlockPos pos = wp(l.pos);
        BlockState s = lvl.getBlockState(pos);
        if (s.hasProperty(ModeBlock.MODE) && s.getValue(ModeBlock.MODE) != mode) lvl.setBlock(pos, s.setValue(ModeBlock.MODE, mode), 2);
    }

    static void flicker(String roomPrefix, int ticks) {
        ServerPlayer p = player();
        if (p == null) return;
        ServerLevel lvl = p.serverLevel();
        for (House.Lamp l : House.LAMPS) if (l.room.startsWith(roomPrefix)) setLamp(lvl, l, 0);
        int n = Math.max(1, ticks / 8);
        for (int i = 1; i <= n; i++) {
            final boolean on = i % 2 == 0 || i == n;
            later(i * 8 + RND.nextInt(4), () -> {
                ServerPlayer pp = player();
                if (pp == null) return;
                for (House.Lamp l : House.LAMPS) if (l.room.startsWith(roomPrefix)) setLamp(pp.serverLevel(), l, on ? lampMode(l) : 0);
            });
        }
    }

    static void setMode(String tag, int mode) {
        ServerPlayer p = player();
        if (p == null) return;
        List<BlockPos> l = House.TAGGED.get(tag);
        if (l == null) return;
        for (BlockPos lpos : l) {
            BlockPos pos = wp(lpos);
            BlockState s = p.serverLevel().getBlockState(pos);
            if (s.hasProperty(ModeBlock.MODE)) p.serverLevel().setBlock(pos, s.setValue(ModeBlock.MODE, mode), 3);
        }
    }

    // ================================================================== doors
    private static final String[] BASE_LOCKED = {"silo", "storage", "shed"};

    static boolean locked(String id) {
        if (S.unlocked.contains(id)) return false;
        if (S.f("lock:" + id)) return true;
        if (id.equals("front") && !S.f("front_open")) return true;
        for (String b : BASE_LOCKED) if (b.equals(id)) return true;
        return false;
    }

    static void unlock(String id) {
        S.unlocked.add(id);
        S.flags.remove("lock:" + id);
    }

    static void lock(String id) {
        S.unlocked.remove(id);
        S.flags.add("lock:" + id);
    }

    static void setDoor(ServerLevel lvl, String id, boolean open) {
        List<BlockPos> l = House.TAGGED.get(id.equals("gate") ? "gate" : "door:" + id);
        if (l == null) return;
        for (BlockPos lpos : l) {
            BlockPos pos = wp(lpos);
            BlockState s = lvl.getBlockState(pos);
            if (s.getBlock() instanceof DoorBlock && s.getValue(DoorBlock.OPEN) != open) lvl.setBlock(pos, s.setValue(DoorBlock.OPEN, open), 10);
            else if (s.getBlock() instanceof TrapDoorBlock && s.getValue(TrapDoorBlock.OPEN) != open) lvl.setBlock(pos, s.setValue(TrapDoorBlock.OPEN, open), 10);
            else if (s.hasProperty(OpenableBlock.OPEN) && s.getValue(OpenableBlock.OPEN) != open) lvl.setBlock(pos, s.setValue(OpenableBlock.OPEN, open), 10);
        }
    }

    static void setDoor(String id, boolean open) {
        ServerPlayer p = player();
        if (p != null) setDoor(p.serverLevel(), id, open);
    }

    static boolean doorOpen(String id) {
        ServerPlayer p = player();
        BlockPos l = House.DOORS.get(id);
        if (p == null || l == null) return false;
        BlockState s = p.serverLevel().getBlockState(wp(l));
        if (s.getBlock() instanceof DoorBlock) return s.getValue(DoorBlock.OPEN);
        if (s.getBlock() instanceof TrapDoorBlock) return s.getValue(TrapDoorBlock.OPEN);
        return false;
    }

    public static boolean isBarred(BlockPos world) { return false; }

    /** doors the story keeps shut: it will not force them (it waits) */
    public static boolean isLockedDoor(BlockPos world) {
        if (!S.active) return false;
        String t = House.HOT.get(local(world));
        if (t == null || !t.startsWith("door:")) return false;
        return locked(t.substring(5));
    }

    private static void resend(ServerPlayer p, BlockPos pos) {
        p.connection.send(new ClientboundBlockUpdatePacket(p.serverLevel(), pos));
        p.connection.send(new ClientboundBlockUpdatePacket(p.serverLevel(), pos.above()));
        p.connection.send(new ClientboundBlockUpdatePacket(p.serverLevel(), pos.below()));
    }

    // ================================================================== interaction
    /** @return true to cancel the vanilla interaction */
    public static boolean interact(ServerPlayer p, BlockPos world) {
        if (cutId != null || dlgId != null || respawnIn >= 0) return true;
        ServerLevel lvl = p.serverLevel();
        BlockState state = lvl.getBlockState(world);
        BlockPos lpos = local(world);
        String tag = House.HOT.get(lpos);
        if (hiddenId != null) return true;
        if (tag == null) {
            // a door that is not part of the story still opens (vanilla); nobody sleeps on this farm
            return state.getBlock() instanceof BedBlock;
        }
        if (tag.startsWith("door:")) {
            String id = tag.substring(5);
            if (Steps.onDoor(p, id)) {
                resend(p, world);
                return true;
            }
            if (locked(id)) {
                snd("fx.lock", 1f, 1f);
                msg("locked");
                resend(p, world);
                return true;
            }
            noiseEvent(lpos, 6);
            if (state.getBlock() instanceof TrapDoorBlock) {
                setDoor(lvl, id, !state.getValue(TrapDoorBlock.OPEN));
                resend(p, world);
                return true;
            }
            return false;
        }
        String posKey = lpos.getX() + "," + lpos.getY() + "," + lpos.getZ();
        if (notYet(tag)) {
            think(S.chapter <= 1 ? "not_yet_lily" : "not_yet");
            return true;
        }
        if (tag.startsWith("pickup:")) {
            String item = tag.substring(7);
            if (S.opened.contains(posKey)) return true;
            S.opened.add(posKey);
            lvl.setBlock(world, Blocks.AIR.defaultBlockState(), 3);
            give(item);
            Steps.onItem(p, item);
            hudDirty = true;
            return true;
        }
        if (tag.startsWith("drawer:")) {
            String content = tag.substring(7);
            if (state.hasProperty(OpenableBlock.OPEN)) {
                if (!state.getValue(OpenableBlock.OPEN)) {
                    lvl.setBlock(world, state.setValue(OpenableBlock.OPEN, true), 3);
                    sndAt(lpos.getX() + 0.5, lpos.getY() + 0.5, lpos.getZ() + 0.5, "fx.drawer", 1f, 0.9f + RND.nextFloat() * 0.2f);
                    noiseEvent(lpos, 4);
                } else if (S.opened.contains(posKey) || content.equals("empty")) {
                    lvl.setBlock(world, state.setValue(OpenableBlock.OPEN, false), 3);
                    sndAt(lpos.getX() + 0.5, lpos.getY() + 0.5, lpos.getZ() + 0.5, "fx.drawer", 0.8f, 0.8f);
                    return true;
                }
            }
            if (S.opened.contains(posKey)) {
                msg("drawer_empty");
                return true;
            }
            if (content.equals("empty")) {
                S.opened.add(posKey);
                msg("drawer_junk" + RND.nextInt(6));
            } else if (content.equals("battery")) {
                S.opened.add(posKey);
                give("battery");
            } else if (content.startsWith("item:")) {
                S.opened.add(posKey);
                give(content.substring(5));
                Steps.onItem(p, content.substring(5));
            } else if (content.startsWith("note:")) {
                note(content.substring(5));
            }
            hudDirty = true;
            return true;
        }
        if (tag.startsWith("note:")) {
            note(tag.substring(5));
            return true;
        }
        if (tag.startsWith("wardrobe:")) {
            for (House.Hide h : House.HIDES) if (h.id.equals(tag.substring(9))) hide(p, h);
            return true;
        }
        Steps.onUse(p, tag, lpos);
        return true;
    }

    /** a chapter's things wait for their chapter: nothing of the next task can be done early */
    static boolean notYet(String tag) {
        int need = switch (tag) {
            case "drawer:item:crowbar", "pickup:gas_can", "toolcab", "pickup:pitchfork" -> 2;
            case "pickup:meat", "coat", "tape" -> 3;
            default -> 0;
        };
        return S.chapter < need;
    }

    /** right click on one of our entities */
    public static boolean interactEntity(ServerPlayer p, Entity e) {
        if (!isStoryPlayer(p) || cutId != null || dlgId != null) return false;
        if (e instanceof TruckEntity) {
            Steps.onTruck(p);
            return true;
        }
        if (e instanceof GuestEntity) {
            Steps.onLily(p);
            return true;
        }
        return false;
    }

    // ================================================================== hiding
    public static House.Hide hiddenIn(ServerPlayer p) {
        if (hiddenId == null || !isStoryPlayer(p)) return null;
        for (House.Hide h : House.HIDES) if (h.id.equals(hiddenId)) return h;
        return null;
    }

    public static boolean seenHiding() { return seenHide; }

    public static int searchDepth() {
        return 2 + Math.min(2, S.v("hides") / 4);
    }

    /** it is breathing at the slats of the place you are in: you held your breath, or you did not */
    public static boolean hideDetected(ServerPlayer p, MimicEntity e) {
        if (seenHide) return true;
        if (S.lightOn) return true; // the beam shows through the gaps
        return breathFailed;
    }

    /** it starts listening at your hiding place: hold your breath */
    public static void atHide(ServerPlayer p, House.Hide h) {
        if (hiddenIn(p) == h) {
            breathFailed = false;
            qte("breath", 76);
        }
    }

    static void hide(ServerPlayer p, House.Hide h) {
        seenHide = hm != null && hm.isAlive() && hm.getState() != MimicEntity.VANISH && hm.sees(p) && hm.distanceTo(p) < 12;
        hiddenId = h.id;
        Vec3 c = worldCenter(h.pos);
        p.teleportTo(p.serverLevel(), c.x, c.y + 0.05, c.z, h.facing.toYRot(), 0);
        p.setInvisible(true);
        p.setDeltaMovement(Vec3.ZERO);
        snd(h.id.contains("hay") ? "fx.hay" : "fx.door_creak", 0.7f, 1.3f);
        Net.toPlayer(p, Msg.of("hide").b("on", true).b("hay", h.id.contains("hay")));
        S.v("hides", S.v("hides") + 1);
        if (S.lightOn) toggleLight(p);
        msg("hide_hint");
        Steps.onHide(p, h.id, true);
    }

    static void unhide(ServerPlayer p) {
        House.Hide h = hiddenIn(p);
        hiddenId = null;
        seenHide = false;
        p.setInvisible(false);
        if (h != null) {
            Vec3 c = worldCenter(h.pos.relative(h.facing, 1));
            p.teleportTo(p.serverLevel(), c.x, c.y + 0.05, c.z, h.facing.toYRot(), 0);
            snd(h.id.contains("hay") ? "fx.hay" : "fx.door_creak", 0.7f, 1.2f);
            noiseEvent(local(p.blockPosition()), 5);
            Steps.onHide(p, h.id, false);
        }
        Net.toPlayer(p, Msg.of("hide").b("on", false));
    }

    // ================================================================== quick time events
    static void qte(String type, int ticks) {
        ServerPlayer p = player();
        if (p == null) return;
        qteType = type;
        qteTicks = ticks + 10;
        Net.toPlayer(p, Msg.of("qte").s("t", type).i("n", ticks));
        snd("fx.qte", 0.9f, 1f);
    }

    private static void onQteResult(String type, boolean ok) {
        if (qteType == null || !qteType.equals(type)) return;
        qteType = null;
        if (type.equals("breath")) {
            breathFailed = !ok;
            if (!ok) snd("fx.gasp", 1f, 1f);
        } else if (type.equals("mash")) {
            Steps.onQteMash(ok);
        } else {
            Steps.onQte(type, ok);
        }
    }

    // ================================================================== the mimic
    static MimicEntity spawnMimic(double x, double y, double z, float yaw) {
        ServerPlayer p = player();
        if (p == null) return null;
        removeMimic();
        MimicEntity e = ModEntities.MIMIC.get().create(p.serverLevel());
        if (e == null) return null;
        Vec3 v = w(x, y, z);
        e.moveTo(v.x, v.y, v.z, yaw, 0);
        e.yBodyRot = yaw;
        e.yHeadRot = yaw;
        e.story = true;
        e.setStateRaw(MimicEntity.SCRIPT);
        p.serverLevel().addFreshEntity(e);
        hm = e;
        relocations = 0;
        return e;
    }

    static void removeMimic() {
        if (hm != null && hm.isAlive()) hm.discard();
        hm = null;
        ServerPlayer p = player();
        if (p != null) {
            AABB box = new AABB(S.ox - 16, S.oy - 12, S.oz - 16, S.ox + 120, S.oy + 40, S.oz + 124);
            for (MimicEntity e : p.serverLevel().getEntitiesOfClass(MimicEntity.class, box)) if (e.story) e.discard();
        }
    }

    static boolean mimicPresent() { return hm != null && hm.isAlive() && hm.getState() != MimicEntity.VANISH; }

    public static void onMimicGone(MimicEntity e) {
        if (e == hm) {
            hm = null;
            Steps.onMimicEvent("gone");
        }
    }

    public static void onMimicEvent(MimicEntity e, String ev) {
        if (!S.active || e != hm) return;
        if (ev.equals("stuck")) {
            relocate(false);
            return;
        }
        if (ev.startsWith("at_hide:")) {
            ServerPlayer p = player();
            for (House.Hide h : House.HIDES) if (p != null && ev.substring(8).equals(h.id)) atHide(p, h);
        }
        Steps.onMimicEvent(ev);
    }

    /** it climbs, it squeezes, it drops from the rafters: it arrives somewhere near, out of sight */
    static boolean relocate(boolean ahead) {
        ServerPlayer p = player();
        if (p == null || hm == null) return false;
        if (relocations++ > 6 && hm.getState() != MimicEntity.HUNT) {
            hm.vanish();
            return false;
        }
        int state = hm.getState();
        double speed = hm.huntSpeed;
        Vec3 spot = findSpot(p, 7, 13, true, ahead);
        if (spot == null) spot = findSpot(p, 6, 16, false, ahead);
        if (spot == null) return false;
        Vec3 l = spot.subtract(S.ox, S.oy, S.oz);
        MimicEntity e = spawnMimic(l.x, l.y, l.z, 0);
        if (e == null) return false;
        e.huntSpeed = speed;
        e.face(p.position());
        e.emerge(state == MimicEntity.SEARCH || state == MimicEntity.SCRIPT ? MimicEntity.HUNT : state);
        return true;
    }

    public static void blink(MimicEntity e, ServerPlayer p) {
        String room = room(p);
        if (!room.isEmpty()) flicker(room, 24);
    }

    static boolean standable(ServerLevel lvl, BlockPos pos) {
        return !lvl.getBlockState(pos.below()).getCollisionShape(lvl, pos.below()).isEmpty()
                && lvl.getBlockState(pos).getCollisionShape(lvl, pos).isEmpty()
                && lvl.getBlockState(pos.above()).getCollisionShape(lvl, pos.above()).isEmpty();
    }

    static boolean playerCanSee(ServerPlayer p, Vec3 point) {
        Vec3 eye = p.getEyePosition();
        Vec3 t = point.add(0, 1.6, 0);
        Vec3 dir = t.subtract(eye).normalize();
        if (dir.dot(p.getViewVector(1f)) < 0.25) return false;
        return p.serverLevel().clip(new ClipContext(eye, t, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS;
    }

    static Vec3 findSpot(ServerPlayer p, double minD, double maxD, boolean unseen, boolean ahead) {
        ServerLevel lvl = p.serverLevel();
        Vec3 look = p.getViewVector(1f);
        Vec3 vel = new Vec3(look.x, 0, look.z).normalize();
        Vec3 best = null;
        double bestScore = -1e9;
        String myRoom = room(p);
        for (int i = 0; i < 60; i++) {
            double a = RND.nextDouble() * Math.PI * 2;
            double d = minD + RND.nextDouble() * (maxD - minD);
            double x = p.getX() + Math.cos(a) * d, z = p.getZ() + Math.sin(a) * d;
            for (int dy = 1; dy >= -1; dy--) {
                BlockPos pos = BlockPos.containing(x, p.getY() + dy + 0.1, z);
                if (!standable(lvl, pos)) continue;
                Vec3 lpv = new Vec3(pos.getX() + 0.5 - S.ox, pos.getY() - S.oy, pos.getZ() + 0.5 - S.oz);
                String r = House.roomAt(lpv.x, lpv.y, lpv.z);
                if (r.isEmpty() || r.equals("ROAD")) continue;
                if (r.equals("SILO") && !S.f("silo_open")) continue;
                Vec3 v = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                boolean seen = playerCanSee(p, v);
                if (unseen && seen) continue;
                double score = RND.nextDouble() * 2;
                Vec3 dir = v.subtract(p.position()).normalize();
                double front = dir.x * vel.x + dir.z * vel.z;
                score += ahead ? front * 3 : -front * 2;
                if (r.equals(myRoom)) score -= 1.5;
                if (score > bestScore) {
                    bestScore = score;
                    best = v;
                }
                break;
            }
        }
        return best;
    }

    // ================================================================== Lily and the truck
    static GuestEntity spawnLily(double x, double y, double z, float yaw) {
        ServerPlayer p = player();
        if (p == null) return null;
        if (lily != null && lily.isAlive()) lily.discard();
        GuestEntity g = ModEntities.LILY.get().create(p.serverLevel());
        if (g == null) return null;
        Vec3 v = w(x, y, z);
        g.moveTo(v.x, v.y, v.z, yaw, 0);
        g.story = true;
        g.homeYaw = yaw;
        p.serverLevel().addFreshEntity(g);
        lily = g;
        return g;
    }

    static TruckEntity spawnTruck() {
        ServerPlayer p = player();
        if (p == null) return null;
        if (truck != null && truck.isAlive()) return truck;
        AABB box = new AABB(S.ox - 16, S.oy - 12, S.oz - 16, S.ox + 120, S.oy + 40, S.oz + 140);
        for (TruckEntity t : p.serverLevel().getEntitiesOfClass(TruckEntity.class, box)) t.discard();
        TruckEntity t = ModEntities.TRUCK.get().create(p.serverLevel());
        if (t == null) return null;
        Vec3 v = w(46.5, Steps.G1, 46.5);
        t.moveTo(v.x, v.y, v.z, 0, 0);
        t.story = true;
        p.serverLevel().addFreshEntity(t);
        truck = t;
        return t;
    }

    // ================================================================== caught / checkpoints
    static void checkpoint(String step, double x, double y, double z, float yaw) {
        S.cpStep = step;
        S.cpX = x;
        S.cpY = y;
        S.cpZ = z;
        S.cpYaw = yaw;
    }

    public static void caught(ServerPlayer p, MimicEntity e) {
        if (!isStoryPlayer(p) || respawnIn >= 0 || cutId != null) return;
        respawnIn = 70;
        S.caught++;
        fx(p, "kill", 64);
        snd("mm.kill", 1f, 1f);
        if (e != null) {
            e.act("kill", 60, null);
            e.face(p.position());
        }
        if (dlgId != null) endDialogueSilently();
        p.setInvulnerable(true);
        hudDirty = true;
    }

    private static void respawn(ServerPlayer p) {
        stamina = 100;
        tired = false;
        removeMimic();
        timers.clear();
        voiceBusy = 0;
        qteType = null;
        if (hiddenId != null) {
            hiddenId = null;
            p.setInvisible(false);
            Net.toPlayer(p, Msg.of("hide").b("on", false));
        }
        Vec3 v = w(S.cpX, S.cpY, S.cpZ);
        p.teleportTo(p.serverLevel(), v.x, v.y + 0.05, v.z, S.cpYaw, 0);
        p.setHealth(p.getMaxHealth());
        fx(p, "fadein", 50);
        think("caught" + (S.caught % 4));
        hauntCd = 500;
        if (!S.cpStep.isEmpty()) Steps.enter(S.cpStep);
    }

    // ================================================================== dialogue engine
    static void startDialogue(String id, String firstNode) {
        dlgId = id;
        sendNode(firstNode);
    }

    private static boolean meets(String need) {
        if (need == null) return true;
        if (need.startsWith("!")) return !meets(need.substring(1));
        for (String part : need.split("\\+")) {
            if (ModItems.STORY.containsKey(part)) {
                if (!has(part)) return false;
            } else if (!S.f(part)) return false;
        }
        return true;
    }

    private static void sendNode(String nodeId) {
        ServerPlayer p = player();
        Dialogues.Node n = Dialogues.NODES.get(nodeId);
        if (p == null || n == null) {
            finishDialogue("");
            return;
        }
        dlgNode = nodeId;
        dlgAuto = n.auto > 0 ? Math.max(n.auto, n.sound != null ? voiceTicks(n.sound) + 12 : 0) : -1;
        Msg m = Msg.of("dlg").s("node", nodeId).s("speaker", n.speaker).s("key", "dlg." + nodeId).i("time", n.time).b("big", n.big)
                .s("pname", p.getGameProfile().getName()).i("auto", n.auto);
        int count = 0;
        for (int i = 0; i < n.choices.size(); i++) {
            Dialogues.Choice c = n.choices.get(i);
            if (!meets(c.need)) continue;
            m.s("c" + count, "dlg." + nodeId + "." + i).i("ci" + count, i);
            count++;
        }
        m.i("count", count);
        Net.toPlayer(p, m);
        if (n.sound != null) {
            int wait = Math.max(0, voiceBusy - age);
            snd(n.sound, 1f, 1f);
            if (dlgAuto > 0) dlgAuto += wait;
        }
        if (n.big) snd("fx.choice", 1f, 1f);
        Steps.onNode(nodeId);
    }

    private static void onChoice(ServerPlayer p, String nodeId, int idx) {
        if (dlgId == null || !nodeId.equals(dlgNode)) return;
        Dialogues.Node n = Dialogues.NODES.get(nodeId);
        if (n == null) return;
        String next;
        if (n.choices.isEmpty()) {
            next = n.next;
        } else if (idx < 0 || idx >= n.choices.size()) {
            next = n.silence != null ? n.silence : n.next;
        } else {
            Dialogues.Choice c = n.choices.get(idx);
            if (!meets(c.need)) return;
            if (c.say != null) {
                // you say it out loud: in your own voice, where it can hear - and the answer waits until you have finished
                final String line = c.say;
                int wait = voiceSlot(line);
                later(Math.max(0, wait), () -> {
                    ServerPlayer pp = player();
                    if (pp == null) return;
                    logVoice(line);
                    pp.serverLevel().playSound(null, pp.getX(), pp.getY() + 1.6, pp.getZ(), ModSounds.get(line), SoundSource.PLAYERS, 1f, 1f);
                    say("sub." + line, "you", voiceTicks(line) + 6);
                });
            }
            if (c.anger > 0) noiseEvent(local(p.blockPosition()), c.anger);
            if (c.learn != null) S.flags.add("learned:" + c.learn);
            if (c.flag != null) S.flags.add(c.flag);
            next = c.next;
            if (c.say != null) {
                final String nx = next;
                dlgNode = null; // nothing else can be answered while you speak
                dlgAuto = -1;
                later(Math.max(0, voiceBusy - age) + 2, () -> {
                    if (dlgId == null) return;
                    if (nx.startsWith("end")) finishDialogue(nx.length() > 4 ? nx.substring(4) : "");
                    else sendNode(nx);
                });
                return;
            }
        }
        if (next.startsWith("end")) finishDialogue(next.length() > 4 ? next.substring(4) : "");
        else sendNode(next);
    }

    private static void finishDialogue(String result) {
        String id = dlgId;
        dlgId = null;
        dlgNode = null;
        dlgAuto = -1;
        ServerPlayer p = player();
        if (p != null) Net.toPlayer(p, Msg.of("dlg_end"));
        hudDirty = true;
        if (id != null) Steps.onDialogueEnd(id, result, 0);
        S.save(server);
    }

    private static void endDialogueSilently() {
        dlgId = null;
        dlgNode = null;
        dlgAuto = -1;
        ServerPlayer p = player();
        if (p != null) Net.toPlayer(p, Msg.of("dlg_end"));
    }

    // ================================================================== cutscene engine
    static void cutscene(String id) {
        ServerPlayer p = player();
        if (p == null) return;
        if (hiddenId != null) unhide(p);
        clearLights();
        cutId = id;
        cutT = 0;
        p.setInvulnerable(true);
        Net.toPlayer(p, Msg.of("cut").s("id", id).i("ox", S.ox).i("oy", S.oy).i("oz", S.oz).s("pname", p.getGameProfile().getName()));
    }

    private static void endCutscene() {
        String id = cutId;
        cutId = null;
        ServerPlayer p = player();
        if (p != null) Net.toPlayer(p, Msg.of("cut_end"));
        if (id != null) Steps.onCutEnd(id);
        S.save(server);
    }

    // ================================================================== client messages
    public static void onClientMsg(ServerPlayer p, Msg m) {
        if (!isStoryPlayer(p)) return;
        switch (m.type) {
            case "light" -> {
                if (cutId == null && respawnIn < 0) toggleLight(p);
            }
            case "dlg_choice" -> onChoice(p, m.s("node"), m.i("i"));
            case "cut_done" -> {
                if (cutId != null && cutId.equals(m.s("id"))) {
                    Cutscenes.Cut c = Cutscenes.ALL.get(cutId);
                    skipping = true;
                    try {
                        if (c != null) while (cutT < c.length) Steps.cutCue(cutId, ++cutT);
                    } finally {
                        skipping = false;
                    }
                    endCutscene();
                }
            }
            case "note_closed" -> Steps.onNoteClosed(p, m.s("id"));
            case "unhide" -> {
                if (hiddenId != null && (hm == null || hm.getState() != MimicEntity.SEARCH || qteType == null)) unhide(p);
            }
            case "qte_res" -> onQteResult(m.s("t"), m.b("ok"));
            case "credits_done" -> Steps.onCreditsDone(p);
            default -> {}
        }
    }

    private static void sendHud(ServerPlayer p) {
        String amb = "outside";
        Vec3 l = lp(p);
        String r = House.roomAt(l.x, l.y, l.z);
        if (r.startsWith("H_") || r.startsWith("U_")) amb = "house";
        else if (r.equals("CELLAR")) amb = "cellar";
        else if (r.equals("BARN") || r.equals("STALLS") || r.equals("LOFT") || r.equals("WORKSHOP") || r.equals("SLAUGHTER")) amb = "barn";
        else if (r.equals("SHED")) amb = "shed";
        else if (r.equals("SILO")) amb = "silo";
        else if (r.equals("FIELD")) amb = "field";
        boolean chase = hm != null && hm.isAlive() && (hm.getState() == MimicEntity.HUNT || (hm.getState() == MimicEntity.SEARCH && hiddenId != null));
        float fear = 0;
        if (mimicPresent()) fear = (float) Math.max(0, 1 - hm.distanceTo(p) / 18.0);
        if (chase) fear = Math.max(fear, 0.6f);
        Msg m = Msg.of("hud").b("active", S.active).s("obj", S.objective).s("arg", S.objectiveArg)
                .f("bat", S.battery).b("light", S.lightOn).b("hasLight", S.hasLight).i("notes", S.notes.size()).i("notesMax", Notes.COUNT)
                .i("ox", S.ox).i("oy", S.oy).i("oz", S.oz).s("amb", amb).b("chase", chase).f("fear", fear)
                .b("hidden", hiddenId != null).i("spare", countItem(p, "battery")).i("chapter", S.chapter)
                .f("disturb", S.step.equals("c2_barn") ? S.v("disturb") / 100f : -1f).f("noise", (float) footstepRadius(p))
                .b("cover", inCover(p)).b("dawn", S.f("dawn"));
        Net.toPlayer(p, m);
        hudDirty = false;
    }

    private static int countItem(ServerPlayer p, String id) {
        Item it = ModItems.story(id);
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.is(it)) n += st.getCount();
        }
        return n;
    }

    // ================================================================== tick
    public static void serverTick(MinecraftServer srv) {
        if (!S.active) return;
        server = srv;
        ServerPlayer p = player();
        if (p == null || !p.isAlive()) return;
        age++;
        noiseAge++;
        if (S.v("resume") == 1) {
            S.vars.remove("resume");
            House.ensureIndexed();
            refreshLamps(null);
            Steps.resume();
            hudDirty = true;
        }
        if (!timers.isEmpty()) {
            List<Runnable> due = new ArrayList<>();
            for (Iterator<Object[]> it = timers.iterator(); it.hasNext(); ) {
                Object[] t = it.next();
                if ((Integer) t[0] <= age) {
                    due.add((Runnable) t[1]);
                    it.remove();
                }
            }
            for (Runnable r : due) r.run();
        }
        if (hm != null && !hm.isAlive()) hm = null;
        if (lily != null && !lily.isAlive()) lily = null;
        if (truck != null && !truck.isAlive()) truck = null;
        if (age % 40 == 0) {
            p.getFoodData().setFoodLevel(20);
            if (p.getHealth() < p.getMaxHealth()) p.setHealth(p.getMaxHealth());
            long dt = p.serverLevel().getDayTime() % 24000;
            if (!S.f("dawn") && (dt < 15000 || dt > 21000)) p.serverLevel().setDayTime(18000);
        }
        if (qteType != null && --qteTicks <= 0) onQteResult(qteType, false);
        if (respawnIn >= 0) {
            if (--respawnIn == 0) {
                respawnIn = -1;
                respawn(p);
            }
            return;
        }
        if (cutId != null) {
            cutT++;
            Steps.cutCue(cutId, cutT);
            Cutscenes.Cut c = Cutscenes.ALL.get(cutId);
            if (c == null || cutT > c.length + 30) endCutscene();
            if (hudDirty) sendHud(p);
            return;
        }
        tickBody(p);
        // the flashlight
        if (S.lightOn) {
            S.battery -= 100f / (20f * 300f);
            if (S.battery <= 0) {
                if (take("battery")) {
                    S.battery = 100;
                    snd("fx.light_on", 0.8f, 1.2f);
                    msg("battery_swap");
                } else {
                    S.battery = 0;
                    S.lightOn = false;
                    snd("fx.light_off", 1f, 0.8f);
                    msg("light_dead");
                    clearLights();
                }
                hudDirty = true;
            } else if (S.battery < 12 && age % 50 == 0) snd("fx.light_flicker", 0.6f, 1f);
        }
        if (age % 2 == 0) updateLights(p);
        // hiding: hold the player still
        House.Hide h = hiddenIn(p);
        if (h != null) {
            Vec3 c = worldCenter(h.pos);
            if (p.position().distanceToSqr(c.x, p.getY(), c.z) > 0.2) p.teleportTo(p.serverLevel(), c.x, c.y + 0.05, c.z, p.getYRot(), p.getXRot());
        }
        // dialogue lines that move on by themselves
        if (dlgId != null && dlgAuto > 0 && --dlgAuto == 0 && dlgNode != null) onChoice(p, dlgNode, -2);
        if (dlgId == null) {
            Steps.tick(p);
            if (S.active) haunt(p);
        }
        if (!S.active) return;
        if (--thunderCd <= 0) {
            thunderCd = 600 + RND.nextInt(900);
            boolean outside = sndOutside(p);
            snd("fx.thunder", outside ? 1f : 0.5f, 0.9f + RND.nextFloat() * 0.2f);
            if (outside) fx(p, "flash", 6);
        }
        if (S.cpStep.isEmpty() && !mimicPresent() && p.onGround() && ++safeTicks > 100) {
            safeTicks = 0;
            Vec3 l = lp(p);
            if (!House.roomAt(l.x, l.y, l.z).isEmpty() && !House.roomAt(l.x, l.y, l.z).equals("ROAD")) {
                S.cpX = l.x;
                S.cpY = l.y;
                S.cpZ = l.z;
                S.cpYaw = p.getYRot();
            }
        }
        if (hudDirty || age % 10 == 0) sendHud(p);
        if (age % 600 == 0) S.save(srv);
    }

    private static boolean sndOutside(ServerPlayer p) {
        String r = room(p);
        return r.equals("YARD") || r.equals("ROAD") || r.equals("FIELD") || r.equals("PORCH");
    }

    // ================================================================== the free roam: the farm at night
    private static final String[] FAR_SOUNDS = {"mm.howl", "fx.creak", "fx.crow", "fx.cow", "mm.click", "fx.chain", "mm.step", "fx.creak"};
    private static int lastEvent = -1;

    private static void haunt(ServerPlayer p) {
        if (S.haunt <= 0 || hiddenId != null) return;
        if (mimicPresent()) {
            if (hm.getState() == MimicEntity.WATCH && hm.stateTicks > 400) hm.vanish();
            if (hm.getState() == MimicEntity.STALK && hm.stateTicks > 1400) hm.vanish();
            return;
        }
        if (--hauntCd > 0) return;
        int lvl = S.haunt;
        hauntCd = (lvl == 1 ? 520 : 400) + RND.nextInt(420);
        int roll = RND.nextInt(100);
        int ev = lvl == 1 ? (roll < 55 ? 0 : roll < 85 ? 1 : 2) : (roll < 35 ? 0 : roll < 60 ? 1 : roll < 82 ? 2 : 3);
        if (ev == lastEvent && ev != 0) ev = 0;
        lastEvent = ev;
        switch (ev) {
            case 0 -> hauntSound(p);
            case 1 -> { // a familiar voice, somewhere out in the dark
                double a = RND.nextDouble() * Math.PI * 2;
                Vec3 v = lp(p).add(Math.cos(a) * 14, 1, Math.sin(a) * 14);
                voiceAt(v.x, v.y, v.z, Steps.bait(), "it");
            }
            case 2 -> { // it is simply there, far away, watching - and gone when the light finds it
                Vec3 spot = findSpot(p, 16, 26, true, true);
                if (spot == null) { hauntSound(p); return; }
                Vec3 l = spot.subtract(S.ox, S.oy, S.oz);
                MimicEntity e = spawnMimic(l.x, l.y, l.z, 0);
                if (e == null) return;
                e.face(p.position());
                e.stareLimit = 30;
                e.setState(MimicEntity.WATCH);
            }
            default -> { // the stalker: it follows in the dark and comes when you stop listening
                Vec3 spot = findSpot(p, 14, 22, true, false);
                if (spot == null) { hauntSound(p); return; }
                Vec3 l = spot.subtract(S.ox, S.oy, S.oz);
                MimicEntity e = spawnMimic(l.x, l.y, l.z, 0);
                if (e == null) return;
                e.face(p.position());
                e.lethal = true;
                e.stareLimit = 40;
                e.setState(MimicEntity.STALK);
            }
        }
    }

    private static void hauntSound(ServerPlayer p) {
        double a = RND.nextDouble() * Math.PI * 2;
        double d = 9 + RND.nextDouble() * 12;
        Vec3 v = p.position().add(Math.cos(a) * d, 1, Math.sin(a) * d);
        p.serverLevel().playSound(null, v.x, v.y, v.z, ModSounds.get(FAR_SOUNDS[RND.nextInt(FAR_SOUNDS.length)]), SoundSource.AMBIENT, 1.2f, 0.85f + RND.nextFloat() * 0.25f);
    }

    // ================================================================== debug
    public static void test(ServerPlayer p, String cmd) {
        if (cmd.startsWith("use:")) {
            BlockPos l = House.first(cmd.substring(4));
            if (l == null) MimicMod.LOG.error("[AUTOTEST] no hotspot {}", cmd);
            else interact(p, wp(l));
            return;
        }
        String[] a = cmd.split(":");
        switch (a[0]) {
            case "light" -> {
                S.hasLight = true;
                S.battery = 100;
                if (S.lightOn != a[1].equals("on")) toggleLight(p);
            }
            case "tp" -> {
                String[] c = a[1].split(",");
                Vec3 v = w(Double.parseDouble(c[0]), Double.parseDouble(c[1]), Double.parseDouble(c[2]));
                p.teleportTo(p.serverLevel(), v.x, v.y, v.z, Float.parseFloat(c[3]), Float.parseFloat(c[4]));
            }
            case "mm" -> {
                String[] c = a[1].split(",");
                MimicEntity e = spawnMimic(Double.parseDouble(c[0]), Double.parseDouble(c[1]), Double.parseDouble(c[2]), Float.parseFloat(c[3]));
                if (e != null) {
                    e.scriptStop();
                    if (c.length > 4) e.setState(Integer.parseInt(c[4]));
                }
            }
            case "act" -> { if (hm != null) hm.act(a[1], 60, null); }
            case "flag" -> { if (hm != null) hm.setFlag(Integer.parseInt(a[1]), a[2].equals("1")); }
            case "move" -> {
                String[] c = a[1].split(",");
                if (hm != null) hm.scriptMove(w(Double.parseDouble(c[0]), Double.parseDouble(c[1]), Double.parseDouble(c[2])), Double.parseDouble(c[3]));
            }
            case "hunt" -> { if (hm != null) hm.beginHunt(); }
            case "sleep" -> { if (hm != null) hm.sleep(); }
            case "step" -> debugStep(p, a[1]);
            case "dlg" -> startDialogue(a[1], a[2]);
            case "note" -> note(a[1]);
            case "haunt" -> { S.haunt = Integer.parseInt(a[1]); hauntCd = 5; }
            case "hide" -> { for (House.Hide h : House.HIDES) if (h.id.equals(a[1])) hide(p, h); }
            case "fx" -> fx(p, a[1], Integer.parseInt(a[2]));
            case "kill" -> removeMimic();
            case "lamp" -> {
                String[] c = a[1].split(",");
                p.serverLevel().setBlock(wp(new BlockPos(Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2]))),
                        Integer.parseInt(c[3]) == 0 ? Blocks.AIR.defaultBlockState() : Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, Integer.parseInt(c[3])), 3);
            }
            case "give" -> debugGiveAll(p);
            case "item" -> give(a[1]);
            case "nv" -> p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 20000, 0, false, false));
            case "hold" -> p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.story(a[1])));
            case "gm" -> p.setGameMode(a[1].equals("spec") ? GameType.SPECTATOR : GameType.ADVENTURE);
            case "obj" -> obj(a.length > 1 ? a[1] : "");
            case "cut" -> cutscene(a[1]);
            case "audit" -> HouseAudit.run(p.serverLevel(), origin());
            case "open" -> { unlock(a[1]); setDoor(a[1], true); }
            case "speedcap" -> MimicEntity.speedCap = Double.parseDouble(a[1]);
            case "sflag" -> S.flags.add(a[1]);
            case "lily" -> spawnLily(Double.parseDouble(a[1].split(",")[0]), Double.parseDouble(a[1].split(",")[1]), Double.parseDouble(a[1].split(",")[2]), 0);
            case "truck" -> spawnTruck();
            case "time" -> p.serverLevel().setDayTime(Long.parseLong(a[1]));
            case "qte" -> onQteResult(a[1], a[2].equals("1"));
            case "advance" -> {
                if (dlgId != null && dlgNode != null) {
                    Dialogues.Node n = Dialogues.NODES.get(dlgNode);
                    onChoice(p, dlgNode, n == null || n.choices.isEmpty() ? -2 : Integer.parseInt(a[1]));
                }
            }
            case "unhide" -> { if (hiddenId != null) unhide(p); }
            case "use_item" -> useItem(p, a[1]);
            case "truck_use" -> Steps.onTruck(p);
            default -> MimicMod.LOG.warn("unknown test command {}", cmd);
        }
    }

    public static void debugStep(ServerPlayer p, String id) {
        if (!isStoryPlayer(p)) return;
        removeMimic();
        timers.clear();
        dlgId = null;
        cutId = null;
        qteType = null;
        Net.toPlayer(p, Msg.of("dlg_end"));
        Net.toPlayer(p, Msg.of("cut_end"));
        Steps.debugJump(p, id);
    }

    public static void debugGiveAll(ServerPlayer p) {
        if (!isStoryPlayer(p)) return;
        for (String id : ModItems.STORY_IDS) if (!has(id)) give(id);
        S.hasLight = true;
        hudDirty = true;
    }

    public static void debugHouse(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        BlockPos pp = p.blockPosition();
        House.build(lvl, new BlockPos(pp.getX() - 50, pp.getY() - 1, pp.getZ() - 102));
    }
}
