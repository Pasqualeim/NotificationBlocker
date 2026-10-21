# Scheda Google Play: Unclock

Tutto quello che va copiato in Play Console per pubblicare Unclock, già scritto e controllato. La guida su *dove* incollarlo è [`../PUBLISHING.md`](../PUBLISHING.md).

Tieni questo file allineato all'app: una nuova funzione, un nuovo permesso o un dato che esce dal telefono cambiano i testi e soprattutto le risposte sulla sicurezza dei dati.

## Dati dell'app

| Campo | Valore |
|---|---|
| Nome del pacchetto (`applicationId`) | `com.pasquale.freetime` (dal 20/10/2026, nuova app; senza il nome del marchio apposta; definitivo dopo il primo caricamento. La prima app `com.pasquale.nook` è stata abbandonata) |
| Versione | `versionName` 1.0, `versionCode` 2 (in `app/build.gradle.kts`) |
| Android minimo | 9 (API 28). `targetSdk` 37 (Play chiede almeno 36 dal 31/08/2026) |
| File da caricare | `app/build/outputs/bundle/release/app-release.aab` (`./gradlew bundleRelease`) |
| Categoria | Produttività |
| Tag (fino a 5) | Produttività, Strumenti, Benessere (scegli tra quelli proposti da Play Console) |
| Prezzo | Gratis, senza acquisti in app né pubblicità |
| Email di contatto | pasquale.ercolino98@gmail.com (pubblica sulla scheda: puoi usarne una dedicata) |
| Sito web | https://unclock.netlify.app/it/ (facoltativo) |
| Informativa sulla privacy (URL da inserire in Play Console) | https://unclock.netlify.app/it/privacy/ (dal 20/10/2026; la copia GitHub Pages resta per l'app fino alla prossima release) |
| Lingua predefinita della scheda | Italiano – it-IT (scelta alla creazione della nuova app, 21/10/2026), con traduzione Inglese (Stati Uniti) – en-US |

## Testi della scheda

Scritti con le regole di `DESIGN_SYSTEM.md` ("Testi e tono"): frasi semplici, fatti concreti, niente promesse sulla salute e niente nomi di app di altri (Gmail, Teams, Slack) per non rischiare segnalazioni sui marchi.

### Italiano (it-IT)

**Nome dell'app** (23/30)

```
Unclock: stacca dal lavoro
```

**Descrizione breve** (78/80)

```
Le notifiche di lavoro in pausa quando stacchi. Le altre arrivano come sempre.
```

**Descrizione completa** (1902/4000)

```
Unclock mette in pausa le notifiche delle app di lavoro quando non lavori. Scegli le app e dici quando lavori: sera, weekend e giorni liberi sono tempo tuo. Messaggi di famiglia, amici e chiamate arrivano come sempre.

COSA FA
• Scegli le app di lavoro. Le altre non vengono toccate.
• Dici quando lavori. Tutto quello che sta fuori da giorni e orari è pausa, sera e weekend compresi.
• Aggiungi altre pause, per esempio il pranzo, con nome, giorni e orari. Se vuoi, ognuna ha le sue app.
• "Pausa subito" per staccare adesso, per il tempo che scegli.
• Giorni liberi con le date e, se vuoi, i festivi italiani.

CONTROLLO
• Un interruttore accende e spegne tutte le pause. Lo trovi anche nelle Impostazioni rapide e in un widget per la schermata Home.
• Durante una pausa puoi avere una notifica silenziosa che dice fino a che ora dura. Con "Termina la pausa" la chiudi con un tocco. È facoltativa.
• Finita la pausa, un riepilogo dice quante notifiche di lavoro sono arrivate e da quali app, con il tasto Apri.

STATISTICHE
• Le ore di tempo libero che hai vissuto. Il sonno non conta.
• Quante di quelle ore hanno avuto la luce del giorno, e quante notifiche sono rimaste fuori.
• Per giorno, settimana e mese. Restano sul telefono.

ASPETTO
• Una scena animata segue la luce vera del giorno e le stagioni: una scrivania mentre lavori, un pontile sul lago quando stacchi.
• Tema chiaro e scuro, in italiano e in inglese.

PRIVACY
Unclock chiede solo l'accesso alle notifiche, che dai tu dalle impostazioni di Android. Vede solo quale app ha inviato ogni notifica. Non legge e non salva il contenuto. Non ha account, pubblicità o tracciamento e non usa internet, quindi niente esce dal telefono. Puoi togliere l'accesso quando vuoi.

DA SAPERE
Le notifiche messe in pausa vengono tolte, non rimandate. Quando la pausa finisce non ricompaiono. I messaggi restano nelle rispettive app, dove li ritrovi.
```

