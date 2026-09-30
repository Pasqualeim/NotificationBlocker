# Roadmap (Nook)

Miglioramenti pianificati per rendere l'app più "Android nativa", animata e pronta per il Play Store. Ordinati per priorità dentro ogni sezione. Quando completi o scarti una voce, aggiornala qui.

Legenda costo: **S** < 1h · **M** mezza giornata · **L** più giorni.

## 1. Fondamenta (prima di tutto il resto)

| Voce | Costo | Note |
|---|---|---|
| Verifica del marchio "Nook" | S | Prima di pubblicare. NOOK è di Barnes & Noble: cerca su EUIPO/UIBM e USPTO (classi 9 e 42) e sulla ricerca di Play. Piano B e riserve in `DESIGN_SYSTEM.md` ("Nome") |

## 2. Look & feel Android

| Voce | Costo | Note |
|---|---|---|
| Transizioni di navigazione + predictive back | S | `transitionSpec` / `popTransitionSpec` in `NavDisplay`, `android:enableOnBackInvokedCallback="true"` |
| Haptic su toggle e selezione app | S | `LocalHapticFeedback` |
| Splash screen API | S | `androidx.core:core-splashscreen`, icona con sfondo `background` |
| Lingua per app | S | `locales_config.xml` → l'utente sceglie italiano/inglese dalle impostazioni di sistema (Android 13+) |
| Material 3 Expressive | M | `MotionScheme.expressive()`, componenti espressivi dove hanno senso. Richiede BOM aggiornata |
| Sole → luna, timeline "ora" animata, `animateItem()` | M | Vedi `MOTION.md` |
| Rifiniture della lista app | S | Lista app a strisce `surface`/`background` fra top bar, header e lista (emersa con la palette Chai) |

## 3. Funzionalità

| Voce | Costo | Note |
|---|---|---|
| Selezione giorni della settimana | M | Es. weekend tutto il giorno. Estendere `OffHours` + test; nuova chiave nelle preferenze |
| Contatore nella card di stato | S | Il contatore per fascia esiste già (`filtered_count`, deduplicato per notifica, mostrato nella notifica zen): mostrarlo anche nella `HeroHeader` |
| Scelta "notifica zen sì/no" nell'app | S | Oggi si spegne dal canale di sistema o negando il permesso: un interruttore in app è più chiaro |
| Onboarding al primo avvio | M | 2–3 pagine con illustrazione (candidata per Lottie, vedi `MOTION.md`) e richiesta del permesso |
| Widget home | L | Glance: nuova dipendenza, da motivare |
| Pulire le notifiche già visibili all'inizio della fascia | L | Richiede di agire al confine della fascia (allarme o job): vedi il vincolo "niente allarmi" in `CLAUDE.md` e valutare con attenzione |

## Fatto

