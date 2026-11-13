package com.krolasyon.hatman.net;

import com.krolasyon.hatman.compat.Compat;
import com.krolasyon.hatman.story.Story;
import net.minecraft.server.level.ServerPlayer;

public final class Net {
    private Net() {}

    public static void toPlayer(ServerPlayer p, Msg m) {
        if (p != null && p.connection != null) Compat.sendToPlayer(p, m);
    }

    public static void toServer(Msg m) {
        Compat.sendToServer(m);
    }

    public static void onServer(ServerPlayer sp, Msg m) {
        Story.onClientMsg(sp, m);
    }

    public static void onClient(Msg m) {
        com.krolasyon.hatman.client.ClientNet.handle(m);
    }
}
