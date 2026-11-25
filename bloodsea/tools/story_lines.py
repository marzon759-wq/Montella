"""Every spoken line, choice and objective of the story, in English (spoken) and Turkish (subtitles). One source for
the voices (gen_story_audio.py), the language files and Lines.java.

Speakers: elias (the man on the raft), anna (his wife, in memory), voice (the thing in the fog wearing Anna's voice),
nurse, rescuer, man (a crewman in the storm)."""

LINES = {
    # ------------------------------------------------------------------ prologue: waking on the raft
    "wake.1": ("elias", "Where... where am I?", "Neredeyim... ben neredeyim?"),
    "wake.2": ("elias", "Is that... blood? All of it?", "Bu... kan mı? Hepsi mi?"),
    "wake.3": ("elias", "Hey. Hey, easy. Where did you come from?", "Hey. Hey, sakin ol. Sen nereden çıktın?"),
    "wake.4": ("elias", "You look at me like you know me.", "Bana beni tanıyormuşsun gibi bakıyorsun."),
    "wake.5": ("elias", "I'll call you Pip. Is that alright? Pip.", "Sana Pip diyeceğim. Olur mu? Pip."),
    "wake.6": ("elias", "A map. Someone drew this. Four places, and an X, right where we are.", "Bir harita. Biri bunu çizmiş. Dört yer, ve bir çarpı, tam bulunduğumuz yerde."),
    "wake.7": ("elias", "Okay. Okay. First things first. The oar.", "Tamam. Tamam. Önce en önemlisi. Kürek."),
    # ------------------------------------------------------------------ the rule
    "rule.1": ("elias", "Pip, get back from the edge!", "Pip, kenardan çekil!"),
    "rule.2": ("elias", "It ate it. Like acid.", "Onu yedi. Asit gibi."),
    "rule.3": ("elias", "Whatever happens, we never touch that water. Never.", "Ne olursa olsun, o suya asla dokunmuyoruz. Asla."),
    # ------------------------------------------------------------------ gameplay thoughts
    "think.hungry": ("elias", "My stomach is eating itself. There has to be something floating out here.", "Midem kendini yiyor. Burada yüzen bir şeyler olmalı."),
    "think.crate": ("elias", "Ship's biscuits. Somebody's cargo. Somebody's ship.", "Gemi peksimeti. Birinin yükü. Birinin gemisi."),
    "think.row": ("elias", "Sit at the oar. Row. Just row.", "Küreğe otur. Kürek çek. Sadece çek."),
    "think.pet": ("elias", "Good girl. Good girl, Pip.", "Aferin kızım. Aferin, Pip."),
    "think.night": ("elias", "It's so dark. Stay close, Pip.", "Çok karanlık. Yakınımda kal, Pip."),
    "think.north": ("elias", "The map shows islands to the north. Islands drawn like bones. Fine. North.", "Harita kuzeyde adalar gösteriyor. Kemik gibi çizilmiş adalar. Peki. Kuzey."),
    "think.east": ("elias", "That sound. Like a heart. It's coming from the east.", "O ses. Kalp gibi. Doğudan geliyor."),
    "think.west": ("elias", "There's a light in the west. A lighthouse? Out here?", "Batıda bir ışık var. Deniz feneri mi? Burada?"),
    "think.alone": ("elias", "It's so quiet without you.", "Sensiz her yer çok sessiz."),
    # ------------------------------------------------------------------ Pip's gift
    "gift.1": ("elias", "What have you got there?", "Ne getirdin bakalım?"),
    "gift.2": ("elias", "For me? You caught that, for me?", "Bana mı? Onu benim için mi yakaladın?"),
    "gift.3": ("elias", "Thank you, Pip.", "Teşekkür ederim, Pip."),
    "gift.share": ("elias", "Half for you, half for me. That's fair.", "Yarısı senin, yarısı benim. Adil olan bu."),
    "gift.keep": ("elias", "Sorry, little one. I need it more than you.", "Üzgünüm ufaklık. Benim daha çok ihtiyacım var."),
    # ------------------------------------------------------------------ the first night
    "moon.1": ("elias", "The moon. It's bleeding.", "Ay. Kanıyor."),
    "moon.2": ("elias", "Come here, Pip. Stay close to me tonight.", "Gel buraya, Pip. Bu gece yanımda kal."),
    "bump.1": ("elias", "Something hit us. Something under the raft.", "Bir şey çarptı. Salın altında bir şey var."),
    "bump.look": ("elias", "God. It's long. It's so long.", "Tanrım. Uzun. Çok uzun."),
    "bump.quiet": ("elias", "Shh. Shh. It'll go. It has to go.", "Şşş. Şşş. Gidecek. Gitmek zorunda."),
    "bump.shout": ("elias", "Get away from us! Get away!", "Uzak dur bizden! Uzak dur!"),
    "dawn.1": ("elias", "Morning. We made it through, Pip. We made it.", "Sabah oldu. Atlattık, Pip. Atlattık."),
    # ------------------------------------------------------------------ memory: the night he left
    "mem1.1": ("anna", "Elias, don't go out tonight. Look at the sky. It's wrong.", "Elias, bu gece çıkma. Gökyüzüne bak. Bir tuhaf."),
    "mem1.2": ("elias", "One more trip, Anna. The nets are already out.", "Bir sefer daha, Anna. Ağlar çoktan atıldı."),
    "mem1.3": ("anna", "Then take that cat of yours. She's been crying at the door all night.", "O zaman kedini de al. Bütün gece kapıda ağladı."),
    "mem1.4": ("elias", "Anna. Her name was Anna.", "Anna. Adı Anna'ydı."),
    "mem1.5": ("elias", "And you. You were with me. You were always with me.", "Ve sen. Sen benimleydin. Hep benimleydin."),
    # ------------------------------------------------------------------ the eye
    "eye.1": ("elias", "Pip. The sun.", "Pip. Güneş."),
    "eye.2": ("elias", "It's looking at us.", "Bize bakıyor."),
    "eye.away": ("elias", "Don't look at it. Just row.", "Ona bakma. Sadece çek."),
    "eye.stare": ("elias", "Go on, then. Look. I'm still here.", "Hadi o zaman. Bak. Ben hâlâ buradayım."),
    # ------------------------------------------------------------------ the Bone Reach
    "bones.1": ("elias", "Those aren't rocks.", "Bunlar kaya değil."),
    "bones.2": ("elias", "They're people. All of them. People.", "İnsanlar. Hepsi. İnsan."),
    "bones.3": ("elias", "Something's moving. Row. Row!", "Bir şey kıpırdıyor. Çek. Çek!"),
    "bones.after": ("elias", "We're alive. We're alive, Pip.", "Yaşıyoruz. Yaşıyoruz, Pip."),
    # ------------------------------------------------------------------ the Leviathan
    "lev.1": ("elias", "Oh God.", "Aman Tanrım."),
    "lev.2": ("elias", "Don't move, Pip. Don't. Move.", "Kıpırdama, Pip. Sakın. Kıpırdama."),
    "lev.look": ("elias", "It's not angry. It's... sad.", "Kızgın değil. Sanki... üzgün."),
    "lev.close": ("elias", "Please. Please, just go.", "Lütfen. Lütfen, git artık."),
    "lev.scream": ("elias", "Come on, then! Take us!", "Hadi o zaman! Al bizi!"),
    # ------------------------------------------------------------------ memory: the storm
    "mem2.1": ("man", "Elias! Grab the line! Grab the line!", "Elias! Halatı tut! Halatı tut!"),
    "mem2.2": ("elias", "Pip! Pip!", "Pip! Pip!"),
    "mem2.3": ("man", "She's going down! Get off the boat!", "Batıyor! Tekneden atla!"),
    "mem2.4": ("elias", "The storm. There was a storm.", "Fırtına. Bir fırtına vardı."),
    # ------------------------------------------------------------------ the voice in the fog
    "voice.1": ("voice", "Elias...", "Elias..."),
    "voice.2": ("voice", "Elias, it's me. I'm here. I'm right here.", "Elias, benim. Buradayım. Tam buradayım."),
    "voice.3": ("elias", "Anna?", "Anna?"),
    "voice.answer": ("elias", "Anna! Anna, I'm here!", "Anna! Anna, buradayım!"),
    "voice.silent": ("elias", "Don't answer. Don't.", "Cevap verme. Sakın."),
    "voice.real": ("elias", "You're not her. She's at home. You're not her.", "Sen o değilsin. O evde. Sen o değilsin."),
    # ------------------------------------------------------------------ Pip is taken
    "taken.1": ("voice", "Pip... Pip... come here, little one.", "Pip... Pip... gel buraya, ufaklık."),
    "taken.2": ("elias", "Pip? Pip, no. Come here. Come back to me.", "Pip? Pip, hayır. Gel buraya. Bana geri gel."),
    "taken.3": ("elias", "Pip!", "Pip!"),
    "taken.4": ("elias", "No. No, no, no...", "Hayır. Hayır, hayır, hayır..."),
    "taken.5": ("elias", "I'm sorry. I'm so sorry. I should have held you.", "Özür dilerim. Çok özür dilerim. Seni tutmalıydım."),
    "dest.hope": ("elias", "The light. If there's a light, there's someone. There has to be.", "Işık. Işık varsa biri vardır. Olmak zorunda."),
    "dest.accept": ("elias", "The bones. I know where I belong now.", "Kemikler. Artık nereye ait olduğumu biliyorum."),
    "dest.courage": ("elias", "The heartbeat. Whatever it is, I'm going through it.", "Kalp atışı. Ne olursa olsun, içinden geçeceğim."),
    "dest.fury": ("elias", "To the end of it. To the very end.", "Sonuna kadar. En sonuna kadar."),
    # ------------------------------------------------------------------ ending: the lighthouse, the hospital, the stomach
    "mirage.1": ("elias", "A lighthouse. A real lighthouse.", "Bir deniz feneri. Gerçek bir deniz feneri."),
    "mirage.2": ("elias", "Hello? Is anyone there? Hello!", "Merhaba? Kimse var mı? Merhaba!"),
    "mirage.3": ("elias", "The light... it's so bright... I can't...", "Işık... çok parlak... yapamıyorum..."),
    "hosp.1": ("nurse", "Mister Hart? Mister Hart, can you hear me?", "Bay Hart? Bay Hart, beni duyabiliyor musunuz?"),
    "hosp.2": ("elias", "Where... where am I?", "Neredeyim... neredeyim ben?"),
    "hosp.3": ("nurse", "Saint Mary's. A fishing boat found you three days ago. You're safe now.", "Saint Mary Hastanesi. Bir balıkçı teknesi sizi üç gün önce buldu. Artık güvendesiniz."),
    "hosp.4": ("elias", "Pip? Pip, you're here. You're alive.", "Pip? Pip, buradasın. Yaşıyorsun."),
    "hosp.5": ("nurse", "That cat hasn't left your bed since they brought you in.", "O kedi sizi getirdiklerinden beri yatağınızdan ayrılmadı."),
    "hosp.6": ("elias", "I dreamt there was a sea. A sea of blood. The sun had an eye. And something took you, Pip. It took you.", "Rüyamda bir deniz vardı. Kandan bir deniz. Güneşin bir gözü vardı. Ve bir şey seni aldı, Pip. Seni aldı."),
    "hosp.7": ("elias", "It was just a dream. Just a dream.", "Sadece bir rüyaydı. Sadece bir rüya."),
    "hosp.8": ("nurse", "Rest now. You're home.", "Şimdi dinlenin. Evdesiniz."),
    "stomach.1": ("elias", "We're home, Pip. We're home.", "Evdeyiz, Pip. Evdeyiz."),
    # ------------------------------------------------------------------ ending: the throne of bones
    "throne.1": ("elias", "I remember now.", "Şimdi hatırlıyorum."),
    "throne.2": ("elias", "No boat found me. There was only the water.", "Beni hiçbir tekne bulmadı. Sadece su vardı."),
    "throne.3": ("elias", "I never came home, Anna. I'm sorry.", "Hiç eve dönmedim, Anna. Özür dilerim."),
    "throne.4": ("elias", "Pip. Wait for me.", "Pip. Beni bekle."),
    # ------------------------------------------------------------------ ending: through the Leviathan
    "maw.1": ("elias", "Alright. Whatever you are. Take me through.", "Tamam. Her neysen. Geçir beni."),
    "rescue.1": ("rescuer", "There! Man in the water! Get the hook!", "Orada! Suda bir adam var! Kancayı getirin!"),
    "rescue.2": ("rescuer", "I've got you, I've got you! Breathe, mate. Breathe!", "Tuttum seni, tuttum! Nefes al dostum. Nefes al!"),
    "rescue.3": ("elias", "Pip... where's my cat...", "Pip... kedim nerede..."),
    "rescue.4": ("rescuer", "Your cat? She's on the hatch cover. Been yowling at us for an hour. Wouldn't let us leave.", "Kedin mi? Ambar kapağının üstünde. Bir saattir bize bağırıyor. Gitmemize izin vermedi."),
    "rescue.5": ("elias", "Pip. You kept me alive. You kept me alive.", "Pip. Beni sen hayatta tuttun. Beni sen hayatta tuttun."),
    # ------------------------------------------------------------------ ending: the edge
    "edge.1": ("elias", "This is where it ends.", "Burası bittiği yer."),
    "edge.2": ("elias", "If this is a dream, I'm done dreaming.", "Eğer bu bir rüyaysa, artık rüya görmeyeceğim."),
    "edge.3": ("elias", "Anna... Pip...", "Anna... Pip..."),
}

