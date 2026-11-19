package com.krolasyon.mimic.compat;

import com.krolasyon.mimic.MimicMod;
import com.krolasyon.mimic.net.Msg;
import com.krolasyon.mimic.net.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Version specific glue: Forge 1.20.1 */
public final class Compat {
    public static final String MC = "1.20.1";
    private static SimpleChannel CH;

    private Compat() {}

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MimicMod.MODID, path);
    }

    public static ResourceLocation rl(String ns, String path) {
        return new ResourceLocation(ns, path);
    }

    public static void initNetwork() {
        CH = NetworkRegistry.newSimpleChannel(rl("main"), () -> "1", "1"::equals, "1"::equals);
        CH.messageBuilder(Msg.class, 0)
                .encoder(Msg::write)
                .decoder(Msg::read)
                .consumerMainThread((msg, ctx) -> {
                    if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                        ServerPlayer sp = ctx.get().getSender();
                        if (sp != null) Net.onServer(sp, msg);
                    } else {
                        Net.onClient(msg);
                    }
                    ctx.get().setPacketHandled(true);
                })
                .add();
    }

    public static void sendToPlayer(ServerPlayer p, Msg m) {
        CH.send(PacketDistributor.PLAYER.with(() -> p), m);
    }

    public static void sendToServer(Msg m) {
        CH.sendToServer(m);
    }

    public static float partialTick() {
        return Minecraft.getInstance().getFrameTime();
    }

    public static net.minecraft.world.level.block.Block stairs(net.minecraft.world.level.block.state.BlockState base, net.minecraft.world.level.block.state.BlockBehaviour.Properties p) {
        return new net.minecraft.world.level.block.StairBlock(() -> base, p);
    }

    public static net.minecraft.world.level.block.Block door(net.minecraft.world.level.block.state.BlockBehaviour.Properties p) {
        return new net.minecraft.world.level.block.DoorBlock(p, net.minecraft.world.level.block.state.properties.BlockSetType.SPRUCE);
    }

    public static net.minecraft.world.level.block.Block trapdoor(net.minecraft.world.level.block.state.BlockBehaviour.Properties p) {
        return new net.minecraft.world.level.block.TrapDoorBlock(p, net.minecraft.world.level.block.state.properties.BlockSetType.SPRUCE);
    }
}
