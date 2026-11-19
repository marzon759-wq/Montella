package com.krolasyon.mimic.client;

import com.krolasyon.mimic.MimicMod;
import com.krolasyon.mimic.net.Msg;
import com.krolasyon.mimic.net.Net;
import com.krolasyon.mimic.story.Story;
import net.minecraft.client.Minecraft;

import static com.krolasyon.mimic.client.AutoTest.cue;
import static com.krolasyon.mimic.client.AutoTest.shot;
import static com.krolasyon.mimic.client.AutoTest.srv;

/**
 * Scripted playthroughs that drive the story through its server hooks (teleports, uses, answers, QTE results) and
 * check that every step arrives where it should. play_dawn / play_passenger / play_harvest / play_learned pick the branches.
 */
final class Scenarios {
    private Scenarios() {}

    private static int fails = 0;

    private static void expect(String step) {
        cue(0, () -> {
            boolean ok = Story.S.step.equals(step);
            if (!ok) fails++;
            MimicMod.LOG.info("[PLAY] expect {} -> {} {}", step, Story.S.step, ok ? "OK" : "FAIL");
        });
    }

    private static void skipCut(int wait) {
        cue(wait, () -> {
            if (ClientState.cutId != null) Net.toServer(Msg.of("cut_done").s("id", ClientState.cutId));
        });
    }

    private static void tp(double x, double y, double z, float yaw) { cue(4, () -> srv("tp:" + x + "," + y + "," + z + "," + yaw + ",0")); }

    private static void use(String tag) { cue(6, () -> srv("use:" + tag)); }

    private static void answer(int wait, int idx) { cue(wait, () -> srv("advance:" + idx)); }

