# Pubblicare un'app su Google Play: la guida passo passo

Guida per la parte che non si può automatizzare: account, moduli, tester, invio in revisione. È scritta per Nook ma vale per qualsiasi app Android: dove c'è un valore specifico di Nook lo trovi in un riquadro **Per Nook**.

Ultima verifica delle regole di Google: settembre 2026. Le regole cambiano spesso: prima di ogni nuova app ricontrolla i link ufficiali in fondo.

---

## Mappa del percorso

| # | Passo | Tempo tuo | Attesa |
|---|---|---|---|
| 1 | Controllare nome e marchio | 30 min | – |
| 2 | Creare l'account Play Console e verificare l'identità | 1 h | da poche ore a qualche giorno |
| 3 | Creare l'app e caricare la prima build | 30 min | – |
| 4 | Compilare scheda dello store e moduli | 1 h | – |
| 5 | Test interno (solo tu) | 15 min | minuti |
| 6 | Test chiuso: 12 tester per 14 giorni | 1–2 h per trovarli | **almeno 14 giorni** |
| 7 | Chiedere l'accesso alla produzione | 20 min | fino a 7 giorni |
| 8 | Pubblicare in produzione | 15 min | revisione: da ore a qualche giorno |
| 9 | Dopo il lancio | continuo | – |

Contando le attese, dal giorno zero alla pubblicazione passano di solito **3–4 settimane**. Il collo di bottiglia è il test chiuso: inizia a cercare i tester subito, anche prima di aprire l'account.

---

## 1. Nome e marchio

Il nome dell'app si cambia facilmente fino alla pubblicazione, dopo molto meno. Il nome del pacchetto (`applicationId`) invece **non si cambia mai più** dopo il primo caricamento.

