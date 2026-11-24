package com.krolasyon.bloodsea.game;

import com.krolasyon.bloodsea.net.Msg;
import com.krolasyon.bloodsea.net.Net;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Four ways the voyage ends, one at the far end of each region: the Ossuary Throne (bone), the Maw (leviathan),
 * the waterfall at the end of the sea (edge) and the lighthouse of home (mirage).
 */
public final class Endings {
    public static final String[] ALL = {"bone", "leviathan", "edge", "mirage"};
    private static final Set<UUID> ACTIVE = new HashSet<>();

    private Endings() {}

    public static boolean active(ServerPlayer p) { return ACTIVE.contains(p.getUUID()); }

    public static void begin(ServerPlayer p, String id) {
        if (!ACTIVE.add(p.getUUID())) return;
        SeaEvents.log("[ENDING] {} reached {}", p.getName().getString(), id);
        // hold the body still while the ending plays
        p.stopRiding();
        p.setDeltaMovement(Vec3.ZERO);
        p.setNoGravity(true);
        p.setInvulnerable(true);
        p.serverLevel().playSound(null, p.blockPosition(), id.equals("edge") ? SoundEvents.ELYTRA_FLYING : SoundEvents.WARDEN_EMERGE, SoundSource.AMBIENT, 1f, 0.6f);
        CompoundTag d = p.getPersistentData();
        CompoundTag seen = d.getCompound("bloodsea_endings");
        seen.putBoolean(id, true);
        d.put("bloodsea_endings", seen);
        Net.toPlayer(p, Msg.of("ending").s("id", id).i("seen", seen.getAllKeys().size()));
    }

    public static void finish(ServerPlayer p) {
        if (!ACTIVE.remove(p.getUUID())) return;
        p.setNoGravity(false);
        p.setInvulnerable(false);
        p.fallDistance = 0;
        SeaEvents.leave(p);
    }
}
