package com.krolasyon.bloodsea.story;

import java.util.HashMap;
import java.util.Map;

import static com.krolasyon.bloodsea.story.Cut.v;

/**
 * Every cutscene. The frame is the raft's deck: x to the left, y up from the deck, z toward the bow; the stern (where
 * the oar is) is at z = -2.5 and Pip's blanket at (-1.4, 0, -1.45). The endings at the hospital, inside the stomach
 * and on the grey sea have their own sets (see Sets).
 */
public final class Cuts {
    public static final Map<String, Cut> ALL = new HashMap<>();

    private static Cut cut(String id) {
        Cut c = new Cut(id);
        ALL.put(id, c);
        return c;
    }

    static {
        // ============================================================== waking on the raft (third person)
        cut("wake")
                .at(0, s -> { s.actorAt(0, 0.02, -1.4, 0); s.actor("wake"); s.pipAt(-6, -20, 0, 0); s.time(1000, 1000, 1); })
                // the raft alone on the blood, the camera coming down out of the sky
                .shot(90, v(26, 22, 22), v(0, 0, 0), v(9, 5, 7), v(0, 0, 0), 55, 60)
                // close on him, face down on the logs; a finger moves
                .shot(60, v(1.1, 0.45, 0.9), v(0, 0.2, 0.3), v(0.9, 0.5, 0.7), v(0, 0.25, 0.3), 50, 46)
                // he pushes himself up, coughing
                .shot(80, v(2.8, 1.1, -0.5), v(0, 0.6, -0.4), v(2.6, 1.3, -0.9), v(0, 0.8, -0.6))
                .say(100, "wake.1")
                // on his knees, looking at his hands
                .shot(80, v(0.1, 1.05, 0.6), v(0, 0.9, -1.4), v(0.2, 1.0, 0.45), v(0, 0.85, -1.4), 55, 48)
                .say(168, "wake.2")
                // the hand to the head; he gets up and looks out at the sea
                .shot(90, v(-1.2, 2.3, -3.8), v(1.5, 1.2, 6), v(-0.8, 2.2, -3.6), v(2.5, 1.3, 8))
                // wide: one man standing on a raft in an endless red sea
                .shot(70, v(7, 3, 5), v(0, 1.1, -1.2), v(5, 3.4, -6), v(0, 1.1, -1.2), 60, 62)
                // a meow behind him: Pip
                .at(388, s -> { s.pipAt(-1.4, 0.02, -1.45, 60); s.pip("stand"); s.pipMeow(); })
                .at(392, s -> { s.actorAt(0, 0.02, -1.4, 90); s.actor("look_around"); })
                .shot(80, v(-0.3, 0.55, -0.5), v(-1.4, 0.3, -1.45), v(-0.2, 0.5, -0.6), v(-1.4, 0.35, -1.45), 50, 44)
                .at(415, s -> s.actor("kneel_reach"))
                .say(420, "wake.3")
                .at(450, s -> { s.pipWalk(-0.7, 0.02, -1.4, 90, 30); s.pip("walk"); })
                .at(480, s -> s.pip("rub"))
                .shot(110, v(1.4, 1.2, -0.6), v(-0.6, 0.5, -1.4), v(1.2, 1.1, -0.9), v(-0.6, 0.45, -1.4))
                .say(485, "wake.4")
                .at(540, s -> { s.actor("pet"); s.pip("petted"); s.sound("pip_purr", 1f, 1f); })
                .say(550, "wake.5")
                .shot(90, v(-0.2, 0.7, 0.4), v(-0.4, 0.6, -1.4), v(-0.3, 0.6, 0.2), v(-0.5, 0.55, -1.4), 48, 42)
                // the map tied to the mast
                .at(620, s -> { s.actorAt(0.2, 0.02, 0.6, 0); s.actor("talk"); s.pip("sit"); })
                .shot(90, v(1.5, 1.7, 0.4), v(0.1, 1.4, 1.3), v(1.3, 1.6, 0.6), v(0.1, 1.35, 1.3))
                .say(625, "wake.6")
                .shot(90, v(0, 1.8, 4.2), v(0, 1.3, 0.4), v(0.4, 1.7, 3.8), v(0, 1.3, 0.4))
                .say(735, "wake.7")
                .len(830);

        // ============================================================== the rule: the blood eats what it touches
        cut("rule")
                .at(0, s -> { s.actorAt(0, 0.02, -1.4, 0); s.actor("stand"); s.pipAt(1.0, 0.02, 0.4, -90); s.pip("walk");
                    s.pipWalk(2.0, 0.02, 0.6, -90, 30); s.drop(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD), 2.3, 0.1, 0.9); })
                .at(32, s -> s.pip("swipe"))
                .hold(70, v(1.2, 0.6, -0.8), v(2.3, 0.2, 0.9))
                .say(30, "rule.1")
                .at(40, s -> { s.actor("recoil"); })
                .at(70, s -> { s.splash(3.1, -0.3, 1.0); s.puff(3.1, -0.2, 1.0, 20); s.sound("blood_splash", 1.2f, 1.3f); })
                .at(72, s -> s.lvl().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, s.p.getBoundingBox().inflate(12)).forEach(e -> e.discard()))
                .shot(80, v(1.6, 1.4, 0.2), v(3.2, -0.3, 1.0), v(1.7, 1.2, 0.3), v(3.2, -0.3, 1.0), 50, 40)
                .at(90, s -> { s.puff(3.1, -0.1, 1.0, 30); s.pip("hiss"); })
                .say(130, "rule.2")
                .at(150, s -> { s.pipWalk(-1.4, 0.02, -1.45, 180, 30); s.pip("walk"); })
                .shot(110, v(0.6, 1.75, 0.8), v(-0.1, 1.55, -1.4), v(0.4, 1.7, 0.5), v(-0.1, 1.55, -1.4), 50, 44)
                .at(180, s -> { s.actor("talk"); s.pip("sit"); })
                .say(190, "rule.3")
                .len(270);

        // ============================================================== Pip's gift
        cut("gift")
                .at(0, s -> { s.actorAt(0, 0.02, -1.0, 0); s.actor("stand"); s.pipAt(2.0, 0.02, 1.4, 150); s.pip("carry"); s.pipWalk(0.15, 0.02, -0.1, 180, 70); })
                .shot(80, v(1.2, 0.35, -0.4), v(1.4, 0.3, 1.0), v(0.9, 0.35, -0.5), v(0.3, 0.3, 0.1), 55, 50)
                .say(20, "gift.1")
                .at(72, s -> s.pip("drop"))
                .at(80, s -> s.actor("kneel_reach"))
                .at(86, s -> s.drop(com.krolasyon.bloodsea.compat.Compat.named(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COD),
                        net.minecraft.network.chat.Component.translatable("item.bloodsea.pip_fish")), 0.1, 0.2, -0.4))
                .shot(90, v(-0.7, 0.55, 0.7), v(0.1, 0.3, -0.3), v(-0.6, 0.5, 0.6), v(0.1, 0.35, -0.3), 46, 40)
                .say(110, "gift.2")
                .at(170, s -> { s.actor("pet"); s.pip("petted"); s.sound("pip_purr", 1f, 1f); })
                .shot(100, v(1.6, 1.3, 0.8), v(0, 0.6, -0.6), v(1.4, 1.2, 0.5), v(0, 0.6, -0.6))
                .say(195, "gift.3")
                .len(280);

        // ============================================================== the moon bleeds
        cut("moon")
                .at(0, s -> { s.time(11800, 14200, 220); s.actorAt(0, 0.02, -1.0, 0); s.actor("stand"); s.pipAt(-1.4, 0.02, -1.45, 0); s.pip("sit"); })
                .shot(130, v(-18, 3, -14), v(0, 1, 0), v(-12, 2, -16), v(0, 2, 0), 60, 64)
                .at(120, s -> s.actor("look_up"))
                .shot(110, v(0.6, 0.9, 1.2), v(-0.1, 1.9, -1.0), v(0.5, 0.8, 1.0), v(-0.1, 1.95, -1.0))
                .say(140, "moon.1")
                .at(200, s -> { s.actor("kneel_reach"); s.pip("look_up"); })
                .shot(90, v(-2.2, 0.9, 0.4), v(-0.8, 0.5, -1.2), v(-2.0, 0.9, 0.2), v(-0.8, 0.5, -1.2))
                .say(245, "moon.2")
                .len(330);

        // ============================================================== something under the raft
        cut("bump")
                .at(0, s -> { s.actorAt(0, 0.02, -1.0, 0); s.actor("sit"); s.pipAt(-1.4, 0.02, -1.45, 0); s.pip("sleep"); })
                .hold(40, v(3, 1.2, 3), v(0, 0.6, -0.8))
                .at(25, s -> { s.shake(18); s.sound("blood_splash", 2f, 0.5f); s.splash(2.8, -0.3, 0.5); })
                .at(32, s -> { s.pip("hiss"); s.actor("recoil"); s.sound("pip_hiss", 1.4f, 1f); })
                .shot(70, v(-0.5, 0.45, 0.4), v(-1.4, 0.35, -1.45), v(-0.5, 0.45, 0.2), v(-1.4, 0.35, -1.45), 48, 44)
                .say(60, "bump.1")
                .shot(90, v(1.0, 0.6, 3.6), v(2.6, -0.3, 0.5), v(1.6, 0.4, 3.4), v(3.2, -0.3, -1.5), 50, 50)
                .at(120, s -> { s.shake(10); s.sound("wake_swim", 2f, 0.6f); })
                .len(200);

        // ============================================================== morning
        cut("dawn")
                .at(0, s -> { s.time(22600, 24800, 220); s.actorAt(0, 0.02, -1.2, 0); s.actor("sit"); s.pipAt(-0.6, 0.02, -1.2, 90); s.pip("knead"); })
                .shot(140, v(20, 4, 20), v(0, 0.5, 0), v(10, 3, 12), v(0, 0.5, 0), 60, 58)
                .shot(110, v(0.9, 0.9, 0.3), v(-0.3, 0.6, -1.2), v(0.8, 0.85, 0.2), v(-0.3, 0.55, -1.2))
                .say(160, "dawn.1")
                .len(260);

        // ============================================================== memory: the night he left (heard, not seen)
        cut("mem1")
                .black(0, 300)
                .at(0, s -> { s.actorAt(0, 0.02, -1.2, 0); s.actor("sit"); s.pipAt(-0.6, 0.02, -1.2, 90); s.pip("sleep"); s.time(2000, 2000, 1); })
                .hold(300, v(0.8, 0.9, 0.5), v(-0.2, 0.6, -1.2))
                .say(20, "mem1.1")
                .say(110, "mem1.2")
                .say(185, "mem1.3")
                .shot(150, v(0.9, 0.95, 0.6), v(-0.2, 0.65, -1.2), v(0.7, 0.85, 0.4), v(-0.4, 0.5, -1.2), 50, 42)
                .say(310, "mem1.4")
                .at(360, s -> s.pip("petted"))
                .say(370, "mem1.5")
                .len(470);

        // ============================================================== the sun opens its eye
        cut("eye")
                .at(0, s -> { s.actorAt(0, 0.02, -1.0, 0); s.actor("stand"); s.pipAt(-1.4, 0.02, -1.45, 0); s.pip("sit"); })
                .shot(90, v(0.8, 1.6, 1.4), v(0, 1.6, -1.0), v(0.7, 1.5, 1.2), v(0, 1.7, -1.0))
                .say(30, "eye.1")
                .at(40, s -> { s.actor("look_up"); s.pip("look_up"); })
                .at(80, s -> s.eye(900))
                // straight up at it
                .shot(120, v(0, 1.6, -0.8), v(0, 60, 2), v(0, 1.6, -0.8), v(0, 60, 1), 70, 56)
                .say(170, "eye.2")
                .shot(90, v(-0.4, 0.4, 0.3), v(-0.2, 2.0, -1.0), v(-0.4, 0.35, 0.2), v(-0.2, 2.1, -1.0), 60, 60)
                .len(300);

        // ============================================================== the Bone Reach, and what gets up out of it
        cut("bones")
                .at(0, s -> { s.actorAt(0, 0.02, 1.4, 0); s.actor("look_around"); s.pipAt(-1.4, 0.02, -1.45, 0); s.pip("hiss"); })
                .shot(110, v(-0.6, 2.4, -1.5), v(0, 1, 30), v(0.4, 2.4, -1.0), v(-8, 2, 30))
                .say(30, "bones.1")
                .shot(80, v(0.8, 1.6, 3.0), v(0, 1.6, 1.4), v(0.6, 1.6, 2.8), v(0, 1.6, 1.4))
                .say(115, "bones.2")
                .at(180, s -> { s.spawn("gatherer", 3, -1.0, 24, 180); s.anim("gatherer", "assemble"); s.keep("gatherer"); })
                .shot(150, v(0.8, 2.2, 1.0), v(3, 4, 24), v(0.5, 2.0, 0.6), v(3, 6, 24), 50, 42)
                .at(300, s -> { s.anim("gatherer", "walk"); s.act("gatherer", "roar"); s.actor("recoil"); })
                .shot(80, v(-2.5, 1.0, -3.0), v(2, 3, 20), v(-2.8, 1.2, -3.4), v(2, 3, 18))
                .say(320, "bones.3")
                .len(430);

        // ============================================================== the Leviathan rises
        cut("lev")
                .at(0, s -> { s.actorAt(0, 0.02, 0.6, 0); s.actor("stand"); s.pipAt(-0.7, 0.02, 0.2, 0); s.pip("scared"); s.sound("lev_heartbeat", 3f, 0.8f); })
                .at(10, s -> { s.spawn("leviathan", 0, -0.2, 34, 180); s.anim("leviathan", "rise"); s.sound("lev_rise", 6f, 0.8f); s.shake(80); })
                .shot(130, v(-2.5, 1.0, -4.0), v(0, 6, 30), v(-3.0, 1.4, -5.5), v(0, 9, 30), 60, 70)
                .say(140, "lev.1")
                .at(160, s -> { s.act("leviathan", "roar"); s.sound("lev_roar", 8f, 0.85f); s.shake(60); s.actor("recoil"); })
                .shot(100, v(0.9, 1.7, 2.4), v(0, 1.6, 0.6), v(0.7, 1.6, 2.2), v(0, 1.6, 0.6), 50, 44)
                .at(260, s -> { s.actorAt(-0.3, 0.02, 0.4, 0); s.actor("kneel_reach"); })
                .shot(110, v(-1.8, 0.7, -0.4), v(0, 7, 30), v(-1.5, 0.6, -0.6), v(0, 8, 30), 55, 50)
                .say(270, "lev.2")
                .len(380);

        // ============================================================== memory: the storm
        cut("mem2")
                .black(0, 280)
                .at(0, s -> { s.sound("storm", 2f, 1f); s.actorAt(0, 0.02, -1.2, 0); s.actor("hold_head"); })
                .hold(280, v(0.8, 1.7, 0.6), v(0, 1.7, -1.2))
                .say(20, "mem2.1")
                .say(95, "mem2.2")
                .at(110, s -> s.sound("pip_scream", 1.2f, 1f))
                .say(150, "mem2.3")
                .shot(140, v(0.9, 1.8, 0.7), v(0, 1.7, -1.2), v(0.7, 1.75, 0.5), v(0, 1.7, -1.2), 50, 44)
                .say(290, "mem2.4")
                .len(430);

        // ============================================================== the voice in the fog
        cut("voice")
                .at(0, s -> { s.actorAt(0, 0.02, 0.8, 0); s.actor("look_around"); s.pipAt(-1.4, 0.02, -1.45, 0); s.pip("look_up");
                    s.spawn("caller", -9, -0.2, 30, 180); s.anim("caller", "whisper"); })
                .shot(120, v(0.5, 1.7, -0.6), v(-9, 4, 30), v(0.4, 1.7, -0.3), v(-9, 5, 30), 60, 48)
                .say(30, "voice.1")
                .shot(110, v(0.7, 1.7, 2.6), v(0, 1.65, 0.8), v(0.6, 1.65, 2.3), v(0, 1.65, 0.8))
                .say(125, "voice.2")
                .at(200, s -> s.actor("stand"))
                .shot(90, v(-1.2, 1.0, -1.0), v(-9, 4, 30), v(-1.1, 1.0, -0.6), v(-9, 4, 30), 45, 35)
                .say(235, "voice.3")
                .len(320);

        // ============================================================== Pip is taken
        cut("taken")
                .at(0, s -> { s.actorAt(0, 0.02, -1.3, 0); s.actor("sit"); s.pipAt(-1.4, 0.02, -1.45, 0); s.pip("sit");
                    s.spawn("caller", 3.6, -7.5, 2.2, -90); s.anim("caller", "idle"); })
                .shot(90, v(-3.5, 2.0, 3.5), v(-0.6, 0.5, -1.2), v(-3.2, 2.1, 3.0), v(-0.6, 0.5, -1.2))
                .say(20, "taken.1")
                .at(80, s -> { s.pip("stand"); })
                .at(100, s -> { s.pip("walk"); s.pipWalk(1.9, 0.02, 1.8, -60, 70); })
                .shot(90, v(-0.6, 0.35, -0.4), v(0.8, 0.3, 0.6), v(-0.2, 0.35, -0.2), v(1.9, 0.3, 1.8), 48, 44)
                .at(150, s -> { s.actor("kneel_reach"); s.pip("look_up"); })
                .say(160, "taken.2")
                .shot(80, v(2.4, 0.5, 2.6), v(0, 0.8, -1.1), v(2.3, 0.5, 2.4), v(0, 0.8, -1.1), 50, 46)
                // the hand
                .at(240, s -> { s.moveThing("caller", 3.4, -1.0, 2.2, -90, 14); s.act("caller", "lunge"); s.sound("caller_lunge", 3f, 0.9f); s.splash(3, -0.2, 2); })
                .at(252, s -> { s.pip("struggle"); s.sound("pip_scream", 1.6f, 1f); s.pipWalk(2.9, 1.0, 2.2, -90, 8); })
                .at(256, s -> { s.actorWalk(1.4, 0.02, 1.0, 40, 12); s.actor("reach_scream"); })
                .say(258, "taken.3")
                .shot(80, v(-1.5, 2.6, -1.8), v(2.4, 0.4, 2.0), v(-1.3, 2.5, -1.4), v(2.6, 0.2, 2.1), 58, 52)
                .at(276, s -> { s.pipWalk(3.4, -3.0, 2.3, -90, 30); s.moveThing("caller", 3.6, -8, 2.2, -90, 34); })
                .at(300, s -> { s.splash(3.2, -0.2, 2.2); s.sound("blood_splash", 2f, 0.6f); })
                .at(318, s -> { s.pipGone(); s.gone("caller"); })
                .shot(70, v(2.0, 1.2, 1.1), v(3.2, -0.3, 2.3), v(2.0, 1.0, 1.2), v(3.2, -0.3, 2.3), 45, 38)
                .at(330, s -> { s.actorAt(1.3, 0.02, 0.9, 40); s.actor("sob"); })
                .shot(130, v(0.1, 0.6, 1.9), v(1.3, 0.7, 0.9), v(0.2, 0.55, 1.8), v(1.3, 0.65, 0.9), 50, 42)
                .say(380, "taken.4")
                .shot(180, v(1.6, 2.2, 0.0), v(1.3, 0.4, 0.9), v(2, 14, -2), v(1.3, 0.4, 0.9), 50, 60)
                .say(500, "taken.5")
                .len(660);

        // ============================================================== ending: the lighthouse (the light, the fall)
        cut("mirage")
                .at(0, s -> { s.actorAt(0, 0.02, 1.6, 0); s.actor("stand"); })
                .shot(120, v(0.8, 1.8, -2.0), v(0, 6, 40), v(0.5, 1.8, -1.0), v(0, 8, 40))
                .say(30, "mirage.1")
                .at(110, s -> s.actor("talk"))
                .shot(100, v(0.8, 1.7, 3.4), v(0, 1.6, 1.6), v(0.6, 1.7, 3.0), v(0, 1.6, 1.6))
                .say(120, "mirage.2")
                .at(220, s -> s.actor("hold_head"))
                .shot(90, v(0.4, 1.7, 2.6), v(0, 1.65, 1.6), v(0.3, 1.7, 2.3), v(0, 1.65, 1.6), 50, 40)
                .say(225, "mirage.3")
                .white(250)
                .at(290, s -> s.actor("collapse"))
                .len(330);

        // ============================================================== ending: the hospital (a dream inside the dream)
        cut("hospital")
                .black(0, 50)
                .at(0, s -> { s.actorAt(0, 0.62, 0, 180); s.actor("lie_back"); s.spawn("pip", -0.3, 0.62, -1.0, 0); s.anim("pip", "sleep"); })
                .at(0, s -> s.sound("monitor_beep", 0.8f, 1f)).at(25, s -> s.sound("monitor_beep", 0.8f, 1f))
                .at(50, s -> s.sound("monitor_beep", 0.8f, 1f)).at(75, s -> s.sound("monitor_beep", 0.8f, 1f))
                .hold(130, v(0, 1.5, 0.3), v(0, 4, 0.2))
                .say(40, "hosp.1")
                .at(120, s -> s.actor("sit_up_bed"))
                .shot(120, v(1.6, 1.7, -1.6), v(0, 1.2, 0.2), v(1.3, 1.6, -1.4), v(0, 1.4, 0.2))
                .say(200, "hosp.2")
                .say(250, "hosp.3")
                .at(330, s -> { s.anim("pip", "stand"); s.soundAt("pip_meow", -0.3, 0.8, -1.0, 1f, 1f); })
                .shot(120, v(-0.6, 1.1, -2.3), v(-0.3, 0.8, -1.0), v(-0.4, 1.1, -2.1), v(-0.2, 0.9, -0.6), 50, 46)
                .say(360, "hosp.4")
                .at(400, s -> { s.moveThing("pip", -0.1, 0.65, -0.4, 180, 30); s.anim("pip", "walk"); })
                .at(430, s -> { s.anim("pip", "petted"); s.actor("bed_talk"); s.sound("pip_purr", 1f, 1f); })
                .shot(140, v(1.4, 1.5, -1.2), v(0, 1.2, 0.1), v(1.2, 1.4, -1.0), v(0, 1.2, 0.1))
                .say(440, "hosp.5")
                .shot(180, v(0.6, 1.55, -0.9), v(0, 1.45, 0.25), v(0.45, 1.5, -0.7), v(0, 1.45, 0.25), 46, 38)
                .say(515, "hosp.6")
                .say(690, "hosp.7")
                .shot(120, v(2.4, 2.2, -2.4), v(0, 1.0, 0), v(2.2, 2.2, -2.2), v(0, 1.0, 0))
                .say(770, "hosp.8")
                .len(880);

        // ============================================================== ending: the stomach (what was really there)
        cut("stomach")
                .at(0, s -> { s.actorAt(0, 0.02, 0, 180); s.actor("cradle"); s.sound("stomach", 2f, 0.8f); s.sound("lev_heartbeat", 3f, 0.7f); })
                .at(80, s -> s.sound("lev_heartbeat", 3f, 0.7f)).at(160, s -> s.sound("lev_heartbeat", 3f, 0.7f))
                .at(240, s -> s.sound("lev_heartbeat", 3f, 0.7f)).at(320, s -> s.sound("lev_heartbeat", 3f, 0.7f))
                .shot(140, v(0.4, 1.3, -0.9), v(0, 1.0, 0.1), v(0.4, 1.3, -1.2), v(0, 1.0, 0.1), 40, 46)
                .say(60, "stomach.1")
                .shot(260, v(0.6, 1.6, -1.6), v(0, 0.8, 0), v(1.5, 4.5, -6.5), v(0, 1.0, 0), 50, 70)
                .len(420);

        // ============================================================== ending: the throne of bones
        cut("throne")
                .at(0, s -> { s.actorAt(0, 0.02, 1.8, 0); s.actor("stand"); })
                .shot(120, v(1.2, 1.8, -1.5), v(0, 8, 60), v(0.8, 2.2, -0.6), v(0, 10, 60))
                .say(30, "throne.1")
                .shot(120, v(0.8, 1.7, 3.6), v(0, 1.6, 1.8), v(0.6, 1.7, 3.2), v(0, 1.6, 1.8), 50, 44)
                .say(130, "throne.2")
                .at(250, s -> { s.spawn("gatherer", 6, -0.5, 14, 200); s.anim("gatherer", "idle"); s.act("gatherer", "roar"); })
                .shot(130, v(-2.2, 1.4, -1.0), v(4, 4, 14), v(-2.0, 1.6, -0.6), v(3, 5, 12))
                .say(255, "throne.3")
                .at(380, s -> { s.actor("kneel_reach"); s.moveThing("gatherer", 1.5, -0.5, 4.5, 200, 120); })
                .shot(160, v(1.6, 2.0, 0.4), v(0, 1.2, 1.8), v(4, 6, -4), v(0, 1.2, 1.8), 50, 62)
                .say(400, "throne.4")
                .len(560);

        // ============================================================== ending: into the Maw
        cut("maw")
                .at(0, s -> { s.actorAt(0, 0.02, 1.6, 0); s.actor("stand"); s.sound("lev_heartbeat", 4f, 0.7f); })
                .shot(130, v(0.6, 1.8, -2.4), v(0, 4, 60), v(0.4, 1.8, -1.0), v(0, 3, 60))
                .say(40, "maw.1")
                .at(140, s -> s.sound("lev_roar", 6f, 0.7f))
                .black(170, 260)
                .hold(130, v(0.8, 1.7, 3.0), v(0, 1.6, 1.6))
                .len(260);

        // ============================================================== ending: the grey sea, and the boat that comes
        cut("rescue")
                .black(0, 40)
                .at(0, s -> { s.actorAt(3.0, -0.05, 0, 90); s.actor("float");
                    s.spawn("rescuer", 0.6, 1.0, 0.2, -90); s.anim("rescuer", "kneel_reach");
                    s.spawn("pip", -1.0, 1.06, 1.6, -90); s.anim("pip", "sit"); })
                .shot(130, v(5.5, 0.4, -2.0), v(1.5, 1.2, 0.3), v(5.0, 0.5, -1.5), v(1.0, 1.4, 0.3), 60, 56)
                .say(30, "rescue.1")
                .at(120, s -> s.actorWalk(1.6, 0.6, 0.1, 90, 40))
                .shot(110, v(3.4, 1.0, 1.4), v(1.6, 1.0, 0), v(3.2, 1.1, 1.2), v(1.4, 1.2, 0))
                .say(125, "rescue.2")
                .at(170, s -> { s.actorAt(-0.6, 1.02, 0.4, 90); s.actor("sit"); })
                .shot(130, v(0.6, 1.9, 2.6), v(-0.6, 1.3, 0.4), v(0.4, 1.8, 2.3), v(-0.6, 1.3, 0.4))
                .say(250, "rescue.3")
                .say(300, "rescue.4")
                .at(360, s -> { s.anim("pip", "walk"); s.moveThing("pip", -0.4, 1.06, 0.5, 180, 30); s.soundAt("pip_meow", -1.0, 1.4, 1.6, 1f, 1f); })
                .at(395, s -> { s.anim("pip", "petted"); s.actor("cradle"); s.sound("pip_purr", 1f, 1f); })
                .shot(160, v(0.5, 1.6, 1.2), v(-0.5, 1.3, 0.4), v(0.4, 1.55, 1.0), v(-0.5, 1.3, 0.4), 50, 42)
                .say(470, "rescue.5")
                .shot(160, v(1.0, 2.0, 1.6), v(-0.4, 1.2, 0.4), v(10, 8, 12), v(-0.4, 1.2, 0.4), 52, 62)
                .len(720);

        // ============================================================== ending: the edge of the world
        cut("edge")
                .at(0, s -> { s.actorAt(0, 0.02, 1.8, 0); s.actor("stand"); })
                .shot(130, v(0.8, 2.0, -1.8), v(0, -6, 40), v(0.5, 2.2, -0.8), v(0, -10, 40))
                .say(30, "edge.1")
                .shot(110, v(0.9, 1.7, 3.6), v(0, 1.6, 1.8), v(0.7, 1.7, 3.3), v(0, 1.6, 1.8), 50, 44)
                .say(140, "edge.2")
                .at(240, s -> s.actor("jump"))
                .at(268, s -> { s.actor("fall"); s.actorWalk(0, -70, 12, 0, 220); })
                .shot(260, v(1.0, 2.4, 1.0), v(0, 0.5, 4.0), v(1.5, -60, 9), v(0, -70, 12), 55, 70)
                .say(330, "edge.3")
                .len(500);
    }

    private Cuts() {}
}
