# Design system

Linee guida visive di NotificationBlocker. La base è **Material 3**: se una regola qui non copre un caso, vale la [specifica M3](https://m3.material.io/). Le animazioni hanno un documento a parte: [`MOTION.md`](MOTION.md).

## Principi

1. **Calma.** L'app parla di silenzio: niente colori saturi urlati, niente badge rossi, niente allarmismi. Il viola notturno è il colore del "silenzio attivo", l'ambra quello del "giorno".
2. **Stato leggibile in un secondo.** Aprendo l'app devi capire subito se le notifiche vengono bloccate *adesso*. Colore della card, icona e titolo dicono tutti la stessa cosa.
3. **Nativo Android.** Componenti Material 3 standard (Switch, TimePicker, TopAppBar, FilterChip) prima di quelli custom. Un componente custom si giustifica solo se comunica meglio lo stato (es. `Timeline24h`, `MutedBellIcon`).
4. **Un tocco per l'azione principale.** Il toggle è sempre visibile senza scroll; la CTA "Seleziona app" è fissa in basso, nella zona del pollice. Fuori dall'app, il riquadro "Stacco & Sole" nelle Impostazioni rapide fa la stessa cosa.
5. **Staccare è vivere.** Il tempo libero non è "notte e sonno": è luce, tempo, vita. L'app valorizza le ore di sole che restano dopo il lavoro (d'estate, staccare alle 17 vuol dire ancora tre ore di sole) e segue la luce reale del giorno e della stagione.

## Colore

Palette definita in `ui/theme/Color.kt`, applicata in `Theme.kt`. Nel codice usa **sempre** i ruoli di `MaterialTheme.colorScheme`, mai i valori esadecimali.

| Ruolo | Scuro "Quiet Hours" | Chiaro "Dawn" | Uso |
|---|---|---|---|
| `primary` | `#B8A6FF` lavanda | `#5C4E8C` viola | Blocco attivo *dentro* la fascia, CTA, alone |
| `primaryContainer` | `#3A237A` | `#E6DFFA` | Card di stato "sto bloccando ora" |
| `secondary` | `#FFC56B` ambra | `#7A5900` | Blocco attivo *fuori* dalla fascia (giorno) |
| `secondaryContainer` | `#5E4000` | `#FFE0B2` | Card di stato "attivo, ma ora è consentito" |
| `tertiary` | `#FFB77C` | `#825512` | Avvisi soft (card del permesso mancante) |
| `background` | `#0B1020` notte | `#F8F9FE` | Sfondo schermate |
| `surface` / `surfaceContainer*` | `#141B33` → `#2C3961` | `#FFFFFF` / `#EFF2FA` | Card, bottom bar, dialog |

### Mappa degli stati

| `HeroStatus` | Container | Icona / tinta | Significato |
|---|---|---|---|
| `DISABLED` | `surfaceContainerHigh` | campanella, `onSurfaceVariant` | Blocco spento |
| `ACTIVE_OUTSIDE` | `secondaryContainer` | campanella barrata, `secondary` | Acceso, fuori fascia |
| `ACTIVE_INSIDE` | `primaryContainer` + alone pulsante | campanella barrata, `primary` | Sta bloccando ora |

### Regole

- **Contrasto** ≥ 4.5:1 per il testo normale, ≥ 3:1 per icone e testo grande. Verifica sempre entrambi i temi.
- Il tema chiaro non definisce `surfaceContainerHigh/Highest`, che quindi ricadono sui default M3: se li usi, aggiungili a `LightColorScheme`.
- `dynamicColor` è `false` di proposito: il viola/ambra fa parte dell'identità. Se un giorno lo abiliti, gli stati devono restare distinguibili.
- L'errore (`error`) si usa solo per veri errori, non per il permesso mancante (quello è un avviso: `tertiaryContainer`).

## Luce del giorno e stagioni

Scena, notifica, riquadro e testi seguono la luce **reale**: un solo modello, `ui/zen/ZenEnvironment.kt`, calcola alba e tramonto dal giorno dell'anno (latitudini italiane, ora legale inclusa: ~05:30–20:30 al solstizio d'estate, ~07:45–16:45 a quello d'inverno).

### Ciclo 24h