1. **Cerca il nome su Google Play** (dal telefono e da play.google.com). Se esiste già un'app con lo stesso nome nella stessa categoria, cambia nome.
2. **Cerca nei registri dei marchi**, con il nome esatto e con varianti:
   - [TMview](https://www.tmdn.org/tmview/) (marchi UE e nazionali europei, compreso l'Italia, in un colpo solo)
   - [EUIPO eSearch](https://euipo.europa.eu/eSearch/) (marchi dell'Unione Europea)
   - [UIBM](https://uibm.mise.gov.it/) (marchi italiani)
   - [USPTO](https://tmsearch.uspto.gov/) (Stati Uniti, se pubblichi anche lì)
3. Guarda le **classi 9** (software, app) e **42** (servizi software). Un marchio identico in queste classi è un rischio concreto; in classi diverse (per esempio abbigliamento) di solito no.
4. Se trovi un conflitto: aggiungi una parola che ti distingue ("Nook: stacca dal lavoro") o cambia nome. Un consulente in proprietà industriale può darti un parere definitivo; per un'app gratuita e personale la ricerca qui sopra è il minimo sensato.

> **Per Nook.** NOOK è un marchio di Barnes & Noble (lettori di e-book, app "NOOK" su Play nella categoria Libri). Nook è in un'altra categoria, ma verifica prima di tutto il resto. Riserve già valutate: *Tempo Mio*, *Dopo*, *Riva* (vedi `DESIGN_SYSTEM.md`, "Nome"). Se cambi nome, dimmelo **prima** del primo caricamento: devo cambiare anche l'`applicationId`.

---

## 2. Account Google Play Console

### Personale o organizzazione?

| | Personale | Organizzazione |
|---|---|---|
| Per chi | Una persona | Azienda, ditta, associazione |
| Serve | Documento d'identità, telefono, indirizzo | In più il numero **D-U-N-S** dell'azienda (gratuito, richiede giorni o settimane) |
| Test chiuso obbligatorio | **Sì**: 12 tester per 14 giorni (account creati dopo il 13/11/2023) | No |
| Nome mostrato sullo store | Il tuo nome da sviluppatore | Il nome dell'azienda |

Per un'app personale scegli **Personale**. Il nome da sviluppatore mostrato sullo store può essere diverso dal tuo nome legale (per esempio "Pasquale Ercolino" o un nome tipo "Ercolino Apps").

### Prima di iniziare

- **Un account Google dedicato**, non quello personale di tutti i giorni (per esempio `ercolino.apps@gmail.com`). Se un giorno ti fai aiutare o cedi un'app, non condividi la tua posta.
- **Verifica in due passaggi** attiva su quell'account (Google la richiede).
- Documento d'identità valido, carta di credito o debito, un numero di telefono.
- Un'**email di contatto pubblica** per gli utenti: comparirà sulla scheda dello store. Può essere la stessa dell'account dedicato.

### Registrazione

1. Vai su [play.google.com/console/signup](https://play.google.com/console/signup) con l'account dedicato.
2. Scegli **Personale** e compila: nome da sviluppatore, email e telefono di contatto, esperienza con Android (rispondi onestamente).
3. Paga la **quota di registrazione di 25 $**, una tantum, valida per tutte le app future.
4. **Verifica dell'identità**: carica il documento. Google controlla che nome e indirizzo coincidano con quelli del pagamento. Di solito serve da qualche ora a qualche giorno; se chiedono altri documenti, rispondi entro i tempi indicati nella mail.
5. Verifica anche email e telefono di contatto dal pannello di Play Console.

Da settembre 2026 Google chiede la verifica dello sviluppatore anche per le app installate fuori da Play: con un account Play Console verificato sei già a posto.

---

## 3. Creare l'app e caricare la prima build

### Chiave di firma: leggi prima di caricare

Ogni build va firmata. Con Play esistono due chiavi:

- **Chiave di caricamento (upload key)**: è tua, sta nel keystore sul tuo Mac. Firmi con questa l'AAB che carichi.
- **Chiave dell'app**: la tiene Google (**Play App Signing**, obbligatorio per le app nuove). È quella che vedono gli utenti.

Se perdi la chiave di caricamento, Google può reimpostarla (richiesta dal pannello "Integrità dell'app"). Se invece gestissi tu la chiave dell'app e la perdessi, non potresti più aggiornare l'app. Quindi: **accetta Play App Signing** e fai comunque il backup del keystore.

> **Per Nook.** Keystore e password sono in `~/keystores/notificationblocker-upload.jks` e `~/keystores/notificationblocker-signing.properties`. **Copia entrambi** in un posto sicuro fuori dal Mac (password manager o cloud cifrato) prima di caricare la prima build. Mai nella repo.

### Creare l'app

1. In Play Console: **Crea app**.
2. Nome dell'app (quello della scheda, max 30 caratteri), lingua predefinita, **App** (non gioco), **Senza costi**.
3. Spunta le dichiarazioni (norme per gli sviluppatori, leggi sull'esportazione degli Stati Uniti).

Attenzione: "Senza costi" non si può più cambiare in "a pagamento" per quell'app.

### La build (AAB)

Il file da caricare è un **Android App Bundle** (`.aab`), non un APK.

> **Per Nook.** Nel terminale, dalla cartella del progetto:
> ```bash
> export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
> ./gradlew bundleRelease
> ```
> Il file è `app/build/outputs/bundle/release/app-release.aab`. Oppure chiedimelo e lo preparo io.

Regole che valgono per sempre:

- Ogni caricamento deve avere un **`versionCode` più alto** del precedente (1, 2, 3…). Il `versionName` ("1.0", "1.1") è quello che vedono gli utenti.
- Il **`targetSdk`** deve rispettare il minimo di Google, che sale ogni anno ad agosto (dal 31/08/2026: almeno API 36).

---

## 4. Scheda dello store e moduli

In Play Console, la **Dashboard** dell'app mostra un elenco di attività ("Configura l'app"): segui quello, in ordine.

> **Per Nook.** Tutti i testi, le immagini e le risposte sono pronti in [`store/LISTING.md`](store/LISTING.md): copia e incolla. Immagini in `docs/store/`.

### Scheda principale dello store

- **Nome** (30 caratteri), **descrizione breve** (80), **descrizione completa** (4000).
- **Icona** 512×512 PNG, **feature graphic** 1024×500, **screenshot** del telefono (da 2 a 8, il lato lungo al massimo il doppio di quello corto).
- **Traduzioni**: in "Gestisci traduzioni" aggiungi le altre lingue e incolla i testi tradotti; puoi caricare immagini diverse per lingua.

Regole di Google sui testi (se le violi, l'app viene rifiutata):

- Niente "il migliore", "n. 1", "gratis" nel nome, niente emoji o maiuscole a effetto nel nome.
- Niente nomi di altre app o marchi usati come parole chiave.
- Niente promesse che l'app non mantiene, e niente affermazioni sulla salute non dimostrate.

### "Contenuti dell'app" (i moduli)

Nel menu **Norme e programmi → Contenuti dell'app**:

| Modulo | Cosa chiede | Consiglio |
|---|---|---|
| Norme sulla privacy | URL di una pagina pubblica | Obbligatorio per tutte le app |
| Accesso all'app | Serve un login? | Se c'è un login, dai ai revisori un account di prova |
| Annunci | L'app mostra pubblicità? | Rispondi con precisione: anche un SDK pubblicitario inattivo conta |
| Classificazione dei contenuti | Questionario IARC | Rispondi per quello che l'app contiene davvero |
| Pubblico di destinazione | Fasce d'età | Se includi under 13 si applicano le regole "Famiglie", molto più severe |
| Sicurezza dei dati | Quali dati raccogli, condividi, perché | Deve coincidere con il codice e con l'informativa. È il modulo più controllato |
| App governative, finanziarie, salute, notizie | Dichiarazioni | Rispondi No se non ti riguarda |

**Sicurezza dei dati, la regola d'oro:** i dati che l'app usa solo sul telefono, senza inviarli a nessuno, **non** sono "raccolti". Ma ogni libreria che invia dati (analisi, crash report, pubblicità, login) va dichiarata, anche se l'hai aggiunta tu senza pensarci.

---

## 5. Test interno (solo tu)

Serve a controllare che la build caricata funzioni, prima di coinvolgere altri.

1. **Test → Test interno → Crea nuova release**, carica l'AAB, scrivi le note di rilascio, **Salva** e **Avvia l'implementazione**.
2. Nella scheda **Tester** crea un elenco con la tua email (e al massimo 100 persone).
3. Copia il **link di partecipazione**, aprilo dal telefono con lo stesso account Google, accetta e installa da Play.
4. Controlla: l'app si apre, il permesso funziona, la funzione principale fa quello che deve.

Il test interno non conta per i 14 giorni: serve solo a te.

---

## 6. Test chiuso: 12 tester per 14 giorni

Obbligatorio per gli account personali. Senza, il pulsante per la produzione resta bloccato.

### Le regole (lette bene)

- Almeno **12 tester** devono essersi **iscritti** al test (opt-in dal link) e restare iscritti **14 giorni di fila**.
- Il conteggio parte quando i tester si iscrivono e installano, non quando crei il test.
- Se qualcuno si disiscrive prima del giorno 14, i conti possono ripartire. **Recluta almeno 15–20 persone** per avere margine.
- Google guarda anche se l'app viene usata e se ricevi feedback: chiedi ai tester di aprirla qualche volta e di scriverti cosa ne pensano.
- I tester devono avere un account Google e un telefono Android (la versione minima dell'app: per Nook Android 9).

### Trovare i tester

In ordine di affidabilità:

1. **Amici, familiari, colleghi** con Android. Le persone che conosci restano iscritte per 14 giorni.
2. **Colleghi di corso o community** di cui fai parte (gruppi WhatsApp/Telegram, Discord, un gruppo LinkedIn).
3. **Scambio di test tra sviluppatori**: community come r/AndroidClosedTesting su Reddit, dove ci si testa a vicenda le app. Funziona ma richiede di ricambiare.
4. **Servizi a pagamento** che forniscono tester: esistono, ma usali con cautela. Google chiede tester veri che usano l'app; account finti o inattivi possono far rifiutare la richiesta di produzione.

### Organizzare l'elenco con un Google Gruppo (consigliato)

Invece di inserire le email una per una:

1. Crea un gruppo su [groups.google.com](https://groups.google.com), per esempio `nook-tester@googlegroups.com`, impostato in modo che chiunque abbia il link possa iscriversi.
2. In Play Console, nel test chiuso, scegli **Google Gruppi** e inserisci l'indirizzo del gruppo.
3. Ai tester mandi due link: quello per **iscriversi al gruppo** e quello per **partecipare al test**.

Così chi si aggiunge dopo entra da solo, e hai l'elenco sempre sotto controllo.

### Creare il test chiuso

1. **Test → Test chiuso → Crea traccia** (o usa "Alpha"), **Crea nuova release**, carica l'AAB (può essere lo stesso del test interno).
2. **Paesi**: seleziona i paesi dei tuoi tester (almeno l'Italia).
3. **Tester**: il Google Gruppo o l'elenco email; inserisci un **URL o un'email per il feedback**.
4. Invia in revisione: la prima revisione può richiedere da qualche ora a qualche giorno.
5. Quando è approvata, copia il **link di partecipazione** e mandalo ai tester.

### Messaggi pronti per i tester

Adattali alla tua app. Scritti per essere brevi e chiari: le persone li leggono dal telefono.

**Invito (email o WhatsApp), italiano**

```
Ciao! Sto per pubblicare la mia prima app Android, Nook: mette in pausa le notifiche delle app di lavoro quando stacchi.

Prima di pubblicarla Google chiede che almeno 12 persone la provino per 14 giorni. Mi daresti una mano? Ci vogliono due minuti:

1. Iscriviti al gruppo dei tester: [LINK GRUPPO]
2. Apri questo link dal telefono Android e tocca "Diventa un tester": [LINK TEST]
3. Installa Nook da Google Play (dallo stesso link) e aprila almeno una volta.

L'unica cosa importante: tienila installata per 14 giorni, fino al [DATA]. Se ti va, usala davvero e dimmi cosa ne pensi, anche due righe.

L'app non raccoglie dati e non usa internet. Grazie!
```

**Invito, inglese**

```
Hi! I'm about to publish my first Android app, Nook: it pauses notifications from your work apps when you switch off.

Before it goes public, Google asks for at least 12 people to test it for 14 days. Could you help? It takes two minutes:

1. Join the testers group: [GROUP LINK]
2. Open this link on your Android phone and tap "Become a tester": [TEST LINK]
3. Install Nook from Google Play (same link) and open it at least once.

The one thing that matters: keep it installed for 14 days, until [DATE]. If you feel like it, use it for real and tell me what you think, even a couple of lines.

The app collects no data and doesn't use the internet. Thanks!
```

**Promemoria a metà (giorno 7)**

```
Ciao! Siamo a metà del test di Nook, grazie per esserci. Tienila installata ancora una settimana, fino al [DATA]. Hai notato qualcosa che non va o che cambieresti? Anche una frase mi aiuta molto.
```

**Ultimo promemoria (giorno 12–13)**

```
Ciao! Mancano due giorni alla fine del test di Nook. Per favore non disinstallarla prima del [DATA]: se qualcuno esce prima, il conteggio di Google riparte da zero. Grazie davvero!
```

**Ringraziamento (a test finito)**

```
Il test di Nook è finito, grazie! Ora chiedo a Google di pubblicarla. Puoi tenerla o disinstallarla, come preferisci. Quando sarà sullo store ti mando il link: se ti è piaciuta, una recensione sincera mi aiuta tantissimo.
```

### Raccogliere il feedback

- Un **Google Modulo** con 3–4 domande: cosa ti è piaciuto, cosa non ti è chiaro, hai avuto problemi, che telefono usi. Metti il link nell'invito e nel campo "feedback" del test chiuso.
- Annota ogni feedback con la data: servirà per il questionario del passo 7 ("come hai usato i risultati del test").
- Se un tester trova un problema, correggilo, alza il `versionCode`, carica una nuova release nel test chiuso: il conteggio dei 14 giorni **non** riparte per un aggiornamento.

### Calendario tipo

| Giorno | Cosa fai |
|---|---|
| −7 | Cerca i tester, crea il Google Gruppo e il modulo di feedback |
| 0 | Test chiuso approvato: manda l'invito con i link |
| 1–2 | Controlla quanti si sono iscritti (Play Console mostra il numero), sollecita chi manca |
| 7 | Promemoria a metà, raccogli i primi feedback |
| 12–13 | Ultimo promemoria |
| 14+ | Chiedi l'accesso alla produzione |

---

## 7. Chiedere l'accesso alla produzione

Dopo i 14 giorni, nella **Dashboard** compare **Richiedi l'accesso alla produzione**. Google fa alcune domande sul test. Rispondi in modo concreto e onesto, con esempi veri. Tracce di risposta:

**Com'è andato il reclutamento dei tester?**
```
I invited friends, family and colleagues who use Android (NUMBER people), through a Google Group. NUMBER of them joined and kept the app installed for the whole 14 days.
```

**Quanto è stato facile far usare l'app ai tester? / Come l'hanno usata?**
```
Testers set their own off-work hours and chose their work apps (mostly email, chat and calendar). They used it during evenings and weekends and checked the "During your break" summary afterwards.
```

**Che feedback hai ricevuto e cosa hai cambiato?**
```
[Esempi veri dal modulo: "Two testers found the permission step unclear, so I rewrote the explanation card." "One tester asked for weekend support; it is planned for a next version."]
```

**A chi si rivolge l'app? Cosa la rende utile?**
```
People who get work notifications on their personal phone and want to switch off after work without silencing the whole phone. Nook pauses only the apps they choose, during the hours they choose, and everything stays on the device.
```

**L'app è pronta per la produzione? Cosa hai fatto per renderla pronta?**
```
Yes. Besides the closed test, I tested it on an Android 17 emulator and on a Samsung Galaxy A32 (Android 13): blocking rules, restarts, large text, dark theme, both languages. The release build is optimized with R8 and signed with Play App Signing.
```

Risposta di solito entro **7 giorni**. Se viene rifiutata, la mail spiega perché: spesso servono più tester attivi o più giorni; correggi e richiedi di nuovo.

---

## 8. Pubblicare in produzione

1. **Produzione → Crea nuova release**: puoi **promuovere** la release del test chiuso invece di ricaricarla.
2. **Paesi e aree geografiche**: tutti o solo alcuni. Per iniziare puoi scegliere Italia e pochi altri, poi allargare.
3. **Implementazione graduale (staged rollout)**: parti dal 20%, controlla crash e recensioni per un paio di giorni, poi sali al 50% e al 100%. Se esce un problema grave puoi **interrompere** l'implementazione.
4. **Invia in revisione.** Per un account nuovo la revisione può durare da qualche ora a qualche giorno. Riceverai una mail in ogni caso.

Nello stesso momento:

- Crea un **tag git** della versione pubblicata (per Nook lo faccio io: `v1.0.0`).
- Salva il file `.aab` caricato insieme al tag, se vuoi poterlo ritrovare.

---

## 9. Dopo il lancio

### Ogni settimana, i primi due mesi

- **Qualità → Android vitals**: crash e ANR (app bloccata). Google penalizza nella visibilità le app sopra l'1,09% di crash o lo 0,47% di ANR.
- **Recensioni**: rispondi a tutte, soprattutto alle negative, con calma e in modo concreto. Esempio:
  ```
  Grazie per averla provata! Mi dispiace per il problema. Nook toglie solo le notifiche delle app che scegli in "App di lavoro": se una notifica arriva comunque, controlla che l'accesso alle notifiche sia ancora attivo nelle impostazioni. Se il problema resta scrivimi a [EMAIL], così lo sistemo.
  ```
- Le **email di Google** (policy, avvisi): leggile sempre. Spesso danno una scadenza.

### Ogni aggiornamento

1. Alza `versionCode` (+1) e, se l'utente deve accorgersene, `versionName` (1.0 → 1.1).
2. Scrivi le note di rilascio in tutte le lingue della scheda.
3. Ricontrolla la **Sicurezza dei dati** se hai aggiunto librerie o permessi.
4. Aggiorna gli screenshot se l'interfaccia è cambiata molto.
5. Carica prima nel test interno, provalo, poi in produzione con implementazione graduale.

### Ogni anno (agosto)

Google alza il `targetSdk` minimo. Le app che restano indietro spariscono per gli utenti con telefoni nuovi. Segnati di aggiornare il progetto prima di fine agosto.

### Far conoscere l'app

- Link diretto alla scheda: `https://play.google.com/store/apps/details?id=` seguito dal nome del pacchetto. Risponde "non trovato" finché l'app non è pubblicata in produzione: usalo solo dopo.
- Un messaggio breve e personale funziona meglio di un annuncio generico: a chi ti conosce, spiega in una frase a cosa serve e perché l'hai fatta.
- Chiedi recensioni **senza incentivi**: offrire premi, sconti o scambi in cambio di recensioni è vietato dalle regole di Google e può far rimuovere l'app.
- Se hai un profilo LinkedIn o un blog, raccontare il perché dell'app (per Nook: il diritto a staccare) interessa più delle funzioni.

Messaggio di lancio pronto (adattalo):

```
Ho pubblicato Nook, la mia prima app Android. Mette in pausa le notifiche delle app di lavoro quando stacchi, e lascia arrivare tutto il resto. Niente account, niente pubblicità, nessun dato che esce dal telefono.

La trovi qui: https://play.google.com/store/apps/details?id=com.pasquale.nook   (da mandare solo a pubblicazione avvenuta)

Se la provi, mi fa piacere sapere cosa ne pensi.
```

---

## Checklist riutilizzabile (per ogni nuova app)

Copiala in un file nuovo e spunta man mano.

**Prima di scrivere codice**
- [ ] Nome controllato su Play e nei registri dei marchi (classi 9 e 42)
- [ ] `applicationId` definitivo scelto
- [ ] Deciso: gratis o a pagamento, con o senza pubblicità, con o senza login

**Prima del primo caricamento**
- [ ] `targetSdk` sopra il minimo di Google
- [ ] Build release firmata, keystore e password salvati fuori dal computer
- [ ] Informativa privacy online, coerente con quello che fa l'app
- [ ] Se l'app usa permessi delicati: spiegazione nell'app prima di chiederli
- [ ] Provata sulla build **release** (non debug) su almeno un telefono vero

**Scheda dello store**
- [ ] Nome (30), descrizione breve (80), descrizione completa (4000), in tutte le lingue
- [ ] Icona 512×512, feature graphic 1024×500, da 2 a 8 screenshot (rapporto massimo 2:1)
- [ ] Categoria, email di contatto, URL dell'informativa

**Contenuti dell'app**
- [ ] Norme sulla privacy, accesso all'app, annunci
- [ ] Classificazione dei contenuti, pubblico di destinazione
- [ ] Sicurezza dei dati coerente con codice e informativa
- [ ] Dichiarazioni speciali (salute, finanza, notizie, permessi delicati) se servono

**Test**
- [ ] Test interno installato e provato
- [ ] 15–20 tester reclutati, Google Gruppo e modulo di feedback pronti
- [ ] Test chiuso approvato, invito inviato
- [ ] Promemoria al giorno 7 e al giorno 12
- [ ] Feedback annotati con le modifiche fatte

**Produzione**
- [ ] Accesso alla produzione richiesto e ottenuto
- [ ] Release promossa, paesi scelti, implementazione graduale
- [ ] Tag git della versione pubblicata
- [ ] Android vitals e recensioni controllati ogni settimana

---

## Chi fa cosa, per Nook

| Cosa | Chi | Stato |
|---|---|---|
| Codice, build firmata, test tecnici | Claude | Fatto |
| Testi, icona, feature graphic, screenshot, risposte ai moduli | Claude | Fatto (`store/LISTING.md`) |
| Informativa privacy online | Pasquale (Pages) + Claude (testo) | Fatto |
| Backup di keystore e password | Pasquale | **Da fare** |
| Verifica del marchio "Nook" | Pasquale | **Da fare** |
| Account Play Console, verifica identità | Pasquale | **Da fare**: dimmelo quando è attivo |
| Tester, Google Gruppo, inviti | Pasquale | **Da fare**, parti subito |
| Caricare AAB e compilare Play Console | Pasquale, con i testi pronti | Dopo l'account |
| Aggiornamenti dopo i feedback | Claude | Quando arrivano |
| Tag `v1.0.0` | Claude | Alla pubblicazione |

---

## Riferimenti ufficiali

- [Registrazione a Play Console](https://support.google.com/googleplay/android-developer/answer/6112435)
- [Requisiti di test per gli account personali nuovi](https://support.google.com/googleplay/android-developer/answer/14151465)
- [Configurare test aperti, chiusi e interni](https://support.google.com/googleplay/android-developer/answer/9845334)
- [Requisiti del `targetSdk`](https://developer.android.com/google/play/requirements/target-sdk)
- [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756)
- [Sezione Sicurezza dei dati](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Risorse grafiche della scheda](https://support.google.com/googleplay/android-developer/answer/9866151)
- [Norme sui metadati (nome, descrizioni)](https://support.google.com/googleplay/android-developer/answer/9898842)
- [Android vitals](https://developer.android.com/topic/performance/vitals)
- [Verifica degli sviluppatori Android (2026)](https://developer.android.com/developer-verification)
