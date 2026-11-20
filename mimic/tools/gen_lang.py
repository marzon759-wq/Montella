#!/usr/bin/env python3
"""All the English text of MIMIC: Harrow Farm. Voice lines carry their subtitles here (sub.<sound>)."""
import json, os, re

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(HERE, "../common/src/main/resources/assets/mimicry")

# ---------------------------------------------------------------------------------------- the voices (spoken text = subtitle)
VOICES = {
    # Lily's voicemail
    "v.lily.vm1": "Sam? It's Lily. I know it's late. I know you said never to call. Please... please just come home.",
    "v.lily.vm2": "Something's wrong with Dad. He's not... it's not Dad anymore. It talks like him. It laughs like him.",
    "v.lily.vm3": "It came out of the old cellar in the summer. It's been learning. It listens to everything we say.",
    "v.lily.vm4": "Don't call the house. If it answers in my voice... hang up. I'm so sorry, Sam.",
    # the walkie-talkie
    "v.lily.r1": "Sam? Sam... is that you? Oh my god. You came.",
    "v.lily.r2": "Shh, not so loud! I'm in the silo. Dad locked me in before... before it got him. It can't get in here.",
    "v.lily.r3": "There's a padlock on the door. Dad's bolt cutters are in the barn workshop.",
    "v.lily.r4": "Sam, listen to me. It copies voices. It's been using Dad's for weeks. If anyone says they're me, ask for the word.",
    "v.lily.r5": "The word is butterscotch. Like Mom's candy.",
    "v.lily.r6": "And Sam... whatever you hear in the house tonight... don't go upstairs.",
    "v.lily.r7": "Sam? Did you get out of the house? Go to the barn. And be quiet in there. That's where it sleeps.",
    "v.lily.r8": "Sam? Sam, are you there? Did you find them?",
    "v.lily.r9": "Sam... the truck keys. Dad had them in his coat. He went to the shed that night... he never came back.",
    "v.lily.silo1": "Sam? Is that... is that you?",
    "v.lily.ask": "What's the word, Sam?",
    "v.lily.word": "Butterscotch. Like Mom's candy.",
    "v.lily.hurry": "...Okay. Okay. I believe you. Hurry.",
    "v.lily.silo2": "Oh god, Sam. Sam...",
    "v.lily.silo3": "I thought it was going to be you. Wearing your voice. I thought I'd never know.",
    "v.lily.end2": "Don't let go of my hand. Not once.",
    "v.lily.field1": "Sam! RUN!",
    "v.lily.field2": "Sam! SAM! It's got me! Please!",
    "v.lily.end1": "Is it... is it dead?",
    "v.lily.corn": "Sam! I'm in the corn! Get the truck started, I'll find you!",
    "v.lily.corn2": "Sam, I'm okay! I'm hiding in the corn. Start the truck. I'll come to you. Don't come looking for me.",
    # the thing wearing Lily
    "v.mlily.up1": "Sam? Sam, I'm up here! I got out! Come up, quick!",
    "v.mlily.up2": "Come up, Sammy.",
    "v.mlily.word_ok": "...Butterscotch.",
    "v.mlily.word_bad": "W-word? What word, Sam? Come up.",
    "v.mlily.truck1": "Sam! Get in! Drive, it's right behind you!",
    "v.mlily.truck_ok": "Butterscotch, Sammy! Just drive!",
    "v.mlily.truck_bad": "There's no time for this! Just drive!",
    "v.mlily.radio_end": "Sam? Sam, come back. It's me. It's really me.",
    "v.mlily.help": "Help me, Sam.",
    # Dad
    "v.dad.machine": "You've reached the Harrow farm. Leave a message after the beep.",
    "v.dad.tape1": "If you're hearing this, Sammy... then I'm dead, and you came home anyway. Stubborn. Like your mother.",
    "v.dad.tape2": "It came up out of the root cellar in July. Hungry. I fed it the calves. Then the cows. Then the men who came asking for work.",
    "v.dad.tape3": "It learns. Every word you say near it, it keeps. It talked to me in their voices. Now it talks in mine.",
    "v.dad.tape4": "Don't trust anything that sounds like family tonight. Not even me. Get your sister and burn it. Burn it all.",
    # the thing wearing Dad
    "v.mdad.phone1": "Sammy? That you, son? You came home.",
    "v.mdad.phone2": "Stay right there. I'm coming down.",
    "v.mdad.machine2": "Sammy. I know you're in the house.",
    "v.mdad.call1": "Sammy... come on out, son.",
    "v.mdad.call2": "It's alright. It's just Dad.",
    "v.mdad.call3": "I'm hungry, Sammy.",
    # you
    "v.sam.loud": "LILY! Where are you?!",
    "v.sam.whisper": "It's me... where are you?",
    "v.sam.whisper2": "...Okay.",
    "v.sam.word": "Butterscotch. Got it.",
    "v.sam.ask": "What's the word?!",
    "v.sam.its_me": "Lily, it's me. Open up.",
    "v.sam.lily": "Lily!",
    # the thing wearing you
    "v.msam.loud": "LILY! Where are you?!",
    "v.msam.word": "Butterscotch. Got it.",
    "v.msam.ask": "What's the word?",
    "v.msam.its_me": "Lily, it's me. Open up.",
}

