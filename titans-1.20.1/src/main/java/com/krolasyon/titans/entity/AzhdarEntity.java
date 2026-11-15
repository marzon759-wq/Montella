package com.krolasyon.titans.entity;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Azhdar: a three-headed storm hydra. A head that takes enough damage is torn off and slowly grows back. */
public class AzhdarEntity extends TitanEntity {
    private static final float HEAD_HP = 60;
    private final float[] hp = {HEAD_HP, HEAD_HP, HEAD_HP}, g = {1, 1, 1}, gO = {1, 1, 1};
    private final int[] delay = new int[3];

    public AzhdarEntity(EntityType<? extends AzhdarEntity> type, Level level) { super(type, level, true); }

    @Override
    public void tick() {
        System.arraycopy(g, 0, gO, 0, 3);
        if (!level().isClientSide) {
            boolean ch = false;
            for (int h = 0; h < 3; h++) {
                if (g[h] >= 1 || isDeadOrDying()) continue;
                if (delay[h] > 0) { delay[h]--; continue; }
                g[h] = Math.min(1, g[h] + 1 / 260f);
                ch = true;
                if (g[h] >= 1) { hp[h] = HEAD_HP; snd(SoundEvents.ENDER_DRAGON_GROWL, 12, 1.2f); }
            }
            if (ch) entityData.set(HEADS, new Vector3f(g[0], g[1], g[2]));
        } else {
            Vector3f v = entityData.get(HEADS);
            g[0] = v.x; g[1] = v.y; g[2] = v.z;
            for (int h = 0; h < 3; h++) if (gO[h] > 0.9f && g[h] < 0.1f) severFx(h);
        }
        super.tick();
    }

    private void severFx(int h) {
        Vec3 c = toWorld(rig.neck[h][2]);
        SHAKER.shake(c.x, c.y, c.z, 0.8f);
        for (int i = 0; i < 40; i++)
            fx.add(Fx.dust(c.x, c.y, c.z, random.nextGaussian() * 0.7, random.nextGaussian() * 0.5 + 0.3, random.nextGaussian() * 0.7, 1.5f + random.nextFloat() * 2.5f, 30 + random.nextInt(20)).tint(0.45f, 0.08f, 0.5f));
    }

    private int active() { int n = 0; for (int h = 0; h < 3; h++) if (g[h] >= 1) n++; return n; }

    @Override public float headGrowth(int h, float pt) { return Mth.lerp(pt, gO[h], g[h]); }
    @Override public boolean headActive(int h) { return g[h] >= 1; }
    @Override protected float beamFactor() { return active() / 3f; }

    @Override
    public int biteHead() {
        int s = getSeq() % 3;
        for (int i = 0; i < 3; i++) if (g[(s + i) % 3] >= 1) return (s + i) % 3;
        return 1;
    }

    @Override
    public int randomHead() {
        int s = random.nextInt(3);
        for (int i = 0; i < 3; i++) if (g[(s + i) % 3] >= 1) return (s + i) % 3;
        return s;
    }

    @Override
    public void damageHead(int h, float amount, TitanEntity from) {
        if (level().isClientSide || g[h] < 1 || isDeadOrDying()) return;
        hp[h] -= amount;
        if (hp[h] > 0 || active() <= 1) return;
        g[h] = 0;
        delay[h] = 70;
        entityData.set(HEADS, new Vector3f(g[0], g[1], g[2]));
        snd(SoundEvents.ENDER_DRAGON_HURT, 16, 0.5f);
        snd(SoundEvents.PHANTOM_DEATH, 14, 0.5f);
        Vec3 dir = from != null ? position().subtract(from.position()).multiply(1, 0, 1).normalize() : forward().scale(-1);
        if (getState() != HELD && getState() != PIN) impact(from, 0.7f, dir, 30);
        else titanHurt(from, 30);
    }

    @Override
    public boolean hurtPart(TitanPart part, DamageSource src, float amt) {
        if (part.kind == TitanPart.HEAD || part.kind == TitanPart.NECK) damageHead(part.index, amt, null);
        return super.hurtPart(part, src, amt);
    }
}