CHOICES = {
    "gift": [("share", "Share it with Pip", "Pip'le paylaş"), ("keep", "Eat it all yourself", "Hepsini kendin ye")],
    "bump": [("look", "Light the lantern and look", "Feneri yak ve bak"), ("quiet", "Hold Pip and stay quiet", "Pip'e sarıl, sessiz kal"),
             ("shout", "Shout at it", "Ona bağır")],
    "eye": [("away", "Look away and keep rowing", "Bakma, kürek çekmeye devam et"), ("stare", "Stare back at it", "Sen de ona bak")],
    "lev": [("look", "Look into its eye", "Gözünün içine bak"), ("close", "Close your eyes", "Gözlerini kapat"), ("scream", "Scream at it", "Ona haykır")],
    "voice": [("answer", "Answer her", "Ona cevap ver"), ("silent", "Stay silent", "Sessiz kal"), ("real", "Tell her she isn't real", "Gerçek olmadığını söyle")],
    "after": [("courage", "Keep going", "Devam et"), ("accept", "Let it end", "Bitsin artık"), ("fury", "Find it. Kill it.", "Onu bul. Öldür."),
              ("hope", "I can still hear her...", "Onu hâlâ duyabiliyorum...")],
}
QUESTIONS = {
    "gift": ("A fish. Pip is watching you.", "Bir balık. Pip sana bakıyor."),
    "bump": ("Something is under the raft.", "Salın altında bir şey var."),
    "eye": ("The sun is looking at you.", "Güneş sana bakıyor."),
    "lev": ("It is right in front of you.", "Tam karşında."),
    "voice": ("The voice is calling your name.", "Ses adını söylüyor."),
    "after": ("Pip is gone.", "Pip gitti."),
}

