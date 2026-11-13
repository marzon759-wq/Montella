# -*- coding: utf-8 -*-
"""Story text: chapters, objectives, inner voice, cutscenes, dialogue. key -> (English, Turkish)"""

L = {}


def add(key, en, tr):
    L[key] = (en, tr)


# ------------------------------------------------------------------ chapters
add("chapter.hatman.1", "KNOCK FIRST", "ÖNCE KAPIYI ÇAL")
add("chapter.hatman.1.sub", "Marlowe House — 11:52 PM", "Marlowe Köşkü — 23:52")
add("chapter.hatman.2", "THE HATTER'S LEDGER", "ŞAPKACININ DEFTERİ")
add("chapter.hatman.2.sub", "Every order is written down. Every debt.", "Her sipariş yazılır. Her borç da.")
add("chapter.hatman.3", "MASQUERADE", "MASKELİ BALO")
add("chapter.hatman.3.sub", "The party never ended. Nobody was allowed to leave.", "Parti hiç bitmedi. Kimsenin gitmesine izin verilmedi.")
add("chapter.hatman.4", "THE FITTING ROOM", "PROVA ODASI")
add("chapter.hatman.4.sub", "He has been taking your measurements all night.", "Bütün gece ölçünü alıyordu.")
add("chapter.hatman.5", "LAST FITTING", "SON PROVA")
add("chapter.hatman.5.sub", "The table is set. One chair is empty.", "Sofra kuruldu. Bir sandalye boş.")

# ------------------------------------------------------------------ objectives
O = {
    "enter": ("Go inside Marlowe House", "Marlowe Köşkü'ne gir"),
    "camp": ("Find Ruth's base camp. She said: the study, ground floor.", "Ruth'un kamp kurduğu yeri bul. Demişti ki: zemin kat, çalışma odası."),
    "camcorder": ("Take Ruth's camcorder from the desk", "Masadaki Ruth'un kamerasını al"),
    "casefile": ("Read Ruth's case file on the desk", "Masadaki Ruth'un dava dosyasını oku"),
    "cellar_key": ("Find the cellar key. Ruth's file says: the hall table.", "Bodrum anahtarını bul. Ruth'un dosyasına göre: girişteki sehpa."),
    "cellar": ("Go down to the cellar. The door is in the stair hall.", "Bodruma in. Kapısı merdiven holünde."),
    "fuel": ("The generator is dry. Find fuel in the cellar.", "Jeneratörün yakıtı bitmiş. Bodrumda yakıt bul."),
    "generator": ("Start the generator", "Jeneratörü çalıştır"),
    "phone": ("The telephone in the entrance hall is ringing. The line was cut in 1962.", "Giriş holündeki telefon çalıyor. Hat 1962'de kesilmişti."),
    "evidence": ("Investigate the ground floor: the dining room, the living room, the kitchen (%s/3)", "Zemin katı araştır: yemek odası, oturma odası, mutfak (%s/3)"),
    "clock": ("The maid's note: the workshop key is inside the grandfather clock", "Hizmetçinin notu: atölye anahtarı büyük ayaklı saatin içinde"),
    "workshop": ("Open the hatter's workshop in the cellar", "Bodrumdaki şapkacı atölyesini aç"),
    "ledger": ("Search the workshop. There is a ledger on the desk.", "Atölyeyi ara. Masada bir defter var."),
    "run_study": ("RUN. Get to Ruth's room and bar the door!", "KOŞ. Ruth'un odasına git ve kapıyı sürgüle!"),
    "upstairs": ("Unlock the gate on the stairs and go up", "Merdivendeki kapıyı aç ve yukarı çık"),
    "masks": ("Look at the masks on the wall", "Duvardaki maskelere bak"),
    "trail": ("Follow Ruth's trail upstairs: the child's room, the guest room, the bathroom (%s/3)", "Üst katta Ruth'un izini sür: çocuk odası, misafir odası, banyo (%s/3)"),
    "recorder": ("Ruth hid her recorder in the master bedroom", "Ruth kayıt cihazını ana yatak odasına sakladı"),
    "hide": ("HIDE. In the wardrobe. Camera OFF.", "SAKLAN. Gardıroba gir. Kamerayı KAPAT."),
    "run_hide": ("He saw you. Break his line of sight and hide!", "Seni gördü. Görüş alanından çık ve saklan!"),
    "dressing": ("He is waiting outside the door. Leave through the dressing room.", "Kapının önünde bekliyor. Giyinme odasından çık."),
    "run_library": ("RUN. The library, end of the hall!", "KOŞ. Kütüphane, koridorun sonunda!"),
    "hide_child": ("Locked! The child's room. Hide in the wardrobe!", "Kilitli! Çocuk odası. Gardıroba saklan!"),
    "library": ("The library door has opened by itself", "Kütüphane kapısı kendiliğinden açıldı"),
    "survive": ("RUN. Lose him. Hide.", "KOŞ. İzini kaybettir. Saklan."),
    "attic": ("Climb to the attic. The ladder is in the upper stair hall.", "Çatı katına çık. Merdiven üst kat holünde."),
    "badge": ("Search the attic. There is something of Ruth's on the desk.", "Çatı katını ara. Masada Ruth'a ait bir şey var."),
    "run_cellar": ("RUN. All the way down. The workshop!", "KOŞ. En aşağıya. Atölyeye!"),
    "servant": ("He is on the main stairs. The servants' stairs: through the library!", "Ana merdivende. Hizmetçi merdiveni: kütüphanenin içinden!"),
    "hatblock": ("The hat block on the workshop counter. Pull it!", "Atölye tezgâhındaki şapka kalıbı. Çek onu!"),
    "ruth": ("Someone is sitting in the cold room", "Soğuk odada biri oturuyor"),
    "dining": ("He is waiting in the dining room. You have his face.", "Yemek odasında bekliyor. Yüzü sende."),
    "dining_safe": ("He is waiting in the dining room. (Marlowe's safe is in the master bedroom. The code is an invoice number.)", "Yemek odasında bekliyor. (Marlowe'un kasası ana yatak odasında. Şifre bir fatura numarası.)"),
    "get_out": ("GET OUT. The front door!", "ÇIK DIŞARI. Ön kapı!"),
    "tunnel": ("RUN. The front door is sealed. The cellar tunnel!", "KOŞ. Ön kapı mühürlü. Bodrumdaki tünel!"),
    "gate": ("The gate. GO.", "Bahçe kapısı. KOŞ."),
}
for k, (en, tr) in O.items():
    add("obj.hatman." + k, en, tr)

