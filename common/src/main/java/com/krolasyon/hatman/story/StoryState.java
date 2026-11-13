package com.krolasyon.hatman.story;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.krolasyon.hatman.HatmanMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Everything the story has to remember. Saved as plain JSON next to the world. */
public class StoryState {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean active = false;
    public String player = "";
    public int ox, oy, oz;
    public String step = "none";
    public int t = 0;
    public int chapter = 0;
    public Map<String, Integer> vars = new HashMap<>();
    public Set<String> flags = new HashSet<>();
    public Set<String> notes = new HashSet<>();
    public Set<String> opened = new HashSet<>();
    public Set<String> unlocked = new HashSet<>();
    public float anger = 0, claim = 0;
    public int lies = 0;
    public float battery = 100;
    public boolean nvOn = false, hasCam = false, power = false;
    public int haunt = 0;
    public String objective = "", objectiveArg = "";
    public String cpStep = "";
    public double cpX, cpY, cpZ;
    public float cpYaw;
    public int caught = 0;
    public String ending = "";
    // what the world looked like before the story took it over
    public String prevMode = "survival";
    public boolean prevDaylight = true, prevWeather = true, prevMobs = true, prevFire = true;
    public long prevTime = 0;

    public int v(String k) { return vars.getOrDefault(k, 0); }
    public void v(String k, int val) { vars.put(k, val); }
    public boolean f(String k) { return flags.contains(k); }
    public void flag(String k) { flags.add(k); }

    private static Path file(MinecraftServer s) {
        return s.getWorldPath(LevelResource.ROOT).resolve("hatman_story.json");
    }

    public static StoryState load(MinecraftServer s) {
        Path p = file(s);
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                StoryState st = GSON.fromJson(r, StoryState.class);
                if (st != null) {
                    if (st.vars == null) st.vars = new HashMap<>();
                    if (st.flags == null) st.flags = new HashSet<>();
                    if (st.notes == null) st.notes = new HashSet<>();
                    if (st.opened == null) st.opened = new HashSet<>();
                    if (st.unlocked == null) st.unlocked = new HashSet<>();
                    return st;
                }
            } catch (Exception e) {
                HatmanMod.LOG.error("Could not read story state", e);
            }
        }
        return new StoryState();
    }

    public void save(MinecraftServer s) {
        try (Writer w = Files.newBufferedWriter(file(s), StandardCharsets.UTF_8)) {
            GSON.toJson(this, w);
        } catch (Exception e) {
            HatmanMod.LOG.error("Could not save story state", e);
        }
    }
}
