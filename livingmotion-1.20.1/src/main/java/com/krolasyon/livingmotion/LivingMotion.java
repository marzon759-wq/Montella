package com.krolasyon.livingmotion;

import com.krolasyon.livingmotion.client.ClientInit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Living Motion: client-side remodel / reanimation of vanilla mobs with GeckoLib, plus procedural motion and
 * combat effects for every other living entity. Nothing runs on a dedicated server.
 */
@Mod(LivingMotion.MODID)
public class LivingMotion {
    public static final String MODID = "livingmotion";
    public static final Logger LOG = LoggerFactory.getLogger("LivingMotion");

    public LivingMotion() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, LMConfig.SPEC);
            ClientInit.init(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }
}
