package com.krolasyon.mimic.story;

import com.krolasyon.mimic.block.OpenableBlock;
import com.krolasyon.mimic.entity.GuestEntity;
import com.krolasyon.mimic.entity.MimicEntity;
import com.krolasyon.mimic.entity.TruckEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import static com.krolasyon.mimic.story.Story.*;

/**
 * MIMIC: HARROW FARM - the story itself, as a chain of steps. A step only begins when the previous one has been
 * completed, so chapters can never be skipped. Decisions in the moment are dialogue nodes with a timer.
 */
final class Steps {
    private Steps() {}

    static final int G1 = House.G + 1, F1 = House.F + 1, B1 = House.B + 1, L1 = House.L + 1;

    private static void laterIn(String step, int ticks, Runnable r) {
        later(ticks, () -> {
            if (S.active && S.step.equals(step)) r.run();
        });
    }

    static void tp(ServerPlayer p, double x, double y, double z, float yaw) {
        Vec3 v = w(x, y, z);
        p.teleportTo(p.serverLevel(), v.x, v.y + 0.05, v.z, yaw, 0);
    }

    private static MimicEntity hunter(double x, double y, double z, double speed, boolean emerge) {
        ServerPlayer p = player();
        MimicEntity e = spawnMimic(x, y, z, 0);
        if (e == null || p == null) return null;
        e.face(p.position());
        e.huntSpeed = speed;
        e.huntPatience = 140;
        e.lethal = true;
        if (emerge) e.emerge(MimicEntity.HUNT);
        else {
            e.beginHunt();
            e.act("scream", 24, "mm.scream");
            e.stateTicks = -22;
        }
        return e;
    }

    private static boolean inHouse(ServerPlayer p) { return room(p).startsWith("H_") || room(p).startsWith("U_") || room(p).equals("CELLAR"); }

    private static boolean nearLocal(ServerPlayer p, double x, double y, double z, double r) { return distLocal(p, x, y, z) < r; }

