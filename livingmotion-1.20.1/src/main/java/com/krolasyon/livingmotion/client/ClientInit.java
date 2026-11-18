package com.krolasyon.livingmotion.client;

import com.krolasyon.livingmotion.client.model.MobKind;
import com.krolasyon.livingmotion.client.model.MobRenderer;
import com.krolasyon.livingmotion.client.model.ReplacedMob;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ClientInit {
    private ClientInit() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientInit::renderers);
        MinecraftForge.EVENT_BUS.register(MotionFX.class);
        MinecraftForge.EVENT_BUS.register(Effects.class);
        MinecraftForge.EVENT_BUS.addListener(ClientInit::leave);
        AutoTest.init();
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        reg(e, EntityType.ZOMBIE, "zombie", MobKind.ZOMBIE, ZombieRenderer::new);
        reg(e, EntityType.HUSK, "husk", MobKind.HUSK, HuskRenderer::new);
        reg(e, EntityType.DROWNED, "drowned", MobKind.DROWNED, DrownedRenderer::new);
        reg(e, EntityType.SKELETON, "skeleton", MobKind.SKELETON, SkeletonRenderer::new);
        reg(e, EntityType.STRAY, "stray", MobKind.STRAY, StrayRenderer::new);
        reg(e, EntityType.WITHER_SKELETON, "wither_skeleton", MobKind.WITHER_SKELETON, WitherSkeletonRenderer::new);
        reg(e, EntityType.CREEPER, "creeper", MobKind.CREEPER, CreeperRenderer::new);
        reg(e, EntityType.SPIDER, "spider", MobKind.SPIDER, SpiderRenderer::new);
        reg(e, EntityType.CAVE_SPIDER, "cave_spider", MobKind.CAVE_SPIDER, CaveSpiderRenderer::new);
        reg(e, EntityType.COW, "cow", MobKind.COW, CowRenderer::new);
        reg(e, EntityType.PIG, "pig", MobKind.PIG, PigRenderer::new);
        reg(e, EntityType.CHICKEN, "chicken", MobKind.CHICKEN, ChickenRenderer::new);
        reg(e, EntityType.SHEEP, "sheep", MobKind.SHEEP, SheepRenderer::new);
        reg(e, EntityType.IRON_GOLEM, "iron_golem", MobKind.IRON_GOLEM, IronGolemRenderer::new);
        reg(e, EntityType.ENDERMAN, "enderman", MobKind.ENDERMAN, EndermanRenderer::new);
        reg(e, EntityType.VILLAGER, "villager", MobKind.VILLAGER, VillagerRenderer::new);
        reg(e, EntityType.WANDERING_TRADER, "wandering_trader", MobKind.WANDERING_TRADER, WanderingTraderRenderer::new);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void reg(EntityRenderersEvent.RegisterRenderers e, EntityType type, String id, MobKind kind,
                            EntityRendererProvider vanilla) {
        e.registerEntityRenderer(type, ctx -> new MobRenderer(ctx, type, id, kind, vanilla.create(ctx)));
    }

    private static void leave(EntityLeaveLevelEvent e) {
        if (e.getLevel().isClientSide()) ReplacedMob.forget(e.getEntity().getId());
    }
}
