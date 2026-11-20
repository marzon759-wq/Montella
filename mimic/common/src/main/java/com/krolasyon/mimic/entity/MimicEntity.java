package com.krolasyon.mimic.entity;

import com.krolasyon.mimic.compat.MimicBase;
import com.krolasyon.mimic.registry.ModSounds;
import com.krolasyon.mimic.story.House;
import com.krolasyon.mimic.story.Story;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The thing in the barn. A hand written state machine instead of vanilla goals: every behaviour is controllable by the
 * story director. It hunts by ear first: footsteps, voices, the walkie, a kicked bucket. In the dark it sees only
 * what is close; a flashlight is a beacon.
 */
public class MimicEntity extends MimicBase {
    public static final int IDLE = 0, WATCH = 1, STALK = 2, HUNT = 3, SEARCH = 4, AMBUSH = 5, TALK = 6, SCRIPT = 7, EMERGE = 8, VANISH = 9, SLEEP = 10;
    public static final int F_DUCK = 1, F_CRAWL = 2, F_SLEEP = 4, F_SIT = 8, F_FROZEN = 16, F_MOVING = 32, F_RUN = 64, F_NOLOOK = 128,
            F_EAT = 256, F_DRAG = 512, F_CLIMB = 1024, F_SPREAD = 2048, F_STALKWALK = 4096;
    private static final String[] ACTIONS = {"stagger", "emerge", "vanish", "roar", "scream", "grab", "kill", "search", "lean_in", "stand_up", "twitch",
            "listen", "sniff", "drop", "burst", "death", "mimic_talk", "bang"};

    public boolean story = false;
    public int stateTicks = 0;
    private int afterEmerge = HUNT;
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
    public double huntSpeed = 4.6;
    public int huntPatience = 120;
    public int stareLimit = 60;
    public boolean lethal = true;
    private boolean stale = false;

    public MimicEntity(EntityType<? extends PathfinderMob> type, Level level) {
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
        if (flag(F_SLEEP)) return "ceiling_hang";
        if (flag(F_SIT)) return "sit";
        if (flag(F_EAT)) return "eat";
        if (flag(F_DRAG)) return flag(F_MOVING) ? "drag_walk" : "drag";
        if (flag(F_CLIMB)) return "climb_wall";
        if (flag(F_SPREAD)) return "spread";
        if (flag(F_CRAWL)) return "crawl";
        if (flag(F_FROZEN)) return "freeze";
        if (flag(F_DUCK)) return flag(F_MOVING) ? "duck_walk" : "duck_idle";
        if (flag(F_MOVING)) return flag(F_RUN) ? "run" : (getState() == STALK || getState() == SEARCH || flag(F_STALKWALK)) ? "stalk" : "walk";
        return "idle";
    }

    @Override
    public boolean isDucking() { return flag(F_DUCK) && !flag(F_CRAWL); }

    public boolean headTracks() { return !flag(F_NOLOOK) && !flag(F_CRAWL) && !flag(F_SLEEP) && !flag(F_EAT) && !flag(F_CLIMB) && !flag(F_SPREAD); }

