package com.krolasyon.mimic.client;

import com.krolasyon.mimic.MimicMod;
import com.krolasyon.mimic.compat.Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * The voices follow the game language: with Turkish selected, every voiced line "v.x" is played from its Turkish
 * recording "tr.v.x" (when there is one). Subtitles already follow the language through the lang files.
 */
public final class VoiceLang {
    private VoiceLang() {}

    static String lang() {
        try {
            return Minecraft.getInstance().getLanguageManager().getSelected();
        } catch (Throwable t) {
            return "en_us";
        }
    }

    public static SoundInstance remap(SoundInstance s) {
        if (s == null) return null;
        ResourceLocation loc = s.getLocation();
        if (!MimicMod.MODID.equals(loc.getNamespace()) || !loc.getPath().startsWith("v.")) return s;
        String l = lang();
        if (l == null || !l.startsWith("tr")) return s;
        var sm = Minecraft.getInstance().getSoundManager();
        ResourceLocation tr = Compat.rl("tr." + loc.getPath());
        if (sm.getSoundEvent(tr) == null) return s;
        s.resolve(sm);
        return new SimpleSoundInstance(tr, s.getSource(), s.getVolume(), s.getPitch(), RandomSource.create(), s.isLooping(), s.getDelay(),
                s.getAttenuation(), s.getX(), s.getY(), s.getZ(), s.isRelative());
    }
}
