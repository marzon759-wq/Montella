package com.krolasyon.mimic.story;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Conversations and the timed decisions. Text lives in the lang file as dlg.<node> and dlg.<node>.<choice index>.
 * A "next" of "end" finishes; "end:xyz" finishes with a result the story reacts to.
 * A node with a time is a decision in the moment: the choices are drawn big, a bar runs out, silence is a choice too.
 */
public final class Dialogues {
    public static final class Choice {
        public final String next;
        /** how much noise this answer makes (the thing listens) */
        public final int anger, claim;
        public final boolean lie;
        public String flag = null;
        /** item id or flag that must be present ("!x" = must be absent); null = always */
        public String need = null;
        /** a line you say out loud: played in your own voice */
        public String say = null;
        /** saying this lets it learn the line: it can use it later */
        public String learn = null;

        Choice(String next, int anger, int claim, boolean lie) { this.next = next; this.anger = anger; this.claim = claim; this.lie = lie; }
        Choice flag(String f) { flag = f; return this; }
        Choice need(String n) { need = n; return this; }
        Choice say(String s) { say = s; return this; }
        Choice learn(String l) { learn = l; return this; }
    }

    public static final class Node {
        public final String id, speaker;
        public String anim = null;
        public String sound = null;
        public String next = "end";
        /** seconds allowed to answer; 0 = no limit. Silence is choice -1. */
        public int time = 0;
        /** a decision in the moment: drawn as big choices */
        public boolean big = false;
        public final List<Choice> choices = new ArrayList<>();
        public String silence = null;
        /** ticks a line without choices stays up before it moves on by itself (0 = wait for the player) */
        public int auto = 0;

        Node(String id, String speaker) { this.id = id; this.speaker = speaker; }
        Node anim(String a) { anim = a; return this; }
        Node sound(String s) { sound = s; return this; }
        Node next(String n) { next = n; return this; }
        Node time(int t, String silenceNext) { time = t; silence = silenceNext; return this; }
        Node big(int t, String silenceNext) { big = true; return time(t, silenceNext); }
        Node auto(int ticks) { auto = ticks; return this; }
        Node c(Choice c) { choices.add(c); return this; }
    }

    public static final Map<String, Node> NODES = new HashMap<>();

    private static Node n(String id, String speaker) {
        Node node = new Node(id, speaker);
        NODES.put(id, node);
        return node;
    }

    private static Choice c(String next) { return new Choice(next, 0, 0, false); }
    private static Choice loud(String next, int noise) { return new Choice(next, noise, 0, false); }

    static {
        // ============ the telephone in the hall
        n("phone", "narr").big(8, "ring1")
                .c(c("ans1").flag("answered"))
                .c(c("ring1"));
        n("ans1", "dad").sound("v.mdad.phone1").auto(70).next("ans2");
        n("ans2", "dad").sound("v.mdad.phone2").auto(80).next("end:ans");
        n("ring1", "machine").sound("v.dad.machine").auto(80).next("ring2");
        n("ring2", "dad").sound("v.mdad.machine2").auto(80).next("end:ring");

        // ============ the walkie-talkie: Lily in the silo
        n("r1", "lily").sound("v.lily.r1").time(20, "r2s")
                .c(c("r2").say("v.sam.whisper"))
                .c(loud("r2s", 30).say("v.sam.loud").learn("loud").flag("was_loud"));
        n("r2s", "lily").sound("v.lily.r2").next("r3");
        n("r2", "lily").sound("v.lily.r2").next("r3");
        n("r3", "lily").sound("v.lily.r3").next("r4");
        n("r4", "lily").sound("v.lily.r4").next("r5");
        n("r5", "lily").sound("v.lily.r5").time(20, "r6")
                .c(loud("r6", 20).say("v.sam.word").learn("word").flag("word_loud"))
                .c(c("r6").say("v.sam.whisper2"))
                .c(c("r6").flag("clicked"));
        n("r6", "lily").sound("v.lily.r6").next("end:radio1");

        // ============ the voice upstairs
        n("upstairs", "narr").big(6, "end:quiet")
                .c(c("end:up"))
                .c(loud("end:ask", 30).say("v.sam.ask").learn("ask"))
                .c(c("end:quiet"));

        // ============ the barn: it wakes
        n("wake", "narr").big(4, "end:still")
                .c(c("end:ladder"))
                .c(c("end:hay"))
                .c(c("end:still"));

        // ============ the shed: it is at the door
        n("trap", "narr").big(6, "end:run")
                .c(c("end:throw").need("meat"))
                .c(c("end:sneak"))
                .c(c("end:run"));

        // ============ Dad's tape
        n("tape1", "dad").sound("v.dad.tape1").auto(150).next("tape2");
        n("tape2", "dad").sound("v.dad.tape2").auto(150).next("tape3");
        n("tape3", "dad").sound("v.dad.tape3").auto(150).next("tape4");
        n("tape4", "dad").sound("v.dad.tape4").auto(150).next("end:tape");

        // ============ the silo door
        n("silo1", "lily").sound("v.lily.silo1").time(20, "silo_ask")
                .c(loud("silo_ask", 10).say("v.sam.its_me").learn("its_me"))
                .c(loud("silo_word", 10).say("v.sam.ask"));
        n("silo_ask", "lily").sound("v.lily.ask").time(15, "silo_bad")
                .c(c("silo_ok").say("v.sam.word"))
                .c(c("silo_bad"));
        n("silo_word", "lily").sound("v.lily.word").next("silo_ok");
        n("silo_bad", "lily").sound("v.lily.hurry").next("silo_ok");
        n("silo_ok", "lily").sound("v.lily.silo2").next("end:silo");

        // ============ the truck: two Lilys
        n("two", "narr").big(8, "end:drive")
                .c(c("end:drive"))
                .c(c("end:corn"))
                .c(loud("two_ask", 0).say("v.sam.ask"));
        n("two_ask", "narr").big(5, "end:drive")
                .c(c("end:drive"))
                .c(c("end:corn"));

        // ============ the corn: face it or run
        n("face", "narr").big(5, "end:run")
                .c(c("end:face"))
                .c(c("end:run"));
    }

    private Dialogues() {}
}
