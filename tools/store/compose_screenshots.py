#!/usr/bin/env python3
"""Store screenshots: 1080 x 1920 (9:16) frames with a caption, one set per language.

Google Play rejects raw phone captures whose long side is more than twice the short side
(1080 x 2400 is 2.22), so every capture is framed here. Captions follow the copy rules of
docs/DESIGN_SYSTEM.md ("Testi e tono").

Usage: python3 tools/store/compose_screenshots.py RAW_DIR path/to/Manrope[wght].ttf
RAW_DIR holds it-IT/ and en-US/ with the captures written by tools/store/capture_screenshots.sh.
Writes docs/store/screenshots/{it,en}/.
"""
from PIL import Image, ImageDraw, ImageFont
import os, sys
from pathlib import Path
ROOT=str(Path(__file__).resolve().parents[2] / 'docs/store/screenshots')
RAW=sys.argv[1]
FONT=sys.argv[2]
W,H=1080,1920
def font(size, weight):
    f=ImageFont.truetype(FONT,size); f.set_variation_by_name(weight); return f
LIGHT=dict(bg=(235,205,188), title=(58,43,34), sub=(101,80,63))
DARK=dict(bg=(27,23,20), title=(242,231,221), sub=(214,197,184))
SHOTS=[
 ('01_pausa','1_break',None,LIGHT,
  ("Il lavoro può aspettare","Le notifiche delle app di lavoro vanno in pausa quando stacchi."),
  ("Work can wait","Notifications from your work apps pause when you switch off.")),
 ('02_app','3_apps',None,LIGHT,
  ("Scegli le app di lavoro","Tutte le altre notifiche arrivano come sempre."),
  ("Pick your work apps","Every other notification comes through as usual.")),
 ('03_notifica','2_shade',(0,70,1080,1060),LIGHT,
  ("Tutto nella tendina","Una notifica silenziosa ti dice fino a quando dura la pausa."),
  ("Your break at a glance","A silent notification shows when it ends.")),
 ('04_resoconto','4_report',None,LIGHT,
  ("Sai cosa ti aspetta","Quando torni vedi quante notifiche sono arrivate e da quali app."),
  ("Know what's waiting","When you're back, see how many came in and from which apps.")),
 ('05_scuro','5_dark',None,DARK,
  ("Anche con il tema scuro","La scena segue la luce vera del giorno e le stagioni."),
  ("Light or dark","The scene follows the real daylight and the seasons.")),
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
        ft=font(78,'ExtraBold'); fs=font(40,'Medium')
        y=120
        for line in wrap(d,title,ft,W-160):
            d.text((W//2,y),line,font=ft,fill=pal['title'],anchor='ma'); y+=94
        y+=18
        for line in wrap(d,sub,fs,W-200):
            d.text((W//2,y),line,font=fs,fill=pal['sub'],anchor='ma'); y+=54
        top=y+50
        shot=Image.open(f'{RAW}/{folder}/{src}.png').convert('RGB')
        if crop: shot=shot.crop(crop)
        sw=900; sh=int(shot.height*sw/shot.width); shot=shot.resize((sw,sh),Image.LANCZOS)
        if crop:
            sw2=980; shot=shot.resize((sw2,int(shot.height*sw2/shot.width)),Image.LANCZOS)
            x=(W-sw2)//2; y0=top+(H-top-shot.height)//2-60
            img.paste(shot,(x,y0),rounded(shot,44))
        else:
            vis=H-top+60  # bleed off the bottom edge
            shot=shot.crop((0,0,sw,min(sh,vis)))
            m=rounded(Image.new('RGB',(sw,shot.height+60)),56).crop((0,0,sw,shot.height))
            img.paste(shot,((W-sw)//2,top),m)
        out=f'{ROOT}/{lang}/{name}.png'; img.save(out,optimize=True); print(out, img.size, os.path.getsize(out)//1024,'KB')
