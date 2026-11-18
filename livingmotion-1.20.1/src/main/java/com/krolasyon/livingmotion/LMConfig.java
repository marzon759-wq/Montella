package com.krolasyon.livingmotion;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Client config: config/livingmotion-client.toml. Every switch is read live, no restart needed. */
public final class LMConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue REMODEL;
    public static final Map<String, ForgeConfigSpec.BooleanValue> MOBS = new LinkedHashMap<>();
    public static final ForgeConfigSpec.BooleanValue FACES;
    public static final ForgeConfigSpec.BooleanValue LOOK_AT;

    public static final ForgeConfigSpec.BooleanValue MOTION_ALL;
    public static final ForgeConfigSpec.BooleanValue MOTION_PLAYERS;
    public static final ForgeConfigSpec.DoubleValue MOTION_STRENGTH;

    public static final ForgeConfigSpec.BooleanValue FX;
    public static final ForgeConfigSpec.BooleanValue FX_IMPACT;
    public static final ForgeConfigSpec.BooleanValue FX_BLOOD;
    public static final ForgeConfigSpec.BooleanValue FX_SWING;
    public static final ForgeConfigSpec.BooleanValue FX_FOOTSTEPS;
    public static final ForgeConfigSpec.BooleanValue FX_LANDING;
    public static final ForgeConfigSpec.BooleanValue FX_DEATH;
    public static final ForgeConfigSpec.BooleanValue FX_FUSE;
    public static final ForgeConfigSpec.BooleanValue FX_BREATH;
    public static final ForgeConfigSpec.IntValue FX_RANGE;

    public static final List<String> MOB_IDS = List.of("zombie", "husk", "drowned", "skeleton", "stray",
            "wither_skeleton", "creeper", "spider", "cave_spider", "cow", "pig", "chicken");

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("GeckoLib remodels (new animated models with faces) for vanilla mobs").push("remodel");
        REMODEL = b.comment("Master switch for all GeckoLib remodels").define("enabled", true);
        for (String id : MOB_IDS) MOBS.put(id, b.define(id, true));
        FACES = b.comment("Blinking, squinting, angry brows and moving mouths/jaws").define("faces", true);
        LOOK_AT = b.comment("Remodelled mobs turn their heads to look around / at targets").define("headLook", true);
        b.pop();

        b.comment("Procedural motion for every other living entity (modded mobs included)").push("motion");
        MOTION_ALL = b.comment("Hit recoil, attack lunge, breathing and landing squash for all other mobs").define("enabled", true);
        MOTION_PLAYERS = b.comment("Also apply it to players").define("players", false);
        MOTION_STRENGTH = b.defineInRange("strength", 1.0, 0.0, 3.0);
        b.pop();

        b.comment("Particle effects").push("effects");
        FX = b.comment("Master switch for all effects").define("enabled", true);
        FX_IMPACT = b.comment("Sparks and material bits when something is hit").define("impact", true);
        FX_BLOOD = b.comment("Red bits for flesh mobs (off = sparks only)").define("blood", true);
        FX_SWING = b.comment("Swipe arc when a hostile mob attacks").define("swing", true);
        FX_FOOTSTEPS = b.comment("Dust kicked up by running mobs").define("footsteps", true);
        FX_LANDING = b.comment("Dust ring when a mob lands from a fall").define("landing", true);
        FX_DEATH = b.comment("Soul / smoke burst on death").define("death", true);
        FX_FUSE = b.comment("Smoke and sparks from a hissing creeper").define("creeperFuse", true);
        FX_BREATH = b.comment("Breath clouds in cold biomes").define("breath", true);
        FX_RANGE = b.comment("Max distance (blocks) for effects").defineInRange("range", 32, 8, 128);
        b.pop();
        SPEC = b.build();
    }

    private LMConfig() {}

    /** Safe read: the config may not be loaded yet during very early rendering. */
    public static boolean on(ForgeConfigSpec.BooleanValue v) {
        try { return v.get(); } catch (Exception e) { return true; }
    }

    public static boolean mob(String id) {
        ForgeConfigSpec.BooleanValue v = MOBS.get(id);
        return on(REMODEL) && (v == null || on(v));
    }

    public static double strength() {
        try { return MOTION_STRENGTH.get(); } catch (Exception e) { return 1.0; }
    }

    public static int range() {
        try { return FX_RANGE.get(); } catch (Exception e) { return 32; }
    }
}
