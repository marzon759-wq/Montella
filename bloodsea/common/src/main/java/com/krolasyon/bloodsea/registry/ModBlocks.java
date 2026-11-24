package com.krolasyon.bloodsea.registry;

import com.krolasyon.bloodsea.BloodSea;
import com.krolasyon.bloodsea.block.EndingBlock;
import com.krolasyon.bloodsea.block.MawCoreBlock;
import com.krolasyon.bloodsea.block.MirageBlock;
import com.krolasyon.bloodsea.block.PhantomWallBlock;
import com.krolasyon.bloodsea.compat.Compat;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BloodSea.MODID);
    /** blocks that get a block item */
    public static final List<RegistryObject<Block>> WITH_ITEM = new ArrayList<>();

    private static BlockBehaviour.Properties bone() { return BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(2.0f).sound(SoundType.BONE_BLOCK); }
    private static BlockBehaviour.Properties meat() { return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.8f).sound(SoundType.SLIME_BLOCK); }
    private static BlockBehaviour.Properties stone() { return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.0f, 9f).sound(SoundType.DEEPSLATE); }

    private static RegistryObject<Block> reg(String name, Supplier<Block> s) {
        RegistryObject<Block> r = BLOCKS.register(name, s);
        WITH_ITEM.add(r);
        return r;
    }

    public static final RegistryObject<Block> BLOOD = BLOCKS.register("blood", () -> Compat.liquidBlock(ModFluids.BLOOD,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).replaceable().noCollission().strength(100f).pushReaction(PushReaction.DESTROY).noLootTable().liquid()));

    // ---- the Bone Reach
    public static final RegistryObject<Block> BONE_MUD = reg("bone_mud", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLACK).strength(0.6f).sound(SoundType.MUD)));
    // ---- the sea floor
    public static final RegistryObject<Block> CLOT = reg("clot", () -> new Block(meat().strength(1.0f)));
    public static final RegistryObject<Block> FLESH = reg("flesh", () -> new Block(meat()));
    public static final RegistryObject<Block> EDGE_STONE = reg("edge_stone", () -> new Block(stone()));
    // ---- Leviathan's Deep
    public static final RegistryObject<Block> MAW_CORE = reg("maw_core", () -> new MawCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
            .strength(-1f, 3600000f).noCollission().noOcclusion().lightLevel(s -> 3)));
    // ---- the Mirage
    public static final RegistryObject<Block> MIRAGE_SAND = reg("mirage_sand", () -> new MirageBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.5f).sound(SoundType.SAND).noCollission()));
    public static final RegistryObject<Block> MIRAGE_GRASS = reg("mirage_grass", () -> new MirageBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).strength(0.5f).sound(SoundType.GRASS).noCollission()));
    public static final RegistryObject<Block> MIRAGE_LOG = reg("mirage_log", () -> new MirageBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.5f).sound(SoundType.WOOD).noCollission()));
    public static final RegistryObject<Block> MIRAGE_LEAVES = reg("mirage_leaves", () -> new MirageBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.2f).sound(SoundType.GRASS).noCollission().noOcclusion()));
    public static final RegistryObject<Block> PHANTOM_WALL = reg("phantom_wall", () -> new PhantomWallBlock(BlockBehaviour.Properties.of().strength(-1f, 3600000f).noOcclusion().noLootTable()));
    public static final RegistryObject<Block> LIGHTHOUSE_WALL = reg("lighthouse_wall", () -> new Block(stone().mapColor(MapColor.SNOW).sound(SoundType.STONE)));
    public static final RegistryObject<Block> LIGHTHOUSE_LAMP = reg("lighthouse_lamp", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(1f).sound(SoundType.GLASS).lightLevel(s -> 15)));
    public static final RegistryObject<Block> HOME_DOOR = reg("home_door", () -> new EndingBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(-1f, 3600000f).sound(SoundType.WOOD), "mirage"));

    private ModBlocks() {}
}
