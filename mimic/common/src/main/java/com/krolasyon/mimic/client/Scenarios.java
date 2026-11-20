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

    /** answer a decision only once its screen is actually up (the server can lag behind the client) */
    private static void decide(int wait, String node, int idx) { cue(wait, () -> AutoTest.pendingAnswer(node, idx)); }

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
        decide(10, "phone", passenger || learned ? 1 : 0);
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
        cue(240, () -> shot("p_upstairs_choice"));
        // ---- the voice upstairs: ask (hide), go up (chase), or stay quiet (sneak)
        if (dawn) {
            decide(10, "upstairs", 1);
            cue(200, () -> {});
            expect("c1_hide");
            cue(4, () -> srv("hide:pantry"));
            cue(300, () -> shot("p_search"));
            cue(200, () -> srv("qte:breath:1"));
            cue(400, () -> {});
        } else if (passenger) {
            decide(10, "upstairs", 0);
            cue(200, () -> {});
            tp(25.5, 6, 61.5, 180);
            cue(30, () -> shot("p_landing"));
            skipCut(30);
            cue(20, () -> {});
            expect("c1_chase");
            tp(31.5, 1, 45.5, 180);
            cue(120, () -> {});
        } else {
            decide(10, "upstairs", 2);
            cue(200, () -> {});
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
        cue(70, () -> shot("p_wake_choice"));
        if (dawn) {
            decide(4, "wake", 1);
            cue(120, () -> {});
            cue(10, () -> {});
            expect("c2_hay");
            cue(300, () -> shot("p_hay"));
            cue(200, () -> srv("qte:breath:1"));
            cue(600, () -> {});
        } else if (passenger) {
            decide(4, "wake", 0);
            cue(120, () -> {});
            expect("c2_ladder");
            tp(62.5, 7, 44.5, 180);
            cue(10, () -> {});
            skipCut(30);
            cue(30, () -> {});
        } else {
            decide(4, "wake", 2);
            cue(120, () -> {});
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
        decide(10, "trap", dawn ? 0 : passenger ? 1 : 2);
        cue(120, () -> {});
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
            decide(10, "two", 2);
            cue(200, () -> shot("p_two_ask"));
            decide(10, "two_ask", 0);
            cue(150, () -> {});
        } else { decide(10, "two", 1); cue(150, () -> {}); }
        cue(20, () -> {});
        if (!passenger) {
            expect("c5_corn");
            tp(76.5, 1, 78.5, 0);
            cue(40, () -> shot("p_face_choice"));
            decide(10, "face", harvest ? 1 : 0);
            cue(150, () -> {});
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

    // ================================================================================== the gameplay video
    /** a real playthrough on foot for the five minute video: Dawn ending, every chapter, the big decisions on screen */
    static void video() {
        PlayDriver d = new PlayDriver(AutoTest::srv);
        AutoTest.drive = d;
        d.wait(20).mark("start");
        // prologue: let the voicemail play
        d.until(() -> ClientState.cutId == null && Story.S.step.equals("c1_gate"), 1200, "intro").wait(20);
        d.mark("gate").light(true).pitch(4)
                .walk(false, PlayDriver.P(55.5, 1, 101), PlayDriver.P(56.5, 1, 98.5), PlayDriver.P(53.5, 1, 95))
                .walk(true, PlayDriver.P(50.5, 1, 88), PlayDriver.P(50.5, 1, 76), PlayDriver.P(30, 1, 75), PlayDriver.P(25.5, 1, 74.6));
        // the porch: locked, the key in the bird box
        d.use("door:front").wait(40).lookAtTag("birdbox", 20).use("birdbox").wait(40).use("door:front").wait(10)
                .run(() -> AutoTest.srv("open:front"))
                .walk(false, PlayDriver.P(25.5, 1, 71.5), PlayDriver.P(25.5, 1, 69.5), PlayDriver.P(24.5, 1, 66), PlayDriver.P(24.5, 1, 60.5))
                .look(25.5, 4, 57, 30).look(24.6, 2.5, 58.5, 20);
        // the telephone: answer it
        d.mark("phone").step("c1_phone").talk(0).step("c1_kitchen");
        d.walk(false, PlayDriver.P(24.5, 1, 56), PlayDriver.P(25.6, 1, 55.5)).run(() -> AutoTest.srv("open:kitchen"))
                .walk(false, PlayDriver.P(28.6, 1, 55.5), PlayDriver.P(30.5, 1, 57.6))
                .read("note:kitchen", 50).wait(10)
                .lookAtTag("pickup:walkie", 15).use("pickup:walkie").wait(30);
        // Lily on the walkie: whisper, but say the word out loud (it hears)
        d.mark("radio").srv("use_item:walkie").talk(0, 0).step("c1_voice");
        // "Sam, I'm up here!": ask for the word
        d.mark("upstairs").look(25.5, 7, 60, 40).talk(1).step("c1_hide");
        d.walk(true, PlayDriver.P(32.5, 1, 57.5)).lookAtTag("wardrobe:pantry", 6).use("wardrobe:pantry")
                .mark("hidden").until(() -> Story.S.step.equals("c2_yard"), 2400, "search").mark("searched").wait(30).srv("unhide").wait(10);
        // chapter 2: the barn
        d.mark("barn").run(() -> AutoTest.srv("open:back"))
                .walk(false, PlayDriver.P(31.5, 1, 52.5), PlayDriver.P(31.5, 1, 49.5))
                .walk(true, PlayDriver.P(40, 1, 47), PlayDriver.P(60, 1, 48), PlayDriver.P(70.5, 1, 49))
                .walk(false, PlayDriver.P(70.5, 1, 45.5)).cutStart().cutEnd();
        d.mark("sneak").light(false).sneak(true)
                .walk(false, PlayDriver.P(70.5, 1, 41), PlayDriver.P(67.5, 1, 33.5), PlayDriver.P(61.5, 1, 33.5))
                .lookAtTag("drawer:item:crowbar", 10).use("drawer:item:crowbar").wait(20)
                .walk(false, PlayDriver.P(67.5, 1, 34.5), PlayDriver.P(73.5, 1, 40.5), PlayDriver.P(78.5, 1, 41.5))
                .lookAtTag("toolcab", 10).use("toolcab").wait(20)
                .walk(false, PlayDriver.P(76, 1, 42.5)).lookAtTag("pickup:gas_can", 10).use("pickup:gas_can").sneak(false)
                .mark("wake").step("c2_wake").look(70.5, 6, 30.5, 30).talk(1).step("c3_shed").wait(20);
        // chapter 3: the shed
        d.mark("shed").run(() -> AutoTest.srv("open:barn_n")).light(true)
                .walk(true, PlayDriver.P(67.5, 1, 38), PlayDriver.P(70.5, 1, 20), PlayDriver.P(70.5, 1, 14.5), PlayDriver.P(66, 1, 10),
                        PlayDriver.P(52, 1, 11), PlayDriver.P(52, 1, 30), PlayDriver.P(44.5, 1, 30))
                .use("door:shed").wait(10).run(() -> AutoTest.srv("open:shed"))
                .walk(false, PlayDriver.P(44.5, 1, 26), PlayDriver.P(44.5, 1, 21)).look(43, 1, 19.5, 40)
                .lookAtTag("pickup:meat", 10).use("pickup:meat").wait(10)
                .lookAtTag("coat", 20).use("coat").wait(40)
                .mark("trap").look(44.5, 2, 30, 30).step("c3_trap").talk(0)
                .walk(true, PlayDriver.P(44.5, 1, 29.5), PlayDriver.P(37, 1, 30), PlayDriver.P(37, 1, 14), PlayDriver.P(56, 1, 12),
                        PlayDriver.P(76, 1, 12), PlayDriver.P(83.5, 1, 20), PlayDriver.P(81.5, 1, 31.5))
                .step("c4_silo");
        // chapter 4: the silo
        d.mark("silo").lookAtTag("door:silo", 15).use("door:silo").talk(0, 0).cutStart().cutEnd().step("c4_follow")
                .walk(false, PlayDriver.P(82.5, 1, 36), PlayDriver.P(84, 1, 48), PlayDriver.P(76, 1, 50), PlayDriver.P(71.5, 1, 50.5))
                .cutStart().cutEnd().step("c4_corn")
                .walk(true, PlayDriver.P(60, 1, 50.5), PlayDriver.P(48.5, 1, 49.5)).step("c5_truck");
        // chapter 5: the truck - and two voices
        d.mark("truck").look(46.5, 1.5, 46.5, 15).srv("truck_use").wait(30).srv("truck_use").cutStart().cutEnd().talk(1).step("c5_corn")
                .walk(true, PlayDriver.P(60, 1, 53), PlayDriver.P(74.5, 1, 53), PlayDriver.P(76.5, 1, 60), PlayDriver.P(76.5, 1, 74.5))
                .look(76.5, 1, 85.5, 10).talk(0)
                .until(() -> Story.S.step.equals("credits"), 3000, "ending").wait(200).mark("end")
                .run(() -> Minecraft.getInstance().stop());
    }
}
