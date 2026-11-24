package com.krolasyon.bloodsea.net;

import com.krolasyon.bloodsea.compat.Compat;
import com.krolasyon.bloodsea.game.SeaEvents;
import net.minecraft.server.level.ServerPlayer;

public final class Net {
    private Net() {}

    public static void toPlayer(ServerPlayer p, Msg m) {
        if (p != null && p.connection != null) Compat.sendToPlayer(p, m);
    }

    public static void toServer(Msg m) { Compat.sendToServer(m); }

    public static void onServer(ServerPlayer sp, Msg m) { SeaEvents.onClientMsg(sp, m); }

    public static void onClient(Msg m) { com.krolasyon.bloodsea.client.ClientNet.handle(m); }
}
