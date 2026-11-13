package com.krolasyon.hatman.client;

import com.krolasyon.hatman.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

import java.util.function.Supplier;

/** Looping beds that cross-fade with where you are and how much danger you are in. */
public final class Ambient {
    private Ambient() {}

    private static class Loop extends AbstractTickableSoundInstance {
        private final Supplier<Float> target;

        Loop(String name, Supplier<Float> target) {
            super(ModSounds.get(name), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.target = target;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.001f;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
        }

        @Override
        public boolean canStartSilent() { return true; }

        @Override
        public void tick() {
            if (!ClientState.active) {
                this.stop();
                return;
            }
            float t = Math.max(0.001f, target.get());
            this.volume += (t - this.volume) * 0.06f;
        }
    }

    private static float zone(float outside, float house, float cellar) {
        return switch (ClientState.amb) {
            case "outside" -> outside;
            case "cellar" -> cellar;
            default -> house;
        };
    }

    public static void start() {
        var sm = Minecraft.getInstance().getSoundManager();
        boolean cut = false;
        sm.play(new Loop("amb.rain", () -> zone(0.9f, 0.3f, 0.05f)));
        sm.play(new Loop("amb.house", () -> ClientState.chase ? 0.15f : zone(0.12f, 0.5f, 0.2f)));
        sm.play(new Loop("amb.cellar", () -> ClientState.chase ? 0.1f : zone(0f, 0f, 0.6f)));
        sm.play(new Loop("amb.chase", () -> ClientState.chase && ClientState.cutId == null ? 0.85f : 0f));
        sm.play(new Loop("amb.heartbeat", () -> ClientState.fearSmooth > 0.2f ? Math.min(1f, ClientState.fearSmooth * 1.2f) : 0f));
        sm.play(new Loop("amb.generator", () -> ClientState.power ? zone(0f, 0.04f, 0.3f) : 0f));
        sm.play(new Loop("amb.fire", () -> ClientState.fire ? 0.8f : 0f));
        sm.play(new Loop("amb.breath", () -> ClientState.hidden ? 0.6f : 0f));
    }
}
