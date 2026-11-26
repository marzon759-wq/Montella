package com.krolasyon.bloodsea.client;

import com.krolasyon.bloodsea.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** What the omens look and sound like on this side: blood rain falling around you, and its hiss on the logs. */
public final class OmenClient {
    private static int rainT, rainMax;
    private static Rain sound;

    private OmenClient() {}

    public static void handle(String kind, int ticks) {
        switch (kind) {
            case "rain" -> { rainT = ticks; rainMax = ticks; }
            case "dread" -> MusicClient.dread(ticks);
            default -> {}
        }
    }

    /** 0..1, how hard it is raining now */
    public static float rain() {
        if (rainT <= 0) return 0f;
        int since = rainMax - rainT;
        return Math.min(1f, Math.min(since / 100f, rainT / 120f));
    }

    static final class Rain extends AbstractTickableSoundInstance {
        Rain() {
            super(ModSounds.get("omen_rain"), SoundSource.WEATHER, RandomSource.create());
            looping = true;
            delay = 0;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
            volume = 0.001f;
        }

        @Override
        public void tick() {
            volume = Math.max(0.001f, rain() * 0.9f);
            if (rainT <= 0) stop();
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        if (rainT <= 0) { sound = null; return; }
        rainT--;
        if (sound == null || !mc.getSoundManager().isActive(sound)) {
            sound = new Rain();
            mc.getSoundManager().play(sound);
        }
        float k = rain();
        RandomSource r = mc.level.random;
        var c = mc.gameRenderer.getMainCamera().getPosition();
        // streaks: thick dark drops coming down hard
        int n = (int) (140 * k);
        for (int i = 0; i < n; i++) {
            double x = c.x + (r.nextDouble() - 0.5) * 26, z = c.z + (r.nextDouble() - 0.5) * 26, y = c.y + 4 + r.nextDouble() * 10;
            Particle part = mc.particleEngine.createParticle(new net.minecraft.core.particles.DustParticleOptions(
                    new org.joml.Vector3f(0.38f + r.nextFloat() * 0.12f, 0.01f, 0.02f), 0.9f + r.nextFloat() * 0.6f), x, y, z, 0, 0, 0);
            if (part != null) part.setParticleSpeed(0, -0.9 - r.nextDouble() * 0.4, 0);
        }
        // and drops that land and splash on the logs
        for (int i = 0; i < (int) (30 * k); i++) {
            double x = c.x + (r.nextDouble() - 0.5) * 10, z = c.z + (r.nextDouble() - 0.5) * 10, y = c.y + 3 + r.nextDouble() * 3;
            Particle part = mc.particleEngine.createParticle(ParticleTypes.FALLING_WATER, x, y, z, 0, 0, 0);
            if (part != null) part.setColor(0.42f + r.nextFloat() * 0.1f, 0.01f, 0.02f);
        }
    }
}
