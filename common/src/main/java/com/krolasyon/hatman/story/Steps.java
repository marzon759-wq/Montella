package com.krolasyon.hatman.story;

import com.krolasyon.hatman.block.OpenableBlock;
import com.krolasyon.hatman.entity.GuestEntity;
import com.krolasyon.hatman.entity.HatmanEntity;
import com.krolasyon.hatman.net.Msg;
import com.krolasyon.hatman.net.Net;
import com.krolasyon.hatman.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import static com.krolasyon.hatman.story.Story.*;

/**
 * THE HATMAN: HOLLOW HOUSE - the story itself, as a chain of steps. A step only begins when the previous one
 * has been completed, so chapters can never be skipped.
 */
final class Steps {
    private Steps() {}

    private static void laterIn(String step, int ticks, Runnable r) {
        later(ticks, () -> {
            if (S.active && S.step.equals(step)) r.run();
        });
    }

    private static void tp(ServerPlayer p, double x, double y, double z, float yaw) {
        Vec3 v = w(x, y, z);
        p.teleportTo(p.serverLevel(), v.x, v.y + 0.05, v.z, yaw, 0);
    }

    private static HatmanEntity hunter(double x, double y, double z, double speed, boolean emerge) {
        ServerPlayer p = player();
        HatmanEntity e = spawnHatman(x, y, z, 0);
        if (e == null || p == null) return null;
        e.face(p.position());
        e.huntSpeed = speed;
        e.huntPatience = 160;
        e.lethal = true;
        if (emerge) e.emerge(HatmanEntity.HUNT);
        else {
            e.beginHunt();
            e.act("scream", 24, "hm.scream");
            e.stateTicks = -22;
        }
        return e;
    }

    /** keep the pressure on: if the hunter is gone during a chase, he comes back out of the floor */
    private static void rehunt(String step, double speed) {
        laterIn(step, 50, () -> {
            ServerPlayer p = player();
            if (p == null || hatmanPresent()) return;
            Vec3 spot = findSpot(p, 8, 14, true, true);
            if (spot == null) spot = findSpot(p, 7, 16, false, false);
            if (spot == null) return;
            Vec3 l = spot.subtract(S.ox, S.oy, S.oz);
            hunter(l.x, l.y, l.z, speed, true);
        });
    }

    private static int count(String... notes) {
        int n = 0;
        for (String s : notes) if (S.notes.contains(s)) n++;
        return n;
    }

