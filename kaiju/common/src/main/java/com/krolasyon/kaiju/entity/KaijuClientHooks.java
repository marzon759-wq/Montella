package com.krolasyon.kaiju.entity;

import java.util.function.Consumer;

/** client-only behaviour plugged in by the client setup, so the entity never loads client classes on a server */
public final class KaijuClientHooks {
    public static Consumer<KaijuEntity> TICK = e -> {};

    static void tick(KaijuEntity e) { TICK.accept(e); }
}
