package com.krolasyon.kaiju;

import net.minecraftforge.common.ForgeConfigSpec;

/** kaiju.toml (common config); the static mirrors are what the code reads */
public final class KaijuConfig {
    public static boolean griefing = true, flyingDebris = true, sameOwnerFights = true;
    public static int blocksPerTick = 2500;
    public static double scale = 8.0;

    static final ForgeConfigSpec SPEC;
    static final ForgeConfigSpec.BooleanValue GRIEF, DEBRIS, SAME_OWNER;
    static final ForgeConfigSpec.IntValue BUDGET;
    static final ForgeConfigSpec.DoubleValue SCALE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        GRIEF = b.comment("Kaiju break blocks (also needs the mobGriefing game rule)").define("griefing", true);
        DEBRIS = b.comment("Smashed blocks sometimes fly off as falling blocks").define("flyingDebris", true);
        SAME_OWNER = b.comment("Kaiju hatched by the same player still fight each other").define("sameOwnerFights", true);
        BUDGET = b.comment("Maximum blocks changed per server tick by all kaiju together").defineInRange("blocksPerTick", 2500, 100, 50000);
        SCALE = b.comment("Size of a newly hatched kaiju: 8 = film scale (about 50 blocks tall)").defineInRange("scale", 8.0, 0.5, 20.0);
        SPEC = b.build();
    }

    static void load() {
        griefing = GRIEF.get();
        flyingDebris = DEBRIS.get();
        sameOwnerFights = SAME_OWNER.get();
        blocksPerTick = BUDGET.get();
        scale = SCALE.get();
    }
}
