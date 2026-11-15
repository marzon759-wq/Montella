package com.krolasyon.valdoria.registry;

import com.krolasyon.valdoria.ValdoriaMod;
import com.krolasyon.valdoria.entity.Gloomlynx;
import com.krolasyon.valdoria.entity.LumenMoth;
import com.krolasyon.valdoria.entity.Veilstag;
import com.krolasyon.valdoria.entity.Vinestalker;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ValdoriaMod.MODID);

    public static final RegistryObject<EntityType<Veilstag>> VEILSTAG = ENTITIES.register("veilstag", () ->
            EntityType.Builder.of(Veilstag::new, MobCategory.CREATURE).sized(1.0F, 1.7F).clientTrackingRange(10).build("veilstag"));
    public static final RegistryObject<EntityType<LumenMoth>> LUMEN_MOTH = ENTITIES.register("lumen_moth", () ->
            EntityType.Builder.of(LumenMoth::new, MobCategory.AMBIENT).sized(0.5F, 0.35F).clientTrackingRange(8).build("lumen_moth"));
    public static final RegistryObject<EntityType<Gloomlynx>> GLOOMLYNX = ENTITIES.register("gloomlynx", () ->
            EntityType.Builder.of(Gloomlynx::new, MobCategory.CREATURE).sized(0.8F, 0.9F).clientTrackingRange(10).build("gloomlynx"));
    public static final RegistryObject<EntityType<Vinestalker>> VINESTALKER = ENTITIES.register("vinestalker", () ->
            EntityType.Builder.of(Vinestalker::new, MobCategory.MONSTER).sized(1.3F, 0.9F).clientTrackingRange(10).build("vinestalker"));

    /** Forest animals spawn on grassy ground whatever the light: under the canopy it is always dim. */
    private static <T extends Animal> boolean onForestFloor(EntityType<T> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource rand) {
        return level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON);
    }

    @Mod.EventBusSubscriber(modid = ValdoriaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Events {
        @SubscribeEvent
        public static void attributes(EntityAttributeCreationEvent event) {
            event.put(VEILSTAG.get(), Veilstag.createAttributes().build());
            event.put(LUMEN_MOTH.get(), LumenMoth.createAttributes().build());
            event.put(GLOOMLYNX.get(), Gloomlynx.createAttributes().build());
            event.put(VINESTALKER.get(), Vinestalker.createAttributes().build());
        }

        @SubscribeEvent
        public static void spawns(SpawnPlacementRegisterEvent event) {
            event.register(VEILSTAG.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    ModEntities::onForestFloor, SpawnPlacementRegisterEvent.Operation.REPLACE);
            event.register(GLOOMLYNX.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    ModEntities::onForestFloor, SpawnPlacementRegisterEvent.Operation.REPLACE);
            event.register(LUMEN_MOTH.get(), SpawnPlacements.Type.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    LumenMoth::checkMothSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
            event.register(VINESTALKER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
    }
}
