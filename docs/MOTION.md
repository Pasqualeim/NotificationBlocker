# Motion

Linee guida per le animazioni di NotificationBlocker. Per colori e componenti vedi [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md).

## Principi

1. **Il movimento spiega un cambio di stato.** Ogni animazione risponde a "cosa è appena cambiato?" (blocco acceso, app selezionata, schermata nuova). Animazioni puramente decorative: al massimo una per schermata, lenta e discreta (es. l'alone del `HeroHeader`).
2. **Rapido per le risposte, calmo per l'ambiente.** Il feedback al tocco è immediato (≤ 200ms); i loop ambientali sono lenti (≥ 2s) e con ampiezza bassa.
3. **Nessuna animazione al primo frame**, salvo le entrate di schermata. Se un valore è già vero all'apertura, mostralo nello stato finale (vedi `MutedBellIcon`: niente scuotimento se il blocco era già attivo).
4. **Mai bloccare l'utente.** Nessuna animazione ritarda un'azione o un input.
5. **Rispetta le impostazioni di sistema.** Compose scala le durate in base alla "Scala durata animazione" dello sviluppatore; con "Rimuovi animazioni" attivo l'app deve restare perfettamente usabile.

## Token

Tutti i token stanno in `ui/theme/Motion.kt` (`object Motion`). **Niente numeri inline**: ogni durata, easing e spring della UI viene da qui. Una coreografia che appartiene a un solo componente (es. i passi dello scuotimento della campanella) può restare una costante `private const` con un nome parlante e un commento di una riga, ed è elencata nel catalogo qui sotto.

| Token | Valore | Uso |
|---|---|---|
| `Motion.INSTANT` | 100 ms | Feedback di pressione |
| `Motion.SHORT` | 200 ms | Colori/bordi di elementi piccoli (`AppItemRow`), fade-out |
| `Motion.MEDIUM` | 300 ms | Cambi di contenuto (`AnimatedContent`), entrate, espansioni, barra della campanella |
| `Motion.LONG` | 400 ms | Cambi di colore di superfici grandi (`HeroHeader`), fade-in del messaggio "vita" |
| `Motion.SLOW` | 1200 ms | Transizioni lente e calme: dissolvenza della scena, passata dello shimmer |
| `Motion.AMBIENT` | 2000 ms | Mezzo ciclo dei loop ambientali (alone che respira) e pause di "attesa" |
| `Motion.Stagger` | 80 ms (`Duration`) | Ritardo tra elementi di un'entrata a cascata (`MainScreen`) |
| `Motion.PRESS_SCALE` | 0.97 | Scala di una CTA o di una pillola premuta |

| Easing / spring | Uso |
|---|---|
| `Motion.StandardEasing` = `FastOutSlowInEasing` | Easing di ogni tween, salvo eccezioni documentate dal componente |
| `Motion.standard(durationMillis = MEDIUM, delayMillis = 0)` | `TweenSpec` con `StandardEasing`: la spec da usare per ogni `tween` (es. `Motion.standard(Motion.SHORT)`) |
| `Motion.press()` = `spring(DampingRatioMediumBouncy, StiffnessLow)` | Pressione della CTA e delle pillole degli orari (scala `PRESS_SCALE`) |
| `spring()` default | Valori interrotti di frequente (drag, toggle ripetuti) |

Le transizioni con spec di default (`fadeIn()`, `expandVertically()` senza argomenti) non si usano: passa sempre una spec di `Motion` (fade-in `MEDIUM`, fade-out `SHORT`, expand/shrink `MEDIUM`).

## Catalogo attuale

| Dove | Cosa | Implementazione |
|---|---|---|
| `MainScreen` | Entrata a cascata di header, permesso, hero, orari, scena | `AnimatedVisibility` fade + slide, `MEDIUM` per ogni elemento; la cascata viene da `Motion.Stagger` (80 ms) tra un elemento e il successivo |
| `MainScreen` | CTA che si "schiaccia" alla pressione | `animateFloatAsState` a `PRESS_SCALE` con `Motion.press()`, `graphicsLayer` scale |
| `ScheduleCard` | Pillole degli orari che si "schiacciano" alla pressione | Come la CTA: `PRESS_SCALE` con `Motion.press()` |
| `HeroHeader` | Colori della card per stato | `animateColorAsState` `LONG` |
| `HeroHeader` | Titolo e sottotitolo che cambiano | `AnimatedContent` fade + slide `MEDIUM` / fade-out `SHORT` |
| `HeroHeader` | Alone che respira quando blocca ora | `rememberInfiniteTransition` in un composable privato (`BreathingGlow`) composto **solo** in `ACTIVE_INSIDE`, così il loop si ferma negli altri stati; scala 0.9→1.35, alpha 0.2→0.55, `AMBIENT` (2000 ms) reverse. Sta fuori da `AnimatedContent`, e la campanella accanto mantiene il suo stato |
| `MutedBellIcon` | Campanella che suona e viene barrata | `Animatable` rotazione ±24°→0 in 7 passi da `SHAKE_STEP_MILLIS` = 70 ms (costante privata: più rapida di ogni token, così si legge come uno squillo; perno in alto), poi barra `MEDIUM` ritardata della durata dello scuotimento (7 × 70 ms); ritaglio con `BlendMode.Clear` in un layer offscreen |
| `PermissionCard` | Comparsa/scomparsa | `AnimatedVisibility`: fade-in `MEDIUM` + expand `MEDIUM`, fade-out `SHORT` + shrink `MEDIUM` |
| `ZenNotificationCard` | Comparsa/scomparsa | Come `PermissionCard` |
| `AppItemRow` | Selezione | `animateColorAsState` `SHORT` su container e bordo |
| `ShimmerSkeleton` | Caricamento lista app | Gradiente che scorre in loop infinito, una passata ogni `SLOW` (1200 ms) |
| `ZenScene` | Scena animata in fondo alla Home, con la luce reale del giorno (alba, sole, tramonto, notte) e la stagione. Al lavoro: la vista dalla scrivania, una finestra sulla città con laptop, tazza fumante e pianta. Fuori orario: paesaggio giapponese (monte innevato, ruscello, airone che pesca, lanterna, bambù; sakura, foglie d'autunno, neve, lucciole) | Livelli vettoriali in `ui/zen` (2:1, 30 fps); livelli fermi in texture GPU. Cambio di stato = `Crossfade` `SLOW` (1200 ms), niente altro; con "Rimuovi animazioni" frame fermo e cambio istantaneo |
| Notifica zen (tendina / blocco schermo) | Illustrazione animata per la fase del giorno: sole con raggi lenti tra i bambù, tramonto sui colli con riflessi, lanterna con fiamma e lucciole | `AnimatedVectorDrawable` (`avd_zen_*`) in un `ProgressBar` indeterminato nelle RemoteViews; loop lenti (2–24 s), l'animazione la gestisce SystemUI (si ferma quando la tendina è chiusa) |
| Messaggio "vita" (`HeroHeader`) | La frase su sole e tempo libero cambia con dissolvenza | `AnimatedContent` fade-in `LONG` / fade-out `SHORT`, ricalcolo al minuto solo con l'app in primo piano (`rememberCurrentMinutes()`: `repeatOnLifecycle(STARTED)`, rilegge l'orologio a ogni ritorno in primo piano) |
| Riquadro "Stacco & Sole" | Icona foglia → sole → lanterna secondo la fase | Nessuna animazione propria: la transizione di stato del riquadro è di sistema |
| `EndOfShiftCard` | "Fine turno": le notifiche volano via, il sole tramonta, sorge la luna con le stelle | Lottie `res/raw/end_of_shift.json` (5s, 60fps), una volta per fascia, poi la card resta ferma per `AMBIENT` (2000 ms) e si chiude da sola; comparsa/scomparsa come `PermissionCard` |

## Regole di implementazione

- **Durate, easing e spring da `Motion`** (`ui/theme/Motion.kt`), mai numeri inline; le eccezioni di un singolo componente sono costanti private con nome, elencate nel catalogo.
- **`label =`** su ogni `animate*AsState`, `Transition` e `InfiniteTransition` (servono all'Animation Inspector di Android Studio).
- Anima in `graphicsLayer { }` (scale, rotation, alpha, translation): niente ricomposizione né nuovo layout a ogni frame. Evita di animare `padding`/`size` se basta una scala.
- Leggi i valori animati **dentro** la lambda (`graphicsLayer { rotationZ = rotation.value }`, `drawBehind { … }`), non nel corpo del composable.
- Lo stato che deve sopravvivere a un cambio di stato va **fuori** da `AnimatedContent`, perché `AnimatedContent` ricrea il contenuto ad ogni `targetState`.
- Sequenze imperative con `Animatable` dentro `LaunchedEffect(key)`; stati dichiarativi con `animate*AsState` / `updateTransition`.
- `rememberInfiniteTransition` solo per elementi visibili e significativi: consuma frame finché è in composizione. Se serve solo in uno stato, mettilo in un composable a parte composto solo in quello stato (vedi `BreathingGlow` in `HeroHeader`).
- Nuovo effetto = preview statica + verifica su dispositivo in entrambi i temi.

## Prossime animazioni candidate

In ordine di rapporto valore/costo (dettagli in [`ROADMAP.md`](ROADMAP.md)):

1. **Haptic** sul toggle e sulla selezione delle app (`LocalHapticFeedback`, `HapticFeedbackType.ToggleOn/Off` con BOM recente).
2. **Transizioni di navigazione** tra Home e Selezione app (`NavDisplay` `transitionSpec` / `popTransitionSpec`) con **predictive back**.
3. **Sole → luna** nella `ScheduleCard`, a seconda che l'ora corrente sia dentro la fascia (`AnimatedContent` tra `ic_sun_dim` e `ic_moon`).
4. **Indicatore "ora" della `Timeline24h`** che scorre al minuto e pulsa leggermente.
5. **`Modifier.animateItem()`** nella lista app quando un elemento passa da "Tutte" a "Selezionate".
6. **Material 3 Expressive** (`MotionScheme.expressive()`) dopo l'aggiornamento della BOM Compose.

## Scena zen

`ZenScene` è un'illustrazione vettoriale disegnata in Compose (niente Lottie né bitmap): nitida a ogni densità, colori e forme generati dal codice. Due scene, un solo passaggio: una dissolvenza lenta quando cambia lo stato di lavoro. Niente porte, cancelli o transizioni "a effetto".

- `ZenEnvironment` (`ui/zen`): da `LocalDate` + `LocalTime` ricava `Season` (mesi meteorologici), `TimeOfDay` (`DAWN`, `DAYLIGHT`, `GOLDEN_HOUR`, `NIGHT`), posizione di sole/luna e la palette di luce **interpolata** tra keyframe attorno ad alba e tramonto (niente scatti). Alba/tramonto seguono il giorno dell'anno (~05:30–20:30 d'estate, ~07:45–16:45 d'inverno): alle 17 d'estate c'è pieno sole.
- `ScenePainting`: base comune in unità 320×160, divisa in livelli per ciò che li fa cambiare: `sky` e `landscape` (solo l'ambiente), `skyMotion` (dietro al paesaggio) e `foreground` (davanti) che dipendono dal tempo. I colori "diurni" passano da `lit()` (moltiplicati per la luce ambiente); cielo, acqua e sorgenti di luce vengono dalla palette dell'ora. La casualità viene da `hash()`, quindi è stabile.
- `DeskPainting` (al lavoro): finestra d'ufficio sulla città (stesso cielo, finestre accese la sera, alberi del parco nei colori della stagione), laptop, tazza con vapore, quaderno, pianta. Gli interni usano `indoor()`: luce del giorno, poi luce calda della lampada quando fuori è buio. In movimento: nuvole, uccelli, un aereo ogni tanto, vapore, cursore.
- `NaturePainting` (fuori orario): monte innevato al centro, ruscello con pietre, airone che ogni tanto abbassa il becco verso l'acqua, lanterna (accesa al tramonto e di notte), due alberi di stagione, bambù nella brezza. Particelle di stagione, lucciole d'estate di notte, nebbia all'alba.
- `ZenPainter`: 7 primitive (rect, gradiente, cerchio, ellisse, alone radiale, poligono, linea). Implementato su `DrawScope` nell'app e su AWT nei test.
- Prestazioni: i livelli fermi usano `CompositingStrategy.Offscreen` (rasterizzati una volta in texture); a ogni frame (max 30 fps) si ridisegnano solo i due livelli in movimento. Sull'emulatore: RenderThread ~0–5 % di CPU. Niente allocazioni per frame nei calcoli (`hash` ad arità fissa, buffer dei poligoni e `Path` riusati).
- `ZenScene` rilegge l'orologio a ogni minuto (`rememberCurrentMinutes()`, anche con "Rimuovi animazioni"); il parametro `environment` fissa stagione/ora nelle preview.
- Palette fissa in `ui/theme/ZenPalette.kt`: colori base, `Light` per ora del giorno, `Flora` per stagione (unica eccezione alla regola "solo `MaterialTheme.colorScheme`": è un'illustrazione incorniciata, uguale nei due temi).
- Anteprime senza device: `./gradlew testDebugUnitTest --tests '*ZenSceneTest*'` scrive PNG di entrambe le scene per ogni stagione × ora in `app/build/zen-previews/`.

## Ciclo 24h e 4 stagioni nel movimento

Il movimento cambia con la luce, sempre lento:

- **Giorno:** raggi di sole che pulsano (periodo ~18 s), riflessi che brillano sull'acqua, uccelli; d'estate la brezza sui bambù è più ampia.
- **Tramonto:** il sole scende piano, i riflessi diventano dorati, la lanterna si accende (fiamma con sfarfallio leggero).
- **Notte:** stelle che pulsano, luna riflessa, lucciole d'estate; niente movimenti rapidi.
- **Stagioni:** petali (primavera, 26 in scena), poche foglie verdi (estate), foglie che ruotano cadendo (autunno), neve lenta (inverno, periodo 14 s).

Regola: al massimo **una** cosa si muove in continuazione nel campo visivo (la scena, oppure l'illustrazione della notifica); il resto dell'app si anima solo a un cambio di stato.

## Live Notification: architettura

```
MainViewModel ──┐                         ┌─ ZenNotificationState.resolve()  (puro, testato)
ZenTileService ─┼─► ZenNotificationManager.refresh() ─┤
Listener service┘   (posta / aggiorna / rimuove)      └─ RemoteViews: notification_zen_collapsed/expanded
   ▲  TIME_TICK (solo schermo acceso), SCREEN_ON, USER_PRESENT, cambio ora/fuso, ogni notifica filtrata
```

- **Nessun allarme** (regola di `CLAUDE.md`): la rimozione a fine fascia la fa il sistema con `setTimeoutAfter`; gli aggiornamenti di fase arrivano dagli eventi sopra, quindi a schermo spento non costa nulla e allo sblocco è già giusta.
- **Anti-spam:** `contentKey` (fase, fascia, fine, contatore) → si ripubblica solo se cambia qualcosa di visibile; `setOnlyAlertOnce` + canale `IMPORTANCE_LOW` → mai suoni o vibrazioni.
- **Animazione nelle RemoteViews:** `avd_zen_day`, `avd_zen_sunset`, `avd_zen_night` sono `AnimatedVectorDrawable` usate come `indeterminateDrawable` di un `ProgressBar` (le `ImageView` delle RemoteViews non le avviano). Loop infiniti lenti (2–24 s), `repeatMode="reverse"` tranne la rotazione dei raggi. Una sola delle tre è visibile (`setViewVisibility`). La gestisce SystemUI: si ferma quando la tendina è chiusa; con alcuni OEM o il risparmio energetico può restare ferma, e l'immagine statica resta corretta.
- **Rispetto dell'utente:** se la scorre via (Android 14+), `DismissReceiver` salva la fascia e non la ripropone fino alla successiva o finché il blocco non viene riacceso.
- **Riquadro (`ZenTileService`):** tile "attivo" (`ACTIVE_TILE`) → si ridisegna solo quando la tendina si apre o su `requestListeningState` (dopo un cambio dall'app); al tocco commuta `blocking_enabled`, aggiorna la notifica, e l'app segue tramite `OnSharedPreferenceChangeListener` nel `MainViewModel`.

## Lottie

Per illustrazioni più ricche (onboarding, stato vuoto, "tutto tranquillo" notturno) si può usare Lottie.

### Quando sì, quando no

- **Sì:** illustrazioni una tantum, a più elementi, difficili da disegnare in codice (luna con stelle che si accendono, personaggio che dorme).
- **No:** micro-interazioni legate allo stato (toggle, selezione, colori del tema). Quelle restano in Compose: seguono il tema chiaro/scuro, costano zero dipendenze e si testano con le preview.

### Integrazione (da fare solo quando serve la prima animazione)

```kotlin
// gradle/libs.versions.toml → lottie = "<ultima versione>"
implementation("com.airbnb.android:lottie-compose:<versione>")
```

```kotlin
val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.quiet_night))
LottieAnimation(
    composition = composition,
    iterations = LottieConstants.IterateForever,
    modifier = Modifier.size(160.dp),
)
```

- File in `res/raw/` (JSON o `.lottie`), nomi `snake_case` descrittivi.
- Colori: usa `rememberLottieDynamicProperties` per sostituire i colori con quelli di `MaterialTheme.colorScheme`, così l'animazione segue il tema.
- Budget: < 50 KB per file, ≤ 60 fps, nessuna immagine raster incorporata.
- Aggiungi la dipendenza in `ROADMAP.md` con la motivazione (regola "dipendenze minime" in `CLAUDE.md`).

### Animazioni generate da script

`end_of_shift.json` è generato da `tools/lottie/end_of_shift.py` (helper in `tools/lottie/lottie_kit.py`): modifica lo script, non il JSON. I nomi dei gruppi sono le keypath che `EndOfShiftCard` ricolora con `MaterialTheme.colorScheme`.

Tre trappole di lottie-android (già gestite dal kit, vedi il docstring di `lottie_kit.py`):

- In ogni oggetto con `"ty"` la chiave `ty` deve essere **la prima**: le chiavi lette prima del tipo vengono ignorate (le trasformazioni dei gruppi sparivano).
- `LottieProperty.COLOR` ricolora solo i fill; per gli stroke serve `STROKE_COLOR`.
- L'ultimo keyframe viene scartato: un keyframe "hold" subito prima non scatterebbe mai (il kit aggiunge una copia).

Per le anteprime veloci `python-lottie` esporta i frame in SVG, ma non riproduce queste trappole: la verifica finale va fatta sul device.
- Evita tinte ambra a bassa opacità sulle superfici blu: diventano grigio fango.
- Il file si può importare in Lottie Creator per ritocchi manuali; in quel caso lo script non è più la fonte di verità.

### Creare le animazioni con Lottie Creator + AI

LottieFiles offre un server MCP che permette a Claude Code di disegnare e animare direttamente dentro [Lottie Creator](https://creator.lottiefiles.com) (editor web).

```bash
claude mcp add lottiefiles-creator -- npx -y @lottiefiles/creator-mcp@latest   # Node 18+
npx skills add LottieFiles/motion-design-skill                                 # opzionale: regole di motion design
```

1. Apri `creator.lottiefiles.com` con il tuo account e lascia la scheda aperta: il bridge MCP passa da lì (toast "Local MCP bridge connected").
2. Riavvia Claude Code e chiedigli l'animazione, dando palette (esadecimali di `Color.kt`), dimensioni, durata e loop.
3. Rifinisci a mano in Creator se serve, poi **esporta in Lottie JSON** in `app/src/main/res/raw/`.

In alternativa, con un browser che supporta WebMCP, l'assistente può controllare Creator direttamente dalla pagina, senza server locale ([WebMCP](https://docs.lottiefiles.com/en/creator/13_ai-tools/lottie-creator-webmcp)).

Documentazione: [Lottie Creator MCP](https://docs.lottiefiles.com/en/creator/13_ai-tools/lottie-creator-mcp).
