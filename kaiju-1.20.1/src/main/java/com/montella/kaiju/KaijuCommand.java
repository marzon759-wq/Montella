package com.montella.kaiju;

import com.mojang.brigadier.CommandDispatcher;
import com.montella.kaiju.entity.GhidorahEntity;
import com.montella.kaiju.entity.GodzillaEntity;
import com.montella.kaiju.entity.S;
import com.montella.kaiju.entity.TitanEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;

/** /kaiju fight | godzilla | ghidorah | clear */
public final class KaijuCommand {
    private KaijuCommand() {}

    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("kaiju").requires(s -> s.hasPermission(2))
                .then(Commands.literal("fight").executes(c -> fight(c.getSource())))
                .then(Commands.literal("godzilla").executes(c -> one(c.getSource(), KaijuMod.GODZILLA.get())))
                .then(Commands.literal("ghidorah").executes(c -> one(c.getSource(), KaijuMod.GHIDORAH.get())))
                .then(Commands.literal("clear").executes(c -> clear(c.getSource()))));
    }

    private static <T extends TitanEntity> T spawn(ServerLevel l, EntityType<T> type, Vec3 p, float yaw) {
        T t = type.create(l);
        if (t == null) return null;
        int y = l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) p.x, (int) p.z);
        t.moveTo(p.x, y, p.z, yaw, 0);
        t.yBodyRot = yaw; t.yHeadRot = yaw;
        t.finalizeSpawn(l, l.getCurrentDifficultyAt(t.blockPosition()), MobSpawnType.COMMAND, null, null);
        l.addFreshEntity(t);
        return t;
    }

    private static int fight(CommandSourceStack s) {
        ServerLevel l = s.getLevel();
        Vec3 o = s.getPosition();
        float yaw = s.getRotation().y;
        Vec3 f = Vec3.directionFromRotation(0, yaw);
        Vec3 side = new Vec3(-f.z, 0, f.x);
        Vec3 mid = o.add(f.scale(60));
        Vec3 a = mid.add(side.scale(28)), b = mid.subtract(side.scale(28));
        GodzillaEntity g = spawn(l, KaijuMod.GODZILLA.get(), a, yawBetween(a, b));
        GhidorahEntity k = spawn(l, KaijuMod.GHIDORAH.get(), b, yawBetween(b, a));
        if (g != null && k != null) { g.setTarget(k); k.setTarget(g); g.startRoar(); }
        s.sendSuccess(() -> Component.translatable("kaiju.command.fight"), true);
        return 1;
    }

    private static float yawBetween(Vec3 from, Vec3 to) {
        return (float) (Math.atan2(to.z - from.z, to.x - from.x) * 180 / Math.PI) - 90f;
    }

    private static int one(CommandSourceStack s, EntityType<? extends TitanEntity> type) {
        Vec3 f = Vec3.directionFromRotation(0, s.getRotation().y);
        Vec3 p = s.getPosition().add(f.scale(40));
        spawn(s.getLevel(), type, p, s.getRotation().y + 180);
        return 1;
    }

    private static int clear(CommandSourceStack s) {
        int n = 0;
        for (var e : s.getLevel().getAllEntities()) if (e instanceof TitanEntity t) { t.discard(); n++; }
        final int c = n;
        s.sendSuccess(() -> Component.literal("Removed " + c + " titans"), true);
        return n;
    }

    @SuppressWarnings("unused")
    private static boolean busy(TitanEntity t) { return t.state() != S.IDLE; }
}
