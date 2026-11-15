package com.montella.kaiju.entity;

/** Titan behaviour states (synced as an int). */
public final class S {
    public static final int IDLE = 0, ROAR = 1, CHARGE_BEAM = 2, FIRE_BEAM = 3, BEAM_END = 4,
            TAIL_WINDUP = 5, TAIL_SWING = 6, TAIL_RECOVER = 7, BITE = 8, HOLD = 9, THROW = 10, RAM = 11,
            STAGGER = 12, FLUNG = 13, FALLEN = 14, GET_UP = 15, HELD = 16, PIN = 17, PINNED = 18, PULSE = 19,
            TAKEOFF = 20, FLY = 21, LAND = 22, WING_BUFFET = 23, LIFT = 24, DEATH = 25, TEAR = 26, SHOVE = 27,
            CLASH_RECOIL = 28;

    private S() {}

    public static boolean down(int s) { return s == FLUNG || s == FALLEN || s == GET_UP || s == PINNED || s == DEATH; }

    public static boolean beam(int s) { return s == CHARGE_BEAM || s == FIRE_BEAM || s == BEAM_END; }
}
