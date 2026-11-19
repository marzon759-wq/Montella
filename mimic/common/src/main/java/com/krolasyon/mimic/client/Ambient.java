package com.krolasyon.mimic.client;

import com.krolasyon.mimic.registry.ModSounds;
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

    private static float zone(float outside, float house, float barn, float cellar) {
        return switch (ClientState.amb) {
            case "outside", "field" -> outside;
            case "cellar", "silo" -> cellar;
            case "barn", "shed" -> barn;
            default -> house;
        };
    }

    public static void start() {
        var sm = Minecraft.getInstance().getSoundManager();
        sm.play(new Loop("amb.wind", () -> ClientState.dawn ? 0.3f : zone(0.85f, 0.25f, 0.4f, 0.1f)));
        sm.play(new Loop("amb.field", () -> ClientState.amb.equals("field") ? 0.7f : 0f));
        sm.play(new Loop("amb.house", () -> ClientState.chase ? 0.15f : zone(0.05f, 0.55f, 0.1f, 0.2f)));
        sm.play(new Loop("amb.barn", () -> ClientState.chase ? 0.15f : zone(0f, 0f, 0.6f, 0f)));
        sm.play(new Loop("amb.cellar", () -> ClientState.chase ? 0.1f : zone(0f, 0f, 0f, 0.6f)));
        sm.play(new Loop("amb.flies", () -> ClientState.amb.equals("shed") ? 0.6f : 0f));
        sm.play(new Loop("amb.chase", () -> ClientState.chase && ClientState.cutId == null ? 0.85f : 0f));
        sm.play(new Loop("amb.heartbeat", () -> ClientState.fearSmooth > 0.2f || Qte.type != null ? Math.min(1f, Math.max(Qte.type != null ? 0.8f : 0f, ClientState.fearSmooth * 1.2f)) : 0f));
    }
}
