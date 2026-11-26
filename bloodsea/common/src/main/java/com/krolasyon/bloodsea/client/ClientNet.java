package com.krolasyon.bloodsea.client;

import com.krolasyon.bloodsea.net.Msg;
import net.minecraft.client.Minecraft;

public final class ClientNet {
    private ClientNet() {}

    public static void handle(Msg m) {
        Minecraft mc = Minecraft.getInstance();
        switch (m.type) {
            case "ending" -> { CutScreen.release(); mc.setScreen(new EndingScreen(m.s("id"), m.i("seen"))); }
            case "cut" -> mc.setScreen(new CutScreen(m.s("id"), m.d("x"), m.d("y"), m.d("z"), m.f("yaw")));
            case "cut_end" -> CutScreen.release();
            case "choice" -> mc.setScreen(new ChoiceScreen(m.s("id"), m.i("secs")));
            case "obj" -> StoryHud.obj(m.s("key"));
            case "say" -> StoryHud.say(m.s("key"));
            case "eye" -> WatcherClient.open(m.i("ticks"));
            case "shake" -> StoryHud.shake(m.i("ticks"));
            case "dawn" -> StoryHud.dawn(m.i("secs"));
            case "omen" -> OmenClient.handle(m.s("kind"), m.i("ticks"));
            default -> {}
        }
    }
}
