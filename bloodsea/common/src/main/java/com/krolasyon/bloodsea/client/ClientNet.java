package com.krolasyon.bloodsea.client;

import com.krolasyon.bloodsea.net.Msg;
import net.minecraft.client.Minecraft;

public final class ClientNet {
    private ClientNet() {}

    public static void handle(Msg m) {
        if (m.type.equals("ending")) Minecraft.getInstance().setScreen(new EndingScreen(m.s("id"), m.i("seen")));
    }
}
