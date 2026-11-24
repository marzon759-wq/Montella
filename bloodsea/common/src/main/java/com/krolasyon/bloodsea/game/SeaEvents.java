package com.krolasyon.bloodsea.game;

import com.krolasyon.bloodsea.BloodSea;
import com.krolasyon.bloodsea.block.EndingBlock;
import com.krolasyon.bloodsea.compat.Compat;
import com.krolasyon.bloodsea.net.Msg;
import com.krolasyon.bloodsea.registry.ModFluids;
import com.krolasyon.bloodsea.world.Regions;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Getting there and back, the region you are in, and what the Pale Sun and the Blood Moon do to you. */
public final class SeaEvents {
    public static final ResourceKey<Level> SEA = ResourceKey.create(Registries.DIMENSION, Compat.rl("blood_sea"));

    private static final Map<UUID, Regions.Region> REGION = new HashMap<>();
    private static final Map<UUID, Boolean> DAY = new HashMap<>();
    private static final Map<UUID, Integer> STARE = new HashMap<>();

    private SeaEvents() {}

    public static boolean inSea(ServerPlayer p) { return p.level().dimension() == SEA; }

    // ================================================================== commands
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        LiteralArgumentBuilder<CommandSourceStack> c = Commands.literal("bloodsea");
        c.then(Commands.literal("enter").executes(ctx -> { enter(ctx.getSource().getPlayerOrException()); return 1; }));
        c.then(Commands.literal("leave").executes(ctx -> { leave(ctx.getSource().getPlayerOrException()); return 1; }));
        c.then(Commands.literal("where").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Regions.Region r = Regions.at(p.getX(), p.getZ());
            p.sendSystemMessage(Component.translatable("region.bloodsea." + Regions.key(r)).append(" (" + (int) Regions.dist(p.getX(), p.getZ()) + ")"));
            return 1;
        }));
        e.getDispatcher().register(c);
    }

    public static void enter(ServerPlayer p) {
        ServerLevel sea = p.server.getLevel(SEA);
        if (sea == null) {
            p.sendSystemMessage(Component.literal("The Blood Sea is missing (datapack not loaded)."));
            return;
        }
        if (!p.getInventory().contains(new ItemStack(Items.SPRUCE_BOAT))) p.getInventory().add(new ItemStack(Items.SPRUCE_BOAT));
        p.teleportTo(sea, 0.5, Regions.SEA + 1, 0.5, 180f, 0f);
        REGION.remove(p.getUUID());
        DAY.remove(p.getUUID());
    }

    public static void leave(ServerPlayer p) {
        ServerLevel ow = p.server.overworld();
        BlockPos s = ow.getSharedSpawnPos();
        p.teleportTo(ow, s.getX() + 0.5, ow.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, s.getX(), s.getZ()) + 0.1, s.getZ() + 0.5, p.getYRot(), 0f);
    }

    // ================================================================== interaction
    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().getBlockState(e.getPos()).getBlock() instanceof EndingBlock) {
            if (!e.getLevel().isClientSide) EndingBlock.use(e.getLevel(), e.getPos(), e.getEntity());
            e.setCanceled(true);
        }
    }

    public static void onClientMsg(ServerPlayer p, Msg m) {
        if (m.type.equals("ending_done")) Endings.finish(p);
    }

    // ================================================================== every tick, for each player
    public static void playerTick(ServerPlayer p) {
        if (!inSea(p) || p.tickCount % 10 != 0) return;
        ServerLevel lvl = p.serverLevel();
        UUID id = p.getUUID();
        if (Endings.active(p)) return;

        // ---- where you are
        Regions.Region r = Regions.at(p.getX(), p.getZ());
        if (REGION.get(id) != r) {
            REGION.put(id, r);
            title(p, "region.bloodsea." + Regions.key(r), "region.bloodsea." + Regions.key(r) + ".sub", 10, 70, 20);
        }
        // ---- off the edge of the world
        if (r == Regions.Region.EDGE && Regions.dist(p.getX(), p.getZ()) > Regions.EDGE_R && p.getY() < Regions.SEA - 50) {
            Endings.begin(p, "edge");
            return;
        }

        // ---- the sky: the Pale Sun by day, the Blood Moon by night
        boolean day = com.krolasyon.bloodsea.world.SeaTime.isDay(lvl);
        Boolean was = DAY.get(id);
        if (was == null || was != day) {
            DAY.put(id, day);
            if (was != null) {
                title(p, day ? "sky.bloodsea.sun" : "sky.bloodsea.moon", day ? "sky.bloodsea.sun.sub" : "sky.bloodsea.moon.sub", 20, 80, 30);
                lvl.playSound(null, p.blockPosition(), day ? SoundEvents.BEACON_ACTIVATE : SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, 1f, day ? 0.5f : 0.6f);
            }
        }
        if (p.tickCount % 20 != 0) return;
        boolean inBlood = p.isInFluidType(ModFluids.BLOOD_TYPE.get());
        boolean open = lvl.canSeeSky(BlockPos.containing(p.getEyePosition()));
        if (day) {
            // the Pale Sun: it sees everything, and it dries you out. Do not look at it.
            if (open && !inBlood) p.causeFoodExhaustion(0.45f);
            double a = lvl.getSunAngle(1f);
            Vec3 sun = new Vec3(-Math.sin(a), Math.cos(a), 0);
            int s = STARE.getOrDefault(id, 0);
            if (open && p.getViewVector(1f).dot(sun) > 0.985) {
                s++;
                if (s >= 2) {
                    p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, false));
                    p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0, false, false));
                    actionBar(p, "msg.bloodsea.stare");
                    s = -6;
                }
            } else if (s > 0) s = 0;
            else if (s < 0) s++;
            STARE.put(id, s);
        } else {
            // the Blood Moon: the sea feeds you if you go into it - and it makes you hungry for more
            if (inBlood) {
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false));
                p.causeFoodExhaustion(0.6f);
            }
            if (lvl.random.nextInt(40) == 0)
                lvl.playSound(null, p.getX() + lvl.random.nextGaussian() * 6, p.getY(), p.getZ() + lvl.random.nextGaussian() * 6,
                        SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.7f, 0.6f + lvl.random.nextFloat() * 0.3f);
        }
    }

    static void title(ServerPlayer p, String title, String sub, int in, int stay, int out) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(in, stay, out));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(sub)));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(title)));
    }

    static void actionBar(ServerPlayer p, String key) {
        p.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable(key)));
    }

    static void log(String s, Object... a) { BloodSea.LOG.info(s, a); }
}
