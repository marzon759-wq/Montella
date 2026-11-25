package com.krolasyon.bloodsea.story;

import com.krolasyon.bloodsea.entity.CrateEntity;
import com.krolasyon.bloodsea.registry.ModEntities;
import com.krolasyon.bloodsea.world.Waves;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Keeps a few floating crates of cargo ahead of the raft: the only food out here, besides Pip's fish. */
public final class Crates {
    private Crates() {}

    public static void keep(ServerPlayer p, int n) {
        var lvl = p.serverLevel();
        var near = lvl.getEntitiesOfClass(CrateEntity.class, p.getBoundingBox().inflate(90));
        for (CrateEntity c : near) if (c.distanceTo(p) > 85) c.discard();
        if (near.size() >= n || p.getRandom().nextInt(3) != 0) return;
        Vec3 f = p.getVehicle() != null ? Vec3.directionFromRotation(0, p.getVehicle().getYRot()) : p.getViewVector(1f);
        f = new Vec3(f.x, 0, f.z).normalize();
        Vec3 side = new Vec3(-f.z, 0, f.x);
        double d = 22 + p.getRandom().nextDouble() * 24, sd = (p.getRandom().nextDouble() - 0.5) * 30;
        Vec3 at = p.position().add(f.scale(d)).add(side.scale(sd));
        CrateEntity c = ModEntities.CRATE.get().create(lvl);
        c.moveTo(at.x, Waves.MEAN - 0.35, at.z, p.getRandom().nextFloat() * 360, 0);
        lvl.addFreshEntity(c);
    }
}
