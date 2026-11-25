package com.krolasyon.bloodsea.registry;

import com.krolasyon.bloodsea.BloodSea;
import com.krolasyon.bloodsea.compat.Compat;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

/** The sounds of the things in the Blood Sea (made by tools/gen_creature_sounds.py). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BloodSea.MODID);
    public static final String[] NAMES = {
            "wake_swim", "wake_lunge", "wake_surface",
            "gatherer_rattle", "gatherer_step", "gatherer_roar", "gatherer_assemble", "gatherer_swipe",
            "caller_whisper", "caller_call", "caller_scream", "caller_wade", "caller_lunge",
            "changer_blink", "changer_scuttle", "changer_bite", "changer_squelch",
            "lev_heartbeat", "lev_roar", "lev_rise", "lev_bite",
            "watcher_stare", "pip_meow", "pip_chirp", "pip_purr", "pip_hiss", "pip_scream", "blood_splash", "monitor_beep", "storm",
            "stomach", "heartbeat_near"};
    private static final Map<String, RegistryObject<SoundEvent>> MAP = new HashMap<>();

    static {
        for (String n : NAMES) MAP.put(n, SOUNDS.register(n, () -> SoundEvent.createVariableRangeEvent(Compat.rl(n))));
        for (String k : com.krolasyon.bloodsea.story.Lines.ALL.keySet()) {
            String n = com.krolasyon.bloodsea.story.Lines.sound(k);
            MAP.put(n, SOUNDS.register(n, () -> SoundEvent.createVariableRangeEvent(Compat.rl(n))));
        }
    }

    public static SoundEvent get(String n) { return MAP.get(n).get(); }

    private ModSounds() {}
}