**Note di rilascio 1.0** (101/500)

```
Prima versione di Unclock: scegli le app di lavoro e quando stacchi, e le loro notifiche vanno in pausa.
```

### English (en-US)

**App name** (23/30)

```
Unclock: Work-Life Balance
```

**Short description** (73/80)

```
Pause work notifications when you're off. Everything else still comes in.
```

**Full description** (2128/4000)

```
Unclock pauses notifications from your work apps when you're off. Pick the apps (your work email, chat and calendar, for example) and set up your breaks. During a break their notifications disappear as soon as they arrive. Everything else, from family messages to calls, comes in as usual.

WHY
We have a life beyond work: a free afternoon, a dinner, a weekend. Unclock keeps work from reaching you in those moments, without switching off your whole phone.

WHAT IT DOES
• You choose which apps are work apps. The others are left alone.
• You set when you work (days and hours): outside it Unclock pauses your work apps, evenings and weekends included. If you like, add other breaks (lunch) with their own name, hours and days; a break can have its own apps.
• "Pause now" to switch off right away, for as long as you choose, without creating a break.
• Days off: holidays or long weekends by date, and Italian public holidays automatically, if you want.
• One switch turns all your breaks on and off. There's also a Quick Settings tile and a home screen widget.
• A silent notification reminds you that the break is on and when it ends. If you need to, you can end it from there with one tap. It's optional.
• When you're back, Unclock tells you how many work notifications came in and from which apps, and you can open them from there.
• "Stats": how many hours of break you lived (sleep not counted), how many in daylight and how many notifications were kept out, by day, week and month. It stays on your phone.
• A small animated scene follows the real daylight and the seasons: a desk while you work, a lake pier when you're off.
• Light and dark theme, in English and Italian.

PRIVACY
Unclock needs notification access, which you grant in Android settings. The app only checks which app sent each notification. It doesn't read or store the content, has no accounts, ads or analytics, and doesn't use the internet, so nothing leaves your phone. You can remove the access anytime.

GOOD TO KNOW
Paused notifications are removed, not postponed. They don't come back when the break ends. The messages are still in their apps.
```

**Release notes 1.0** (98/500)

```
First release of Unclock: pick your work apps and when you switch off, and their notifications pause.
```

## Grafica

Tutti i file sono già nelle dimensioni richieste da Play.

| Elemento | File | Specifiche Play |
|---|---|---|
| Icona | `icon-512.png` | 512×512, PNG a 32 bit, meno di 1 MB (è 10 KB) |
| Feature graphic | `feature-graphic-it.png`, `feature-graphic-en.png` | 1024×500, PNG a 24 bit senza trasparenza. Obbligatoria. Rifatta il 21/10/2026: lucchetto, "Unclock" in Fraunces, "Il tempo che ti torna" / "The time you get back" in Inter (i font del sito), pontile al tramonto d'inverno |
| Screenshot telefono | `screenshots/it/*.png`, `screenshots/en/*.png` | 1080×1920 (9:16), da 2 a 8. Caricali in ordine di nome. Didascalia in alto e telefono con cornice che esce dal bordo basso |

Gli screenshot, nell'ordine (elenco del 05/10/2026: `capture_screenshots.sh` ora scrive i file di Nook 2, ma `compose_screenshots.py` e questa tabella vanno ancora aggiornati):

| File | Didascalia IT | Didascalia EN |
|---|---|---|
| `01_pausa` | Il lavoro può aspettare | Work can wait |
| `02_app` | Scegli le app di lavoro | Pick your work apps |
| `03_notifica` | Sai che la pausa è attiva | Always know it's on |
| `04_resoconto` | Poi vedi cosa è arrivato | See what you missed |
| `05_scuro` | Chiaro e scuro | Light or dark, your call |

Come rifarli (dopo un cambio di interfaccia o delle scene):

```bash
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
tools/store/capture_screenshots.sh /tmp/nook-raw          # emulatore acceso, testo e tema di default; LANGS=it-IT per una lingua sola. Scrive 1_break, 2_shade, 3_apps, 4_report, 5_dark, 6_breaks, 7_stats (dati demo: tools/store/demo_prefs.py)
python3 tools/store/compose_screenshots.py /tmp/nook-raw path/to/Manrope.ttf
./gradlew testDebugUnitTest --tests '*ZenSceneTest*'     # scene per la feature graphic
python3 tools/store/feature_graphic.py                    # legge i font da ../nook-site/public/assets/fonts
python3 tools/icon/launcher_icon.py                     # icona 512
```

