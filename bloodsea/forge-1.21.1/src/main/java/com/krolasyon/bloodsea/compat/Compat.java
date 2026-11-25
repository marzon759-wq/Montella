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
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

import java.util.function.Supplier;

/** Version specific glue: Forge 1.21.1 */
public final class Compat {
    public static final String MC = "1.21.1";
    private static SimpleChannel CH;

    private Compat() {}

    public static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath(BloodSea.MODID, path); }

    public static ResourceLocation rl(String ns, String path) { return ResourceLocation.fromNamespaceAndPath(ns, path); }

    public static Block liquidBlock(Supplier<? extends FlowingFluid> f, BlockBehaviour.Properties p) { return new LiquidBlock(f, p); }

    public static void initNetwork() {
        CH = ChannelBuilder.named(rl("main")).networkProtocolVersion(1).simpleChannel();
        CH.messageBuilder(Msg.class, 0)
                .encoder(Msg::write)
                .decoder(Msg::read)
                .consumerMainThread((msg, ctx) -> {
                    if (ctx.isServerSide()) {
                        ServerPlayer sp = ctx.getSender();
                        if (sp != null) Net.onServer(sp, msg);
                    } else Net.onClient(msg);
                    ctx.setPacketHandled(true);
                })
                .add();
    }

    public static void sendToPlayer(ServerPlayer p, Msg m) { CH.send(m, PacketDistributor.PLAYER.with(p)); }

    public static void sendToServer(Msg m) { CH.send(m, PacketDistributor.SERVER.noArg()); }

    public static net.minecraft.world.item.ItemStack named(net.minecraft.world.item.ItemStack s, net.minecraft.network.chat.Component c) {
        s.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, c);
        return s;
    }

    public static boolean edible(net.minecraft.world.item.ItemStack s) { return s.has(net.minecraft.core.component.DataComponents.FOOD); }
}
