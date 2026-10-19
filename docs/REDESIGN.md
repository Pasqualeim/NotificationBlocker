# Nook 2: più pause, statistiche

Piano del rifacimento (fase di test, punto 1 della roadmap). Stato: **fatto** (più pause, storico, tre schede, revisione dei testi); il sito è il prossimo passo; Google Play per ultimo, dopo il test via Telegram/email.

> **Aggiornamento (ottobre 2026):** il modello sotto è quello di partenza. Oggi l'utente imposta **quando lavora** (`work_schedule`, `data/WorkSchedule.kt`) e le pause fuori dal lavoro (sera, giorni liberi) sono generate; le pause create a mano restano per il resto (pranzo). Vedi `CLAUDE.md`.

## Perché

Prima di Nook 2 c'era una sola fascia al giorno, nessuno storico e un personaggio (il germoglio, nello stash) messo da parte. Il proprietario vuole:

- **più pause al giorno**, ognuna con nome, orari e giorni (pranzo, sera, weekend);
- **statistiche vere**, con un grafico grande per giorno, settimana o mese;
- tutto questo senza perdere la sensazione di semplicità e tranquillità.

## Decisioni prese

| Tema | Scelta |
|---|---|
| Cosa conta | Tempo di pausa vissuto + ore di luce vissute in pausa |
| Pause | Nome, inizio, fine, giorni della settimana; più pause al giorno |
| App di lavoro | Una lista unica; ogni pausa usa "tutte le app di lavoro" e, solo se vuoi, "scegli per questa pausa" (vedi sotto) |
| Navigazione | Barra in basso con 3 voci, stile delle app di benessere digitale (vedi sotto) |
| Statistiche | Grafico grande con selettore Giorno / Settimana / Mese, periodi precedenti scorrendo |

## 1. Il modello delle pause

```kotlin
data class Pause(
    val id: String,            // stabile, generato alla creazione
    val name: String,          // "Pranzo", "Sera", "Weekend"
    val start: Int,            // minuti da mezzanotte
    val end: Int,              // start == end: tutto il giorno
    val days: Set<DayOfWeek>,  // giorni in cui la pausa INIZIA
    val enabled: Boolean,
    val apps: Set<String>?,    // null = tutte le app di lavoro (il caso normale)
)
```

- **Giorni:** contano dal giorno di inizio. "Sera" 18:30 → 08:30 di venerdì finisce sabato mattina ed è la pausa di venerdì.
- **Pause sovrapposte** (Sera tutti i giorni + Weekend tutto il giorno): ammesse. Una notifica è trattenuta se almeno una pausa attiva la blocca; ogni minuto vissuto conta una volta sola.
- **Occorrenza:** ogni volta che una pausa si ripete ha una chiave `data-di-inizio#id` (oggi la chiave è solo la data). La usano "Termina la pausa", la notifica della pausa scorsa via, il resoconto, lo storico.
- **Interruttore generale:** resta (Home e riquadro rapido) e spegne tutto; ogni pausa ha anche il suo.
- **Migrazione:** chi ha già `start_time`/`end_time` si ritrova una pausa "Sera" ogni giorno con le stesse app. Poi le chiavi vecchie non si leggono più.
- **Ancora niente allarmi:** tutto si decide guardando l'orologio quando arriva una notifica o quando l'app si aggiorna, come oggi.

### App per pausa, in modo semplice

Nell'editor della pausa una sola riga: **App da silenziare: Tutte le app di lavoro ›**. Toccandola si apre la lista di sempre con i soli spunti di quella pausa. Il 90% delle persone non la tocca mai. Nei dati è solo `apps = null` oppure un insieme.

### Regola di blocco (resta l'unica fonte di verità)

`shouldBlock(pkg) = attivo && esiste una pausa p tale che p.enabled && p è in corso adesso && p non è stata terminata && pkg ∈ (p.apps ?: appDiLavoro)`

Logica pura in `data/PauseSchedule.kt` (sostituisce `OffHours` per le finestre; `OffHours.format` resta), con i test sui casi: notte a cavallo dei giorni, tutto il giorno, sovrapposizioni, giorni non selezionati, terminata in anticipo, cambio d'ora legale.

## 2. Lo storico (la base delle statistiche)

Si recupera l'idea buona del germoglio (`FreeTimeLedger` nello stash): si salda il tempo tra due sguardi all'orologio, prima di ogni cambio di impostazioni e a ogni aggiornamento. Cambia cosa si salva: non più un totale, ma **un registro per giorno**.

