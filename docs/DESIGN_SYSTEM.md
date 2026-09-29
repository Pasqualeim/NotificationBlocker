# Design system

Linee guida visive di NotificationBlocker. La base è **Material 3**: se una regola qui non copre un caso, vale la [specifica M3](https://m3.material.io/). Le animazioni hanno un documento a parte: [`MOTION.md`](MOTION.md).

## Principi

1. **Calma.** L'app parla di silenzio: niente colori saturi urlati, niente badge rossi, niente allarmismi. Toni caldi da "lo-fi anime" (carta crema, terracotta, cioccolato): l'arancio della lampada è il colore del "silenzio attivo", lo smeraldo quello di "acceso, tutto sotto controllo".
2. **Stato leggibile in un secondo.** Aprendo l'app devi capire subito se le notifiche vengono bloccate *adesso*. Colore della card, icona e titolo dicono tutti la stessa cosa.
3. **Nativo Android.** Componenti Material 3 standard (Switch, TimePicker, TopAppBar, FilterChip) prima di quelli custom. Un componente custom si giustifica solo se comunica meglio lo stato (es. `Timeline24h`, `MutedBellIcon`).
4. **Un tocco per l'azione principale.** Il toggle è sempre visibile senza scroll; la CTA "Seleziona app" è fissa in basso, nella zona del pollice. Fuori dall'app, il riquadro "Stacco & Sole" nelle Impostazioni rapide fa la stessa cosa.
5. **Staccare è vivere.** Abbiamo una vita oltre il lavoro: l'app esiste per restituirla, non per "far dormire". Il tempo libero non è "notte e sonno": è luce, tempo, vita, a qualsiasi ora finisca il lavoro. L'app valorizza le ore di sole che restano dopo il lavoro (d'estate, staccare alle 17 vuol dire ancora tre ore di sole) e segue la luce reale del giorno e della stagione.

## Icona dell'app

Una **tazza fumante su fondo terracotta**: la pausa che ti riprendi, non il lavoro che manca. L'icona non deve parlare di notte, sonno o "non disturbare" (luna, stelle, zzz), né di un divieto (campanella barrata, cartello di stop): sono concetti di un'app per dormire, non di questa.

- Tazza e volute crema (`#F8EDD7`) su gradiente terracotta (`#CF6D43` → `#BB5931`), leggermente più chiaro in alto.
- Icona adattiva (sfondo + primo piano vettoriale) e variante monocromatica: lo stesso primo piano, un solo colore. Simbolo nella zona sicura del 66%, leggibile a 48 px.
- Generata da `tools/icon/launcher_icon.py` (modifica lo script, non i file): scrive `ic_launcher_foreground.xml`, `ic_launcher_background.xml` e il PNG 512 dello store (`docs/store/icon-512.png`). Nata da una bozza di Gemini, ridisegnata a mano in vettoriale.

## Colore

Palette calda "lo-fi anime", definita in `ui/theme/Color.kt` e applicata in `Theme.kt` (`QuietHoursTheme`):

- **Chiaro "Chai"**: carta crema, card in terracotta chiara, inchiostro cioccolato, accento arancio bruciato.
- **Scuro "Lo-fi night"**: una stanza illuminata da una lampada calda, pareti prugna-marrone, luce ambra.

`Theme.kt` assegna **tutti** i ruoli M3 in entrambi i temi (anche `surfaceContainer*`, `outline*`, `inverse*`): nessun ruolo ricade sui default viola di Material. Edge-to-edge: barre di sistema trasparenti, icone scure nel tema chiaro e chiare nello scuro. Nel codice usa **sempre** i ruoli di `MaterialTheme.colorScheme`, mai i valori esadecimali (né `Color.White`/`Color.Black`). Unica eccezione: un colore usato solo come maschera con un `BlendMode` (il ritaglio della barra in `MutedBellIcon`), con un commento.

Riferimento visivo: [`mockups/home_active_warm.svg`](mockups/home_active_warm.svg) (Home con blocco attivo, palette calda). La palette dell'app nasce da lì; il layout del mock-up arriva nella fase 3 della revisione.

| Ruolo | Chai (chiaro) | Lo-fi night (scuro) | Uso |
|---|---|---|---|
| `primary` / `onPrimary` | `#A6490C` arancio bruciato / `#FFFFFF` | `#FFB575` ambra lampada / `#4D2200` | CTA "Seleziona app", Switch acceso, fascia della `Timeline24h`, campanella e alone in `ACTIVE_INSIDE`, intestazioni di sezione nella lista app, icone d'accento (sole nella `ScheduleCard`, `ZenNotificationCard`), riquadro del lucchetto nel titolo e cerchio della freccia tra gli orari (con `onPrimary`), etichetta "Ora" della timeline dentro la fascia |
| `primaryContainer` / `onPrimaryContainer` | `#F0B899` terracotta chiara / `#3E1B0A` | `#7A3A12` / `#FFDCC4` | Card di stato "sto bloccando ora", badge contatore nella CTA, riga app selezionata (alpha 0.35) |
| `secondary` / `onSecondary` | `#0B7350` smeraldo / `#FFFFFF` | `#7FD6AC` / `#00391F` | Tempo per te, badge positivi: icona in `ACTIVE_OUTSIDE`, badge pieno "9 ore per te" nella `ScheduleCard` (con `onSecondary`) |
| `secondaryContainer` / `onSecondaryContainer` | `#C9EEDA` / `#073A28` | `#145B3F` / `#BFF2D6` | Card di stato "in attesa" (attivo, ma ora è consentito) |
| `tertiary` / `onTertiary` | `#7A4A9E` viola del crepuscolo / `#FFFFFF` | `#D8B8F2` / `#3C1D57` | Accenti di sera e riposo: luna nella `ScheduleCard` quando la fascia non ha sole; icona e bottone della card del permesso |
| `tertiaryContainer` / `onTertiaryContainer` | `#EEDDF8` / `#35164F` | `#56377A` / `#F2DCFF` | Avvisi soft (card del permesso mancante) |
| `background` / `onBackground` | `#FBF3E6` crema / `#3E2417` cioccolato | `#1A1210` prugna-marrone / `#F7E8DB` | Sfondo schermate e bottom bar della Home (la CTA galleggia, niente banda) |
| `surface` / `onSurface` | `#FFF9F1` / `#3E2417` | `#1F1613` / `#F7E8DB` | Intestazioni della lista app |
| `surfaceVariant` / `onSurfaceVariant` | `#F1DAC4` / `#6B4633` | `#4A3931` / `#DBC2B1` | Binario della timeline, skeleton; `onSurfaceVariant` per testi secondari |
| `surfaceContainerLowest` | `#FFFCF8` | `#140D0B` | Disco della campanella in `ACTIVE_INSIDE` e `DISABLED`, pillole degli orari |
| `surfaceContainerLow` | `#FAEEE1` | `#241A17` | Card dello skeleton |
| `surfaceContainer` | `#F9E8D6` | `#2B201C` | `SceneCard`, `EndOfShiftCard`, `ZenNotificationCard`, riga app non selezionata |
| `surfaceContainerHigh` | `#F6DFC8` | `#362924` | `ScheduleCard`, barra di ricerca, dialog del `TimePicker` |
| `surfaceContainerHighest` | `#F3D5B9` | `#42332C` | Card di stato `DISABLED` (un gradino più scura della `ScheduleCard`); default M3 (binario dello Switch spento, con bordo `outline`; quadrante del `TimePicker`) |
| `outline` / `outlineVariant` | `#8F6D5B` / `#E2C6AE` | `#A68C7D` / `#54433A` | Fascia della timeline a blocco spento; bordo della riga app (`outlineVariant`) |
| `inverseSurface` / `inverseOnSurface` / `inversePrimary` | `#3A2A22` / `#FBEDE2` / `#FFB575` | `#F7E8DB` / `#3E2417` / `#A6490C` | Solo default M3 (Snackbar, tooltip): oggi non usati |

L'illustrazione (`ZenScene`) mantiene la sua palette in `ui/theme/ZenPalette.kt`, invariata in questa fase: le scene lo-fi arrivano nella fase 2 (vedi "Luce del giorno e stagioni").

### Mappa degli stati

| `HeroStatus` | Container | Icona / tinta | Significato |
|---|---|---|---|
| `DISABLED` | `surfaceContainerHighest` | campanella, `onSurfaceVariant` | Blocco spento |
| `ACTIVE_OUTSIDE` | `secondaryContainer` | campanella barrata, `secondary` | Acceso, fuori fascia |
| `ACTIVE_INSIDE` | `primaryContainer` + alone che respira | campanella barrata `primary` su disco opaco `surfaceContainerLowest` (5.74 / 11.1, anche al picco dell'alone) | Sta bloccando ora |

### Contrasto

Rapporti WCAG 2.1 calcolati sugli esadecimali di `Color.kt` (Chai / Lo-fi night). Soglie: **4.5:1** per il testo normale, **3:1** per icone e testo grande.

Coppie `onX` / `X`:

| Coppia | Chai | Lo-fi night |
|---|---|---|
| `onPrimary` / `primary` | 5.87 | 7.85 |
| `onPrimaryContainer` / `primaryContainer` | 8.80 | 6.67 |
| `onSecondary` / `secondary` | 5.86 | 7.55 |
| `onSecondaryContainer` / `secondaryContainer` | 10.17 | 6.50 |
| `onTertiary` / `tertiary` | 6.36 | 8.01 |
| `onTertiaryContainer` / `tertiaryContainer` | 11.84 | 7.40 |
| `onBackground` / `background` | 12.97 | 15.40 |
| `onSurface` / `surface` | 13.66 | 14.82 |
| `onSurfaceVariant` / `surfaceVariant` | 6.11 | 6.44 |
| `inverseOnSurface` / `inverseSurface` | 11.94 | 11.92 |

Testo sulle superfici (Chai / Lo-fi night; in grassetto i valori sotto 4.5):

| Superficie | `onSurface` | `onSurfaceVariant` | `primary` come testo |
|---|---|---|---|
| `background` | 12.97 / 15.40 | 7.48 / 10.87 | 5.33 / 10.65 |
| `surface` | 13.66 / 14.82 | 7.87 / 10.46 | 5.61 / 10.25 |
| `surfaceContainerLowest` | 13.97 / 16.04 | 8.05 / 11.32 | 5.74 / 11.10 |
| `surfaceContainerLow` | 12.51 / 14.18 | 7.21 / 10.01 | 5.13 / 9.81 |
| `surfaceContainer` | 11.94 / 13.21 | 6.88 / 9.32 | 4.90 / 9.14 |
| `surfaceContainerHigh` | 11.10 / 11.68 | 6.40 / 8.25 | 4.56 / 8.08 |
| `surfaceContainerHighest` | 10.23 / 10.06 | 5.90 / 7.10 | **4.20** / 6.96 |
| `surfaceVariant` | 10.59 / 9.12 | 6.11 / 6.44 | **4.35** / 6.31 |
| `primaryContainer` | 8.18 / 7.18 | 4.72 / 5.06 | **3.36** / 4.96 |

Bianco su `primary`: 5.87 in Chai (è `onPrimary`), **1.73** in Lo-fi night, dove il testo su `primary` è `onPrimary` `#4D2200` (7.85). Per questo sul `primary` si usa sempre `onPrimary`, mai un bianco fisso.

### Regole

- **`primary` come testo mai su `surfaceContainerHighest` né su `primaryContainer`** (in Chai scende a 4.20 e 3.36); lo stesso vale per `surfaceVariant` (4.35). Come testo va bene su `background`, `surface`, `surfaceContainerLow`, `surfaceContainer`, `surfaceContainerHigh` (≥ 4.5 in entrambi i temi). Sul `primaryContainer` il testo è `onPrimaryContainer`; come **icona** (≥ 3:1) il `primary` regge il `primaryContainer` pieno (3.36, icona del messaggio "vita"), ma non una superficie schiarita da trasparenze o dall'alone: per questo la campanella di `ACTIVE_INSIDE` sta su un disco opaco `surfaceContainerLowest`.
- `secondary` ha la stessa luminanza di `primary` (stessi rapporti, ±0.01): stessa regola.
- La card di stato ha un gradiente verso `primary`/`secondary` al **6%**: più forte, il sottotitolo (alpha 0.85) nel tema scuro scende sotto 4.5 (al 18% era 3.96). Caso peggiore, all'estremità del gradiente: 4.83 (`ACTIVE_INSIDE`) e 4.73 (`ACTIVE_OUTSIDE`) in Lo-fi night, ≥ 5.96 in Chai.
- Indicatore "ora" della `Timeline24h`: anello `onSurface` con centro `surface`, alone `onSurface` al 12%. Il centro si vede sulla fascia `primary` (5.61 / 10.25), l'anello sul binario (10.59 / 9.12). Non usare `secondary`: ha la stessa luminanza di `primary` (1:1) e sparisce sulla fascia.
- `FilterChip` selezionati (lista app): `primaryContainer` / `onPrimaryContainer`, non il default M3 `secondaryContainer` (smeraldo).
- Se cambi un esadecimale in `Color.kt`, ricalcola le tabelle qui sopra per entrambi i temi.
- `dynamicColor` è `false` di proposito: la palette calda fa parte dell'identità. Se un giorno lo abiliti, gli stati devono restare distinguibili.
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

Font **Manrope** (Google Fonts scaricabili, `ui/theme/Type.kt`), scala M3 completa. Pesi caricati: Normal, Medium, SemiBold, Bold, ExtraBold (usa solo questi: un peso non dichiarato in `ManropeFontFamily` ricade sul più vicino).

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

`ui/theme/Shape.kt`. Nei componenti usa sempre `MaterialTheme.shapes.*` (o `CircleShape`), mai `RoundedCornerShape(N.dp)`:

| Token | Raggio | Uso |
|---|---|---|
| `extraSmall` | 8dp | Righe dello skeleton (`ShimmerSkeleton`) |
| `small` | 12dp | Bottoni dentro le card (`PermissionCard`, `ZenNotificationCard`) |
| `medium` | 20dp | Card secondarie (`PermissionCard`, `EndOfShiftCard`, `ZenNotificationCard`), riga app, pillole degli orari |
| `large` | 28dp | Card principali (card di stato, `ScheduleCard`), dialog del `TimePicker` |
| `extraLarge` | 32dp | `SceneCard` |
| `CircleShape` | pill | Bottone CTA, barra di ricerca, filtri, badge, contenitori icone |

Spaziature su griglia **4dp**: 4 · 8 · 12 · 16 · 20 · 24.

- Margine orizzontale delle schermate: **16dp**. Spazio tra le card: **16dp**. Padding interno delle card: **20dp**. Niente card annidate: lo switch sta nella riga del titolo della card di stato.
- Target di tocco ≥ **48dp**. CTA principale alta **56dp**.
- Rispetta sempre gli insets (`WindowInsets.safeDrawing`, `navigationBarsPadding()`): l'app è edge-to-edge.

## Iconografia

- Solo `material-icons-core` (Filled). Se serve un'icona che non c'è, copiala come vector drawable in `res/drawable/` (es. `ic_moon.xml`, `ic_sun_dim.xml`) invece di aggiungere `material-icons-extended`.
- Dimensioni: 22–24dp inline, 28–30dp in contenitori circolari da 56dp, 32dp accanto ai titoli.
- Icone decorative con `contentDescription = null`; quelle interattive con una stringa `cd_*` tradotta.

## Componenti

Ordine della Home (come `docs/mockups/home_active_warm.svg`): titolo con lucchetto → card dei permessi (se servono) → card di stato → `SceneCard` → `ScheduleCard`; in fondo la CTA "App da silenziare".

| Componente | File | Note |
|---|---|---|
| Card di stato | `HeroHeader.kt` | Una riga: campanella, stato breve ("Attivo" / "In attesa" / "Disattivato", `headlineSmall` ExtraBold) e Switch con la spunta; sotto il sottotitolo (+ saluto in grassetto dentro la fascia) e il messaggio "vita". Colore animato per stato, testi in `AnimatedContent` |
| Campanella animata | `MutedBellIcon.kt` | Stato "silenziato" = barra diagonale; vedi `MOTION.md` |
| Orari | `ScheduleCard.kt`, `Timeline24h.kt` | Titolo + badge "N ore per te", pillole INIZIO → FINE (`surfaceContainerLowest`, orario 24sp ExtraBold, freccia in un cerchio `primary`), timeline con l'etichetta "Ora 16:30" sopra l'indicatore. Tocca un orario per aprire il `TimePicker` 24h in `AlertDialog` |
| Permesso | `PermissionCard.kt` | Visibile solo senza accesso; porta alle impostazioni di sistema |
| Riga app | `AppItemRow.kt` | Bordo e container animati (`Motion.SHORT`) quando l'app è selezionata |
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