| Voce | Note |
|---|---|
| Scena animata (`ZenScene`) | Scrivania al lavoro / paesaggio fuori orario, luce reale e 4 stagioni, 30 fps con livelli in cache GPU |
| Notifica zen (Live Notification) | Silenziosa, fissa, animata per fase del giorno, fine fascia e contatore; niente allarmi. Vedi `MOTION.md` |
| Riquadro Impostazioni rapide "Stacco & Sole" | `ZenTileService`, icona per fase, sincronizzato con app e notifica |
| Fondamenta (fase 0 della revisione lo-fi) | Scena visibile (`SceneCard` 2:1), messaggi "vita" e ore di sole collegati alla UI, permesso notifiche chiesto con `ZenNotificationCard`, timeline/saluto/fascia aggiornati ogni minuto (`rememberCurrentMinutes`), contatore filtrate deduplicato, lista app ricaricata al resume. Via `INTERNET`, stringhe inutilizzate e componenti morti. Dipendenze AndroidX aggiornate (BOM 2026.09), `targetSdk` 36, R8 in release |
| Messaggi "vita" | Ore di sole e tempo libero rimasti, in `HeroHeader` e `ScheduleCard` (`LifeCopy`, testato) |
| Palette e token (fase 1 della revisione lo-fi) | Palette calda "Chai" (chiaro) / "Lo-fi night" (scuro) con tutti i ruoli M3 assegnati e contrasto WCAG verificato (tabelle in `DESIGN_SYSTEM.md`, regola "`primary` come testo mai su `surfaceContainerHighest` né `primaryContainer`"). Token di movimento in `ui/theme/Motion.kt` usati ovunque (niente durate inline; alone dell'`HeroHeader` attivo solo in `ACTIVE_INSIDE`). Forme del tema (`MaterialTheme.shapes`) al posto dei `RoundedCornerShape` sparsi, niente `Color.White`/`Color(0x…)` nei componenti (salvo la maschera `BlendMode` di `MutedBellIcon`). Illustrazione (`ZenPalette`) invariata: le scene lo-fi sono la fase 2, il nuovo layout della Home (mock-up `mockups/home_active_warm.svg`) la fase 3 |
| Scene e Home (fasi 2–3 della revisione) | Scene procedurali tenute (scrivania sulla città, giardino giapponese; la stanza lo-fi con personaggio è stata scartata) e arricchite: pioggia stabile per data, schermo scuro di notte, sole sulla scrivania, carpe koi. Home come il mock-up: card di stato compatta con lo Switch nella riga del titolo, scena prima del programma, pillole INIZIO → FINE, etichetta "Ora" sulla timeline, CTA senza banda. Icona del launcher: lucchetto crema con la luna come buco della serratura su arancio (anche monocromatica); rimosse le webp di default |
| Rapporto del mattino | `MorningReportCard` sulla Home dopo la fine della fascia: quante notifiche di lavoro hanno aspettato fuori e da quali app (le due con più notifiche + "altre N"), "niente è perso". Una volta per fascia, finché non tocchi "Ok, grazie"; solo per fasce iniziate oggi o ieri. Dati dal contatore esistente (`filtered_keys` ora salva anche il pacchetto) |
| CI e test della regola di blocco | `.github/workflows/ci.yml` (`assembleDebug`, `testDebugUnitTest`, `lintDebug` a ogni push e PR). `BlockingRule` è la regola pura, `PreferencesManager.shouldBlock` la usa con clock e preferenze reali; coperta da `BlockingRuleTest` |
| `targetSdk` 37 e Gradle 9.8 | Wrapper 9.8.0 con checksum ufficiale; lint a 0 errori. Blocco verificato su emulatore API 36 (nessuna immagine API 37 provata: rifare la prova quando c'è) |
| Kotlin 2.4.20 + kotlinx-serialization 1.11 | Versioni nel catalogo (`kotlin`, `kotlinxSerializationCore`); il plugin Kotlin di AGP risolve a 2.4.20. Lint senza avvisi |
| Disclosure e informativa privacy | `PermissionCard` spiega cosa vede l'app (solo il pacchetto), cosa fa e che nulla lascia il telefono; il pulsante "Accetto e apri le impostazioni" è il consenso. Link all'informativa (`docs/index.html`, EN+IT, servita da GitHub Pages: URL in `privacy_policy_url`) |
| Firma release | `signingConfigs.release` letto da `keystore.properties` (ignorato da git); `bundleRelease` produce l'AAB firmato con la chiave di upload. APK release con R8 provato su emulatore (avvio e UI senza crash; blocco end-to-end solo in debug, perché `run-as` non funziona su release) |
| README e backup | README allineato all'app (scena, notifica zen, riquadro, rapporto, palette). Backup Android limitato a `notification_blocker_prefs.xml` (`backup_rules.xml`, `data_extraction_rules.xml`) invece dei file di esempio |
| Nuova icona | Tazza fumante su terracotta al posto del lucchetto con la luna (parlava di notte), anche nell'header della Home (`ic_mug`). Vettoriale adattiva + monocromatica, PNG 512 per lo store, generati da `tools/icon/launcher_icon.py` |
| Nome "Nook" e palette desaturata | Nome scelto insieme (uguale in italiano e inglese; motivi e rischio marchio in `DESIGN_SYSTEM.md`), `applicationId` `com.pasquale.nook`. Palette Chai / Lo-fi night desaturata (saturazione del `primary` 0.87 → 0.50 in chiaro), salvia e blu polvere al posto di smeraldo e viola; contrasto verificato da `tools/palette/contrast.py`. Icona allineata |
| Card "Fine turno" tolta | Luna e stelle contraddicevano il brand e spingevano la Home in basso; tolta insieme a `lottie-compose` (APK più leggero) |

## 4. Qualità

| Voce | Costo | Note |
|---|---|---|
| Test UI Compose | M | `HeroHeader` (3 stati + messaggio "vita"), `AppSelectionScreen` (ricerca, filtro) |
| Test strumentali della notifica zen e del riquadro | M | Oggi verificati a mano su emulatore (`cmd statusbar add-tile/click-tile`, `cmd alarm set-time`): automatizzarli |
| Screenshot test delle preview | M | Compose Preview Screenshot Testing (plugin AGP) |
| Baseline profile | M | Avvio e scroll della lista app più fluidi |

## Dipendenze da valutare

Ogni nuova dipendenza va motivata qui (regola "dipendenze minime").

| Libreria | Per cosa | Stato |
|---|---|---|
| `com.airbnb.android:lottie-compose` | Animazione "Fine turno" | Tolta con la card (luna e stelle contraddicevano il brand, e la scena racconta già lo stacco). Riaggiungerla solo per l'onboarding, se serve |
| `androidx.core:core-splashscreen` | Splash screen API su API < 31 | Proposta |
| `androidx.glance:glance-appwidget` | Widget | In attesa |
