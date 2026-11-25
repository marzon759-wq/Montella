package com.krolasyon.bloodsea.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Changer. One of the barrels on the raft is not a barrel. While you look at it, it is a barrel. While you do
 * not, it opens its eye, and it moves - a little closer each time you turn away.
 */
public class ChangerEntity extends SeaCreature {
    public static final int DISGUISE = 0, PEEK = 1, CREEP = 2, BITE = 3, FLEE = 4;
    private int unseen;

    public ChangerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level, false);
    }

    @Override public String kind() { return "changer"; }
    @Override public String[] actionNames() { return new String[]{"reveal", "bite"}; }
    @Override public String[] lookBones() { return getState() == DISGUISE ? new String[0] : new String[]{"eye"}; }

    @Override
    public String loopAnim() {
        return switch (getState()) {
            case DISGUISE -> "disguise";
            case PEEK, BITE -> "peek";
            default -> "run";
        };
    }

    @Override
    public boolean hurt(DamageSource s, float amount) {
        if (s.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(s, amount);
        if (!level().isClientSide && s.getEntity() instanceof Player && getState() != FLEE) {
            go(FLEE);
            playAction("reveal");
            sound("changer_squelch", 1.5f, 0.8f);
        }
        return false;
    }

    @Override
    protected void brain(ServerLevel lvl) {
        ServerPlayer p = nearest(48);
        if (p == null) return;
        double dist = p.distanceTo(this);
        boolean seen = dist < 40 && watchedBy(p, 0.8);
        unseen = seen ? 0 : unseen + 1;
        switch (getState()) {
            case DISGUISE -> {
                getNavigation().stop();
                if (unseen > 30) {
                    go(PEEK);
                    sound("changer_blink", 0.6f, rnd(0.9f, 1.1f));
                }
            }
            case PEEK -> {
                lookAt(p);
                if (seen) {
                    // sometimes it does not close the eye at once
                    if (stateTicks > 2 && random.nextInt(3) != 0) go(DISGUISE);
                    else if (stateTicks > 12) go(DISGUISE);
                } else if (stateTicks > 40 && dist > 2.0) go(CREEP);
                else if (stateTicks > 40 && dist <= 2.0) go(BITE);
            }
            case CREEP -> {
                lookAt(p);
                if (seen) {
                    getNavigation().stop();
                    go(DISGUISE);
                    break;
                }
                getNavigation().moveTo(p, 1.0);
                if (stateTicks % 5 == 0) sound("changer_scuttle", 0.8f, rnd(0.9f, 1.2f));
                if (dist < 1.9) {
                    getNavigation().stop();
                    go(BITE);
                }
            }
            case BITE -> {
                lookAt(p);
                if (stateTicks == 1) playAction("bite");
                if (stateTicks == 8) {
                    sound("changer_bite", 1.6f, 1f);
                    if (dist < 2.8) strike(p, 5f);
                }
                if (stateTicks > 20) {
                    go(FLEE);
                    playAction("reveal");
                }
            }
            case FLEE -> {
                Vec3 away = position().subtract(p.position());
                away = new Vec3(away.x, 0, away.z);
                if (away.lengthSqr() < 1e-3) away = new Vec3(1, 0, 0);
                Vec3 t = position().add(away.normalize().scale(8));
                if (stateTicks % 10 == 1) getNavigation().moveTo(t.x, getY(), t.z, 1.4);
                if (stateTicks % 4 == 0) sound("changer_scuttle", 1f, 1.3f);
                if (isInFluidType() || stateTicks > 90) {
                    sound("changer_squelch", 1.2f, 0.6f);
                    discard();
                }
            }
        }
    }
}
