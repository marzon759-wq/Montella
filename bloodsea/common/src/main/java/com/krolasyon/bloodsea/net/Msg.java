package com.krolasyon.bloodsea.net;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/** The single packet type used in both directions: a type string plus an NBT payload. */
public class Msg {
    public final String type;
    public final CompoundTag tag;

    public Msg(String type, CompoundTag tag) {
        this.type = type;
        this.tag = tag == null ? new CompoundTag() : tag;
    }

    public static Msg of(String type) { return new Msg(type, new CompoundTag()); }

    public Msg s(String k, String v) { tag.putString(k, v); return this; }
    public Msg i(String k, int v) { tag.putInt(k, v); return this; }
    public Msg f(String k, float v) { tag.putFloat(k, v); return this; }
    public Msg b(String k, boolean v) { tag.putBoolean(k, v); return this; }
    public Msg d(String k, double v) { tag.putDouble(k, v); return this; }

    public String s(String k) { return tag.getString(k); }
    public int i(String k) { return tag.getInt(k); }
    public float f(String k) { return tag.getFloat(k); }
    public boolean b(String k) { return tag.getBoolean(k); }
    public double d(String k) { return tag.getDouble(k); }

    public static void write(Msg m, FriendlyByteBuf buf) {
        buf.writeUtf(m.type);
        buf.writeNbt(m.tag);
    }

    public static Msg read(FriendlyByteBuf buf) {
        String t = buf.readUtf();
        CompoundTag tag = buf.readNbt();
        return new Msg(t, tag);
    }
}
