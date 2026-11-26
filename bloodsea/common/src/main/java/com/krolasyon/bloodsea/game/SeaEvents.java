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
        c.then(Commands.literal("start").executes(ctx -> { com.krolasyon.bloodsea.story.Story.start(ctx.getSource().getPlayerOrException()); return 1; }));
        c.then(Commands.literal("stage").requires(s -> s.hasPermission(2)).then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                .executes(ctx -> { var p = ctx.getSource().getPlayerOrException(); com.krolasyon.bloodsea.story.Story.jump(p, com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "name")); return 1; })));
        c.then(Commands.literal("leave").executes(ctx -> { leave(ctx.getSource().getPlayerOrException()); return 1; }));
        c.then(Commands.literal("summon").requires(s -> s.hasPermission(2))
                .then(Commands.argument("kind", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(new String[]{"wake", "gatherer", "caller", "changer", "leviathan"}, b))
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            boolean ok = Creatures.spawn(p.serverLevel(), p, com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "kind"));
                            return ok ? 1 : 0;
                        })));
        c.then(Commands.literal("debug").requires(s -> s.hasPermission(2))
                .then(Commands.literal("freeze").then(Commands.argument("on", com.mojang.brigadier.arguments.BoolArgumentType.bool()).executes(ctx -> {
                    com.krolasyon.bloodsea.entity.SeaCreature.FROZEN = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "on");
                    return 1;
                })))
                .then(Commands.literal("state").then(Commands.argument("s", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
                        .executes(ctx -> debugState(ctx.getSource().getPlayerOrException(), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "s"), null))
                        .then(Commands.argument("action", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(ctx ->
                                debugState(ctx.getSource().getPlayerOrException(), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "s"),
                                        com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "action")))))));
        c.then(Commands.literal("cut").requires(s -> s.hasPermission(2)).then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.word())
                .executes(ctx -> { com.krolasyon.bloodsea.story.Story.play(ctx.getSource().getPlayerOrException(), com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "id"), () -> {}); return 1; })));
        c.then(Commands.literal("moveraft").requires(s -> s.hasPermission(2)).then(Commands.argument("x", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg())
                .then(Commands.argument("z", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    double x = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(ctx, "x"), z = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(ctx, "z");
                    ServerLevel sea = p.server.getLevel(SEA);
                    var r = raftFor(sea, p);
                    p.stopRiding();
                    int cx = net.minecraft.util.Mth.floor(x) >> 4, cz = net.minecraft.util.Mth.floor(z) >> 4;
                    sea.setChunkForced(cx, cz, true);
                    // the destination loads first, with the player still on the raft; then the raft goes, then the player
                    com.krolasyon.bloodsea.story.Story.later(80, () -> {
                        r.teleportTo(x, com.krolasyon.bloodsea.entity.RaftEntity.Y, z);
                        for (var c2 : sea.getEntitiesOfClass(com.krolasyon.bloodsea.entity.PipEntity.class, new net.minecraft.world.phys.AABB(-4000, -64, -4000, 4000, 320, 4000))) {
                            c2.stopRiding();
                            c2.teleportTo(x - 1.4, com.krolasyon.bloodsea.entity.RaftEntity.Y + 0.6, z - 1.4);
                        }
                    });
                    com.krolasyon.bloodsea.story.Story.later(84, () -> {
                        p.teleportTo(sea, x, com.krolasyon.bloodsea.entity.RaftEntity.Y + 0.6, z - 1.2, p.getYRot(), 0);
                        BloodSea.LOG.info("[TEST] after move: raft {}", sea.getEntity(r.getUUID()));
                    });
                    com.krolasyon.bloodsea.story.Story.later(400, () -> sea.setChunkForced(cx, cz, false));
                    return 1;
                }))));
        c.then(Commands.literal("where").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Regions.Region r = Regions.at(p.getX(), p.getZ());
            p.sendSystemMessage(Component.translatable("region.bloodsea." + Regions.key(r)).append(" (" + (int) Regions.dist(p.getX(), p.getZ()) + ")"));
            return 1;
        }));
        e.getDispatcher().register(c);
    }

    private static int debugState(ServerPlayer p, int state, String action) {
        for (com.krolasyon.bloodsea.entity.SeaCreature c : p.serverLevel().getEntitiesOfClass(com.krolasyon.bloodsea.entity.SeaCreature.class, p.getBoundingBox().inflate(120))) {
            c.go(state);
            if (action != null) c.playAction(action);
        }
        return 1;
    }

    public static com.krolasyon.bloodsea.entity.RaftEntity enter(ServerPlayer p) {
        ServerLevel sea = p.server.getLevel(SEA);
        if (sea == null) {
            p.sendSystemMessage(Component.literal("The Blood Sea is missing (datapack not loaded)."));
            return null;
        }
        com.krolasyon.bloodsea.entity.RaftEntity raft = raftFor(sea, p);
        p.teleportTo(sea, raft.getX(), raft.getY() + com.krolasyon.bloodsea.entity.RaftEntity.TOP + 0.05, raft.getZ() + 1.5, 180f, 0f);
        REGION.remove(p.getUUID());
        DAY.remove(p.getUUID());
        return raft;
    }

    /** the raft: the one already afloat near the middle, or a new one */
    public static com.krolasyon.bloodsea.entity.RaftEntity raftFor(ServerLevel sea, ServerPlayer p) {
        net.minecraft.nbt.CompoundTag d = p.getPersistentData();
        if (d.hasUUID("bloodsea_raft") && sea.getEntity(d.getUUID("bloodsea_raft")) instanceof com.krolasyon.bloodsea.entity.RaftEntity r0 && !r0.isRemoved()) return r0;
        sea.getChunkAt(BlockPos.containing(0, Regions.SEA, 0));
        var list = sea.getEntitiesOfClass(com.krolasyon.bloodsea.entity.RaftEntity.class, new net.minecraft.world.phys.AABB(-4000, -64, -4000, 4000, 320, 4000));
        com.krolasyon.bloodsea.entity.RaftEntity r;
        if (!list.isEmpty()) r = list.get(0);
        else {
            r = com.krolasyon.bloodsea.registry.ModEntities.RAFT.get().create(sea);
            // the raft not loaded yet (after dying far from it, say): it comes back where it was last seen, not at the start;
            // the old one, once its chunk loads, is the extra that oneRaft clears away
            if (d.contains("bloodsea_raft_x"))
                r.moveTo(d.getDouble("bloodsea_raft_x"), com.krolasyon.bloodsea.entity.RaftEntity.Y, d.getDouble("bloodsea_raft_z"), d.getFloat("bloodsea_raft_yaw"), 0f);
            else r.moveTo(0.5, com.krolasyon.bloodsea.entity.RaftEntity.Y, 0.5, 0f, 0f);
            sea.addFreshEntity(r);
        }
        d.putUUID("bloodsea_raft", r.getUUID());
        return r;
    }

    /** only ever one raft: any other found near the player (a double made while the world was still loading) goes */
    static void oneRaft(ServerPlayer p, ServerLevel lvl) {
        var rafts = lvl.getEntitiesOfClass(com.krolasyon.bloodsea.entity.RaftEntity.class, p.getBoundingBox().inflate(96));
        if (rafts.size() < 2) return;
        net.minecraft.nbt.CompoundTag d = p.getPersistentData();
        com.krolasyon.bloodsea.entity.RaftEntity keep = null;
        for (var r : rafts) if (r.getControllingPassenger() == p) keep = r;
        if (keep == null && d.hasUUID("bloodsea_raft")) for (var r : rafts) if (r.getUUID().equals(d.getUUID("bloodsea_raft"))) keep = r;
        if (keep == null) keep = rafts.get(0);
        for (var r : rafts) if (r != keep) { r.ejectPassengers(); r.discard(); }
        d.putUUID("bloodsea_raft", keep.getUUID());
        log("[RAFT] removed {} extra raft(s)", rafts.size() - 1);
    }

    // ================================================================== the blood: touch it and it kills you
    private static final Map<UUID, Integer> BURN = new HashMap<>();

    public static void bloodTick(ServerPlayer p) {
        if (!inSea(p) || p.isCreative() || p.isSpectator() || !p.isAlive() || com.krolasyon.bloodsea.story.Story.inCut(p)) { BURN.remove(p.getUUID()); return; }
        if (!p.isInFluidType(ModFluids.BLOOD_TYPE.get())) { BURN.remove(p.getUUID()); return; }
        int t = BURN.merge(p.getUUID(), 1, Integer::sum);
        if (t == 1) {
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false));
            p.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 3, false, false));
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 1.2f, 0.5f);
            actionBar(p, "msg.bloodsea.blood_burn");
        }
        if (t == 14) {
            var type = p.serverLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, Compat.rl("blood")));
            p.hurt(new net.minecraft.world.damagesource.DamageSource(type), Float.MAX_VALUE);
            BURN.remove(p.getUUID());
        }
    }

    public static void leave(ServerPlayer p) {
        ServerLevel ow = p.server.overworld();
        BlockPos s = ow.getSharedSpawnPos();
        p.teleportTo(ow, s.getX() + 0.5, ow.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, s.getX(), s.getZ()) + 0.1, s.getZ() + 0.5, p.getYRot(), 0f);
    }

    // ================================================================== interaction
    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().getBlockState(e.getPos()).getBlock() == com.krolasyon.bloodsea.registry.ModProps.get("heart")) {
            if (!e.getLevel().isClientSide && e.getEntity() instanceof ServerPlayer sp) Endings.begin(sp, "bone");
            e.setCanceled(true);
            return;
        }
        if (e.getLevel().getBlockState(e.getPos()).getBlock() instanceof EndingBlock) {
            if (!e.getLevel().isClientSide) EndingBlock.use(e.getLevel(), e.getPos(), e.getEntity());
            e.setCanceled(true);
        }
    }

    public static void onClientMsg(ServerPlayer p, Msg m) {
        if (m.type.equals("ending_done")) Endings.finish(p);
        if (m.type.equals("cut_done")) com.krolasyon.bloodsea.story.Story.onCutDone(p);
        if (m.type.equals("pick")) com.krolasyon.bloodsea.story.Story.onPick(p, m.s("id"), m.s("pick"));
    }

    // ================================================================== every tick, for each player
    public static void playerTick(ServerPlayer p) {
        int now = p.server.getTickCount();
        if (!inSea(p) || now % 10 != 0) return;
        ServerLevel lvl = p.serverLevel();
        UUID id = p.getUUID();
        if (Endings.active(p)) return;

        // ---- where you are
        Regions.Region r = Regions.at(p.getX(), p.getZ());
        if (REGION.get(id) != r) {
            REGION.put(id, r);
            if (!com.krolasyon.bloodsea.story.Story.inCut(p)) title(p, "region.bloodsea." + Regions.key(r), "region.bloodsea." + Regions.key(r) + ".sub", 10, 70, 20);
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
            if (was != null && !com.krolasyon.bloodsea.story.Story.active(p)) {
                title(p, day ? "sky.bloodsea.sun" : "sky.bloodsea.moon", day ? "sky.bloodsea.sun.sub" : "sky.bloodsea.moon.sub", 20, 80, 30);
                lvl.playSound(null, p.blockPosition(), day ? SoundEvents.BEACON_ACTIVATE : SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, 1f, day ? 0.5f : 0.6f);
            }
        }
        if (now % 20 != 0) return;
        if (now % 100 == 0) oneRaft(p, lvl);
        if (!p.isCreative() || Boolean.getBoolean("bloodsea.creatures")) Creatures.tick(p, lvl, r);
        com.krolasyon.bloodsea.story.Story.tick(p);
        boolean inBlood = p.isInFluidType(ModFluids.BLOOD_TYPE.get());
        boolean open = lvl.canSeeSky(BlockPos.containing(p.getEyePosition()));
        if (day) {
            // the Pale Sun: it sees everything, and it dries you out. Do not look at it.
            if (open && !inBlood) p.causeFoodExhaustion(0.45f);
            double a = lvl.getSunAngle(1f);
            Vec3 sun = new Vec3(-Math.sin(a), Math.cos(a), 0);
            int s = STARE.getOrDefault(id, 0);
            if (open && com.krolasyon.bloodsea.story.Story.eyeOpen(p) && p.getViewVector(1f).dot(sun) > 0.985) {
                s++;
                if (s == 1) lvl.playSound(null, p.blockPosition(), com.krolasyon.bloodsea.registry.ModSounds.get("watcher_stare"), SoundSource.AMBIENT, 1.2f, 1f);
                if (s >= 2) {
                    for (com.krolasyon.bloodsea.entity.SeaCreature c : lvl.getEntitiesOfClass(com.krolasyon.bloodsea.entity.SeaCreature.class, p.getBoundingBox().inflate(120)))
                        c.alert(p);
                    p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0, false, false));
                    lvl.playSound(null, p.blockPosition(), com.krolasyon.bloodsea.registry.ModSounds.get("heartbeat_near"), SoundSource.AMBIENT, 1.5f, 1f);
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

    /** a new player in a new world wakes on the raft at once: the story is the game, no command needed */
    @SubscribeEvent
    public static void onLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        String test = System.getProperty("bloodsea.autotest", "");
        if (!test.isEmpty() && !test.contains("auto") && !test.contains("video")) return;
        net.minecraft.nbt.CompoundTag d = p.getPersistentData();
        if (d.getBoolean("bloodsea_began") || d.contains("bloodsea_story")) return;
        d.putBoolean("bloodsea_began", true);
        com.krolasyon.bloodsea.story.Story.later(40, () -> { if (!p.hasDisconnected()) com.krolasyon.bloodsea.story.Story.start(p); });
    }

    /** dying out here puts you back on the raft */
    @SubscribeEvent
    public static void onRespawn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !com.krolasyon.bloodsea.story.Story.active(p)) return;
        p.server.execute(() -> {
            ServerLevel sea = p.server.getLevel(SEA);
            if (sea == null) return;
            var raft = raftFor(sea, p);
            var st = raft.local(0, com.krolasyon.bloodsea.entity.RaftEntity.TOP + 0.05, -1.2);
            p.teleportTo(sea, st.x, st.y, st.z, raft.getYRot(), 0f);
            p.getFoodData().setFoodLevel(12);
        });
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