| `TimeOfDay` | Quando | Scena | Notifica zen | Riquadro | Testo Home |
|---|---|---|---|---|---|
| `DAWN` | da 40' prima a 50' dopo l'alba | cielo rosato, nebbia | illustrazione giorno, "Il giorno inizia piano…" | foglia | "Un nuovo giorno sta iniziando…" (prima dell'alba) |
| `DAYLIGHT` | fino a 70' prima del tramonto | azzurro, sole alto, raggi | sole tra i bambù, "Il sole è ancora alto…" | sole, "Stacco & Sole" | "Ti restano 3 h 20 min di sole…" |
| `GOLDEN_HOUR` | da 70' prima a 30' dopo il tramonto | arancio dorato, lanterna che si accende | tramonto sui colli, "Il tramonto è solo per te…" | lanterna, "Stacco zen" | ore di sole rimaste, poi "la serata è tutta tua" |
| `NIGHT` | il resto | blu cobalto, luna, stelle, lanterna, lucciole d'estate | lanterna e lucciole, "La foresta riposa…" | lanterna | "Il sole è tramontato: la serata è tutta tua." |

I colori della scena non scattano al cambio di fase: la palette `Light` è interpolata minuto per minuto tra keyframe attorno ad alba e tramonto.

### Quattro stagioni

| `Season` | Mesi | Paesaggio (fuori orario) | Scrivania (al lavoro) |
|---|---|---|---|
| `SPRING` | mar–mag | sakura rosa, petali nel vento | alberi del parco rosa |
| `SUMMER` | giu–ago | verde acceso, bambù mossi dalla brezza, raggi più forti, lucciole la sera | alberi verdi |
| `AUTUMN` | set–nov | momiji rosso e ginkgo dorato, foglie che cadono | alberi rossi |
| `WINTER` | dic–feb | rami spogli innevati, lanterna con la neve, fiocchi lenti, monte più bianco | alberi bianchi |

I colori di scena e stagioni stanno in `ui/theme/ZenPalette.kt` (`Light` per ora del giorno, `Flora` per stagione): unica eccezione alla regola "solo `MaterialTheme.colorScheme`", perché è un'illustrazione incorniciata, uguale nei due temi.

## Tipografia

Font **Manrope** (Google Fonts scaricabili, `ui/theme/Type.kt`), scala M3 completa.

| Stile | Uso tipico |
|---|---|
| `headlineMedium` ExtraBold | Titolo della schermata principale |
| `titleMedium` Bold | Titoli card, testo CTA |
| `bodyLarge` SemiBold | Etichetta del toggle |
| `bodySmall` | Sottotitoli e descrizioni (alpha 0.85 sul colore del contenuto) |
| `labelMedium` Bold | Badge e contatori |
| `TimeDisplayTextStyle` | Orari grandi (64sp, cifre tabulari `tnum`) |

- Orari sempre in formato `HH:mm` tramite `OffHours.format()`, con cifre tabulari, così non "ballano" quando cambiano.
- **Sentence case** in entrambe le lingue ("Seleziona app da bloccare", non "Seleziona App da Bloccare"). Alcune stringhe italiane oggi sono in Title Case: vanno sistemate.

## Forme e spaziature

`ui/theme/Shape.kt`:

| Token | Raggio | Uso |
|---|---|---|
| `extraSmall` | 8dp | Chip piccoli, tag |
| `small` | 12dp | Icone app, campi |
| `medium` | 20dp | Card standard |
| `large` | 28dp | Dialog, bottom sheet |
| `extraLarge` | 32dp | Superfici hero |
| `CircleShape` | pill | Bottone CTA, barra di ricerca, badge, contenitori icone |

Spaziature su griglia **4dp**: 4 · 8 · 12 · 16 · 20 · 24.

- Margine orizzontale delle schermate: **16dp**. Spazio tra le card: **16dp**. Padding interno delle card: **20dp** (hero) / **16dp** (card annidate).
- Target di tocco ≥ **48dp**. CTA principale alta **56dp**.
- Rispetta sempre gli insets (`WindowInsets.safeDrawing`, `navigationBarsPadding()`): l'app è edge-to-edge.

## Iconografia

