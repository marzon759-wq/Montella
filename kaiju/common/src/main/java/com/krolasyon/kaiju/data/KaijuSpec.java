package com.krolasyon.kaiju.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.krolasyon.kaiju.KaijuMod;
import net.minecraft.world.phys.Vec3;

import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Everything a kaiju species is, loaded from data/kaiju/kaiju/&lt;id&gt;.json (written by tools/gen_anims.py):
 * size and mass, collision nodes, burn regions, severable parts, the clip list, the attack catalogue, and for every
 * clip a pose track (each node's model-space position at every game tick, from the same animation the client plays).
 * A new kaiju is a new json + model + textures; the entity code is shared.
 */
public final class KaijuSpec {
    private static final Map<String, KaijuSpec> CACHE = new HashMap<>();

    public final String id;
    public final float scale, mass, maxHp, walkPx, fastPx, turnDeg, heightPx;
    public final List<Node> nodes = new ArrayList<>();
    public final Map<String, Integer> nodeIndex = new HashMap<>();
    public final List<String> clipNames = new ArrayList<>();
    public final Map<String, Integer> clipIndex = new HashMap<>();
    public final Map<String, Clip> clips = new HashMap<>();
    public final List<Attack> attacks = new ArrayList<>();
    public final Map<String, Attack> attackById = new LinkedHashMap<>();
    public final Map<String, String> severBones = new LinkedHashMap<>();
    public final List<String> severGroups = new ArrayList<>();
    public final Map<String, List<String>> regions = new LinkedHashMap<>();
    public final List<String> regionNames = new ArrayList<>();
    public final List<Plate> plates = new ArrayList<>();

    public record Node(String name, String bone, float r, int region, int sever) {}

    public record Plate(String bone, float s, float travel) {}

    public static final class Clip {
        public final String name;
        public final int index, ticks;
        public final float len;
        public final boolean loop;
        public final float[] impacts;
        /** per-tick body turn (degrees, model +y = to the left) for clips that turn the whole kaiju; null otherwise */
        public float[] yaw;
        /** ticks * nodes * 3, model px */
        public final float[] pos;

        Clip(String name, int index, float len, boolean loop, float[] impacts, int ticks, float[] pos) {
            this.name = name; this.index = index; this.len = len; this.loop = loop; this.impacts = impacts; this.ticks = ticks; this.pos = pos;
        }

        public int tickIndex(int t) {
            if (ticks <= 0) return 0;
            return loop ? Math.floorMod(t, ticks) : Math.max(0, Math.min(ticks - 1, t));
        }
    }

    public record Hit(float t0, float t1, int[] nodes, String kind, float power) {}

    public record Event(float t, String type, JsonObject args) {}

    public record Move(float t0, float t1, float side, float fwd) {}

    public record Phase(String clip, float secs) {}

    public static final class Attack {
        public String id, clip, cat, arc;
        public float len, minRange, maxRange, cooldown, weight;
        public List<Hit> hits = new ArrayList<>();
        public List<Event> events = new ArrayList<>();
        public List<Move> moves = new ArrayList<>();
        public Set<String> need = new HashSet<>(), tags = new HashSet<>();
        public List<Phase> phases;
        public JsonObject beam;

        public boolean has(String tag) { return tags.contains(tag); }

        public boolean isBreath() { return beam != null; }
    }

    private KaijuSpec(String id, JsonObject j) {
        this.id = id;
        scale = j.get("scale").getAsFloat();
        mass = j.get("mass").getAsFloat();
        maxHp = j.get("max_hp").getAsFloat();
        walkPx = j.get("walk_speed_px").getAsFloat();
        fastPx = j.get("fast_speed_px").getAsFloat();
        turnDeg = j.get("turn_deg").getAsFloat();
        heightPx = j.get("model_height_px").getAsFloat();
        for (Map.Entry<String, JsonElement> e : j.getAsJsonObject("sever_bones").entrySet()) {
            severBones.put(e.getKey(), e.getValue().getAsString());
            severGroups.add(e.getKey());
        }
        for (Map.Entry<String, JsonElement> e : j.getAsJsonObject("regions").entrySet()) {
            List<String> l = new ArrayList<>();
            e.getValue().getAsJsonArray().forEach(x -> l.add(x.getAsString()));
            regions.put(e.getKey(), l);
            regionNames.add(e.getKey());
        }
        for (JsonElement e : j.getAsJsonArray("nodes")) {
            JsonObject o = e.getAsJsonObject();
            String sv = o.get("sever").getAsString();
            Node n = new Node(o.get("name").getAsString(), o.get("bone").getAsString(), o.get("r").getAsFloat(),
                    regionNames.indexOf(o.get("region").getAsString()), sv.isEmpty() ? -1 : severGroups.indexOf(sv));
            nodeIndex.put(n.name, nodes.size());
            nodes.add(n);
        }
        for (JsonElement e : j.getAsJsonArray("plates")) {
            JsonObject o = e.getAsJsonObject();
            plates.add(new Plate(o.get("bone").getAsString(), o.get("s").getAsFloat(), o.get("travel").getAsFloat()));
        }
        JsonObject clipInfo = j.getAsJsonObject("clips");
        JsonObject tracks = j.getAsJsonObject("tracks");
        for (Map.Entry<String, JsonElement> e : clipInfo.entrySet()) {
            String name = e.getKey();
            JsonObject ci = e.getValue().getAsJsonObject();
            JsonObject tr = tracks.getAsJsonObject(name);
            int ticks = tr.get("ticks").getAsInt();
            byte[] raw = Base64.getDecoder().decode(tr.get("data").getAsString());
            ShortBuffer sb = new ShortBuffer(raw);
            float[] pos = new float[ticks * nodes.size() * 3];
            for (int i = 0; i < pos.length; i++) pos[i] = sb.next() / 10f;
            JsonArray imp = ci.has("impacts") ? ci.getAsJsonArray("impacts") : new JsonArray();
            float[] impacts = new float[imp.size()];
            for (int i = 0; i < impacts.length; i++) impacts[i] = imp.get(i).getAsFloat();
            Clip c = new Clip(name, clipNames.size(), ci.get("len").getAsFloat(), ci.get("loop").getAsBoolean(), impacts, ticks, pos);
            if (ci.has("yaw")) {
                JsonArray ya = ci.getAsJsonArray("yaw");
                c.yaw = new float[ya.size()];
                for (int i = 0; i < c.yaw.length; i++) c.yaw[i] = ya.get(i).getAsFloat();
            }
            clipIndex.put(name, c.index);
            clipNames.add(name);
            clips.put(name, c);
        }
        for (JsonElement e : j.getAsJsonArray("attacks")) {
            JsonObject o = e.getAsJsonObject();
            Attack a = new Attack();
            a.id = o.get("id").getAsString();
            a.clip = o.get("clip").getAsString();
            a.cat = o.get("cat").getAsString();
            a.arc = o.get("arc").getAsString();
            a.len = o.get("len").getAsFloat();
            JsonArray rng = o.getAsJsonArray("rng");
            a.minRange = rng.get(0).getAsFloat();
            a.maxRange = rng.get(1).getAsFloat();
            a.cooldown = o.get("cd").getAsFloat();
            a.weight = o.has("weight") ? o.get("weight").getAsFloat() : 1f;
            for (JsonElement h : o.getAsJsonArray("hits")) {
                JsonArray ha = h.getAsJsonArray();
                JsonArray ns = ha.get(2).getAsJsonArray();
                List<Integer> idx = new ArrayList<>();
                ns.forEach(x -> { Integer i = nodeIndex.get(x.getAsString()); if (i != null) idx.add(i); });
                a.hits.add(new Hit(ha.get(0).getAsFloat(), ha.get(1).getAsFloat(), idx.stream().mapToInt(Integer::intValue).toArray(), ha.get(3).getAsString(), ha.get(4).getAsFloat()));
            }
            for (JsonElement ev : o.getAsJsonArray("events")) {
                JsonArray ea = ev.getAsJsonArray();
                a.events.add(new Event(ea.get(0).getAsFloat(), ea.get(1).getAsString(), ea.size() > 2 ? ea.get(2).getAsJsonObject() : new JsonObject()));
            }
            for (JsonElement mv : o.getAsJsonArray("move")) {
                JsonArray ma = mv.getAsJsonArray();
                a.moves.add(new Move(ma.get(0).getAsFloat(), ma.get(1).getAsFloat(), ma.get(2).getAsFloat(), ma.get(3).getAsFloat()));
            }
            o.getAsJsonArray("need").forEach(x -> a.need.add(x.getAsString()));
            o.getAsJsonArray("tags").forEach(x -> a.tags.add(x.getAsString()));
            if (o.has("phases") && o.get("phases").isJsonArray()) {
                a.phases = new ArrayList<>();
                for (JsonElement p : o.getAsJsonArray("phases")) {
                    JsonArray pa = p.getAsJsonArray();
                    a.phases.add(new Phase(pa.get(0).getAsString(), pa.get(1).getAsFloat()));
                }
            }
            if (o.has("beam") && o.get("beam").isJsonObject()) a.beam = o.getAsJsonObject("beam");
            attacks.add(a);
            attackById.put(a.id, a);
        }
    }

    private static final class ShortBuffer {
        private final ByteBuffer b;

        ShortBuffer(byte[] raw) { b = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN); }

        short next() { return b.getShort(); }
    }

    public static KaijuSpec get(String id) {
        return CACHE.computeIfAbsent(id, k -> {
            String path = "/data/kaiju/kaiju/" + k + ".json";
            try (var in = KaijuMod.class.getResourceAsStream(path)) {
                if (in == null) throw new IllegalStateException("missing " + path);
                JsonObject j = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                KaijuSpec s = new KaijuSpec(k, j);
                KaijuMod.LOG.info("Kaiju spec {}: {} nodes, {} clips, {} attacks", k, s.nodes.size(), s.clips.size(), s.attacks.size());
                return s;
            } catch (Exception e) {
                throw new RuntimeException("Failed to load kaiju spec " + k, e);
            }
        });
    }

    public Clip clip(String name) { return clips.get(name); }

    public int node(String name) { Integer i = nodeIndex.get(name); return i == null ? -1 : i; }

    /** model px (bedrock space) -> world offset from the entity's feet, for a body yaw in degrees */
    public static Vec3 toWorld(double bx, double by, double bz, float yawDeg, float k) {
        double yr = Math.toRadians(yawDeg);
        double fx = -Math.sin(yr), fz = Math.cos(yr); // forward
        double lx = Math.cos(yr), lz = Math.sin(yr);  // left
        double f = -bz;
        return new Vec3((bx * lx + f * fx) * k, by * k, (bx * lz + f * fz) * k);
    }

    /** node position in model px for a clip at tick t */
    public void nodePx(Clip c, int t, int node, float[] out) {
        int ti = c.tickIndex(t);
        int i = (ti * nodes.size() + node) * 3;
        out[0] = c.pos[i]; out[1] = c.pos[i + 1]; out[2] = c.pos[i + 2];
    }
}
