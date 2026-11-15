package com.krolasyon.valdoria.entity;

import com.krolasyon.valdoria.registry.ModBlocks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Shared rules of the Glimmerveil food web: who fears what, what counts as light. */
public final class Ecology {
    private Ecology() {}

    /** Torches, lanterns and anything glowing: gloomlynxes will not approach a player carrying one. */
    public static boolean isLight(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH) || stack.is(Items.LANTERN) || stack.is(Items.SOUL_LANTERN)
                || stack.is(Items.GLOWSTONE) || stack.is(Items.SHROOMLIGHT) || stack.is(Items.SEA_LANTERN) || stack.is(Items.JACK_O_LANTERN)
                || stack.is(Items.GLOW_BERRIES) || stack.is(Items.GLOW_INK_SAC)) return true;
        return stack.getItem() instanceof BlockItem bi && bi.getBlock().defaultBlockState().getLightEmission() >= 9;
    }

    public static boolean holdsLight(LivingEntity e) {
        return isLight(e.getMainHandItem()) || isLight(e.getOffhandItem());
    }

    public static boolean isShimmercap(ItemStack stack) {
        return stack.is(ModBlocks.SHIMMERCAP.get().asItem());
    }

    /** Predators every prey animal watches out for. */
    public static boolean isPredator(LivingEntity e) {
        if (e instanceof Gloomlynx lynx) return !lynx.isNapping();
        if (e instanceof Vinestalker stalker) return !stalker.isHanging();
        return false;
    }

    /** Development only: ecosystem events are logged when the autotest runs. */
    public static void log(String fmt, Object... args) {
        if (System.getProperty("valdoria.autotest") != null) com.krolasyon.valdoria.ValdoriaMod.LOG.info("[ECO] " + fmt, args);
    }

    public static boolean isHuntablePlayer(LivingEntity e) {
        return e instanceof Player p && !p.isCreative() && !p.isSpectator();
    }
}