- Solo `material-icons-core` (Filled). Se serve un'icona che non c'è, copiala come vector drawable in `res/drawable/` (es. `ic_moon.xml`, `ic_sun_dim.xml`) invece di aggiungere `material-icons-extended`.
- Dimensioni: 22–24dp inline, 28–30dp in contenitori circolari da 56dp, 32dp accanto ai titoli.
- Icone decorative con `contentDescription = null`; quelle interattive con una stringa `cd_*` tradotta.

## Componenti

| Componente | File | Note |
|---|---|---|
| Card di stato | `HeroHeader.kt` | Colore animato per stato, testo in `AnimatedContent`, toggle incluso nella card |
| Campanella animata | `MutedBellIcon.kt` | Stato "silenziato" = barra diagonale; vedi `MOTION.md` |
| Orari | `ScheduleCard.kt`, `Timeline24h.kt` | Tocca un orario per aprire il `TimePicker` 24h in `AlertDialog` |
| Permesso | `PermissionCard.kt` | Visibile solo senza accesso; porta alle impostazioni di sistema |
| Riga app | `AppItemRow.kt` | Bordo e container animati (200ms) quando l'app è selezionata |
| Caricamento / vuoto | `ShimmerSkeleton.kt`, `EmptyState.kt` | Mai spinner a tutto schermo: skeleton con la forma del contenuto |
| Messaggio "vita" | `HeroHeader.kt` (`lifeMessage`), `LifeMessageText.kt` | Sotto il titolo della card di stato: sole o luna + frase da `LifeCopy`. Aggiornato ogni minuto con l'app in primo piano |
| Luce nella fascia | `ScheduleCard.kt` (`sunshineMinutes`) | "La tua fascia include 3 h 42 min di luce del sole" (sera + mattina dopo), oppure "luna e lanterne" |
| Scena | `ZenScene.kt` | Scrivania al lavoro, paesaggio fuori orario; vedi `MOTION.md` |
| Invito notifica zen | `ZenNotificationCard.kt` | Solo Android 13+, dopo il permesso essenziale; "Non ora" non viene più riproposto |

### Superfici di sistema

| Superficie | File | Note |
|---|---|---|
| Notifica zen | `notification/ZenNotificationManager.kt`, `res/layout/notification_zen_*.xml` | Silenziosa, fissa, `DecoratedCustomViewStyle` (header di sistema coerente). Illustrazione 40dp chiusa / 72dp espansa, testi con gli stili `TextAppearance.Compat.Notification*` così seguono tema e OEM |
| Riquadro "Stacco & Sole" | `tile/ZenTileService.kt` | Attivo = blocco acceso. Icona monocromatica per fase (foglia, sole, lanterna), sottotitolo "Fino alle 09:00" / "Dalle 17:00" / "Spento" |
| Icona barra di stato | `ic_notification_zen.xml` | Ensō monocromatico |

Nuovi componenti: stateless, in `ui/components/`, con `modifier` come primo parametro opzionale e una `@Preview` per stato (chiaro + scuro).

## Testi e tono

- Seconda persona, frasi brevi, niente gergo tecnico ("accesso alle notifiche", non "Notification Listener").
- Rassicurante sul fatto che nulla va perso nell'app di origine; onesto sul fatto che le notifiche bloccate non ritornano.
- Ogni stringa in `values/strings.xml` **e** `values-it/strings.xml`; `plurals` per i conteggi (in italiano `one`, `many`, `other`).
- **Tono "vita"** (messaggi di Home, notifica, riquadro): celebra il tempo e la luce che restano, non il lavoro che manca. Concreto quando possibile ("Ti restano 3 h 20 min di sole"), mai colpevolizzante, mai "vai a dormire". Frasi brevi, un invito gentile alla fine ("goditeli", "respira e rilassati"). Esempi in `strings.xml` alle chiavi `life_*`, `zen_msg_*`, `tile_*`.

## Accessibilità

- Ogni controllo ha un nome leggibile da TalkBack (`semantics { contentDescription = … }` o testo visibile).
- Lo stato non è mai comunicato **solo** dal colore: c'è sempre anche testo o icona.
- Testa con carattere di sistema al 200% e con "Rimuovi animazioni" attivo.
