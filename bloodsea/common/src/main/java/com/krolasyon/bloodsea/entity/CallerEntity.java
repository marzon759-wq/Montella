package com.krolasyon.bloodsea.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Name Caller. It stands in the fog and it cannot see. It hears: oars, running, splashing, the creak of a boat.
 * It goes to where it last heard you, and while it waits it says your name in voices you know.
 */
public class CallerEntity extends SeaCreature {
    public static final int RISE = 0, LISTEN = 1, STALK = 2, WHISPER = 3, LUNGE = 4, SINK = 5;
    private Vec3 heard;
    private int lastHeard = 0, nextWhisper = 200;
    private boolean screamed;

    public CallerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level, true);
        this.noCulling = true;
    }

    @Override public String kind() { return "caller"; }
    @Override public String[] actionNames() { return new String[]{"scream", "lunge"}; }
    @Override public boolean holds(String a) { return a.equals("sink") || a.equals("rise"); }
    @Override public String[] lookBones() { return new String[]{"head"}; }
    @Override public float lookWeight() { return 0.5f; }

    @Override
    public String loopAnim() {
        return switch (getState()) {
            case RISE -> "rise";
            case STALK -> "walk";
            case WHISPER -> "whisper";
            case SINK -> "sink";
            default -> "idle";
        };
    }

    /** how loud a player is being, 0..1 */
    public static double noise(Player p) {
        if (p.isCrouching()) return 0.0;
        double s = speedOf(p);
        if (p.getVehicle() != null) return s > 0.06 ? Math.min(1.0, 0.35 + s * 2.2) : 0.05;
        if (p.isSprinting()) return 1.0;
        if (!p.onGround() && p.getDeltaMovement().y > 0.08) return 0.7;
        if (p.isSwimming() || p.isInWater()) return s > 0.04 ? 0.65 : 0.1;
        return s > 0.05 ? 0.45 : 0.05;
    }

    @Override
    public void alert(Player p) {
        heard = p.position();
        lastHeard = life;
    }

    @Override
    protected void brain(ServerLevel lvl) {
        ServerPlayer p = nearest(120);
        if (p == null) return;
        double y = surfaceAt(getX(), getZ());
        double dist = p.distanceTo(this);
        if (dist < 60 * noise(p)) {
            heard = p.position();
            lastHeard = life;
        }
        boolean fresh = life - lastHeard < 12;
        switch (getState()) {
            case RISE -> {
                setPos(getX(), y, getZ());
                face(p, 2f);
                if (stateTicks == 2) sound("caller_wade", 2f, 0.7f);
                if (stateTicks > 60) go(LISTEN);
            }
            case LISTEN -> {
                setPos(getX(), y, getZ());
                if (heard != null) turnTo((float) (Math.atan2(heard.z - getZ(), heard.x - getX()) * 57.29578) - 90f, 3f);
                if (!screamed && dist < 16 && watchedBy(p, 0.93)) {
                    screamed = true;
                    playAction("scream");
                    sound("caller_scream", 4f, 0.9f);
                    heard = p.position();
                    lastHeard = life;
                } else if (fresh && heard != null && heard.distanceTo(new Vec3(getX(), heard.y, getZ())) > 2) {
                    go(STALK);
                } else if (stateTicks > nextWhisper) {
                    go(WHISPER);
                    nextWhisper = 140 + random.nextInt(160);
                } else if (life - lastHeard > 900) go(SINK);
            }
            case STALK -> {
                if (heard == null) {
                    go(LISTEN);
                    break;
                }
                double d = glide(heard.x, heard.z, screamed && life - lastHeard < 200 ? 0.16 : 0.095, y);
                if (stateTicks % 22 == 0) sound("caller_wade", 1.2f, rnd(0.8f, 1.0f));
                if (dist < 3.4) {
                    go(LUNGE);
                } else if (d < 1.2 && !fresh) go(LISTEN);
            }
            case WHISPER -> {
                setPos(getX(), y, getZ());
                lookAt(p);
                if (stateTicks == 4) {
                    sound("caller_whisper", 2.5f, rnd(0.92f, 1.04f));
                    // and close behind them, as if someone were on the raft
                    Vec3 b = p.getViewVector(1f).scale(-2.5);
                    soundAt(p, "caller_call", b.x, b.z, 0.55f, rnd(0.95f, 1.05f));
                }
                if (fresh && stateTicks > 20) go(STALK);
                else if (stateTicks > 80) go(LISTEN);
            }
            case LUNGE -> {
                face(p, 12f);
                if (stateTicks == 1) {
                    playAction("lunge");
                    sound("caller_lunge", 3f, 0.9f);
                }
                if (stateTicks == 10 && dist < 4.8) strike(p, 10f);
                if (stateTicks > 30) go(LISTEN);
            }
            case SINK -> {
                if (stateTicks > 62) discard();
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putBoolean("Screamed", screamed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        screamed = t.getBoolean("Screamed");
    }
}
