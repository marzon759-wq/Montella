package com.krolasyon.bloodsea.entity;

import com.krolasyon.bloodsea.compat.CreatureBase;
import com.krolasyon.bloodsea.registry.ModSounds;
import com.krolasyon.bloodsea.world.SeaTime;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Pip. A black cat with yellow-green eyes who was on the raft when you woke. She stays near you, sleeps on the
 * blanket, washes, stretches, rubs against your legs, purrs when you stroke her and brings you the fish she hooks out
 * of the blood. She is frightened of the things in the sea and she tells you when they are near.
 */
public class PipEntity extends CreatureBase {
    public static final int IDLE = 0, WANDER = 1, FOLLOW = 2, SLEEP = 3, GROOM = 4, STRETCH = 5, RUB = 6, FISH = 7, CARRY = 8,
            DROP = 9, EAT = 10, PETTED = 11, HISS = 12, SCARED = 13, RIDE = 14, MEOW = 15, GOFISH = 16;
    private static final Set<String> HOLD = Set.of("stretch", "meow", "drop", "swipe", "jump");
    public boolean scripted = false;
    public int bond = 0;
    private int stateT, nextGift = 1800, nextMeow = 300, petCool;
    private Vec3 goal;
    private float hunger;

    public PipEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setInvulnerable(true);
    }

    @Override public String kind() { return "pip"; }
    @Override public String[] actionNames() { return new String[]{"meow"}; }
    @Override public boolean holds(String a) { return HOLD.contains(a); }
    @Override protected void registerGoals() {}
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean hurt(DamageSource s, float a) { return s.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(s, a); }

    @Override
    public String loopAnim() {
        return switch (getState()) {
            case WANDER, FOLLOW, GOFISH -> getDeltaMovement().horizontalDistanceSqr() > 0.0004 ? "walk" : "stand";
            case SLEEP -> "sleep";
            case GROOM -> "groom";
            case STRETCH -> "stretch";
            case RUB -> "rub";
            case FISH -> "swipe";
            case CARRY -> "carry";
            case DROP -> "drop";
            case EAT -> "eat";
            case PETTED -> "petted";
            case HISS -> "hiss";
            case SCARED -> "scared";
            case RIDE -> SeaTime.isDay(level()) ? "sit" : "sleep";
            default -> "sit";
        };
    }

    void go(int s) {
        setState(s);
        stateT = 0;
    }

    private RaftEntity raft() {
        var l = level().getEntitiesOfClass(RaftEntity.class, getBoundingBox().inflate(6));
        return l.isEmpty() ? null : l.get(0);
    }

    private ServerPlayer owner() {
        ServerPlayer best = null;
        double bd = 48 * 48;
        for (ServerPlayer p : ((ServerLevel) level()).players()) {
            double d = p.distanceToSqr(this);
            if (d < bd && !p.isSpectator()) { bd = d; best = p; }
        }
        return best;
    }

    /** somewhere on the deck, away from the edge */
    private Vec3 onDeck(RaftEntity r, double lx, double lz) {
        Vec3 v = r.local(Mth.clamp(lx, -1.9, 1.9), RaftEntity.TOP, Mth.clamp(lz, -1.9, 1.9));
        return v;
    }

    private void walkTo(Vec3 g, double speed) {
        goal = g;
        getMoveControl().setWantedPosition(g.x, g.y, g.z, speed);
    }

    private boolean arrived() { return goal == null || position().distanceTo(new Vec3(goal.x, getY(), goal.z)) < 0.35; }

    @Override
    public InteractionResult mobInteract(Player p, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (scripted) return InteractionResult.PASS;
        ItemStack it = p.getItemInHand(hand);
        if (com.krolasyon.bloodsea.compat.Compat.edible(it) && getState() != EAT) {
            if (!p.getAbilities().instabuild) it.shrink(1);
            bond += 2;
            go(EAT);
            getLookControl().setLookAt(p);
            sound("pip_chirp", 0.8f, 1.1f);
            return InteractionResult.CONSUME;
        }
        if (it.isEmpty() && petCool <= 0) {
            bond += 1;
            petCool = 60;
            if (isPassenger()) stopRiding();
            go(PETTED);
            getLookControl().setLookAt(p);
            sound("pip_purr", 0.9f, 1f);
            if (p instanceof ServerPlayer sp) com.krolasyon.bloodsea.story.Story.onPet(sp);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private void sound(String n, float v, float pitch) {
        level().playSound(null, getX(), getY() + 0.3, getZ(), ModSounds.get(n), SoundSource.NEUTRAL, v, pitch * (0.95f + random.nextFloat() * 0.1f));
    }

    public void meow() {
        playAction("meow");
        sound("pip_meow", 1f, 1f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        this.setYBodyRot(getYRot());
        if (scripted) {
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        stateT++;
        if (petCool > 0) petCool--;
        RaftEntity r = raft();
        ServerPlayer o = owner();
        if (r == null || o == null) return;
        // off the deck (pushed, or the raft moved from under her): back onto the blanket
        if (!isPassenger() && (Math.abs(getX() - r.getX()) > 2.3 || Math.abs(getZ() - r.getZ()) > 2.3 || getY() < r.getY() - 0.3)) {
            Vec3 b = r.local(-1.4, RaftEntity.TOP + 0.05, -1.45);
            teleportTo(b.x, b.y, b.z);
            setDeltaMovement(Vec3.ZERO);
        }
        // the raft is being rowed: she rides, on the blanket
        boolean moving = r.getControllingPassenger() != null && r.getDeltaMovement().horizontalDistanceSqr() > 0.0009;
        if (moving && !isPassenger()) {
            startRiding(r, true);
            go(RIDE);
        }
        if (isPassenger()) {
            if (r.getControllingPassenger() == null && stateT > 40) {
                stopRiding();
                Vec3 b = r.local(-1.4, RaftEntity.TOP + 0.05, -1.45);
                teleportTo(b.x, b.y, b.z);
                go(IDLE);
            } else if (getState() != RIDE && getState() != HISS && getState() != SCARED) go(RIDE);
        }
        // the things in the sea
        var near = level().getEntitiesOfClass(SeaCreature.class, getBoundingBox().inflate(28), c -> !c.scripted);
        if (!near.isEmpty() && getState() != HISS && getState() != SCARED) {
            SeaCreature c = near.get(0);
            getLookControl().setLookAt(c);
            go(c.distanceTo(this) < 12 ? SCARED : HISS);
            sound("pip_hiss", 1.2f, 1f);
        }
        if ((getState() == HISS || getState() == SCARED)) {
            getNavigation().stop();
            if (!near.isEmpty()) getLookControl().setLookAt(near.get(0));
            if (near.isEmpty() && stateT > 60) go(IDLE);
            if (stateT % 40 == 0 && getState() == HISS) sound("pip_hiss", 1f, 1f);
            return;
        }
        if (isPassenger()) return;
        if (--nextMeow <= 0) {
            nextMeow = 400 + random.nextInt(900);
            if (getState() != SLEEP) meow();
        }
        double d = distanceTo(o);
        switch (getState()) {
            case IDLE -> {
                if (stateT > 60 + random.nextInt(200)) chooseIdle(r, o);
            }
            case WANDER, FOLLOW -> {
                if (arrived() || stateT > 140) go(IDLE);
            }
            case SLEEP -> {
                if (stateT > 600 && SeaTime.isDay(level())) go(STRETCH);
            }
            case GROOM -> { if (stateT > 160) go(IDLE); }
            case STRETCH -> { if (stateT > 60) go(IDLE); }
            case RUB -> {
                if (d > 1.0) walkTo(o.position(), 0.6);
                if (stateT % 30 == 0) sound("pip_purr", 0.6f, 1f);
                if (stateT > 120) go(IDLE);
            }
            case PETTED -> {
                getLookControl().setLookAt(o);
                if (stateT > 70) go(random.nextInt(3) == 0 ? RUB : IDLE);
            }
            case EAT -> { if (stateT > 60) go(IDLE); }
            case GOFISH -> { if (arrived() || stateT > 140) go(FISH); }
            case FISH -> {
                // at the edge, crouched over the blood
                if (stateT == 26) sound("pip_chirp", 0.6f, 1.3f);
                if (stateT > 36) {
                    go(CARRY);
                    sound("blood_splash", 0.6f, 1.4f);
                }
            }
            case CARRY -> {
                walkTo(o.position(), 0.7);
                if (d < 1.3 || stateT > 200) go(DROP);
            }
            case DROP -> {
                getLookControl().setLookAt(o);
                if (stateT == 12) {
                    ItemStack fish = new ItemStack(Items.COD);
                    com.krolasyon.bloodsea.compat.Compat.named(fish, Component.translatable("item.bloodsea.pip_fish"));
                    ItemEntity ie = new ItemEntity(level(), getX(), getY() + 0.2, getZ(), fish);
                    ie.setDeltaMovement(Vec3.ZERO);
                    level().addFreshEntity(ie);
                    meow();
                    com.krolasyon.bloodsea.story.Story.onGift(o);
                }
                if (stateT > 34) go(IDLE);
            }
            default -> go(IDLE);
        }
        // gifts: more often when you are hungry, and when she loves you
        if (--nextGift <= 0 && SeaTime.isDay(level()) && getState() == IDLE) {
            nextGift = Math.max(900, 3000 - bond * 120 - (o.getFoodData().getFoodLevel() < 8 ? 1200 : 0)) + random.nextInt(600);
            double side = random.nextBoolean() ? 1 : -1;
            walkTo(onDeck(r, side * 2.0, random.nextDouble() * 2 - 1), 0.6);
            go(GOFISH);
        }
    }

    private void chooseIdle(RaftEntity r, ServerPlayer o) {
        int k = random.nextInt(10);
        boolean night = !SeaTime.isDay(level());
        if (night && k < 5) {
            Vec3 b = r.local(-1.4, RaftEntity.TOP, -1.45);
            walkTo(b, 0.5);
            go(SLEEP);
        } else if (k < 3) {
            walkTo(onDeck(r, random.nextDouble() * 3.6 - 1.8, random.nextDouble() * 3.6 - 1.8), 0.5);
            go(WANDER);
        } else if (k < 5 && distanceTo(o) > 2) {
            walkTo(o.position(), 0.6);
            go(FOLLOW);
        } else if (k < 6) go(GROOM);
        else if (k < 7) go(STRETCH);
        else if (k < 8 && distanceTo(o) < 4) go(RUB);
        else go(IDLE);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("Bond", bond);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        bond = t.getInt("Bond");
    }
}
