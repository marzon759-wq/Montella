package com.krolasyon.hatman.entity;

import com.krolasyon.hatman.compat.HatmanBase;
import com.krolasyon.hatman.registry.ModSounds;
import com.krolasyon.hatman.story.House;
import com.krolasyon.hatman.story.Story;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The Hatman. A hand written state machine instead of vanilla goals: every behaviour has to be controllable
 * by the story director, and has to feel deliberate rather than mob-like.
 */
public class HatmanEntity extends HatmanBase {
    public static final int IDLE = 0, WATCH = 1, STALK = 2, HUNT = 3, SEARCH = 4, AMBUSH = 5, TALK = 6, SCRIPT = 7, EMERGE = 8, VANISH = 9;
    public static final int F_DUCK = 1, F_CRAWL = 2, F_CAMONLY = 4, F_SIT = 8, F_FROZEN = 16, F_MOVING = 32, F_RUN = 64, F_NOLOOK = 128;
    private static final String[] ACTIONS = {"emerge", "vanish", "hat_tip", "knock", "scream", "grab", "search", "point", "twitch",
            "head_tilt", "talk_calm", "talk_angry", "lean_in", "offer", "bow", "drop", "burst", "stand_up"};

    /** true while the story owns this entity: it never acts on its own initiative outside of its assigned state */
    public boolean story = false;
    public int stateTicks = 0;
    private int afterEmerge = WATCH;
    private int actionLock = 0;
    private Vec3 lastSeen = null;
    private int lostTicks = 0;
    private int stareTicks = 0;
    private int unseenTicks = 0;
    private int stuckTicks = 0;
    private Vec3 lastPos = Vec3.ZERO;
    private Vec3 scriptTarget = null;
    private double scriptSpeed = 2.0;
    private int grabTicks = -1;
    private int duckHold = 0;
    private int crawlTicks = 0;
    private int repath = 0;
    private int stepTimer = 0;
    private final List<House.Hide> searchList = new ArrayList<>();
    private int searchPhase = 0;
    private int searchWait = 0;
    public double huntSpeed = 5.0;
    public int huntPatience = 100;
    /** how tolerant he is of being stared at while stalking, in ticks */
    public int stareLimit = 120;
    public boolean lethal = true;
    private boolean stale = false;