# ------------------------------------------------------------------ inner voice (shown in grey, no speaker)
T = {
    "c1_door": ("(The door. I didn't touch it.)", "(Kapı. Ben dokunmadım.)"),
    "c1_dark": ("(No power. No light. Ruth's notes said she set up in the study.)", "(Elektrik yok. Işık yok. Ruth notlarında çalışma odasına yerleştiğini yazmıştı.)"),
    "c1_camp": ("(Her bedroll. Her lantern, still burning. She left in a hurry... or she didn't leave.)", "(Uyku tulumu. Feneri hâlâ yanıyor. Aceleyle çıkmış... ya da hiç çıkmamış.)"),
    "c1_file": ("(\"Do not lie to him.\" Ruth doesn't write things twice unless she's scared.)", "(\"Ona yalan söyleme.\" Ruth korkmadıkça bir şeyi iki kez yazmaz.)"),
    "c1_glitch": ("(The camera just... caught something. Down the hall.)", "(Kamera az önce... bir şey yakaladı. Koridorun ucunda.)"),
    "c1_cellar": ("(Footsteps. Above me. Nobody else is supposed to be in this house.)", "(Ayak sesleri. Tam üstümde. Bu evde başka kimse olmamalı.)"),
    "c1_gen_empty": ("(Generator. Tank's bone dry. There has to be fuel somewhere down here.)", "(Jeneratör. Deposu kupkuru. Aşağıda bir yerde yakıt olmalı.)"),
    "c1_end": ("(He hung up. ...Something is walking down the stairs.)", "(Kapattı. ...Bir şey merdivenlerden iniyor.)"),
    "c2_tv": ("(He was standing right behind the chairs. He was watching the film with me.)", "(Koltukların hemen arkasında duruyordu. Filmi benimle birlikte izliyordu.)"),
    "c2_clock": ("(Thirty guests. No hosts. And the maid knew where the workshop key was.)", "(Otuz misafir. Ev sahibi yok. Ve hizmetçi atölye anahtarının yerini biliyordu.)"),
    "c2_thirteen": ("(That clock just struck thirteen.)", "(O saat az önce on üç kez çaldı.)"),
    "c2_shop": ("(Hat blocks. Felt. Mercury. And a ledger, open on the desk like someone was just reading it.)", "(Şapka kalıpları. Keçe. Cıva. Ve masada açık duran bir defter; sanki biri az önce okuyormuş gibi.)"),
    "c2_run": ("(The lights— RUN!)", "(Işıklar— KOŞ!)"),
    "c2_ahead": ("(He's in FRONT of me. How is he in front of me?!)", "(ÖNÜMDE. Nasıl önüme geçti?!)"),
    "c2_quiet": ("(...He stopped.)", "(...Durdu.)"),
    "c2_window": ("(That was the window. We're on the ground floor. He's tall enough to knock on the top pane.)", "(Bu pencereydi. Zemin kattayız. Üst cama vuracak kadar uzun.)"),
    "c2_key": ("(Something slid out of the ledger's spine. A small brass key. For the gate on the stairs.)", "(Defterin sırtından bir şey kaydı. Küçük pirinç bir anahtar. Merdivendeki kapının.)"),
    "c3_plates": ("(Thirty masks. Thirty brass plates. The newest one says CALLOWAY. And there is an empty hook.)", "(Otuz maske. Otuz pirinç plaka. En yenisinde CALLOWAY yazıyor. Ve boş bir kanca var.)"),
    "c3_musicbox": ("(Nobody wound that.)", "(Onu kimse kurmadı.)"),
    "c3_bath": ("(Written in the dust on the mirror. Ruth's handwriting: DON'T LIE TO HIM.)", "(Aynadaki toza yazılmış. Ruth'un el yazısı: ONA YALAN SÖYLEME.)"),
    "c3_recorder": ("(Ruth's bag. Her log says she hid the recorder in the master bedroom. The lock just clicked.)", "(Ruth'un çantası. Günlüğünde kayıt cihazını ana yatak odasına sakladığını yazmış. Kilit az önce tıkladı.)"),
    "c3_knock": ("(Three knocks. He's at the door. HIDE.)", "(Üç vuruş. Kapıda. SAKLAN.)"),
    "c3_outside": ("(He closed the door behind him. Politely. ...He's still standing on the other side.)", "(Kapıyı arkasından kapattı. Kibarca. ...Hâlâ öbür tarafında duruyor.)"),
    "c3_piano": ("(The piano. One note. He knows which door I used.)", "(Piyano. Tek bir nota. Hangi kapıdan çıktığımı biliyor.)"),
    "c3_locked": ("(LOCKED. The boy's room, across the hall!)", "(KİLİTLİ. Karşıdaki çocuk odası!)"),
    "c3_door_open": ("(The library door. He unlocked it. That's an invitation.)", "(Kütüphane kapısı. Kilidini o açtı. Bu bir davet.)"),
    "c3_rage": ("(RUN!)", "(KOŞ!)"),
    "c3_hatch": ("(Wood splintering above me. He tore the attic hatch open. He WANTS me up there.)", "(Yukarıda tahta parçalanıyor. Çatı kapağını söktü. Oraya çıkmamı İSTİYOR.)"),
    "c3_key": ("(He put a key on the armrest before he left. The attic.)", "(Gitmeden önce koltuğun koluna bir anahtar bıraktı. Çatı katının.)"),
    "c4_mirrors": ("(The mirrors. He is in every one of them. ...He is not behind me in any of them.)", "(Aynalar. Hepsinde o var. ...Hiçbirinde arkamda değil.)"),
    "c4_run": ("(The cellar. Behind the shelves. GO!)", "(Bodrum. Rafların arkası. KOŞ!)"),
    "c4_stairs": ("(He's coming UP the stairs. Other way. Library. Servants' stairs!)", "(Merdivenden YUKARI geliyor. Öbür taraf. Kütüphane. Hizmetçi merdiveni!)"),
    "c4_guests": ("(The guests. They're standing in the hall. They're all looking at me.)", "(Misafirler. Koridorda ayakta duruyorlar. Hepsi bana bakıyor.)"),
    "c4_block": ("(Third hat block on the counter. Ruth's note. PULL IT!)", "(Tezgâhtaki üçüncü şapka kalıbı. Ruth'un notu. ÇEK ONU!)"),
    "c4_safe": ("(He's stopped. He won't come in here. ...Why won't he come in here?)", "(Durdu. Buraya girmiyor. ...Neden buraya girmiyor?)"),
    "c4_go": ("(The shelf is open. The whole house is quiet. He's letting me walk.)", "(Raf açıldı. Bütün ev sessiz. Yürümeme izin veriyor.)"),
    "c5_quiet": ("(Every lamp is lit. Every door is open. He has tidied up for company.)", "(Bütün lambalar yanıyor. Bütün kapılar açık. Misafir için evi toplamış.)"),
    "c5_fire": ("(The whole table went up. RUN!)", "(Bütün masa alev aldı. KOŞ!)"),
    "c5_front": ("(Front door won't move. The tunnel in the wine cellar. GO!)", "(Ön kapı kıpırdamıyor. Şarap mahzenindeki tünel. KOŞ!)"),
    "code": ("(Invoice No. 0731. Eleanor's letter said the safe opens with the order number.)", "(Fatura No. 0731. Eleanor'un mektubunda kasanın sipariş numarasıyla açıldığı yazıyordu.)"),
    "oil_and_fire": ("(Lamp oil and a lighter. This house is a hundred years of dry wood.)", "(Lamba yağı ve bir çakmak. Bu ev yüz yıllık kuru tahta.)"),
    "caught0": ("(...I'm in the hall. I'm breathing. He put me back. Why did he put me back?)", "(...Koridordayım. Nefes alıyorum. Beni geri koydu. Neden geri koydu?)"),
    "caught1": ("(\"Again,\" he said. \"With better manners.\")", "(\"Tekrar,\" dedi. \"Daha terbiyeli bir şekilde.\")"),
    "caught2": ("(My face hurts. Like someone tried it on.)", "(Yüzüm acıyor. Sanki biri denemiş gibi.)"),
    "caught3": ("(He is not trying to kill me. He is practising.)", "(Beni öldürmeye çalışmıyor. Prova yapıyor.)"),
    "hunt0": ("(The floor— he's coming up through the FLOOR!)", "(Zemin— ZEMİNDEN çıkıyor!)"),
    "hunt1": ("(Hide. Hide hide hide.)", "(Saklan. Saklan saklan saklan.)"),
    "hunt2": ("(Don't run in a straight line. Doors. Put doors between us.)", "(Düz koşma. Kapılar. Araya kapı koy.)"),
    "tape0": ("Day six. I've stopped pretending this is a missing persons case.", "Altıncı gün. Bunun bir kayıp vakası olduğunu söylemeyi bıraktım."),
    "tape1": ("He talks to me through doors. He asks permission. If I say no politely, he goes away. If I lie, the lights go out.", "Benimle kapıların ardından konuşuyor. İzin istiyor. Kibarca hayır dersem gidiyor. Yalan söylersem ışıklar sönüyor."),
    "tape2": ("He can't see into a closed wardrobe. But the camera's little red lamp shines through the slats. I learned that the hard way.", "Kapalı bir gardırobun içini göremiyor. Ama kameranın küçük kırmızı lambası aralıklardan sızıyor. Bunu acı yoldan öğrendim."),
    "tape3": ("He isn't hunting. He's shopping. He's trying us on. Thirty guests in 1962, and none of them fit.", "Avlanmıyor. Alışveriş yapıyor. Bizi deniyor. 1962'de otuz misafir; hiçbiri uymadı."),
    "tape4": ("If you're hearing this, %s— I told you not to come. ...He's knocking. Someone's knocking. Is that on the tape, or—", "Bunu duyuyorsan, %s— sana gelme demiştim. ...Kapıyı çalıyor. Biri kapıyı çalıyor. Bu kasette mi, yoksa—"),
}
for k, (en, tr) in T.items():
    add("line." + k, en, tr)

