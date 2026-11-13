package com.krolasyon.hatman.story;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Conversation trees. Text lives in the lang files as dlg.<node> and dlg.<node>.<choice index>.
 * A "next" of "end" finishes the conversation; "end:xyz" finishes it with a result the story reacts to.
 */
public final class Dialogues {
    public static final class Choice {
        public final String next;
        public final int anger, claim;
        public final boolean lie;
        public String flag = null;
        /** item id or flag that must be present ("!x" = must be absent); null = always */
        public String need = null;

        Choice(String next, int anger, int claim, boolean lie) { this.next = next; this.anger = anger; this.claim = claim; this.lie = lie; }
        Choice flag(String f) { flag = f; return this; }
        Choice need(String n) { need = n; return this; }
    }

    public static final class Node {
        public final String id, speaker;
        /** hatman animation to trigger when this line starts (may be null) */
        public String anim = null;
        public String sound = null;
        public String next = "end";
        /** seconds allowed to answer; 0 = no limit. Silence is choice -1. */
        public int time = 0;
        public final List<Choice> choices = new ArrayList<>();
        /** where silence leads */
        public String silence = null;

        Node(String id, String speaker) { this.id = id; this.speaker = speaker; }
        Node anim(String a) { anim = a; return this; }
        Node sound(String s) { sound = s; return this; }
        Node next(String n) { next = n; return this; }
        Node time(int t, String silenceNext) { time = t; silence = silenceNext; return this; }
        Node c(Choice c) { choices.add(c); return this; }
    }

    public static final Map<String, Node> NODES = new HashMap<>();

    private static Node n(String id, String speaker) {
        Node node = new Node(id, speaker);
        NODES.put(id, node);
        return node;
    }

    private static Choice c(String next, int anger, int claim) { return new Choice(next, anger, claim, false); }
    private static Choice lie(String next, int anger) { return new Choice(next, anger, 0, true); }

    static {
        // ============ D1: the telephone
        n("p1", "voice").next("p2");
        n("p2", "voice").time(14, "p3s")
                .c(c("p3a", -2, 6)).c(c("p3b", 0, 0)).c(c("p3c", 8, 0));
        n("p3a", "voice").next("p4");
        n("p3b", "voice").next("p4");
        n("p3c", "voice").next("p4");
        n("p3s", "voice").next("p4");
        n("p4", "voice").time(14, "p5c")
                .c(c("p5a", 0, 5).flag("honest_phone")).c(lie("p5b", 12).flag("lied_phone")).c(c("p5c", 6, 0));
        n("p5a", "voice").next("p6");
        n("p5b", "voice").sound("fx.static").next("p6");
        n("p5c", "voice").next("p6");
        n("p6", "voice").next("end");

        // ============ D2: through the study door
        n("k1", "hatman").sound("hm.knock").next("k2");
        n("k2", "hatman").time(12, "k3s")
                .c(c("k3a", -5, 0).flag("refused_politely")).c(c("k3b", 15, 0).flag("cursed_him")).c(c("k3c", 0, 25).flag("invited"));
        n("k3a", "hatman").next("k4");
        n("k3b", "hatman").sound("hm.bang").next("k4");
        n("k3s", "hatman").next("k4");
        n("k3c", "hatman").next("end:invited");
        n("k4", "hatman").time(14, "k5s")
                .c(c("k5a", -3, 0).flag("read_ledger")).c(lie("k5b", 12)).c(c("k5c", 0, 0));
        n("k5a", "hatman").next("k6");
        n("k5b", "hatman").sound("hm.bang").next("k6");
        n("k5c", "hatman").next("k6");
        n("k5s", "hatman").next("k6");
        n("k6", "hatman").next("end");

        // ============ D3: the library, face to face
        n("l1", "hatman").anim("head_tilt").next("l2");
        n("l2", "hatman").anim("talk_calm").time(16, "l3s")
                .c(c("l3a", -8, 0).flag("understands")).c(c("l3b", 6, 0)).c(c("l3c", 14, 0).flag("called_monster"));
        n("l3a", "hatman").anim("head_tilt").next("l4");
        n("l3b", "hatman").anim("talk_calm").next("l4");
        n("l3c", "hatman").anim("talk_angry").sound("hm.growl").next("l4");
        n("l3s", "hatman").anim("twitch").next("l4");
        n("l4", "hatman").anim("talk_calm").time(16, "l5s")
                .c(c("l5a", -4, 0).flag("honest_ruth")).c(lie("l5b", 12)).c(c("l5c", 3, 0));
        n("l5a", "hatman").anim("head_tilt").next("l6");
        n("l5b", "hatman").anim("talk_angry").sound("hm.growl").next("l6");
        n("l5c", "hatman").anim("point").next("l6");
        n("l5s", "hatman").anim("twitch").next("l6");
        n("l6", "hatman").anim("talk_calm").time(16, "l7s")
                .c(c("l7a", 0, 0).flag("refused_hat")).c(c("l7b", 0, 15).flag("offered_trade")).c(c("l7c", 0, 30).flag("said_yes"));
        n("l7a", "hatman").anim("head_tilt").next("l8");
        n("l7b", "hatman").anim("talk_calm").next("l8");
        n("l7c", "hatman").anim("lean_in").next("l8");
        n("l7s", "hatman").anim("twitch").next("l8");
        n("l8", "hatman").anim("point").next("end");

        // ============ D4: Ruth, in the cold room
        n("r1", "ruth").next("r2");
        n("r2", "ruth").c(c("r3a", 0, 0)).c(c("r3b", 0, 0)).c(c("r3c", 0, 0));
        n("r3a", "ruth").next("r4");
        n("r3b", "ruth").next("r4");
        n("r3c", "ruth").next("r4");
        n("r4", "ruth").next("r5");
        n("r5", "ruth").next("end");

        // ============ D5: the last fitting
        n("f1", "hatman").anim("talk_calm").next("f2");
        n("f2a", "hatman").anim("talk_angry").sound("hm.growl").next("f3");
        n("f2b", "hatman").anim("head_tilt").next("f3");
        n("f2c", "hatman").anim("talk_calm").next("f3");
        n("f3", "hatman").anim("offer").time(25, "f_silence")
                .c(c("f_face", 0, 0).need("original_face"))
                .c(c("f_yes", 0, 0))
                .c(c("f_no", 0, 0).need("!bound"))
                .c(c("f_yes", 0, 0).need("bound"))
                .c(c("f_burn", 0, 0).need("lamp_oil+lighter"));
        n("f_silence", "hatman").anim("talk_angry").next("end:run");
        n("f_face", "hatman").anim("twitch").sound("fx.sting")
                .c(c("f_face2", 0, 0)).c(c("f_wear", 0, 0));
        n("f_face2", "hatman").anim("bow").next("end:closed");
        n("f_wear", "narr").sound("fx.sting").next("end:worn");
        n("f_yes", "hatman").anim("lean_in").next("end:newface");
        n("f_no", "hatman").anim("stand_up").next("end:run");
        n("f_burn", "hatman").anim("scream").sound("hm.scream").next("end:burn");
    }

    private Dialogues() {}
}
