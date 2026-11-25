package com.krolasyon.bloodsea.story;

import com.krolasyon.bloodsea.entity.SeaCreature;
import com.krolasyon.bloodsea.game.Creatures;
import net.minecraft.server.level.ServerPlayer;

/** Story hooks into the creatures: one swims past, one is let off the leash after its cutscene. */
final class Creatures2 {
    private Creatures2() {}

    static void wakePast(ServerPlayer p) { Creatures.spawn(p.serverLevel(), p, "wake"); }

    static void release(ServerPlayer p, String kind) {
        for (SeaCreature c : p.serverLevel().getEntitiesOfClass(SeaCreature.class, p.getBoundingBox().inflate(80)))
            if (c.kind().equals(kind)) {
                c.scripted = false;
                c.setAnim("");
                c.removeTag("keep");
                c.go(1);
            }
    }
}
