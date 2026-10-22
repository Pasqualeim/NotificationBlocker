# Instagram

Profilo per trovare tester durante la fase di test (ROADMAP, sezione 0). Il tono è quello della pagina tester: Pasquale racconta cosa sta costruendo e cosa impara, errori compresi, e chiede aiuto. Niente "gratis", niente urgenza, niente inviti da pubblicità. Nessuna promessa di email: le novità stanno su Telegram.

## Profilo

- **Nome utente:** `@unclock_it` (come il canale Telegram).
- **Nome:** Unclock · Pasquale
- **Bio** (massimo 150 caratteri):

  > Sto costruendo Unclock, un'app Android che mette in pausa le notifiche di lavoro e conta il tempo libero. Cerco chi la provi e mi dica cosa non va.

  (147 caratteri)

- **Link:** https://unclock.netlify.app/it/prova
- **Immagine del profilo:** l'icona del lucchetto (`docs/store/icon-512.png`).

## Primi sei post

Formato 4:5 (1080×1350). Le schermate sono quelle del sito (`nook-site/public/assets/img/*-it.webp`), su uno sfondo della palette Chai. Le card di solo testo si preparano con il prompt in fondo.

### 1. La domanda

Immagine: card di testo con la domanda.

> Perché ci sono app che contano quanto lavori e quanto ti concentri, e niente per il tempo libero?
>
> Da questa domanda è nato Unclock, l'app Android che sto costruendo. Dici quando lavori e lei, fuori da quell'orario, mette in pausa le notifiche delle app di lavoro e conta il tempo che ti riprendi.
>
> Sto imparando a farla da solo, con l'aiuto di Claude Code. Qui racconto come va.

### 2. Cosa succede la sera

Immagine: `notif-it.webp` (la notifica "Lavoro in pausa fino alle 09:00").

> Mettiamo che alle 18:00 smetti di lavorare. La mail dell'ufficio e la chat del team continuano a scrivere, ma Unclock toglie le loro notifiche appena arrivano. Messaggi, chiamate e tutte le altre app restano come sempre.
>
> Quando la pausa finisce, Unclock ti dice quante notifiche ha messo in pausa e da quali app. Le apri da lì, quando vuoi tu.

### 3. Il weekend è una pausa sola

Immagine: `breaks-it.webp` (la scheda Pause).

> A Unclock dici solo quando lavori, per esempio dal lunedì al venerdì dalle 9 alle 18, e tutto il resto va in pausa da solo.
>
> Da venerdì sera a lunedì mattina è una pausa sola. Se vuoi aggiungi il pranzo o un'altra pausa. Se qualcosa nel modo di impostarlo non ti torna, dimmelo.

### 4. Le ore di luce

Immagine: `stats-it.webp` (le statistiche della settimana).

> Per ogni giorno, settimana e mese Unclock ti dice quanto tempo libero hai avuto e quanto di questo con la luce del giorno, perché calcola alba e tramonto.
>
> Le ore di sonno non contano, le imposti tu. Non ci sono serie di giorni da mantenere né obiettivi: se una sera lavori, quel giorno conta meno ore e il totale resta com'era.

### 5. Cosa vede e cosa no

Immagine: card di testo con i quattro punti.

> Per funzionare Unclock ha bisogno dell'accesso alle notifiche, che gli dai tu dalle impostazioni di Android. Con quell'accesso:
>
> 1. Vede solo quale app ha mandato la notifica.
> 2. Non legge il titolo né il testo, e non li salva.
> 3. Non ha il permesso di usare internet: niente esce dal telefono.
> 4. Togli l'accesso quando vuoi dalle impostazioni di Android.

### 6. Cerco tester

Immagine: `home-it.webp` (la schermata Oggi).

> Prima di metterla su Google Play vorrei che la provassero persone vere, con il lavoro vero che scrive la sera.
>
> Se hai un telefono Android e ti va, il link è nella bio: lasci l'email del tuo account Google, entri nel gruppo dei tester e la installi dal Play Store. L'email mi serve solo per il gruppo, non ti scrivo.
>
> Se qualcosa non va, scrivimelo qui o su Telegram.
>
> Sul canale Telegram (t.me/unclock_it) racconto i progressi, anche quelli andati storti.

Hashtag, uno o due gruppi al massimo: `#android #tempolibero #disconnessione #worklifebalance #indiedev`

## Prompt per Antigravity (card di testo)

```
Crea due immagini 1080x1350 (4:5) per Instagram, stile piatto e calmo.
Sfondo crema #F8F2E9, testo marrone scuro #3A2B22, un accento terracotta #9A4F34
(solo per un dettaglio piccolo, mai per il testo lungo). Titoli in Fraunces, testo in Inter
(gli stessi del sito), grandi e leggibili, allineati a sinistra con margini di 96 px. In basso a destra,
piccola, l'icona allegata (il lucchetto) a 72 px. Niente persone, niente foto stock,
niente sveglie, lune o stelle, niente emoji.
Immagine 1, testo: "Perché ci sono app che contano quanto lavori, e niente per il tempo libero?"
Immagine 2, titolo "Cosa vede Unclock" e quattro righe numerate:
"Vede solo quale app ti scrive" / "Non legge cosa c'è scritto" / "Non usa internet" /
"Lo spegni quando vuoi".
```
