package com.krolasyon.hatman.story;

import com.krolasyon.hatman.HatmanMod;
import com.krolasyon.hatman.block.ModeBlock;
import com.krolasyon.hatman.block.OpenableBlock;
import com.krolasyon.hatman.entity.GuestEntity;
import com.krolasyon.hatman.entity.HatmanEntity;
import com.krolasyon.hatman.net.Msg;
import com.krolasyon.hatman.net.Net;
import com.krolasyon.hatman.registry.ModEntities;
import com.krolasyon.hatman.registry.ModItems;
import com.krolasyon.hatman.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** The story director: world plumbing, interaction, the dialogue and cutscene engines and the free-roam haunting. */
public final class Story {
    public static StoryState S = new StoryState();
    static MinecraftServer server;
    static final Random RND = new Random();
    static HatmanEntity hm;
    static GuestEntity ruth;
    static final List<GuestEntity> guests = new ArrayList<>();
    private static final List<Object[]> timers = new ArrayList<>();
    private static boolean hudDirty = true;
    private static int age = 0;
    // dialogue
    static String dlgId = null;
    private static String dlgNode = null;
    private static float dlgAngerStart = 0;
    // cutscene
    static String cutId = null;
    static int cutT = 0;
    // hiding
    private static String hiddenId = null;
    private static boolean seenHide = false;
    // caught
    private static int respawnIn = -1;
    // haunting
    static int hauntCd = 400;
    private static int relocations = 0;
    private static int thunderCd = 300;
    private static int safeTicks = 0;

    private Story() {}

    // ================================================================== lifecycle
    public static void load(MinecraftServer s) {
        server = s;
        S = StoryState.load(s);
        House.ensureIndexed();
        hm = null;
        ruth = null;
        guests.clear();
        timers.clear();
        dlgId = null;
        cutId = null;
        hiddenId = null;
        respawnIn = -1;
        if (S.active) S.vars.put("resume", 1);
    }

