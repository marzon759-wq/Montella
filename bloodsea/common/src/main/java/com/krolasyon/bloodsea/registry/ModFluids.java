package com.krolasyon.bloodsea.registry;

import com.krolasyon.bloodsea.BloodSea;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.SoundAction;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Blood: thick, slow, warm. Boats float on it; you can swim in it, and drown in it. */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, BloodSea.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, BloodSea.MODID);

    public static final RegistryObject<FluidType> BLOOD_TYPE = FLUID_TYPES.register("blood", () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.bloodsea.blood")
            .density(1600).viscosity(2600).temperature(310)
            .motionScale(0.009).canSwim(true).canDrown(true).canPushEntity(true).supportsBoating(true).canExtinguish(true)
            .fallDistanceModifier(0f)
            .sound(SoundAction.get("bucket_fill"), SoundEvents.BUCKET_FILL)
            .sound(SoundAction.get("bucket_empty"), SoundEvents.BUCKET_EMPTY)) {
        @Override
        public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions> c) {
            c.accept(com.krolasyon.bloodsea.client.BloodFluidClient.INSTANCE);
        }
    });

    public static final RegistryObject<ForgeFlowingFluid.Source> BLOOD = FLUIDS.register("blood", () -> new ForgeFlowingFluid.Source(props()));
    public static final RegistryObject<ForgeFlowingFluid.Flowing> BLOOD_FLOWING = FLUIDS.register("flowing_blood", () -> new ForgeFlowingFluid.Flowing(props()));

    private static ForgeFlowingFluid.Properties props() {
        return new ForgeFlowingFluid.Properties(BLOOD_TYPE, BLOOD, BLOOD_FLOWING)
                .block(() -> (net.minecraft.world.level.block.LiquidBlock) ModBlocks.BLOOD.get()).bucket(ModItems.BLOOD_BUCKET)
                .slopeFindDistance(3).levelDecreasePerBlock(2).tickRate(20);
    }

    private ModFluids() {}
}
