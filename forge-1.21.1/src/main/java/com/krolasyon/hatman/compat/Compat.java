package com.krolasyon.hatman.compat;

import com.krolasyon.hatman.HatmanMod;
import com.krolasyon.hatman.net.Msg;
import com.krolasyon.hatman.net.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/** Version specific glue: Forge 1.21.1 */
public final class Compat {
    public static final String MC = "1.21.1";
    private static SimpleChannel CH;

    private Compat() {}

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(HatmanMod.MODID, path);
    }

    public static ResourceLocation rl(String ns, String path) {
        return ResourceLocation.fromNamespaceAndPath(ns, path);
    }

    public static void initNetwork() {
        CH = ChannelBuilder.named(rl("main")).networkProtocolVersion(1).simpleChannel();
        CH.messageBuilder(Msg.class, 0)
                .encoder(Msg::write)
                .decoder(Msg::read)
                .consumerMainThread((msg, ctx) -> {
                    if (ctx.isServerSide()) {
                        ServerPlayer sp = ctx.getSender();
                        if (sp != null) Net.onServer(sp, msg);
                    } else {
                        Net.onClient(msg);
                    }
                    ctx.setPacketHandled(true);
                })
                .add();
    }

    public static void sendToPlayer(ServerPlayer p, Msg m) {
        CH.send(m, PacketDistributor.PLAYER.with(p));
    }

    public static void sendToServer(Msg m) {
        CH.send(m, PacketDistributor.SERVER.noArg());
    }

    public static float partialTick() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
    }
}