# ------------------------------------------------------------------ cutscenes
C = {
    "intro": [
        ("This is Detective Ruth Calloway. If you're hearing this, I've been inside Marlowe House longer than I planned.", "Ben Dedektif Ruth Calloway. Bunu dinliyorsan, Marlowe Köşkü'nde planladığımdan uzun kaldım demektir."),
        ("Six trespassers missing in two years. I thought: squatters, a sinkhole, a bad floor. It isn't a bad floor.", "İki yılda altı kayıp. Dedim ki: evsizler, bir çukur, çürük bir döşeme. Çürük döşeme değil."),
        ("There is someone in this house. He is very tall. He wears a hat. He knocks before he comes in.", "Bu evde biri var. Çok uzun. Şapka takıyor. İçeri girmeden önce kapıyı çalıyor."),
        ("My camera can see him when my eyes can't. I'm leaving it on the desk in the study, in case I... in case.", "Gözlerim göremediğinde kameram onu görebiliyor. Çalışma odasındaki masaya bırakıyorum; olur da ben... olur da."),
        ("Don't come looking for me. I mean it. But if you do, because you never listen—", "Beni aramaya gelme. Ciddiyim. Ama gelirsen, çünkü sen hiç söz dinlemezsin—"),
        ("—whatever he asks you, do not lie to him.", "—sana ne sorarsa sorsun, ona yalan söyleme."),
        ("(That message is nineteen days old. The front door is standing open.)", "(Bu mesaj on dokuz günlük. Ön kapı ardına kadar açık.)"),
    ],
    "power": [
        ("(Come on. Come on...)", "(Hadi. Hadi...)"),
        ("(Light. The whole house is waking up.)", "(Işık. Bütün ev uyanıyor.)"),
        ("(The dining room. There are... people. Sitting at the table. They've been sitting in the dark this whole time.)", "(Yemek odası. Orada... insanlar var. Masada oturuyorlar. Bunca zamandır karanlıkta oturuyorlarmış.)"),
        ("(...That telephone is not plugged into anything.)", "(...O telefon hiçbir yere bağlı değil.)"),
    ],
    "tv": [
        ("...on the first of November, nineteen sixty-two, police entered the Marlowe residence on Hollow Lane.", "...bin dokuz yüz altmış ikinin bir Kasım günü polis, Oyuk Sokak'taki Marlowe evine girdi."),
        ("Thirty guests of a masquerade ball were found seated at the dinner table. None could say how long they had been there.", "Bir maskeli balonun otuz misafiri yemek masasında otururken bulundu. Hiçbiri ne zamandır orada olduğunu söyleyemedi."),
        ("Of the hosts, hat-maker Edmund Marlowe, his wife Eleanor and their son Thomas, there was no trace.", "Ev sahiplerinden, şapkacı Edmund Marlowe, eşi Eleanor ve oğulları Thomas'tan hiçbir iz yoktu."),
        ("One guest repeated a single phrase: \"He asked so nicely.\"", "Misafirlerden biri tek bir cümleyi tekrarlıyordu: \"O kadar kibar sordu ki.\""),
        ("(...That isn't the broadcast any more. That's this room.)", "(...Bu artık yayın değil. Bu, bu oda.)"),
    ],
    "masquerade": [
        ("(Streamers. Paper hats. A birthday... and a ball. Sixty years of dust on a party.)", "(Kâğıt süsler. Parti şapkaları. Bir doğum günü... ve bir balo. Bir partinin üstünde altmış yıllık toz.)"),
        ("(Masks on the walls. Dozens. Each one with a little brass name plate.)", "(Duvarlarda maskeler. Onlarca. Her birinin altında küçük pirinç bir isim plakası.)"),
        ("(That one is new. The plate isn't even tarnished. R. CALLOWAY.)", "(Bu yeni. Plakası kararmamış bile. R. CALLOWAY.)"),
        ("(And an empty hook. The plate is already engraved. It says %s.)", "(Ve boş bir kanca. Plakası çoktan kazınmış. Üstünde %s yazıyor.)"),
    ],
    "fitting": [
        ("(The attic. Mirrors. Hat stands. A tailor's fitting room... for hats.)", "(Çatı katı. Aynalar. Şapka askıları. Bir terzi prova odası... şapkalar için.)"),
        ("(A row of hooks. A name under every one. Every person who ever went missing here.)", "(Bir sıra kanca. Her birinin altında bir isim. Burada kaybolan herkes.)"),
        ("(A hat box. New ribbon. The card says: FOR %s. He has been making it all night.)", "(Bir şapka kutusu. Kurdelesi yeni. Kartta şöyle yazıyor: %s İÇİN. Bütün gece bunu dikiyormuş.)"),
    ],
    "supper": [
        ("(Thirty masks turn to look at the door. Nobody is breathing. I don't think they need to.)", "(Otuz maske kapıya dönüyor. Kimse nefes almıyor. Almaları gerektiğini sanmıyorum.)"),
        ("(One chair is empty. There's a place card. I don't have to read it.)", "(Bir sandalye boş. Bir isim kartı var. Okumama gerek yok.)"),
        ("Detective. You knocked this time. Do sit down.", "Dedektif. Bu sefer kapıyı çaldın. Buyur, otur."),
    ],
    "end_closed": [
        ("...Sixty-four years. I had forgotten the shape of it. Thank you. The account is closed.", "...Altmış dört yıl. Şeklini unutmuşum. Teşekkür ederim. Hesap kapanmıştır."),
        ("I have no further business with this house. Or with the people in it. Good night, Detective.", "Bu evle başka işim kalmadı. İçindekilerle de. İyi geceler, Dedektif."),
        ("At 6:02 AM the front door of Marlowe House opened by itself. Thirty-one people walked out into the rain. They did not remember the party.", "Sabah 06:02'de Marlowe Köşkü'nün ön kapısı kendiliğinden açıldı. Otuz bir kişi yağmura çıktı. Partiyi hatırlamıyorlardı."),
        ("You still owe me twenty for the Delgado stakeout. ...Take me home, rookie.", "Delgado gözetlemesinden bana hâlâ yirmilik borçlusun. ...Beni eve götür, çaylak."),
        ("Case 0731, Marlowe House. Status: CLOSED. Paid in full.", "Dosya 0731, Marlowe Köşkü. Durum: KAPANDI. Tamamı ödendi."),
    ],
    "end_newface": [
        ("There. It fits. I knew it would. I measured you in every doorway.", "İşte. Oldu. Olacağını biliyordum. Her kapı eşiğinde ölçünü aldım."),
        ("By morning there was a new mask on the wall of the upper hall. The brass plate under it had been engraved for some time.", "Sabaha üst kat koridorunun duvarında yeni bir maske vardı. Altındaki pirinç plaka epeydir kazınmış duruyordu."),
        ("Three weeks later another detective parked at the gate of Marlowe House, listening to an old voicemail. It was your voice.", "Üç hafta sonra bir başka dedektif Marlowe Köşkü'nün kapısına park etti; eski bir sesli mesajı dinliyordu. Ses seninkiydi."),
    ],
    "end_worn": [
        ("You put the face on. It was cold. It was the right size for nothing human. And suddenly you were very, very tall.", "Yüzü taktın. Soğuktu. İnsana ait hiçbir şeyin ölçüsünde değildi. Ve birdenbire çok, çok uzundun."),
        ("Ah. So that is how it is done. I had wondered for sixty-four years. Good evening... sir.", "Ah. Demek böyle oluyormuş. Altmış dört yıldır merak ediyordum. İyi akşamlar... efendim."),
        ("The chair at the head of the table is never empty. Someone always knocks. Someone always asks so nicely.", "Sofranın başındaki sandalye hiç boş kalmaz. Hep biri kapıyı çalar. Hep biri çok kibar sorar."),
    ],
    "end_ashes": [
        ("Marlowe House burned for nine hours. The fire brigade could not get the front gate open until it was over.", "Marlowe Köşkü dokuz saat yandı. İtfaiye, her şey bitene kadar ön kapıyı açamadı."),
        ("They found no bodies. They found thirty-one masks in the ashes, unburnt. They found no trace of Detective Ruth Calloway.", "Hiç ceset bulamadılar. Küllerin içinde yanmamış otuz bir maske buldular. Dedektif Ruth Calloway'den hiçbir iz bulamadılar."),
        ("You drove until dawn. When you stopped, there was a hat on the passenger seat. It was still warm.", "Şafağa kadar sürdün. Durduğunda yan koltukta bir şapka vardı. Hâlâ sıcaktı."),
    ],
    "end_cold": [
        ("You came up out of the well with dirt under your nails and ran until the house was gone behind the trees.", "Tırnaklarının altında toprakla kuyudan çıktın ve ev ağaçların ardında kaybolana dek koştun."),
        ("He stood at the gate and did not follow. He tipped his hat. A guest who leaves may always come back.", "Bahçe kapısında durdu ve peşinden gelmedi. Şapkasını eğdi. Giden misafir her zaman geri gelebilir."),
        ("Case 0731, Marlowe House. Status: OPEN. Detective Ruth Calloway: missing. Some nights, at home, something knocks. Three times. Politely.", "Dosya 0731, Marlowe Köşkü. Durum: AÇIK. Dedektif Ruth Calloway: kayıp. Bazı geceler evinde bir şey kapıyı çalıyor. Üç kez. Kibarca."),
    ],
}
for cid, lines in C.items():
    for i, (en, tr) in enumerate(lines):
        add(f"cut.{cid}.{i}", en, tr)