    // ================================================================== enter
    static void enter(String step) {
        ServerPlayer p = player();
        S.step = step;
        S.t = 0;
        S.vars.remove("beat");
        S.vars.remove("sub");
        if (p == null) return;
        ServerLevel lvl = p.serverLevel();
        switch (step) {
            case "intro" -> {
                S.haunt = 0;
                tp(p, 22.5, 1, 56.5, 180);
                setDoor(lvl, "gate", true);
                setDoor(lvl, "front", false);
                cutscene("intro");
            }
            case "c1_enter" -> {
                later(30, () -> chapter(1));
                obj("enter");
                S.haunt = 0;
            }
            case "c1_camp" -> obj("camp");
            case "c1_cam" -> obj("camcorder");
            case "c1_file" -> {
                obj("casefile");
                if (S.notes.contains("case_file")) enter("c1_key");
            }
            case "c1_key" -> {
                obj("cellar_key");
                S.haunt = 1;
                hauntCd = 260;
                if (has("cellar_key")) enter("c1_cellar");
            }
            case "c1_cellar" -> obj("cellar");
            case "c1_fuel" -> {
                obj("fuel");
                if (has("fuel_can")) enter("c1_gen");
            }
            case "c1_gen" -> obj("generator");
            case "c1_phone" -> {
                obj("phone");
                S.haunt = 0;
                setMode("phone", 1);
            }
            case "c2_evidence" -> {
                chapter(2);
                S.haunt = 2;
                hauntCd = 300;
                obj("evidence", "" + evidence());
            }
            case "c2_clock" -> obj("clock");
            case "c2_workshop" -> obj("workshop");
            case "c2_ledger" -> obj("ledger");
            case "c2_chase" -> {
                checkpoint("c2_chase", 9.5, B1, 3.5, 90);
                S.haunt = 0;
                S.flags.remove("bar:study");
                unlock("workshop");
                unlock("cellar");
                power(false);
                snd("fx.power_down", 1f, 1f);
                setDoor(lvl, "study", true);
                obj("run_study");
                think("c2_run");
                hunter(3.5, B1, 10.5, 4.75, true);
            }
            case "c2_door" -> {
                S.cpStep = "";
                obj("");
            }
            case "c3_up" -> {
                chapter(3);
                S.haunt = 2;
                hauntCd = 400;
                S.flags.remove("bar:study");
                obj("upstairs");
            }
            case "c3_masks" -> obj("masks");
            case "c3_trail" -> {
                S.haunt = 3;
                hauntCd = 500;
                obj("trail", "" + count("diary", "ruth_bag", "mirror_note"));
            }
            case "c3_master" -> obj("recorder");
            case "c3_hide" -> {
                checkpoint("c3_hide", 8.5, F1, 25.5, 180);
                S.haunt = 0;
                removeHatman();
                setDoor(lvl, "master", false);
                lock("master");
                obj("hide");
                think("c3_knock");
                sndAt(7.5, F1 + 1, 17.5, "hm.knock", 1f, 1f);
            }
            case "c3_sneak" -> {
                obj("dressing");
                lock("master");
            }
            case "c3_chase" -> {
                checkpoint("c3_chase", 16.5, F1, 24.5, -90);
                unlock("master");
                setDoor(lvl, "dress", true);
                obj("run_library");
                HatmanEntity e = hunter(11.5, F1, 24.5, 4.8, false);
                if (e != null) e.huntPatience = 60;
            }
            case "c3_lib" -> {
                S.cpStep = "";
                S.haunt = 0;
                unlock("library");
                setDoor(lvl, "library", true);
                sndAt(37.5, F1 + 1, 13.5, "fx.door_creak", 1f, 0.8f);
                obj("library");
                think("c3_door_open");
            }
            case "c3_talk" -> {
                obj("");
                removeHatman();
                campfire("fireplace", true);
                HatmanEntity e = spawnHatman(38.5, F1, 7.5, 90);
                if (e != null) {
                    e.setState(HatmanEntity.TALK);
                    e.sit(true);
                }
                setDoor(lvl, "library", false);
                laterIn("c3_talk", 50, () -> startDialogue("d3", "l1"));
            }
            case "c3_rage" -> {
                checkpoint("c3_lib", 37.5, F1, 15.5, 0);
                obj("survive");
                setDoor(lvl, "library", true);
            }
            case "c4_attic" -> {
                chapter(4);
                S.cpStep = "";
                S.haunt = 2;
                hauntCd = 500;
                campfire("fireplace", false);
                obj("attic");
            }
            case "c4_search" -> obj("badge");
            case "c4_mirrors" -> {
                obj("");
                S.haunt = 0;
                removeHatman();
                think("c4_mirrors");
                laterIn("c4_mirrors", 50, () -> {
                    flicker("ATTIC", 40);
                    snd("fx.sting", 1f, 1f);
                });
                laterIn("c4_mirrors", 110, () -> enter("c4_chase"));
            }
            case "c4_chase" -> {
                checkpoint("c4_chase", 17.5, A1, 10.5, 90);
                S.haunt = 0;
                power(false);
                snd("fx.power_down", 1f, 0.9f);
                for (String d : new String[]{"hatch", "cellar", "workshop", "library", "servant", "stairgate"}) unlock(d);
                setDoor(lvl, "hatch", true);
                setDoor(lvl, "stairgate", true);
                closeShelf(lvl);
                S.flags.remove("cold_open");
                guestsToCorridor(false);
                obj("run_cellar");
                think("c4_run");
                removeHatman();
                HatmanEntity e = spawnHatman(30.5, A1, 15.5, 90);
                if (e != null) {
                    e.huntSpeed = 5.0;
                    e.huntPatience = 200;
                    e.face(p.position());
                    e.drop(HatmanEntity.HUNT);
                }
                spawnRuth(lvl);
            }
            case "c4_cold" -> {
                S.cpStep = "";
                obj("ruth");
                spawnRuth(lvl);
            }
            case "c5_walk" -> {
                chapter(5);
                S.haunt = 0;
                S.cpStep = "";
                S.flags.remove("dark:all");
                power(true);
                guestsToCorridor(false);
                guestsWatch(true);
                for (String d : new String[]{"hatch", "cellar", "workshop", "library", "servant", "stairgate", "master"}) unlock(d);
                openShelf(lvl);
                obj(has("original_face") ? "dining" : "dining_safe");
            }
            case "c5_talk" -> {
                obj("");
                startDialogue("d5", "f1");
            }
            case "c5_fire" -> {
                checkpoint("c5_fire", 33.5, G1, 21.5, 90);
                S.flags.add("fire");
                S.flags.remove("sealed");
                unlock("front");
                unlock("gate");
                setDoor(lvl, "front", true);
                setDoor(lvl, "gate", true);
                setDoor(lvl, "dining", true);
                obj("get_out");
                think("c5_fire");
                snd("amb.fire", 1f, 1f);
                for (int x = 33; x <= 41; x++)
                    for (int z = 23; z <= 24; z++) fire(lvl, x, G1 + 1, z);
                for (int i = 0; i < 26; i++) fire(lvl, 31 + RND.nextInt(13), G1, 18 + RND.nextInt(12));
                for (GuestEntity g : guests)
                    if (g.isAlive()) {
                        g.setSitting(false);
                        g.playAction("point");
                    }
                removeHatman();
                HatmanEntity e = spawnHatman(42.5, G1, 24.5, 90);
                if (e != null) {
                    e.huntSpeed = 5.1 + S.anger / 250.0;
                    e.huntPatience = 200;
                    e.beginHunt();
                    e.act("scream", 50, "hm.scream");
                    e.stateTicks = -50;
                }
                fx("shake", 40);
            }
            case "c5_run" -> {
                checkpoint("c5_run", 33.5, G1, 21.5, 90);
                obj("");
                unlock("cellar");
                setDoor(lvl, "dining", true);
                BlockPos gate = House.first("tunnelgate");
                if (gate != null) {
                    lvl.setBlock(wp(gate), Blocks.AIR.defaultBlockState(), 3);
                    lvl.setBlock(wp(gate).above(), Blocks.AIR.defaultBlockState(), 3);
                }
                removeHatman();
                HatmanEntity e = spawnHatman(42.5, G1, 24.5, 90);
                if (e != null) {
                    e.setState(HatmanEntity.TALK);
                    e.act("stand_up", 30, null);
                }
            }
            case "credits" -> {
                obj("");
                Net.toPlayer(p, Msg.of("credits").s("ending", S.ending).i("notes", S.notes.size()).i("notesMax", Notes.COUNT).i("caught", S.caught).i("lies", S.lies));
            }
            default -> {}
        }
        hud();
    }

    static final int G1 = House.G + 1, F1 = House.F + 1, A1 = House.A + 1, B1 = House.B + 1;

    static void resume() {
        // after a reload the scripted bits of the current step are gone: begin the step again
        String s = S.step;
        if (s.equals("none")) return;
        if (s.equals("c2_door")) s = "c2_chase";
        if (s.equals("c3_sneak")) s = "c3_hide";
        if (s.equals("c3_talk") || s.equals("c3_rage")) s = "c3_lib";
        if (s.equals("c4_mirrors")) s = "c4_search";
        if (s.equals("c5_talk")) s = "c5_walk";
        enter(s);
    }

    private static int evidence() {
        return count("guest_list", "maid") + (S.f("tv_seen") ? 1 : 0);
    }

    private static void fire(ServerLevel lvl, int x, int y, int z) {
        BlockPos pos = wp(x, y, z);
        if (lvl.getBlockState(pos).isAir() && !lvl.getBlockState(pos.below()).isAir()) lvl.setBlock(pos, Blocks.FIRE.defaultBlockState(), 2);
    }

    private static void openShelf(ServerLevel lvl) {
        for (BlockPos l : House.TAGGED.getOrDefault("secret_shelf", java.util.List.of())) lvl.setBlock(wp(l), Blocks.AIR.defaultBlockState(), 3);
        S.flags.add("cold_open");
    }

    private static void closeShelf(ServerLevel lvl) {
        for (BlockPos l : House.TAGGED.getOrDefault("secret_shelf", java.util.List.of())) lvl.setBlock(wp(l), Blocks.BOOKSHELF.defaultBlockState(), 3);
        S.flags.remove("cold_open");
    }

