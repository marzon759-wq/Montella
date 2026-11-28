package com.krolasyon.beyblade.workbench;

import com.krolasyon.beyblade.item.LibraItem;
import com.krolasyon.beyblade.item.PartItem;
import com.krolasyon.beyblade.parts.BeyPart;
import com.krolasyon.beyblade.parts.Loadout;
import com.krolasyon.beyblade.parts.PartType;
import com.krolasyon.beyblade.registry.ModBlocks;
import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModMenus;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public class WorkbenchMenu extends AbstractContainerMenu {
   public static final int BEY = 0;
   public static final int PART0 = 1;
   public static final int MATERIAL = 6;
   public static final int SIZE = 7;
   public static final int[][] POS = new int[][]{{62, 76}, {62, 30}, {22, 54}, {102, 54}, {22, 100}, {102, 100}, {62, 128}};
   public static final int INV_X = 48;
   public static final int INV_Y = 166;
   public static final Item[] UPGRADE_ITEM = new Item[]{Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.EMERALD, Items.NETHERITE_INGOT};
   public static final int[] UPGRADE_COUNT = new int[]{4, 4, 2, 2, 1};
   private final SimpleContainer bench = new SimpleContainer(7);
   private final ContainerLevelAccess access;
   private boolean syncing;

   public WorkbenchMenu(int id, Inventory inv) {
      this(id, inv, ContainerLevelAccess.NULL);
   }

   public WorkbenchMenu(int id, Inventory inv, ContainerLevelAccess access) {
      super((MenuType)ModMenus.WORKBENCH.get(), id);
      this.access = access;
      this.addSlot(new WorkbenchMenu.BeySlot(this.bench, 0, POS[0][0], POS[0][1]));

      for (PartType t : PartType.values()) {
         this.addSlot(new WorkbenchMenu.PartSlot(this.bench, 1 + t.ordinal(), POS[1 + t.ordinal()][0], POS[1 + t.ordinal()][1], t));
      }

      this.addSlot(new Slot(this.bench, 6, POS[6][0], POS[6][1]) {
         public boolean mayPlace(ItemStack s) {
            for (Item i : WorkbenchMenu.UPGRADE_ITEM) {
               if (s.is(i)) {
                  return true;
               }
            }

            return false;
         }
      });

      for (int r = 0; r < 3; r++) {
         for (int c = 0; c < 9; c++) {
            this.addSlot(new Slot(inv, 9 + r * 9 + c, 48 + c * 18, 166 + r * 18));
         }
      }

      for (int c = 0; c < 9; c++) {
         this.addSlot(new Slot(inv, c, 48 + c * 18, 224));
      }
   }

   public ItemStack bey() {
      return this.bench.getItem(0);
   }

   public ItemStack partStack(PartType t) {
      return this.bench.getItem(1 + t.ordinal());
   }

   public ItemStack material() {
      return this.bench.getItem(6);
   }

   public Loadout loadout() {
      return this.bey().isEmpty() ? null : Loadout.of(this.bey());
   }

   private void unpack() {
      this.syncing = true;
      ItemStack b = this.bey();
      Loadout l = b.isEmpty() ? null : Loadout.of(b);

      for (PartType t : PartType.values()) {
         BeyPart p = l == null ? null : l.get(t);
         ItemStack s = ItemStack.EMPTY;
         if (p != null) {
            s = new ItemStack(ModItems.part(p.id()));
            if (l.level(t) > 0) {
               PartItem.setLevel(s, l.level(t));
            }
         }

         this.bench.setItem(1 + t.ordinal(), s);
      }

      this.syncing = false;
   }

   private void pack(PartType t) {
      if (!this.syncing && !this.bey().isEmpty()) {
         ItemStack s = this.partStack(t);
         BeyPart p = s.getItem() instanceof PartItem pi ? pi.part() : null;
         Loadout.write(this.bey(), t, p, p == null ? 0 : PartItem.level(s));
      }
   }

   public boolean clickMenuButton(Player player, int id) {
      if (id >= 0 && id < PartType.values().length) {
         PartType t = PartType.values()[id];
         ItemStack s = this.partStack(t);
         if (s.getItem() instanceof PartItem && !this.bey().isEmpty()) {
            int lvl = PartItem.level(s);
            if (lvl >= 5) {
               return false;
            } else {
               ItemStack m = this.material();
               if (m.is(UPGRADE_ITEM[lvl]) && m.getCount() >= UPGRADE_COUNT[lvl]) {
                  m.shrink(UPGRADE_COUNT[lvl]);
                  this.bench.setItem(6, m.isEmpty() ? ItemStack.EMPTY : m);
                  ItemStack up = s.copy();
                  PartItem.setLevel(up, lvl + 1);
                  this.bench.setItem(1 + t.ordinal(), up);
                  this.pack(t);
                  this.access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7F, 1.3F + 0.1F * (float)lvl));
                  this.broadcastChanges();
                  return true;
               } else {
                  return false;
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean canUpgrade(PartType t) {
      ItemStack s = this.partStack(t);
      if (!(s.getItem() instanceof PartItem)) {
         return false;
      } else {
         int lvl = PartItem.level(s);
         return lvl < 5 && this.material().is(UPGRADE_ITEM[lvl]) && this.material().getCount() >= UPGRADE_COUNT[lvl];
      }
   }

   public ItemStack quickMoveStack(Player player, int index) {
      Slot slot = (Slot)this.slots.get(index);
      if (!slot.hasItem()) {
         return ItemStack.EMPTY;
      } else {
         ItemStack s = slot.getItem();
         ItemStack copy = s.copy();
         int invStart = 7;
         int invEnd = 43;
         if (index < 7) {
            if (!this.moveItemStackTo(s, invStart, invEnd, true)) {
               return ItemStack.EMPTY;
            }
         } else if (s.getItem() instanceof LibraItem && this.bey().isEmpty()) {
            if (!this.moveItemStackTo(s, 0, 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (s.getItem() instanceof PartItem pi) {
            int target = 1 + pi.part().type().ordinal();
            if (this.bey().isEmpty() || ((Slot)this.slots.get(target)).hasItem() || !this.moveItemStackTo(s, target, target + 1, false)) {
               return ItemStack.EMPTY;
            }
         } else {
            if (!((Slot)this.slots.get(6)).mayPlace(s)) {
               return ItemStack.EMPTY;
            }

            if (!this.moveItemStackTo(s, 6, 7, false)) {
               return ItemStack.EMPTY;
            }
         }

         if (s.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         return copy;
      }
   }

   public boolean stillValid(Player player) {
      return stillValid(this.access, player, (Block)ModBlocks.WORKBENCH.get());
   }

   public void removed(Player player) {
      super.removed(player);
      this.syncing = true;

      for (int i = 1; i < 6; i++) {
         this.bench.setItem(i, ItemStack.EMPTY);
      }

      this.syncing = false;
      this.access.execute((level, pos) -> this.clearContainer(player, this.bench));
   }

   class BeySlot extends Slot {
      BeySlot(Container c, int i, int x, int y) {
         super(c, i, x, y);
      }

      public boolean mayPlace(ItemStack s) {
         return s.getItem() instanceof LibraItem;
      }

      public int getMaxStackSize() {
         return 1;
      }

      public void setChanged() {
         super.setChanged();
         WorkbenchMenu.this.unpack();
      }
   }

   class PartSlot extends Slot {
      final PartType type;

      PartSlot(Container c, int i, int x, int y, PartType type) {
         super(c, i, x, y);
         this.type = type;
      }

      public boolean mayPlace(ItemStack s) {
         return !WorkbenchMenu.this.bey().isEmpty() && s.getItem() instanceof PartItem pi && pi.part().type() == this.type;
      }

      public int getMaxStackSize() {
         return 1;
      }

      public void setChanged() {
         super.setChanged();
         WorkbenchMenu.this.pack(this.type);
      }
   }
}
