package com.krolasyon.beyblade.network;

import com.krolasyon.beyblade.compat.V;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/** Client to server commands (Forge 1.21.1 channel API). */
public final class ModNetwork {
   public static final int CMD_RECALL = 0;
   public static final int CMD_ORDER = 20;
   public static final int CMD_MODE_CYCLE = 39;
   public static final int CMD_MODE = 40;
   public static final int CMD_SPIRIT = 50;
   private static SimpleChannel CHANNEL;

   private ModNetwork() {
   }

   public static void register() {
      CHANNEL = ChannelBuilder.named(V.rl("librabeyblade", "main")).networkProtocolVersion(1).simpleChannel();
      CHANNEL.messageBuilder(Command.class, 0)
         .encoder(Command::encode)
         .decoder(Command::decode)
         .consumerMainThread((m, ctx) -> {
            CommandHandler.handle(ctx.getSender(), m.cmd);
            ctx.setPacketHandled(true);
         })
         .add();
   }

   public static void sendCommand(int cmd) {
      CHANNEL.send(new Command(cmd), PacketDistributor.SERVER.noArg());
   }

   public static record Command(int cmd) {
      static void encode(Command m, FriendlyByteBuf buf) {
         buf.writeByte(m.cmd);
      }

      static Command decode(FriendlyByteBuf buf) {
         return new Command(buf.readByte());
      }
   }
}