    private static void spawnRuth(ServerLevel lvl) {
        if (ruth != null && ruth.isAlive()) return;
        GuestEntity g = ModEntities.GUEST.get().create(lvl);
        if (g == null) return;
        Vec3 v = w(7.5, B1, 20.5);
        g.moveTo(v.x, v.y, v.z, 180, 0);
        g.story = true;
        g.setup(180, 1, true);
        lvl.addFreshEntity(g);
        ruth = g;
    }

    // ================================================================== tick
    static void tick(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        Vec3 l = lp(p);
        String room = House.roomAt(l.x, l.y, l.z);
        int t = S.t;
        switch (S.step) {
            case "c1_enter" -> {
                if (room.equals("G_FOYER") && l.z < 28.3) {
                    setDoor(lvl, "front", false);
                    setDoor(lvl, "gate", false);
                    S.flags.add("sealed");
                    sndAt(22.5, 2, 30.5, "fx.door_slam", 1f, 0.9f);
                    fx("shake", 8);
                    later(34, () -> think("c1_door"));
                    enter("c1_camp");
                }
            }
            case "c1_camp" -> {
                if (t == 240) think("c1_dark");
                if (room.equals("G_STUDY")) {
                    think("c1_camp");
                    enter("c1_cam");
                }
            }
            case "c1_key" -> {
                if (!S.f("sc_corr") && room.equals("G_CORRIDOR") && t > 60) {
                    S.flags.add("sc_corr");
                    HatmanEntity e = spawnHatman(l.x < 22 ? 41.5 : 2.5, G1, 15.5, 0);
                    if (e != null) {
                        e.face(p.position());
                        e.setFlag(HatmanEntity.F_CAMONLY, true);
                        e.stareLimit = 46;
                        e.setState(HatmanEntity.WATCH);
                    }
                    fx("static", 6);
                    think("c1_glitch");
                    if (!S.nvOn) later(50, () -> msg("cam_hint"));
                }
            }
            case "c1_cellar" -> {
                if (room.equals("B_HALL") && l.z < 3.5 && l.y < -3) {
                    setDoor(lvl, "cellar", false);
                    sndAt(28.5, 1, 10.5, "fx.door_slam", 1f, 0.8f);
                    later(50, () -> sndAt(22, 2, 6, "fx.fake_steps", 1f, 0.9f));
                    later(70, () -> think("c1_cellar"));
                    enter("c1_fuel");
                }
            }
            case "c1_fuel" -> {
                if (t == 260) sndAt(22, B1, 24, "fx.thump", 1f, 0.8f);
                if (room.equals("B_GENERATOR") && !S.f("gen_seen")) {
                    S.flags.add("gen_seen");
                    think("c1_gen_empty");
                }
            }
            case "c1_phone" -> {
                if (t % 70 == 5) {
                    BlockPos ph = House.first("phone");
                    sndAt(ph.getX() + 0.5, ph.getY() + 0.5, ph.getZ() + 0.5, "fx.phone_ring", 1.6f, 1f);
                }
            }
            case "c2_evidence" -> {
                int n = evidence();
                if (!S.objectiveArg.equals("" + n)) obj("evidence", "" + n);
                BlockPos tv = House.first("tv");
                if (!S.f("tv_seen") && room.equals("G_LIVING") && distLocal(p, tv.getX() + 0.5, l.y, tv.getZ() + 0.5) < 6.5) {
                    removeHatman();
                    setMode("tv", 1);
                    cutscene("tv");
                    return;
                }
                if (n >= 3) {
                    think("c2_clock");
                    enter("c2_clock");
                }
            }
            case "c2_workshop" -> {
                if (room.equals("B_WORKSHOP")) {
                    think("c2_shop");
                    enter("c2_ledger");
                }
            }
            case "c2_chase" -> {
                if (S.v("beat") == 0 && (room.equals("G_STAIRHALL") || room.equals("G_CORRIDOR")) && l.y > 0.5) {
                    S.v("beat", 1);
                    if (hm == null || hm.distanceTo(p) > 9) {
                        boolean east = l.x < 30;
                        hunter(east ? 39.5 : 3.5, G1, 15.5, 4.9, true);
                        think("c2_ahead");
                    }
                }
                if (room.equals("G_STUDY") && t % 50 == 0) msg("bar_hint");
            }
            case "c2_door" -> tickStudyDoor(p, lvl, t);
            case "c3_up" -> {
                if ((room.equals("F_STAIRHALL") || room.equals("F_CORRIDOR")) && l.y > 6.5) {
                    removeHatman();
                    cutscene("masquerade");
                }
            }
            case "c3_trail" -> {
                int n = count("diary", "ruth_bag", "mirror_note");
                if (!S.objectiveArg.equals("" + n)) obj("trail", "" + n);
                if (room.equals("F_CHILD") && !S.f("mb")) {
                    S.flags.add("mb");
                    setMode("musicbox", 1);
                    BlockPos mb = House.first("musicbox");
                    sndAt(mb.getX() + 0.5, mb.getY(), mb.getZ() + 0.5, "fx.music_box", 1.2f, 1f);
                    think("c3_musicbox");
                }
                if (room.equals("F_BATH") && !S.f("bath")) {
                    S.flags.add("bath");
                    later(40, () -> think("c3_bath"));
                }
                if (n >= 3 && !hatmanPresent()) {
                    think("c3_recorder");
                    unlock("master");
                    sndAt(7.5, F1 + 1, 17.5, "fx.unlock", 1f, 1f);
                    enter("c3_master");
                }
            }
            case "c3_hide" -> tickHide(p, lvl, t);
            case "c3_sneak" -> {
                if (room.equals("F_DRESSING") && l.x > 17.5 && hiddenIn(p) == null) {
                    snd("fx.piano", 1f, 1f);
                    think("c3_piano");
                    enter("c3_chase");
                }
            }
            case "c3_chase" -> {
                if (S.v("beat") == 0 && room.equals("F_CORRIDOR") && l.x > 31 && t > 30) lockedLibrary();
                if (t > 1300 && hm != null && hm.getState() == HatmanEntity.HUNT) hm.beginSearch(hm.position());
                if (!hatmanPresent() && t > 100 && S.v("sub") == 0) {
                    S.v("sub", 1);
                    laterIn("c3_chase", 50, () -> enter("c3_lib"));
                }
            }
            case "c3_lib" -> {
                if (room.equals("F_LIBRARY") && l.z < 11.5 && l.x < 41) enter("c3_talk");
            }
            case "c3_rage" -> {
                if ((t > 700 || (!hatmanPresent() && t > 80)) && S.v("sub") == 0) {
                    S.v("sub", 1);
                    if (hm != null) hm.vanish();
                    later(40, () -> {
                        sndAt(15.5, A1, 9.5, "fx.wood_break", 1.6f, 0.8f);
                        unlock("hatch");
                        setDoor("hatch", true);
                        think("c3_hatch");
                    });
                    later(120, () -> enter("c4_attic"));
                }
            }
            case "c4_attic" -> {
                if (room.equals("ATTIC") && l.y > 12.5) {
                    removeHatman();
                    cutscene("fitting");
                }
            }
            case "c4_chase" -> tickBigChase(p, lvl, l, room, t);
            case "c4_cold" -> {
                if (ruth != null && ruth.isAlive() && ruth.distanceTo(p) < 4.2 && t > 40) startDialogue("d4", "r1");
            }
            case "c5_walk" -> {
                String want = has("original_face") ? "dining" : "dining_safe";
                if (!S.objective.equals(want)) obj(want);
                if (t == 60) think("c5_quiet");
                if (room.equals("G_DINING") && l.z > 18.8) cutscene("supper");
            }
            case "c5_fire" -> {
                if (t % 12 == 0) {
                    // the fire follows you out
                    for (int i = 0; i < 3; i++) fire(lvl, (int) l.x - 6 + RND.nextInt(13), (int) Math.round(l.y), (int) l.z - 6 + RND.nextInt(13));
                }
                if (t % 100 == 50) snd("amb.fire", 1f, 1f);
                if (room.equals("ROAD") || l.z > 53.2) {
                    removeHatman();
                    endingCut("ashes", "end_ashes");
                }
            }
            case "c5_run" -> {
                if (t == 20) say("dlg.count1", "hatman", 36);
                if (t == 60) say("dlg.count2", "hatman", 36);
                if (t == 100) say("dlg.count3", "hatman", 40);
                if (t == 140 && hm != null) {
                    hm.huntSpeed = 4.8 + S.anger * 0.006;
                    hm.huntPatience = 240;
                    hm.beginHunt();
                    hm.act("scream", 30, "hm.scream");
                    hm.stateTicks = -28;
                    fx("shake", 30);
                    obj("tunnel");
                    think("c5_front");
                }
                if (room.equals("YARD") && l.y > 0.5 && S.v("sub") == 0) {
                    S.v("sub", 1);
                    S.flags.remove("sealed");
                    unlock("gate");
                    setDoor(lvl, "gate", true);
                    obj("gate");
                }
                if (l.z > 53.2 && l.y > 0.5) {
                    removeHatman();
                    endingCut("cold", "end_cold");
                }
            }
            default -> {}
        }
    }

