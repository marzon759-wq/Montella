package com.krolasyon.valdoria.registry;

import com.krolasyon.valdoria.ValdoriaMod;
import com.krolasyon.valdoria.block.GlowingPlantBlock;
import com.krolasyon.valdoria.block.VeilLeavesBlock;
import com.krolasyon.valdoria.block.VeilLogBlock;
import com.krolasyon.valdoria.block.VeilPlantBlock;
import com.krolasyon.valdoria.block.VeilTallPlantBlock;
import com.krolasyon.valdoria.block.VeilVineBlock;
import com.krolasyon.valdoria.block.VeilwoodSaplingBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ValdoriaMod.MODID);

    // --- Veilwood (the giant arching trees of the Glimmerveil Forest) ---
    public static final RegistryObject<Block> STRIPPED_VEILWOOD_LOG = reg("stripped_veilwood_log", () -> new VeilLogBlock(wood(MapColor.COLOR_PURPLE), null));
    public static final RegistryObject<Block> STRIPPED_VEILWOOD_WOOD = reg("stripped_veilwood_wood", () -> new VeilLogBlock(wood(MapColor.COLOR_PURPLE), null));
    public static final RegistryObject<Block> VEILWOOD_LOG = reg("veilwood_log", () -> new VeilLogBlock(wood(MapColor.COLOR_BLACK), STRIPPED_VEILWOOD_LOG));
    public static final RegistryObject<Block> VEILWOOD_WOOD = reg("veilwood_wood", () -> new VeilLogBlock(wood(MapColor.COLOR_BLACK), STRIPPED_VEILWOOD_WOOD));
    public static final RegistryObject<Block> VEILWOOD_PLANKS = reg("veilwood_planks", () -> new Block(wood(MapColor.COLOR_PURPLE)));
    public static final RegistryObject<Block> VEILWOOD_STAIRS = reg("veilwood_stairs", () -> new StairBlock(() -> VEILWOOD_PLANKS.get().defaultBlockState(), wood(MapColor.COLOR_PURPLE)));
    public static final RegistryObject<Block> VEILWOOD_SLAB = reg("veilwood_slab", () -> new SlabBlock(wood(MapColor.COLOR_PURPLE)));
    public static final RegistryObject<Block> VEILWOOD_LEAVES = reg("veilwood_leaves", () -> new VeilLeavesBlock(leaves(0)));
    public static final RegistryObject<Block> GLIMMERING_VEILWOOD_LEAVES = reg("glimmering_veilwood_leaves", () -> new VeilLeavesBlock(leaves(9)));
    public static final RegistryObject<Block> VEILWOOD_SAPLING = reg("veilwood_sapling", () -> new VeilwoodSaplingBlock(
            Properties.copy(Blocks.OAK_SAPLING).mapColor(MapColor.COLOR_PURPLE)));

    // --- Hanging vines and glowing growth ---
    public static final RegistryObject<Block> VEIL_VINES = reg("veil_vines", () -> new VeilVineBlock(Properties.of()
            .mapColor(MapColor.COLOR_BLACK).noCollission().instabreak().sound(SoundType.WEEPING_VINES)
            .noOcclusion().pushReaction(PushReaction.DESTROY)
            .lightLevel(s -> s.getValue(VeilVineBlock.GLOW) ? 10 : 0)));
    public static final RegistryObject<Block> GLIMMER_LICHEN = reg("glimmer_lichen", () -> new GlowLichenBlock(
            Properties.copy(Blocks.GLOW_LICHEN).mapColor(MapColor.COLOR_CYAN).lightLevel(GlowLichenBlock.emission(9))));

    // --- Forest floor ---
    public static final RegistryObject<Block> DUSKGRASS_BLOCK = reg("duskgrass_block", () -> new Block(
            Properties.copy(Blocks.DIRT).mapColor(MapColor.COLOR_CYAN).sound(SoundType.GRASS).strength(0.6F)));
    public static final RegistryObject<Block> VEIL_FERN = reg("veil_fern", () -> new VeilPlantBlock(plant(0)));
    public static final RegistryObject<Block> TALL_VEIL_FERN = reg("tall_veil_fern", () -> new VeilTallPlantBlock(plant(0)));
    public static final RegistryObject<Block> SHIMMERCAP = reg("shimmercap", () -> new GlowingPlantBlock(plant(7)));

    // --- Violetstone (the stone of the old, sunken ruins) ---
    public static final RegistryObject<Block> VIOLETSTONE = reg("violetstone", () -> new Block(stone()));
    public static final RegistryObject<Block> VIOLETSTONE_BRICKS = reg("violetstone_bricks", () -> new Block(stone()));
    public static final RegistryObject<Block> MOSSY_VIOLETSTONE_BRICKS = reg("mossy_violetstone_bricks", () -> new Block(stone()));
    public static final RegistryObject<Block> VIOLETSTONE_BRICK_STAIRS = reg("violetstone_brick_stairs", () -> new StairBlock(() -> VIOLETSTONE_BRICKS.get().defaultBlockState(), stone()));
    public static final RegistryObject<Block> VIOLETSTONE_BRICK_SLAB = reg("violetstone_brick_slab", () -> new SlabBlock(stone()));
    public static final RegistryObject<Block> VIOLETSTONE_BRICK_WALL = reg("violetstone_brick_wall", () -> new WallBlock(stone().forceSolidOn()));

    private static <T extends Block> RegistryObject<T> reg(String name, Supplier<T> sup) {
        return BLOCKS.register(name, sup);
    }

    private static Properties wood(MapColor color) {
        return Properties.copy(Blocks.OAK_LOG).mapColor(color).strength(2.0F).sound(SoundType.WOOD);
    }

    private static Properties leaves(int light) {
        return Properties.copy(Blocks.OAK_LEAVES).mapColor(MapColor.COLOR_PURPLE).lightLevel(s -> light);
    }

    private static Properties plant(int light) {
        return Properties.copy(Blocks.FERN).mapColor(MapColor.COLOR_CYAN).lightLevel(s -> light);
    }

    private static Properties stone() {
        return Properties.copy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_PURPLE);
    }
}
