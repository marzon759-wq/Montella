package com.krolasyon.bloodsea.world;

/** Small deterministic value noise: the same world every time, without any dependence on the game's RandomState. */
public final class Noise {
    private Noise() {}

    public static long hash(long x, long z, long seed) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ seed * 0x165667B19E3779F9L;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return h;
    }

    /** 0..1 */
    public static double rand(long x, long z, long seed) {
        return (hash(x, z, seed) >>> 11) * 0x1.0p-53;
    }

    private static double smooth(double t) { return t * t * (3 - 2 * t); }

    /** -1..1 value noise at a scale */
    public static double value(double x, double z, double scale, long seed) {
        double fx = x / scale, fz = z / scale;
        long ix = (long) Math.floor(fx), iz = (long) Math.floor(fz);
        double tx = smooth(fx - ix), tz = smooth(fz - iz);
        double a = rand(ix, iz, seed), b = rand(ix + 1, iz, seed), c = rand(ix, iz + 1, seed), d = rand(ix + 1, iz + 1, seed);
        double v = a + (b - a) * tx + (c - a) * tz + (a - b - c + d) * tx * tz;
        return v * 2 - 1;
    }

    /** a few octaves of value noise, -1..1 */
    public static double fbm(double x, double z, double scale, long seed) {
        return (value(x, z, scale, seed) * 0.6 + value(x, z, scale / 2.3, seed + 7) * 0.28 + value(x, z, scale / 5.1, seed + 13) * 0.12);
    }
}
