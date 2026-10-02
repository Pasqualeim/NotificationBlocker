# Nook

*Prima si chiamava NotificationBlocker: la repo e il package Kotlin mantengono il vecchio nome.*

App Android che mette in pausa le notifiche delle app di lavoro quando non stai lavorando.

L'idea: abbiamo una vita oltre il lavoro. Non si tratta di dormire tranquilli, ma di riprendersi il tempo che resta (un pomeriggio di sole, una cena, un weekend) senza che il lavoro bussi dal telefono.

Scegli le app di lavoro (Gmail, Teams, Slack…), imposta quando stacchi (predefinito 22:00 → 07:00) e accendi l'interruttore: in quella fascia le notifiche di quelle app vengono tolte appena arrivano, tutte le altre restano.

## Funzionalità

- **App di lavoro**: elenco ricercabile di tutte le app con icona nel launcher, comprese quelle preinstallate. Filtro *Tutte / Selezionate*, sezioni separate per le app già scelte, skeleton di caricamento e stato vuoto.
- **Quando staccare**: inizio e fine con il time picker Material 3, anche a cavallo della mezzanotte. Con inizio uguale alla fine la pausa vale tutto il giorno. Una timeline a 24 ore mostra la fascia, le ore libere e quanta luce del sole contiene.
- **Interruttore**: accende o spegne la pausa in un tocco, senza perdere le impostazioni. La campanella nella card di stato suona e viene barrata quando si accende.
- **Stato a colpo d'occhio**: tre stati (Spento, Programmato, In pausa ora) con colori e testi diversi.
- **Permesso spiegato**: se manca l'accesso alle notifiche, una card dice cosa vede l'app e porta alle impostazioni di sistema. Viene ricontrollato ogni volta che torni nell'app.
- **Scena animata**: una scrivania in ufficio con lo skyline dietro mentre lavori, un pontile sul lago quando stacchi, con la luce reale del giorno, quattro stagioni, pioggia e neve.
- **Notifica della pausa** (facoltativa, Android 13+): notifica silenziosa e fissa mentre la pausa è attiva, con l'ora in cui finisce e quante notifiche di lavoro sono in attesa.
- **Riquadro "Nook"** nelle Impostazioni rapide: accende e spegne la pausa dalla tendina.
- **Durante la pausa**: quando la fascia finisce, la Home mostra quante notifiche di lavoro sono arrivate e da quali app.
- **Tema**: palette calda e desaturata, chiara ("Chai") e scura ("Lo-fi night"), font Manrope, edge-to-edge.
- **Lingue**: italiano e inglese.

## Requisiti

- Android 9 (API 28) o superiore
- Accesso alle notifiche, concesso dall'utente nelle impostazioni di sistema

## Primo utilizzo

1. Apri Nook e tocca **Apri impostazioni** nella card "Consenti l'accesso alle notifiche", poi attiva *Nook* nell'elenco di sistema e torna indietro.
2. (Facoltativo, Android 13+) Tocca **Consenti** sulla card *Notifica della pausa* per vedere la pausa nella tendina.
3. In **Quando staccare** tocca gli orari di inizio e fine.
4. Tocca **App di lavoro** in basso e attiva le app da mettere in pausa.
5. Accendi l'interruttore nella card di stato.

> Le notifiche messe in pausa vengono tolte, non rimandate: quando la fascia finisce non ricompaiono. I messaggi restano dentro le rispettive app.

## Come funziona

Un `NotificationListenerService` riceve ogni notifica pubblicata e applica una sola regola:

```
blocco attivo  &&  ora corrente dentro la fascia  &&  app nella lista  →  rimuovi
```

La fascia viene valutata **al momento dell'arrivo della notifica** leggendo l'orologio: niente allarmi né servizi in background che possono sfasarsi dopo un riavvio o in Doze.

## Sviluppo

Serve Android Studio (JDK incluso) o un JDK 17+. La CI (`.github/workflows/ci.yml`) esegue build, test e lint a ogni push.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"   # se java non è nel PATH (macOS)

./gradlew assembleDebug                 # APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest             # test unitari (fascia oraria, regola di blocco, rapporto del mattino, notifica e riquadro)
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
| [`docs/ROADMAP.md`](docs/ROADMAP.md) | Stato del progetto: cosa manca per pubblicare, miglioramenti, cosa è fatto |
| [`docs/PUBLISHING.md`](docs/PUBLISHING.md) | Guida passo passo per pubblicare su Google Play (riutilizzabile per altre app) |
| [`docs/store/LISTING.md`](docs/store/LISTING.md) | Scheda dello store: testi IT/EN, screenshot, risposte ai moduli di Play Console |

## Privacy

L'app legge solo il **nome del pacchetto** delle notifiche in arrivo, per decidere se rimuoverle. Non legge, salva né invia il contenuto delle notifiche. Le impostazioni restano sul dispositivo e l'app non ha backend né analytics. Informativa completa: https://pasqualeim.github.io/NotificationBlocker/
