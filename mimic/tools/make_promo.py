from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageEnhance, ImageChops, ImageOps
import glob, random
C='/tmp/claude-0/-home-user-Montella/ca4834bb-a456-5944-b90d-b1794b013286/scratchpad/mpromo/'
O='/home/user/Montella/mimic/curseforge/'
def font(name,size):
    return ImageFont.truetype(glob.glob('/usr/share/fonts/**/'+name,recursive=True)[0],size)
BONE=(236,228,210); RED=(196,36,30); DIM=(160,152,138)
W,H=1920,1080

def lift(im, gamma=0.78, sat=0.8, con=1.18):
    im=im.convert('RGB')
    im=im.point(lambda v: int(255*((v/255)**gamma)))
    im=ImageEnhance.Contrast(im).enhance(con)
    im=ImageEnhance.Color(im).enhance(sat)
    r,g,b=im.split()
    r=r.point(lambda v:int(min(255,v*1.04+4))); b=b.point(lambda v:int(v*0.94+8))
    return Image.merge('RGB',(r,g,b))

def vignette(im,strength=0.75):
    w,h=im.size
    m=Image.new('L',(w,h),0); d=ImageDraw.Draw(m)
    for i in range(40):
        t=i/40; d.ellipse([w*0.5-w*(0.75-0.45*t), h*0.5-h*(0.8-0.45*t), w*0.5+w*(0.75-0.45*t), h*0.5+h*(0.8-0.45*t)], fill=int(255*t))
    m=m.filter(ImageFilter.GaussianBlur(60))
    dark=Image.new('RGB',(w,h),(2,3,4))
    return Image.composite(im,Image.blend(dark,im,1-strength),m)

def grain(im,amt=0.05,seed=1):
    random.seed(seed)
    n=Image.effect_noise(im.size,40).convert('RGB')
    return Image.blend(im,n,amt)

def spaced(d,xy,text,f,fill,spacing,anchor='l',shadow=True):
    x,y=xy
    ws=[d.textlength(c,font=f) for c in text]; total=sum(ws)+spacing*(len(text)-1)
    if anchor=='r': x-=total
    if anchor=='m': x-=total/2
    for c,wd in zip(text,ws):
        if shadow: d.text((x+4,y+5),c,font=f,fill=(0,0,0))
        d.text((x,y),c,font=f,fill=fill); x+=wd+spacing
    return total

def glow(im,fn,blur=16,k=0.6):
    l=Image.new('RGB',im.size,(0,0,0)); fn(ImageDraw.Draw(l))
    return ImageChops.add(im,ImageEnhance.Brightness(l.filter(ImageFilter.GaussianBlur(blur))).enhance(k))

def scratched(im, box, seed=3):
    """claw marks across the title"""
    d=ImageDraw.Draw(im); r=random.Random(seed)
    x0,y0,x1,y1=box
    for k in range(3):
        sx=x0+ (x1-x0)*(0.30+0.13*k); 
        d.line([(sx, y0), (sx+90, y1)], fill=(4,4,5), width=9)
    return im

TITLE=font('Caladea-Bold.ttf',190); SUB=font('InterDisplay-Light.otf',50); TAG=font('FreeSerifItalic.ttf',44); SMALL=font('InterDisplay-Medium.otf',27); CAP=font('InterDisplay-Light.otf',40)

# ---------------- cover
src=lift(Image.open(C+'pr_barn_roar.png'),0.72)
canvas=src.crop((0,0,W,H))
canvas=ImageChops.offset(canvas,330,0)   # the creature into the right half
dark=Image.new('RGB',(W,H),(3,4,5))
m=Image.new('L',(W,H),0); dm=ImageDraw.Draw(m)
for x in range(W): dm.line([(x,0),(x,H)],fill=int(255*min(1,max(0,(x-360)/520))))
canvas=Image.composite(canvas,dark,m.filter(ImageFilter.GaussianBlur(30)))
im=vignette(canvas,0.6)
def t(d): spaced(d,(110,300),'MIMIC',TITLE,BONE,14,shadow=False)
im=glow(im,t,18,0.55)
d=ImageDraw.Draw(im)
spaced(d,(110,300),'MIMIC',TITLE,BONE,14)
# (no claw marks: they read as a strike-through)
d=ImageDraw.Draw(im)
spaced(d,(118,520),'HARROW  FARM',SUB,RED,24)
d.line([(118,605),(600,605)],fill=(130,28,24),width=2)
d.text((118,640),'It learned your sister\'s voice.',font=TAG,fill=BONE)
d.text((118,696),'Now it is learning yours.',font=TAG,fill=BONE)
spaced(d,(120,880),'HORROR STORY MOD  ·  FORGE 1.20.1 & 1.21.1',SMALL,DIM,3,shadow=False)
im=grain(im,0.045)
im.save(O+'cover_1920x1080.png'); im.save(O+'cover_1920x1080.jpg',quality=93)
im.resize((1280,720),Image.LANCZOS).save(O+'banner_1280x720.jpg',quality=92)

