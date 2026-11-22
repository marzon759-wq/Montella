package com.krolasyon.kaiju;

import com.krolasyon.kaiju.entity.*;
import com.krolasyon.kaiju.item.ControllerItem;
import com.krolasyon.kaiju.item.KaijuEggItem;
import com.krolasyon.kaiju.net.Net;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Comparator;
import java.util.List;

@Mod(KaijuMod.MODID)
public class KaijuMod {
    public static final String MODID = "kaiju";
    public static final Logger LOG = LogManager.getLogger("Kaiju");

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<EntityType<AtomicKaijuEntity>> ATOMIC = ENTITIES.register("atomic_kaiju",
            () -> EntityType.Builder.<AtomicKaijuEntity>of(AtomicKaijuEntity::new, MobCategory.MONSTER).sized(20f, 48f)
                    .clientTrackingRange(40).updateInterval(1).fireImmune().build("atomic_kaiju"));
    public static final RegistryObject<EntityType<DestoroyahEntity>> DESTOROYAH = ENTITIES.register("destoroyah",
            () -> EntityType.Builder.<DestoroyahEntity>of(DestoroyahEntity::new, MobCategory.MONSTER).sized(22f, 48f)
                    .clientTrackingRange(40).updateInterval(1).fireImmune().build("destoroyah"));
    public static final RegistryObject<EntityType<DebrisEntity>> DEBRIS = ENTITIES.register("debris",
            () -> EntityType.Builder.<DebrisEntity>of(DebrisEntity::new, MobCategory.MISC).sized(2f, 2f)
                    .clientTrackingRange(32).updateInterval(1).fireImmune().build("debris"));
    public static final RegistryObject<EntityType<SeveredPartEntity>> SEVERED = ENTITIES.register("severed_part",
            () -> EntityType.Builder.<SeveredPartEntity>of(SeveredPartEntity::new, MobCategory.MISC).sized(4f, 4f)
                    .clientTrackingRange(32).updateInterval(1).fireImmune().build("severed_part"));