# ------------------------------------------------------------------ dialogue
# node: (text_en, text_tr, [(choice_en, choice_tr), ...])
D = {
    # ---- the telephone
    "p1": ("Good evening. You are standing in my hall.", "İyi akşamlar. Benim holümde duruyorsun.", []),
    "p2": ("You came in without knocking. That is not how one visits.", "Kapıyı çalmadan girdin. Misafirliğe böyle gelinmez.", [
        ("I'm sorry. The door was open.", "Özür dilerim. Kapı açıktı."),
        ("Who is this? Where is Ruth Calloway?", "Kimsiniz? Ruth Calloway nerede?"),
        ("Police. Identify yourself.", "Polis. Kendinizi tanıtın.")]),
    "p3a": ("An apology. How rare. The last one did not apologise for nine days.", "Bir özür. Ne nadir. Bir önceki dokuz gün boyunca özür dilemedi.", []),
    "p3b": ("A question for a question. That is fair. She is being fitted.", "Soruya soru. Adil. Ona prova yapılıyor.", []),
    "p3c": ("I have no self to identify, Detective. That is rather the whole difficulty.", "Tanıtacak bir kendim yok, Dedektif. Bütün mesele de bu zaten.", []),
    "p3s": ("Nothing to say. Then listen.", "Söyleyecek bir şeyin yok. O hâlde dinle.", []),
    "p4": ("I shall ask you things tonight. Answer truthfully and we will get along. Did you come here alone?", "Bu gece sana sorular soracağım. Doğru cevap verirsen iyi anlaşırız. Buraya yalnız mı geldin?", [
        ("Yes. Alone.", "Evet. Yalnız."),
        ("My team is right outside the gate.", "Ekibim kapının hemen dışında."),
        ("(Hang up.)", "(Telefonu kapat.)")]),
    "p5a": ("Honest. And alone. Good. I prefer my guests one at a time.", "Dürüst. Ve yalnız. Güzel. Misafirlerimi teker teker tercih ederim.", []),
    "p5b": ("...No. They are not. Do not lie to me in my own house.", "...Hayır. Değiller. Bana kendi evimde yalan söyleme.", []),
    "p5c": ("Silence is also an answer. A rude one.", "Sessizlik de bir cevaptır. Kaba bir cevap.", []),
    "p6": ("I am coming downstairs now. I will knock. I always knock.", "Şimdi aşağı iniyorum. Kapıyı çalacağım. Ben her zaman çalarım.", []),
    # ---- the study door
    "k1": ("Detective. You ran. Guests do not run.", "Dedektif. Kaçtın. Misafirler kaçmaz.", []),
    "k2": ("May I come in?", "İçeri girebilir miyim?", [
        ("No. You may not.", "Hayır. Giremezsiniz."),
        ("Go to hell.", "Cehenneme git."),
        ("...Come in.", "...Gir.")]),
    "k3a": ("...No. Very well. A closed door is a closed door. You have better manners than you know.", "...Hayır. Pekâlâ. Kapalı kapı, kapalı kapıdır. Sandığından daha terbiyelisin.", []),
    "k3b": ("I have BEEN. It was dull. There was no one to talk to.", "GİTTİM. Sıkıcıydı. Konuşacak kimse yoktu.", []),
    "k3s": ("You do not answer. I will take that as a no. For tonight.", "Cevap vermiyorsun. Bunu hayır sayıyorum. Bu gecelik.", []),
    "k3c": ("Thank you.", "Teşekkür ederim.", []),
    "k4": ("You took my ledger from the workshop. Did you read it?", "Atölyeden defterimi aldın. Okudun mu?", [
        ("Yes. Order 0731. A hat for a gentleman without a face.", "Evet. Sipariş 0731. Yüzü olmayan bir beyefendi için şapka."),
        ("No. I haven't opened it.", "Hayır. Açmadım."),
        ("What did Marlowe take from you?", "Marlowe senden ne aldı?")]),
    "k5a": ("Then you know what I am owed.", "O hâlde bana ne borçlu olunduğunu biliyorsun.", []),
    "k5b": ("Your thumb is on the page, Detective. I can hear paper.", "Parmağın sayfanın üstünde, Dedektif. Kâğıt sesini duyabiliyorum.", []),
    "k5c": ("Something I had only one of.", "Bende yalnızca bir tane olan bir şeyi.", []),
    "k5s": ("Quiet again. You will learn to answer me.", "Yine sessizlik. Bana cevap vermeyi öğreneceksin.", []),
    "k6": ("Keep the book. Read it again. I shall be upstairs. They are having a party. They have been having it for a very long time.", "Defter sende kalsın. Tekrar oku. Ben yukarıda olacağım. Parti veriyorlar. Çok uzun zamandır veriyorlar.", []),
    # ---- the library
    "l1": ("Sit, if you like. No? Stand, then. Ruth stood too. For the first three days.", "İstersen otur. Hayır mı? Ayakta dur o zaman. Ruth da ayakta durdu. İlk üç gün.", []),
    "l2": ("You have been through my rooms. Tell me, Detective: what do you think I am?", "Odalarımı gezdin. Söyle bakalım Dedektif: sence ben neyim?", [
        ("A customer who was cheated.", "Dolandırılmış bir müşteri."),
        ("A hallucination. Mercury fumes from the workshop.", "Bir halüsinasyon. Atölyedeki cıva buharı."),
        ("A monster.", "Bir canavar.")]),
    "l3a": ("...Yes. Sixty-four years, and you are the first to say customer.", "...Evet. Altmış dört yıl; ve müşteri diyen ilk kişi sensin.", []),
    "l3b": ("Then breathe deeper. See if I thin out.", "O zaman daha derin nefes al. Bak bakalım silikleşiyor muyum.", []),
    "l3c": ("I knocked. I waited. I asked. Which of us broke into whose house?", "Kapıyı çaldım. Bekledim. Sordum. Hangimiz kimin evine zorla girdi?", []),
    "l3s": ("No opinion. Ruth had several.", "Fikrin yok. Ruth'un birkaç tane vardı.", []),
    "l4": ("Now a question that matters. The woman. Did she send for you, or did you come on your own?", "Şimdi önemli bir soru. Kadın. Seni o mu çağırdı, yoksa kendi başına mı geldin?", [
        ("She told me not to come. I came anyway.", "Bana gelme dedi. Yine de geldim."),
        ("She begged me to come.", "Gelmem için yalvardı."),
        ("Where is she?", "O nerede?")]),
    "l5a": ("Disobedient. Loyal. She said you would be. She has been honest with me for some time now.", "Söz dinlemez. Sadık. Öyle olacağını söylemişti. Bir süredir bana karşı dürüst.", []),
    "l5b": ("No. I have her voice, Detective. I have listened to all of it. She begged you to stay away.", "Hayır. Sesi bende, Dedektif. Hepsini dinledim. Uzak durman için yalvardı.", []),
    "l5c": ("Below. Where things keep.", "Aşağıda. Şeylerin bozulmadığı yerde.", []),
    "l5s": ("You hold your tongue. She did that too, at first.", "Dilini tutuyorsun. O da öyle yapmıştı, başta.", []),
    "l6": ("One more. If I gave you a hat tonight, a good one, made to measure, would you wear it?", "Bir tane daha. Bu gece sana bir şapka versem, iyi bir şapka, tam ölçüne göre; takar mıydın?", [
        ("No.", "Hayır."),
        ("If it means Ruth walks out of here.", "Ruth buradan çıkıp gidecekse, evet."),
        ("Yes.", "Evet.")]),
    "l7a": ("Hm. We shall ask again when you have seen the fitting room.", "Hım. Prova odasını gördükten sonra tekrar soracağız.", []),
    "l7b": ("A trade. I like trades. I have not had good luck with them.", "Bir takas. Takasları severim. Pek şansım yaver gitmedi gerçi.", []),
    "l7c": ("...Do not say that lightly in this house.", "...Bu evde bunu hafife alarak söyleme.", []),
    "l7s": ("Then I will decide for you.", "O hâlde senin yerine ben karar veririm.", []),
    "l8": ("The attic is open to you. Go up. See what I have made.", "Çatı katı sana açık. Çık. Ne diktiğimi gör.", []),
    # ---- Ruth
    "r1": ("...Is that you? Don't come closer. Don't look at my face. There isn't one.", "...Sen misin? Yaklaşma. Yüzüme bakma. Yok çünkü.", []),
    "r2": ("Listen. I don't have long before he's back in my mouth. He isn't cruel. He's OWED. Marlowe took his face as a deposit and gave him a hat that didn't fit.", "Dinle. O tekrar ağzıma girmeden çok vaktim yok. Zalim değil. ALACAKLI. Marlowe yüzünü depozito olarak aldı ve ona olmayan bir şapka verdi.", [
        ("I'm getting you out of here.", "Seni buradan çıkarıyorum."),
        ("How do I stop him?", "Onu nasıl durdururum?"),
        ("How do I know you're really Ruth?", "Gerçekten Ruth olduğunu nereden bileyim?")]),
    "r3a": ("Not through that door you're not. Not while he holds the account.", "O kapıdan çıkaramazsın. Hesap onun elindeyken olmaz.", []),
    "r3b": ("You don't stop him. You pay him.", "Onu durduramazsın. Borcunu ödersin.", []),
    "r3c": ("You don't. ...You still owe me twenty for the Delgado stakeout. He wouldn't know that. He never asks about money.", "Bilemezsin. ...Delgado gözetlemesinden bana hâlâ yirmilik borçlusun. O bunu bilemez. Parayı hiç sormaz.", []),
    "r4": ("There's a safe in the master bedroom. The invoice number opens it. His face is inside. Give it BACK. Don't lie to him, don't take the hat, and whatever you do, do not put the face on.", "Ana yatak odasında bir kasa var. Fatura numarasıyla açılıyor. Yüzü içinde. Ona GERİ VER. Yalan söyleme, şapkayı alma ve ne yaparsan yap, o yüzü takma.", []),
    "r5": ("He's set the table. He wants you in the dining room. ...Go. And hey. Thanks for not listening to me.", "Sofrayı kurdu. Seni yemek odasında istiyor. ...Git. Ve hey. Beni dinlemediğin için sağ ol.", []),
    # ---- the last fitting
    "f1": ("Sit. No one has sat in that chair since nineteen sixty-two. I had it dusted.", "Otur. O sandalyeye bin dokuz yüz altmış ikiden beri kimse oturmadı. Tozunu aldırdım.", []),
    "f2a": ("You have been rude in every room of my house. You have lied to me, or cursed me, or both. I am out of patience, and I had a great deal.", "Evimin her odasında kabalık ettin. Bana yalan söyledin ya da küfrettin, belki ikisi de. Sabrım tükendi; ki çok sabrım vardı.", []),
    "f2b": ("You have been so very agreeable. Yes to this, yes to that. Do you know what a man who says yes to everything is, Detective? Mine.", "Ne kadar da uysaldın. Şuna evet, buna evet. Her şeye evet diyen adam nedir, bilir misin Dedektif? Benimdir.", []),
    "f2c": ("You have been honest with me. More or less. That is more than Marlowe managed.", "Bana karşı dürüsttün. Aşağı yukarı. Marlowe'un becerdiğinden fazlası bu.", []),
    "f3": ("Here is my last question. I ask it of everyone. Will you wear it?", "Son sorum şu. Herkese sorarım. Takar mısın?", [
        ("I have something of yours. (Show him the face.)", "Sana ait bir şey var bende. (Ona yüzü göster.)"),
        ("Yes.", "Evet."),
        ("No.", "Hayır."),
        ("(You try to say no. Nothing comes out.)", "(Hayır demeye çalışıyorsun. Sesin çıkmıyor.)"),
        ("I'll burn this house down first. (Tip the lamp oil over the table.)", "Önce bu evi yakarım. (Lamba yağını masaya dök.)")]),
    "f_silence": ("No answer. At my table. ...I will count to three.", "Cevap yok. Benim soframda. ...Üçe kadar sayacağım.", []),
    "f_face": ("............Where did you find that.", "............Onu nerede buldun.", [
        ("In Marlowe's safe. He never meant to give it back. I do.", "Marlowe'un kasasında. O geri vermeyi hiç düşünmedi. Ben veriyorum."),
        ("(Put the face on.)", "(Yüzü tak.)")]),
    "f_face2": ("...Paid. In full. By a stranger. I have no words for this, and I have had so many.", "...Ödendi. Tamamı. Bir yabancı tarafından. Buna diyecek sözüm yok; oysa ne çok sözüm vardı.", []),
    "f_wear": ("It is cold. It fits nothing human. Around the table, thirty masks bow their heads.", "Soğuk. İnsana ait hiçbir şeye uymuyor. Masanın çevresinde otuz maske başını eğiyor.", []),
    "f_yes": ("Then hold still. This will not hurt. That is what they tell me, afterwards.", "O hâlde kıpırdama. Acımayacak. Sonradan bana öyle diyorlar.", []),
    "f_no": ("No. ...Then you are a guest who is leaving. I will count to three. I am not good at counting.", "Hayır. ...O hâlde giden bir misafirsin. Üçe kadar sayacağım. Saymayı pek beceremem.", []),
    "f_burn": ("THAT IS MY HOUSE—", "O BENİM EVİM—", []),
}
for node, (en, tr, choices) in D.items():
    add("dlg." + node, en, tr)
    for i, (cen, ctr) in enumerate(choices):
        add(f"dlg.{node}.{i}", cen, ctr)
