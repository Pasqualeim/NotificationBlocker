#!/usr/bin/env python3
"""Store screenshots: 1080 x 1920 (9:16) frames with a caption, one set per language.

Google Play rejects raw phone captures whose long side is more than twice the short side
(1080 x 2400 is 2.22), so every capture is framed here, inside a phone bezel that bleeds off the bottom edge. Captions follow the copy rules of
docs/DESIGN_SYSTEM.md ("Testi e tono").

Type as on the site: Fraunces 600 for the titles, Inter for the subtitles, from the site repo's fonts.

Usage: python3 tools/store/compose_screenshots.py RAW_DIR [path/to/nook-site]   (default: ../nook-site)
RAW_DIR holds it-IT/ and en-US/ with the captures written by tools/store/capture_screenshots.sh.
Writes docs/store/screenshots/{it,en}/.
"""
from PIL import Image, ImageDraw, ImageFilter, ImageFont
import os, sys
from pathlib import Path
ROOT=str(Path(__file__).resolve().parents[2] / 'docs/store/screenshots')
RAW=sys.argv[1]
FONTS=Path(sys.argv[2] if len(sys.argv)>2 else Path(__file__).resolve().parents[3]/'nook-site')/'public/assets/fonts'
W,H=1080,1920
def font(name, size, **axes):
    f=ImageFont.truetype(str(FONTS/name),size)
    ax=f.get_variation_axes(); f.set_variation_by_axes([axes.get(a['name'].decode(),a['default']) for a in ax]); return f
LIGHT=dict(bg=(235,205,188), bg2=(246,226,212), title=(58,43,34), sub=(101,80,63))
DARK=dict(bg=(27,23,20), bg2=(46,38,33), title=(242,231,221), sub=(214,197,184))
SHOTS=[
 ('01_oggi','1_break',None,LIGHT,
  ("La sera è tua","Finito il lavoro, la pausa parte da sola. La sera è già cominciata."),
  ("The evening is yours","When work ends, the break starts on its own. The evening has begun.")),
 ('02_pause','6_breaks',None,LIGHT,
  ("Decidi tu quando lavori","Imposta giorni e orari, poi aggiungi pranzo e giorni liberi. Il resto è pausa."),
  ("You set when you work","Pick your days and hours, then add lunch and days off. Everything else is a break.")),
 ('03_app','3_apps',None,LIGHT,
  ("Scegli le app di lavoro","Solo quelle restano in pausa. Il resto del telefono non cambia."),
  ("Pick your work apps","Only those get paused. The rest of your phone stays as it is.")),
 ('04_statistiche','7_stats',None,LIGHT,
  ("Le tue ore libere","Ore libere al giorno, quante con la luce e quante notifiche in pausa."),
  ("Your free hours","Free hours each day, how many in daylight, and how many notifications were paused.")),
 ('05_resoconto','4_report',None,LIGHT,
  ("Il resoconto del lunedì","Cosa è arrivato nel weekend, app per app. I messaggi restano nelle loro app."),
  ("Monday's summary","What came in over the weekend, app by app. The messages stay in their own apps.")),
 ('06_scuro','5_dark',None,DARK,
  ("Chiaro o scuro","La scena segue l'ora del giorno e le stagioni."),
  ("Light or dark","The scene follows the time of day and the seasons.")),
]
def wrap(d, text, f, maxw):
    words=text.split(); lines=[]; cur=''
    for w in words:
        t=(cur+' '+w).strip()
        if d.textlength(t,font=f)<=maxw: cur=t
        else: lines.append(cur); cur=w
    lines.append(cur); return lines
def rounded(im, r):
    m=Image.new('L',im.size,0); ImageDraw.Draw(m).rounded_rectangle((0,0,im.width-1,im.height-1),r,fill=255); return m
for lang,folder in (('it','it-IT'),('en','en-US')):
    for name,src,crop,pal,it,en in SHOTS:
        title,sub = it if lang=='it' else en
        img=Image.new('RGB',(W,H),pal['bg']); d=ImageDraw.Draw(img)
        for yy in range(H):  # vertical gradient, lighter towards the bottom
            t=yy/H; d.line((0,yy,W,yy),fill=tuple(int(pal['bg'][i]+(pal['bg2'][i]-pal['bg'][i])*t) for i in range(3)))
        ft=font('fraunces-normal.woff2',80,Weight=600,**{'Optical Size':144}); fs=font('inter-normal.woff2',40,Weight=500)
        y=120
        for line in wrap(d,title,ft,W-160):
            d.text((W//2,y),line,font=ft,fill=pal['title'],anchor='ma'); y+=94
        y+=18
        for line in wrap(d,sub,fs,W-200):
            d.text((W//2,y),line,font=fs,fill=pal['sub'],anchor='ma'); y+=54
        top=420  # fixed: the phone sits at the same height in every shot
        shot=Image.open(f'{RAW}/{folder}/{src}.png').convert('RGB')
        if crop: shot=shot.crop(crop)
        sw=940; sh=int(shot.height*sw/shot.width); shot=shot.resize((sw,sh),Image.LANCZOS)
        BZ=16; OR=86   # bezel width, outer corner radius (inner = OR - BZ)
        fx=(W-sw-2*BZ)//2; fw=sw+2*BZ
        vis=H-top-BZ+60  # screen height shown: it bleeds off the bottom edge
        shot=shot.crop((0,0,sw,min(sh,vis)))
        if pal is LIGHT:  # soft shadow under the phone
            sh_=Image.new('RGBA',(W,H),(0,0,0,0)); ImageDraw.Draw(sh_).rounded_rectangle((fx,top+14,fx+fw,H+200),OR,fill=(58,43,34,70))
            img.paste(sh_.filter(ImageFilter.GaussianBlur(26)),(0,0),sh_.filter(ImageFilter.GaussianBlur(26)))
        d=ImageDraw.Draw(img)
        rim=(30,27,25) if pal is LIGHT else (92,84,78)
        d.rounded_rectangle((fx-4,top-4,fx+fw+4,H+200),OR+4,fill=rim)               # rim
        d.rounded_rectangle((fx,top,fx+fw,H+200),OR,fill=(14,13,12))                   # bezel
        d.rounded_rectangle((fx+fw,top+330,fx+fw+9,top+470),4,fill=rim)                # power button
        d.rounded_rectangle((fx+fw,top+520,fx+fw+9,top+700),4,fill=rim)                # volume
        m=rounded(Image.new('RGB',(sw,shot.height+200)),OR-BZ).crop((0,0,sw,shot.height))
        img.paste(shot,(fx+BZ,top+BZ),m)
        d.ellipse((W//2-17,top+BZ+22,W//2+17,top+BZ+56),fill=(8,8,8))                  # camera
        out=f'{ROOT}/{lang}/{name}.png'; img.save(out,optimize=True); print(out, img.size, os.path.getsize(out)//1024,'KB')