OBJECTIVES = {
    "oar": ("Sit at the oar at the back of the raft.", "Salın arkasındaki küreğe otur."),
    "food": ("You are starving. Row to a floating crate and pull it aboard.", "Açlıktan ölüyorsun. Yüzen bir sandığa kürek çek ve onu sala al."),
    "explore": ("Row. Look for anything.", "Kürek çek. Herhangi bir şey ara."),
    "night": ("Survive the night. Stay on the raft.", "Geceyi atlat. Salda kal."),
    "north": ("Row north, to the islands drawn like bones.", "Kuzeye, kemik gibi çizilmiş adalara kürek çek."),
    "escape": ("Get away from it!", "Ondan uzaklaş!"),
    "east": ("Follow the heartbeat east.", "Kalp atışını takip et, doğuya git."),
    "west": ("Row west, toward the light.", "Batıya, ışığa doğru kürek çek."),
    "dest_hope": ("Row to the lighthouse in the west.", "Batıdaki deniz fenerine kürek çek."),
    "dest_accept": ("Row north, to the throne of bones.", "Kuzeye, kemikten tahta kürek çek."),
    "dest_courage": ("Row east, into the heart of it. The Maw.", "Doğuya, kalbine doğru kürek çek. Ağız'a."),
    "dest_fury": ("Row south, to the end of the sea.", "Güneye, denizin sonuna kürek çek."),
}

