package com.krolasyon.hatman.story;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ServerEvents {
    private ServerEvents() {}

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent e) {
        Story.load(e.getServer());
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        Story.unload(e.getServer());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) Story.onLogin(sp);
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().isClientSide || !(e.getEntity() instanceof ServerPlayer sp)) return;
        if (!Story.isStoryPlayer(sp)) return;
        if (e.getHand() != InteractionHand.MAIN_HAND) {
            if (Story.hotspot(e.getPos()) != null) e.setCanceled(true);
            return;
        }
        if (Story.interact(sp, e.getPos())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        for (String root : new String[]{"hatman", "sapkaliadam"}) {
            LiteralArgumentBuilder<CommandSourceStack> c = Commands.literal(root);
            for (String s : new String[]{"start", "basla"})
                c.then(Commands.literal(s).executes(ctx -> {
                    Story.start(ctx.getSource().getPlayerOrException());
                    return 1;
                }));
            for (String s : new String[]{"stop", "bitir"})
                c.then(Commands.literal(s).executes(ctx -> {
                    Story.stop(ctx.getSource().getPlayerOrException(), true);
                    return 1;
                }));
            for (String s : new String[]{"status", "durum"})
                c.then(Commands.literal(s).executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(Story.status()), false);
                    return 1;
                }));
            c.then(Commands.literal("debug").requires(src -> src.hasPermission(2))
                    .then(Commands.literal("step").then(Commands.argument("id", StringArgumentType.word()).executes(ctx -> {
                        Story.debugStep(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "id"));
                        return 1;
                    })))
                    .then(Commands.literal("mood").then(Commands.argument("anger", IntegerArgumentType.integer(0, 100))
                            .then(Commands.argument("claim", IntegerArgumentType.integer(0, 100)).executes(ctx -> {
                                Story.S.anger = IntegerArgumentType.getInteger(ctx, "anger");
                                Story.S.claim = IntegerArgumentType.getInteger(ctx, "claim");
                                return 1;
                            }))))
                    .then(Commands.literal("give").executes(ctx -> {
                        Story.debugGiveAll(ctx.getSource().getPlayerOrException());
                        return 1;
                    }))
                    .then(Commands.literal("house").executes(ctx -> {
                        Story.debugHouse(ctx.getSource().getPlayerOrException());
                        return 1;
                    })));
            e.getDispatcher().register(c);
        }
    }
}