    private static void lockedLibrary() {
        if (S.v("beat") != 0) return;
        S.v("beat", 1);
        obj("hide_child");
        think("c3_locked");
        // he savours it: a moment's head start, just enough to reach the boy's wardrobe
        if (hm != null && hm.isAlive()) {
            hm.act("twitch", 20, "hm.twitch");
            hm.stateTicks = -50;
            hm.huntSpeed = 4.4;
        }
        flicker("F_CORRIDOR", 40);
    }

    // ---------------------------------------------------------------- chapter 2: the study door
    private static void tickStudyDoor(ServerPlayer p, ServerLevel lvl, int t) {
        if (S.v("sub") != 0) {
            // he was invited in
            if (S.v("sub") == 1 && hm != null && (hm.scriptArrived() || t > 160)) {
                S.v("sub", 2);
                hm.scriptStop();
                hm.face(p.position());
                hm.act("lean_in", 120, "hm.breath");
                fx("freeze", 130);
                laterIn("c2_door", 130, () -> {
                    if (hm != null) hm.act("hat_tip", 44, null);
                });
                laterIn("c2_door", 180, () -> {
                    if (hm != null) hm.vanish();
                    afterDoor();
                });
            }
            return;
        }
        if (t == 2) {
            // wherever the chase left him, he arrives at this door
            if (!hatmanPresent() || hm.position().distanceTo(w(7.5, G1, 14.6)) > 12) spawnHatman(13.5, G1, 15.5, 90);
            if (hm != null) hm.scriptMove(w(7.5, G1, 14.6), 5.2);
        }
        if (t == 50 || t == 80 || t == 104) {
            if (hm != null) {
                hm.scriptStop();
                hm.face(w(7.5, G1, 13.5));
                hm.act("burst", 12, "hm.bang");
            }
            fx("shake", 8);
        }
        if (t == 190) think("c2_quiet");
        if (t == 270) {
            if (hm != null) hm.act("knock", 40, null);
            startDialogue("d2", "k1");
        }
    }

    private static void afterDoor() {
        laterIn("c2_door", 70, () -> {
            think("c2_key");
            give("stair_key");
        });
        laterIn("c2_door", 170, () -> enter("c3_up"));
    }

