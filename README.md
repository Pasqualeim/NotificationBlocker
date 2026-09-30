# Nook

*Prima si chiamava NotificationBlocker: la repo mantiene il vecchio nome.*

App Android che silenzia le notifiche delle app di lavoro quando non stai lavorando.

L'idea: abbiamo una vita oltre il lavoro. Non si tratta di dormire tranquilli, ma di riprendersi il tempo che resta (un pomeriggio di sole, una cena, un weekend) senza che il lavoro bussi dal telefono.

Scegli le app da bloccare (Gmail, Teams, Slack…), imposta la fascia oraria di "fuori orario" (predefinita 22:00 → 07:00) e attiva il blocco: durante quella fascia le notifiche di quelle app vengono rimosse appena arrivano, mentre tutte le altre restano visibili.

## Funzionalità

- **Selezione app**: elenco ricercabile di tutte le app con icona nel launcher, comprese quelle preinstallate. Filtro *Tutte / Selezionate*, sezioni separate per le app già bloccate, skeleton di caricamento e stato vuoto illustrato.
- **Fascia oraria**: inizio e fine configurabili con time picker Material 3, anche a cavallo della mezzanotte. Con inizio uguale alla fine il blocco vale tutto il giorno. Una timeline a 24 ore mostra la fascia e la durata del silenzio.
- **Interruttore principale**: attiva o disattiva il blocco in un tocco, senza perdere le impostazioni. La campanella nella card di stato "suona" e viene barrata quando il blocco si attiva.
- **Stato in tempo reale**: tre stati visivi (disattivato, attivo fuori fascia, attivo dentro la fascia) con colori e testi dedicati.
- **Permesso guidato**: se manca l'accesso alle notifiche compare una card che porta direttamente alle impostazioni di sistema. Viene ricontrollato ogni volta che torni nell'app.
- **Scena animata**: una scrivania sulla città durante il lavoro, un giardino giapponese fuori orario, con luce reale, quattro stagioni e pioggia.
- **Notifica zen**: notifica silenziosa e fissa, mostrata mentre il blocco è attivo dentro la fascia, con un messaggio calmo, la fine della fascia e il conteggio delle notifiche trattenute (Android 13+, facoltativa).
- **Riquadro Impostazioni rapide** "Stacco & Sole": attiva o disattiva il blocco dalla tendina.
- **Rapporto del mattino**: a fine fascia mostra quante notifiche di lavoro hanno aspettato fuori e da quali app.
- **Tema**: palette calda e desaturata "Chai" (chiara) e "Lo-fi night" (scura): terracotta morbida, salvia e blu polvere, font Manrope, edge-to-edge.
- **Lingue**: inglese e italiano.

## Requisiti

- Android 9 (API 28) o superiore
- Accesso alle notifiche concesso all'app (richiesto al primo avvio)

## Primo utilizzo

1. Apri l'app e tocca **Concedi l'accesso alle notifiche**, poi abilita *Nook* nelle impostazioni di sistema.
2. Imposta inizio e fine della fascia di blocco.
3. Tocca **App da silenziare** e attiva le app di lavoro.
   (Facoltativo, Android 13+: tocca **Consenti** sulla card *Notifica zen* per vedere lo stato nella tendina.)
4. Attiva l'interruttore **Blocco notifiche**.

> Le notifiche bloccate vengono eliminate, non rimandate: quando la fascia finisce non ricompaiono. I messaggi restano comunque disponibili dentro le rispettive app.

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
./gradlew connectedDebugAndroidTest     # test strumentali, serve emulatore o dispositivo
./gradlew bundleRelease                 # AAB per il Play Store, firmato se esiste keystore.properties
```

Installazione su emulatore: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

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
| [`docs/ROADMAP.md`](docs/ROADMAP.md) | Miglioramenti pianificati, in ordine di priorità |

## Privacy

L'app legge solo il **nome del pacchetto** delle notifiche in arrivo, per decidere se rimuoverle. Non legge, salva né invia il contenuto delle notifiche. Le impostazioni restano sul dispositivo e l'app non ha backend né analytics. Informativa completa: https://pasqualeim.github.io/NotificationBlocker/
