# Motion

Linee guida per le animazioni di Nook. Per colori e componenti vedi [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md).

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
| `MainScreen` | Entrata a cascata di header, card in cima, scena, orari | `Modifier.homeEntrance` (`animateFloatAsState`, `MEDIUM`): alpha 0→1 e salita di metà altezza, `graphicsLayer`; la cascata viene da `Motion.Stagger` (80 ms). I blocchi sono già composti e misurati dal primo frame (non `AnimatedVisibility`): `HomeColumn` ne calcola la scala sull'altezza di tutti, vedi "Home in una schermata" in `DESIGN_SYSTEM.md` |
| `HomeColumn` | Scala della Home che cambia ("Home in una schermata" in `DESIGN_SYSTEM.md`) | Nessuna animazione: cambia in un colpo quando le altezze sono ferme da `SETTLE_MILLIS` (200 ms); durante un'animazione di altezza la pagina resta alla scala di prima |
| `MainActivity` (`NavDisplay`) | Navigazione Home ↔ App di lavoro, anche col gesto "indietro" predittivo | Scorrimento laterale `MEDIUM`, schermate opache: la nuova copre la vecchia, che si sposta di un quarto (`COVERED_SHIFT`) nello stesso verso; tornando indietro la schermata che esce resta sopra (`targetContentZIndex = -1`). Niente dissolvenza: quella predefinita (700 ms) disegnava due schermate trasparenti in buffer offscreen, ~25 fps su Galaxy A32 |
| `MainScreen` | CTA che si "schiaccia" alla pressione | `animateFloatAsState` a `PRESS_SCALE` con `Motion.press()`, `graphicsLayer` scale |
| `ScheduleCard` | Pillole degli orari che si "schiacciano" alla pressione | Come la CTA: `PRESS_SCALE` con `Motion.press()` |
| `HeroHeader` | Colori della card per stato | `animateColorAsState` `LONG` |
| `HeroHeader` | Titolo e sottotitolo che cambiano | `AnimatedContent` fade + slide `MEDIUM` / fade-out `SHORT` |
| `HeroHeader` | Alone che respira quando blocca ora | `rememberInfiniteTransition` in un composable privato (`BreathingGlow`) composto **solo** in `ACTIVE_INSIDE`, così il loop si ferma negli altri stati; scala 0.9→1.35, alpha 0.2→0.55, `AMBIENT` (2000 ms) reverse. Sta fuori da `AnimatedContent`, e la campanella accanto mantiene il suo stato |
| `MutedBellIcon` | Campanella che suona e viene barrata | `Animatable` rotazione ±24°→0 in 7 passi da `SHAKE_STEP_MILLIS` = 70 ms (costante privata: più rapida di ogni token, così si legge come uno squillo; perno in alto), poi barra `MEDIUM` ritardata della durata dello scuotimento (7 × 70 ms); ritaglio con `BlendMode.Clear` in un layer offscreen |
| `MainScreen` | Card in cima che cambia (permesso → invito alla notifica → resoconto del mattino → card di stato) | `AnimatedContent` (`HomeTopCard`): fade-in `MEDIUM` / fade-out `SHORT`, l'altezza segue con il `SizeTransform` di default. `PermissionCard`, `ZenNotificationCard` e `MorningReportCard` non hanno più un'animazione propria; `contentKey` per tipo, così un resoconto che si aggiorna non si dissolve |
| `AppItemRow` | Selezione | `animateColorAsState` `SHORT` su container e bordo |
| `ShimmerSkeleton` | Caricamento lista app | Gradiente che scorre in loop infinito, una passata ogni `SLOW` (1200 ms) |
| `ZenScene` | Scena animata nella Home, con la luce reale del giorno (alba, sole, tramonto, notte), la stagione e il meteo. Al lavoro: scrivania in un ufficio in alto, il codice che si scrive da solo sul monitor, vetrata sullo skyline. Fuori orario: un pontile di legno su un lago calmo, con barca a remi, anatre, ramo e canne | Livelli vettoriali in `ui/zen` (2:1, 30 fps); livelli fermi in texture GPU. Cambio di stato = `Crossfade` `SLOW` (1200 ms), niente altro; con "Rimuovi animazioni" frame fermo e cambio istantaneo |
| Notifica della pausa (tendina / blocco schermo) | Il pontile sul lago della Home in miniatura, per fase del giorno: riflessi che scorrono, barca che dondola, canne al vento; di giorno l'alone del sole e una nuvola, al tramonto il sole grande sui colli, di notte stelle che brillano e il lampione acceso | `AnimatedVectorDrawable` (`avd_zen_*`) in un `ProgressBar` indeterminato nelle RemoteViews; loop lenti (2–24 s), l'animazione la gestisce SystemUI (si ferma quando la tendina è chiusa) |
| Messaggio "vita" (`HeroHeader`) | La frase su sole e tempo libero cambia con dissolvenza | `AnimatedContent` fade-in `LONG` / fade-out `SHORT`, ricalcolo al minuto solo con l'app in primo piano (`rememberCurrentMinutes()`: `repeatOnLifecycle(STARTED)`, rilegge l'orologio a ogni ritorno in primo piano) |
| Riquadro "Nook" | Icona foglia → sole → lanterna secondo la fase | Nessuna animazione propria: la transizione di stato del riquadro è di sistema |

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
2. **Sole → luna** nella `ScheduleCard`, a seconda che l'ora corrente sia dentro la fascia (`AnimatedContent` tra `ic_sun_dim` e `ic_moon`).
3. **Indicatore "ora" della `Timeline24h`** che scorre al minuto e pulsa leggermente.
4. **`Modifier.animateItem()`** nella lista app quando un elemento passa da "Tutte" a "Selezionate".
5. **Material 3 Expressive** (`MotionScheme.expressive()`) dopo l'aggiornamento della BOM Compose.