    // ---------------------------------------------------------------- chapter 3: hide in the wardrobe
    private static void tickHide(ServerPlayer p, ServerLevel lvl, int t) {
        House.Hide hid = hiddenIn(p);
        if (S.v("sub") == 9) return; // it went wrong: the hunt AI owns him now
        if (t == 60) sndAt(7.5, F1 + 1, 17.5, "hm.knock", 1f, 0.9f);
        if (t == 130) msg(S.nvOn ? "hide_cam" : "hide_now");
        if (t == 170) {
            unlock("master");
            setDoor(lvl, "master", true);
            sndAt(7.5, F1 + 1, 17.5, "fx.door_creak", 1f, 0.7f);
            HatmanEntity e = spawnHatman(7.5, F1, 15.6, 0);
            if (e != null) e.scriptMove(w(7.5, F1, 21.0), 1.7);
        }
        if (t > 190 && hm != null && hid == null && hm.sees(p)) {
            // he found you standing in the open
            S.v("sub", 9);
            hm.huntSpeed = 4.9;
            hm.beginHunt();
            hm.act("scream", 26, "hm.scream");
            hm.stateTicks = -24;
            obj("run_hide");
            return;
        }
        if (hm == null) return;
        if (t == 270) hm.scriptMove(w(5.5, F1, 26.4), 1.7);
        if (t == 370) {
            hm.scriptStop();
            hm.act("search", 84, "hm.search");
        }
        if (t == 465) {
            BlockPos target = hid != null ? hid.pos.relative(hid.facing, 1) : new BlockPos(12, F1, 20);
            hm.scriptMove(worldCenter(target), 1.6);
        }
        if (t == 560) {
            hm.scriptStop();
            if (hid != null) hm.face(worldCenter(hid.pos));
            hm.act("lean_in", 90, "hm.breath");
            fx("slats", 90);
        }
        if (t == 600) hm.say("hm.breath", 1f, 0.7f);
        if (t == 650) {
            if (hid != null && S.nvOn) {
                hm.act("grab", 16, "hm.grab");
                caught(p, hm);
                return;
            }
            hm.playAction("stand_up");
        }
        if (t == 690) hm.scriptMove(w(7.5, F1, 19.0), 1.8);
        if (t == 770) hm.scriptMove(w(7.5, F1, 15.6), 1.8);
        if (t == 850) {
            hm.scriptStop();
            setDoor(lvl, "master", false);
            sndAt(7.5, F1 + 1, 17.5, "fx.door_slam", 0.7f, 1f);
            think("c3_outside");
            S.cpStep = "c3_hide";
            enter("c3_sneak");
        }
    }

    // ---------------------------------------------------------------- chapter 4: the long way down
    private static void tickBigChase(ServerPlayer p, ServerLevel lvl, Vec3 l, String room, int t) {
        int beat = S.v("beat");
        if (beat == 0 && l.y < 12.2 && l.y > 6.5 && (room.equals("F_STAIRHALL") || room.equals("F_CORRIDOR"))) {
            S.v("beat", 1);
            // he is already coming up the main stairs
            hunter(22.5, G1, 11.6, 5.0, false);
            think("c4_stairs");
            obj("servant");
            sndAt(22.5, 4, 8, "hm.step", 1.5f, 1f);
        } else if (beat == 1 && l.y < 6 && l.y > 0.5 && (room.equals("G_KITCHEN") || room.equals("G_CORRIDOR"))) {
            S.v("beat", 2);
            guestsToCorridor(true);
            obj("run_cellar");
            laterIn("c4_chase", 36, () -> {
                ServerPlayer pp = player();
                if (pp == null) return;
                if (hm == null || hm.distanceTo(pp) > 11) {
                    removeHatman();
                    HatmanEntity e = spawnHatman(40.5, G1, 6.5, 0);
                    if (e != null) {
                        e.huntSpeed = 5.0;
                        e.huntPatience = 200;
                        e.face(pp.position());
                        e.drop(HatmanEntity.HUNT);
                    }
                }
                think("c4_guests");
            });
        } else if (beat == 2 && l.y < -1.5 && room.equals("B_HALL")) {
            S.v("beat", 3);
            if (hm == null || hm.distanceTo(p) > 8) {
                setDoor(lvl, "generator", true);
                sndAt(30.5, B1 + 1, 6.5, "fx.wood_break", 1.4f, 1f);
                hunter(32.5, B1, 6.5, 5.1, false);
            }
            think("c4_block");
            obj("hatblock");
        }
        if (room.equals("B_WORKSHOP") && t % 60 == 0 && !S.f("cold_open")) msg("hatblock_hint");
        if (S.f("cold_open") && room.equals("B_COLD") && l.z > 15.2) {
            closeShelf(lvl);
            sndAt(7.5, B1 + 1, 13.5, "fx.door_slam", 1f, 0.7f);
            later(20, () -> {
                sndAt(7.5, B1 + 1, 12.5, "hm.bang", 1f, 1f);
                fx("shake", 8);
            });
            later(50, () -> sndAt(7.5, B1 + 1, 12.5, "hm.bang", 1f, 0.9f));
            later(90, () -> {
                removeHatman();
                think("c4_safe");
            });
            enter("c4_cold");
        }
    }

    // ================================================================== callbacks
    /** @return true if the step consumed the click and vanilla should not toggle the door */
    static boolean onDoor(ServerPlayer p, String id) {
        Vec3 l = lp(p);
        if (S.step.equals("c2_chase") && id.equals("study") && l.z < 13.2) {
            setDoor(p.serverLevel(), "study", false);
            S.flags.add("bar:study");
            snd("fx.lock", 1f, 0.8f);
            msg("barred");
            enter("c2_door");
            return true;
        }
        if (S.f("bar:" + id)) {
            msg("bar_stays");
            return true;
        }
        if (S.step.equals("c3_sneak") && id.equals("master")) {
            msg("he_is_there");
            snd("hm.breath", 0.8f, 0.8f);
            return true;
        }
        if (S.step.equals("c3_chase") && id.equals("library")) {
            snd("fx.lock", 1f, 1f);
            msg("locked");
            lockedLibrary();
            return true;
        }
        return false;
    }

    static void onItem(ServerPlayer p, String id) {
        if (id.equals("cellar_key") && S.step.equals("c1_key")) enter("c1_cellar");
        if (id.equals("fuel_can") && S.step.equals("c1_fuel")) enter("c1_gen");
        if (id.equals("lamp_oil") || id.equals("lighter")) {
            if (has("lamp_oil") && has("lighter")) think("oil_and_fire");
        }
    }

