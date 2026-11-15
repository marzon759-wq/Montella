package com.krolasyon.titans;

import com.krolasyon.titans.entity.AzhdarEntity;
import com.krolasyon.titans.entity.KorgathEntity;
import com.krolasyon.titans.entity.TitanEntity;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

@Mod(TitansMod.ID)
public class TitansMod {
    public static final String ID = "titanclash";

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);

    public static final RegistryObject<EntityType<KorgathEntity>> KORGATH = ENTITIES.register("korgath", () ->
            EntityType.Builder.<KorgathEntity>of(KorgathEntity::new, MobCategory.MISC).sized(12f, 38f).fireImmune()
                    .clientTrackingRange(32).updateInterval(1).setShouldReceiveVelocityUpdates(true).build("korgath"));
    public static final RegistryObject<EntityType<AzhdarEntity>> AZHDAR = ENTITIES.register("azhdar", () ->
            EntityType.Builder.<AzhdarEntity>of(AzhdarEntity::new, MobCategory.MISC).sized(12f, 40f).fireImmune()
                    .clientTrackingRange(32).updateInterval(1).setShouldReceiveVelocityUpdates(true).build("azhdar"));

    public static final RegistryObject<Item> KORGATH_EGG = ITEMS.register("korgath_spawn_egg", () -> new ForgeSpawnEggItem(KORGATH, 0x2b2927, 0xff7a1a, new Item.Properties()));
    public static final RegistryObject<Item> AZHDAR_EGG = ITEMS.register("azhdar_spawn_egg", () -> new ForgeSpawnEggItem(AZHDAR, 0x3a4660, 0xb98cff, new Item.Properties()));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("titans", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.titanclash")).icon(() -> KORGATH_EGG.get().getDefaultInstance())
            .displayItems((p, out) -> { out.accept(KORGATH_EGG.get()); out.accept(AZHDAR_EGG.get()); }).build());

    public TitansMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(KORGATH.get(), TitanEntity.attrs().build());
            e.put(AZHDAR.get(), TitanEntity.attrs().build());
        });
        MinecraftForge.EVENT_BUS.addListener(this::commands);
    }

    private void commands(RegisterCommandsEvent e) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("titans").requires(s -> s.hasPermission(2));
        for (String n : new String[]{"fight", "dovus"}) root.then(Commands.literal(n).executes(c -> fight(c.getSource())));
        root.then(Commands.literal("korgath").executes(c -> one(c.getSource(), KORGATH.get())));
        root.then(Commands.literal("azhdar").executes(c -> one(c.getSource(), AZHDAR.get())));
        for (String n : new String[]{"clear", "temizle"}) root.then(Commands.literal(n).executes(c -> {
            int k = 0;
            for (TitanEntity t : all(c.getSource().getLevel())) { t.discard(); k++; }
            final int kk = k;
            c.getSource().sendSuccess(() -> Component.translatable("titanclash.cleared", kk), true);
            return k;
        }));
        root.then(Commands.literal("act").then(Commands.argument("who", StringArgumentType.word()).then(Commands.argument("what", StringArgumentType.word()).executes(c -> {
            String who = StringArgumentType.getString(c, "who"), what = StringArgumentType.getString(c, "what");
            int k = 0;
            for (TitanEntity t : all(c.getSource().getLevel()))
                if (who.equals("all") || (who.startsWith("k") != t.hydra)) { t.forceAct(what); k++; }
            return k;
        }))));
        e.getDispatcher().register(root);
    }

    private static List<TitanEntity> all(ServerLevel lv) {
        List<TitanEntity> l = new ArrayList<>();
        for (Entity en : lv.getAllEntities()) if (en instanceof TitanEntity t) l.add(t);
        return l;
    }

    private static TitanEntity spawn(ServerLevel lv, EntityType<? extends TitanEntity> type, Vec3 at, Vec3 look) {
        TitanEntity t = type.create(lv);
        if (t == null) return null;
        int y = lv.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(at.x), Mth.floor(at.z));
        float yaw = (float) (Mth.atan2(-(look.x - at.x), look.z - at.z) * 180 / Math.PI);
        t.moveTo(at.x, y, at.z, yaw, 0);
        t.yBodyRot = t.yHeadRot = yaw;
        lv.addFreshEntity(t);
        return t;
    }

    private static int fight(CommandSourceStack src) {
        Vec3 p = src.getPosition();
        float yr = src.getRotation().y * Mth.DEG_TO_RAD;
        Vec3 f = new Vec3(-Mth.sin(yr), 0, Mth.cos(yr)), r = new Vec3(-f.z, 0, f.x);
        Vec3 c = p.add(f.scale(110));
        spawn(src.getLevel(), KORGATH.get(), c.add(r.scale(80)), c);
        spawn(src.getLevel(), AZHDAR.get(), c.add(r.scale(-80)), c);
        src.sendSuccess(() -> Component.translatable("titanclash.fight"), true);
        return 1;
    }

    private static int one(CommandSourceStack src, EntityType<? extends TitanEntity> type) {
        Vec3 p = src.getPosition();
        float yr = src.getRotation().y * Mth.DEG_TO_RAD;
        spawn(src.getLevel(), type, p.add(-Mth.sin(yr) * 80, 0, Mth.cos(yr) * 80), p);
        return 1;
    }
}