# ---------------- square logo
src=lift(Image.open(C+'pr_barn_roar.png'),0.7)
sq=src.crop((380,0,1460,1080)).resize((1024,1024),Image.LANCZOS)
ov=Image.new('L',(1024,1024),0); dd=ImageDraw.Draw(ov)
for y in range(600,1024): dd.line([(0,y),(1024,y)],fill=int(250*((y-600)/424)**1.2))
sq=Image.composite(Image.new('RGB',(1024,1024),(3,4,5)),sq,ov)
sq=vignette(sq,0.5)
f1=font('Caladea-Bold.ttf',200); f2=font('InterDisplay-Light.otf',56)
def t2(d): spaced(d,(512,735),'MIMIC',f1,BONE,10,anchor='m',shadow=False)
sq=glow(sq,t2,16,0.5)
d=ImageDraw.Draw(sq)
spaced(d,(512,735),'MIMIC',f1,BONE,10,anchor='m')
spaced(d,(512,935),'HARROW  FARM',f2,RED,18,anchor='m')
sq=grain(sq,0.04)
sq.save(O+'logo_1024x1024.png'); sq.resize((400,400),Image.LANCZOS).save(O+'logo_400x400.png')

# ---------------- gallery
G=[('pr_barn_roar','Something has been living under Harrow Farm since July.',0.72),
   ('pr_corn','Six arms. A skull for a face. It hunts by sound.',0.85),
   ('pr_choice','Until Dawn-style decisions - with a clock running out.',0.8),
   ('pr_hiding','Hide. Hold your breath. It is right outside.',0.75),
   ('pr_fight','Or take Dad\'s pitchfork and face it.',0.85),
   ('pr_sleeping','It sleeps in the barn rafters. Don\'t wake it.',0.62),
   ('pr_silo','Lily has been hiding in the silo for three days.',0.75),
   ('pr_stairs','Whatever you hear in the house tonight - don\'t go upstairs.',0.66),
   ('pr_scarecrows','A full 3D farm: house, barn, silo, shed and corn field.',0.8)]
for i,(name,cap,gm) in enumerate(G,1):
    im=lift(Image.open(C+name+'.png'),gm)
    im=vignette(im,0.45)
    top = name in ('pr_hiding', 'pr_fight')   # the HUD is at the bottom of these: the caption goes on top
    bar=Image.new('L',(W,H),0); db=ImageDraw.Draw(bar)
    if top:
        for y in range(0,280): db.line([(0,y),(W,y)],fill=int(250*min(1,(280-y)/90)))
    else:
        for y in range(840,H): db.line([(0,y),(W,y)],fill=int(220*((y-840)/240)))
    im=Image.composite(Image.new('RGB',(W,H),(2,3,4)),im,bar)
    d=ImageDraw.Draw(im)
    cy=92 if top else 940
    d.line([(90,cy+22),(150,cy+22)],fill=RED,width=3)
    d.text((170,cy),cap,font=CAP,fill=BONE)
    spaced(d,(W-90,cy+6),'MIMIC',font('Caladea-Bold.ttf',40),DIM,6,anchor='r',shadow=False)
    im=grain(im,0.035,i)
    im.save(O+f'{i:02d}_{name[3:]}.jpg',quality=92)
print('ok')

# ---------------- alternative covers
def alt_cover(srcname, out, gm, title_xy, right, tag1, tag2, shift=0):
    im=lift(Image.open(C+srcname+'.png'),gm)
    if shift: im=ImageChops.offset(im,shift,0)
    m=Image.new('L',(W,H),0); dm=ImageDraw.Draw(m)
    for x in range(W):
        f=(x-1100)/600 if right else (820-x)/600
        dm.line([(x,0),(x,H)],fill=int(255*min(1,max(0,f))*0.85))
    im=Image.composite(Image.new('RGB',(W,H),(3,4,5)),im,m.filter(ImageFilter.GaussianBlur(40)))
    im=vignette(im,0.5)
    x,y=title_xy; a='r' if right else 'l'
    def t(d): spaced(d,(x,y),'MIMIC',TITLE,BONE,14,anchor=a,shadow=False)
    im=glow(im,t,18,0.55)
    d=ImageDraw.Draw(im)
    spaced(d,(x,y),'MIMIC',TITLE,BONE,14,anchor=a)
    spaced(d,(x+(-6 if right else 8),y+220),'HARROW  FARM',SUB,RED,24,anchor=a)
    lx0,lx1=(x-480,x) if right else (x+8,x+490)
    d.line([(lx0,y+305),(lx1,y+305)],fill=(130,28,24),width=2)
    for k,tline in enumerate((tag1,tag2)):
        d.text((x if right else x+8, y+340+56*k), tline, font=TAG, fill=BONE, anchor='ra' if right else 'la')
    spaced(d,(x+(-4 if right else 10),y+580),'HORROR STORY MOD  ·  FORGE 1.20.1 & 1.21.1',SMALL,DIM,3,anchor=a,shadow=False)
    im=grain(im,0.045)
    im.save(O+out,quality=93)

alt_cover('pr_corn','cover_corn_1920x1080.jpg',0.85,(1820,250),True,'Six arms. A skull for a face.','It hunts by sound.',-260)
alt_cover('pr_stairs','cover_stairs_1920x1080.jpg',0.62,(110,250),False,'Whatever you hear tonight...','don\'t go upstairs.',300)
alt_cover('pr_sleeping','cover_sleeping_1920x1080.jpg',0.6,(1820,250),True,'It sleeps in the barn.','Don\'t wake it.',-280)
alt_cover('pr_silo','cover_lily_1920x1080.jpg',0.75,(110,250),False,'Lily has been in the silo for three days.','Is that really her voice?',360)
print('alt ok')