    static void onUse(ServerPlayer p, String tag, BlockPos lpos) {
        ServerLevel lvl = p.serverLevel();
        switch (tag) {
            case "camcorder" -> {
                if (S.hasCam) return;
                lvl.setBlock(wp(lpos), Blocks.AIR.defaultBlockState(), 3);
                give("camcorder");
                S.hasCam = true;
                S.battery = 100;
                Net.toPlayer(p, Msg.of("hint").s("key", "cam"));
                if (S.step.equals("c1_cam")) enter("c1_file");
            }
            case "generator" -> {
                if (S.power) { msg("gen_running"); return; }
                if (!S.step.equals("c1_gen") && !has("fuel_can")) { msg("gen_empty"); return; }
                if (!take("fuel_can")) { msg("gen_empty"); return; }
                setMode("generator", 1);
                snd("fx.generator_start", 1f, 1f);
                obj("");
                later(56, () -> cutscene("power"));
            }
            case "phone" -> {
                if (S.step.equals("c1_phone")) {
                    setMode("phone", 0);
                    snd("fx.phone_pick", 1f, 1f);
                    obj("");
                    startDialogue("d1", "p1");
                } else msg("phone_dead");
            }
            case "tv" -> {
                setMode("tv", 1);
                sndAt(lpos.getX() + 0.5, lpos.getY() + 0.5, lpos.getZ() + 0.5, "fx.static", 0.7f, 1f);
                later(60, () -> setMode("tv", 0));
                msg("tv_static");
            }
            case "clock" -> {
                if (S.step.equals("c2_clock")) {
                    give("workshop_key");
                    for (int i = 0; i < 3; i++) later(20 + i * 30, () -> snd("fx.clock_chime", 1f, 0.8f));
                    later(120, () -> think("c2_thirteen"));
                    enter("c2_workshop");
                } else {
                    snd("fx.clock_tick", 1f, 1f);
                    msg("clock");
                }
            }
            case "ledger" -> {
                if (!has("ledger") && !S.f("ledger_taken")) {
                    S.flags.add("ledger_taken");
                    give("ledger");
                }
                note("ledger");
            }
            case "plates", "plate_ruth", "plate_you" -> {
                if (tag.equals("plate_you")) msg("plate_you", p.getGameProfile().getName());
                note("plates");
            }
            case "musicbox" -> {
                setMode("musicbox", 0);
                msg("musicbox");
            }
            case "recorder" -> {
                if (!S.step.equals("c3_master")) { msg("recorder_later"); return; }
                if (S.v("sub") != 0) return;
                S.v("sub", 1);
                lvl.setBlock(wp(lpos), Blocks.AIR.defaultBlockState(), 3);
                give("recorder_tape");
                S.haunt = 0;
                removeHatman();
                snd("fx.tape", 1f, 1f);
                obj("");
                for (int i = 0; i < 5; i++) {
                    final int k = i;
                    laterIn("c3_master", 30 + i * 96, () -> say("line.tape" + k, "ruth", 90));
                }
                laterIn("c3_master", 30 + 5 * 96, () -> {
                    S.notes.add("ruth_tape");
                    enter("c3_hide");
                });
            }
            case "badge" -> {
                if (!has("badge")) {
                    lvl.setBlock(wp(lpos), Blocks.AIR.defaultBlockState(), 3);
                    give("badge");
                }
                note("badge_note");
            }
            case "hatbox" -> {
                note("hatbox_card");
            }
            case "hook_you" -> msg("hook_you", p.getGameProfile().getName());
            case "hooks" -> msg("hooks");
            case "hatblock" -> {
                if (S.f("cold_open")) return;
                if (!S.f("knows_block") && !S.step.equals("c4_chase")) { msg("hatblock_stuck"); return; }
                openShelf(lvl);
                sndAt(7.5, B1 + 1, 13.5, "fx.wood_break", 1f, 0.7f);
                msg("shelf_open");
            }
            case "safe" -> {
                if (S.f("safe_open")) msg("safe_empty");
                else Net.toPlayer(p, Msg.of("keypad"));
            }
            case "piano" -> snd("fx.piano", 0.8f, 0.8f + RND.nextFloat() * 0.5f);
            case "litter" -> msg("litter");
            case "mannequin" -> msg("mannequin");
            case "ruth_coat" -> msg("ruth_coat");
            case "atticmirror" -> msg("mirror_attic");
            case "bathmirror" -> msg("mirror_bath");
            case "seat" -> msg("seat", p.getGameProfile().getName());
            case "host_chair" -> msg("host_chair");
            case "his_chair" -> msg("his_chair");
            case "fireplace" -> msg("fireplace");
            case "secret_shelf" -> msg("shelf");
            case "tunnelgate" -> msg("tunnelgate");
            case "ruth_chair" -> msg("ruth_chair");
            case "bed" -> msg("bed");
            default -> {}
        }
    }

    static void onHide(ServerPlayer p, String id, boolean in) {}

    static void onHatmanEvent(String ev) {
        switch (S.step) {
            case "c2_chase" -> {
                if (ev.equals("gone")) rehunt("c2_chase", 4.8);
            }
            case "c3_hide" -> {
                // the scripted search failed and turned into a real hunt; when it is over he comes back to the door
                if (ev.equals("gone") && S.v("sub") == 9) laterIn("c3_hide", 60, () -> enter("c3_hide"));
            }
            case "c4_chase" -> {
                if (ev.equals("gone")) rehunt("c4_chase", 5.0);
            }
            case "c5_fire" -> {
                if (ev.equals("gone")) rehunt("c5_fire", 5.2);
            }
            case "c5_run" -> {
                if (ev.equals("gone") && S.t > 150) rehunt("c5_run", 4.9);
            }
            default -> {}
        }
    }