    public static final RegistryObject<Item> EGG = ITEMS.register("atomic_kaiju_egg",
            () -> new KaijuEggItem(ATOMIC, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> DESTOROYAH_EGG = ITEMS.register("destoroyah_egg",
            () -> new KaijuEggItem(DESTOROYAH, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> CONTROLLER = ITEMS.register("kaiju_controller",
            () -> new ControllerItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public KaijuMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        ITEMS.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, KaijuConfig.SPEC);
        bus.addListener((ModConfigEvent e) -> { if (e.getConfig().getSpec() == KaijuConfig.SPEC) KaijuConfig.load(); });
        bus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(ATOMIC.get(), KaijuEntity.attributes().build());
            e.put(DESTOROYAH.get(), KaijuEntity.attributes().build());
        });
        bus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(() -> {
            Net.init();
            com.krolasyon.kaiju.data.KaijuSpec.get("atomic_kaiju");
            com.krolasyon.kaiju.data.KaijuSpec.get("destoroyah");
        }));
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) { e.accept(EGG); e.accept(DESTOROYAH_EGG); }
            if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) e.accept(CONTROLLER);
        });
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(this::mount);
        if (FMLEnvironment.dist == Dist.CLIENT) com.krolasyon.kaiju.client.KaijuClient.init(bus);
    }

    void mount(EntityMountEvent e) {
        if (e.isDismounting() && !e.getLevel().isClientSide && e.getEntityBeingMounted() instanceof KaijuEntity k
                && k.controller == e.getEntityMounting() && !k.allowDismount && e.getEntityMounting() instanceof ServerPlayer sp && !sp.hasDisconnected() && sp.isAlive()) {
            e.setCanceled(true);
        }
    }

    static KaijuEntity nearest(CommandSourceStack s) {
        Vec3 p = s.getPosition();
        List<KaijuEntity> all = s.getLevel().getEntitiesOfClass(KaijuEntity.class, new net.minecraft.world.phys.AABB(p, p).inflate(800));
        return all.stream().min(Comparator.comparingDouble(k -> k.position().distanceToSqr(p))).orElse(null);
    }

    void commands(RegisterCommandsEvent ev) {
        ev.getDispatcher().register(Commands.literal("kaiju").requires(s -> s.hasPermission(2))
                .then(Commands.literal("versus").executes(c -> fight(c.getSource(), 150, true))
                        .then(Commands.argument("distance", FloatArgumentType.floatArg(20, 1000)).executes(c -> fight(c.getSource(), FloatArgumentType.getFloat(c, "distance"), true))))
                .then(Commands.literal("fight").executes(c -> fight(c.getSource(), 150))
                        .then(Commands.argument("distance", FloatArgumentType.floatArg(20, 1000)).executes(c -> fight(c.getSource(), FloatArgumentType.getFloat(c, "distance")))))
                .then(Commands.literal("attack").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> {
                            java.util.Set<String> ids = new java.util.TreeSet<>(com.krolasyon.kaiju.data.KaijuSpec.get("atomic_kaiju").attackById.keySet());
                            ids.addAll(com.krolasyon.kaiju.data.KaijuSpec.get("destoroyah").attackById.keySet());
                            return SharedSuggestionProvider.suggest(ids, b);
                        })
                        .executes(c -> {
                            KaijuEntity k = nearest(c.getSource());
                            String id = StringArgumentType.getString(c, "id");
                            if (k == null) return 0;
                            if (k.state != KaijuEntity.State.IDLE && k.state != KaijuEntity.State.DOWN) k.state = KaijuEntity.State.IDLE;
                            k.cooldown.remove(id);
                            boolean ok = k.startAttack(id);
                            c.getSource().sendSuccess(() -> Component.literal(id + (ok ? " ok" : " not possible now")), false);
                            return ok ? 1 : 0;
                        })))
                .then(Commands.literal("list").executes(c -> {
                    var spec = com.krolasyon.kaiju.data.KaijuSpec.get("atomic_kaiju");
                    c.getSource().sendSuccess(() -> Component.literal(spec.attacks.size() + " attacks: " + String.join(", ", spec.attackById.keySet())), false);
                    return 1;
                }))
                .then(Commands.literal("control").executes(c -> c.getSource().getEntity() instanceof ServerPlayer sp && ControllerItem.take(sp) ? 1 : 0))
                .then(Commands.literal("release").executes(c -> {
                    if (c.getSource().getEntity() instanceof ServerPlayer sp && sp.getVehicle() instanceof KaijuEntity k) k.releaseControl();
                    return 1;
                }))
                .then(Commands.literal("scale").then(Commands.argument("value", FloatArgumentType.floatArg(0.5f, 20f)).executes(c -> {
                    KaijuEntity k = nearest(c.getSource());
                    if (k != null) k.setScale(FloatArgumentType.getFloat(c, "value"));
                    return 1;
                })))
                .then(Commands.literal("ai").then(Commands.argument("on", BoolArgumentType.bool()).executes(c -> {
                    boolean on = BoolArgumentType.getBool(c, "on");
                    for (KaijuEntity k : c.getSource().getLevel().getEntitiesOfClass(KaijuEntity.class, new net.minecraft.world.phys.AABB(c.getSource().getPosition(), c.getSource().getPosition()).inflate(1000)))
                        k.brain.enabled = on;
                    return 1;
                })))
                .then(Commands.literal("griefing").then(Commands.argument("on", BoolArgumentType.bool()).executes(c -> {
                    KaijuConfig.griefing = BoolArgumentType.getBool(c, "on");
                    return 1;
                })))
                .then(Commands.literal("sever").then(Commands.argument("part", StringArgumentType.word()).executes(c -> {
                    KaijuEntity k = nearest(c.getSource());
                    if (k != null) k.sever(k.spec.severGroups.indexOf(StringArgumentType.getString(c, "part")));
                    return 1;
                })))
                .then(Commands.literal("burn").executes(c -> {
                    KaijuEntity k = nearest(c.getSource());
                    if (k != null) java.util.Arrays.fill(k.heat, 0.9f);
                    return 1;
                }))
                .then(Commands.literal("clear").executes(c -> {
                    for (KaijuEntity k : c.getSource().getLevel().getEntitiesOfClass(KaijuEntity.class, new net.minecraft.world.phys.AABB(c.getSource().getPosition(), c.getSource().getPosition()).inflate(2000)))
                        k.discard();
                    return 1;
                })));
    }

    static int fight(CommandSourceStack s, float dist) { return fight(s, dist, false); }

    static int fight(CommandSourceStack s, float dist, boolean versus) {
        ServerLevel sl = s.getLevel();
        Vec3 p = s.getPosition();
        float yaw = s.getRotation().y;
        Vec3 fwd = Vec3.directionFromRotation(0, yaw);
        Vec3 mid = p.add(fwd.scale(dist * 0.5 + 80));
        Vec3 side = new Vec3(-fwd.z, 0, fwd.x);
        for (int i = 0; i < 2; i++) {
            Vec3 at = mid.add(side.scale((i == 0 ? -1 : 1) * dist * 0.5));
            KaijuEntity k = (versus && i == 1 ? DESTOROYAH.get() : ATOMIC.get()).create(sl);
            if (k == null) return 0;
            double y = sl.getHeight(Heightmap.Types.OCEAN_FLOOR, (int) at.x, (int) at.z);
            float face = (float) Math.toDegrees(Math.atan2(-(mid.x - at.x), mid.z - at.z));
            k.moveTo(at.x, y, at.z, face, 0);
            k.faceYaw = face;
            k.setScale((float) KaijuConfig.scale);
            sl.addFreshEntity(k);
        }
        s.sendSuccess(() -> Component.literal("Two kaiju are facing each other " + (int) dist + " blocks apart."), false);
        return 1;
    }
}
