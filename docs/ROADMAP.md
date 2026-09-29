# Roadmap

Miglioramenti pianificati per rendere l'app più "Android nativa", animata e pronta per il Play Store. Ordinati per priorità dentro ogni sezione. Quando completi o scarti una voce, aggiornala qui.

Legenda costo: **S** < 1h · **M** mezza giornata · **L** più giorni.

## 1. Fondamenta (prima di tutto il resto)

| Voce | Costo | Note |
|---|---|---|
| Kotlin 2.4 + kotlinx-serialization 1.11 | M | Oggi Kotlin 2.2.10 (integrato in AGP 9): aggiornare plugin compose/serialization insieme |
| `targetSdk` 37 e Gradle 9.8 | S | Oggi 36 e 9.6; lint `OldTargetApi` e `AndroidGradlePluginVersion` sono gli unici avvisi rimasti |

## 2. Look & feel Android

| Voce | Costo | Note |
|---|---|---|
| Transizioni di navigazione + predictive back | S | `transitionSpec` / `popTransitionSpec` in `NavDisplay`, `android:enableOnBackInvokedCallback="true"` |
| Haptic su toggle e selezione app | S | `LocalHapticFeedback` |
| Splash screen API | S | `androidx.core:core-splashscreen`, icona con sfondo `background` notte |
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
| Card "Fine turno" | S | Decidere se toglierla (la scena e la notifica raccontano già lo stacco): libera anche la dipendenza Lottie |
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
| `com.airbnb.android:lottie-compose` | Animazione "Fine turno" (`EndOfShiftCard`), poi onboarding / stato vuoto. Se "Fine turno" viene assorbito da `ZenScene`, la dipendenza si può togliere | Aggiunta (6.7.1) |
| `androidx.core:core-splashscreen` | Splash screen API su API < 31 | Proposta |
| `androidx.glance:glance-appwidget` | Widget | In attesa |