add("dlg.count1", "One.", "Bir.")
add("dlg.count2", "Two.", "İki.")
add("dlg.count3", "...Three.", "...Üç.")

# ------------------------------------------------------------------ endings (credits screen)
EN = {
    "closed": ("ENDING: CASE CLOSED", "SON: DOSYA KAPANDI", [
        ("You gave back what was taken. He kept his word. He always keeps his word.", "Alınanı geri verdin. O sözünü tuttu. O her zaman sözünü tutar."),
        ("Thirty guests and one detective walked out of Marlowe House at dawn.", "Otuz misafir ve bir dedektif şafakta Marlowe Köşkü'nden yürüyerek çıktı."),
        ("The house was demolished the following spring. Nothing was found in the walls.", "Köşk ertesi bahar yıkıldı. Duvarların içinde hiçbir şey bulunmadı."),
        ("Somewhere, a tall gentleman is wearing his own face for the first time in sixty-four years.", "Bir yerlerde uzun boylu bir beyefendi, altmış dört yıl sonra ilk kez kendi yüzünü taşıyor.")]),
    "newface": ("ENDING: THE NEW FACE", "SON: YENİ YÜZ", [
        ("You said yes. It fit.", "Evet dedin. Oldu."),
        ("There are thirty-two masks on the wall now.", "Artık duvarda otuz iki maske var."),
        ("He still does not have one of his own. But yours is a very good likeness.", "Hâlâ kendine ait bir yüzü yok. Ama seninki ona çok yakışıyor."),
        ("Someone will come looking for you, %s. They always do.", "Biri seni aramaya gelecek, %s. Hep gelirler.")]),
    "worn": ("SECRET ENDING: THE CALLER", "GİZLİ SON: ZİYARETÇİ", [
        ("Ruth told you not to put it on.", "Ruth sana onu takma demişti."),
        ("The account did not close. It changed hands.", "Hesap kapanmadı. El değiştirdi."),
        ("You are very tall now. You are very patient. You always knock.", "Artık çok uzunsun. Çok sabırlısın. Kapıyı her zaman çalıyorsun."),
        ("You are owed nothing. But you have so many questions to ask.", "Kimse sana bir şey borçlu değil. Ama soracak ne çok sorun var.")]),
    "ashes": ("ENDING: ASHES", "SON: KÜLLER", [
        ("You burned the house, the workshop, the ledger, and the debt.", "Evi, atölyeyi, defteri ve borcu yaktın."),
        ("A debt does not burn.", "Borç yanmaz."),
        ("Ruth Calloway was never found. Neither were the guests.", "Ruth Calloway hiç bulunamadı. Misafirler de öyle."),
        ("The hat on your passenger seat is your size.", "Yan koltuğundaki şapka tam senin ölçün.")]),
    "cold": ("ENDING: COLD CASE", "SON: FAİLİ MEÇHUL", [
        ("You got out. That is more than most.", "Dışarı çıktın. Bu, çoğunun başardığından fazlası."),
        ("You left Ruth in the cold room. You left his face in the safe.", "Ruth'u soğuk odada bıraktın. Onun yüzünü kasada bıraktın."),
        ("The file stays open on your desk. So does the door, some nights.", "Dosya masanda açık duruyor. Bazı geceler kapı da öyle."),
        ("He knocks. He is still asking, very nicely.", "Kapıyı çalıyor. Hâlâ soruyor; çok kibarca.")]),
}
for k, (en, tr, lines) in EN.items():
    add("ending.hatman." + k, en, tr)
    for i, (len_, ltr) in enumerate(lines):
        add(f"ending.hatman.{k}.{i}", len_, ltr)

