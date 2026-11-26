package com.krolasyon.bloodsea.client;

import com.krolasyon.bloodsea.game.SeaEvents;
import com.krolasyon.bloodsea.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * The score under the Blood Sea: a drifting day piece, an emptier night piece, and dread when the eye is open or
 * something is close. One plays at a time, they cross-fade, and the game's own music keeps quiet out here.
 */
public final class MusicClient {
    private static Track now;
    private static String want = "";
    private static int dreadHold;

    private MusicClient() {}

    /** the story can ask for dread for a while (a creature, a chase) */
    private static boolean menu;

    /** the title screen plays the drift too */
    public static void menu(boolean on) { menu = on; }

    public static void dread(int ticks) { dreadHold = Math.max(dreadHold, ticks); }

    static final class Track extends AbstractTickableSoundInstance {
        final String id;
        float target, level;

        Track(String id) {
            super(ModSounds.get("music_" + id), SoundSource.MUSIC, RandomSource.create());
            this.id = id;
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = 0.001f;
            this.target = 1f;
        }

        @Override
        public void tick() {
            float step = target > level ? 1f / 80f : 1f / 100f;
            level = level < target ? Math.min(target, level + step) : Math.max(target, level - step);
            volume = Math.max(0.001f, level * level * base(id));
            if (target == 0f && level <= 0f) stop();
        }

        void fadeOut() { target = 0f; }
    }

    static float base(String id) { return switch (id) { case "dread" -> 0.85f; case "night" -> 0.75f; default -> 0.7f; }; }

    public static void tick() {
        TitleMenu.check();
        Minecraft mc = Minecraft.getInstance();
        boolean here = mc.level != null && mc.player != null
                && (mc.level.dimension() == SeaEvents.SEA || mc.screen instanceof CutScreen);
        if (dreadHold > 0) dreadHold--;
        String w = "";
        if (here) {
            boolean close = false;
            for (var e : mc.level.getEntitiesOfClass(com.krolasyon.bloodsea.entity.SeaCreature.class, mc.player.getBoundingBox().inflate(30))) {
                // the barrel that is not a barrel stays a secret; what follows in the wake only counts once it is near
                if (e.isInvisible() || e instanceof com.krolasyon.bloodsea.entity.ChangerEntity) continue;
                if (e instanceof com.krolasyon.bloodsea.entity.WakeEntity && e.distanceToSqr(mc.player) > 12 * 12) continue;
                close = true;
                break;
            }
            // once it has turned dark, it stays dark a while: no flicking back and forth
            if (close || WatcherClient.openness(1f) > 0.3f) dreadHold = Math.max(dreadHold, 160);
            long day = mc.level.getDayTime() % 24000L;
            boolean night = day > 12800 && day < 23200;
            w = dreadHold > 0 ? "dread" : night ? "night" : "drift";
            // our own score, not the game's
            mc.getMusicManager().stopPlaying();
        }
        if (menu && mc.screen instanceof TitleMenu) {
            w = "drift";
            mc.getMusicManager().stopPlaying();
        }
        if (!w.equals(want)) {
            want = w;
            if (now != null) now.fadeOut();
            now = null;
            if (!w.isEmpty()) {
                now = new Track(w);
                mc.getSoundManager().play(now);
            }
        }
        // a track the engine dropped (a reload, a dimension change) starts again
        if (now != null && !mc.getSoundManager().isActive(now) && now.target > 0) {
            now = new Track(want);
            mc.getSoundManager().play(now);
        }
    }
}