    static void play(String mode) {
        boolean dawn = mode.contains("dawn"), passenger = mode.contains("passenger"), harvest = mode.contains("harvest"), learned = mode.contains("learned");
        // ---- prologue
        expect("intro");
        cue(80, () -> shot("p_intro"));
        skipCut(10);
        cue(20, () -> {});
        expect("c1_gate");
        tp(50.5, 1, 92.5, 180);
        cue(10, () -> {});
        tp(25.5, 1, 75.5, 180);
        cue(10, () -> {});
        expect("c1_door");
        use("door:front");
        cue(10, () -> {});
        expect("c1_door");
        use("birdbox");
        use("door:front");
        tp(25.5, 1, 66.5, 180);
        cue(20, () -> shot("p_inside"));
        expect("c1_inside");
        tp(25.5, 1, 58.5, 180);
        cue(60, () -> {});
        expect("c1_phone");
        cue(60, () -> shot("p_phone_choice"));
        answer(10, passenger || learned ? 1 : 0);
        cue(90, () -> srv("advance:0"));
        cue(90, () -> srv("advance:0"));
        cue(40, () -> {});
        expect("c1_kitchen");
        tp(30.5, 1, 57.5, 0);
        use("pickup:walkie");
        use("note:kitchen");
        cue(20, () -> {
            if (Minecraft.getInstance().screen instanceof NoteScreen) Minecraft.getInstance().setScreen(null);
        });
        expect("c1_radio");
        if (dawn) cue(4, () -> srv("use:drawer:item:lighter"));
        cue(10, () -> srv("use_item:walkie"));
        cue(30, () -> shot("p_radio"));
        answer(10, dawn ? 0 : 1);            // whisper or shout
        for (int i = 0; i < 4; i++) answer(40, 0);
        answer(40, passenger ? 0 : 1);       // say the word out loud (it learns it) or whisper
        answer(40, 0);
        cue(140, () -> {});
        expect("c1_voice");
        cue(110, () -> shot("p_upstairs_choice"));
        // ---- the voice upstairs: ask (hide), go up (chase), or stay quiet (sneak)
        if (dawn) {
            answer(10, 1);
            cue(10, () -> {});
            expect("c1_hide");
            cue(4, () -> srv("hide:pantry"));
            cue(300, () -> shot("p_search"));
            cue(200, () -> srv("qte:breath:1"));
            cue(400, () -> {});
        } else if (passenger) {
            answer(10, 0);
            tp(25.5, 6, 61.5, 180);
            cue(30, () -> shot("p_landing"));
            skipCut(30);
            cue(20, () -> {});
            expect("c1_chase");
            tp(31.5, 1, 45.5, 180);
            cue(120, () -> {});
        } else {
            answer(10, 2);
            expect("c1_sneak");
            tp(31.5, 1, 45.5, 180);
            cue(100, () -> {});
        }
        cue(40, () -> { srv("unhide"); if (!Story.S.step.equals("c2_yard")) srv("step:c2_yard"); });
        cue(10, () -> {});
        expect("c2_yard");
        // ---- the barn
        tp(70.5, 1, 44.5, 180);
        cue(20, () -> shot("p_barn_cut"));
        skipCut(30);
        cue(20, () -> {});
        expect("c2_barn");
        cue(5, () -> srv("light:off"));
        tp(61.5, 1, 33.5, -90);
        use("drawer:item:crowbar");
        tp(78.5, 1, 41.5, -90);
        use("toolcab");
        tp(75.5, 1, 42.5, 180);
        use("pickup:gas_can");
        cue(60, () -> {});
        expect("c2_wake");
        cue(40, () -> shot("p_wake_choice"));
        if (dawn) {
            answer(4, 1);
            cue(10, () -> {});
            expect("c2_hay");
            cue(300, () -> shot("p_hay"));
            cue(200, () -> srv("qte:breath:1"));
            cue(600, () -> {});
        } else if (passenger) {
            answer(4, 0);
            expect("c2_ladder");
            tp(62.5, 7, 44.5, 180);
            cue(10, () -> {});
            skipCut(30);
            cue(30, () -> {});
        } else {
            answer(4, 2);
            expect("c2_still");
            cue(150, () -> shot("p_still"));
            cue(200, () -> {});
        }
        cue(40, () -> { srv("unhide"); if (!Story.S.step.equals("c3_shed")) { MimicMod.LOG.info("[PLAY] barn branch did not finish by itself"); srv("step:c3_shed"); } });
        cue(10, () -> {});
        expect("c3_shed");
        // ---- the shed
        tp(44.5, 1, 27.5, 180);
        cue(30, () -> shot("p_shed"));
        use("pickup:meat");
        use("tape");
        for (int i = 0; i < 4; i++) cue(160, () -> {});
        cue(10, () -> {});
        use("coat");
        cue(100, () -> {});
        expect("c3_trap");
        cue(160, () -> shot("p_trap_choice"));
        answer(10, dawn ? 0 : passenger ? 1 : 2);
        cue(30, () -> {});
        tp(82.5, 1, 31.5, 90);
        cue(20, () -> {});
        expect("c4_silo");
        // ---- the silo
        use("door:silo");
        cue(60, () -> shot("p_silo"));
        answer(10, dawn ? 1 : 0);
        cue(60, () -> srv("advance:0"));
        cue(80, () -> srv("advance:0"));
        cue(60, () -> srv("advance:0"));
        cue(40, () -> {});
        skipCut(40);
        cue(20, () -> {});
        expect("c4_follow");
        tp(66.5, 1, 50.5, 90);
        cue(20, () -> shot("p_ambush"));
        skipCut(20);
        cue(20, () -> {});
        expect("c4_corn");
        tp(46.5, 1, 49.5, 180);
        cue(20, () -> {});
        expect("c5_truck");
        cue(4, () -> srv("truck_use"));
        cue(10, () -> srv("truck_use"));
        cue(70, () -> shot("p_two"));
        skipCut(30);
        cue(20, () -> shot("p_two_choice"));
        if (passenger) {
            answer(10, 2);
            cue(120, () -> shot("p_two_ask"));
            answer(10, 0);
        } else answer(10, 1);
        cue(20, () -> {});
        if (!passenger) {
            expect("c5_corn");
            tp(76.5, 1, 78.5, 0);
            cue(40, () -> shot("p_face_choice"));
            if (dawn) answer(10, 0);
            else if (harvest) answer(10, 1);
            else answer(10, 0);
            cue(60, () -> shot("p_ending"));
        } else cue(60, () -> shot("p_ending"));
        skipCut(100);
        skipCut(60);
        skipCut(60);
        cue(30, () -> {});
        expect("credits");
        cue(20, () -> MimicMod.LOG.info("[PLAY] ending={} fails={} caught={}", Story.S.ending, fails, Story.S.caught));
        cue(20, () -> shot("p_credits"));
        cue(40, () -> Minecraft.getInstance().stop());
    }
}
