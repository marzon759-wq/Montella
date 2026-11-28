package com.krolasyon.beyblade.registry;

import com.krolasyon.beyblade.block.StadiumBarrierBlock;
import com.krolasyon.beyblade.workbench.WorkbenchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
   public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "librabeyblade");
   public static final RegistryObject<Block> STADIUM_BARRIER = BLOCKS.register(
      "stadium_barrier",
      () -> new StadiumBarrierBlock(
            Properties.of().strength(-1.0F, 3600000.0F).noLootTable().noOcclusion().isValidSpawn((s, l, p, t) -> false).pushReaction(PushReaction.BLOCK)
         )
   );
   public static final RegistryObject<Block> WORKBENCH = BLOCKS.register(
      "bey_workbench", () -> new WorkbenchBlock(Properties.of().strength(2.5F).sound(SoundType.METAL))
   );

   private ModBlocks() {
   }
}