Cose da sapere sulla cattura (`capture_screenshots.sh`, ~4 minuti):

- Serve un emulatore con le app Google (Gmail, Calendar, Drive): sono le tre "app di lavoro" delle schermate. Il lancio si ferma se Unclock non arriva in Home.
- L'orologio dell'emulatore viene spostato (`cmd alarm set-time`, ora automatica spenta) a un giorno feriale alle 18:15 e a un lunedì alle 09:40: la scena non dipende dall'ora in cui lanci lo script. Rimetti l'ora automatica dopo.
- Cambia da solo la lingua di Unclock e di `com.android.systemui` (data nella tendina) e le rimette a "predefinita" alla fine. "Silent" e "Mobile data" nella tendina restano in inglese anche nel set italiano: le stringhe non sono tradotte nell'immagine dell'emulatore.
- Scrive le preferenze solo a processo fermo e con il listener spento: un processo Unclock vivo le riscrive con i valori vecchi.

Manrope: `Manrope[wght].ttf` da https://github.com/google/fonts/tree/main/ofl/manrope.

## Risposte ai moduli di Play Console ("Contenuti dell'app")

Risposte basate sul codice attuale (nessun permesso `INTERNET`, nessuna libreria di analisi o pubblicità, dati solo in `SharedPreferences`). Se il codice cambia, ricontrollale.

### Norme sulla privacy
URL: `https://unclock.netlify.app/it/privacy/`

### Accesso all'app
**Tutte le funzionalità sono disponibili senza restrizioni di accesso** (niente login). Nel campo delle istruzioni, facoltativo ma utile ai revisori, incolla:

```
No account is needed. To try the main feature:
1. Open Unclock and tap "Open settings" on the "Allow notification access" card, then enable Unclock.
2. Open the "Breaks" tab and edit the first break: set its start a few minutes before now and its end later (or use the same time for both, which is the whole day).
3. Tap "Work apps" and select any app that can post a notification.
4. Back on the "Today" tab, turn on the switch on the status card. Notifications from the selected app are removed while the break is on.
Unclock only reads the package name of each notification to decide whether to remove it. It never reads or stores notification content and has no internet permission.
```

### Annunci
**No**, l'app non contiene annunci.

### Classificazione dei contenuti
- Categoria: **Tutte le altre tipologie di app** (utilità, produttività, comunicazione o altro).
- Violenza, sesso, linguaggio, sostanze, gioco d'azzardo: **No** a tutto.
- Interazione tra utenti, condivisione della posizione, acquisti digitali: **No**.
- Risultato atteso: PEGI 3 / Everyone.

### Pubblico di destinazione
- Fasce d'età: **18 anni e oltre** (è un'app per chi lavora). Così non si applicano le regole "Famiglie".
- L'app attira i bambini? **No**.

### App di notizie, app governative, funzionalità finanziarie, salute
**No** a tutte.

### Sicurezza dei dati (Data safety)
- L'app raccoglie o condivide uno dei tipi di dati utente richiesti? **No.**
  - Motivo: Unclock legge il nome del pacchetto delle notifiche e lo usa solo sul telefono, senza inviarlo a nessuno. Google non considera "raccolti" i dati trattati solo sul dispositivo.
  - Il backup automatico di Android salva le impostazioni e lo storico del tempo libero nell'account Google dell'utente, gestito dal sistema: lo sviluppatore non riceve niente.
- Con "No" il modulo salta le domande sulla crittografia e sulla cancellazione dei dati.
- Cancellazione account: non applicabile, l'app non ha account.

### Permessi
- `POST_NOTIFICATIONS`: facoltativo, mostra la notifica della pausa.
- Accesso alle notifiche (`BIND_NOTIFICATION_LISTENER_SERVICE`): lo concede l'utente nelle impostazioni di sistema. Oggi Play Console non ha un modulo di dichiarazione dedicato a questo permesso (quel modulo riguarda SMS, registro chiamate, accesso a tutti i file e pochi altri). Se in revisione chiedono spiegazioni, usa il testo di "Accesso all'app" qui sopra.

## Stato

| Voce | Stato |
|---|---|
| Testi IT/EN | Pronti |
| Icona e feature graphic pronte (21/10/2026). 5 screenshot IT + 5 EN | Vecchi (05/10/2026: Home con orari e pulsante in basso, prima delle tre schede): da rifare prima della pubblicazione |
| Risposte ai moduli | Pronte, da ricontrollare in Play Console |
| Verifica marchio "Unclock" | Da fare (Pasquale) |