T = {}


def put(k, v):
    T[k] = v


def main():
    put("itemGroup.mimicry.main", "MIMIC: Harrow Farm")
    put("entity.mimicry.mimic", "The Mimic")
    put("entity.mimicry.lily", "Lily Harrow")
    put("entity.mimicry.truck", "Pickup Truck")
    put("item.mimicry.mimic_spawn_egg", "Mimic Spawn Egg")
    put("key.categories.mimic", "MIMIC")
    put("key.mimic.light", "Flashlight")
    items = {"flashlight": "Flashlight", "battery": "D Battery", "house_key": "Spare House Key", "walkie": "Lily's Walkie-Talkie",
             "crowbar": "Crowbar", "bolt_cutters": "Bolt Cutters", "gas_can": "Gas Can", "truck_keys": "Dad's Truck Keys",
             "meat": "Raw Meat", "lighter": "Dad's Zippo", "pitchfork": "Pitchfork"}
    for k, v in items.items():
        put(f"item.mimicry.{k}", v)
    # blocks: the readable name of every block, from its id
    for f in sorted(os.listdir(os.path.join(A, "blockstates"))):
        bid = f[:-5]
        nice = bid.replace("_", " ").title()
        put(f"block.mimicry.{bid}", nice)
    for k, v in {"farm_door": "Farmhouse Door", "barn_door": "Barn Door", "farm_ladder": "Old Ladder", "barbed_fence": "Barbed Wire Fence",
                 "window_lit": "Lit Window", "corpse_head": "Body", "corpse_legs": "Body", "drifter": "Remains", "hanging_body": "Hanging Body",
                 "carcass": "Carcass", "walkie": "Walkie-Talkie", "birdbox": "Bird Box"}.items():
        put(f"block.mimicry.{k}", v)

    # ------------------------------------------------------------------ messages
    msgs = {
        "busy": "Someone else is already playing the story in this world.",
        "building": "Harrow Farm is being built around you...",
        "not_running": "No story is running.",
        "stopped": "The story was stopped. The farm stays where it is.",
        "done": "Thank you for playing MIMIC. Your ending: %s",
        "picked": "Picked up: %s",
        "locked": "Locked.",
        "drawer_empty": "Nothing else in here.",
        "drawer_junk0": "Rags, and old receipts for feed.",
        "drawer_junk1": "Candle stubs. Matches, all burnt.",
        "drawer_junk2": "Rope. A lot of rope.",
        "drawer_junk3": "Mom's sewing things. Untouched for years.",
        "drawer_junk4": "Shotgun shells. No shotgun.",
        "drawer_junk5": "A child's drawing of the barn. Something on the roof.",
        "hide_hint": "Hiding. Hold SPACE to hold your breath when it comes close. Shift to get out.",
        "hide_exit": "SHIFT: get out",
        "light_dead": "The flashlight is dead. Find batteries.",
        "battery_swap": "Fresh battery.",
        "battery_full": "The battery is still good.",
        "walkie_static": "Only static. And something breathing, very far away.",
        "threw_meat": "You throw the meat into the dark.",
        "item_house_key": "The spare key to the farmhouse.",
        "item_crowbar": "Heavy. It will pry open the tool cabinet - and anything nailed shut.",
        "item_bolt_cutters": "For the silo padlock. And the chain on the gate.",
        "item_gas_can": "Half full. Enough for the truck.",
        "item_truck_keys": "Dad's keys. There's blood dried in the teeth.",
        "item_lighter": "Dad's Zippo. It still lights.",
        "item_pitchfork": "If it gets hold of you, this is all you have.",
    }
    for k, v in msgs.items():
        put(f"msg.mimic.{k}", v)

    # ------------------------------------------------------------------ inner voice
    lines = {
        "gate_start": "Chained. Dad never chained the gate. There's a gap in the fence further along.",
        "fence": "The gate's chained. The fence is broken to the right of it.",
        "porch": "Locked. Dad always kept a spare in the old bird box by the steps.",
        "locked_front": "Locked. The spare key... the bird box by the steps.",
        "key": "Still here. Some things don't change.",
        "birdbox_empty": "Empty now.",
        "inside": "It smells like meat left out in the sun. Lily? ...Lily?",
        "phone_ans": "That was Dad's voice. Dad's been dead for three weeks. Lily said so.",
        "phone_ring": "Dad's voice. On the machine. Recorded tonight.",
        "phone_now": "It's ringing.",
        "phone_dead": "The line is dead. It's been dead all night.",
        "kitchen_note": "Lily's handwriting. Channel three.",
        "radio_done": "The silo. Bolt cutters, barn workshop. And... don't go upstairs.",
        "not_her": "That's not her. That is not her voice. It's coming down.",
        "escaped": "It stopped at the door. It's letting me go. Why is it letting me go?",
        "hide_ok": "It went outside. Toward the barn.",
        "sneaked": "It didn't hear me. Barn. Now.",
        "still_ok": "It smelled me. It was right there, and it smelled me. And it left.",
        "hay_ok": "It's gone. Go. Go now.",
        "jumped": "Get up. Keep moving. The shed is behind the barn.",
        "shed": "Oh god. Oh god, the smell. Don't look. Find the coat.",
        "coat": "Dad. I'm so sorry, Dad. ...The keys are in his pocket.",
        "coat2": "I can't look at him again.",
        "fork_found": "Dad's pitchfork, stuck in the dirt by the scarecrow. It'll have to do.",
        "bare_hands": "Not with my bare hands. The pitchfork.",
        "crowbar": "That'll open the tool cabinet in the workshop.",
        "lighter": "Dad's Zippo. It still sparks.",
        "pitchfork": "If it grabs me... at least I can make it let go.",
        "gas_can": "Gas. The truck will need it.",
        "meat": "It's hungry all the time. Lily said so.",
        "toolcab_locked": "Padlocked. I need something to pry it.",
        "toolcab_empty": "Empty now.",
        "storage": "Nailed shut. Someone wrote 'DON'T' on the boards. A crowbar would get them off.",
        "storage_open": "The boards come away with a shriek. Everything for a mile heard that.",
        "storage_later": "Not now. Lily first.",
        "silo_locked": "Padlocked. I need the bolt cutters from the barn.",
        "silo_later": "Not yet. She said keys first, then come for her.",
        "shed_locked": "Dad's shed. Something is buzzing in there. Flies. Thousands of them.",
        "clock": "It stopped at 3:12. Every clock in the house stopped at 3:12.",
        "stove": "Still warm. Someone has been burning paper in here. Photographs.",
        "stove2": "Ash, and a melted wedding ring.",
        "sink": "The water runs red, then brown, then clear.",
        "hamradio": "Static. Under the static, someone is repeating my name.",
        "hamradio2": "A weather report from the county. 'Clear skies by morning.'",
        "mirror": "For a second, there were two of me in the mirror.",
        "mirror2": "Just me. I look like Dad.",
        "bell": "Stupid. Stupid! Everything for a mile heard that.",
        "well": "A long, long time before the splash.",
        "generator_on": "It's running.",
        "generator_dry": "Out of gas.",
        "generator_save": "I need that gas for the truck.",
        "tape_again": "I can't listen to it again.",
        "gate": "Chained shut. The truck will go through it, or I'll cut it.",
        "gate_truck": "The truck can go through the gate.",
        "truck_early": "Dad's truck. No keys, no gas. Not yet.",
        "truck_nokeys": "The keys. Dad had the keys.",
        "truck_nogas": "Bone dry. I need gas.",
        "lily_stay": "Stay close to me.",
        "not_yet_lily": "Not now. Lily first. Find Lily.",
        "not_yet": "Not yet. One thing at a time.",
        "caught0": "...Again. Do it again. Quieter.",
        "caught1": "Not like that. Think.",
        "caught2": "Keep the light off it. Keep low.",
        "caught3": "It hears everything. Everything.",
        "look_portrait": "Grandpa Harrow. Somebody scratched his eyes out.",
        "look_family": "Our family. Mom's face has been burned off the photograph.",
        "look_books": "Almanacs. A Bible with half the pages torn out.",
        "look_stove": "Pots of something that boiled dry a long time ago.",
        "look_antlers": "Dad's first buck. The antlers are missing a tine. I know where it went.",
        "look_tub": "Don't look in the tub. Don't.",
        "look_saddle": "Mom's saddle. Nobody's ridden since she died.",
        "look_drifter": "He's been here a long time. He has no hands.",
        "look_scarecrow": "Bones, antlers and a feed sack. It made these. It's making a herd.",
        "look_scarecrow_half": "An unfinished one. Fresh.",
        "look_mailbox": "HARROW. The flag is up. Nobody has collected the mail since August.",
    }
    for k, v in lines.items():
        put(f"line.{k}", v)

    # ------------------------------------------------------------------ objectives
    objs = {
        "house": "Get to the farmhouse",
        "door": "Find a way into the house",
        "look": "Find Lily",
        "kitchen": "Find Lily. Search the house",
        "radio": "Use Lily's walkie-talkie",
        "upstairs": "Go upstairs",
        "run_out": "GET OUT OF THE HOUSE",
        "hide": "Hide",
        "sneak_out": "Get out of the house without being seen",
        "barn": "Get the bolt cutters from the barn workshop",
        "cutters": "Find the bolt cutters and the gas can. Don't wake it",
        "loft_door": "The hayloft door - jump",
        "stay_hidden": "Stay hidden. Hold your breath",
        "dont_move": "Don't move",
        "run": "RUN",
        "shed": "Find Dad's truck keys in the shed: search his coat",
        "fight": "It let go of Lily. Stab it with the pitchfork",
        "slip_away": "Get to the silo",
        "run_silo": "RUN to the silo",
        "padlock": "Cut the padlock on the silo",
        "truck": "Get Lily to the truck",
        "truck_alone": "Get to the truck",
        "fuel": "Fuel the truck",
        "start": "Start the truck",
        "lily_corn": "Find Lily in the corn",
    }
    for k, v in objs.items():
        put(f"obj.mimic.{k}", v)

    hints = {
        "fight": "Left-click to stab it. When it rears back with its hands open, step aside. If it gets you, hammer SPACE.",
        "run": "Running takes your breath. Out of breath you can't run - and you pant. It can hear that.",
        "light": "Press %s for the flashlight. It sees the light.",
        "walkie": "Right-click with the walkie-talkie in your hand to call Lily.",
        "hide": "Wardrobes and hay piles: right-click to hide. Hold SPACE to hold your breath when it is close.",
        "crouch": "Crouch to move quietly. It hunts by sound.",
    }
    for k, v in hints.items():
        put(f"hint.mimic.{k}", v)

    hud = {"objective": "OBJECTIVE", "notes": "Notes: %s/%s", "chapter": "CHAPTER %s", "skip": "Hold SPACE to skip",
           "close": "click to close", "page": "page %s of %s - click", "cover": "in the corn",
           "act.take": "Right-click: take %s", "act.search": "Right-click: search", "act.read": "Right-click: read", "act.hide": "Right-click: hide",
           "act.look": "Right-click: look", "act.open": "Right-click: open", "act.coat": "Right-click: search Dad's coat", "act.tape": "Right-click: play the tape",
           "act.use": "Right-click: use"}
    for k, v in hud.items():
        put(f"hud.mimic.{k}", v)
    put("qte.mimic.breath", "HOLD [SPACE] - HOLD YOUR BREATH")
    put("qte.mimic.still", "DON'T MOVE")
    put("qte.mimic.mash", "PRESS [SPACE] - FIGHT!")

    chapters = {1: ("Homecoming", "Harrow Farm, Iowa. 3:12 a.m."), 2: ("The Barn", "It sleeps where the cattle used to."),
                3: ("The Butcher's Shed", "Dad's last night."), 4: ("The Silo", "Three days in the dark."), 5: ("Dawn", "Two voices. One sister.")}
    for n, (a, b) in chapters.items():
        put(f"chapter.mimic.{n}", a)
        put(f"chapter.mimic.{n}.sub", b)

    speakers = {"you": "", "narr": "", "lily": "LILY", "lily_q": "LILY?", "dad": "DAD", "machine": "ANSWERING MACHINE", "it": "???"}
    for k, v in speakers.items():
        put(f"speaker.mimic.{k}", v)

    # ------------------------------------------------------------------ dialogue
    dlg = {
        "phone": "The phone is ringing. Nobody has paid that bill since August.",
        "phone.0": "Answer",
        "phone.1": "Let it ring",
        "upstairs": "Upstairs. Lily's voice.",
        "upstairs.0": "Go up",
        "upstairs.1": "Ask for the word",
        "upstairs.2": "Stay quiet",
        "wake": "It's awake.",
        "wake.0": "Run for the ladder",
        "wake.1": "Hide in the hay",
        "wake.2": "Don't move",
        "trap": "It's at the door. Eating.",
        "trap.0": "Throw the meat",
        "trap.1": "Sneak past",
        "trap.2": "Run",
        "two": "The truck - or the corn?",
        "two.0": "Drive",
        "two.1": "Into the corn",
        "two.2": "Ask for the word",
        "two_ask": "They both said it.",
        "two_ask.0": "Drive",
        "two_ask.1": "Into the corn",
        "face": "It has her by the ankles.",
        "face.0": "Face it",
        "face.1": "Run with Lily",
        "r1.0": "(whisper) It's me. Where are you?",
        "r1.1": "(shout) LILY! Where are you?!",
        "r5.0": "(say it back) Butterscotch. Got it.",
        "r5.1": "(whisper) ...Okay.",
        "r5.2": "(click the radio twice)",
        "silo1.0": "Lily, it's me. Open up.",
        "silo1.1": "What's the word?",
        "silo_ask.0": "Butterscotch.",
        "silo_ask.1": "I... don't remember.",
    }
    for k, v in dlg.items():
        put(f"dlg.{k}", v)
    for snd, text in VOICES.items():
        put(f"sub.{snd}", text)
    node_voice = {"ans1": "v.mdad.phone1", "ans2": "v.mdad.phone2", "ring1": "v.dad.machine", "ring2": "v.mdad.machine2",
                  "r1": "v.lily.r1", "r2": "v.lily.r2", "r2s": "v.lily.r2", "r3": "v.lily.r3", "r4": "v.lily.r4", "r5": "v.lily.r5",
                  "r6": "v.lily.r6", "tape1": "v.dad.tape1", "tape2": "v.dad.tape2", "tape3": "v.dad.tape3", "tape4": "v.dad.tape4",
                  "silo1": "v.lily.silo1", "silo_ask": "v.lily.ask", "silo_word": "v.lily.word", "silo_bad": "v.lily.hurry", "silo_ok": "v.lily.silo2"}
    for node, snd in node_voice.items():
        put(f"dlg.{node}", VOICES[snd])

    # ------------------------------------------------------------------ cutscenes
    cuts = {
        "intro": [VOICES["v.lily.vm1"], VOICES["v.lily.vm2"], VOICES["v.lily.vm3"], VOICES["v.lily.vm4"], "Three weeks after that message, I finally came home."],
        "landing": ["That's not Lily."],
        "barn": ["There. In the rafters.", "Keep the light off it. Keep low. Don't make a sound."],
        "jump": ["Jump!"],
        "rescue": [VOICES["v.lily.silo3"], "I'm here. I've got you.", VOICES["v.lily.end2"]],
        "ambush": [VOICES["v.lily.field1"], "Sam! I'm in the corn! Get the truck started, I'll find you!"],
        "two": [VOICES["v.mlily.truck1"], VOICES["v.lily.field2"]],
        "burn": ["Hey! HEY! Look at me!", "It's burning... it's burning!"],
        "end_dawn": [VOICES["v.lily.end1"], "It doesn't matter. We're never coming back.", "Sam... it's morning.",
                     "Don't look back, Lily. Don't ever look back.", "Sam? ...What's the word?", "...Butterscotch, Lily. It's me."],
        "end_harvest": [VOICES["v.lily.field2"], "I'm sorry. I'm so sorry, Lily.", VOICES["v.mlily.radio_end"], VOICES["v.mlily.help"]],
        "end_passenger": ["Lily? ...Say something. Please.", "Sammy.", "...Butterscotch."],
        "end_learned": ["It never stops learning.", "Somewhere, a phone is ringing. And it is using my voice.", VOICES["v.msam.its_me"]],
    }
    for cid, ls in cuts.items():
        for i, l in enumerate(ls):
            put(f"cut.{cid}.{i}", l)
    put("cut.ambush.1", "Sam! I'm in the corn! Get the truck started, I'll find you!")

    endings = {"dawn": "Dawn", "harvest": "The Harvest", "passenger": "Passenger", "learned": "Learned"}
    for k, v in endings.items():
        put(f"ending.mimic.{k}", v)
    epi = {
        "dawn": ["The sun came up over Harrow Farm for the first time in weeks.", "Lily slept the whole drive to the interstate, her hand in yours.",
                 "Behind you, the corn burned until noon.", "Sometimes, at night, she still asks you for the word. You always know it."],
        "harvest": ["You drove until the tank ran dry.", "The walkie-talkie on the passenger seat never stopped talking.",
                    "It sounds exactly like her. It knows everything she knew.", "You never turned it off."],
        "passenger": ["You made it to the highway before it spoke again.", "'Sammy,' it said. Only Dad ever called you that.",
                      "Back in the corn, Lily waited for a truck that never came back.", "It is learning to drive."],
        "learned": ["They found the truck at the gate, keys in the ignition.", "They never found you.",
                    "Two weeks later, your best friend got a voicemail in your voice.", "'Please... please just come home.'"],
    }
    for k, ls in epi.items():
        for i, l in enumerate(ls):
            put(f"ending.mimic.{k}.{i}", l)
    credits = ["MIMIC", "Harrow Farm", "Story, farm, creature and code", "krolasyon",
               "Voices and sound", "krolasyon", "Thank you for playing", "It is still listening.", "Every word you said tonight, it kept."]
    for i, c in enumerate(credits):
        put(f"credits.mimic.{i}", c)
    put("credits.mimic.stats", "Notes found: %s/%s    Times caught: %s    Words it learned from you: %s")
    put("credits.mimic.ending", "Ending: %s")

    # ------------------------------------------------------------------ notes
    notes = {
        "kitchen": ("A note in Lily's handwriting", "Sam -\n\nIf you're reading this you came. I'm in the silo. Dad locked me in there before it got him, it's the only place with a steel door.\n\nThe walkie is on channel 3. TALK QUIET. It listens. It always listens.\n\nIf you hear me anywhere else, it is NOT me.\n\n- L"),
        "clipping": ("Clipping, Cass County Ledger", "THIRD FARMHAND MISSING THIS SUMMER\n\nSheriff's deputies are asking for information about Dale Pruitt, 34, last seen hitchhiking toward the Harrow place on Route 9, where he had been promised a week's work.\n\nPruitt is the third transient laborer reported missing in the county since July. Walter Harrow told deputies that no one by that name had ever arrived."),
        "journal1": ("Dad's journal - July", "July 9. Found the root cellar wall pushed in from the other side. Something has been living in the dirt under the house. Lost two calves this week.\n\nJuly 14. I saw it. God help me, I saw it. Arms. Too many arms.\n\nJuly 15. It spoke to me tonight in Martha's voice. Martha has been dead six years. It said my name the way she used to."),
        "journal2": ("Dad's journal - August", "Aug 2. It only stays away when it's fed. The cattle are gone. I have done something I can never take back.\n\nAug 20. Lily asked who the man in the barn was. I told her a hired hand. She is not stupid.\n\nAug 30. It sits in the rafters and repeats things. Things I said in this barn twenty years ago. It has been listening to this family a long time."),
        "sheriff": ("Letter from the Sheriff's Office", "Mr. Harrow,\n\nThis office has received a third complaint regarding the smell from your property and the condition of your livestock fences.\n\nA deputy will visit on the 4th. Please ensure the gate is open.\n\nSheriff R. Doyle\n\n(Across the bottom, in Dad's hand: 'Doyle came. Doyle didn't leave.')"),
        "ledger": ("Feed ledger", "AUG\n 3 - 2 calves\n 6 - heifer\n 9 - heifer\n12 - Pruitt\n15 - the Mexican boy (didn't get his name)\n18 - 3 hogs\n21 - DOYLE\n24 - nothing left in the pens\n27 - it was in the house tonight\n\nSEPT\n 1 - it wants the girl"),
        "diary": ("Lily's diary", "Dad stopped talking at dinner. He just listens. When I talk he moves his lips with me, like he's practicing.\n\nI don't think that's Dad anymore. I think Dad has been gone for a while.\n\nI called Sam today. I left a message. He won't come. Why would he come, after everything.\n\nIf he comes I need to tell him about the word."),
        "drawing": ("A drawing, crayon on feed sack", "A tall black shape on the barn roof with six long arms.\n\nUnderneath, a child's handwriting, years old: 'THE MAN WHO LIVES UNDER THE HOUSE'.\n\nAnd underneath that, newer, in Lily's grown-up hand: 'I drew this when I was six. Nobody believed me.'"),
        "funeral": ("Funeral card", "In loving memory of\nMARTHA ANNE HARROW\n1951 - 2018\n\n'And the dead shall be raised incorruptible.'\n\nOn the back, in pencil: 'It wore her voice to the funeral. I heard it in the cornfield the whole service.'"),
        "mother": ("A letter from Mom, never sent", "Walt,\n\nI keep hearing the children calling me from the field at night. Both of them, together, in perfect unison.\n\nThe children are asleep in their beds when it happens. I have checked.\n\nPlease let's sell the farm. Please. Before it learns all of us."),
        "cellar": ("Scrawled on the cellar wall", "IT CAME FROM HERE\n\nThe earth is pushed out of a hole in the wall, big enough for a man to crawl through. The edges are scored with long, parallel claw marks.\n\nSomeone has nailed a crucifix over the hole. Upside down. Or it fell."),
        "silo": ("Lily's tally marks", "Three rows of marks scratched into the steel. Days.\n\nUnder them: 'it walked around the silo all night saying my name in Sam's voice. sam has never been here. how does it know his voice.'\n\n'oh god. the voicemail. it heard me record the voicemail.'"),
        "butcher": ("Dad's last list", "IF I DON'T COME BACK\n- truck keys in my coat\n- gas in the barn\n- the zippo is in the dining room cabinet\n- it hates fire. it hates fire more than anything\n- don't open the silo for anyone who doesn't know the word\n- I'm sorry, Lily.\n- I'm sorry, Sam."),
        "vet": ("Veterinary invoice", "HARROW, W.\nCall-out, after hours: 1 heifer, 'mauled'\n\nVet's note: 'Not a coyote. Not a bear. Bite radius I have no animal for. Recommend you contact Fish & Wildlife.'\n\n(Unpaid.)"),
        "tape": ("Dad's tape (transcript)", "\"If you're hearing this, Sammy... then I'm dead, and you came home anyway. Stubborn. Like your mother.\n\nIt came up out of the root cellar in July. Hungry. I fed it the calves. Then the cows. Then the men who came asking for work.\n\nIt learns. Every word you say near it, it keeps. It talked to me in their voices. Now it talks in mine.\n\nDon't trust anything that sounds like family tonight. Not even me. Get your sister and burn it. Burn it all.\""),
    }
    for k, (title, body) in notes.items():
        put(f"note.mimic.{k}.title", title)
        put(f"note.mimic.{k}.body", body)

    os.makedirs(os.path.join(A, "lang"), exist_ok=True)
    json.dump(T, open(os.path.join(A, "lang/en_us.json"), "w"), indent=1, ensure_ascii=False)
    from lang_tr import TR
    json.dump({k: TR.get(k, v) for k, v in T.items()}, open(os.path.join(A, "lang/tr_tr.json"), "w"), indent=1, ensure_ascii=False)
    print("lang", len(T))


if __name__ == "__main__":
    main()
