package com.krolasyon.mimic.client;

/** Everything the client knows about the running story. Written by ClientNet, read by the HUD and screens. */
public final class ClientState {
    public static boolean active, lightOn, hasLight, hidden, hayHide, chase, cover, dawn;
    public static float battery = 100, fear, disturb = -1, noise, stamina = 100;
    public static boolean tired;
    public static int spare, notes, notesMax = 15, chapter;
    public static String objective = "", objArg = "", amb = "outside";
    public static int ox, oy, oz;
    public static String playerName = "";
    // effects (ticks remaining)
    public static int staticT, shakeT, flashT, jumpT, killT, fadeInT, fadeInMax = 1, freezeT, slatsT;
    public static int objFlash = 999;
    public static int chapterT = -1, chapterN;
    public static String lineKey = null, lineSpeaker = "";
    public static int lineT, lineDur;
    public static String hintKey = null;
    public static int hintT;
    public static int ticks;
    public static float fogFar = 24, fearSmooth, lightFade;
    // cutscene camera
    public static String cutId = null;
    public static int cutT;
    public static float cutFov = -1;
    public static boolean cutBlack;
    public static boolean dialogue;

    public static void reset() {
        active = lightOn = hasLight = hidden = hayHide = chase = cover = dawn = false;
        objective = "";
        lineKey = null;
        hintKey = null;
        chapterT = -1;
        staticT = shakeT = flashT = jumpT = killT = fadeInT = freezeT = slatsT = 0;
        cutId = null;
        cutFov = -1;
        cutBlack = false;
        dialogue = false;
        fear = 0;
        disturb = -1;
        Qte.cancel();
    }

    private ClientState() {}
}
