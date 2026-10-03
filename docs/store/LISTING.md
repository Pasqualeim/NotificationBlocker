# Scheda Google Play: Nook

Tutto quello che va copiato in Play Console per pubblicare Nook, già scritto e controllato. La guida su *dove* incollarlo è [`../PUBLISHING.md`](../PUBLISHING.md).

Tieni questo file allineato all'app: una nuova funzione, un nuovo permesso o un dato che esce dal telefono cambiano i testi e soprattutto le risposte sulla sicurezza dei dati.

## Dati dell'app

| Campo | Valore |
|---|---|
| Nome del pacchetto (`applicationId`) | `com.pasquale.nook` (definitivo dopo il primo caricamento) |
| Versione | `versionName` 1.0, `versionCode` 1 (in `app/build.gradle.kts`) |
| Android minimo | 9 (API 28). `targetSdk` 37 (Play chiede almeno 36 dal 31/08/2026) |
| File da caricare | `app/build/outputs/bundle/release/app-release.aab` (`./gradlew bundleRelease`) |
| Categoria | Produttività |
| Tag (fino a 5) | Produttività, Strumenti, Benessere (scegli tra quelli proposti da Play Console) |
| Prezzo | Gratis, senza acquisti in app né pubblicità |
| Email di contatto | pasquale.ercolino98@gmail.com (pubblica sulla scheda: puoi usarne una dedicata) |
| Sito web | https://pasqualeim.github.io/NotificationBlocker/ (facoltativo) |
| Informativa sulla privacy | https://pasqualeim.github.io/NotificationBlocker/ |
| Lingua predefinita della scheda | Inglese (Stati Uniti) – en-US, con traduzione Italiano – it-IT |

## Testi della scheda

Scritti con le regole di `DESIGN_SYSTEM.md` ("Testi e tono"): frasi semplici, fatti concreti, niente promesse sulla salute e niente nomi di app di altri (Gmail, Teams, Slack) per non rischiare segnalazioni sui marchi.

### Italiano (it-IT)

**Nome dell'app** (23/30)

```
Nook: stacca dal lavoro
```

**Descrizione breve** (72/80)

```
Metti in pausa le notifiche di lavoro quando stacchi. Le altre arrivano.
```

**Descrizione completa** (1571/4000)

```
Nook mette in pausa le notifiche delle app di lavoro quando stacchi. Scegli le app (per esempio la mail, la chat e il calendario dell'ufficio) e l'orario in cui non lavori. In quella fascia le loro notifiche spariscono appena arrivano. Tutte le altre, dai messaggi di famiglia alle chiamate, arrivano come sempre.

PERCHÉ
Abbiamo una vita oltre il lavoro: un pomeriggio libero, una cena, un weekend. Nook serve a non farsi raggiungere dal lavoro in quei momenti, senza spegnere tutto il telefono.

COSA FA
• Scegli quali app sono di lavoro. Le altre non vengono toccate.
• Imposti quando stacchi, anche a cavallo della mezzanotte o per tutto il giorno.
• Un interruttore accende e spegne la pausa. C'è anche un riquadro nelle Impostazioni rapide.
• Una notifica silenziosa ti ricorda che la pausa è attiva e fino a che ora. Se ti serve, la termini da lì con un tocco. È facoltativa.
• Quando torni, Nook ti dice quante notifiche di lavoro sono arrivate e da quali app.
• Una piccola scena animata segue la luce vera del giorno e le stagioni: una scrivania mentre lavori, un giardino quando stacchi.
• Tema chiaro e scuro, in italiano e in inglese.

PRIVACY
Nook ha bisogno dell'accesso alle notifiche, che dai tu dalle impostazioni di Android. L'app guarda solo quale app ha inviato ogni notifica. Non legge e non salva il contenuto, non ha account, pubblicità o statistiche e non usa internet, quindi niente esce dal telefono. Puoi togliere l'accesso quando vuoi.

DA SAPERE
Le notifiche messe in pausa vengono tolte, non rimandate. Quando la pausa finisce non ricompaiono. I messaggi restano nelle rispettive app.
```

**Note di rilascio 1.0** (101/500)

```
Prima versione di Nook: scegli le app di lavoro e quando stacchi, e le loro notifiche vanno in pausa.
```

### English (en-US)

**App name** (23/30)

```
Nook: Work-Life Balance
```

**Short description** (73/80)

```
Pause work notifications when you're off. Everything else still comes in.
```

**Full description** (1486/4000)

```
Nook pauses notifications from your work apps when you're off. Pick the apps (your work email, chat and calendar, for example) and the hours when you don't work. During those hours their notifications disappear as soon as they arrive. Everything else, from family messages to calls, comes in as usual.

WHY
We have a life beyond work: a free afternoon, a dinner, a weekend. Nook keeps work from reaching you in those moments, without switching off your whole phone.

WHAT IT DOES
• You choose which apps are work apps. The others are left alone.
• You set when you switch off, even across midnight or for the whole day.
• One switch turns the break on and off. There's also a Quick Settings tile.
• A silent notification reminds you that the break is on and when it ends. If you need to, you can end it from there with one tap. It's optional.
• When you're back, Nook tells you how many work notifications came in and from which apps.
• A small animated scene follows the real daylight and the seasons: a desk while you work, a garden when you're off.
• Light and dark theme, in English and Italian.

PRIVACY
Nook needs notification access, which you grant in Android settings. The app only checks which app sent each notification. It doesn't read or store the content, has no accounts, ads or analytics, and doesn't use the internet, so nothing leaves your phone. You can remove the access anytime.

GOOD TO KNOW
Paused notifications are removed, not postponed. They don't come back when the break ends. The messages are still in their apps.
```

