# Unclock

*Prima si chiamava NotificationBlocker: la repo e il package Kotlin mantengono il vecchio nome.*

App Android che mette in pausa le notifiche delle app di lavoro quando non stai lavorando.

La domanda da cui nasce: perché esistono app che ti premiano e ti mostrano statistiche solo per quanto tempo ti concentri e lavori, mentre non esiste niente per il tempo libero e la salute personale? Unclock conta il tempo che ti riprendi (vedi [`docs/IDEA.md`](docs/IDEA.md)).

Abbiamo una vita oltre il lavoro. Non si tratta di dormire tranquilli, ma di riprendersi il tempo che resta (un pomeriggio di sole, una cena, un weekend) senza che il lavoro bussi dal telefono.

Scegli le app di lavoro (Gmail, Teams, Slack…), dici quando lavori (giorni e orari) e accendi l'interruttore: tutto il resto è pausa, e durante una pausa le notifiche di quelle app vengono tolte appena arrivano, tutte le altre restano. Se vuoi, aggiungi altre pause (il pranzo) con nome, orari, giorni e app diverse.

## Funzionalità

Tre schede in basso: **Oggi**, **Pause**, **Statistiche**.

- **Quando lavori** (scheda *Pause*): giorni e orari di lavoro (predefinito lun–ven 09:00–18:00, turni di giorno). Fuori dal lavoro Unclock mette tutto in pausa, sera e weekend compresi: da venerdì sera a lunedì mattina è una pausa sola, quindi "Termina la pausa", il conteggio e il resoconto valgono per tutto il weekend.
- **Altre pause** (scheda *Pause*): quante ne vuoi (il pranzo), ognuna con nome, inizio e fine con il time picker (anche a cavallo della mezzanotte; inizio uguale alla fine = tutto il giorno), giorni della settimana e un interruttore proprio. Di norma usano tutte le app di lavoro; per una pausa si possono scegliere app diverse. Le pause possono sovrapporsi.
- **Stacca ora**: dalla card di stato, una pausa subito per il tempo che scegli, senza crearne una.
- **Giorni liberi**: ferie o ponti con le date (fino a 62 giorni) e, se vuoi, i festivi italiani, Pasqua e Pasquetta comprese.
- **App di lavoro**: elenco ricercabile di tutte le app con icona nel launcher, comprese quelle preinstallate. Filtro *Tutte / Selezionate*, skeleton di caricamento e stato vuoto.
- **Oggi**: card di stato con l'interruttore generale (Spento, Programmato, In pausa ora), la scena animata e le pause di oggi su una barra di 24 ore con il tempo vissuto finora. Se manca l'accesso alle notifiche, una card spiega cosa vede l'app e porta alle impostazioni di sistema (ricontrollato a ogni ritorno nell'app).
- **Statistiche**: storico per giorno del tempo di pausa vissuto, di quanto è stato con la luce del sole e delle notifiche di lavoro lasciate fuori. Grafici per giorno, settimana e mese, scorrendo ai periodi precedenti. In fondo la settimana impostata e il **Sonno**: le ore di sonno restano in pausa ma non contano come tempo libero.
- **Scena animata**: una scrivania in ufficio con lo skyline mentre lavori, un pontile sul lago in pausa, con la luce reale del giorno, quattro stagioni, pioggia e neve.
- **Notifica della pausa** (facoltativa, Android 13+): notifica silenziosa e fissa mentre una pausa è attiva, con l'ora in cui finisce e quante notifiche di lavoro sono state messe in pausa. Il pulsante **Termina la pausa** fa arrivare di nuovo le notifiche di lavoro fino alla prossima pausa.
- **Riquadro "Unclock"** nelle Impostazioni rapide: accende e spegne tutto dalla tendina.
- **Widget** per la schermata Home: stato, pausa in corso o prossima e "Stacca 1 h".
- **Dopo la pausa**: la Home mostra quante notifiche di lavoro sono arrivate e da quali app, con "Apri".
- **Primo avvio**: tre passi (app di lavoro, quando lavori, accesso alle notifiche con le quattro garanzie).
- **Tema**: aspetto nativo Pixel (Material 3 Expressive) nei colori di Unclock, palette calda "Chai" / "Lo-fi night" (niente colori dallo sfondo), font di sistema; edge-to-edge.
- **Lingue**: italiano e inglese.

Prossimi passi: il sito con l'iscrizione al test, poi Google Play.

## Requisiti

- Android 9 (API 28) o superiore
- Accesso alle notifiche, concesso dall'utente nelle impostazioni di sistema

## Primo utilizzo

1. Apri Unclock: il primo avvio ha tre passi. Scegli le **app di lavoro**, poi **quando lavori** (giorni e orari, con il pranzo facoltativo) e infine consenti l'**accesso alle notifiche**: *Consenti l'accesso*, attiva *Unclock* nell'elenco di sistema e torna indietro.
2. (Facoltativo, Android 13+) Toccando *Inizia* Android chiede se Unclock può mostrare la *notifica della pausa* nella tendina.
3. Nella scheda **Pause** puoi cambiare giorni e orari di lavoro, aggiungere altre pause o i giorni liberi e cambiare le app di lavoro (riga *App di lavoro*).
4. In **Oggi** l'interruttore nella card di stato è già acceso; **Stacca ora** avvia subito una pausa.

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
| UI | Jetpack Compose, Material 3 Expressive (`material3` 1.5.0-beta01), `material-icons-core` |
| Navigazione | Navigation 3 (`androidx.navigation3`) |
| Stato | `AndroidViewModel` + `StateFlow`, `collectAsStateWithLifecycle` |
| Persistenza | `SharedPreferences` |
| Font | Font di sistema del telefono |
| Build | Gradle 9.8, version catalog, minSdk 28, targetSdk 37, compileSdk 37 |

### Documentazione

| File | Contenuto |
|---|---|
| [`CLAUDE.md`](CLAUDE.md) | Architettura, regole e convenzioni (anche per assistenti AI) |
| [`docs/DESIGN_SYSTEM.md`](docs/DESIGN_SYSTEM.md) | Stile visivo: colori, tipografia, forme, componenti, tono dei testi |
| [`docs/MOTION.md`](docs/MOTION.md) | Linee guida sulle animazioni |
| [`docs/IDEA.md`](docs/IDEA.md) | La domanda da cui nasce Unclock, le quattro regole e la checklist anti-stress |
| [`docs/REDESIGN.md`](docs/REDESIGN.md) | Piano di Nook 2 (pause, storico, tre schede) |
| [`docs/ROADMAP.md`](docs/ROADMAP.md) | Stato del progetto: fase di test, miglioramenti, pubblicazione, cosa è fatto |
| [`docs/PUBLISHING.md`](docs/PUBLISHING.md) | Guida passo passo per pubblicare su Google Play (riutilizzabile per altre app) |
| [`docs/store/LISTING.md`](docs/store/LISTING.md) | Scheda dello store: testi IT/EN, screenshot, risposte ai moduli di Play Console |

Il sito dell'app (presentazione e iscrizione al test chiuso) è in un repository separato, `nook-site`: HTML statico su Netlify, iscrizioni con Netlify Forms.

## Privacy

L'app legge solo il **nome del pacchetto** delle notifiche in arrivo, per decidere se rimuoverle. Non legge, salva né invia il contenuto delle notifiche. Le impostazioni restano sul dispositivo e l'app non ha backend né analytics. Informativa completa: https://pasqualeim.github.io/NotificationBlocker/