    static void onDialogueEnd(String id, String result, float angerDelta) {
        ServerPlayer p = player();
        if (p == null) return;
        ServerLevel lvl = p.serverLevel();
        switch (id) {
            case "d1" -> {
                flicker("", 50);
                snd("fx.power_down", 0.7f, 1.3f);
                setMode("tv", 1);
                later(90, () -> setMode("tv", 0));
                later(70, () -> think("c1_end"));
                later(60, () -> sndAt(22, 8, 6, "fx.fake_steps", 1.4f, 0.8f));
                laterIn("c1_phone", 170, () -> enter("c2_evidence"));
            }
            case "d2" -> {
                if (result.equals("invited")) {
                    S.flags.remove("bar:study");
                    setDoor(lvl, "study", true);
                    sndAt(7.5, G1 + 1, 13.5, "fx.door_creak", 1f, 0.7f);
                    if (!hatmanPresent()) spawnHatman(7.5, G1, 14.6, 180);
                    Vec3 l = lp(p);
                    Vec3 d = new Vec3(7.5 - l.x, 0, 13.5 - l.z);
                    Vec3 stand = d.lengthSqr() > 0.5 ? l.add(d.normalize().scale(1.5)) : l.add(0, 0, 1.4);
                    if (hm != null) hm.scriptMove(w(stand.x, G1, stand.z), 1.5);
                    S.t = 0;
                    S.v("sub", 1);
                } else if (angerDelta >= 14 || S.f("cursed_him")) {
                    if (hm != null) hm.act("scream", 50, "hm.scream");
                    fx("shake", 30);
                    flicker("G_", 60);
                    laterIn("c2_door", 80, () -> sndAt(0.5, G1 + 1, 4.5, "hm.knock", 1.3f, 0.8f));
                    laterIn("c2_door", 110, () -> think("c2_window"));
                    laterIn("c2_door", 150, () -> {
                        if (hm != null) hm.vanish();
                        afterDoor();
                    });
                    S.v("sub", 3);
                } else {
                    if (hm != null) hm.act("hat_tip", 44, null);
                    laterIn("c2_door", 50, () -> {
                        if (hm != null) hm.scriptMove(w(22.5, G1, 15.5), 1.8);
                    });
                    laterIn("c2_door", 150, () -> {
                        if (hm != null) hm.vanish();
                        afterDoor();
                    });
                    S.v("sub", 3);
                }
            }
            case "d3" -> {
                boolean angry = angerDelta >= 18 || S.anger >= 60;
                if (hm != null) hm.sit(false);
                if (angry) {
                    if (hm != null) {
                        hm.act("scream", 50, "hm.scream");
                        hm.huntSpeed = 4.9;
                        hm.huntPatience = 80;
                    }
                    fx("shake", 40);
                    flicker("F_", 60);
                    enter("c3_rage");
                    think("c3_rage");
                    later(52, () -> {
                        if (hm != null && S.step.equals("c3_rage")) hm.beginHunt();
                    });
                } else {
                    if (hm != null) hm.act("stand_up", 30, null);
                    later(44, () -> {
                        if (hm != null) hm.act("hat_tip", 44, null);
                    });
                    later(96, () -> {
                        if (hm != null) hm.vanish();
                        give("attic_key");
                        think("c3_key");
                        setDoor("library", true);
                    });
                    laterIn("c3_talk", 190, () -> enter("c4_attic"));
                }
            }
            case "d4" -> {
                S.flags.add("met_ruth");
                S.flags.add("knows_block");
                openShelf(lvl);
                sndAt(7.5, B1 + 1, 13.5, "fx.wood_break", 0.8f, 0.7f);
                later(40, () -> think("c4_go"));
                laterIn("c4_cold", 110, () -> enter("c5_walk"));
            }
            case "d5" -> {
                switch (result) {
                    case "closed" -> endingCut("closed", "end_closed");
                    case "newface" -> endingCut("newface", "end_newface");
                    case "worn" -> endingCut("worn", "end_worn");
                    case "burn" -> enter("c5_fire");
                    default -> enter("c5_run");
                }
            }
            default -> {}
        }
    }

    private static void endingCut(String ending, String cut) {
        S.ending = ending;
        S.cpStep = "";
        S.step = "ending";
        obj("");
        cutscene(cut);
    }

    static void cutCue(String id, int t) {
        ServerPlayer p = player();
        if (p == null) return;
        ServerLevel lvl = p.serverLevel();
        switch (id) {
            case "intro" -> {
                if (t == 4) snd("fx.tape", 1f, 1f);
                if (t == 300) sndAt(22, 3, 40, "fx.crow", 1.5f, 1f);
                if (t == 340) {
                    HatmanEntity e = spawnHatman(20.5, G1, 32.5, -40);
                    if (e != null) e.scriptStop();
                }
                if (t == 404) {
                    snd("fx.thunder", 1f, 1f);
                    fx("flash", 8);
                }
                if (t == 412) removeHatman();
                if (t == 548) sndAt(22.5, 2, 52, "fx.door_creak", 1f, 0.6f);
            }
            case "power" -> {
                if (t == 6) {
                    power(true);
                    snd("fx.switch", 1f, 1f);
                }
                if (t == 186) {
                    guestsWatch(true);
                    snd("fx.sting", 0.9f, 1f);
                }
                if (t == 296 || t == 350) {
                    BlockPos ph = House.first("phone");
                    sndAt(ph.getX() + 0.5, ph.getY() + 0.5, ph.getZ() + 0.5, "fx.phone_ring", 1.6f, 1f);
                }
                if (t == 290) setMode("phone", 1);
            }
            case "tv" -> {
                if (t == 4) snd("fx.tv_on", 1f, 1f);
                if (t == 148) {
                    HatmanEntity e = spawnHatman(5.5, G1, 24.5, -90);
                    if (e != null) e.scriptStop();
                    setMode("tv", 2);
                }
                if (t == 200 && hm != null) hm.act("head_tilt", 40, null);
                if (t == 262) {
                    setMode("tv", 0);
                    snd("fx.sting", 1f, 1f);
                    flicker("G_LIVING", 40);
                }
                if (t == 300 && hm != null) hm.act("hat_tip", 40, null);
                if (t == 336) removeHatman();
            }
            case "masquerade" -> {
                if (t == 4) sndAt(37, 8, 22, "fx.music_box", 1.6f, 0.8f);
                if (t == 232) snd("fx.sting", 0.8f, 0.9f);
                if (t == 330) snd("hm.whisper", 0.8f, 0.9f);
            }
            case "fitting" -> {
                if (t == 110) snd("fx.sting", 0.7f, 0.8f);
                if (t == 214) snd("hm.whisper", 0.9f, 0.8f);
            }
            case "supper" -> {
                if (t == 2) {
                    removeHatman();
                    HatmanEntity e = spawnHatman(42.5, G1, 24.5, 90);
                    if (e != null) {
                        e.setState(HatmanEntity.TALK);
                        e.sit(true);
                    }
                    guestsToCorridor(false);
                    guestsWatch(true);
                    setDoor(lvl, "dining", false);
                    Vec3 v = w(32.5, G1, 24.5);
                    p.teleportTo(lvl, v.x, v.y + 0.05, v.z, -90, 0);
                }
                if (t == 120) snd("fx.sting", 0.8f, 0.8f);
                if (t == 300 && hm != null) hm.act("hat_tip", 40, null);
            }
            case "end_closed" -> {
                if (t == 130) {
                    for (GuestEntity g : guests) if (g.isAlive()) g.discard();
                    snd("hm.vanish", 1f, 0.7f);
                    fx("flash", 10);
                }
                if (t == 270) {
                    if (hm != null) hm.vanish();
                    S.flags.remove("sealed");
                    unlock("front");
                    unlock("gate");
                    setDoor(lvl, "front", true);
                    setDoor(lvl, "gate", true);
                }
            }
            case "end_newface" -> {
                if (t == 110) {
                    fx("jumpscare", 14);
                    snd("fx.sting", 1f, 0.7f);
                }
            }
            case "end_worn" -> {
                if (t == 100) {
                    if (hm != null) hm.vanish();
                    snd("fx.sting", 1f, 0.6f);
                }
            }
            case "end_ashes" -> {
                if (t % 6 == 0 && t < 400) {
                    for (int i = 0; i < 6; i++) fire(lvl, RND.nextInt(45), RND.nextInt(3) == 0 ? House.A + 1 : (RND.nextBoolean() ? G1 : F1), RND.nextInt(31));
                    for (int i = 0; i < 4; i++) fire(lvl, -1 + RND.nextInt(47), 1, 32 + RND.nextInt(3));
                }
                if (t == 300) {
                    HatmanEntity e = spawnHatman(22.5, G1, 33.5, 0);
                    if (e != null) {
                        e.scriptStop();
                        e.act("hat_tip", 60, null);
                    }
                }
            }
            case "end_cold" -> {
                if (t == 150) {
                    HatmanEntity e = spawnHatman(22.5, G1, 44.5, 0);
                    if (e != null) e.scriptStop();
                }
                if (t == 230 && hm != null) hm.act("hat_tip", 60, null);
            }
            default -> {}
        }
    }