    public HatmanEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        if (this.getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
            nav.setCanPassDoors(true);
        }
    }

    @Override
    protected void registerGoals() {}

    @Override
    public String[] actionNames() { return ACTIONS; }

    // ------------------------------------------------------------------ animation hooks (both sides)
    public boolean flag(int f) { return (getFlags() & f) != 0; }

    public void setFlag(int f, boolean on) {
        int cur = getFlags();
        int n = on ? cur | f : cur & ~f;
        if (n != cur) setFlags(n);
    }

    @Override
    public String loopAnim() {
        if (flag(F_SIT)) return "sit";
        if (flag(F_CRAWL)) return "crawl";
        if (flag(F_FROZEN)) return "freeze";
        if (flag(F_MOVING)) return flag(F_RUN) ? "run" : getState() == STALK ? "stalk" : "walk";
        return "idle";
    }

    @Override
    public boolean isDucking() { return flag(F_DUCK) && !flag(F_CRAWL); }

    public boolean headTracks() { return !flag(F_NOLOOK) && !flag(F_CRAWL); }

    public boolean cameraOnly() { return flag(F_CAMONLY); }

    // ------------------------------------------------------------------ commands
    public void setState(int s) {
        setStateRaw(s);
        stateTicks = 0;
        stareTicks = 0;
        unseenTicks = 0;
        lostTicks = 0;
        grabTicks = -1;
        if (s != STALK) setFlag(F_FROZEN, false);
        if (s != SCRIPT && s != TALK) setFlag(F_SIT, false);
        if (s == IDLE || s == WATCH || s == TALK || s == AMBUSH) halt();
    }

    public void act(String anim, int lockTicks, String sound) {
        playAction(anim);
        actionLock = Math.max(actionLock, lockTicks);
        if (sound != null) say(sound, 1.0f, 1.0f);
    }

    public void say(String sound, float vol, float pitch) {
        SoundEvent ev = ModSounds.get(sound);
        level().playSound(null, getX(), getY() + 1.5, getZ(), ev, SoundSource.HOSTILE, vol, pitch * (0.95f + random.nextFloat() * 0.1f));
    }

    public void emerge(int then) {
        afterEmerge = then;
        setState(EMERGE);
        act("emerge", 48, "hm.emerge");
    }

    public void drop(int then) {
        afterEmerge = then;
        setState(EMERGE);
        stateTicks = 20;
        act("drop", 30, "fx.thump");
    }

    public void vanish() {
        if (getState() == VANISH) return;
        setState(VANISH);
        setFlag(F_SIT, false);
        act("vanish", 24, "hm.vanish");
    }

    public void beginHunt() {
        setState(HUNT);
        lastSeen = null;
    }

    public void beginSearch(Vec3 where) {
        setState(SEARCH);
        lastSeen = where;
        searchPhase = 0;
        searchList.clear();
    }

    public void scriptMove(Vec3 target, double metersPerSecond) {
        if (getState() != SCRIPT) setState(SCRIPT);
        scriptTarget = target;
        scriptSpeed = metersPerSecond;
        repath = 0;
    }

    public void scriptStop() {
        if (getState() != SCRIPT) setState(SCRIPT);
        scriptTarget = null;
        halt();
    }

    public boolean scriptArrived() {
        return scriptTarget == null || position().distanceToSqr(scriptTarget) < 1.6;
    }

    public void sit(boolean on) {
        setFlag(F_SIT, on);
        halt();
    }

    public void face(Vec3 p) {
        double dx = p.x - getX(), dz = p.z - getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
        yRotO = yaw;
        yBodyRotO = yaw;
        yHeadRotO = yaw;
    }

    private void halt() {
        getNavigation().stop();
        setFlag(F_MOVING, false);
        setFlag(F_RUN, false);
    }

    private double speedMod(double mps) {
        double attr = getAttributeValue(Attributes.MOVEMENT_SPEED);
        return Math.sqrt(0.0227 * mps) / attr;
    }

    private void go(Vec3 t, double mps) {
        if (repath-- <= 0 || getNavigation().isDone()) {
            getNavigation().moveTo(t.x, t.y, t.z, speedMod(mps));
            repath = 6;
        } else {
            getNavigation().setSpeedModifier(speedMod(mps));
        }
        setFlag(F_MOVING, true);
        setFlag(F_RUN, mps >= 4.2);
    }

    // ------------------------------------------------------------------ senses
    public ServerPlayer prey() {
        if (!(level() instanceof ServerLevel sl)) return null;
        if (story) return Story.player(sl.getServer());
        Player p = sl.getNearestPlayer(this, 48);
        return p instanceof ServerPlayer sp && !sp.isSpectator() && !sp.isCreative() ? sp : null;
    }

    private boolean los(Vec3 from, Vec3 to) {
        return level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    /** can the player actually see me right now: facing, line of sight, and enough light (or the camera) */
    public boolean observedBy(ServerPlayer p) {
        if (p == null) return false;
        Vec3 eye = p.getEyePosition();
        Vec3 chest = position().add(0, 1.3, 0);
        double dist = eye.distanceTo(chest);
        if (dist > 40) return false;
        Vec3 dir = chest.subtract(eye).normalize();
        if (dir.dot(p.getViewVector(1f)) < 0.55) return false;
        if (!los(eye, chest) && !los(eye, position().add(0, 2.3, 0))) return false;
        boolean nv = Story.nvOn(p);
        if (flag(F_CAMONLY)) return nv;
        if (dist < 3.5) return true;
        int light = level().getMaxLocalRawBrightness(blockPosition().above());
        if (light >= 5) return true;
        return nv && dist < 24;
    }

    /** can I see the player */
    public boolean sees(ServerPlayer p) {
        if (p == null || Story.hiddenIn(p) != null) return false;
        Vec3 eye = position().add(0, 2.4, 0);
        Vec3 t = p.getEyePosition();
        double d = eye.distanceTo(t);
        if (d > 26) return false;
        if (d > 5) {
            Vec3 look = Vec3.directionFromRotation(0, yHeadRot);
            Vec3 dir = t.subtract(eye).normalize();
            if (look.dot(dir) < -0.2 && !Story.nvOn(p)) return false;
        }
        return los(eye, t) || los(position().add(0, 1.0, 0), t);
    }

    // ------------------------------------------------------------------ tick
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (stale) { discard(); return; }
        stateTicks++;
        if (actionLock > 0) actionLock--;
        ServerPlayer p = prey();
        updatePosture();
        openDoors();
        switch (getState()) {
            case EMERGE -> {
                halt();
                if (p != null) getLookControl().setLookAt(p, 30, 30);
                if (stateTicks > 50) setState(afterEmerge);
            }
            case VANISH -> {
                halt();
                if (stateTicks > 22) {
                    Story.onHatmanGone(this);
                    discard();
                }
            }
            case WATCH -> tickWatch(p);
            case STALK -> tickStalk(p);
            case HUNT -> tickHunt(p);
            case SEARCH -> tickSearch(p);
            case AMBUSH -> tickAmbush(p);
            case TALK -> {
                halt();
                if (p != null) getLookControl().setLookAt(p.getX(), p.getEyeY(), p.getZ(), 20, 20);
                if (p != null && !flag(F_SIT)) face(p.position());
            }
            case SCRIPT -> tickScript(p);
            default -> {
                halt();
                if (!story && p != null && stateTicks > 40) setState(WATCH);
            }
        }
        footsteps();
        if (grabTicks >= 0) tickGrab(p);
        if (!story && p == null && stateTicks > 600) vanish();
    }

    private void tickWatch(ServerPlayer p) {
        halt();
        if (p == null) return;
        getLookControl().setLookAt(p.getX(), p.getEyeY(), p.getZ(), 15, 15);
        face(p.position());
        double d = distanceTo(p);
        boolean seen = observedBy(p);
        if (seen) stareTicks++;
        if (story) {
            // the story decides when a watcher leaves; he only refuses to be approached
            if (d < 5.5 || stareTicks > stareLimit) {
                if (stareTicks > 10 && actionLock == 0 && random.nextInt(3) == 0) act("hat_tip", 40, null);
                if (actionLock == 0) vanish();
            }
            return;
        }
        if (d < 6 || stareTicks > 100) {
            if (getAnger() > 40 || random.nextInt(3) == 0) setState(STALK);
            else vanish();
        } else if (stateTicks > 300 && !seen) setState(STALK);
    }

    private void tickStalk(ServerPlayer p) {
        if (p == null) { halt(); return; }
        double d = distanceTo(p);
        boolean seen = observedBy(p);
        if (seen) {
            // statue
            halt();
            setFlag(F_FROZEN, true);
            setFlag(F_NOLOOK, false);
            getLookControl().setLookAt(p.getX(), p.getEyeY(), p.getZ(), 60, 60);
            stareTicks++;
            unseenTicks = 0;
            if (stareTicks == stareLimit - 14 && actionLock == 0) act("twitch", 14, "hm.twitch");
            if (stareTicks > stareLimit) {
                setFlag(F_FROZEN, false);
                if (getAnger() >= 35 || !story) {
                    act("scream", 46, "hm.scream");
                    Story.fx(p, "shake", 30);
                    beginHunt();
                    stateTicks = -46;
                } else {
                    act("hat_tip", 40, null);
                    stareTicks = 0;
                    stareLimit = Math.max(50, stareLimit - 20);
                    Story.blink(this, p);
                }
            }
        } else {
            setFlag(F_FROZEN, false);
            unseenTicks++;
            if (stareTicks > 0 && unseenTicks > 30) stareTicks = Math.max(0, stareTicks - 2);
            if (d > 2.6) {
                go(p.position(), d > 12 ? 4.6 : 3.3);
            } else {
                halt();
                if (grabTicks < 0 && actionLock == 0) {
                    if (lethal && (getAnger() >= 50 || !story)) {
                        startGrab();
                    } else {
                        // close enough to touch - and he chooses not to. yet.
                        say("hm.whisper", 1.0f, 0.8f);
                        Story.fx(p, "static", 12);
                        Story.onHatmanEvent(this, "tapped");
                        vanish();
                    }
                }
            }
        }
        checkStuck(p);
    }

    private void tickHunt(ServerPlayer p) {
        if (p == null) { halt(); return; }
        if (stateTicks < 0) { halt(); getLookControl().setLookAt(p, 30, 30); return; }
        boolean sees = sees(p);
        double d = distanceTo(p);
        if (sees) {
            lastSeen = p.position();
            lostTicks = 0;
        } else {
            lostTicks++;
        }
        if (Story.hiddenIn(p) != null && lostTicks > 10) {
            beginSearch(lastSeen != null ? lastSeen : p.position());
            return;
        }
        if (lostTicks > huntPatience) {
            beginSearch(lastSeen != null ? lastSeen : position());
            return;
        }
        // rubber band: relentless at range, a hair slower than a sprinting player up close
        double sp = huntSpeed + (d > 14 ? 1.6 : d > 8 ? 0.7 : d < 4 ? -0.25 : 0);
        Vec3 target = lostTicks < 40 || lastSeen == null ? p.position() : lastSeen;
        go(target, sp);
        getLookControl().setLookAt(p.getX(), p.getEyeY(), p.getZ(), 40, 40);
        if (d < 1.9 && grabTicks < 0 && (sees || d < 1.2)) startGrab();
        if (stateTicks % 70 == 35) say(random.nextBoolean() ? "hm.growl" : "hm.breath", 1.0f, 1.0f);
        checkStuck(p);
    }

    private void tickSearch(ServerPlayer p) {
        if (p == null) { vanish(); return; }
        if (sees(p) && Story.hiddenIn(p) == null && stateTicks > 20) {
            act("twitch", 10, "hm.twitch");
            beginHunt();
            return;
        }
        switch (searchPhase) {
            case 0 -> { // walk to where he lost you
                Vec3 t = lastSeen != null ? lastSeen : position();
                go(t, 2.6);
                if (position().distanceToSqr(t) < 3.5 || stateTicks > 160) {
                    halt();
                    act("search", 80, "hm.search");
                    searchPhase = 1;
                    searchWait = 80;
                    // pick hiding places to check, nearest first
                    searchList.clear();
                    for (House.Hide h : House.HIDES) {
                        Vec3 w = Story.worldCenter(h.pos);
                        if (w.distanceToSqr(position()) < 14 * 14 && Math.abs(w.y - getY()) < 3) searchList.add(h);
                    }
                    searchList.sort((a, b) -> Double.compare(Story.worldCenter(a.pos).distanceToSqr(position()), Story.worldCenter(b.pos).distanceToSqr(position())));
                    int max = story ? Story.searchDepth() : 2;
                    House.Hide mine = Story.hiddenIn(p);
                    // he always checks at least the hide he heard you get into
                    if (mine != null && Story.seenHiding()) {
                        searchList.remove(mine);
                        searchList.add(0, mine);
                    }
                    while (searchList.size() > max) searchList.remove(searchList.size() - 1);
                }
            }
            case 1 -> {
                halt();
                if (--searchWait <= 0) searchPhase = searchList.isEmpty() ? 4 : 2;
            }
            case 2 -> { // walk up to a wardrobe
                House.Hide h = searchList.get(0);
                Vec3 front = Story.worldCenter(h.pos.relative(h.facing, 1));
                go(front, 2.2);
                if (position().distanceToSqr(front) < 1.5 || searchWait-- < -200) {
                    halt();
                    face(Story.worldCenter(h.pos));
                    act("lean_in", 70, "hm.breath");
                    searchPhase = 3;
                    searchWait = 76;
                    Story.onHatmanEvent(this, "at_hide:" + h.id);
                }
            }
            case 3 -> { // listening at the slats
                halt();
                House.Hide h = searchList.get(0);
                if (searchWait == 40) say("hm.breath", 1.0f, 0.7f);
                if (--searchWait <= 0) {
                    House.Hide mine = Story.hiddenIn(p);
                    if (mine == h && Story.hideDetected(p, this)) {
                        act("grab", 16, "hm.grab");
                        Story.caught(p, this);
                        return;
                    }
                    playAction("stand_up");
                    searchList.remove(0);
                    searchPhase = searchList.isEmpty() ? 4 : 2;
                    searchWait = 0;
                }
            }
            default -> {
                // gives up - or pretends to
                if (stateTicks % 5 == 0) {
                    Story.onHatmanEvent(this, "search_done");
                    if (isAlive() && getState() == SEARCH) vanish();
                }
            }
        }
    }

    private void tickAmbush(ServerPlayer p) {
        halt();
        setFlag(F_FROZEN, true);
        if (p == null) return;
        double d = distanceTo(p);
        if (d < 4.2 && (sees(p) || d < 2.5)) {
            setFlag(F_FROZEN, false);
            act("scream", 20, "hm.scream");
            Story.fx(p, "shake", 20);
            beginHunt();
            stateTicks = -16;
        } else if (stateTicks > 900) {
            setFlag(F_FROZEN, false);
            vanish();
        }
    }

    private void tickScript(ServerPlayer p) {
        if (flag(F_SIT)) { halt(); if (p != null) getLookControl().setLookAt(p.getX(), p.getEyeY(), p.getZ(), 20, 20); return; }
        if (scriptTarget != null && position().distanceToSqr(scriptTarget) > 1.2) {
            go(scriptTarget, scriptSpeed);
            if (getNavigation().isDone() && stateTicks % 20 == 0) repath = 0;
        } else {
            halt();
            if (p != null) getLookControl().setLookAt(p.getX(), p.getEyeY(), p.getZ(), 20, 20);
        }
    }

    private void startGrab() {
        grabTicks = 0;
        act("grab", 18, "hm.grab");
    }

    private void tickGrab(ServerPlayer p) {
        grabTicks++;
        if (grabTicks == 7) {
            if (p != null && distanceTo(p) < 2.8) {
                if (story) {
                    Story.caught(p, this);
                } else {
                    p.hurt(damageSources().mobAttack(this), 9f);
                    p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0, false, false, false));
                    Story.fx(p, "jumpscare", 24);
                    vanish();
                }
            }
        }
        if (grabTicks > 18) grabTicks = -1;
    }

    private void checkStuck(ServerPlayer p) {
        if (stateTicks % 20 != 0) return;
        if (flag(F_MOVING) && position().distanceToSqr(lastPos) < 0.3) stuckTicks += 20;
        else stuckTicks = 0;
        lastPos = position();
        // ladders, barred doors, bad paths: he does not need to walk. he arrives.
        if (stuckTicks >= 60 || (getState() == HUNT && p != null && Math.abs(p.getY() - getY()) > 2.5 && getNavigation().isDone() && stateTicks > 40)) {
            stuckTicks = 0;
            Story.onHatmanEvent(this, "stuck");
        }
    }

    private void updatePosture() {
        // bow under anything lower than his hat
        BlockPos bp = blockPosition();
        boolean low = false;
        Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
        for (int i = 0; i <= 1 && !low; i++) {
            BlockPos c = BlockPos.containing(getX() + fwd.x * i * 0.9, getY(), getZ() + fwd.z * i * 0.9);
            for (int dy = 2; dy <= 2; dy++) {
                BlockState s = level().getBlockState(c.above(dy));
                if (!s.getCollisionShape(level(), c.above(dy)).isEmpty()) low = true;
            }
        }
        if (low) duckHold = 12;
        else if (duckHold > 0) duckHold--;
        boolean duck = duckHold > 0 && !flag(F_SIT) && getState() != EMERGE && getState() != VANISH;
        setFlag(F_DUCK, duck);
        // a long low passage - he goes down on all fours
        if (duck && flag(F_MOVING) && low) crawlTicks++;
        else if (!low) crawlTicks = Math.max(0, crawlTicks - 4);
        setFlag(F_CRAWL, crawlTicks > 26 && (getState() == HUNT || getState() == SCRIPT));
    }

    private void openDoors() {
        if (!flag(F_MOVING)) return;
        Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
        for (int i = 0; i <= 1; i++) {
            BlockPos c = BlockPos.containing(getX() + fwd.x * i, getY() + 0.2, getZ() + fwd.z * i);
            BlockState s = level().getBlockState(c);
            if (s.getBlock() instanceof DoorBlock door && !s.getValue(DoorBlock.OPEN)) {
                if (story && Story.isLockedDoor(c) && !Story.isBarred(c)) return;
                if (Story.isBarred(c)) {
                    if (stateTicks % 26 == 0) {
                        say("hm.bang", 1.0f, 1.0f);
                        act("burst", 12, null);
                        Story.onHatmanEvent(this, "barred");
                    }
                    return;
                }
                door.setOpen(this, level(), s, c, true);
                if (getState() == HUNT) {
                    say("fx.door_slam", 0.9f, 1.0f);
                    act("burst", 10, null);
                } else {
                    say("fx.door_creak", 0.8f, 0.9f);
                }
            } else if (s.getBlock() instanceof FenceGateBlock && !s.getValue(FenceGateBlock.OPEN)) {
                if (story && Story.isLockedDoor(c)) return;
                level().setBlock(c, s.setValue(FenceGateBlock.OPEN, true), 10);
                say("fx.door_slam", 0.7f, 1.2f);
            }
        }
    }

    private void footsteps() {
        if (!flag(F_MOVING) || flag(F_CAMONLY)) { stepTimer = 0; return; }
        int st = getState();
        if (st == STALK) return; // the stalker makes no sound at all
        int every = flag(F_RUN) ? 6 : 12;
        if (++stepTimer >= every) {
            stepTimer = 0;
            say("hm.step", flag(F_RUN) ? 1.0f : 0.55f, flag(F_RUN) ? 1.05f : 0.85f);
        }
    }

    // ------------------------------------------------------------------ misc entity plumbing
    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(src, amount);
        if (!level().isClientSide && src.getEntity() instanceof ServerPlayer sp && actionLock == 0) {
            act("twitch", 12, "hm.twitch");
            setAnger(Math.min(100, getAnger() + 3));
            Story.onHatmanEvent(this, "struck");
            if (!story && getState() != HUNT) { beginHunt(); }
        }
        return false;
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity e) {}

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public boolean causeFallDamage(float a, float b, DamageSource s) { return false; }

    @Override
    protected SoundEvent getAmbientSound() { return null; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Story", story);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        story = tag.getBoolean("Story");
        // a story hatman never survives a reload: the director re-creates what it needs
        if (story) stale = true;
    }
}
