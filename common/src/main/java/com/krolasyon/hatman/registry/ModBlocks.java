package com.krolasyon.hatman.registry;

import com.krolasyon.hatman.HatmanMod;
import com.krolasyon.hatman.block.FacingBlock;
import com.krolasyon.hatman.block.ModeBlock;
import com.krolasyon.hatman.block.OpenableBlock;
import com.krolasyon.hatman.block.TallBlock;
import com.krolasyon.hatman.block.VariantBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, HatmanMod.MODID);
    public static final List<RegistryObject<Block>> ALL = new ArrayList<>();

    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.of().strength(2.0f, 6.0f).sound(SoundType.WOOD);
    }

    private static BlockBehaviour.Properties prop() {
        return BlockBehaviour.Properties.of().strength(1.5f, 6.0f).sound(SoundType.WOOD).noOcclusion();
    }

    private static final double[] FULL = {0, 0, 0, 16, 16, 16};

    private static RegistryObject<Block> reg(String name, Supplier<Block> s) {
        RegistryObject<Block> r = BLOCKS.register(name, s);
        ALL.add(r);
        return r;
    }

    private static RegistryObject<Block> cube(String name, SoundType st) {
        return reg(name, () -> new Block(BlockBehaviour.Properties.of().strength(2.0f, 6.0f).sound(st)));
    }

    // ---- structure
    public static final RegistryObject<Block> WALLPAPER_GREEN = cube("wallpaper_green", SoundType.WOOD);
    public static final RegistryObject<Block> WALLPAPER_STAINED = cube("wallpaper_stained", SoundType.WOOD);
    public static final RegistryObject<Block> WALLPAPER_RED = cube("wallpaper_red", SoundType.WOOD);
    public static final RegistryObject<Block> WALLPAPER_BLUE = cube("wallpaper_blue", SoundType.WOOD);
    public static final RegistryObject<Block> WAINSCOT = cube("wainscot", SoundType.WOOD);
    public static final RegistryObject<Block> PLASTER = cube("plaster", SoundType.STONE);
    public static final RegistryObject<Block> ROTTEN_PLANKS = cube("rotten_planks", SoundType.WOOD);
    public static final RegistryObject<Block> BOARDED_WINDOW = cube("boarded_window", SoundType.WOOD);
    public static final RegistryObject<Block> CELLAR_BRICK = cube("cellar_brick", SoundType.STONE);
    public static final RegistryObject<Block> TILE_FLOOR = cube("tile_floor", SoundType.STONE);

    // ---- furniture
    public static final RegistryObject<Block> ARMCHAIR = reg("armchair", () -> new FacingBlock(prop(), new double[]{1, 0, 1, 15, 9, 15}, true));
    public static final RegistryObject<Block> DINING_CHAIR = reg("dining_chair", () -> new FacingBlock(prop(), new double[]{3, 0, 3, 13, 8, 13}, true));
    public static final RegistryObject<Block> CAN_SHELF = reg("can_shelf", () -> new FacingBlock(prop(), new double[]{0, 0, 8, 16, 16, 16}, true));
    public static final RegistryObject<Block> MIRROR = reg("mirror", () -> new FacingBlock(prop().sound(SoundType.GLASS), new double[]{1, 0, 14, 15, 16, 16}, false));
    public static final RegistryObject<Block> CAMCORDER = reg("camcorder", () -> new FacingBlock(prop().sound(SoundType.METAL), new double[]{4, 0, 3, 12, 6, 13}, false));
    public static final RegistryObject<Block> HAT_BOX = reg("hat_box", () -> new FacingBlock(prop(), new double[]{2, 0, 2, 14, 9, 14}, true));
    public static final RegistryObject<Block> HAT_STAND = reg("hat_stand", () -> new VariantBlock(prop(), new double[]{3, 0, 3, 13, 14, 13}, true));
    public static final RegistryObject<Block> RECORDER = reg("recorder", () -> new FacingBlock(prop().sound(SoundType.METAL), new double[]{3, 0, 4, 13, 4, 12}, false));
    public static final RegistryObject<Block> NAME_PLATE = reg("name_plate", () -> new FacingBlock(prop().sound(SoundType.METAL), new double[]{3, 4, 15, 13, 12, 16}, false));
    public static final RegistryObject<Block> STREAMER = reg("streamer", () -> new FacingBlock(prop().sound(SoundType.WOOL).noCollission(), new double[]{0, 6, 6, 16, 16, 10}, false));
    public static final RegistryObject<Block> PIANO = reg("piano", () -> new FacingBlock(prop(), new double[]{0, 0, 4, 16, 16, 16}, true));
    public static final RegistryObject<Block> BATHTUB = reg("bathtub", () -> new FacingBlock(prop().sound(SoundType.STONE), new double[]{0, 0, 0, 16, 9, 16}, true));
    public static final RegistryObject<Block> DESK = reg("desk", () -> new OpenableBlock(prop(), FULL, true));
    public static final RegistryObject<Block> CABINET = reg("cabinet", () -> new OpenableBlock(prop(), new double[]{1, 0, 3, 15, 13, 16}, true));
    public static final RegistryObject<Block> SAFE = reg("safe", () -> new OpenableBlock(prop().sound(SoundType.METAL), new double[]{1, 0, 3, 15, 16, 16}, true));
    public static final RegistryObject<Block> TRUNK = reg("trunk", () -> new OpenableBlock(prop(), new double[]{1, 0, 2, 15, 10, 14}, true));
    public static final RegistryObject<Block> WARDROBE = reg("wardrobe", () -> new TallBlock(prop(), FULL, true));
    public static final RegistryObject<Block> CLOCK = reg("clock", () -> new TallBlock(prop(), new double[]{3, 0, 6, 13, 16, 16}, true));
    public static final RegistryObject<Block> MANNEQUIN = reg("mannequin", () -> new TallBlock(prop().sound(SoundType.WOOL), new double[]{4, 0, 5, 12, 16, 11}, true));
    public static final RegistryObject<Block> WALL_MASK = reg("wall_mask", () -> new VariantBlock(prop().sound(SoundType.STONE), new double[]{4, 3, 14, 12, 13, 16}, false));
    public static final RegistryObject<Block> PAPER = reg("paper", () -> new VariantBlock(prop().sound(SoundType.WOOL).noCollission(), new double[]{2, 0, 2, 14, 2, 14}, false));

    // ---- machines / lights
    public static final RegistryObject<Block> CRT_TV = reg("crt_tv", () -> new ModeBlock(prop().lightLevel(s -> s.getValue(ModeBlock.MODE) == 0 ? 0 : 8), new double[]{1, 0, 2, 15, 13, 15}, true));
    public static final RegistryObject<Block> PHONE = reg("phone", () -> new ModeBlock(prop().sound(SoundType.METAL), new double[]{4, 0, 4, 12, 5, 12}, false));
    public static final RegistryObject<Block> GENERATOR = reg("generator", () -> new ModeBlock(prop().sound(SoundType.METAL), FULL, true));
    public static final RegistryObject<Block> MUSIC_BOX = reg("music_box", () -> new ModeBlock(prop(), new double[]{4, 0, 4, 12, 6, 12}, false));
    public static final RegistryObject<Block> WALL_LAMP = reg("wall_lamp", () -> new ModeBlock(prop().sound(SoundType.GLASS).noCollission().lightLevel(s -> s.getValue(ModeBlock.MODE) == 1 ? 13 : s.getValue(ModeBlock.MODE) == 2 ? 5 : 0), new double[]{5, 3, 10, 11, 13, 16}, false));
    public static final RegistryObject<Block> CEILING_LAMP = reg("ceiling_lamp", () -> new ModeBlock(prop().sound(SoundType.GLASS).noCollission().lightLevel(s -> s.getValue(ModeBlock.MODE) == 1 ? 14 : s.getValue(ModeBlock.MODE) == 2 ? 5 : 0), new double[]{4, 6, 4, 12, 16, 12}, false));

    private ModBlocks() {}
}
