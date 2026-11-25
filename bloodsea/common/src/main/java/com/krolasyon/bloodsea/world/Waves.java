package com.krolasyon.bloodsea.world;

/**
 * The swell of the Blood Sea: a sum of travelling waves, the same on the client (which draws it and rocks the raft on
 * it) and the server. Time is in seconds of game time. Heights are blocks above the mean surface.
 */
public final class Waves {
    /** the mean surface the waves are drawn around; kept a little above the blood blocks so the flat fluid never shows */
    public static final double MEAN = Regions.SEA + 1.56;
    // direction x, direction z, amplitude, wavelength, phase
    private static final double[][] W = {
            {0.82, 0.57, 0.25, 26.0, 0.0},
            {-0.35, 0.94, 0.14, 14.0, 1.7},
            {0.99, -0.14, 0.08, 7.3, 4.1},
            {-0.71, -0.70, 0.045, 4.1, 2.2},
    };
    private static final double G = 9.0;

    private Waves() {}

    /** height of the surface above MEAN */
    public static double height(double x, double z, double t) {
        double h = 0;
        for (double[] w : W) {
            double k = 2 * Math.PI / w[3];
            double om = Math.sqrt(G * k) * 0.55;
            h += w[2] * Math.sin(k * (w[0] * x + w[1] * z) - om * t + w[4]);
        }
        return h;
    }

    /** slope of the surface: {dh/dx, dh/dz} */
    public static double[] slope(double x, double z, double t) {
        double sx = 0, sz = 0;
        for (double[] w : W) {
            double k = 2 * Math.PI / w[3];
            double om = Math.sqrt(G * k) * 0.55;
            double c = w[2] * k * Math.cos(k * (w[0] * x + w[1] * z) - om * t + w[4]);
            sx += c * w[0];
            sz += c * w[1];
        }
        return new double[]{sx, sz};
    }

    public static double time(net.minecraft.world.level.Level lvl, float pt) { return (lvl.getGameTime() + pt) / 20.0; }
}
