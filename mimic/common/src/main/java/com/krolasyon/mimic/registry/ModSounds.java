package com.krolasyon.mimic.registry;

import com.krolasyon.mimic.MimicMod;
import com.krolasyon.mimic.compat.Compat;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MimicMod.MODID);
    private static final Map<String, RegistryObject<SoundEvent>> MAP = new HashMap<>();

    /** Keep in sync with tools/gen_sounds.py */
    public static final String[] NAMES = {
            // ambience
            "amb.wind", "amb.house", "amb.barn", "amb.cellar", "amb.field", "amb.chase", "amb.heartbeat", "amb.flies", "amb.silo",
            // the thing
            "mm.step", "mm.breath", "mm.growl", "mm.roar", "mm.scream", "mm.click", "mm.sniff", "mm.eat", "mm.kill", "mm.climb",
            "mm.drop", "mm.emerge", "mm.whisper", "mm.howl", "mm.bang", "mm.twitch", "mm.grab",
            // voices: Lily
            "v.lily.vm1", "v.lily.vm2", "v.lily.vm3", "v.lily.vm4", "v.lily.r1", "v.lily.r2", "v.lily.r3", "v.lily.r4", "v.lily.r5", "v.lily.r6",
            "v.lily.r7", "v.lily.r8", "v.lily.r9", "v.lily.silo1", "v.lily.silo2", "v.lily.silo3", "v.lily.word", "v.lily.ask", "v.lily.field1", "v.lily.field2",
            "v.lily.end1", "v.lily.end2", "v.lily.hurry", "v.lily.corn", "v.lily.corn2",
            // voices: the thing wearing Lily
            "v.mlily.up1", "v.mlily.up2", "v.mlily.word_ok", "v.mlily.word_bad", "v.mlily.truck1", "v.mlily.truck_ok", "v.mlily.truck_bad",
            "v.mlily.radio_end", "v.mlily.help",
            // voices: Dad, and the thing wearing Dad
            "v.dad.tape1", "v.dad.tape2", "v.dad.tape3", "v.dad.tape4", "v.dad.machine",
            "v.mdad.phone1", "v.mdad.phone2", "v.mdad.machine2", "v.mdad.call1", "v.mdad.call2", "v.mdad.call3",
            // voices: you, and the thing wearing you
            "v.sam.loud", "v.sam.whisper", "v.sam.whisper2", "v.sam.word", "v.sam.ask", "v.sam.its_me", "v.sam.lily",
            "v.msam.loud", "v.msam.word", "v.msam.ask", "v.msam.its_me",
            // effects
            "fx.door_creak", "fx.door_slam", "fx.lock", "fx.unlock", "fx.drawer", "fx.paper", "fx.pickup", "fx.phone_ring", "fx.phone_pick",
            "fx.radio_static", "fx.radio_click", "fx.tape", "fx.light_on", "fx.light_off", "fx.light_flicker", "fx.bolt_cut", "fx.crowbar",
            "fx.bell", "fx.well", "fx.bucket", "fx.thunder", "fx.creak", "fx.thump", "fx.jumpscare", "fx.sting", "fx.chapter", "fx.gas_pour",
            "fx.engine_fail", "fx.engine_start", "fx.fire", "fx.glass", "fx.wood_break", "fx.crow", "fx.clock_tick", "fx.clock_chime",
            "fx.breath_hold", "fx.gasp", "fx.qte", "fx.choice", "fx.hay", "fx.corn", "fx.stove", "fx.water", "fx.cow", "fx.chain",
            "fx.truck_door", "fx.static", "fx.type", "fx.gore", "fx.stab", "fx.lighter", "fx.throw"
    };

    static {
        for (String n : NAMES) MAP.put(n, SOUNDS.register(n, () -> SoundEvent.createVariableRangeEvent(Compat.rl(n))));
        for (String n : ExtraVoices.NAMES) MAP.put(n, SOUNDS.register(n, () -> SoundEvent.createVariableRangeEvent(Compat.rl(n))));
    }

    public static SoundEvent get(String name) {
        RegistryObject<SoundEvent> r = MAP.get(name);
        if (r == null) throw new IllegalArgumentException("unknown sound " + name);
        return r.get();
    }

    public static boolean has(String name) { return MAP.containsKey(name); }

    private ModSounds() {}
}
