package com.krolasyon.livingmotion.client.model;

import software.bernie.geckolib.core.animation.RawAnimation;

/** Per-entity client state that the animation controllers and the procedural face share. */
public final class Track {
    // action controller (attack / hurt one-shots)
    public int lastHurt;
    public boolean wasSwinging;
    public int lastSwingTime;
    public int lastAttackTick;
    public RawAnimation action;
    public float actionStart, actionLen;
    // timed idle variants (grazing ...)
    public int lastTick = -1;
    public int variantUntil;
    public RawAnimation variant;
    // face
    public float attackFlash;      // age at which the last attack started (for the mouth)
    public float hurtFlash;
    public int seed;
}