    public static void unload(MinecraftServer s) {
        if (S.active) S.save(s);
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

    public static boolean nvOn(ServerPlayer p) { return isStoryPlayer(p) && S.nvOn; }

    public static String status() {
        if (!S.active) return "The Hatman: no story running. /hatman start";
        return "The Hatman: chapter " + S.chapter + ", step " + S.step + ", notes " + S.notes.size() + "/" + Notes.COUNT
                + ", anger " + (int) S.anger + ", claim " + (int) S.claim + ", caught " + S.caught;
    }

    public static void start(ServerPlayer p) {
        if (S.active) {
            ServerPlayer cur = player();
            if (cur != null && cur != p) {
                p.sendSystemMessage(Component.translatable("msg.hatman.busy"));
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
        S.ox = pp.getX() - 22;
        S.oy = oy;
        S.oz = pp.getZ() - 56;
        S.prevMode = p.gameMode.getGameModeForPlayer().getName();
        GameRules gr = lvl.getGameRules();
        S.prevDaylight = gr.getBoolean(GameRules.RULE_DAYLIGHT);
        S.prevWeather = gr.getBoolean(GameRules.RULE_WEATHER_CYCLE);
        S.prevMobs = gr.getBoolean(GameRules.RULE_DOMOBSPAWNING);
        S.prevFire = gr.getBoolean(GameRules.RULE_DOFIRETICK);
        S.prevTime = lvl.getDayTime();
        applyWorldRules(lvl, true);
        p.sendSystemMessage(Component.translatable("msg.hatman.building"));
        long t0 = System.currentTimeMillis();
        clearEntities(lvl);
        House.build(lvl, origin());
        HatmanMod.LOG.info("Marlowe House built in {} ms at {}", System.currentTimeMillis() - t0, origin());
        p.setGameMode(GameType.ADVENTURE);
        spawnGuests(lvl);
        Steps.enter("intro");
        S.save(server);
    }

    public static void stop(ServerPlayer p, boolean announce) {
        if (!S.active) {
            if (announce) p.sendSystemMessage(Component.translatable("msg.hatman.not_running"));
            return;
        }
        ServerPlayer sp = player();
        ServerLevel lvl = sp != null ? sp.serverLevel() : p.serverLevel();
        applyWorldRules(lvl, false);
        removeHatman();
        if (sp != null) {
            if (hiddenId != null) unhide(sp);
            sp.setInvulnerable(false);
            sp.setInvisible(false);
            GameType gt = GameType.byName(S.prevMode, GameType.SURVIVAL);
            sp.setGameMode(gt);
            Net.toPlayer(sp, Msg.of("end"));
        }
        // leave the house standing, but let people out of it
        for (String id : House.DOORS.keySet()) S.unlocked.add(id);
        S.flags.remove("sealed");
        setDoor(lvl, "gate", true);
        S.active = false;
        dlgId = null;
        cutId = null;
        hiddenId = null;
        timers.clear();
        S.save(server);
        if (announce) p.sendSystemMessage(Component.translatable("msg.hatman.stopped"));
    }

    private static void applyWorldRules(ServerLevel lvl, boolean story) {
        GameRules gr = lvl.getGameRules();
        gr.getRule(GameRules.RULE_DAYLIGHT).set(!story && S.prevDaylight, server);
        gr.getRule(GameRules.RULE_WEATHER_CYCLE).set(!story && S.prevWeather, server);
        gr.getRule(GameRules.RULE_DOMOBSPAWNING).set(!story && S.prevMobs, server);
        gr.getRule(GameRules.RULE_DOFIRETICK).set(!story && S.prevFire, server);
        if (story) {
            lvl.setDayTime(18000);
            lvl.setWeatherParameters(0, 2000000, true, true);
        } else {
            lvl.setWeatherParameters(12000, 0, false, false);
        }
    }

    private static void clearEntities(ServerLevel lvl) {
        AABB box = new AABB(S.ox - 16, S.oy - 12, S.oz - 16, S.ox + 62, S.oy + 40, S.oz + 68);
        for (HatmanEntity e : lvl.getEntitiesOfClass(HatmanEntity.class, box)) e.discard();
        for (GuestEntity e : lvl.getEntitiesOfClass(GuestEntity.class, box)) e.discard();
        for (net.minecraft.world.entity.Mob m : lvl.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, box)) m.discard();
        for (net.minecraft.world.entity.item.ItemEntity m : lvl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) m.discard();
    }

    // ================================================================== coordinates
    public static BlockPos origin() { return new BlockPos(S.ox, S.oy, S.oz); }

    static BlockPos wp(int x, int y, int z) { return new BlockPos(S.ox + x, S.oy + y, S.oz + z); }

    static BlockPos wp(BlockPos local) { return local.offset(S.ox, S.oy, S.oz); }

    static Vec3 w(double x, double y, double z) { return new Vec3(S.ox + x, S.oy + y, S.oz + z); }

    public static Vec3 worldCenter(BlockPos local) { return new Vec3(S.ox + local.getX() + 0.5, S.oy + local.getY(), S.oz + local.getZ() + 0.5); }

    static BlockPos local(BlockPos world) { return world.offset(-S.ox, -S.oy, -S.oz); }

    static Vec3 lp(ServerPlayer p) { return new Vec3(p.getX() - S.ox, p.getY() - S.oy, p.getZ() - S.oz); }

    static String room(ServerPlayer p) {
        Vec3 l = lp(p);
        return House.roomAt(l.x, l.y, l.z);
    }

    static boolean in(ServerPlayer p, String room) { return room(p).equals(room); }

    static double distLocal(ServerPlayer p, double x, double y, double z) { return lp(p).distanceTo(new Vec3(x, y, z)); }

    public static String hotspot(BlockPos world) {
        if (!S.active) return null;
        return House.HOT.get(local(world));
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
    static void think(String key) { say("line." + key, "you", 90); }

    static void msg(String key, Object... args) {
        ServerPlayer p = player();
        if (p != null) p.displayClientMessage(Component.translatable("msg.hatman." + key, args), true);
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

    static void snd(String name, float vol, float pitch) {
        ServerPlayer p = player();
        if (p != null) p.playNotifySound(ModSounds.get(name), SoundSource.AMBIENT, vol, pitch);
    }

    static void sndAt(double x, double y, double z, String name, float vol, float pitch) {
        ServerPlayer p = player();
        if (p == null) return;
        Vec3 v = w(x, y, z);
        p.serverLevel().playSound(null, v.x, v.y, v.z, ModSounds.get(name), SoundSource.AMBIENT, vol, pitch);
    }

    static void later(int ticks, Runnable r) { timers.add(new Object[]{age + ticks, r}); }

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
        p.displayClientMessage(Component.translatable("msg.hatman.picked", name), true);
        snd("fx.pickup", 0.9f, 1f);
    }

    static void note(String id) {
        ServerPlayer p = player();
        if (p == null) return;
        boolean fresh = S.notes.add(id);
        snd("fx.paper", 0.9f, 1f);
        Net.toPlayer(p, Msg.of("note").s("id", id).b("fresh", fresh));
        hudDirty = true;
    }

    // ================================================================== lights
    private static int lampMode(House.Lamp l) {
        if (!S.power) return 0;
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

    static void power(boolean on) {
        S.power = on;
        refreshLamps(null);
        hudDirty = true;
    }

    /** kill the lights in a room (prefix) for a while */
    static void flicker(String roomPrefix, int ticks) {
        ServerPlayer p = player();
        if (p == null || !S.power) return;
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
        snd("fx.switch", 0.6f, 0.8f);
    }

    static void dark(String room, boolean dark) {
        if (dark) S.flags.add("dark:" + room);
        else S.flags.remove("dark:" + room);
        refreshLamps(room.equals("all") ? null : room);
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
    private static final String[] BASE_LOCKED = {"cellar", "workshop", "stairgate", "master", "library", "servant", "hatch"};

    static boolean locked(String id) {
        if (S.unlocked.contains(id)) return false;
        if (S.f("lock:" + id)) return true;
        if ((id.equals("front") || id.equals("gate")) && S.f("sealed")) return true;
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
        List<BlockPos> l = House.TAGGED.get("door:" + id);
        if (l == null) return;
        for (BlockPos lpos : l) {
            BlockPos pos = wp(lpos);
            BlockState s = lvl.getBlockState(pos);
            if (s.getBlock() instanceof DoorBlock && s.getValue(DoorBlock.OPEN) != open) lvl.setBlock(pos, s.setValue(DoorBlock.OPEN, open), 10);
            else if (s.getBlock() instanceof FenceGateBlock && s.getValue(FenceGateBlock.OPEN) != open) lvl.setBlock(pos, s.setValue(FenceGateBlock.OPEN, open), 10);
            else if (s.getBlock() instanceof TrapDoorBlock && s.getValue(TrapDoorBlock.OPEN) != open) lvl.setBlock(pos, s.setValue(TrapDoorBlock.OPEN, open), 10);
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
        if (s.getBlock() instanceof FenceGateBlock) return s.getValue(FenceGateBlock.OPEN);
        if (s.getBlock() instanceof TrapDoorBlock) return s.getValue(TrapDoorBlock.OPEN);
        return false;
    }

    public static boolean isBarred(BlockPos world) {
        if (!S.active) return false;
        String t = House.HOT.get(local(world));
        if (t == null || !t.startsWith("door:")) return false;
        return S.f("bar:" + t.substring(5));
    }

    /** doors the story keeps shut: he is a guest too, and will not force them */
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
            // nobody sleeps in this house
            return state.getBlock() instanceof BedBlock;
        }
        if (tag.startsWith("door:")) {
            String id = tag.substring(5);
            if (Steps.onDoor(p, id)) {
                resend(p, world);
                return true;
            }
            if (locked(id)) {
                String key = id.equals("cellar") ? "cellar_key" : id.equals("workshop") ? "workshop_key" : id.equals("stairgate") ? "stair_key" : id.equals("hatch") ? "attic_key" : null;
                if (key != null && has(key)) {
                    unlock(id);
                    snd("fx.unlock", 1f, 1f);
                    msg("unlocked");
                    if (id.equals("stairgate")) {
                        setDoor(lvl, id, true);
                        resend(p, world);
                        return true;
                    }
                    return false;
                }
                snd("fx.lock", 1f, 1f);
                msg(id.equals("front") || id.equals("gate") ? "sealed" : id.equals("master") ? "locked_inside" : key != null ? "locked_key" : "locked");
                resend(p, world);
                return true;
            }
            if (id.equals("stairgate") || id.equals("gate")) {
                boolean open = !doorOpen(id);
                setDoor(lvl, id, open);
                resend(p, world);
                return true;
            }
            return false;
        }
        String posKey = lpos.getX() + "," + lpos.getY() + "," + lpos.getZ();
        if (tag.startsWith("drawer:")) {
            String content = tag.substring(7);
            if (state.hasProperty(OpenableBlock.OPEN)) {
                if (!state.getValue(OpenableBlock.OPEN)) {
                    lvl.setBlock(world, state.setValue(OpenableBlock.OPEN, true), 3);
                    sndAt(lpos.getX() + 0.5, lpos.getY() + 0.5, lpos.getZ() + 0.5, "fx.drawer", 1f, 0.9f + RND.nextFloat() * 0.2f);
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
                msg("drawer_junk" + RND.nextInt(5));
            } else if (content.equals("battery")) {
                S.opened.add(posKey);
                give("battery");
            } else if (content.startsWith("item:")) {
                S.opened.add(posKey);
                give(content.substring(5));
                Steps.onItem(p, content.substring(5));
            } else if (content.startsWith("note:")) {
                // a note stays in its drawer: it can be re-read
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

    // ================================================================== hiding
    public static House.Hide hiddenIn(ServerPlayer p) {
        if (hiddenId == null || !isStoryPlayer(p)) return null;
        for (House.Hide h : House.HIDES) if (h.id.equals(hiddenId)) return h;
        return null;
    }

    public static boolean seenHiding() { return seenHide; }

    /** how many wardrobes he bothers to open: he learns from how often you hide */
    public static int searchDepth() {
        return 1 + (S.anger > 60 ? 2 : S.anger > 25 ? 1 : 0) + Math.min(2, S.v("hides") / 3);
    }

    /** called while he is breathing at the slats of the wardrobe you are in */
    public static boolean hideDetected(ServerPlayer p, HatmanEntity e) {
        if (S.nvOn) return true; // the camera's infrared lamp glows through the slats
        if (seenHide) return true;
        return RND.nextFloat() < Math.max(0, (S.anger - 55) / 200f);
    }

    static void hide(ServerPlayer p, House.Hide h) {
        seenHide = hm != null && hm.isAlive() && hm.getState() != HatmanEntity.VANISH && hm.sees(p) && hm.distanceTo(p) < 14;
        hiddenId = h.id;
        Vec3 c = worldCenter(h.pos);
        p.teleportTo(p.serverLevel(), c.x, c.y + 0.05, c.z, h.facing.toYRot(), 0);
        p.setInvisible(true);
        p.setDeltaMovement(Vec3.ZERO);
        snd("fx.door_creak", 0.7f, 1.3f);
        Net.toPlayer(p, Msg.of("hide").b("on", true));
        S.v("hides", S.v("hides") + 1);
        if (S.nvOn) msg("hide_cam");
        else msg("hide_hint");
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
            snd("fx.door_creak", 0.7f, 1.2f);
            Steps.onHide(p, h.id, false);
        }
        Net.toPlayer(p, Msg.of("hide").b("on", false));
    }

    // ================================================================== the hatman
    static HatmanEntity spawnHatman(double x, double y, double z, float yaw) {
        ServerPlayer p = player();
        if (p == null) return null;
        removeHatman();
        HatmanEntity e = ModEntities.HATMAN.get().create(p.serverLevel());
        if (e == null) return null;
        Vec3 v = w(x, y, z);
        e.moveTo(v.x, v.y, v.z, yaw, 0);
        e.yBodyRot = yaw;
        e.yHeadRot = yaw;
        e.story = true;
        e.setAnger(S.anger);
        e.setStateRaw(HatmanEntity.SCRIPT);
        p.serverLevel().addFreshEntity(e);
        hm = e;
        relocations = 0;
        return e;
    }

    static void removeHatman() {
        if (hm != null && hm.isAlive()) hm.discard();
        hm = null;
        ServerPlayer p = player();
        if (p != null) {
            AABB box = new AABB(S.ox - 16, S.oy - 12, S.oz - 16, S.ox + 62, S.oy + 40, S.oz + 68);
            for (HatmanEntity e : p.serverLevel().getEntitiesOfClass(HatmanEntity.class, box)) if (e.story) e.discard();
        }
    }

    static boolean hatmanPresent() { return hm != null && hm.isAlive() && hm.getState() != HatmanEntity.VANISH; }

    public static void onHatmanGone(HatmanEntity e) {
        if (e == hm) {
            hm = null;
            ServerPlayer p = player();
            if (p != null) fx(p, "static", 8);
            Steps.onHatmanEvent("gone");
        }
    }

    public static void onHatmanEvent(HatmanEntity e, String ev) {
        if (!S.active || e != hm) return;
        if (ev.equals("stuck")) {
            relocate(false);
            return;
        }
        Steps.onHatmanEvent(ev);
    }

    /** He does not need to walk. He arrives. Re-appears near the player, preferably ahead of them and out of sight. */
    static boolean relocate(boolean ahead) {
        ServerPlayer p = player();
        if (p == null || hm == null) return false;
        if (relocations++ > 6 && hm.getState() != HatmanEntity.HUNT) {
            hm.vanish();
            return false;
        }
        int state = hm.getState();
        double speed = hm.huntSpeed;
        Vec3 spot = findSpot(p, 7, 13, true, ahead);
        if (spot == null) spot = findSpot(p, 6, 16, false, ahead);
        if (spot == null) return false;
        Vec3 l = spot.subtract(S.ox, S.oy, S.oz);
        HatmanEntity e = spawnHatman(l.x, l.y, l.z, 0);
        if (e == null) return false;
        e.huntSpeed = speed;
        e.face(p.position());
        e.emerge(state == HatmanEntity.SEARCH ? HatmanEntity.HUNT : state == HatmanEntity.SCRIPT ? HatmanEntity.HUNT : state);
        fx(p, "static", 6);
        return true;
    }

    /** stared down while he is still calm: the lights die for a moment and he is closer */
    public static void blink(HatmanEntity e, ServerPlayer p) {
        String room = room(p);
        if (!room.isEmpty()) flicker(room, 24);
        Vec3 to = p.position().subtract(e.position());
        double d = to.length();
        if (d > 6) {
            Vec3 np = e.position().add(to.normalize().scale(Math.min(5, d - 4)));
            if (standable(p.serverLevel(), BlockPos.containing(np))) e.teleportTo(np.x, np.y, np.z);
        }
        fx(p, "static", 10);
        snd("hm.whisper", 0.8f, 0.9f);
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

    /** a place to stand near the player, inside the house, on the player's floor */
    static Vec3 findSpot(ServerPlayer p, double minD, double maxD, boolean unseen, boolean ahead) {
        ServerLevel lvl = p.serverLevel();
        Vec3 look = p.getViewVector(1f);
        Vec3 vel = new Vec3(look.x, 0, look.z).normalize();
        Vec3 best = null;
        double bestScore = -1e9;
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
                if (r.equals("B_COLD") && !S.f("cold_open")) continue;
                Vec3 v = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                boolean seen = playerCanSee(p, v);
                if (unseen && seen) continue;
                double score = RND.nextDouble() * 2;
                Vec3 dir = v.subtract(p.position()).normalize();
                double front = dir.x * vel.x + dir.z * vel.z;
                score += ahead ? front * 3 : -front * 2;
                if (r.equals(room(p))) score -= 1.5;
                if (score > bestScore) {
                    bestScore = score;
                    best = v;
                }
                break;
            }
        }
        return best;
    }

    // ================================================================== guests
    static void spawnGuests(ServerLevel lvl) {
        for (GuestEntity g : guests) if (g.isAlive()) g.discard();
        guests.clear();
        for (int[] d : House.GUESTS) {
            GuestEntity g = ModEntities.GUEST.get().create(lvl);
            if (g == null) continue;
            Vec3 v = w(d[0] + 0.5, d[1], d[2] + 0.5);
            g.moveTo(v.x, v.y, v.z, d[3], 0);
            g.story = true;
            g.setup(d[3], d[4], d[5] == 1);
            lvl.addFreshEntity(g);
            guests.add(g);
        }
    }

    static void ensureGuests() {
        ServerPlayer p = player();
        if (p == null) return;
        boolean any = false;
        for (GuestEntity g : guests) if (g.isAlive()) any = true;
        if (!any) spawnGuests(p.serverLevel());
    }

    static void guestsWatch(boolean w) {
        for (GuestEntity g : guests) if (g.isAlive()) g.setWatching(w);
    }

    /** line the guests up along the ground floor corridor, or put them back in their chairs */
    static void guestsToCorridor(boolean corridor) {
        ServerPlayer p = player();
        if (p == null) return;
        ensureGuests();
        int i = 0;
        for (GuestEntity g : guests) {
            if (!g.isAlive()) continue;
            int[] d = House.GUESTS.get(Math.min(i, House.GUESTS.size() - 1));
            if (corridor) {
                boolean north = i % 2 == 0;
                double x = 30 - i * 2.4;
                Vec3 v = w(x, 1, north ? 14.4 : 16.6);
                g.teleportTo(v.x, v.y, v.z);
                g.setup(north ? 0 : 180, d[4], false);
                g.setWatching(true);
            } else {
                Vec3 v = w(d[0] + 0.5, d[1], d[2] + 0.5);
                g.teleportTo(v.x, v.y, v.z);
                g.setup(d[3], d[4], true);
            }
            i++;
        }
    }

    // ================================================================== caught / checkpoints
    static void checkpoint(String step, double x, double y, double z, float yaw) {
        S.cpStep = step;
        S.cpX = x;
        S.cpY = y;
        S.cpZ = z;
        S.cpYaw = yaw;
    }

    public static void caught(ServerPlayer p, HatmanEntity e) {
        if (!isStoryPlayer(p) || respawnIn >= 0 || cutId != null) return;
        respawnIn = 56;
        S.caught++;
        fx(p, "jumpscare", 40);
        snd("fx.jumpscare", 1f, 1f);
        if (dlgId != null) endDialogueSilently();
        p.setInvulnerable(true);
        hudDirty = true;
    }

    private static void respawn(ServerPlayer p) {
        removeHatman();
        timers.clear();
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
        dlgAngerStart = S.anger;
        sendNode(firstNode);
    }

    private static boolean meets(String need) {
        if (need == null) return true;
        if (need.equals("bound")) return S.claim >= 70 && !has("original_face");
        if (need.equals("!bound")) return !(S.claim >= 70 && !has("original_face"));
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
        if (nodeId.equals("f2")) nodeId = S.anger >= 60 ? "f2a" : S.claim >= 50 ? "f2b" : "f2c";
        Dialogues.Node n = Dialogues.NODES.get(nodeId);
        if (p == null || n == null) {
            finishDialogue("");
            return;
        }
        dlgNode = nodeId;
        Msg m = Msg.of("dlg").s("node", nodeId).s("speaker", n.speaker).s("key", "dlg." + nodeId).i("time", n.time).s("pname", p.getGameProfile().getName());
        int count = 0;
        for (int i = 0; i < n.choices.size(); i++) {
            Dialogues.Choice c = n.choices.get(i);
            if (!meets(c.need)) continue;
            m.s("c" + count, "dlg." + nodeId + "." + i).i("ci" + count, i);
            count++;
        }
        m.i("count", count);
        m.f("anger", S.anger);
        Net.toPlayer(p, m);
        if (hm != null && hm.isAlive()) {
            hm.setAnger(S.anger);
            if (n.anim != null) hm.act(n.anim, 30, null);
        }
        if (n.sound != null) {
            if (hm != null && hm.isAlive() && n.sound.startsWith("hm.")) hm.say(n.sound, 1f, 1f);
            else snd(n.sound, 1f, 1f);
        }
    }

    private static void onChoice(ServerPlayer p, String nodeId, int idx) {
        if (dlgId == null || !nodeId.equals(dlgNode)) return;
        Dialogues.Node n = Dialogues.NODES.get(nodeId);
        if (n == null) return;
        String next;
        if (n.choices.isEmpty()) {
            next = n.next;
        } else if (idx < 0 || idx >= n.choices.size()) {
            // silence. it is rude not to answer.
            S.anger = Math.min(100, S.anger + 6);
            next = n.silence != null ? n.silence : n.next;
        } else {
            Dialogues.Choice c = n.choices.get(idx);
            if (!meets(c.need)) return;
            S.anger = Math.max(0, Math.min(100, S.anger + c.anger));
            S.claim = Math.max(0, Math.min(100, S.claim + c.claim));
            if (c.lie) {
                S.lies++;
                // he punishes a lie where it hurts: the lights
                String room = room(p);
                if (!room.isEmpty()) flicker(room, 30);
                fx(p, "shake", 12);
            }
            if (c.flag != null) S.flags.add(c.flag);
            next = c.next;
        }
        if (next.startsWith("end")) finishDialogue(next.length() > 4 ? next.substring(4) : "");
        else sendNode(next);
    }

    private static void finishDialogue(String result) {
        String id = dlgId;
        float delta = S.anger - dlgAngerStart;
        dlgId = null;
        dlgNode = null;
        ServerPlayer p = player();
        if (p != null) Net.toPlayer(p, Msg.of("dlg_end"));
        hudDirty = true;
        if (id != null) Steps.onDialogueEnd(id, result, delta);
        S.save(server);
    }

    private static void endDialogueSilently() {
        dlgId = null;
        dlgNode = null;
        ServerPlayer p = player();
        if (p != null) Net.toPlayer(p, Msg.of("dlg_end"));
    }

    // ================================================================== cutscene engine
    static void cutscene(String id) {
        ServerPlayer p = player();
        if (p == null) return;
        if (hiddenId != null) unhide(p);
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
            case "cam" -> toggleCam(p);
            case "dlg_choice" -> onChoice(p, m.s("node"), m.i("i"));
            case "cut_done" -> {
                if (cutId != null && cutId.equals(m.s("id"))) {
                    Cutscenes.Cut c = Cutscenes.ALL.get(cutId);
                    // run any cues the skip jumped over
                    if (c != null) while (cutT < c.length) Steps.cutCue(cutId, ++cutT);
                    endCutscene();
                }
            }
            case "note_closed" -> Steps.onNoteClosed(p, m.s("id"));
            case "unhide" -> {
                if (hiddenId != null) unhide(p);
            }
            case "keypad" -> Steps.onKeypad(p, m.s("code"));
            case "credits_done" -> Steps.onCreditsDone(p);
            default -> {}
        }
    }

    static void toggleCam(ServerPlayer p) {
        if (!S.hasCam) return;
        if (!S.nvOn && S.battery <= 0 && !take("battery")) {
            msg("cam_dead");
            snd("fx.cam_low", 1f, 1f);
            return;
        }
        if (!S.nvOn && S.battery <= 0) S.battery = 100;
        S.nvOn = !S.nvOn;
        snd(S.nvOn ? "fx.cam_on" : "fx.cam_off", 1f, 1f);
        if (S.nvOn && hiddenId != null) msg("hide_cam");
        hudDirty = true;
        sendHud(p);
    }

    private static void sendHud(ServerPlayer p) {
        String amb = "house";
        Vec3 l = lp(p);
        String r = House.roomAt(l.x, l.y, l.z);
        if (r.equals("YARD") || r.equals("ROAD") || r.isEmpty()) amb = "outside";
        else if (l.y < -0.5) amb = "cellar";
        boolean chase = hm != null && hm.isAlive() && (hm.getState() == HatmanEntity.HUNT || (hm.getState() == HatmanEntity.SEARCH && hiddenId != null));
        float fear = 0;
        if (hatmanPresent() && !hm.cameraOnly()) fear = (float) Math.max(0, 1 - hm.distanceTo(p) / 16.0);
        if (chase) fear = Math.max(fear, 0.6f);
        Msg m = Msg.of("hud").b("active", S.active).s("obj", S.objective).s("arg", S.objectiveArg)
                .f("bat", S.battery).b("nv", S.nvOn).b("cam", S.hasCam).i("notes", S.notes.size()).i("notesMax", Notes.COUNT)
                .i("ox", S.ox).i("oy", S.oy).i("oz", S.oz).s("amb", amb).b("chase", chase).f("fear", fear)
                .b("hidden", hiddenId != null).i("spare", countItem(p, "battery")).b("power", S.power).i("chapter", S.chapter)
                .b("fire", S.f("fire")).f("anger", S.anger);
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
        if (S.v("resume") == 1) {
            S.vars.remove("resume");
            House.ensureIndexed();
            ensureGuests();
            refreshLamps(null);
            Steps.resume();
            hudDirty = true;
        }
        // timers
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
        // the player is looked after: no hunger, no stray damage, the night never ends
        if (age % 40 == 0) {
            p.getFoodData().setFoodLevel(20);
            if (p.getHealth() < p.getMaxHealth()) p.setHealth(p.getMaxHealth());
            if (p.serverLevel().getDayTime() % 24000 < 15000 || p.serverLevel().getDayTime() % 24000 > 21000) p.serverLevel().setDayTime(18000);
            if (S.f("fire")) p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0, false, false, false));
        }
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
        // hiding: hold the player still inside the wardrobe
        House.Hide h = hiddenIn(p);
        if (h != null) {
            Vec3 c = worldCenter(h.pos);
            if (p.position().distanceToSqr(c.x, p.getY(), c.z) > 0.2) p.teleportTo(p.serverLevel(), c.x, c.y + 0.05, c.z, p.getYRot(), p.getXRot());
        }
        // camera battery
        if (S.nvOn) {
            S.battery -= 100f / (20f * 260f);
            if (S.battery <= 0) {
                if (take("battery")) {
                    S.battery = 100;
                    snd("fx.cam_on", 0.8f, 1.2f);
                    msg("cam_swap");
                } else {
                    S.battery = 0;
                    S.nvOn = false;
                    snd("fx.cam_off", 1f, 0.8f);
                    msg("cam_dead");
                }
                hudDirty = true;
            } else if (S.battery < 15 && age % 60 == 0) snd("fx.cam_low", 0.5f, 1f);
        }
        S.t++;
        if (dlgId == null) {
            Steps.tick(p);
            if (S.active) haunt(p);
        }
        if (!S.active) return;
        // thunder
        if (--thunderCd <= 0) {
            thunderCd = 500 + RND.nextInt(900);
            snd("fx.thunder", sndOutside(p) ? 1f : 0.55f, 0.9f + RND.nextFloat() * 0.2f);
            fx(p, "flash", 6);
        }
        // remember a safe place to come back to, when no chase owns the checkpoint
        if (S.cpStep.isEmpty() && !hatmanPresent() && p.onGround() && ++safeTicks > 100) {
            safeTicks = 0;
            Vec3 l = lp(p);
            if (!House.roomAt(l.x, l.y, l.z).isEmpty()) {
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
        return r.equals("YARD") || r.equals("ROAD");
    }

    // ================================================================== the haunting (free roam)
    private static final String[] FAR_SOUNDS = {"fx.fake_steps", "fx.creak", "fx.thump", "hm.knock", "fx.piano", "fx.creak", "fx.fake_steps", "fx.clock_chime"};
    private static final String[] NEAR_SOUNDS = {"hm.breath", "hm.whisper", "fx.creak", "fx.fake_steps", "hm.whisper"};
    private static int lastEvent = -1;

    private static void haunt(ServerPlayer p) {
        if (S.haunt <= 0 || hiddenId != null) return;
        if (hatmanPresent()) {
            // lamps choke when he is in the room
            if (age % 14 == 0 && !hm.cameraOnly()) {
                Vec3 l = hm.position().subtract(S.ox, S.oy, S.oz);
                String r = House.roomAt(l.x, l.y, l.z);
                if (!r.isEmpty() && RND.nextInt(3) == 0) flicker(r, 16);
            }
            // a watcher does not stay forever
            if (hm.getState() == HatmanEntity.WATCH && hm.stateTicks > 500) hm.vanish();
            if (hm.getState() == HatmanEntity.STALK && hm.stateTicks > 1500) hm.vanish();
            return;
        }
        if (--hauntCd > 0) return;
        String room = room(p);
        if (room.isEmpty() || room.equals("ROAD") || room.equals("YARD")) {
            hauntCd = 200;
            return;
        }
        int lvl = S.haunt;
        hauntCd = (lvl == 1 ? 520 : lvl == 2 ? 420 : 360) + RND.nextInt(lvl == 3 ? 300 : 420);
        // angrier host, shorter silences
        hauntCd = (int) (hauntCd * (1.0 - S.anger / 250.0));
        int roll = RND.nextInt(100);
        int ev;
        if (lvl == 1) ev = roll < 62 ? 0 : roll < 80 ? 1 : 2;
        else if (lvl == 2) ev = roll < 34 ? 0 : roll < 48 ? 1 : roll < 70 ? 2 : 3;
        else ev = roll < 18 ? 0 : roll < 26 ? 1 : roll < 38 ? 2 : roll < 60 ? 3 : roll < 80 ? 4 : 5;
        if (ev == lastEvent && ev != 0) ev = 0;
        lastEvent = ev;
        switch (ev) {
            case 0 -> hauntSound(p);
            case 1 -> hauntProp(p, room);
            case 2 -> { // apparition: he is simply there, watching
                Vec3 spot = findSpot(p, 9, 20, true, true);
                if (spot == null) { hauntSound(p); return; }
                Vec3 l = spot.subtract(S.ox, S.oy, S.oz);
                HatmanEntity e = spawnHatman(l.x, l.y, l.z, 0);
                if (e == null) return;
                e.face(p.position());
                e.setFlag(HatmanEntity.F_CAMONLY, lvl == 1);
                e.stareLimit = 50;
                e.setState(HatmanEntity.WATCH);
                if (lvl == 1 && !S.nvOn && S.hasCam) later(30, () -> msg("cam_hint"));
            }
            case 3 -> { // the stalker: moves only when you are not looking
                Vec3 spot = findSpot(p, 10, 18, true, false);
                if (spot == null) { hauntSound(p); return; }
                Vec3 l = spot.subtract(S.ox, S.oy, S.oz);
                HatmanEntity e = spawnHatman(l.x, l.y, l.z, 0);
                if (e == null) return;
                e.face(p.position());
                e.lethal = lvl >= 3;
                e.stareLimit = Math.max(50, 150 - (int) S.anger - S.v("stares") * 12);
                e.setState(HatmanEntity.STALK);
                S.v("stares", S.v("stares") + 1);
            }
            case 5 -> { // the ambush: he is already standing where you are about to walk, perfectly still
                Vec3 spot = findSpot(p, 8, 15, true, true);
                if (spot == null) { hauntSound(p); return; }
                Vec3 l = spot.subtract(S.ox, S.oy, S.oz);
                HatmanEntity e = spawnHatman(l.x, l.y, l.z, 0);
                if (e == null) return;
                e.face(p.position());
                e.huntSpeed = 4.8 + S.anger / 120.0;
                e.setState(HatmanEntity.AMBUSH);
            }
            default -> { // the hunt: he comes up out of the floor and runs you down
                Vec3 spot = findSpot(p, 9, 14, false, false);
                if (spot == null) { hauntSound(p); return; }
                Vec3 l = spot.subtract(S.ox, S.oy, S.oz);
                HatmanEntity e = spawnHatman(l.x, l.y, l.z, 0);
                if (e == null) return;
                e.face(p.position());
                e.huntSpeed = 4.7 + S.anger / 100.0;
                e.huntPatience = 80 + S.v("hides") * 10;
                e.emerge(HatmanEntity.HUNT);
                flicker(room, 40);
                think("hunt" + RND.nextInt(3));
            }
        }
    }

    private static void hauntSound(ServerPlayer p) {
        boolean near = RND.nextInt(3) == 0;
        Vec3 look = p.getViewVector(1f);
        if (near) {
            // right behind you
            Vec3 v = p.position().subtract(look.x * 2.2, -1.2, look.z * 2.2);
            p.serverLevel().playSound(null, v.x, v.y, v.z, ModSounds.get(NEAR_SOUNDS[RND.nextInt(NEAR_SOUNDS.length)]), SoundSource.AMBIENT, 0.8f, 0.9f + RND.nextFloat() * 0.2f);
        } else {
            double a = RND.nextDouble() * Math.PI * 2;
            double d = 7 + RND.nextDouble() * 9;
            int dy = RND.nextInt(3) == 0 ? 0 : (RND.nextBoolean() ? 5 : -5);
            Vec3 v = p.position().add(Math.cos(a) * d, dy, Math.sin(a) * d);
            p.serverLevel().playSound(null, v.x, v.y, v.z, ModSounds.get(FAR_SOUNDS[RND.nextInt(FAR_SOUNDS.length)]), SoundSource.AMBIENT, 1.0f, 0.85f + RND.nextFloat() * 0.25f);
        }
    }

    /** the house itself misbehaves */
    private static void hauntProp(ServerPlayer p, String room) {
        ServerLevel lvl = p.serverLevel();
        int kind = RND.nextInt(4);
        if (kind == 0) {
            flicker(room, 30 + RND.nextInt(30));
        } else if (kind == 1) {
            // the nearest closed door behind you opens by itself
            BlockPos best = null;
            double bd = 1e9;
            for (BlockPos lpos : House.DOORS.values()) {
                BlockPos wpos = wp(lpos);
                BlockState s = lvl.getBlockState(wpos);
                if (!(s.getBlock() instanceof DoorBlock) || s.getValue(DoorBlock.OPEN)) continue;
                String t = House.HOT.get(lpos);
                if (t != null && locked(t.substring(5))) continue;
                double d = wpos.distSqr(p.blockPosition());
                if (d < bd && d > 9 && d < 220 && Math.abs(wpos.getY() - p.getY()) < 3) {
                    bd = d;
                    best = wpos;
                }
            }
            if (best != null) {
                BlockState s = lvl.getBlockState(best);
                ((DoorBlock) s.getBlock()).setOpen(null, lvl, s, best, true);
                lvl.playSound(null, best, ModSounds.get("fx.door_creak"), SoundSource.AMBIENT, 1f, 0.8f);
            } else hauntSound(p);
        } else if (kind == 2 && room.equals("G_LIVING")) {
            setMode("tv", 1);
            BlockPos tv = House.first("tv");
            sndAt(tv.getX(), tv.getY(), tv.getZ(), "fx.static", 0.8f, 1f);
            later(80, () -> setMode("tv", 0));
        } else {
            // every lamp in the house, once
            snd("fx.power_down", 0.5f, 1.2f);
            flicker("", 20);
        }
    }

    // ================================================================== debug
    /** hooks for the automated test run */
    public static void test(ServerPlayer p, String cmd) {
        if (cmd.startsWith("use:")) {
            BlockPos l = House.first(cmd.substring(4));
            if (l == null) HatmanMod.LOG.error("[AUTOTEST] no hotspot {}", cmd);
            else interact(p, wp(l));
            return;
        }
        String[] a = cmd.split(":");
        switch (a[0]) {
            case "power" -> power(a[1].equals("on"));
            case "nv" -> {
                S.hasCam = true;
                S.battery = 100;
                S.nvOn = a[1].equals("on");
                hud();
            }
            case "tp" -> {
                String[] c = a[1].split(",");
                Vec3 v = w(Double.parseDouble(c[0]), Double.parseDouble(c[1]), Double.parseDouble(c[2]));
                p.teleportTo(p.serverLevel(), v.x, v.y, v.z, Float.parseFloat(c[3]), Float.parseFloat(c[4]));
            }
            case "hm" -> {
                String[] c = a[1].split(",");
                HatmanEntity e = spawnHatman(Double.parseDouble(c[0]), Double.parseDouble(c[1]), Double.parseDouble(c[2]), Float.parseFloat(c[3]));
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
            case "step" -> debugStep(p, a[1]);
            case "dlg" -> startDialogue(a[1], a[2]);
            case "note" -> note(a[1]);
            case "haunt" -> { S.haunt = Integer.parseInt(a[1]); hauntCd = 5; }
            case "hide" -> { for (House.Hide h : House.HIDES) if (h.id.equals(a[1])) hide(p, h); }
            case "fx" -> fx(p, a[1], Integer.parseInt(a[2]));
            case "kill" -> removeHatman();
            case "mood" -> { S.anger = Float.parseFloat(a[1]); S.claim = Float.parseFloat(a[2]); }
            case "keypad" -> Net.toPlayer(p, Msg.of("keypad"));
            case "give" -> debugGiveAll(p);
            case "gm" -> p.setGameMode(a[1].equals("spec") ? GameType.SPECTATOR : GameType.ADVENTURE);
            case "obj" -> obj(a.length > 1 ? a[1] : "");
            case "cut" -> cutscene(a[1]);
            case "audit" -> HouseAudit.run(p.serverLevel(), origin());
            case "advance" -> {
                if (dlgId != null && dlgNode != null) {
                    Dialogues.Node n = Dialogues.NODES.get(dlgNode);
                    onChoice(p, dlgNode, n == null || n.choices.isEmpty() ? -2 : Integer.parseInt(a[1]));
                }
            }
            case "unhide" -> { if (hiddenId != null) unhide(p); }
            case "code" -> Steps.onKeypad(p, a[1]);
            default -> HatmanMod.LOG.warn("unknown test command {}", cmd);
        }
    }

    public static void debugStep(ServerPlayer p, String id) {
        if (!isStoryPlayer(p)) return;
        removeHatman();
        timers.clear();
        dlgId = null;
        cutId = null;
        Net.toPlayer(p, Msg.of("dlg_end"));
        Net.toPlayer(p, Msg.of("cut_end"));
        Steps.debugJump(p, id);
    }

    public static void debugGiveAll(ServerPlayer p) {
        if (!isStoryPlayer(p)) return;
        for (String id : ModItems.STORY_IDS) if (!has(id)) give(id);
        S.hasCam = true;
        hudDirty = true;
    }

    public static void debugHouse(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        BlockPos pp = p.blockPosition();
        House.build(lvl, new BlockPos(pp.getX() - 22, pp.getY() - 1, pp.getZ() - 56));
    }

    static void campfire(String tag, boolean lit) {
        ServerPlayer p = player();
        BlockPos l = House.first(tag);
        if (p == null || l == null) return;
        BlockState s = p.serverLevel().getBlockState(wp(l));
        if (s.hasProperty(CampfireBlock.LIT)) p.serverLevel().setBlock(wp(l), s.setValue(CampfireBlock.LIT, lit), 3);
    }

    static Direction dummy() { return Direction.NORTH; }

    static DoubleBlockHalf dummy2() { return DoubleBlockHalf.LOWER; }

    static net.minecraft.world.level.block.Block air() { return Blocks.AIR; }
}
