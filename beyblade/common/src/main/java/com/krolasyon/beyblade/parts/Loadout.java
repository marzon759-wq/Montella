package com.krolasyon.beyblade.parts;

import com.krolasyon.beyblade.compat.V;
import com.krolasyon.beyblade.item.LibraItem;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class Loadout {
   public static final int MAX_LEVEL = 5;
   public static final float LEVEL_BONUS = 0.07F;
   private final BeyPart[] parts = new BeyPart[PartType.values().length];
   private final int[] levels = new int[PartType.values().length];

   @Nullable
   public BeyPart get(PartType t) {
      return this.parts[t.ordinal()];
   }

   public int level(PartType t) {
      return this.levels[t.ordinal()];
   }

   public void set(PartType t, @Nullable BeyPart p, int lvl) {
      this.parts[t.ordinal()] = p;
      this.levels[t.ordinal()] = p == null ? 0 : Math.max(0, Math.min(5, lvl));
   }

   public boolean complete() {
      for (BeyPart p : this.parts) {
         if (p == null) {
            return false;
         }
      }

      return true;
   }

   public static Loadout stock(String kind) {
      Loadout l = new Loadout();
      String[] ids = BeyPart.stock(kind);

      for (PartType t : PartType.values()) {
         l.set(t, BeyPart.get(ids[t.ordinal()]), 0);
      }

      return l;
   }

   public static Loadout of(ItemStack bey) {
      String kind = bey.getItem() instanceof LibraItem li ? li.kind() : "libra";
      Loadout l = stock(kind);
      CompoundTag t = V.tag(bey);
      if (t != null && t.contains("Parts")) {
         CompoundTag p = t.getCompound("Parts");

         for (PartType ty : PartType.values()) {
            if (p.contains(ty.key)) {
               String id = p.getString(ty.key);
               l.set(ty, id.isEmpty() ? null : BeyPart.get(id), p.getInt(ty.key + "Lv"));
            }
         }

         return l;
      } else {
         return l;
      }
   }

   public static void write(ItemStack bey, PartType ty, @Nullable BeyPart part, int lvl) {
      V.editTag(bey, t -> {
         CompoundTag p = t.getCompound("Parts");
         p.putString(ty.key, part == null ? "" : part.id());
         p.putInt(ty.key + "Lv", part == null ? 0 : lvl);
         t.put("Parts", p);
      });
   }

   public String encode() {
      StringBuilder sb = new StringBuilder();

      for (PartType t : new PartType[]{PartType.WHEEL, PartType.TRACK, PartType.TIP, PartType.RING, PartType.BOLT}) {
         if (sb.length() > 0) {
            sb.append(';');
         }

         BeyPart p = this.get(t);
         sb.append(p == null ? "" : p.id());
      }

      return sb.toString();
   }

   public static Loadout decode(String kind, String s) {
      Loadout l = stock(kind);
      if (s != null && !s.isEmpty()) {
         String[] a = s.split(";", -1);
         PartType[] order = new PartType[]{PartType.WHEEL, PartType.TRACK, PartType.TIP, PartType.RING, PartType.BOLT};

         for (int i = 0; i < order.length && i < a.length; i++) {
            if (!a[i].isEmpty() && BeyPart.get(a[i]) != null) {
               l.set(order[i], BeyPart.get(a[i]), 0);
            }
         }

         return l;
      } else {
         return l;
      }
   }

   private float sum(ToIntFunction<BeyPart> f) {
      float s = 0.0F;

      for (int i = 0; i < this.parts.length; i++) {
         if (this.parts[i] != null) {
            s += (float)f.applyAsInt(this.parts[i]) * (1.0F + 0.07F * (float)this.levels[i]);
         }
      }

      return s;
   }

   public float attack() {
      return this.sum(BeyPart::attack);
   }

   public float defense() {
      return this.sum(BeyPart::defense);
   }

   public float stamina() {
      return this.sum(BeyPart::stamina);
   }

   public float speed() {
      return this.sum(BeyPart::speed);
   }

   public float weight() {
      float w = 0.0F;

      for (int i = 0; i < this.parts.length; i++) {
         if (this.parts[i] != null) {
            w += this.parts[i].weight() * (1.0F + 0.02F * (float)this.levels[i]);
         }
      }

      return w;
   }

   public int height() {
      BeyPart t = this.get(PartType.TRACK);
      return t == null ? 145 : t.height();
   }

   public BeyPart.Tip tip() {
      BeyPart t = this.get(PartType.TIP);
      return t == null ? BeyPart.Tip.NONE : t.tip();
   }

   public BeyPart.Trait trackTrait() {
      BeyPart t = this.get(PartType.TRACK);
      return t == null ? BeyPart.Trait.NONE : t.trait();
   }

   public static float mul(float stat) {
      return Math.max(0.7F, Math.min(1.5F, 1.0F + (stat - 35.0F) / 140.0F));
   }

   public int leaning() {
      float a = this.attack() + this.speed() * 0.4F;
      float d = this.defense();
      float s = this.stamina();
      float max = Math.max(a, Math.max(d, s));
      float min = Math.min(a, Math.min(d, s));
      if (max - min < 10.0F) {
         return 3;
      } else {
         return max == a ? 0 : (max == d ? 1 : 2);
      }
   }

   public int partsRarityScore() {
      int r = 0;

      for (BeyPart p : this.parts) {
         if (p != null) {
            r += p.rarity();
         }
      }

      return r;
   }
}
