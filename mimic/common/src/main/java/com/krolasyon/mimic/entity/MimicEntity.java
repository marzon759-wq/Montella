package com.krolasyon.mimic.entity;

import com.krolasyon.mimic.compat.MimicBase;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

import java.util.List;

/** The creature. For now: a puppet whose looping animation can be chosen, for checking the model in the game. */
public class MimicEntity extends MimicBase {
    public static final List<String> LOOPS = List.of("idle", "walk", "stalk", "run", "listen", "mimic_talk", "search", "sniff",
            "spread", "eat", "crawl", "climb_wall", "bang", "twitch");
    public static final String[] ACTIONS = {"roar", "scream", "grab", "kill", "emerge", "death"};

    public MimicEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setPersistenceRequired();
    }

    @Override
    public String loopAnim() {
        int s = getState();
        return s >= 0 && s < LOOPS.size() ? LOOPS.get(s) : "idle";
    }

    @Override
    public String[] actionNames() { return ACTIONS; }

    public void setLoop(String name) { setStateRaw(Math.max(0, LOOPS.indexOf(name))); }

    public boolean headTracks() { return false; }
}