    static void onCutEnd(String id) {
        switch (id) {
            case "intro" -> {
                removeHatman();
                enter("c1_enter");
            }
            case "power" -> enter("c1_phone");
            case "tv" -> {
                S.flags.add("tv_seen");
                removeHatman();
                setMode("tv", 0);
                think("c2_tv");
            }
            case "masquerade" -> enter("c3_masks");
            case "fitting" -> enter("c4_search");
            case "supper" -> enter("c5_talk");
            case "end_closed", "end_newface", "end_worn", "end_ashes", "end_cold" -> {
                removeHatman();
                enter("credits");
            }
            default -> {}
        }
    }

    static void onNoteClosed(ServerPlayer p, String id) {
        switch (S.step) {
            case "c1_file" -> {
                if (id.equals("case_file")) {
                    think("c1_file");
                    enter("c1_key");
                }
            }
            case "c2_ledger" -> {
                if (id.equals("ledger")) enter("c2_chase");
            }
            case "c3_masks" -> {
                if (id.equals("plates")) {
                    think("c3_plates");
                    enter("c3_trail");
                }
            }
            case "c4_search" -> {
                if (id.equals("badge_note")) {
                    S.flags.add("knows_block");
                    enter("c4_mirrors");
                }
            }
            default -> {}
        }
        if (id.equals("invoice") && !S.f("code_known")) {
            S.flags.add("code_known");
            later(10, () -> think("code"));
        }
    }

    static void onKeypad(ServerPlayer p, String code) {
        if (S.f("safe_open")) return;
        if (code.equals("0731")) {
            S.flags.add("safe_open");
            BlockPos l = House.first("safe");
            BlockState s = p.serverLevel().getBlockState(wp(l));
            if (s.hasProperty(OpenableBlock.OPEN)) p.serverLevel().setBlock(wp(l), s.setValue(OpenableBlock.OPEN, true), 3);
            snd("fx.safe_open", 1f, 1f);
            give("original_face");
            later(30, () -> note("safe_note"));
            later(20, () -> snd("hm.whisper", 1f, 0.7f));
        } else {
            snd("fx.lock", 1f, 1.2f);
            msg("wrong_code");
        }
    }

    static void onCreditsDone(ServerPlayer p) {
        if (!S.step.equals("credits")) return;
        String ending = S.ending;
        Story.stop(p, false);
        p.sendSystemMessage(Component.translatable("msg.hatman.done", Component.translatable("ending.hatman." + ending)));
    }

    // ================================================================== debug
    static void debugJump(ServerPlayer p, String id) {
        S.hasCam = true;
        S.battery = 100;
        S.flags.add("sealed");
        if (!has("camcorder")) give("camcorder");
        double[] at = switch (id) {
            case "c1_enter" -> new double[]{22.5, 1, 50, 180};
            case "c1_camp", "c1_key", "c1_phone" -> new double[]{22.5, G1, 26, 180};
            case "c1_cam", "c1_file" -> new double[]{7.5, G1, 8, 180};
            case "c1_gen" -> new double[]{36.5, B1, 6.5, 180};
            case "c2_evidence", "c2_clock" -> new double[]{22.5, G1, 22, 180};
            case "c2_ledger", "c2_chase", "c2_workshop" -> new double[]{9.5, B1, 4.5, 90};
            case "c3_up" -> new double[]{22.5, G1, 14.5, 180};
            case "c3_masks", "c3_trail" -> new double[]{22.5, F1, 15.5, 90};
            case "c3_master", "c3_hide" -> new double[]{8.5, F1, 25.5, 180};
            case "c3_sneak" -> new double[]{10.5, F1, 24.5, -90};
            case "c3_chase" -> new double[]{16.5, F1, 24.5, -90};
            case "c3_lib" -> new double[]{37.5, F1, 15.5, 180};
            case "c4_attic" -> new double[]{17.5, F1, 9.5, 90};
            case "c4_search", "c4_mirrors", "c4_chase" -> new double[]{17.5, A1, 10.5, 90};
            case "c4_cold" -> new double[]{7.5, B1, 16.5, 0};
            case "c5_walk" -> new double[]{37.5, G1, 15.5, 0};
            case "c5_fire", "c5_run" -> new double[]{33.5, G1, 21.5, 90};
            default -> null;
        };
        int ch = id.length() > 1 && id.charAt(0) == 'c' ? id.charAt(1) - '0' : 0;
        S.power = ch >= 2 && !(id.equals("c2_chase") || id.equals("c4_chase"));
        if (ch >= 2) { unlock("cellar"); S.notes.add("case_file"); }
        if (ch >= 3) { unlock("workshop"); unlock("stairgate"); setDoor("stairgate", true); S.flags.add("tv_seen"); }
        if (ch >= 4) { unlock("master"); unlock("library"); unlock("hatch"); setDoor("hatch", true); }
        if (id.equals("c3_master") || id.equals("c3_lib")) { unlock("master"); unlock("library"); }
        refreshLamps(null);
        if (at != null) tp(p, at[0], at[1], at[2], (float) at[3]);
        if (ch > 0 && ch != S.chapter) S.chapter = ch;
        enter(id);
    }
}
