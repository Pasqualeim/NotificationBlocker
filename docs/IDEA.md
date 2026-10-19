# L'idea di Unclock

## La domanda

> Perché esistono app che ti premiano e ti mostrano statistiche solo per quanto tempo ti concentri e lavori, mentre non esiste niente per il tempo libero e la salute personale?

Unclock nasce da qui. Le app di focus (Forest, Flora, Finch) contano le ore date al lavoro e allo studio. Unclock conta le ore che ti riprendi: le sere, i weekend, il pranzo, il tempo con la luce del giorno, senza che il lavoro bussi dal telefono.

Ogni scelta dell'app (funzioni, testi, grafici, premi futuri) si misura su questa domanda. Lo stato del progetto è in [`ROADMAP.md`](ROADMAP.md).

## Le quattro regole

1. **Unclock non chiede mai niente.** Niente notifiche sue per farsi aprire, niente "ci manchi", niente da mantenere in vita.
2. **Il tempo libero si vive, non si avvia.** La pausa parte da sola fuori dall'orario di lavoro: nessun timer da far partire, nessuna sessione da completare.
3. **Nessuna punizione.** Una sera di lavoro o un weekend senza pausa non tolgono niente: semplicemente quel giorno non si aggiunge nulla.
4. **Ricordi, non gare.** Quello che Unclock mostra racconta il tempo ripreso; non è un numero da battere.

## Checklist anti-stress

Ogni idea nuova (statistiche, premi, testi) passa da qui. Se una risposta è "sì", l'idea va rifatta.

| Domanda | Perché |
|---|---|
| C'è una serie di giorni da non interrompere (streak)? | Una serie rotta è una piccola colpa |
| Qualcosa si perde, si rompe o peggiora se non apri l'app o lavori una sera? | La sera di lavoro capita: quella sera non si aggiunge niente, fine |
| Un giorno senza pausa appare rosso, vuoto o "mancato"? | Va mostrato neutro |
| C'è un confronto con altre persone o con "la settimana scorsa" in percentuale? | Il tempo libero non è una gara |
| C'è un conto alla rovescia o una scadenza? | Mette fretta |
| Unclock manda notifiche per chiedere attenzione? | Unclock toglie notifiche, non ne aggiunge |
| Su Home c'è un traguardo numerico in evidenza (barra, "mancano 12 h")? | Trasforma la pausa in un obiettivo. I numeri stanno in Statistiche, che apri se ti va |

## Le app di focus, al contrario

| Nelle app di focus | In Unclock |
|---|---|
| Forest: l'albero cresce mentre lavori o studi; se esci dall'app muore e resta secco | Conta il tempo di pausa vissuto; aprire un'app di lavoro per un'urgenza non conta contro (Unclock non lo vede). "Termina la pausa" ferma solo il conteggio |
| Forest, Flora: sessioni da avviare a mano col timer | Niente da avviare: la pausa parte da sola quando smetti di lavorare |
| Flora: in gruppo si vede chi ha fatto morire l'albero | Niente social, niente classifiche |
| Flora: gli alberi durano 7 giorni, per spingere alla costanza | Niente scade |
| Finch: energia guadagnata completando obiettivi; l'enfasi sulla costanza quotidiana pesa ad alcuni utenti | Nessun obiettivo: basta vivere la pausa che hai già |
| Statistiche: ore di focus al giorno, alla settimana, al mese | Statistiche del tempo tuo (sotto) |

Fonti: [Forest (Headway)](https://makeheadway.com/blog/forest-app-review/), [Forest (Desirability Lab)](https://desirabilitylab.com/posts/forest), [Flora (Business of Business)](https://www.businessofbusiness.com/articles/flora-focus-productivity-app-review/), [Flora (App Store)](https://apps.apple.com/app/id1225155794), [Finch (Headway)](https://makeheadway.com/blog/finch-app-review/), [Finch, critica (Vocal)](https://vocal.media/critique/the-criousity-of-the-finch-app).

Il vantaggio che le altre non hanno: Unclock sa **quando** sei libero e **quante notifiche di lavoro** ha lasciato fuori, senza chiederti di fare niente.

## Le statistiche del tempo tuo

| Statistica | Da dove viene | Nota |
|---|---|---|
| Tempo libero del giorno, della settimana, del mese | `FreeTimeLedger` (storico per giorno), `Stats.kt` | Il sonno non conta (`SleepWindow`) |
| **Ore di luce vissute in pausa** | Pausa vissuta incrociata con alba e tramonto (`SunTimes`), nello storico come `daylightMinutes` | Il dato che nessun'altra app ha |
| Notifiche di lavoro lasciate fuori | `held` per giorno nello storico | Anche nella notifica della pausa e nel resoconto |

Un giorno senza pausa è neutro, mai rosso. Niente "meglio della settimana scorsa".