ENDINGS = {
    "mirage": ("The Stomach", "Mide"),
    "bone": ("Acceptance", "Kabulleniş"),
    "leviathan": ("Return", "Dönüş"),
    "edge": ("The Fall", "Düşüş"),
}
ENDING_TEXT = {
    "mirage": [("Elias Hart was never found.", "Elias Hart hiç bulunamadı."),
               ("Something in the Blood Sea keeps what it eats alive, and dreaming.", "Kan Denizi'nde bir şey, yediklerini hayatta ve rüyada tutar."),
               ("He is still in the white room. Pip is still on his bed.", "O hâlâ beyaz odada. Pip hâlâ yatağında."),
               ("He is happy. That is the cruelest part.", "Mutlu. En acımasız kısmı da bu.")],
    "bone": [("The Bone Gatherer took one more.", "Kemik Toplayıcı bir kişiyi daha aldı."),
             ("Its crown has a new skull now, facing the empty sea.", "Tacında artık yeni bir kafatası var, boş denize bakan."),
             ("Somewhere, a woman still leaves a light on in the window.", "Bir yerlerde bir kadın hâlâ pencerede bir ışık yakık bırakıyor."),
             ("He stopped waiting for it.", "O ise artık beklemiyor.")],
    "leviathan": [("The trawler Margaret found him three days after the storm.", "Margaret adlı tekne onu fırtınadan üç gün sonra buldu."),
                  ("He had been floating on a hatch cover. The cat had kept him awake.", "Bir ambar kapağının üstünde sürükleniyordu. Kedi onu uyanık tutmuştu."),
                  ("He never told anyone about the red sea.", "Kızıl denizi kimseye anlatmadı."),
                  ("But he never let Pip out of his sight again.", "Ama Pip'i bir daha gözünün önünden ayırmadı.")],
    "edge": [("He is still falling.", "Hâlâ düşüyor."),
             ("The sea pours past him into a dark that has no bottom.", "Deniz yanından dibi olmayan bir karanlığa akıyor."),
             ("He has forgotten Anna's face. He is forgetting his own.", "Anna'nın yüzünü unuttu. Kendi yüzünü de unutuyor."),
             ("He is not dreaming any more. He is not anything.", "Artık rüya görmüyor. Artık hiçbir şey değil.")],
}
