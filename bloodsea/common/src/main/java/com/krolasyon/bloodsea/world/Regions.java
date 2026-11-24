package com.krolasyon.bloodsea.world;

/**
 * The map of the Blood Sea. The voyage starts on a raft in the calm middle - the Drift - and the sea splits four ways:
 * north the Bone Reach, east Leviathan's Deep, south the Edge of the World, west the Mirage. Each way ends in one place.
 */
public final class Regions {
    public enum Region { DRIFT, BONES, LEVIATHAN, EDGE, MIRAGE }

    /** the calm middle */
    public static final double DRIFT_R = 420;
    /** where the sea ends: an arc of waterfall this far south */
    public static final double EDGE_R = 1900;
    /** the four places where it ends */
    public static final int THRONE_X = 0, THRONE_Z = -1450;
    public static final int MAW_X = 1450, MAW_Z = 0;
    public static final int LIGHTHOUSE_X = -1450, LIGHTHOUSE_Z = 0;

    public static final int SEA = 62;     // the top of the blood
    public static final int MIN_Y = -64;

    private Regions() {}

    /** the region at a block column; borders wander a little so they do not look ruled */
    public static Region at(double x, double z) {
        double r = Math.sqrt(x * x + z * z);
        double wob = Noise.value(x, z, 160, 91) * 60;
        if (r < DRIFT_R + wob) return Region.DRIFT;
        double ang = Math.toDegrees(Math.atan2(x, -z)) + Noise.value(x, z, 260, 92) * 12; // 0 = north, 90 = east
        if (ang < 0) ang += 360;
        if (ang >= 315 || ang < 45) return Region.BONES;
        if (ang < 135) return Region.LEVIATHAN;
        if (ang < 225) return Region.EDGE;
        return Region.MIRAGE;
    }

    public static String key(Region r) { return r.name().toLowerCase(); }

    /** distance from the middle of the sea */
    public static double dist(double x, double z) { return Math.sqrt(x * x + z * z); }
}