    public boolean cameraOnly() { return false; }

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
        if (s != SLEEP) setFlag(F_SLEEP, false);
        if (s == HUNT || s == SEARCH) {
            setFlag(F_EAT, false);
            setFlag(F_DRAG, false);
            setFlag(F_CLIMB, false);
            setFlag(F_SPREAD, false);
        }
        if (s == IDLE || s == WATCH || s == TALK || s == AMBUSH || s == SLEEP) halt();
    }

    public void act(String anim, int lockTicks, String sound) {
        playAction(anim);
        actionLock = Math.max(actionLock, lockTicks);
        if (sound != null) say(sound, 1.0f, 1.0f);
    }

    public void say(String sound, float vol, float pitch) {
        SoundEvent ev = ModSounds.get(sound);
        level().playSound(null, getX(), getY() + 2.0, getZ(), ev, SoundSource.HOSTILE, vol, pitch * (0.95f + random.nextFloat() * 0.1f));
    }

    public void emerge(int then) {
        afterEmerge = then;
        setState(EMERGE);
        act("emerge", 48, "mm.emerge");
    }

    public void drop(int then) {
        afterEmerge = then;
        setState(EMERGE);
        stateTicks = 20;
        act("drop", 30, "mm.drop");
    }

    public void vanish() {
        if (getState() == VANISH) return;
        setState(VANISH);
        act("vanish", 24, "mm.climb");
    }

    /** hanging from the rafters, asleep */
    public void sleep() {
        setState(SLEEP);
        setFlag(F_SLEEP, true);
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
    }

    public void wake() {
        setFlag(F_SLEEP, false);
        setNoGravity(false);
        afterEmerge = SCRIPT;
        setState(EMERGE);
        stateTicks = 20;
        act("drop", 30, "mm.drop");
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

    /** development only: an upper limit on its speed, for recordings */
    public static double speedCap = 0;

    private void go(Vec3 t, double mps) {
        if (speedCap > 0) mps = Math.min(mps, speedCap);
        if (repath-- <= 0 || getNavigation().isDone()) {
            getNavigation().moveTo(t.x, t.y, t.z, speedMod(mps));
            repath = 6;
        } else {
            getNavigation().setSpeedModifier(speedMod(mps));
        }
        setFlag(F_MOVING, true);
        setFlag(F_RUN, mps >= 4.0);
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

    /** is the flashlight beam on me */
    public boolean litBy(ServerPlayer p) {
        if (p == null || !Story.lightOn(p)) return false;
        Vec3 eye = p.getEyePosition();
        Vec3 chest = position().add(0, 1.8, 0);
        double dist = eye.distanceTo(chest);
        if (dist > 24) return false;
        Vec3 dir = chest.subtract(eye).normalize();
        if (dir.dot(p.getViewVector(1f)) < 0.94) return false;
        return los(eye, chest) || los(eye, position().add(0, 2.8, 0));
    }

    /** can the player see me at all */
    public boolean observedBy(ServerPlayer p) {
        if (p == null) return false;
        if (litBy(p)) return true;
        Vec3 eye = p.getEyePosition();
        Vec3 chest = position().add(0, 1.8, 0);
        double dist = eye.distanceTo(chest);
        if (dist > 40) return false;
        Vec3 dir = chest.subtract(eye).normalize();
        if (dir.dot(p.getViewVector(1f)) < 0.6) return false;
        if (!los(eye, chest)) return false;
        if (dist < 4) return true;
        int light = level().getMaxLocalRawBrightness(blockPosition().above());
        return light >= 6;
    }

    /** can I see the player: far, if they carry a light; close, in the dark; barely, in the corn */
    public boolean sees(ServerPlayer p) {
        if (p == null || Story.hiddenIn(p) != null) return false;
        Vec3 eye = position().add(0, 2.6, 0);
        Vec3 t = p.getEyePosition();
        double d = eye.distanceTo(t);
        double range = Story.lightOn(p) ? 30 : p.isCrouching() ? 7 : 11;
        int light = level().getMaxLocalRawBrightness(p.blockPosition());
        if (light >= 9) range = Math.max(range, 18);
        if (Story.inCover(p)) range = 3.2;
        if (d > range) return false;
        if (d > 4) {
            Vec3 look = Vec3.directionFromRotation(0, yHeadRot);
            Vec3 dir = t.subtract(eye).normalize();
            if (look.dot(dir) < -0.1 && !Story.lightOn(p)) return false;
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
        if (getState() != SLEEP) updatePosture();
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
                    Story.onMimicGone(this);
                    discard();
                }
            }
            case SLEEP -> {
                halt();
                setDeltaMovement(Vec3.ZERO);
            }
            case WATCH -> tickWatch(p);
            case STALK -> tickStalk(p);
            case HUNT -> tickHunt(p);
            case SEARCH -> tickSearch(p);
            case AMBUSH -> tickAmbush(p);
            case TALK -> {
                halt();
                if (p != null) getLookControl().setLookAt(p.getX(), p.getEyeY(), p.getZ(), 20, 20);
            }
            case SCRIPT -> tickScript(p);
            default -> {
                halt();
                if (!story && p != null && stateTicks > 40) setState(STALK);
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
        if (observedBy(p)) stareTicks++;
        // caught in the beam, it is gone into the dark - unless it is curious
        if (stareTicks > stareLimit || distanceTo(p) < 8) {
            if (actionLock == 0 && random.nextInt(3) == 0) act("twitch", 14, "mm.click");
            if (actionLock == 0) vanish();
        }
        Vec3 heard = Story.heard(position());
        if (heard != null && distanceTo(p) < 14) beginSearch(heard);
    }

    private void tickStalk(ServerPlayer p) {
        if (p == null) { halt(); return; }
        double d = distanceTo(p);
        boolean lit = litBy(p);
        if (lit) {
            // the light finds it: it freezes, head cocked... then comes
            halt();
            setFlag(F_FROZEN, true);
            getLookControl().setLookAt(p.getX(), p.getEyeY(), p.getZ(), 60, 60);
            stareTicks++;
            unseenTicks = 0;
            if (stareTicks == stareLimit - 12 && actionLock == 0) act("twitch", 12, "mm.twitch");
            if (stareTicks > stareLimit) {
                setFlag(F_FROZEN, false);
                act("roar", 30, "mm.roar");
                Story.fx(p, "shake", 30);
                beginHunt();
                stateTicks = -28;
            }
        } else {
            setFlag(F_FROZEN, false);
            unseenTicks++;
            if (stareTicks > 0 && unseenTicks > 30) stareTicks = Math.max(0, stareTicks - 2);
            if (sees(p) && d < 9) {
                act("roar", 24, "mm.roar");
                beginHunt();
                stateTicks = -20;
                return;
            }
            if (d > 2.6) {
                go(p.position(), d > 12 ? 3.8 : 2.2);
            } else {
                halt();
                if (grabTicks < 0 && actionLock == 0) {
                    if (lethal) startGrab();
                    else vanish();
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
        Vec3 heard = Story.heard(position());
        if (sees) {
            lastSeen = p.position();
            lostTicks = 0;
        } else {
            lostTicks++;
            if (heard != null) {
                lastSeen = heard;
                lostTicks = Math.min(lostTicks, 30);
            }
        }
        if (Story.hiddenIn(p) != null && lostTicks > 10) {
            beginSearch(lastSeen != null ? lastSeen : p.position());
            return;
        }
        if (lostTicks > huntPatience) {
            beginSearch(lastSeen != null ? lastSeen : position());
            return;
        }
        double sp = huntSpeed + (d > 16 ? 1.4 : d > 9 ? 0.6 : d < 4 ? -0.3 : 0);
        Vec3 target = lostTicks < 30 || lastSeen == null ? p.position() : lastSeen;
        go(target, sp);
        getLookControl().setLookAt(p.getX(), p.getEyeY(), p.getZ(), 40, 40);
        if (d < 2.0 && grabTicks < 0 && (sees || d < 1.3)) startGrab();
        if (stateTicks % 60 == 30) say(random.nextInt(3) == 0 ? "mm.click" : random.nextBoolean() ? "mm.growl" : "mm.breath", 1.1f, 1.0f);
        checkStuck(p);
    }

    private void tickSearch(ServerPlayer p) {
        if (p == null) { vanish(); return; }
        if (sees(p) && Story.hiddenIn(p) == null && stateTicks > 20) {
            act("roar", 22, "mm.roar");
            beginHunt();
            stateTicks = -20;
            return;
        }
        Vec3 heard = Story.heard(position());
        if (heard != null && searchPhase <= 1 && (lastSeen == null || heard.distanceToSqr(lastSeen) > 9)) {
            lastSeen = heard;
            searchPhase = 0;
        }
        switch (searchPhase) {
            case 0 -> {
                Vec3 t = lastSeen != null ? lastSeen : position();
                go(t, 2.4);
                if (position().distanceToSqr(t) < 3.5 || stateTicks > 200) {
                    halt();
                    act(random.nextBoolean() ? "sniff" : "listen", 70, random.nextBoolean() ? "mm.sniff" : "mm.click");
                    searchPhase = 1;
                    searchWait = 70;
                    searchList.clear();
                    for (House.Hide h : House.HIDES) {
                        Vec3 w = Story.worldCenter(h.pos);
                        if (w.distanceToSqr(position()) < 14 * 14 && Math.abs(w.y - getY()) < 3) searchList.add(h);
                    }
                    searchList.sort((a, b) -> Double.compare(Story.worldCenter(a.pos).distanceToSqr(position()), Story.worldCenter(b.pos).distanceToSqr(position())));
                    int max = story ? Story.searchDepth() : 2;
                    House.Hide mine = Story.hiddenIn(p);
                    if (mine != null && (Story.seenHiding() || mine.pos.distSqr(BlockPos.containing(Story.lpos(position()))) < 12 * 12)) {
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
            case 2 -> {
                House.Hide h = searchList.get(0);
                Vec3 front = Story.worldCenter(h.pos.relative(h.facing, 1));
                go(front, 2.0);
                if (position().distanceToSqr(front) < 1.5 || searchWait-- < -200) {
                    halt();
                    face(Story.worldCenter(h.pos));
                    act("lean_in", 80, "mm.breath");
                    searchPhase = 3;
                    searchWait = 84;
                    Story.onMimicEvent(this, "at_hide:" + h.id);
                }
            }
            case 3 -> {
                halt();
                House.Hide h = searchList.get(0);
                if (searchWait == 44) say("mm.sniff", 1.0f, 0.8f);
                if (--searchWait <= 0) {
                    House.Hide mine = Story.hiddenIn(p);
                    if (mine == h && Story.hideDetected(p, this)) {
                        act("grab", 16, "mm.grab");
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
                if (stateTicks % 5 == 0) {
                    Story.onMimicEvent(this, "search_done");
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
            act("scream", 20, "mm.scream");
            Story.fx(p, "shake", 20);
            beginHunt();
            stateTicks = -16;
        } else if (stateTicks > 900) {
            setFlag(F_FROZEN, false);
            vanish();
        }
    }

    private void tickScript(ServerPlayer p) {
        if (flag(F_SIT) || flag(F_EAT)) { halt(); return; }
        if (scriptTarget != null && position().distanceToSqr(scriptTarget) > 1.2) {
            go(scriptTarget, scriptSpeed);
            if (getNavigation().isDone() && stateTicks % 20 == 0) repath = 0;
        } else {
            halt();
            if (p != null && !flag(F_DRAG)) getLookControl().setLookAt(p.getX(), p.getEyeY(), p.getZ(), 20, 20);
        }
    }

    private void startGrab() {
        grabTicks = 0;
        act("grab", 18, "mm.grab");
    }

    private void tickGrab(ServerPlayer p) {
        grabTicks++;
        if (grabTicks == 7 && p != null && distanceTo(p) < 2.9) {
            if (story) Story.caught(p, this);
            else {
                p.hurt(damageSources().mobAttack(this), 12f);
                Story.fx(p, "jumpscare", 24);
                vanish();
            }
        }
        if (grabTicks > 18) grabTicks = -1;
    }

    private void checkStuck(ServerPlayer p) {
        if (stateTicks % 20 != 0) return;
        if (flag(F_MOVING) && position().distanceToSqr(lastPos) < 0.3) stuckTicks += 20;
        else stuckTicks = 0;
        lastPos = position();
        if (stuckTicks >= 60 || (getState() == HUNT && p != null && Math.abs(p.getY() - getY()) > 2.5 && getNavigation().isDone() && stateTicks > 40)) {
            stuckTicks = 0;
            Story.onMimicEvent(this, "stuck");
        }
    }

    private void updatePosture() {
        // it folds itself under anything lower than its skull
        BlockPos bp = blockPosition();
        boolean low = false;
        Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
        for (int i = 0; i <= 1 && !low; i++) {
            BlockPos c = BlockPos.containing(getX() + fwd.x * i * 0.9, getY(), getZ() + fwd.z * i * 0.9);
            for (int dy = 2; dy <= 3; dy++) {
                BlockState s = level().getBlockState(c.above(dy));
                if (!s.getCollisionShape(level(), c.above(dy)).isEmpty()) low = true;
            }
        }
        if (low) duckHold = 12;
        else if (duckHold > 0) duckHold--;
        boolean duck = duckHold > 0 && !flag(F_SIT) && getState() != EMERGE && getState() != VANISH;
        setFlag(F_DUCK, duck);
        if (duck && flag(F_MOVING) && low) crawlTicks++;
        else if (!low) crawlTicks = Math.max(0, crawlTicks - 4);
        boolean wantCrawl = crawlTicks > 26 && (getState() == HUNT || getState() == SCRIPT || getState() == SEARCH);
        if (wantCrawl != flag(F_CRAWL) && (wantCrawl || crawlTicks == 0)) setFlag(F_CRAWL, wantCrawl);
    }

    private void openDoors() {
        if (!flag(F_MOVING)) return;
        Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
        for (int i = 0; i <= 1; i++) {
            BlockPos c = BlockPos.containing(getX() + fwd.x * i, getY() + 0.2, getZ() + fwd.z * i);
            BlockState s = level().getBlockState(c);
            if (s.getBlock() instanceof DoorBlock door && !s.getValue(DoorBlock.OPEN)) {
                if (story && Story.isLockedDoor(c)) {
                    if (stateTicks % 30 == 0) {
                        say("mm.bang", 1.0f, 1.0f);
                        act("bang", 14, null);
                    }
                    return;
                }
                door.setOpen(this, level(), s, c, true);
                say(getState() == HUNT ? "fx.door_slam" : "fx.door_creak", 0.9f, 0.9f);
                if (getState() == HUNT) act("burst", 10, null);
            }
        }
    }

    private void footsteps() {
        if (!flag(F_MOVING)) { stepTimer = 0; return; }
        if (getState() == STALK || flag(F_STALKWALK)) {
            if (++stepTimer >= 24) {
                stepTimer = 0;
                say("mm.click", 0.35f, 1.1f);
            }
            return;
        }
        int every = flag(F_RUN) ? 5 : flag(F_CRAWL) ? 7 : flag(F_DUCK) ? 12 : 14;
        if (++stepTimer >= every) {
            stepTimer = 0;
            say("mm.step", flag(F_RUN) ? 1.1f : 0.6f, flag(F_RUN) ? 1.05f : 0.85f);
        }
    }

    // ------------------------------------------------------------------ misc entity plumbing
    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(src, amount);
        if (src.is(DamageTypeTags.IS_FIRE)) return false;
        if (!level().isClientSide && src.getEntity() instanceof ServerPlayer sp && com.krolasyon.mimic.story.Story.onMimicHit(sp)) return false;
        if (!level().isClientSide && src.getEntity() instanceof ServerPlayer && actionLock == 0) {
            act("twitch", 12, "mm.twitch");
            if (!story && getState() != HUNT) beginHunt();
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
        if (story) stale = true;
    }
}