CR = [
    ("THE HATMAN: HOLLOW HOUSE", "ŞAPKALI ADAM: OYUK EV"),
    ("an analog horror story for Minecraft", "Minecraft için bir analog korku hikâyesi"),
    ("Story, house, models, animation and code: krolasyon & Claude", "Hikâye, ev, modeller, animasyon ve kod: krolasyon & Claude"),
    ("Sound effects: CC0 recordings from OpenGameArt.org", "Ses efektleri: OpenGameArt.org'dan CC0 kayıtlar"),
    ("Detective Ruth Calloway", "Dedektif Ruth Calloway"),
    ("The Marlowe family: Edmund, Eleanor, Thomas", "Marlowe ailesi: Edmund, Eleanor, Thomas"),
    ("The Hatman appeared as himself.", "Şapkalı Adam kendisini canlandırdı."),
    ("There are five endings. He remembers how you spoke to him.", "Beş farklı son var. Onunla nasıl konuştuğunu hatırlıyor."),
    ("Thank you for knocking.", "Kapıyı çaldığın için teşekkürler."),
]
for i, (en, tr) in enumerate(CR):
    add(f"credits.hatman.{i}", en, tr)
add("credits.hatman.stats", "Notes found: %s/%s    Times caught: %s    Lies told: %s", "Bulunan notlar: %s/%s    Yakalanma: %s    Söylenen yalan: %s")
