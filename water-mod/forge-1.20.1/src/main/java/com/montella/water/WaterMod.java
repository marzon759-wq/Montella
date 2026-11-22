package com.montella.water;

import com.montella.water.client.ClientSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Montella Water Physics: waterfalls, currents, splashes, wakes and ripples. */
@Mod(WaterMod.ID)
public class WaterMod {
    public static final String ID = "montella_water";
    public static final Logger LOG = LoggerFactory.getLogger("MontellaWater");

    public WaterMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModParticles.REG.register(bus);
        ModSounds.REG.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        MinecraftForge.EVENT_BUS.register(Currents.class);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSetup.init(bus));
    }
}