## Scena zen

`ZenScene` è un'illustrazione vettoriale disegnata in Compose (niente Lottie né bitmap): nitida a ogni densità, colori e forme generati dal codice. Due scene, un solo passaggio: una dissolvenza lenta quando cambia lo stato di lavoro. Niente porte, cancelli o transizioni "a effetto".

- `ZenEnvironment` (`ui/zen`): da `LocalDate` + `LocalTime` ricava `Season` (mesi meteorologici), `TimeOfDay` (`DAWN`, `DAYLIGHT`, `GOLDEN_HOUR`, `NIGHT`), posizione di sole/luna e la palette di luce **interpolata** tra keyframe attorno ad alba e tramonto (niente scatti). Alba/tramonto: formule solari standard nel fuso del telefono, ora legale inclusa (~05:30–20:50 d'estate, ~07:40–16:40 d'inverno; dettagli in `DESIGN_SYSTEM.md`): alle 17 d'estate c'è pieno sole. Meteo inventato ma stabile (`isRaining`): alcuni giorni hanno un rovescio di 6 ore (più spesso in primavera e autunno, mai d'inverno); d'inverno invece alcuni giorni nevica per 6 ore (`isSnowing`). Con pioggia o neve la luce passa da `rainy()` (cielo grigio, ambiente più scuro, niente raggi). `ZenState.clock` porta i minuti del giorno per l'orologio della scrivania.
- `ScenePainting`: base comune in unità 320×160, divisa in livelli per ciò che li fa cambiare: `sky` e `landscape` (solo l'ambiente), `skyMotion` (dietro al paesaggio) e `foreground` (davanti) che dipendono dal tempo. I colori "diurni" passano da `lit()` (moltiplicati per la luce ambiente); cielo, acqua e sorgenti di luce vengono dalla palette dell'ora. La casualità viene da `hash()`, quindi è stabile.
- `DeskPainting` (al lavoro): scrivania in un ufficio in alto, colori "Chai" all'interno. Sul monitor sette righe di codice si scrivono da sole, otto battute per riga (`CODE_CYCLE` 9 s, poi lo schermo sfuma e riparte); la tastiera si accende solo mentre una riga viene scritta. Tazza con vapore, pianta che oscilla (~4°, periodo 10 s), orologio a muro con l'ora vera (lancetta dei secondi a scatti). Dietro, una vetrata a tutta altezza sullo skyline: torri di vetro che riflettono il cielo, una torre a gradoni con la guglia (luce rossa che lampeggia la sera), piani accesi dopo il tramonto, parco ai piedi delle torri nei colori della stagione, neve sui tetti d'inverno, un aereo ogni tanto, pioggia (con gocce sul vetro) o neve. Gli interni usano `indoor()`: luce del giorno, poi luce calda quando fuori è buio; lo schermo è emissivo e la sera illumina la parete intorno.
- `NaturePainting` (fuori orario): un pontile di legno in prospettiva che entra in un lago calmo e finisce con il palo della lanterna (accesa al tramonto e di notte, la sua luce trema sull'acqua). Una barca a remi legata accanto dondola piano (`BOAT_ROCK`); due anatre attraversano lente con la scia (di notte dormono). La riva lontana (alberi tondi e abeti nel colore della stagione) si specchia nel lago. Un ramo in alto a sinistra con foglie a ciuffi che fremono, canne con tife agli angoli in basso nella brezza. Stagioni: fiori e petali (primavera), ninfee in fiore e lucciole di notte (estate), foglie d'acero e foglie che galleggiano (autunno), ramo spoglio, neve su pontile, barca e tife (inverno). Pioggia con cerchi sull'acqua, nebbia sul lago all'alba, sole basso o luna riflessi in una colonna di luce.
- `ZenPainter`: 7 primitive (rect, gradiente, cerchio, ellisse, alone radiale, poligono, linea). Implementato su `DrawScope` nell'app e su AWT nei test.
- Prestazioni: i livelli fermi usano `CompositingStrategy.Offscreen` (rasterizzati una volta in texture); a ogni frame (max 30 fps) si ridisegnano solo i due livelli in movimento. Niente allocazioni per frame nei calcoli (`hash` ad arità fissa, buffer dei poligoni e `Path` riusati, gradienti delle luci in cache per colore e raggio). La scena si ferma mentre la Home scorre (`ZenScene(animate = false)`) e riprende dallo stesso istante. Su un Galaxy A32 (GPU Mali-G52, release) la scena gira a 30 fps costanti, un frame ogni 33,3 ms. Il limite ha 2 ms di margine (`FRAME_SLACK_NANOS`): senza, il frame previsto al 3° vsync a 90 Hz (al 2° a 60 Hz) slittava spesso al successivo e la scena andava a 22,5 fps a scatti irregolari. Ogni frame costa circa 20 ms di GPU perché il telefono ridisegna tutta la finestra: misurati una finestra vuota ~7 ms, la Home ferma ~16 ms, la scena ~5 ms in più. Misure e metodo in `CLAUDE.md` ("Performance").
- `ZenScene` rilegge l'orologio a ogni minuto (`rememberCurrentMinutes()`, anche con "Rimuovi animazioni"); il parametro `environment` fissa stagione/ora nelle preview.
- Palette fissa in `ui/theme/ZenPalette.kt`: colori base, `Light` per ora del giorno, `Flora` per stagione (unica eccezione alla regola "solo `MaterialTheme.colorScheme`": è un'illustrazione incorniciata, uguale nei due temi).
- Anteprime senza device: `./gradlew testDebugUnitTest --tests '*ZenSceneTest*'` scrive PNG di entrambe le scene per ogni stagione × ora in `app/build/zen-previews/`.

## Ciclo 24h e 4 stagioni nel movimento

Il movimento cambia con la luce, sempre lento:

- **Giorno:** riflessi che brillano sull'acqua, anatre, uccelli e nuvole; in ufficio un aereo ogni tanto.
- **Tramonto:** il sole scende piano e si riflette sul lago, la lanterna del pontile si accende; in città si accendono i piani.
- **Notte:** stelle che pulsano, luna riflessa, luce della lanterna che trema sull'acqua, lucciole d'estate; niente movimenti rapidi.
- **Stagioni:** petali dal ramo (primavera), lucciole (estate), foglie che ruotano cadendo e galleggiano (autunno), neve lenta (inverno, periodo 14 s; più fitta nei giorni di nevicata).

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
- **Animazione nelle RemoteViews:** `avd_zen_day`, `avd_zen_sunset`, `avd_zen_night` sono `AnimatedVectorDrawable` usate come `indeterminateDrawable` di un `ProgressBar` (le `ImageView` delle RemoteViews non le avviano). Generate da `tools/notification/zen_art.py` (modifica lo script, non gli XML). Loop infiniti lenti (1,8–9 s), tutti `repeatMode="reverse"`. Una sola delle tre è visibile (`setViewVisibility`). La gestisce SystemUI: si ferma quando la tendina è chiusa; con alcuni OEM o il risparmio energetico può restare ferma, e l'immagine statica resta corretta.
- **Rispetto dell'utente:** se la scorre via (Android 14+), `DismissReceiver` salva la fascia e non la ripropone fino alla successiva o finché il blocco non viene riacceso.
- **Riquadro (`ZenTileService`):** tile "attivo" (`ACTIVE_TILE`) → si ridisegna solo quando la tendina si apre o su `requestListeningState` (dopo un cambio dall'app); al tocco commuta `blocking_enabled`, aggiorna la notifica, e l'app segue tramite `OnSharedPreferenceChangeListener` nel `MainViewModel`.

## Lottie

Per illustrazioni più ricche (onboarding, stato vuoto) si può usare Lottie. Oggi l'app **non** include `lottie-compose`: è stata tolta con la card "Fine turno". Reintrodurla è una nuova dipendenza da motivare in `ROADMAP.md`.

### Quando sì, quando no

- **Sì:** illustrazioni una tantum, a più elementi, difficili da disegnare in codice (per esempio una tazza che si riempie in un onboarding). Sempre nel tono dell'app: niente lune, stelle o personaggi che dormono.
- **No:** micro-interazioni legate allo stato (toggle, selezione, colori del tema). Quelle restano in Compose: seguono il tema chiaro/scuro, costano zero dipendenze e si testano con le preview.

### Integrazione (da fare solo quando serve la prima animazione)

```kotlin
// gradle/libs.versions.toml → lottie = "<ultima versione>"
implementation("com.airbnb.android:lottie-compose:<versione>")
```

```kotlin
val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.onboarding_break))
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

Gli helper in `tools/lottie/lottie_kit.py` scrivono Lottie JSON a mano (usati per la vecchia card "Fine turno"): un'animazione generata va tenuta come script, non come JSON modificato a mano.

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
