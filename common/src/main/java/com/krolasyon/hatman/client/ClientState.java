package com.krolasyon.hatman.client;

/** Everything the client knows about the running story. Written by ClientNet, read by the HUD and screens. */
public final class ClientState {
    public static boolean active, nvOn, hasCam, hidden, chase, power, fire;
    public static float battery = 100, fear, anger;
    public static int spare, notes, notesMax = 25, chapter;
    public static String objective = "", objArg = "", amb = "house";
    public static int ox, oy, oz;
    public static String playerName = "";
    // effects (ticks remaining)
    public static int staticT, shakeT, flashT, jumpT, fadeInT, fadeInMax = 1, freezeT, slatsT;
    public static int objFlash = 999;
    public static int chapterT = -1, chapterN;
    public static String lineKey = null, lineSpeaker = "";
    public static int lineT, lineDur;
    public static String hintKey = null;
    public static int hintT;
    public static int ticks;
    public static float nvFade, fogFar = 24, fearSmooth;
    // cutscene camera
    public static String cutId = null;
    public static int cutT;
    public static float cutFov = -1;
    public static boolean dialogue;

    public static void reset() {
        active = nvOn = hasCam = hidden = chase = power = fire = false;
        objective = "";
        lineKey = null;
        hintKey = null;
        chapterT = -1;
        staticT = shakeT = flashT = jumpT = fadeInT = freezeT = slatsT = 0;
        cutId = null;
        cutFov = -1;
        dialogue = false;
        fear = 0;
    }

    private ClientState() {}
}
