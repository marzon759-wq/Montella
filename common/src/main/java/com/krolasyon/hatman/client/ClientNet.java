package com.krolasyon.hatman.client;

import com.krolasyon.hatman.net.Msg;
import com.krolasyon.hatman.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

public final class ClientNet {
    private ClientNet() {}

    static void ui(String sound, float pitch, float vol) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.get(sound), pitch, vol));
    }

    public static void handle(Msg m) {
        Minecraft mc = Minecraft.getInstance();
        switch (m.type) {
            case "hud" -> {
                boolean was = ClientState.active;
                ClientState.active = m.b("active");
                ClientState.objective = m.s("obj");
                ClientState.objArg = m.s("arg");
                ClientState.battery = m.f("bat");
                ClientState.nvOn = m.b("nv");
                ClientState.hasCam = m.b("cam");
                ClientState.notes = m.i("notes");
                ClientState.notesMax = m.i("notesMax");
                ClientState.ox = m.i("ox");
                ClientState.oy = m.i("oy");
                ClientState.oz = m.i("oz");
                ClientState.amb = m.s("amb");
                ClientState.chase = m.b("chase");
                ClientState.fear = m.f("fear");
                ClientState.hidden = m.b("hidden");
                ClientState.spare = m.i("spare");
                ClientState.power = m.b("power");
                ClientState.chapter = m.i("chapter");
                ClientState.fire = m.b("fire");
                ClientState.anger = m.f("anger");
                if (ClientState.playerName.isEmpty()) ClientState.playerName = mc.getUser().getName();
                if (!was && ClientState.active) Ambient.start();
            }
            case "objective" -> {
                ClientState.objFlash = 0;
                ui("fx.type", 0.8f, 0.7f);
            }
            case "line" -> {
                ClientState.lineKey = m.s("key");
                ClientState.lineSpeaker = m.s("speaker");
                ClientState.lineDur = Math.max(30, m.i("dur"));
                ClientState.lineT = 0;
            }
            case "chapter" -> {
                ClientState.chapterN = m.i("n");
                ClientState.chapterT = 0;
                ui("fx.chapter", 1f, 1f);
            }
            case "fx" -> {
                int n = m.i("n");
                switch (m.s("t")) {
                    case "static" -> ClientState.staticT = Math.max(ClientState.staticT, n);
                    case "shake" -> ClientState.shakeT = Math.max(ClientState.shakeT, n);
                    case "flash" -> {
                        ClientState.flashT = n;
                        if (mc.level != null) mc.level.setSkyFlashTime(n);
                    }
                    case "jumpscare" -> {
                        ClientState.jumpT = n;
                        ClientState.shakeT = n;
                    }
                    case "fadein" -> {
                        ClientState.fadeInT = n;
                        ClientState.fadeInMax = Math.max(1, n);
                    }
                    case "freeze" -> ClientState.freezeT = n;
                    case "slats" -> ClientState.slatsT = n;
                    default -> {}
                }
            }
            case "note" -> mc.setScreen(new NoteScreen(m.s("id")));
            case "dlg" -> {
                ClientState.playerName = m.s("pname");
                DialogueScreen.show(m);
            }
            case "dlg_end" -> {
                ClientState.dialogue = false;
                DialogueScreen.pending = null;
                if (ClientState.cutId == null) mc.options.hideGui = false;
                if (mc.screen instanceof DialogueScreen) mc.setScreen(null);
            }
            case "cut" -> {
                ClientState.ox = m.i("ox");
                ClientState.oy = m.i("oy");
                ClientState.oz = m.i("oz");
                ClientState.playerName = m.s("pname");
                mc.setScreen(new CutsceneScreen(m.s("id")));
            }
            case "cut_end" -> {
                if (mc.screen instanceof CutsceneScreen) mc.setScreen(null);
                CutsceneScreen.release();
            }
            case "hide" -> ClientState.hidden = m.b("on");
            case "hint" -> {
                ClientState.hintKey = m.s("key");
                ClientState.hintT = 0;
            }
            case "keypad" -> mc.setScreen(new KeypadScreen());
            case "credits" -> mc.setScreen(new CreditsScreen(m.s("ending"), m.i("notes"), m.i("notesMax"), m.i("caught"), m.i("lies")));
            case "end" -> {
                DialogueScreen.pending = null;
                mc.options.hideGui = false;
                ClientState.reset();
                CutsceneScreen.release();
            }
            default -> {}
        }
    }
}