**Release notes 1.0** (98/500)

```
First release of Nook: pick your work apps and when you switch off, and their notifications pause.
```

## Grafica

Tutti i file sono già nelle dimensioni richieste da Play.

| Elemento | File | Specifiche Play |
|---|---|---|
| Icona | `icon-512.png` | 512×512, PNG a 32 bit, meno di 1 MB (è 10 KB) |
| Feature graphic | `feature-graphic-it.png`, `feature-graphic-en.png` | 1024×500, PNG a 24 bit senza trasparenza. Obbligatoria |
| Screenshot telefono | `screenshots/it/*.png`, `screenshots/en/*.png` | 1080×1920 (9:16), da 2 a 8. Caricali in ordine di nome. Didascalia in alto e telefono con cornice che esce dal bordo basso |

Gli screenshot, nell'ordine:

| File | Didascalia IT | Didascalia EN |
|---|---|---|
| `01_pausa` | Il lavoro può aspettare | Work can wait |
| `02_app` | Scegli le app di lavoro | Pick your work apps |
| `03_notifica` | Tutto nella tendina | Your break at a glance |
| `04_resoconto` | Sai cosa ti aspetta | Know what's waiting |
| `05_scuro` | Anche con il tema scuro | Light or dark |

Come rifarli (dopo un cambio di interfaccia o delle scene):

```bash
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
tools/store/capture_screenshots.sh /tmp/nook-raw          # emulatore acceso, testo e tema di default
python3 tools/store/compose_screenshots.py /tmp/nook-raw path/to/Manrope.ttf
./gradlew testDebugUnitTest --tests '*ZenSceneTest*'     # scene per la feature graphic
python3 tools/store/feature_graphic.py path/to/Manrope.ttf
python3 tools/icon/launcher_icon.py                     # icona 512
```

Cose da sapere sulla cattura (`capture_screenshots.sh`, ~4 minuti):

- Serve un emulatore con le app Google (Gmail, Calendar, Drive): sono le tre "app di lavoro" delle schermate. Il lancio si ferma se Nook non arriva in Home.
- La scena e l'arte della notifica seguono l'ora vera: gira nel pomeriggio (luce piena o ora dorata). A cavallo del passaggio giorno/tramonto le due lingue potrebbero avere notifiche diverse: aspetta qualche minuto e rilancia.
- Cambia da solo la lingua di Nook e di `com.android.systemui` (data nella tendina) e le rimette a "predefinita" alla fine. "Silent" e "Mobile data" nella tendina restano in inglese anche nel set italiano: le stringhe non sono tradotte nell'immagine dell'emulatore.
- Scrive le preferenze solo a processo fermo e con il listener spento: un processo Nook vivo le riscrive con i valori vecchi.

Manrope: `Manrope[wght].ttf` da https://github.com/google/fonts/tree/main/ofl/manrope.

## Risposte ai moduli di Play Console ("Contenuti dell'app")

Risposte basate sul codice attuale (nessun permesso `INTERNET`, nessuna libreria di analisi o pubblicità, dati solo in `SharedPreferences`). Se il codice cambia, ricontrollale.

### Norme sulla privacy
URL: `https://pasqualeim.github.io/NotificationBlocker/`

### Accesso all'app
**Tutte le funzionalità sono disponibili senza restrizioni di accesso** (niente login). Nel campo delle istruzioni, facoltativo ma utile ai revisori, incolla:

```
No account is needed. To try the main feature:
1. Open Nook and tap "Open settings" on the "Allow notification access" card, then enable Nook.
2. Under "When to switch off", set a start time a few minutes before now.
3. Tap "Work apps" and select any app that can post a notification.
4. Turn on the switch on the status card. Notifications from the selected app are removed while the break is on.
Nook only reads the package name of each notification to decide whether to remove it. It never reads or stores notification content and has no internet permission.
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
  - Motivo: Nook legge il nome del pacchetto delle notifiche e lo usa solo sul telefono, senza inviarlo a nessuno. Google non considera "raccolti" i dati trattati solo sul dispositivo.
  - Il backup automatico di Android salva le impostazioni nell'account Google dell'utente, gestito dal sistema: lo sviluppatore non riceve niente.
- Con "No" il modulo salta le domande sulla crittografia e sulla cancellazione dei dati.
- Cancellazione account: non applicabile, l'app non ha account.

### Permessi
- `POST_NOTIFICATIONS`: facoltativo, mostra la notifica della pausa.
- Accesso alle notifiche (`BIND_NOTIFICATION_LISTENER_SERVICE`): lo concede l'utente nelle impostazioni di sistema. Oggi Play Console non ha un modulo di dichiarazione dedicato a questo permesso (quel modulo riguarda SMS, registro chiamate, accesso a tutti i file e pochi altri). Se in revisione chiedono spiegazioni, usa il testo di "Accesso all'app" qui sopra.

## Stato

| Voce | Stato |
|---|---|
| Testi IT/EN | Pronti |
| Icona, feature graphic, 5 screenshot IT + 5 EN | Pronti (rifatti il 02/10/2026 con le scene nuove) |
| Risposte ai moduli | Pronte, da ricontrollare in Play Console |
| Verifica marchio "Nook" | Da fare (Pasquale) |
