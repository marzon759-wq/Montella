package com.montella.water;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    private Config() {}

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_DENSITY;
    public static final ForgeConfigSpec.IntValue SCAN_RADIUS;
    public static final ForgeConfigSpec.BooleanValue WATERFALL_SOUNDS;
    public static final ForgeConfigSpec.BooleanValue STREAM_SOUNDS;
    public static final ForgeConfigSpec.DoubleValue SOUND_VOLUME;
    public static final ForgeConfigSpec.BooleanValue SPLASHES;
    public static final ForgeConfigSpec.BooleanValue WAKES;
    public static final ForgeConfigSpec.BooleanValue RAIN_RIPPLES;

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.DoubleValue CURRENT_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue WATERFALL_PUSH;

    static {
        ForgeConfigSpec.Builder c = new ForgeConfigSpec.Builder();
        c.comment("Montella Water Physics - visual and sound settings").push("client");
        PARTICLE_DENSITY = c.comment("Amount of spray, mist and foam (0 = off)").defineInRange("particleDensity", 1.0, 0.0, 3.0);
        SCAN_RADIUS = c.comment("How far (blocks) waterfalls are searched around the player").defineInRange("waterfallScanRadius", 32, 8, 64);
        WATERFALL_SOUNDS = c.define("waterfallSounds", true);
        STREAM_SOUNDS = c.define("streamSounds", true);
        SOUND_VOLUME = c.defineInRange("soundVolume", 1.0, 0.0, 2.0);
        SPLASHES = c.comment("Big splashes when things fall into water").define("splashes", true);
        WAKES = c.comment("Ripples and foam trails behind things moving through water").define("wakes", true);
        RAIN_RIPPLES = c.comment("Raindrops make rings on water").define("rainRipples", true);
        c.pop();
        CLIENT_SPEC = c.build();

        ForgeConfigSpec.Builder s = new ForgeConfigSpec.Builder();
        s.comment("Montella Water Physics - gameplay").push("physics");
        CURRENT_STRENGTH = s.comment("How strongly flowing water carries players, mobs, items and boats (0 = vanilla)").defineInRange("currentStrength", 1.0, 0.0, 3.0);
        WATERFALL_PUSH = s.comment("How hard falling water pushes things down (0 = vanilla)").defineInRange("waterfallPush", 1.0, 0.0, 3.0);
        s.pop();
        COMMON_SPEC = s.build();
    }
}
