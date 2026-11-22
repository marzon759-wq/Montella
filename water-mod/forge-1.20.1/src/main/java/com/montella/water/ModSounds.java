package com.montella.water;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    private ModSounds() {}
    public static final DeferredRegister<SoundEvent> REG = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, WaterMod.ID);
    public static final RegistryObject<SoundEvent> WATERFALL = reg("waterfall");
    public static final RegistryObject<SoundEvent> STREAM = reg("stream");
    public static final RegistryObject<SoundEvent> SPLASH_SMALL = reg("splash_small");
    public static final RegistryObject<SoundEvent> SPLASH_BIG = reg("splash_big");

    private static RegistryObject<SoundEvent> reg(String name) {
        return REG.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(WaterMod.ID, name)));
    }
}
