package com.krolasyon.hatman.registry;

import com.krolasyon.hatman.HatmanMod;
import com.krolasyon.hatman.compat.Compat;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, HatmanMod.MODID);
    private static final Map<String, RegistryObject<SoundEvent>> MAP = new HashMap<>();

    /** Keep in sync with tools/gen_sounds.py */
    public static final String[] NAMES = {
            "amb.house", "amb.rain", "amb.cellar", "amb.chase", "amb.heartbeat", "amb.generator", "amb.fire", "amb.breath",
            "hm.step", "hm.breath", "hm.growl", "hm.scream", "hm.whisper", "hm.emerge", "hm.vanish", "hm.knock", "hm.twitch",
            "hm.grab", "hm.voice", "hm.bang", "hm.search",
            "fx.static", "fx.jumpscare", "fx.thunder", "fx.creak", "fx.door_slam", "fx.door_creak", "fx.lock", "fx.unlock",
            "fx.drawer", "fx.paper", "fx.pickup", "fx.phone_ring", "fx.phone_pick", "fx.tv_on", "fx.generator_start",
            "fx.power_down", "fx.switch", "fx.bulb_pop", "fx.glass", "fx.wood_break", "fx.music_box", "fx.clock_tick",
            "fx.clock_chime", "fx.cam_on", "fx.cam_off", "fx.cam_low", "fx.keypad", "fx.safe_open", "fx.fake_steps",
            "fx.crow", "fx.sting", "fx.chapter", "fx.type", "fx.voice_ruth", "fx.thump", "fx.piano", "fx.tape"
    };

    static {
        for (String n : NAMES) MAP.put(n, SOUNDS.register(n, () -> SoundEvent.createVariableRangeEvent(Compat.rl(n))));
    }

    public static SoundEvent get(String name) {
        RegistryObject<SoundEvent> r = MAP.get(name);
        if (r == null) throw new IllegalArgumentException("unknown sound " + name);
        return r.get();
    }

    public static boolean has(String name) { return MAP.containsKey(name); }

    private ModSounds() {}
}
