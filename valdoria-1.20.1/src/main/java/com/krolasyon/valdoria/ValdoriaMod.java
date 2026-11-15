package com.krolasyon.valdoria;

import com.krolasyon.valdoria.registry.ModBlocks;
import com.krolasyon.valdoria.registry.ModFeatures;
import com.krolasyon.valdoria.registry.ModItems;
import com.krolasyon.valdoria.registry.ModParticles;
import com.krolasyon.valdoria.registry.ModTabs;
import com.krolasyon.valdoria.world.ValdoriaRegion;
import com.krolasyon.valdoria.world.ValdoriaSurfaceRules;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;

@Mod(ValdoriaMod.MODID)
public class ValdoriaMod {
    public static final String MODID = "valdoria";
    public static final Logger LOG = LogUtils.getLogger();

    public ValdoriaMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModFeatures.FEATURES.register(bus);
        ModParticles.PARTICLES.register(bus);
        ModTabs.TABS.register(bus);
        bus.addListener(this::commonSetup);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Regions.register(new ValdoriaRegion(id("overworld"), 4));
            SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, MODID, ValdoriaSurfaceRules.overworld());
        });
    }
}
