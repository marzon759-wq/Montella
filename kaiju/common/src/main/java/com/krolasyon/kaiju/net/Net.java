package com.krolasyon.kaiju.net;

import com.krolasyon.kaiju.KaijuMod;
import com.krolasyon.kaiju.entity.KaijuEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Net {
    private static SimpleChannel CH;
    /** set by the client setup: (type, x, y, z, strength) */
    public static Consumer<Fx> CLIENT_FX = f -> {};

    public record Control(ControlInput in) {
        void write(FriendlyByteBuf b) { in.write(b); }
        static Control read(FriendlyByteBuf b) { return new Control(ControlInput.read(b)); }
    }

    public record Select(String attack, boolean perform) {
        void write(FriendlyByteBuf b) { b.writeUtf(attack); b.writeBoolean(perform); }
        static Select read(FriendlyByteBuf b) { return new Select(b.readUtf(), b.readBoolean()); }
    }

    public record Fx(int type, double x, double y, double z, float strength) {
        public static final int SHAKE = 0, FLASH = 1;
        void write(FriendlyByteBuf b) { b.writeVarInt(type); b.writeDouble(x); b.writeDouble(y); b.writeDouble(z); b.writeFloat(strength); }
        static Fx read(FriendlyByteBuf b) { return new Fx(b.readVarInt(), b.readDouble(), b.readDouble(), b.readDouble(), b.readFloat()); }
    }

    public static void init() {
        CH = NetworkRegistry.newSimpleChannel(new ResourceLocation(KaijuMod.MODID, "main"), () -> "1", "1"::equals, "1"::equals);
        CH.messageBuilder(Control.class, 0, NetworkDirection.PLAY_TO_SERVER).encoder(Control::write).decoder(Control::read)
                .consumerMainThread(Net::onControl).add();
        CH.messageBuilder(Select.class, 1, NetworkDirection.PLAY_TO_SERVER).encoder(Select::write).decoder(Select::read)
                .consumerMainThread(Net::onSelect).add();
        CH.messageBuilder(Fx.class, 2, NetworkDirection.PLAY_TO_CLIENT).encoder(Fx::write).decoder(Fx::read)
                .consumerMainThread((m, c) -> { CLIENT_FX.accept(m); c.get().setPacketHandled(true); }).add();
    }

    static void onControl(Control m, Supplier<NetworkEvent.Context> c) {
        ServerPlayer p = c.get().getSender();
        if (p != null && p.getVehicle() instanceof KaijuEntity k && k.controller == p) {
            if ((m.in().buttons & ControlInput.AI) != 0 && (k.input.buttons & ControlInput.AI) == 0) {
                boolean on = !k.flag(KaijuEntity.F_AI_ASSIST);
                k.setAiAssist(on);
                p.displayClientMessage(net.minecraft.network.chat.Component.translatable(on ? "msg.kaiju.ai_on" : "msg.kaiju.ai_off"), true);
            }
            k.input.update(m.in());
        }
        c.get().setPacketHandled(true);
    }

    static void onSelect(Select m, Supplier<NetworkEvent.Context> c) {
        ServerPlayer p = c.get().getSender();
        if (p != null && p.getVehicle() instanceof KaijuEntity k && k.controller == p && k.spec.attackById.containsKey(m.attack())) {
            k.special = m.attack();
            if (m.perform()) k.startAttack(m.attack());
        }
        c.get().setPacketHandled(true);
    }

    public static void sendControl(ControlInput in) { CH.sendToServer(new Control(in)); }

    public static void sendSelect(String id, boolean perform) { CH.sendToServer(new Select(id, perform)); }

    static void around(ServerLevel sl, Vec3 p, float radius, Fx fx) {
        for (ServerPlayer sp : sl.players()) {
            double d = sp.position().distanceTo(p);
            if (d < radius) CH.send(PacketDistributor.PLAYER.with(() -> sp), new Fx(fx.type(), fx.x(), fx.y(), fx.z(), (float) (fx.strength() * (1 - d / radius))));
        }
    }

    public static void shake(ServerLevel sl, Vec3 p, float strength, float radius) { around(sl, p, Math.max(40, radius), new Fx(Fx.SHAKE, p.x, p.y, p.z, strength)); }

    public static void flash(ServerLevel sl, Vec3 p, float strength, float radius) { around(sl, p, radius, new Fx(Fx.FLASH, p.x, p.y, p.z, strength)); }
}
