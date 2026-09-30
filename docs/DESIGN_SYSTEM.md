# Design system

Linee guida visive di **Nook**. La base è **Material 3**: se una regola qui non copre un caso, vale la [specifica M3](https://m3.material.io/). Le animazioni hanno un documento a parte: [`MOTION.md`](MOTION.md).

## Principi

1. **Calma.** L'app parla di silenzio: niente colori saturi urlati, niente badge rossi, niente allarmismi. Toni caldi da "lo-fi anime" (carta crema, terracotta, cioccolato): l'arancio della lampada è il colore del "silenzio attivo", lo smeraldo quello di "acceso, tutto sotto controllo".
2. **Stato leggibile in un secondo.** Aprendo l'app devi capire subito se le notifiche vengono bloccate *adesso*. Colore della card, icona e titolo dicono tutti la stessa cosa.
3. **Nativo Android.** Componenti Material 3 standard (Switch, TimePicker, TopAppBar, FilterChip) prima di quelli custom. Un componente custom si giustifica solo se comunica meglio lo stato (es. `Timeline24h`, `MutedBellIcon`).
4. **Un tocco per l'azione principale.** Il toggle è sempre visibile senza scroll; la CTA "Seleziona app" è fissa in basso, nella zona del pollice. Fuori dall'app, il riquadro "Stacco & Sole" nelle Impostazioni rapide fa la stessa cosa.
5. **Staccare è vivere.** Abbiamo una vita oltre il lavoro: l'app esiste per restituirla, non per "far dormire". Il tempo libero non è "notte e sonno": è luce, tempo, vita, a qualsiasi ora finisca il lavoro. L'app valorizza le ore di sole che restano dopo il lavoro (d'estate, staccare alle 17 vuol dire ancora tre ore di sole) e segue la luce reale del giorno e della stagione.

## Icona dell'app

Una **tazza fumante su fondo terracotta**: la pausa che ti riprendi, non il lavoro che manca. L'icona non deve parlare di notte, sonno o "non disturbare" (luna, stelle, zzz), né di un divieto (campanella barrata, cartello di stop): sono concetti di un'app per dormire, non di questa.

- Tazza e volute crema (`#F8EFE3`) su gradiente terracotta desaturato (`#BA6C4D` → `#A45A3D`), leggermente più chiaro in alto: stessa famiglia del `primary`, un po' più chiara perché l'icona non porta testo.
- Icona adattiva (sfondo + primo piano vettoriale) e variante monocromatica: lo stesso primo piano, un solo colore. Simbolo nella zona sicura del 66%, leggibile a 48 px.
- **Centratura**: né il rettangolo che contiene il disegno né il puro baricentro bastano (il primo lo fa sembrare basso, il secondo alto). In orizzontale vince il baricentro, perché il manico sbilancia il rettangolo; in verticale lo script prende la via di mezzo tra baricentro e rettangolo. Il vapore sta sullo stesso asse dell'insieme tazza e manico (x 55,7), non sul solo corpo. Vale per launcher e header (`ic_mug`).
- Generata da `tools/icon/launcher_icon.py` (modifica lo script, non i file): scrive `ic_launcher_foreground.xml`, `ic_launcher_background.xml` e il PNG 512 dello store (`docs/store/icon-512.png`). Nata da una bozza di Gemini, ridisegnata a mano in vettoriale.

## Nome

L'app si chiama **Nook**, lo stesso in italiano e in inglese (niente traduzione del nome): l'angolo tutto tuo, fuori dal lavoro. Nomi da utility ("Blocker", "Shield", "Guard", "Mute", "Lock", "Filter") ricordano il lavoro e la difesa; un nome breve e domestico no.

- Una sillaba, consonanti nasali e continue (N) con vocale lunga e arrotondata; richiama il rituale e lo spazio proprio (chai, nook, haven).
- Sullo store il titolo aggiunge le parole chiave (max 30 caratteri) e può cambiare per lingua: "Nook: stacca dal lavoro" / "Nook: Work-Life Balance".
- **Rischio marchio, da chiudere prima di pubblicare**: NOOK è un marchio di Barnes & Noble (e-reader, con app "NOOK" su Play, categoria libri). L'app è in un'altra categoria (produttività), ma una segnalazione è possibile. Prima del lancio: cercare "Nook" su EUIPO/UIBM e USPTO (classi 9 e 42) e sulla ricerca di Play; se serve, ripiegare su un nome composto o su un'alternativa (vedi sotto).
- Scartati dopo una ricerca: *Sosta* (in italiano è il parcheggio, "divieto di sosta"; esiste anche un'app di parcheggi), *Respiro* (app medica omonima su Play), *Lume* (app di focus omonima), *Lull* e *Mellow* (molte app di calma e sonno), *Oltre* (suite di presenze Zucchetti), *Sundown* (parla di sera). Riserve: *Tempo Mio*, *Dopo*, *Riva*.
- `applicationId` `com.pasquale.nook` (non si cambia più dopo la pubblicazione); il package Kotlin resta `com.pasquale.notificationblocker`, invisibile all'utente.

## Colore

Palette calda "lo-fi anime", definita in `ui/theme/Color.kt` e applicata in `Theme.kt` (`QuietHoursTheme`):

- **Chiaro "Chai"**: carta crema (mai bianco puro), card in terracotta pallida, inchiostro cioccolato, accento terracotta morbida; salvia per il tempo tuo, blu polvere per le informazioni.
- **Scuro "Lo-fi night"**: una stanza illuminata da una lampada calda, pareti color carbone caldo, luce pesca tenue.

### Perché questi colori

- **La saturazione conta più della tinta.** Nello studio di Valdez & Mehrabian (1994, *Journal of Experimental Psychology: General*) l'attivazione emotiva cresce soprattutto con la saturazione (arousal ≈ −0.31 luminosità + 0.60 saturazione), la piacevolezza con la luminosità (pleasure ≈ 0.69 luminosità + 0.22 saturazione). Quindi: accenti desaturati, superfici chiare e calde. Saturazione HSL del `primary` scesa da 0.87 a 0.50 (Chai) e da 1.00 a 0.65 (Lo-fi night).
- **Tinte**: blu, blu-verde e verde sono tra le più piacevoli nello stesso studio; per questo il `secondary` è salvia e il `tertiary` blu polvere (carta da zucchero). La terracotta resta l'unico accento forte: calda, domestica, senza l'allerta del rosso puro.
- **Niente affermazioni sulla salute.** Questi principi guidano il design; non scriviamo nello store che l'app "abbassa lo stress" o la pressione.
- **Leggibilità prima di tutto**: nessuna riduzione di contrasto sotto 4.5:1 per il testo. `tools/palette/contrast.py` rigenera le tabelle qui sotto e fallisce se una coppia obbligatoria scende sotto soglia.

`Theme.kt` assegna **tutti** i ruoli M3 in entrambi i temi (anche `surfaceContainer*`, `outline*`, `inverse*`): nessun ruolo ricade sui default viola di Material. Edge-to-edge: barre di sistema trasparenti, icone scure nel tema chiaro e chiare nello scuro. Nel codice usa **sempre** i ruoli di `MaterialTheme.colorScheme`, mai i valori esadecimali (né `Color.White`/`Color.Black`). Unica eccezione: un colore usato solo come maschera con un `BlendMode` (il ritaglio della barra in `MutedBellIcon`), con un commento.

Riferimento visivo: [`mockups/home_active_warm.svg`](mockups/home_active_warm.svg) (Home con blocco attivo, palette calda). La palette dell'app nasce da lì; il layout del mock-up arriva nella fase 3 della revisione.

| Ruolo | Chai (chiaro) | Lo-fi night (scuro) | Uso |
|---|---|---|---|
| `primary` / `onPrimary` | `#9A4F34` terracotta morbida / `#FFFFFF` | `#E8B093` pesca lampada / `#4A2414` | CTA "Seleziona app", Switch acceso, fascia della `Timeline24h`, campanella e alone in `ACTIVE_INSIDE`, intestazioni di sezione nella lista app, icone d'accento (sole nella `ScheduleCard`, `ZenNotificationCard`), riquadro della tazza nel titolo e cerchio della freccia tra gli orari (con `onPrimary`), etichetta "Ora" della timeline dentro la fascia |
| `primaryContainer` / `onPrimaryContainer` | `#EBCDBC` terracotta pallida / `#3B1E11` | `#6B3B27` / `#FBDCCB` | Card di stato "sto bloccando ora", badge contatore nella CTA, riga app selezionata (alpha 0.35) |
| `secondary` / `onSecondary` | `#4B6A55` salvia / `#FFFFFF` | `#ABC8B2` / `#1B3424` | Tempo per te, badge positivi: icona in `ACTIVE_OUTSIDE`, badge pieno "9 ore per te" nella `ScheduleCard` (con `onSecondary`) |
| `secondaryContainer` / `onSecondaryContainer` | `#D6E5D8` / `#16301F` | `#34503D` / `#D2E7D7` | Card di stato "in attesa" (attivo, ma ora è consentito) |
| `tertiary` / `onTertiary` | `#4C6682` blu polvere / `#FFFFFF` | `#B1C5DC` / `#1C3048` | Informazioni e accenti tranquilli: luna nella `ScheduleCard` quando la fascia non ha sole; icona e bottone della card del permesso |
| `tertiaryContainer` / `onTertiaryContainer` | `#DCE5EF` / `#1A2E43` | `#384C64` / `#D9E5F3` | Avvisi soft (card del permesso mancante) |
| `background` / `onBackground` | `#F8F2E9` crema / `#3A2B22` cioccolato | `#1B1714` carbone caldo / `#F2E7DD` | Sfondo schermate e bottom bar della Home (la CTA galleggia, niente banda) |
| `surface` / `onSurface` | `#FCF8F2` / `#3A2B22` | `#201B18` / `#F2E7DD` | Intestazioni della lista app |
| `surfaceVariant` / `onSurfaceVariant` | `#EDE0D2` / `#65503F` | `#4A3F37` / `#D6C5B8` | Binario della timeline, skeleton; `onSurfaceVariant` per testi secondari |
| `surfaceContainerLowest` | `#FFFDF9` | `#151210` | Disco della campanella in `ACTIVE_INSIDE` e `DISABLED`, pillole degli orari |
| `surfaceContainerLow` | `#F6EEE4` | `#241E1B` | Card dello skeleton |
| `surfaceContainer` | `#F2E8DC` | `#2A2420` | `SceneCard`, `ZenNotificationCard`, riga app non selezionata |
| `surfaceContainerHigh` | `#EEE2D4` | `#352E29` | `ScheduleCard`, barra di ricerca, dialog del `TimePicker` |
| `surfaceContainerHighest` | `#E9DBCB` | `#403833` | Card di stato `DISABLED` (un gradino più scura della `ScheduleCard`); default M3 (binario dello Switch spento, con bordo `outline`; quadrante del `TimePicker`) |
| `outline` / `outlineVariant` | `#8C7565` / `#DDCDBD` | `#A0907F` / `#52463E` | Fascia della timeline a blocco spento; bordo della riga app (`outlineVariant`) |
| `inverseSurface` / `inverseOnSurface` / `inversePrimary` | `#3A2E27` / `#F7EEE5` / `#EBB396` | `#F2E7DD` / `#3A2B22` / `#9A4F34` | Solo default M3 (Snackbar, tooltip): oggi non usati |

L'illustrazione (`ZenScene`) mantiene la sua palette in `ui/theme/ZenPalette.kt`, invariata in questa fase: le scene lo-fi arrivano nella fase 2 (vedi "Luce del giorno e stagioni").

### Mappa degli stati

| `HeroStatus` | Container | Icona / tinta | Significato |
|---|---|---|---|
| `DISABLED` | `surfaceContainerHighest` | campanella, `onSurfaceVariant` | Blocco spento |
| `ACTIVE_OUTSIDE` | `secondaryContainer` | campanella barrata, `secondary` | Acceso, fuori fascia |
| `ACTIVE_INSIDE` | `primaryContainer` + alone che respira | campanella barrata `primary` su disco opaco `surfaceContainerLowest` (5.84 / 9.83, anche al picco dell'alone) | Sta bloccando ora |

### Contrasto

Rapporti WCAG 2.1 calcolati sugli esadecimali di `Color.kt` (Chai / Lo-fi night). Soglie: **4.5:1** per il testo normale, **3:1** per icone e testo grande.

Coppie `onX` / `X`:

| Coppia | Chai | Lo-fi night |
|---|---|---|
| `onPrimary` / `primary` | 5.93 | 7.12 |
| `onPrimaryContainer` / `primaryContainer` | 10.14 | 7.11 |
| `onSecondary` / `secondary` | 6.01 | 7.44 |
| `onSecondaryContainer` / `secondaryContainer` | 10.87 | 6.84 |
| `onTertiary` / `tertiary` | 5.95 | 7.59 |
| `onTertiaryContainer` / `tertiaryContainer` | 10.88 | 6.90 |
| `onBackground` / `background` | 12.18 | 14.63 |
| `onSurface` / `surface` | 12.82 | 14.01 |
| `onSurfaceVariant` / `surfaceVariant` | 5.84 | 6.09 |
| `inverseOnSurface` / `inverseSurface` | 11.45 | 11.14 |

Testo sulle superfici (Chai / Lo-fi night; in grassetto i valori sotto 4.5):

| Superficie | `onSurface` | `onSurfaceVariant` | `primary` come testo |
|---|---|---|---|
| `background` | 12.18 / 14.63 | 6.81 / 10.63 | 5.33 / 9.38 |
| `surface` | 12.82 / 14.01 | 7.16 / 10.18 | 5.60 / 8.98 |
| `surfaceContainerLowest` | 13.35 / 15.32 | 7.46 / 11.14 | 5.84 / 9.83 |
| `surfaceContainerLow` | 11.80 / 13.51 | 6.59 / 9.82 | 5.16 / 8.67 |
| `surfaceContainer` | 11.20 / 12.58 | 6.26 / 9.14 | 4.90 / 8.07 |
| `surfaceContainerHigh` | 10.63 / 10.96 | 5.94 / 7.96 | 4.65 / 7.03 |
| `surfaceContainerHighest` | 9.98 / 9.42 | 5.58 / 6.85 | **4.36** / 6.04 |
| `surfaceVariant` | 10.46 / 8.38 | 5.84 / 6.09 | 4.57 / 5.38 |
| `primaryContainer` | 9.03 / 7.57 | 5.05 / 5.50 | **3.95** / 4.85 |

Card di stato, caso peggiore (gradiente al 6% verso l'accento, sottotitolo con alpha 0.85):

- `ACTIVE_INSIDE`: 6.64 in Chai, 5.12 in Lo-fi night
- `ACTIVE_OUTSIDE`: 6.87 in Chai, 4.94 in Lo-fi night

Saturazione HSL degli accenti (Chai / Lo-fi night): `primary` 0.50 / 0.65, `secondary` 0.17 / 0.21, `tertiary` 0.26 / 0.38

Bianco su `primary`: 5.93 in Chai (è `onPrimary`); in Lo-fi night il testo su `primary` è `onPrimary` `#4A2414` (7.12), perché il `primary` scuro è chiaro. Per questo sul `primary` si usa sempre `onPrimary`, mai un bianco fisso.

### Regole

- **`primary` come testo mai su `surfaceContainerHighest` né su `primaryContainer`** (in Chai scende a 4.36 e 3.95). Come testo va bene su `background`, `surface`, `surfaceContainerLow`, `surfaceContainer`, `surfaceContainerHigh` (≥ 4.5 in entrambi i temi). Sul `primaryContainer` il testo è `onPrimaryContainer`; come **icona** (≥ 3:1) il `primary` regge il `primaryContainer` pieno (3.95, icona del messaggio "vita"), ma non una superficie schiarita da trasparenze o dall'alone: per questo la campanella di `ACTIVE_INSIDE` sta su un disco opaco `surfaceContainerLowest`.
- `secondary` e `tertiary` hanno quasi la stessa luminanza di `primary` (rapporti entro ±0.1): stessa regola.
- La card di stato ha un gradiente verso `primary`/`secondary` al **6%**: più forte, il sottotitolo (alpha 0.85) nel tema scuro scende verso 4.5. Casi peggiori nella tabella sopra.
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
| `DAYLIGHT` | fino a 70' prima del tramonto | azzurro, sole alto, raggi | sole tra i bambù, "Il sole è ancora alto…" | sole, "Nook" | "Ti restano 3 h 20 min di sole…" |
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
| `medium` | 20dp | Card secondarie (`PermissionCard`, `ZenNotificationCard`), riga app, pillole degli orari |
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

Ordine della Home (come `docs/mockups/home_active_warm.svg`): titolo con la tazza (`ic_mug`, la stessa dell'icona) → card dei permessi (se servono) → card di stato → `SceneCard` → `ScheduleCard`; in fondo la CTA "App da silenziare".

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
| Riquadro "Nook" | `tile/ZenTileService.kt` | Attivo = blocco acceso. Icona monocromatica per fase (foglia, sole, lanterna), sottotitolo "Fino alle 09:00" / "Dalle 17:00" / "Spento" |
| Icona barra di stato | `ic_notification_zen.xml` | Ensō monocromatico |

Nuovi componenti: stateless, in `ui/components/`, con `modifier` come primo parametro opzionale e una `@Preview` per stato (chiaro + scuro).

## Testi e tono

- Seconda persona, frasi brevi, niente gergo tecnico ("accesso alle notifiche", non "Notification Listener").
- Rassicurante sul fatto che nulla va perso nell'app di origine; onesto sul fatto che le notifiche bloccate non ritornano.
- Ogni stringa in `values/strings.xml` **e** `values-it/strings.xml`; `plurals` per i conteggi (in italiano `one`, `many`, `other`).
- **Scritti da una persona, non da una macchina.** Parole di tutti i giorni, come le direbbe un amico: "Il lavoro può aspettare", "Stacchi alle 22:00", "Le trovi nelle rispettive app". Niente frasi poetiche o motivazionali ("respira e rilassati", "bentornato alla tua vita"), niente due punti a effetto, niente trattini lunghi, niente elenchi forzati da tre. Una frase dice un fatto concreto (un orario, una durata, un numero) o non c'è.
- **Revisione con `unslop`.** Prima di aggiungere o cambiare testi, passali con le regole della skill `unslop` (`npx skills add cursor/plugins@unslop`, installata in `.agents/`, ignorata da git): voce attiva, parola semplice, niente riempitivi. I testi attuali partono da una riscrittura più naturale fatta con Gemini, poi corretta così.
- **Parole fisse.** "Pausa" per il tempo libero (mai "blocco" o "quiete" nei testi dell'app), "app di lavoro" per quelle scelte, "stacchi" per la fine del lavoro. Il nome del riquadro e dell'app è **Nook** in tutte le lingue.
- **Tono "vita"** (messaggi di Home, notifica): parla del tempo e della luce che restano, non del lavoro che manca. Concreto ("Ti restano 5 h 58 min di luce"), mai colpevolizzante, mai "vai a dormire". Esempi in `strings.xml` alle chiavi `life_*` e `zen_msg_*`.
- **Disclosure del permesso** (`permission_warning_text`): deve sempre dire cosa vede l'app, cosa non fa (non legge, non salva, niente esce dal telefono) e che si può revocare. È richiesta da Google Play: accorciala solo senza perdere questi quattro punti.

## Accessibilità

- Ogni controllo ha un nome leggibile da TalkBack (`semantics { contentDescription = … }` o testo visibile).
- Lo stato non è mai comunicato **solo** dal colore: c'è sempre anche testo o icona.
- Testa con carattere di sistema al 200% e con "Rimuovi animazioni" attivo.
