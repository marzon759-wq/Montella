package com.krolasyon.beyblade.network;

import com.krolasyon.beyblade.compat.V;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
   private static final String VERSION = "1";
   public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(V.rl("librabeyblade", "main"), () -> "1", "1"::equals, "1"::equals);
   public static final int CMD_RECALL = 0;
   public static final int CMD_ORDER = 20;
   public static final int CMD_MODE_CYCLE = 39;
   public static final int CMD_MODE = 40;
   public static final int CMD_SPIRIT = 50;

   private ModNetwork() {
   }

   public static void register() {
      CHANNEL.registerMessage(0, ModNetwork.Command.class, ModNetwork.Command::encode, ModNetwork.Command::decode, ModNetwork.Command::handle);
   }

   public static void sendCommand(int cmd) {
      CHANNEL.sendToServer(new ModNetwork.Command(cmd));
   }

   public static record Command(int cmd) {
      static void encode(ModNetwork.Command m, FriendlyByteBuf buf) {
         buf.writeByte(m.cmd);
      }

      static ModNetwork.Command decode(FriendlyByteBuf buf) {
         return new ModNetwork.Command(buf.readByte());
      }

      static void handle(ModNetwork.Command m, Supplier<Context> ctx) {
         Context c = ctx.get();
         c.enqueueWork(() -> CommandHandler.handle(c.getSender(), m.cmd));
         c.setPacketHandled(true);
      }
   }
}
