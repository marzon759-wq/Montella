package com.krolasyon.bloodsea.block;

import com.krolasyon.bloodsea.game.Endings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** The heart on the Ossuary Throne, the door of the lighthouse: touching it ends the voyage. */
public class EndingBlock extends Block {
    public final String ending;

    public EndingBlock(Properties p, String ending) {
        super(p);
        this.ending = ending;
    }

    /** called from the right click event (the signature of use() differs between versions) */
    public static boolean use(Level lvl, BlockPos pos, Player player) {
        if (!(lvl.getBlockState(pos).getBlock() instanceof EndingBlock e)) return false;
        if (player instanceof ServerPlayer sp) Endings.begin(sp, e.ending);
        return true;
    }
}
