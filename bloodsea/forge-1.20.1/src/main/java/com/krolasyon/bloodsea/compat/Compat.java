package com.krolasyon.bloodsea.compat;

import com.krolasyon.bloodsea.BloodSea;
import com.krolasyon.bloodsea.net.Msg;
import com.krolasyon.bloodsea.net.Net;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/** Version specific glue: Forge 1.20.1 */
public final class Compat {
    public static final String MC = "1.20.1";
    private static SimpleChannel CH;

    private Compat() {}

    public static ResourceLocation rl(String path) { return new ResourceLocation(BloodSea.MODID, path); }

    public static ResourceLocation rl(String ns, String path) { return new ResourceLocation(ns, path); }

    public static Block liquidBlock(Supplier<? extends FlowingFluid> f, BlockBehaviour.Properties p) { return new LiquidBlock(f, p); }

    public static void initNetwork() {
        CH = NetworkRegistry.newSimpleChannel(rl("main"), () -> "1", "1"::equals, "1"::equals);
        CH.messageBuilder(Msg.class, 0)
                .encoder(Msg::write)
                .decoder(Msg::read)
                .consumerMainThread((msg, ctx) -> {
                    if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                        ServerPlayer sp = ctx.get().getSender();
                        if (sp != null) Net.onServer(sp, msg);
                    } else Net.onClient(msg);
                    ctx.get().setPacketHandled(true);
                })
                .add();
    }

    public static void sendToPlayer(ServerPlayer p, Msg m) { CH.send(PacketDistributor.PLAYER.with(() -> p), m); }

    public static void sendToServer(Msg m) { CH.sendToServer(m); }

    public static net.minecraft.world.item.ItemStack named(net.minecraft.world.item.ItemStack s, net.minecraft.network.chat.Component c) { s.setHoverName(c); return s; }

    public static boolean edible(net.minecraft.world.item.ItemStack s) { return s.isEdible(); }
}