| Per ogni giorno | Da dove |
|---|---|
| Minuti di pausa vissuti, per pausa | Saldo come nel germoglio, esteso a più pause |
| Di cui con la luce del giorno | Alba e tramonto già calcolati in `ZenEnvironment.sunTimes` (da spostare in `data/`, è logica pura) |
| Notifiche di lavoro lasciate fuori | Il contatore attuale, salvato per giorno invece che solo per l'ultima fascia |

- Salvato in `SharedPreferences` come testo compatto (circa 30 KB l'anno), quindi niente database e il backup Android lo porta con sé. Si tiene tutto: i grafici mensili e i totali "da sempre" ne hanno bisogno.
- Nessun nuovo permesso, niente rete: le risposte di Data safety non cambiano.

## 3. Le schermate

Barra in basso Material 3 con tre voci, come le app di benessere digitale; transizioni opache (regola di prestazioni).

| Voce | Cosa contiene |
|---|---|
| **Oggi** | La scena (sempre 2:1), lo stato con l'interruttore generale, la pausa in corso o la prossima ("Pranzo fino alle 14:00", "Sera alle 18:30"), una frase con un fatto concreto |
| **Pause** | Elenco delle pause come righe in una superficie raggruppata (nome, orari, giorni a pillole, interruttore), "Aggiungi pausa", editor a foglio dal basso. In fondo: "App di lavoro" (la lista unica) |
| **Statistiche** (era "Il tempo tuo") | Selettore Giorno / Settimana / Mese, grafico grande, frecce o scorrimento per i periodi precedenti, sotto i totali del periodo (ore tue, ore di luce, notifiche lasciate fuori) |

Grafici disegnati in Compose con `Canvas` (nessuna libreria nuova):
- **Giorno:** le 24 ore orizzontali, la fascia di luce sullo sfondo, i blocchi delle pause vissute colorati per pausa.
- **Settimana:** 7 colonne, ognuna divisa tra ore di luce e il resto.
- **Mese:** una colonna per giorno; oppure il calendario con l'intensità, da scegliere con i mockup.
- Un giorno senza pausa è un puntino neutro. Niente media, niente "meglio della settimana scorsa".

Il resto cambia di conseguenza:
- **Notifica della pausa:** il nome della pausa in corso ("Pranzo fino alle 14:00"); "Termina la pausa" chiude solo quell'occorrenza.
- **Riquadro rapido:** l'interruttore generale; il sottotitolo dice la pausa in corso o la prossima.
- **Resoconto:** dopo la fine di una pausa, le notifiche lasciate fuori da quando l'hai chiuso l'ultima volta (anche dopo il pranzo, non solo la mattina).

## 4. Fasi

Ogni fase finisce con test, lint, review `android-reviewer` e commit su `main`.

1. **[Fatta] Pause e regola di blocco.** `Pause`, `PauseSchedule` (puro, testato), salvataggio e migrazione, servizio, notifica della pausa, riquadro, resoconto. La UI resta quella di oggi, ma mostra la prima pausa: l'app funziona a ogni passo.
2. **[Fatta] Storico.** Registro per giorno (vissuto, luce, notifiche) e saldo; ore di sole spostate in `data/`. Test.
3. **[Fatta] Navigazione e schermate.** Barra in basso, Pause con editor, Statistiche (prima "Il tempo tuo") con i tre grafici, Oggi rifatta; poi restyling Material 3 nativo: `TopAppBar` condivisa, icona Home, righe `ListItem`. Prima i mockup (SVG come quelli di `docs/mockups/`), poi il codice. Controllo di Home in una schermata e dei frame sul Galaxy A32.
4. **[Fatta per l'app] Testi e documenti.** `copywriter` per tutte le stringhe IT/EN, `docs-keeper` per `CLAUDE.md`, `MOTION.md`, `DESIGN_SYSTEM.md`, store e sito.

Le fasi 1 e 2 non cambiano l'aspetto dell'app; dalla 3 si vede il nuovo Unclock.

## Verifica

- `./gradlew testDebugUnitTest`: `PauseSchedule` (notte, tutto il giorno, sovrapposte, giorni, terminata, ora legale), migrazione, storico (luce, più pause, nessun doppio conteggio, orologio che torna indietro).
- `./gradlew lintDebug` a 0 errori.
- Dall'emulatore: due pause brevi (es. tra 2 e 4 minuti da ora), notifiche di prova da `com.android.shell` (comandi in `CLAUDE.md`), controllo di cosa viene trattenuto, del registro e del resoconto dopo ciascuna.
- Sul Galaxy A32, build release: Oggi con la scena sempre a 2:1 (`wm size`, `font_scale`), frame a riposo con la pausa attiva (circa 150 in 5 s, non peggiorare), passaggio fra le tre voci senza scatti.
