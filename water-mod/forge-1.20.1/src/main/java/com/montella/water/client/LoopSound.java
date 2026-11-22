package com.montella.water.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** a positioned looping ambience whose volume glides towards a target and that fades out before stopping */
public class LoopSound extends AbstractTickableSoundInstance {
    float target = 0f;
    boolean fading = false;

    LoopSound(SoundEvent ev, double x, double y, double z, float pitch) {
        super(ev, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.x = x; this.y = y; this.z = z;
        this.volume = 0.001f;
        this.pitch = pitch;
        this.attenuation = Attenuation.LINEAR;
    }

    void moveTo(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }

    @Override
    public void tick() {
        float goal = fading ? 0f : target;
        volume += (goal - volume) * 0.04f;
        if (fading && volume < 0.005f) stop();
    }

    @Override public boolean canStartSilent() { return true; }
    @Override public boolean canPlaySound() { return true; }
}
