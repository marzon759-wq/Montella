package com.krolasyon.beyblade.parts;

import java.util.Locale;

public enum PartType {
   BOLT("Bolt"),
   RING("Ring"),
   WHEEL("Wheel"),
   TRACK("Track"),
   TIP("Tip");

   public final String key;

   private PartType(String key) {
      this.key = key;
   }

   public String nameKey() {
      return "parttype.librabeyblade." + this.name().toLowerCase(Locale.ROOT);
   }
}
