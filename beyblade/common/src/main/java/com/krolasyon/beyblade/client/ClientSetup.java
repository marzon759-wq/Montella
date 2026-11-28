package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.registry.ModEntities;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
   modid = "librabeyblade",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public final class ClientSetup {
   public static final String CATEGORY = "key.categories.librabeyblade";
   public static final KeyMapping RECALL_KEY = new KeyMapping("key.librabeyblade.recall", Type.KEYSYM, 82, "key.categories.librabeyblade");
   public static final KeyMapping COMMAND_KEY = new KeyMapping("key.librabeyblade.commands", Type.KEYSYM, 66, "key.categories.librabeyblade");
   public static final KeyMapping SPIRIT_KEY = new KeyMapping("key.librabeyblade.spirit", Type.KEYSYM, 86, "key.categories.librabeyblade");
   public static final KeyMapping MODE_KEY = new KeyMapping("key.librabeyblade.mode", Type.KEYSYM, 77, "key.categories.librabeyblade");
   public static final KeyMapping[] ABILITY_KEYS = new KeyMapping[]{
      new KeyMapping("key.librabeyblade.ability1", Type.KEYSYM, 71, "key.categories.librabeyblade"),
      new KeyMapping("key.librabeyblade.ability2", Type.KEYSYM, 72, "key.categories.librabeyblade"),
      new KeyMapping("key.librabeyblade.ability3", Type.KEYSYM, 74, "key.categories.librabeyblade"),
      new KeyMapping("key.librabeyblade.ability4", Type.KEYSYM, 75, "key.categories.librabeyblade"),
      new KeyMapping("key.librabeyblade.ability5", Type.KEYSYM, 78, "key.categories.librabeyblade"),
      new KeyMapping("key.librabeyblade.ability6", Type.KEYSYM, 85, "key.categories.librabeyblade")
   };

   private ClientSetup() {
   }

   @SubscribeEvent
   public static void registerRenderers(RegisterRenderers event) {
      event.registerEntityRenderer((EntityType)ModEntities.LIBRA.get(), com.krolasyon.beyblade.compat.BeyGeoRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.LDRAGO.get(), com.krolasyon.beyblade.compat.BeyGeoRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.LEONE.get(), com.krolasyon.beyblade.compat.BeyGeoRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.EAGLE.get(), com.krolasyon.beyblade.compat.BeyGeoRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.PEGASUS.get(), com.krolasyon.beyblade.compat.BeyGeoRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.TEMPO.get(), com.krolasyon.beyblade.compat.BeyGeoRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.VARIARES.get(), com.krolasyon.beyblade.compat.BeyGeoRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.BEFFAL.get(), com.krolasyon.beyblade.compat.BeyGeoRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.STADIUM.get(), StadiumRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.COMMON.get(), com.krolasyon.beyblade.compat.BeyGeoRenderer::new);
   }

   @SubscribeEvent
   public static void registerKeys(RegisterKeyMappingsEvent event) {
      event.register(RECALL_KEY);
      event.register(COMMAND_KEY);
      event.register(MODE_KEY);
      event.register(SPIRIT_KEY);

      for (KeyMapping k : ABILITY_KEYS) {
         event.register(k);
      }
   }
}
