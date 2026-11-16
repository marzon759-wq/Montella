package com.krolasyon.valdoria.world.mistfall;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

/**
 * The shape of the Mistfall Vale, as pure functions of (x, z) so any column can be computed anywhere, in any order:
 * <ul>
 *   <li>terraced plateaus (flat tops, short steep steps between tiers),</li>
 *   <li>winding canyons with near-vertical purple cliffs cut deep into them, with pools and a river on their floor,</li>
 *   <li>rivers running across the plateau tops; where a river meets a lower tier or a canyon it pours over the edge
 *       as a waterfall.</li>
 * </ul>
 */
public final class MistfallTerrain {
    public static final int MIN_Y = 0;
    public static final int HEIGHT = 320;
    /** Water level on the canyon floors. */
    public static final int FLOOR_WATER = 52;

    private final ImprovedNoise plateau, detail, canyon, warp, river, strata;

    public MistfallTerrain(long seed) {
        RandomSource r = RandomSource.create(seed);
        plateau = new ImprovedNoise(r);
        detail = new ImprovedNoise(r);
        canyon = new ImprovedNoise(r);
        warp = new ImprovedNoise(r);
        river = new ImprovedNoise(r);
        strata = new ImprovedNoise(r);
    }

    private static double fbm(ImprovedNoise n, double x, double z, int octaves) {
        double sum = 0, amp = 1, norm = 0, f = 1;
        for (int i = 0; i < octaves; i++) {
            sum += n.noise(x * f, 0.5 * i, z * f) * amp;
            norm += amp;
            amp *= 0.5;
            f *= 2.03;
        }
        return sum / norm;
    }

    /** Terraced plateau height before canyons and rivers are cut in. */
    public double plateauHeight(int x, int z) {
        double n = fbm(plateau, x / 300.0, z / 300.0, 4);
        double level = 118 + n * 95;
        double step = 13;
        double base = Math.floor(level / step) * step;
        double frac = (level - base) / step;
        double t = smooth(0.8, 1.0, frac);
        double h = base + t * step + fbm(detail, x / 40.0, z / 40.0, 2) * 2.0;
        return Math.max(70, h);
    }

    /** 0 on the plateau, 1 on the canyon floor; the steep band in between is the cliff. */
    public double canyonFactor(int x, int z) {
        double wx = x + fbm(warp, x / 180.0, z / 180.0, 2) * 60;
        double wz = z + fbm(warp, z / 180.0 + 31, x / 180.0, 2) * 60;
        double a = Math.abs(fbm(canyon, wx / 420.0, wz / 420.0, 3));
        a += fbm(detail, x / 9.0, z / 9.0, 1) * 0.006; // ragged rock walls
        return smooth(0.1, 0.07, a);
    }

    /** True where a river flows (on plateaus, down cliffs, and through canyons). */
    public boolean isRiver(int x, int z) {
        double wx = x + fbm(warp, x / 90.0 + 7, z / 90.0, 2) * 25;
        double wz = z + fbm(warp, z / 90.0, x / 90.0 + 7, 2) * 25;
        return Math.abs(fbm(river, wx / 230.0, wz / 230.0, 3)) < 0.04;
    }

    /** Height of the highest solid block of the column (before trees). */
    public int solidTop(int x, int z) {
        double k = canyonFactor(x, z);
        double plat = plateauHeight(x, z);
        double floor = 47 + fbm(detail, x / 30.0, z / 30.0, 2) * 7;
        double h = Mth.lerp(Math.pow(k, 0.55), plat, floor);
        int top = (int) Math.floor(h);
        if (isRiver(x, z) && k < 0.98) top -= 3; // river channel cut into the rock
        return top;
    }

    /** Top of the water standing in this column, or -1 for none. */
    public int waterTop(int x, int z) {
        int top = solidTop(x, z);
        if (top < FLOOR_WATER) return FLOOR_WATER;
        if (isRiver(x, z) && canyonFactor(x, z) < 0.98) return top + 2;
        return -1;
    }

    /**
     * If water from a higher neighbouring river column spills over into this column, returns the y it falls from;
     * otherwise -1. This is what turns river ends at tier steps and canyon edges into waterfalls.
     */
    public int fallTop(int x, int z) {
        int own = Math.max(solidTop(x, z), waterTop(x, z));
        int best = -1;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            int nx = x + d[0], nz = z + d[1];
            if (!isRiver(nx, nz)) continue;
            int w = waterTop(nx, nz);
            if (w > own + 1 && w > best) best = w;
        }
        return best;
    }

    /** True where the column is a steep face (exposed rock instead of grass). */
    public boolean isSteep(int x, int z) {
        int h = solidTop(x, z);
        int d = 0;
        d = Math.max(d, Math.abs(solidTop(x + 2, z) - h));
        d = Math.max(d, Math.abs(solidTop(x - 2, z) - h));
        d = Math.max(d, Math.abs(solidTop(x, z + 2) - h));
        d = Math.max(d, Math.abs(solidTop(x, z - 2) - h));
        return d > 4;
    }

    /** Rock strata index for the cliff bands (0..3). */
    public int band(int x, int y, int z) {
        double s = y + strata.noise(x / 60.0, 0, z / 60.0) * 4;
        return Math.floorMod((int) Math.floor(s / 4.0), 4);
    }

    /** Deterministic per-column hash in [0,1) for scattering boulders etc. */
    public static double hash(int x, int z, int salt) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    static double smooth(double a, double b, double x) {
        double t = Mth.clamp((x - a) / (b - a), 0.0, 1.0);
        return t * t * (3 - 2 * t);
    }
}