    /** what it says to draw you out: your own words if it has learned them, otherwise your father's */
    static String bait() {
        if (S.f("learned:loud")) return "v.msam.loud";
        if (S.f("learned:its_me")) return "v.msam.its_me";
        if (S.f("learned:word")) return "v.msam.word";
        return RND.nextBoolean() ? "v.mdad.call1" : "v.mdad.call2";
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
            // ---------------------------------------------------------------- prologue
            case "intro" -> {
                S.haunt = 0;
                tp(p, 50.5, 1, 102.5, 180);
                cutscene("intro");
            }
            // ---------------------------------------------------------------- chapter 1: homecoming
            case "c1_gate" -> {
                later(30, () -> chapter(1));
                S.hasLight = true;
                S.battery = 100;
                if (!has("flashlight")) give("flashlight");
                obj("house");
                S.haunt = 1;
                checkpoint("", 50.5, 1, 102.5, 180);
                later(80, () -> think("gate_start"));
                later(200, () -> hint("light"));
            }
            case "c1_door" -> {
                obj("door");
                later(10, () -> think("porch"));
            }
            case "c1_inside" -> {
                obj("look");
                S.haunt = 0;
                later(40, () -> think("inside"));
            }
            case "c1_phone" -> obj("");
            case "c1_kitchen" -> {
                obj("kitchen");
                S.haunt = 1;
            }
            case "c1_radio" -> {
                obj("radio");
                hint("walkie");
            }
            case "c1_voice" -> {
                obj("");
                S.haunt = 0;
            }
            case "c1_up" -> obj("upstairs");
            case "c1_chase" -> {
                obj("run_out");
                checkpoint("c1_chase", 25.5, G1, 58.5, 0);
                MimicEntity e = hunter(25.5, F1, 53.5, 4.6, false);
                if (e != null) e.huntPatience = 100;
            }
            case "c1_hide" -> {
                obj("hide");
                checkpoint("c1_hide", 25.5, G1, 58.5, 180);
                hint("hide");
            }
            case "c1_sneak" -> {
                obj("sneak_out");
                checkpoint("c1_sneak", 25.5, G1, 58.5, 180);
                hint("crouch");
            }
            // ---------------------------------------------------------------- chapter 2: the barn
            case "c2_yard" -> {
                removeMimic();
                later(20, () -> chapter(2));
                obj("barn");
                S.haunt = 1;
                checkpoint("", 37.5, G1, 60.5, -90);
                laterIn("c2_yard", 120, () -> radio("v.lily.r7", 140));
            }
            case "c2_barn" -> {
                obj("cutters");
                S.haunt = 0;
                checkpoint("c2_barn", 70.5, G1, 48.5, 180);
                sleeper();
                S.v("disturb", 0);
            }
            case "c2_wake" -> {
                obj("");
                S.flags.add("woke");
                radio("v.lily.r8", 90);
                if (hm != null) {
                    hm.act("twitch", 20, "mm.twitch");
                    laterIn("c2_wake", 24, () -> {
                        if (hm != null) {
                            hm.wake();
                            hm.face(player().position());
                        }
                        startDialogue("wake", "wake");
                    });
                }
            }
            case "c2_ladder" -> {
                obj("loft_door");
                checkpoint("c2_barn", 70.5, G1, 48.5, 180);
                if (hm != null) {
                    hm.huntSpeed = 4.3;
                    hm.huntPatience = 200;
                    hm.beginHunt();
                    hm.act("roar", 30, "mm.roar");
                    hm.stateTicks = -28;
                } else hunter(70.5, G1, 30.5, 4.3, true);
            }
            case "c2_hay" -> {
                obj("stay_hidden");
                House.Hide best = null;
                double bd = 1e9;
                for (House.Hide h : House.HIDES) {
                    if (!h.id.startsWith("hay")) continue;
                    double d = worldCenter(h.pos).distanceToSqr(p.position());
                    if (d < bd) { bd = d; best = h; }
                }
                if (best != null && bd < 30 * 30) hide(p, best);
                S.flags.add("hid_hay");
            }
            case "c2_still" -> {
                obj("dont_move");
                qte("still", 200);
                if (hm != null) {
                    hm.scriptMove(p.position().add(p.getViewVector(1f).scale(1.6)), 1.3);
                }
            }
            // ---------------------------------------------------------------- chapter 3: the butcher's shed
            case "c3_shed" -> {
                removeMimic();
                later(20, () -> chapter(3));
                obj("shed");
                S.haunt = 2;
                checkpoint("", 56.5, G1, 14.5, 90);
                laterIn("c3_shed", 100, () -> radio("v.lily.r9", 160));
            }
            case "c3_trap" -> {
                obj("");
                S.haunt = 0;
                sndAt(44.5, G1, 31.5, "mm.eat", 1.2f, 1f);
                MimicEntity e = spawnMimic(44.5, G1, 32.5, 180);
                if (e != null) {
                    e.scriptStop();
                    e.setFlag(MimicEntity.F_EAT, true);
                }
                String b = bait();
                laterIn("c3_trap", 70, () -> {
                    if (hm != null) {
                        hm.setFlag(MimicEntity.F_EAT, false);
                        hm.act("listen", 30, null);
                    }
                    voiceAt(44.5, G1 + 2, 31.5, b, "it");
                });
                laterIn("c3_trap", 70 + voiceTicks(b) + 20, () -> startDialogue("trap", "trap"));
            }
            case "c3_throw" -> {
                obj("slip_away");
                take("meat");
                snd("fx.throw", 1f, 1f);
                if (hm != null) {
                    hm.scriptMove(w(52.5, G1, 36.5), 2.6);
                    laterIn("c3_throw", 60, () -> {
                        if (hm != null) {
                            hm.scriptStop();
                            hm.setFlag(MimicEntity.F_EAT, true);
                            hm.say("mm.eat", 1.2f, 1f);
                        }
                    });
                }
            }
            case "c3_sneak" -> {
                obj("slip_away");
                hint("crouch");
                if (hm != null) {
                    hm.setFlag(MimicEntity.F_EAT, false);
                    hm.scriptMove(w(44.5, G1, 26.5), 1.2);
                }
            }
            case "c3_run" -> {
                obj("run_silo");
                if (hm != null) {
                    hm.setFlag(MimicEntity.F_EAT, false);
                    hm.huntSpeed = 4.4;
                    hm.huntPatience = 160;
                    hm.beginHunt();
                    hm.act("roar", 30, "mm.roar");
                    hm.stateTicks = -28;
                }
            }
            // ---------------------------------------------------------------- chapter 4: the silo
            case "c4_silo" -> {
                removeMimic();
                later(20, () -> chapter(4));
                obj("padlock");
                S.haunt = 1;
                checkpoint("", 80.5, G1, 31.5, -90);
            }
            case "c4_door" -> obj("");
            case "c4_follow" -> {
                obj("truck");
                S.haunt = 0;
                spawnLily(88.5, G1, 31.5, 90);
                if (lily != null) lily.follow(true);
                checkpoint("c4_follow", 82.5, G1, 31.5, 90);
            }
            case "c4_corn" -> {
                obj("truck_alone");
                S.haunt = 0;
                checkpoint("c4_corn", 63.5, G1, 49.5, 90);
                if (lily != null) lily.discard();
                lily = null;
                // a head start: it comes up out of the corn behind you a few seconds later
                laterIn("c4_corn", 70, () -> {
                    MimicEntity e = hunter(80.5, G1, 56.5, 3.9, true);
                    if (e != null) e.huntPatience = 120;
                });
                laterIn("c4_corn", 80, () -> radio("v.lily.corn2", 100));
            }
            // ---------------------------------------------------------------- chapter 5: dawn
            case "c5_truck" -> {
                later(10, () -> chapter(5));
                obj(S.f("fueled") ? "start" : "fuel");
                S.haunt = 0;
                checkpoint("c5_truck", 44.5, G1, 50.5, 180);
            }
            case "c5_corn" -> {
                obj("lily_corn");
                checkpoint("c5_corn", 74.5, G1, 74.5, 180);
                spawnLily(76.5, G1, 85.5, 0);
                if (lily != null) lily.dragged(true);
                MimicEntity e = spawnMimic(76.5, G1, 87.0, 180);
                if (e != null) {
                    e.scriptStop();
                    e.setFlag(MimicEntity.F_DRAG, true);
                }
                later(40, () -> voiceAt(76.5, G1 + 1, 85.5, "v.lily.field2", "lily"));
            }
            case "credits" -> {
                obj("");
                removeMimic();
                S.save(server);
                credits(p);
            }
            default -> {}
        }
        S.save(server);
    }

    // ================================================================== resume after a reload
    static void resume() {
        ServerPlayer p = player();
        if (p == null) return;
        String st = S.step;
        // anything mid-scene restarts from its checkpoint
        if (st.equals("c2_barn") || st.equals("c2_wake") || st.equals("c2_ladder") || st.equals("c2_hay") || st.equals("c2_still")) { tp(p, 70.5, G1, 48.5, 180); enter("c2_barn"); return; }
        if (st.startsWith("c1_chase") || st.equals("c1_hide") || st.equals("c1_sneak") || st.equals("c1_up") || st.equals("c1_voice")) { tp(p, 25.5, G1, 58.5, 180); enter("c1_voice"); return; }
        if (st.equals("c3_trap") || st.equals("c3_throw") || st.equals("c3_sneak") || st.equals("c3_run")) { tp(p, 44.5, G1, 22.5, 180); enter("c3_trap"); return; }
        if (st.equals("c4_follow") || st.equals("c4_corn")) { tp(p, 82.5, G1, 31.5, 90); enter("c4_follow"); return; }
        if (st.equals("c5_corn")) { tp(p, 74.5, G1, 74.5, 180); enter("c5_corn"); return; }
        if (st.equals("c5_truck")) { spawnTruck(); enter("c5_truck"); return; }
        if (st.equals("intro") || st.equals("ending")) { enter("c1_gate"); return; }
        if (S.chapter >= 2) spawnTruck();
    }

    /** the thing asleep, hanging from the barn rafters */
    private static void sleeper() {
        MimicEntity e = spawnMimic(70.5, 6.2, 30.5, 180);
        if (e != null) e.sleep();
    }

    // ================================================================== tick
    static void tick(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        Vec3 l = lp(p);
        String room = room(p);
        int t = ++S.t;
        switch (S.step) {
            case "c1_gate" -> {
                if (l.z < 76 && l.x > 16 && l.x < 34) enter("c1_door");
                if (t == 600 && l.z > 96) think("fence");
            }
            case "c1_door" -> {
                if (room.startsWith("H_")) enter("c1_inside");
            }
            case "c1_inside" -> {
                if (t == 160 || (t > 60 && room.equals("H_HALL") && l.z < 61)) {
                    if (S.v("sub") == 0) {
                        S.v("sub", 1);
                        setMode("phone", 1);
                        snd("fx.phone_ring", 1f, 1f);
                        enter("c1_phone");
                    }
                }
            }
            case "c1_phone" -> {
                if (t % 40 == 1 && t < 140) sndAt(24.5, G1 + 1, 58.5, "fx.phone_ring", 1.2f, 1f);
                if (t == 60) startDialogue("phone", "phone");
            }
            case "c1_kitchen" -> {
                if (has("walkie")) enter("c1_radio");
                // up there, something walks across the floor
                if (t == 300 || t == 900) sndAt(19.5, F1, 64.5, "mm.step", 0.8f, 0.8f);
                if (t == 600) sndAt(25.5, F1, 55.5, "fx.creak", 1f, 0.8f);
            }
            case "c1_radio" -> {
                if (t == 300 && S.v("sub") == 0) hint("walkie");
                if (t % 300 == 150 && S.v("sub") == 0) snd("fx.radio_static", 0.6f, 1f);
            }
            case "c1_voice" -> {
                if (!inHouse(p) && t > 40) { enter("c2_yard"); return; }
                if (t == 100) {
                    voiceAt(25.5, F1 + 1, 58.5, "v.mlily.up1", "lily_q");
                    S.v("sub", 1);
                }
                if (t == 225 && S.v("sub") == 1) {
                    S.v("sub", 2);
                    startDialogue("upstairs", "upstairs");
                }
            }
            case "c1_up" -> {
                if (l.y > F1 - 0.5 && room.startsWith("U_")) {
                    MimicEntity e = spawnMimic(25.5, F1, 53.5, 0);
                    if (e != null) {
                        e.scriptStop();
                        e.setFlag(MimicEntity.F_CLIMB, true);
                    }
                    cutscene("landing");
                }
                if (t == 300) voiceAt(25.5, F1 + 1, 56.5, "v.mlily.up2", "lily_q");
            }
            case "c1_chase" -> {
                if (!inHouse(p) && !room.equals("PORCH") && (hm == null || !hm.sees(p) || hm.distanceTo(p) > 18)) {
                    S.v("sub", S.v("sub") + 1);
                    if (S.v("sub") > 60) {
                        if (hm != null) hm.vanish();
                        think("escaped");
                        enter("c2_yard");
                    }
                } else S.v("sub", 0);
            }
            case "c1_hide" -> tickHouseSearch(p, lvl, t);
            case "c1_sneak" -> tickHouseSneak(p, lvl, l, t);
            case "c2_yard" -> {
                if (room.equals("BARN") || room.equals("STALLS") || room.equals("WORKSHOP") || room.equals("SLAUGHTER")) {
                    cutscene("barn");
                }
            }
            case "c2_barn" -> tickBarn(p, l, room, t);
            case "c2_ladder" -> {
                if (l.y > L1 - 0.5 && l.x < 65 && l.z > 43.2) {
                    removeMimic();
                    cutscene("jump");
                }
                if (!room.startsWith("BARN") && !room.equals("STALLS") && !room.equals("LOFT") && !room.equals("WORKSHOP") && !room.equals("SLAUGHTER")
                        && (hm == null || hm.distanceTo(p) > 16)) {
                    if (inc("sub") > 60) {
                        if (hm != null) hm.vanish();
                        enter("c3_shed");
                    }
                }
            }
            case "c2_hay" -> {
                if (t == 30 && hm != null) {
                    hm.beginSearch(p.position());
                }
                if (t > 40 && (hm == null || !hm.isAlive())) enter("c3_shed");
                if (t > 1400) { removeMimic(); enter("c3_shed"); }
            }
            case "c2_still" -> {
                if (hm == null) return;
                if (t == 60) {
                    hm.scriptStop();
                    hm.face(p.position());
                    hm.act("sniff", 60, "mm.sniff");
                    fx("freeze", 140);
                }
                if (t == 120) hm.say("mm.breath", 1.2f, 0.7f);
                if (t == 150) hm.act("listen", 40, null);
                if (t == 205) {
                    hm.scriptMove(w(70.5, G1, 17.5), 1.8);
                    hm.setFlag(MimicEntity.F_CRAWL, true);
                }
                if (t == 300) {
                    removeMimic();
                    setDoor("barn_n", false);
                    sndAt(70.5, G1, 16.5, "fx.door_slam", 0.8f, 1f);
                    think("still_ok");
                    enter("c3_shed");
                }
            }
            case "c3_shed" -> {
                if (room.equals("SHED") && S.v("sub") == 0) {
                    S.v("sub", 1);
                    think("shed");
                    snd("amb.flies", 0.8f, 1f);
                }
                if (has("truck_keys") && S.v("beat") == 0) {
                    S.v("beat", 1);
                    laterIn("c3_shed", 60, () -> enter("c3_trap"));
                }
            }
            case "c3_throw", "c3_sneak", "c3_run" -> {
                if (nearLocal(p, 84, G1, 31, 7)) {
                    removeMimic();
                    enter("c4_silo");
                    return;
                }
                if (S.step.equals("c3_throw") && hm != null && hm.getState() == MimicEntity.SCRIPT && t > 80 && hm.sees(p) && hm.distanceTo(p) < 5) {
                    hm.setFlag(MimicEntity.F_EAT, false);
                    hm.beginHunt();
                    hm.act("roar", 24, "mm.roar");
                    hm.stateTicks = -22;
                }
                if (S.step.equals("c3_sneak") && hm != null && hm.getState() == MimicEntity.SCRIPT) {
                    if (t == 90) hm.act("sniff", 50, "mm.sniff");
                    if (t == 160) hm.beginSearch(w(44.5, G1, 22.5));
                }
            }
            case "c4_silo" -> {}
            case "c4_follow" -> {
                // the ambush comes on the way to the truck, whichever way you go
                boolean nearTruck = nearLocal(p, 46, G1, 46, 20);
                if (((l.x < 73 && l.x > 58 && l.z > 45 && l.z < 56) || nearTruck) && S.v("sub") == 0) {
                    S.v("sub", 1);
                    cutscene("ambush");
                }
            }
            case "c4_corn" -> {
                if (nearLocal(p, 46, G1, 46, 6)) {
                    removeMimic();
                    enter("c5_truck");
                }
            }
            case "c5_corn" -> {
                if (lily != null && hm != null && p.distanceTo(lily) < 12 && S.v("sub") == 0) {
                    S.v("sub", 1);
                    startDialogue("face", "face");
                }
                if (t % 160 == 80 && S.v("sub") == 0) voiceAt(76.5, G1 + 1, 85.5, "v.lily.field2", "lily");
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- chapter 1: it comes down the stairs
    private static void tickHouseSearch(ServerPlayer p, ServerLevel lvl, int t) {
        if (S.v("sub") == 9) {
            // it went wrong: the hunt AI owns it now; get out of the house
            if (!inHouse(p) && (hm == null || hm.distanceTo(p) > 16)) {
                if (inc("beat") > 60) {
                    if (hm != null) hm.vanish();
                    enter("c2_yard");
                }
            }
            return;
        }
        if (t == 40) {
            sndAt(25.5, F1, 56.5, "mm.step", 1f, 0.8f);
            MimicEntity e = spawnMimic(25.5, F1, 60.5, 180);
            if (e != null) e.scriptMove(w(25.5, G1, 67.5), 1.4);
        }
        if (hm == null) return;
        if (t > 60 && hiddenIn(p) == null && hm.sees(p) && hm.distanceTo(p) < 14) {
            S.v("sub", 9);
            hm.huntSpeed = 4.4;
            hm.huntPatience = 100;
            hm.beginHunt();
            hm.act("roar", 26, "mm.roar");
            hm.stateTicks = -24;
            obj("run_out");
            return;
        }
        if (t == 200) {
            hm.scriptStop();
            hm.act("listen", 50, "mm.click");
        }
        if (t == 260) {
            House.Hide hid = hiddenIn(p);
            hm.beginSearch(hid != null ? worldCenter(hid.pos) : w(30.5, G1, 64.5));
        }
    }

    private static final double[][] SNEAK_PATH = {{25.5, G1, 66.5}, {25.5, G1, 57.5}, {19.5, G1, 64.5}, {25.5, G1, 60.5}, {31.5, G1, 64.5}, {25.5, G1, 55.5}, {19.5, G1, 55.5}};

    private static void tickHouseSneak(ServerPlayer p, ServerLevel lvl, Vec3 l, int t) {
        if (S.v("sub") == 9) {
            if (!inHouse(p) && (hm == null || hm.distanceTo(p) > 16)) {
                if (inc("beat") > 60) {
                    if (hm != null) hm.vanish();
                    enter("c2_yard");
                }
            }
            return;
        }
        if (t == 60) {
            sndAt(25.5, F1, 60.5, "fx.creak", 1f, 0.8f);
            MimicEntity e = spawnMimic(25.5, F1, 60.5, 180);
            if (e != null) {
                e.scriptMove(w(25.5, G1, 67.5), 0.9);
                e.setFlag(MimicEntity.F_STALKWALK, true);
            }
        }
        if (hm == null) return;
        if (t > 80 && hm.sees(p) && hm.distanceTo(p) < 11) {
            S.v("sub", 9);
            hm.setFlag(MimicEntity.F_STALKWALK, false);
            hm.huntSpeed = 4.4;
            hm.huntPatience = 100;
            hm.beginHunt();
            hm.act("roar", 26, "mm.roar");
            hm.stateTicks = -24;
            obj("run_out");
            return;
        }
        // it walks the ground floor, stopping to listen
        if (t > 200 && t % 160 == 40) {
            int i = S.v("beat") % SNEAK_PATH.length;
            S.v("beat", S.v("beat") + 1);
            double[] wp = SNEAK_PATH[i];
            hm.scriptMove(w(wp[0], wp[1], wp[2]), 0.9);
        }
        if (t > 200 && t % 160 == 130) {
            hm.act(RND.nextBoolean() ? "listen" : "sniff", 40, RND.nextBoolean() ? "mm.click" : "mm.sniff");
            if (RND.nextInt(3) == 0) voiceAt(hm.getX() - S.ox, hm.getY() - S.oy + 2, hm.getZ() - S.oz, bait(), "it");
        }
        if (!inHouse(p) && !room(p).equals("PORCH") && hm.distanceTo(p) > 10) {
            if (inc("sub2") > 40) {
                S.vars.remove("sub2");
                hm.vanish();
                think("sneaked");
                enter("c2_yard");
            }
        }
    }

    // ---------------------------------------------------------------- chapter 2: do not wake it
    private static void tickBarn(ServerPlayer p, Vec3 l, String room, int t) {
        if (hm == null || !hm.isAlive()) { sleeper(); return; }
        if (hm.getState() != MimicEntity.SLEEP) return; // already awake: the hunt owns it
        int d = S.v("disturb");
        double dist = hm.distanceTo(p);
        int add = 0;
        if (S.lightOn && dist < 22 && Story.playerLooksAt(p, hm.position().add(0, 2.5, 0), 0.93)) add += 3;
        if (p.isSprinting() && dist < 18) add += 3;
        else if (!p.isCrouching() && p.getDeltaMovement().horizontalDistanceSqr() > 0.001 && dist < 9) add += 1;
        d = Math.max(0, Math.min(100, d + add - (add == 0 ? 1 : 0)));
        S.v("disturb", d);
        hud();
        if (d > 40 && t % 40 == 0) hm.act("twitch", 14, "mm.twitch");
        if (d > 70 && t % 60 == 30) hm.say("mm.breath", 0.9f, 0.8f);
        if (d >= 100) {
            // awake, and furious
            S.v("disturb", 0);
            hm.wake();
            hm.huntSpeed = 4.4;
            hm.huntPatience = 160;
            hm.beginHunt();
            hm.act("roar", 30, "mm.roar");
            hm.stateTicks = -28;
            enter("c2_ladder");
            obj("run");
            return;
        }
        if (has("bolt_cutters") && has("gas_can") && S.v("sub") == 0) {
            S.v("sub", 1);
            laterIn("c2_barn", 40, () -> enter("c2_wake"));
        }
    }

    // ================================================================== interaction
    /** @return true when the story handled (and so cancels) the door */
    static boolean onDoor(ServerPlayer p, String id) {
        switch (id) {
            case "front" -> {
                if (!S.f("front_open") && !has("house_key")) {
                    snd("fx.lock", 1f, 1f);
                    think("locked_front");
                    return true;
                }
                if (!S.f("front_open")) {
                    S.flags.add("front_open");
                    snd("fx.unlock", 1f, 1f);
                }
                return false;
            }
            case "storage" -> {
                snd("fx.lock", 1f, 0.8f);
                think("storage");
                return true;
            }
            case "silo" -> {
                if (S.f("silo_open")) return false;
                if (!S.step.equals("c4_silo")) {
                    snd("fx.lock", 1f, 1f);
                    think(has("bolt_cutters") ? "silo_later" : "silo_locked");
                    return true;
                }
                if (!has("bolt_cutters")) {
                    snd("fx.lock", 1f, 1f);
                    think("silo_locked");
                    return true;
                }
                snd("fx.bolt_cut", 1f, 1f);
                S.flags.add("padlock_cut");
                enter("c4_door");
                later(30, () -> {
                    sndAt(86.5, G1, 31.5, "fx.thump", 1f, 1f);
                    startDialogue("silo", "silo1");
                });
                return true;
            }
            case "shed" -> {
                if (S.chapter < 3) {
                    snd("fx.lock", 1f, 1f);
                    think("shed_locked");
                    return true;
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    static void onItem(ServerPlayer p, String id) {
        switch (id) {
            case "walkie" -> {
                if (S.step.equals("c1_kitchen")) enter("c1_radio");
            }
            case "crowbar" -> think("crowbar");
            case "lighter" -> think("lighter");
            case "pitchfork" -> think("pitchfork");
            case "gas_can" -> think("gas_can");
            case "meat" -> think("meat");
            default -> {}
        }
    }

    static void onUse(ServerPlayer p, String tag, BlockPos lpos) {
        ServerLevel lvl = p.serverLevel();
        switch (tag) {
            case "birdbox" -> {
                if (!S.opened.contains("birdbox")) {
                    S.opened.add("birdbox");
                    give("house_key");
                    think("key");
                } else think("birdbox_empty");
            }
            case "phone" -> {
                snd("fx.phone_pick", 1f, 1f);
                think(S.step.equals("c1_phone") ? "phone_now" : "phone_dead");
            }
            case "clock" -> {
                snd("fx.clock_chime", 1f, 1f);
                think("clock");
            }
            case "stove" -> {
                snd("fx.stove", 1f, 1f);
                if (!S.notes.contains("mother")) think("stove");
                else think("stove2");
            }
            case "sink" -> {
                snd("fx.water", 1f, 1f);
                think("sink");
            }
            case "hamradio" -> {
                setMode("hamradio", 1);
                snd("fx.radio_static", 1f, 1f);
                think(S.chapter >= 2 ? "hamradio2" : "hamradio");
                later(80, () -> setMode("hamradio", 0));
                noiseEvent(lpos, 14);
            }
            case "mirror" -> {
                if (!S.f("mirror_seen")) {
                    S.flags.add("mirror_seen");
                    fx("static", 12);
                    snd("fx.sting", 1f, 1f);
                    think("mirror");
                } else think("mirror2");
            }
            case "toolcab" -> {
                if (S.f("toolcab_open")) {
                    think("toolcab_empty");
                    return;
                }
                if (!has("crowbar")) {
                    snd("fx.lock", 1f, 1f);
                    think("toolcab_locked");
                    return;
                }
                S.flags.add("toolcab_open");
                snd("fx.crowbar", 1f, 1f);
                noiseEvent(lpos, 10);
                BlockPos wpos = wp(lpos);
                BlockState s = lvl.getBlockState(wpos);
                if (s.hasProperty(OpenableBlock.OPEN)) lvl.setBlock(wpos, s.setValue(OpenableBlock.OPEN, true), 3);
                give("bolt_cutters");
                if (S.step.equals("c2_barn")) S.v("disturb", Math.min(100, S.v("disturb") + 25));
            }
            case "kick" -> {
                snd("fx.bucket", 1.2f, 1f);
                noiseEvent(lpos, 18);
                if (S.step.equals("c2_barn")) S.v("disturb", Math.min(100, S.v("disturb") + 40));
            }
            case "bell" -> {
                snd("fx.bell", 1.4f, 1f);
                noiseEvent(lpos, 60);
                think("bell");
            }
            case "well" -> {
                later(30, () -> sndAt(lpos.getX() + 0.5, lpos.getY() - 8, lpos.getZ() + 0.5, "fx.well", 1f, 1f));
                think("well");
            }
            case "generator" -> {
                if (S.f("power")) { think("generator_on"); return; }
                if (!has("gas_can")) { think("generator_dry"); return; }
                think("generator_save");
            }
            case "tape" -> {
                if (S.f("tape_heard")) {
                    think("tape_again");
                    return;
                }
                setMode("tape", 1);
                snd("fx.tape", 1f, 1f);
                startDialogue("tape", "tape1");
            }
            case "coat" -> {
                if (!S.opened.contains("coat")) {
                    S.opened.add("coat");
                    snd("fx.gore", 1f, 1f);
                    give("truck_keys");
                    think("coat");
                } else think("coat2");
            }
            case "gate" -> think(S.chapter >= 5 ? "gate_truck" : "gate");
            default -> {
                if (tag.startsWith("look:")) think("look_" + tag.substring(5));
            }
        }
    }

    static void onHide(ServerPlayer p, String id, boolean in) {}

    /** right click on Lily */
    static void onLily(ServerPlayer p) {
        if (lily == null) return;
        if (S.step.equals("c4_follow")) {
            lily.playAction("hug");
            say("line.lily_stay", "lily", 70);
        }
    }

    /** right click on the truck */
    static void onTruck(ServerPlayer p) {
        if (!S.step.equals("c5_truck")) {
            snd("fx.truck_door", 1f, 1f);
            think(S.chapter < 3 ? "truck_early" : "truck_nokeys");
            return;
        }
        if (!S.f("fueled")) {
            if (!has("gas_can")) { think("truck_nogas"); return; }
            take("gas_can");
            S.flags.add("fueled");
            S.flags.add("gas_left");
            snd("fx.gas_pour", 1f, 1f);
            obj("start");
            return;
        }
        if (!has("truck_keys")) { think("truck_nokeys"); return; }
        snd("fx.engine_fail", 1f, 1f);
        later(50, () -> {
            snd("fx.engine_start", 1f, 1f);
            if (truck != null) truck.lights(true);
            cutscene("two");
        });
    }

    static void onMimicEvent(String ev) {
        switch (S.step) {
            case "c1_hide" -> {
                if (ev.equals("search_done")) {
                    if (hm != null) {
                        hm.scriptMove(w(25.5, G1, 71.5), 1.6);
                        setDoor("front", true);
                    }
                    laterIn("c1_hide", 140, () -> {
                        removeMimic();
                        setDoor("front", false);
                        sndAt(25.5, G1, 70.5, "fx.door_slam", 0.8f, 1f);
                        think("hide_ok");
                        enter("c2_yard");
                    });
                }
            }
            case "c2_hay" -> {
                if (ev.equals("search_done")) {
                    if (hm != null) {
                        hm.scriptMove(w(70.5, G1, 17.5), 1.6);
                        hm.setFlag(MimicEntity.F_CRAWL, true);
                    }
                    laterIn("c2_hay", 140, () -> {
                        removeMimic();
                        think("hay_ok");
                        if (hiddenIn(player()) != null) unhide(player());
                        enter("c3_shed");
                    });
                }
            }
            case "c3_sneak" -> {
                if (ev.equals("search_done") && hm != null) hm.vanish();
            }
            default -> {}
        }
    }

    static void onQte(String type, boolean ok) {
        if (type.equals("still") && S.step.equals("c2_still")) {
            if (!ok || S.lightOn) {
                ServerPlayer p = player();
                if (hm != null && p != null) {
                    hm.act("grab", 16, "mm.grab");
                    caught(p, hm);
                }
            }
        }
    }

    /** an extra beat for a dialogue node (plays what the other voices answer) */
    static void onNode(String node) {
        if (node.equals("two_ask")) {
            boolean learned = S.f("learned:word") || S.f("word_loud");
            voiceAt(47.5, G1 + 1, 46.5, learned ? "v.mlily.truck_ok" : "v.mlily.truck_bad", "lily_q");
            String fake = learned ? "v.mlily.truck_ok" : "v.mlily.truck_bad";
            later(voiceTicks(fake) + 10, () -> voiceAt(70.5, G1 + 1, 66.5, "v.lily.word", "lily"));
        }
    }

    static void onDialogueEnd(String id, String result, float delta) {
        ServerPlayer p = player();
        if (p == null) return;
        switch (id) {
            case "phone" -> {
                setMode("phone", 0);
                think(result.equals("ans") ? "phone_ans" : "phone_ring");
                enter("c1_kitchen");
            }
            case "radio1" -> {
                S.v("sub", 1);
                think("radio_done");
                laterIn("c1_radio", 120, () -> enter("c1_voice"));
            }
            case "upstairs" -> {
                switch (result) {
                    case "up" -> enter("c1_up");
                    case "ask" -> {
                        boolean knows = S.f("word_loud");
                        String ans = knows ? "v.mlily.word_ok" : "v.mlily.word_bad";
                        int t1 = 40, t2 = t1 + voiceTicks(ans) + 15, t3 = t2 + voiceTicks("v.mlily.up2") + 15;
                        later(t1, () -> voiceAt(25.5, F1 + 1, 58.5, ans, "lily_q"));
                        later(t2, () -> voiceAt(25.5, F1 + 1, 58.5, "v.mlily.up2", "lily_q"));
                        later(t3, () -> think("not_her"));
                        enter("c1_hide");
                    }
                    default -> enter("c1_sneak");
                }
            }
            case "wake" -> {
                switch (result) {
                    case "ladder" -> enter("c2_ladder");
                    case "hay" -> enter("c2_hay");
                    default -> enter("c2_still");
                }
            }
            case "trap" -> {
                switch (result) {
                    case "throw" -> enter("c3_throw");
                    case "sneak" -> enter("c3_sneak");
                    default -> enter("c3_run");
                }
            }
            case "tape" -> {
                S.flags.add("tape_heard");
                setMode("tape", 0);
                note("tape");
            }
            case "silo" -> cutscene("rescue");
            case "two" -> {
                if (result.equals("drive")) endingCut("passenger", "end_passenger");
                else enter("c5_corn");
            }
            case "face" -> {
                if (result.equals("face")) {
                    if (has("lighter") && S.f("gas_left")) {
                        cutscene("burn");
                    } else if (has("pitchfork")) {
                        qte("mash", 100);
                        S.v("sub", 2);
                    } else endingCut("learned", "end_learned");
                } else endingCut("harvest", "end_harvest");
            }
            default -> {}
        }
    }

    static void onQteMash(boolean ok) {
        if (!S.step.equals("c5_corn") || S.v("sub") != 2) return;
        if (ok) {
            snd("fx.stab", 1f, 1f);
            if (hm != null) {
                hm.act("scream", 30, "mm.scream");
                hm.setFlag(MimicEntity.F_DRAG, false);
            }
            S.flags.add("stabbed");
            endingCut("dawn", "end_dawn");
        } else endingCut("learned", "end_learned");
    }

    static void endingCut(String ending, String cut) {
        S.ending = ending;
        S.cpStep = "";
        S.step = "ending";
        obj("");
        cutscene(cut);
    }

    // ================================================================== cutscene cues
    static void cutCue(String id, int t) {
        ServerPlayer p = player();
        if (p == null) return;
        ServerLevel lvl = p.serverLevel();
        switch (id) {
            case "intro" -> {
                if (t == 10) snd("v.lily.vm1", 1f, 1f);
                if (t == 170) snd("v.lily.vm2", 1f, 1f);
                if (t == 310) snd("v.lily.vm3", 1f, 1f);
                if (t == 450) snd("v.lily.vm4", 1f, 1f);
                if (t == 160 || t == 300) sndAt(48, 3, 86, "fx.crow", 1.6f, 1f);
                if (t == 300) {
                    MimicEntity e = spawnMimic(70.5, 23.5, 40.5, 180);
                    if (e != null) {
                        e.scriptStop();
                        e.setFlag(MimicEntity.F_SPREAD, true);
                    }
                }
                if (t == 440) removeMimic();
                if (t == 590) snd("fx.thunder", 1f, 1f);
            }
            case "landing" -> {
                if (t == 60 && hm != null) {
                    hm.setFlag(MimicEntity.F_CLIMB, false);
                    hm.act("roar", 40, "mm.roar");
                }
                if (t == 70) fx("shake", 30);
            }
            case "barn" -> {
                if (t == 120) snd("mm.breath", 1f, 0.7f);
            }
            case "jump" -> {
                if (t == 30) {
                    tp(p, 62.5, 3, 49.5, 0);
                    snd("fx.hay", 1f, 1f);
                }
            }
            case "rescue" -> {
                if (t == 2) {
                    S.flags.add("silo_open");
                    unlock("silo");
                    setDoor("silo", true);
                    snd("fx.door_creak", 1f, 1f);
                    spawnLily(87.5, G1, 32.5, 90);
                }
                if (t == 60) snd("v.lily.silo3", 1f, 1f);
                if (t == 220) snd("v.lily.end2", 1f, 1f);
            }
            case "ambush" -> {
                if (t == 2) {
                    MimicEntity e = spawnMimic(70.5, G1, 45.5, 180);
                    if (e != null) e.scriptMove(w(67.5, G1, 51.0), 6.0);
                    if (lily != null) {
                        lily.follow(false);
                        lily.teleportTo(w(65.5, G1, 52.5).x, w(65.5, G1, 52.5).y, w(65.5, G1, 52.5).z);
                    }
                }
                if (t == 50) {
                    if (hm != null) hm.act("roar", 30, "mm.roar");
                    fx("shake", 40);
                }
                if (t == 70) {
                    if (lily != null) lily.playAction("scream");
                    snd("v.lily.field1", 1f, 1f);
                }
                if (t == 100) {
                    fx("jumpscare", 12);
                    snd("fx.thump", 1f, 0.7f);
                    if (lily != null) {
                        lily.follow(false);
                        lily.runTo(w(78.5, G1, 70.5));
                    }
                }
                if (t == 115) sndAt(78.5, G1 + 1, 70.5, "v.lily.corn", 1.4f, 1f);
            }
            case "two" -> {
                if (t == 20) voiceAt(47.5, G1 + 1, 46.5, "v.mlily.truck1", "lily_q");
                if (t == 140) voiceAt(70.5, G1 + 1, 66.5, "v.lily.field2", "lily");
            }
            case "burn" -> {
                if (t == 30) snd("fx.gas_pour", 1f, 1f);
                if (t == 90) snd("fx.lighter", 1f, 1f);
                if (t == 100) {
                    snd("fx.fire", 1.5f, 1f);
                    if (hm != null) {
                        hm.setFlag(MimicEntity.F_DRAG, false);
                        hm.setRemainingFireTicks(400);
                        hm.act("scream", 40, "mm.scream");
                    }
                    if (lily != null) lily.dragged(false);
                    burnCorn(lvl);
                }
                if (t == 160 && hm != null) hm.act("death", 80, "mm.howl");
                if (t == 210) snd("mm.scream", 1f, 0.6f);
            }
            case "end_dawn" -> {
                if (t == 1) {
                    lvl.setDayTime(23400);
                    S.flags.add("dawn");
                    removeMimic();
                    spawnTruck();
                    if (truck != null) {
                        truck.lights(true);
                        truck.driveTo(w(50.5, G1, 74.5), w(50.5, G1, 130.5));
                    }
                    setDoor("gate", true);
                }
                if (t == 20) snd("v.lily.end1", 1f, 1f);
            }
            case "end_harvest" -> {
                if (t == 1) {
                    if (lily != null) lily.dragged(true);
                    MimicEntity e = spawnMimic(72.5, G1, 82.5, 0);
                    if (e != null) {
                        e.scriptStop();
                        e.act("kill", 80, "mm.kill");
                    }
                }
                if (t == 20) snd("v.lily.field2", 1f, 1f);
                if (t == 120) {
                    removeMimic();
                    if (lily != null) lily.discard();
                    lily = null;
                    spawnTruck();
                    if (truck != null) {
                        truck.lights(true);
                        truck.driveTo(w(50.5, G1, 74.5), w(50.5, G1, 130.5));
                    }
                    setDoor("gate", true);
                }
                if (t == 150) snd("v.mlily.radio_end", 1f, 1f);
            }
            case "end_passenger" -> {
                if (t == 1) {
                    spawnTruck();
                    if (truck != null) {
                        truck.lights(true);
                        truck.driveTo(w(50.5, G1, 74.5), w(50.5, G1, 130.5));
                    }
                    setDoor("gate", true);
                }
                if (t == 140) {
                    if (truck != null) truck.discard();
                    truck = null;
                    spawnTruck();
                    MimicEntity e = spawnMimic(47.6, G1 - 0.3, 46.6, 0);
                    if (e != null) {
                        e.scriptStop();
                        e.setFlag(MimicEntity.F_SIT, true);
                        e.face(w(46.0, G1 + 1.4, 49.0));
                    }
                }
                if (t == 150) snd("v.mlily.truck_ok", 1f, 0.8f);
                if (t == 230 && hm != null) {
                    hm.act("scream", 40, "mm.scream");
                    snd("fx.jumpscare", 1f, 1f);
                    fx("jumpscare", 30);
                }
            }
            case "end_learned" -> {
                if (t == 1) {
                    snd("mm.kill", 1f, 1f);
                    fx("jumpscare", 30);
                }
                if (t == 100) snd("fx.phone_ring", 1f, 1f);
                if (t == 190) snd(S.f("learned:its_me") ? "v.msam.its_me" : S.f("learned:loud") ? "v.msam.loud" : "v.msam.ask", 1f, 1f);
            }
            default -> {}
        }
    }

    private static void burnCorn(ServerLevel lvl) {
        for (int x = 72; x <= 81; x++)
            for (int z = 82; z <= 90; z++) {
                BlockPos wpos = wp(new BlockPos(x, G1, z));
                if (lvl.getBlockState(wpos).isAir() && RND.nextInt(3) == 0) lvl.setBlock(wpos, Blocks.FIRE.defaultBlockState(), 3);
            }
    }

    static void onCutEnd(String id) {
        ServerPlayer p = player();
        switch (id) {
            case "intro" -> {
                removeMimic();
                enter("c1_gate");
            }
            case "landing" -> {
                if (hm != null) {
                    hm.huntSpeed = 4.6;
                    hm.huntPatience = 100;
                    hm.beginHunt();
                }
                checkpoint("c1_chase", 25.5, G1, 58.5, 0);
                S.step = "c1_chase";
                S.t = 0;
                obj("run_out");
            }
            case "barn" -> enter("c2_barn");
            case "jump" -> {
                think("jumped");
                enter("c3_shed");
            }
            case "rescue" -> enter("c4_follow");
            case "ambush" -> {
                removeMimic();
                enter("c4_corn");
            }
            case "two" -> startDialogue("two", "two");
            case "burn" -> endingCut("dawn", "end_dawn");
            case "end_dawn", "end_harvest", "end_passenger", "end_learned" -> {
                removeMimic();
                enter("credits");
            }
            default -> {}
        }
    }

    static void onNoteClosed(ServerPlayer p, String id) {
        if (id.equals("kitchen") && !S.f("kitchen_read")) {
            S.flags.add("kitchen_read");
            later(10, () -> think("kitchen_note"));
        }
    }

    static void onKeypad(ServerPlayer p, String code) {}

    static void onCreditsDone(ServerPlayer p) {
        if (!S.step.equals("credits")) return;
        String ending = S.ending;
        Story.stop(p, false);
        p.sendSystemMessage(Component.translatable("msg.mimic.done", Component.translatable("ending.mimic." + ending)));
    }

    // ================================================================== debug
    static void debugJump(ServerPlayer p, String id) {
        S.hasLight = true;
        S.battery = 100;
        if (!has("flashlight")) give("flashlight");
        int ch = id.length() > 1 && id.charAt(0) == 'c' ? id.charAt(1) - '0' : 0;
        if (ch >= 1 && !id.equals("c1_gate") && !id.equals("c1_door")) {
            S.flags.add("front_open");
            if (!has("house_key")) give("house_key");
        }
        if (ch >= 2 || id.equals("c1_voice") || id.startsWith("c1_up") || id.equals("c1_hide") || id.equals("c1_sneak")) {
            if (!has("walkie")) give("walkie");
        }
        if (ch >= 3) {
            if (!has("bolt_cutters")) give("bolt_cutters");
            if (!has("gas_can") && ch < 5) give("gas_can");
        }
        if (ch >= 4) {
            if (!has("truck_keys")) give("truck_keys");
        }
        if (ch >= 2) spawnTruck();
        double[] at = switch (id) {
            case "c1_gate" -> new double[]{50.5, 1, 102.5, 180};
            case "c1_door" -> new double[]{25.5, G1, 75.5, 180};
            case "c1_inside", "c1_phone" -> new double[]{25.5, G1, 66.5, 180};
            case "c1_kitchen", "c1_radio", "c1_voice", "c1_hide", "c1_sneak", "c1_up" -> new double[]{30.5, G1, 57.5, 180};
            case "c2_yard" -> new double[]{37.5, G1, 60.5, -90};
            case "c2_barn" -> new double[]{70.5, G1, 48.5, 180};
            case "c3_shed" -> new double[]{44.5, G1, 31.5, 180};
            case "c4_silo" -> new double[]{80.5, G1, 31.5, -90};
            case "c4_follow" -> new double[]{82.5, G1, 31.5, 90};
            case "c4_corn" -> new double[]{62.5, G1, 52.5, 90};
            case "c5_truck" -> new double[]{44.5, G1, 50.5, 180};
            case "c5_corn" -> new double[]{74.5, G1, 74.5, 180};
            default -> null;
        };
        if (at != null) tp(p, at[0], at[1], at[2], (float) at[3]);
        if (ch > 0 && ch != S.chapter) S.chapter = ch;
        refreshLamps(null);
        enter(id);
    }
}
