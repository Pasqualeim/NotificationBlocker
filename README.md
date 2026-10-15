# Nook

*Prima si chiamava NotificationBlocker: la repo e il package Kotlin mantengono il vecchio nome.*

App Android che mette in pausa le notifiche delle app di lavoro quando non stai lavorando.

L'idea: abbiamo una vita oltre il lavoro. Non si tratta di dormire tranquilli, ma di riprendersi il tempo che resta (un pomeriggio di sole, una cena, un weekend) senza che il lavoro bussi dal telefono.

Scegli le app di lavoro (Gmail, Teams, Slack…), crea le tue pause (nome, orari, giorni, se vuoi app diverse per ciascuna) e accendi l'interruttore: durante una pausa le notifiche di quelle app vengono tolte appena arrivano, tutte le altre restano.

## Funzionalità

Tre schede in basso: **Oggi**, **Pause**, **Il tempo tuo**.

- **Pause** (scheda *Pause*): quante ne vuoi, ognuna con nome (Pranzo, Sera, Weekend), inizio e fine con il time picker (anche a cavallo della mezzanotte; inizio uguale alla fine = tutto il giorno), giorni della settimana e un interruttore proprio. Di norma usano tutte le app di lavoro; per una pausa si possono scegliere app diverse. Le pause possono sovrapporsi.
- **App di lavoro**: elenco ricercabile di tutte le app con icona nel launcher, comprese quelle preinstallate. Filtro *Tutte / Selezionate*, skeleton di caricamento e stato vuoto.
- **Oggi**: card di stato con l'interruttore generale (Spento, Programmato, In pausa ora), la scena animata e le pause di oggi su una barra di 24 ore con il tempo vissuto finora. Se manca l'accesso alle notifiche, una card spiega cosa vede l'app e porta alle impostazioni di sistema (ricontrollato a ogni ritorno nell'app).
- **Il tempo tuo**: storico per giorno del tempo di pausa vissuto, di quanto è stato con la luce del sole e delle notifiche di lavoro lasciate fuori. Grafici per giorno, settimana e mese, scorrendo ai periodi precedenti.
- **Scena animata**: una scrivania in ufficio con lo skyline mentre lavori, un pontile sul lago in pausa, con la luce reale del giorno, quattro stagioni, pioggia e neve.
- **Notifica della pausa** (facoltativa, Android 13+): notifica silenziosa e fissa mentre una pausa è attiva, con l'ora in cui finisce e quante notifiche di lavoro sono state messe in pausa. Il pulsante **Termina la pausa** fa arrivare di nuovo le notifiche di lavoro fino alla prossima pausa.
- **Riquadro "Nook"** nelle Impostazioni rapide: accende e spegne tutto dalla tendina.
- **Dopo la pausa**: la Home mostra quante notifiche di lavoro sono arrivate e da quali app.
- **Tema**: palette calda e desaturata, chiara ("Chai") e scura ("Lo-fi night"), font Manrope, edge-to-edge.
- **Lingue**: italiano e inglese.

Prossimi passi: il personaggio (un riccio, non ancora disegnato), il sito con l'iscrizione al test, poi Google Play.

## Requisiti

- Android 9 (API 28) o superiore
- Accesso alle notifiche, concesso dall'utente nelle impostazioni di sistema

## Primo utilizzo

1. Apri Nook e tocca **Apri impostazioni** nella card "Consenti l'accesso alle notifiche", poi attiva *Nook* nell'elenco di sistema e torna indietro.
2. (Facoltativo, Android 13+) Tocca **Consenti** sulla card *Notifica della pausa* per vedere la pausa nella tendina.
3. Nella scheda **Pause** modifica la pausa di partenza (predefinita 22:00 → 07:00, ogni giorno) o aggiungine altre.
4. Nella stessa scheda tocca **App di lavoro** e attiva le app da mettere in pausa.
5. In **Oggi** accendi l'interruttore nella card di stato.

> Le notifiche messe in pausa vengono tolte, non rimandate: quando la fascia finisce non ricompaiono. I messaggi restano dentro le rispettive app.

## Come funziona

Un `NotificationListenerService` riceve ogni notifica pubblicata e applica una sola regola:

```
interruttore acceso  &&  una pausa in corso adesso (non terminata)  &&  app tra quelle della pausa  →  rimuovi
```

Le pause vengono valutate **al momento dell'arrivo della notifica** leggendo l'orologio: niente allarmi né servizi in background che possono sfasarsi dopo un riavvio o in Doze.

## Sviluppo

Serve Android Studio (JDK incluso) o un JDK 17+. La CI (`.github/workflows/ci.yml`) esegue build, test e lint a ogni push.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"   # se java non è nel PATH (macOS)

./gradlew assembleDebug                 # APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest             # test unitari (pause e regola di blocco, storico, statistiche, rapporto, notifica e riquadro)
./gradlew lintDebug                     # analisi statica, deve riportare 0 errori
./gradlew connectedDebugAndroidTest     # test strumentali, serve emulatore o dispositivo (disinstalla l'app alla fine)
./gradlew bundleRelease                 # AAB per il Play Store, firmato se esiste keystore.properties
```

Installazione su emulatore: `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Per giudicare la fluidità usa la release (`assembleRelease`): la debug è circa due volte più lenta.

### Stack tecnico

| Area | Scelta |
|---|---|
| Linguaggio | Kotlin 2.4 (Kotlin integrato in AGP 9) |
| UI | Jetpack Compose, Material 3, `material-icons-core` |
| Navigazione | Navigation 3 (`androidx.navigation3`) |
| Stato | `AndroidViewModel` + `StateFlow`, `collectAsStateWithLifecycle` |
| Persistenza | `SharedPreferences` |
| Font | Manrope via Google Fonts scaricabili |
| Build | Gradle 9.8, version catalog, minSdk 28, targetSdk 37, compileSdk 37 |

### Documentazione

| File | Contenuto |
|---|---|
| [`CLAUDE.md`](CLAUDE.md) | Architettura, regole e convenzioni (anche per assistenti AI) |
| [`docs/DESIGN_SYSTEM.md`](docs/DESIGN_SYSTEM.md) | Stile visivo: colori, tipografia, forme, componenti, tono dei testi |
| [`docs/MOTION.md`](docs/MOTION.md) | Linee guida sulle animazioni |
| [`docs/REDESIGN.md`](docs/REDESIGN.md) | Piano di Nook 2 (pause, storico, tre schede; il riccio è la fase 4) |
| [`docs/CHARACTER.md`](docs/CHARACTER.md) | Studio del personaggio: scelto il riccio, ancora da disegnare |
| [`docs/ROADMAP.md`](docs/ROADMAP.md) | Stato del progetto: fase di test, miglioramenti, pubblicazione, cosa è fatto |
| [`docs/PUBLISHING.md`](docs/PUBLISHING.md) | Guida passo passo per pubblicare su Google Play (riutilizzabile per altre app) |
| [`docs/store/LISTING.md`](docs/store/LISTING.md) | Scheda dello store: testi IT/EN, screenshot, risposte ai moduli di Play Console |

Il sito dell'app (presentazione e iscrizione al test chiuso) è in un repository separato, `nook-site`: HTML statico su Cloudflare Pages, iscrizioni in Cloudflare D1.

## Privacy

L'app legge solo il **nome del pacchetto** delle notifiche in arrivo, per decidere se rimuoverle. Non legge, salva né invia il contenuto delle notifiche. Le impostazioni restano sul dispositivo e l'app non ha backend né analytics. Informativa completa: https://pasqualeim.github.io/NotificationBlocker/
