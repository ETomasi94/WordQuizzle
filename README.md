# WordQuizzle
A multiplayer italian to english translation challenges
0 – OVERVIEW
Il progetto consiste nell’implementazione di un sistema di sfide di traduzione italiano-inglese tra utenti registrati ad un servizio.
Il sistema è strutturato secondo il paradigma Client-Server ed è implementato in linguaggio Java.

1 – ARCHITETTURA E SCELTE PROGETTUALI
Il sistema è suddiviso in due package diversi, consistenti ognuno in un’applicazione diversa:
	WordQuizzleClient: Applicazione a lato client, consiste nell’implementazione di un programma provvisto di GUI tramite cui un utente invia richieste ad un servizio WordQuizzle. Il client gestisce le risposte in arrivo e risponde a seconda della risposta ricevuta. 

	WQServer: Applicazione a lato server, consiste in un programma in riga di comando che gestisce tutte le richieste in arrivo dai vari client, le delega ad un servizio WordQuizzle sovrastante e restituisce ai client la risposta in base all’esito di esse.

1.1 - VISIONE GENERALE DEL FUNZIONAMENTO DEL SISTEMA
Presi in input due interi rappresentanti il numero di porta su cui collegarsi in TCP e quello su cui mettersi in ascolto per offrire ai client un servizio in RMI, il server WQSERVER si mette in ascolto di connessioni ed eventuali richieste da parte di client sulla porta, leggendo i messaggi in arrivo dai client e rispondendo a seconda del caso delegando, nella maggior parte dei casi, il soddisfacimento delle richieste al un servizio WORDQUIZZLESERVER centrale

	WORDQUIZZLESERVER:
È il nucleo centrale del sistema, implementa il servizio di gestione degli utenti e delle sfide, verifica la validità delle credenziali immesse dagli utenti, gestisce lo stato e le informazioni di questi e le serializza su un file JSON univoco, occupandosi inoltre dell’istanziazione dei thread relativi alle sfide e fornendo una risposta in base all’esito di un’operazione al server.
Dall’altra parte uno o più utenti che utilizzano un programma client immettono le proprie credenziali, consistenti di NickName e Password ai fini di registrarsi via RMI al servizio o effettuare il login ed i client stabiliscono una connessione TCP con il server principale sulla porta data come parametro ed effettuano una o più operazioni tra quelle offerte, restando nel frattempo in ascolto mediante un apposito thread di richieste di sfida in arrivo su una connessione UDP collegata allo stesso indirizzo IP della connessione TCP stabilita in precedenza
Un utente può sfidare un altro utente se questi è connesso, in tal caso il server si occuperà di notificare l’utente sfidato, o di avvisare lo sfidante con un messaggio di errore nel caso l’operazione non possa essere completata.

All’arrivo di una richiesta di sfida, l’utente ha 5 secondi per decidere se accettare la richiesta o meno, dopodiché il client sfidante verrà notificato del mancato arrivo di una risposta in tempo e l’utente non potrà più accettare quella sfida.

Nel caso la sfida venga accettata, il server notifica i due client coinvolti in essa e delega al servizio l’inizializzazione della sfida.
Il servizio dunque istanzia un thread per la sfida apposito e collega i due client, seleziona 10 parole da un dizionario presente in memoria, inizializza un timer di 2 minuti ed invia il primo vocabolo ad ognuno dei due sfidanti.

Le traduzioni vengono reperite all’indirizzo https://api.mymemory.translated.net/get?q="+word+"&langpair=it|en ed analizzate estraendo gli opportuni campi della stringa in formato JSON ottenuta.

Ogni client propone una traduzione per la parola inviatagli, e la sottomette al thread sfida, che verifica mediante il servizio centrale se la traduzione è corretta o meno ed aggiorna il punteggio relativo alla sfida in corso (chiamato punteggio partita), se uno dei due sfidanti traduce tutte le parole, deve attendere che scatti il timeout.

Durante lo svolgimento della partita, un client può effettuare comunque tutte le operazioni escluse login e sfida (per ovvie ragioni).

Quando una partita termina, che sia a causa dell’abbandono da parte di uno sfidante oppure perché il tempo è scaduto, la sfida viene dichiarata conclusa ed il thread sfida (che è anch’esso un server che gestisce le richieste dei due client durante quest’ultima) notifica i due client dell’esito della partita assegnando ad ognuno dei due sfidanti il rispettivo punteggio ed aggiornando, mediante il servizio WordQuizzle, il punteggio utente (ovvero la somma dei punteggi ottenuti durante tutte le sfide a cui l’utente ha partecipato)

L’esito di una partita viene determinato come segue:
1.	ABBANDONO DI UNA SFIDA IN CORSO DA PARTE DI UNO SFIDANTE

a.	Lo sfidante che ha abbandonato la partita (quitter) viene collegato nuovamente al server principale e non riceve nessun punto

b.	Lo sfidante rimasto per ultimo in partita viene notificato dell’abbandono di questa e riceve in aggiunta ai punti da lui guadagnati fino a quel momento, tre punti extra

2.	SCADENZA DEL TEMPO DISPONIBILE (TIMEOUT)

a.	Il vincitore guadagna, oltre i punti che gli spettano di diritto per le parole tradotte correttamente, tre punti extra
b.	Lo sconfitto guadagna solamente i punti che gli spettano

N.B: In caso di pareggio, entrambi gli sfidanti guadagnano solo i punti relativi alle traduzioni corrette.

Dopo la fine di una sfida, i client ritornano a comunicare con il server principale ed è possibile di nuovo sfidare gli utenti connessi.

1.2 - PRINCIPALI SCELTE PROGETTUALI

	Si è scelto di implementare la comunicazione Client-Server mediante un protocollo che utilizza il seguente formato di messaggi inviati sotto forma di stringhe:

o	<OP.NEE--Parametro1--(Parametro2)--...--(ParametroN)>

CON:
o	OP.NE = Operazione richiesta
o	PARAMETRO1…PARAMETRON = Paramteri dell’operazione, che possono essere uno,due o quattro.
Il server, tramite la funzione split() della classe String di Java, scompone le richieste e ne deriva i parametri
Questa scelta ha reso obbligatorio rendere la stringa “--“riservata al programma, e quindi inutilizzabile per le varie operazioni che richiedo una stringa in input.
•	Si è scelto di disaccoppiare il server dal servizio vero e proprio implementando ognuno di essi in una classe a sé stante, in modo rendere più modulare la gestione delle richieste inviate dal server principale e dai vari thread sfida istanziati.

•	Riguardo al client, si è preferito l’utilizzo di un’interfaccia grafica piuttosto che l’utilizzo della linea di comando, il che ha comportato l’inclusione della libreria esterna “Absolute Layout” presente nella cartella del progetto.

•	Per non bloccare il flusso di esecuzione del client quando l’utente invia o deve rispondere ad una richiesta di sfida, ogni client istanzia, nel momento opportuno, due thread UPDListener e TCPWaiter, per gestire le varie richieste di sfida.

•	La notifica di una richiesta di sfida in arrivo e la risposta a quest’ultima avvengono mediante comunicazione UDP, la perdita è stata gestita mediante l’utilizzo di un timeout per la connessione e l’eventuale notifica allo sfidante, mentre la risposta alla sfida viene inviata dallo sfidato in UDP ed inoltrata in TCP ad entrambi gli utenti, in modo da poter iniziare la sfida in contemporanea.

•	È stata implementata la classe Player ai fini di agevolare la serializzazione in JSON delle credenziali di un utente al momento della prima registrazione ed identificare meglio i giocatori in linea.

•	Per non utilizzare iteratori in maniera massiccia, la lista amici e la classifica vengono restituite al server come un’unica stringa con dei line feeds dopo ogni elemento e visualizzati nell’apposito pannello dell’interfaccia utente del client

•	Nel caso del server principale e dei server relativi alle sfide, per migliorare la scalabilità, utilizzare un numero minore di thread ed anche per familiarità con l’implementazione utilizzata, è stato scelto di utilizzare un selettore


1.3 - RAPPRESENTAZIONE DELLO SCHEMA GENERALE DEL SISTEMA
 



2 – THREADS E STRUTTURE DATI

Analizziamo i thread attivati e le strutture dati utilizzate su ogni lato del programma

2.1-LATO CLIENT 
  

1.	THREAD CLIENT: Thread eseguito da ogni client con interfaccia grafica
1.1.	RightAnswers = Numero di parole tradotte correttamente
1.2.	TimeChallenge = Tempo a disposizione per la sfida
1.3.	TranslatedWords = Numero di parole tradotte in totale
1.4.	Password = Password dell’utente
1.5.	Name = NickName dell’utente
1.6.	Opponent = Nome dell’avversario (in caso di sfida)
1.7.	Mode = Modalità del client (LOGIN in caso di comunicazione con il server principale o CHALLENGE in caso di sfida) in base a cui cambia anche l’interfaccia grafica
1.8.	Word = Parola da tradurre in arrivo dal thread sfida
1.9.	ConnectionBuffer = Buffer su cui avviene la lettura e la scrittura in TCP
1.10.	RequestManager = Registry dell’oggetto remoto per la registrazione in RMI
1.11.	RMObject = Oggetto remoto per la registrazione in RMI
1.12.	Clientaddress = Indirizzo IP del client
1.13.	Client = Connessione TCP dal client verso il server 
1.14.	ClientUDP = Connessione UDP dal client verso il server
1.15.	UDPChallengeRequest = Pacchetto UDP passato al client dal suo UDPListener
1.16.	UDPMessage = Array di byte che rappresenta la risposta ad una richiesta di sfida da inviare in UDP al server

2.	THREAD UDPLISTENER: Thread atto alla gestione delle richieste di sfida via UDP
2.1.	Termination = Booleano che rappresenta un flag per far terminare il thread (il thread termina quando è uguale a true)
2.2.	NoMorePackets = Booleano che rappresenta un flag per impedire e consentire l’arrivo di ulteriori pacchetti UDP (il listener non riceve ulteriori pacchetti se è uguale a true)
2.3.	Message = Array di byte che rappresenta il contenuto del pacchetto UDP da inviare
2.4.	Packet = Pacchetto UDP che verrà passato, una volta ricevuto, al client

3.	THREAD TCPWAITER: Thread finalizzato all’attesa di risposta in TCP per ogni richiesta di sfida inviata 
3.1.	ChallengerName = Nome dello sfidante
3.2.	OpponentName = Nome dello sfidato




2.2 – LATO SERVER
 
1.	THREAD SERVER: Thread principale che gestisce le varie connessioni e richiesta da parte dei client.
1.1.	SelectionMap = Tabella hash concorrente che indicizza ogni chiave di selezione (SelectionKey) di un client con il suo nome utente al fine di verificare se quell’utente è connesso al server o meno, è stata scelta una ConcurrentHashMap per evitare che può client vi accedano in contemporanea durante la connessione.
1.2.	ConnectionServerSocket = ServerSocket su cui il server è in attesa di connessioni
1.3.	ConnectionChannel = SocketChannel su cui il server è in attesa di connessioni per il selettore
1.4.	Address = Indirizzo a cui è connesso il server
1.5.	Buffer = Buffer su cui avviene la lettura e la scrittura di dati durante la connessione TCP da e verso ogni client
1.6.	Selector: Selettore che scorre tra i canali disponibili relativi agli utenti online e soddisfa di volta in volta le richieste in arrivo, è implementato in modalità non bloccante

2.	RMI WORDQUIZZLESERVICE: Thread che rappresenta il servizio di registrazione in remoto

3.	SERVIZIO WORDQUIZZLE: Servizio di gestione degli utenti e delle sfide, si occupa di registrare ed aggiornare i dati utente e di inizializzare le sfide, fornendo ad esse anche un dizionario (file Dizionario.txt nella cartella dove si trova il progetto) da cui scegliere i vocaboli da tradurre.

3.1.	AvailablePlayers = Tabella hash concorrente che indicizza mediante i NickName degli utenti la struttura dati Player associata, è stata scelta una ConcurrentHashMap per evitare che più client vi accedano in contemporanea durante il login o il logout.
3.2.	PendingChallenges = Tabella hash concorrente che indicizza ogni risposta (sottoforma di stringa) mediante la chiave di selezione (SelectionKey) dello sfidante.
3.3.	Dictionary = Vector di stringhe rappresentante il dizionario, viene caricato in memoria dal servizio WordQuizzle, è stato scelto ai fini di utilizzare una struttura thread-safe.
3.4.	Challenges = ThreadPool che esegue tutti i thread sfida avviati
4.	PLAYER: Struttura dati che rappresenta un giocatore e i suoi dati, è utilizzata per la fase di registrazione e per fare in modo che il servizio WordQuizzle verifichi che un utente sia disponibile a giocare.
4.1.	NickName = Nome alfanumerico che identifica univocamente l’utente
4.2.	Password = Stringa che rappresenta la password per accedere al servizio dell’utente
4.3.	Friends = Lista di stringhe che rappresenta la lista delle amicizie dell’utente
4.4.	Score = Intero lungo che rappresenta il punteggio ottenuto dall’utente in tutte le sue sfide.

5.	THREAD SFIDA: Thread che istanzia una sfida in corso tra due utenti, viene eseguito dal threadpool “Challenges”.

5.1.	Player1 = NickName del primo utente in sfida.
5.2.	Player2 = NickName del secondo utente in sfida.
5.3.	ChallengeName = Nome della sfida, identificato dalla stringa “<NickNameUtente1>--<NickNameUtente2>”. 
5.4.	Buffer = Array di byte su cui avviene la lettura e la scrittura di dati
5.5.	Score1 = Intero lungo che rappresenta il punteggio totalizzato dall’utente numero uno fino ad un determinato momento
5.6.	Score2 = Intero lungo che rappresenta il punteggio totalizzato dall’utente numero due fino ad un determinato momento
5.7.	Termination = Booleano che funge da flag per terminare il thread (il thread termina quando il flag è uguale a true)
5.8.	WordsDisposed = Vector di Entry<String,String> le cui entry indicizzano ogni traduzione con la parola in lingua originale, serve a controllare la correttezza delle traduzioni proposte dai client, è stato scelto l’utilizzo di un Vector per utilizzare una struttura thread-safe.

6.	ALTRE STUTTURE DATI DEGNE DI NOTA: 
6.1.	Per favorire la sincronizzazione tra thread durante il recupero della lista amici, è stata utilizzata una SynchronizedList
6.2.	Nel metodo per selezionare le parole da tradurre, queste sono state immesse in una HashMap concorrente, convertita in EntrySet per facilitare l’inserzione delle entry parola->traduzione nel Vector WordsDisposed.

3 – CLASSI E CONTENUTO DELLA DIRECTORY
Descriviamo le classi implementate ed il contenuto della directory del progetto (chiamata WordQuizzle)

 3.1 - CLASSI IMPLEMENTATE
1.	LATO CLIENT (WordQuizzle/Source/WordQuizzleClient)
1.1.	MainClassClient.java: Classe principale del programma client, ne implementa le funzionalità e l’interfaccia grafica.
1.2.	UDPListener.java: Classe che implementa il thread listener UDP per la gestione delle richieste di sfida.
1.3.	TCPWaiter.java: Classe che implementa il thread TCPWaiter finalizzato all’attesa della risposta alla richiesta di sfida inviata.
1.4.	RemoteRequests.java: Interfaccia che implementa il comportamento dell’oggetto remoto mediante il quale il client si registra al servizio.
2.	LATO SERVER (WordQuizzle/Source/WQServer)
2.1.	MainClassServer.java: Classe principale del programma server, ne implementa le funzionalità ed il ciclo di vita.
2.2.	WordQuizzleServer.java: Classe che implementa il servizio WordQuizzle e le sue operazioni-
2.3.	Challenge.java: Classe che implementa un generico thread di sfida.
2.4.	Player.java: Classe che implementa una struttura dati che ingloba le credenziali e i dati di gioco di un utente.
2.5.	ScoreComparator.java: Classe che implementa un criterio di confronto utile ad ordinare la classifica degli utenti in ordine decrescente di punteggio
2.6.	Requests.java: Interfaccia che descrive i metodi relativi alle principali operazioni offerte dal servizio WordQuizzle.
2.7.	RemoteRequests.java: Interfaccia che implementa il comportamento dell’oggetto remoto che il server offre al client per la registrazione.
3.	CLASSI ESTERNE
3.1.	DEMO.java: Classe che implementa l’avvio in contemporanea di un server e tre client per testare con facilità il programma
3.2 - CONTENUTO DELLA DIRECTORY
1.	“WordQuizzle/”
1.1.	/Source: Cartella relativa ai sorgenti del progetto
1.1.1.	AbsoluteLayout.jar: Libreria esterna utilizzata per l’interfaccia grafica del client.
1.1.2.	JSON_Simple.jar: Libreria esterna utilizzata per la serializzazione in formato JSON
1.1.3.	Demo.java: Vedi “CLASSI ESTERNE”
1.1.4.	Ari.JSON/Defra.JSON/Tenax.JSON: Utenti già registrati utili a testare il programma
1.1.5.	Dizionario.txt: Dizionario utilizzato per la selezione delle parole da tradurre.
1.1.6.	/WordQuizzleClient: Vedi “CLASSI IMPLEMENTATE – LATO CLIENT”.
1.1.6.1.	Background.png/Offline.png/Online.png: Immagini utilizzate nell’interfaccia grafica.
1.1.7.	/WordQuizzleServer: Vedi “CLASSI IMPLEMENTATE – LATO SERVER”
1.2.	/Relazione: Cartella relativa alla relazione ed alle immagini in essa contenute


4 – ISTRUZIONI PER L’USO
Descriviamo come eseguire passo dopo passo il programma, riferendoci alle operazioni principali di WordQuizzle e osservando l’interfaccia grafica nelle due modalità in cui essa si presenta all’utente.

Il programma è stato sviluppato su NetBeans 8.2 e successivamente testato su Eclipse 2019-12 per formulare le istruzioni per la compilazione in riferimento a quest’ultimo ambiente di sviluppo.

4.1 - COMPILAZIONE
AMBIENTE DI RIFERIMENTO: Eclipse 2019-12
1.	Dal menù a tendina “File” cliccare sulla voce “Open Projects from File System”
2.	Sulla finestra che viene a comparire, selezionare la voce “Directory” ed importare tutta la sottocartella “Source” all’interno della cartella principale e cliccare su “Finish”
3.	Cliccare con il tasto destro sulla cartella “Source” e nel menù a tendina che compare cliccare sulla voce “Properties”
4.	Nella finestra che compare, andare su Java Build Path->Add External Jars e selezionare i file “Absolute_Layout.jar” e “JSON_Simple.jar”, successivamente premere “Apply and close”, in modo da importare le librerie esterne.
5.	Possiamo eseguire il programma in due modi:
5.1.	Via DEMO (Tre client ed un server avviati contemporanetamente):
5.1.1.	Fare click con il tasto destro sul file Demo.java e selezionare la voce “Properties”
5.1.2.	Nella finestra che segue, cliccare su “Run/Debug settings”->New e successivamente su “Java Application”
5.1.3.	Nel tab intitolato “Main” lasciare “Demo” come Main Class, mentre nel tab intitolato “Arguments”, alla voce “Program Arguments” inserire come parametro i numeri 1899,6789 separati da una virgola.
5.1.4.	Cliccare su “Apply and close”
5.1.5.	Premere il tasto “Run Demo” in alto

5.2.	Singolarmente (Numero variabile di client e server avviati indipendentemente dall’utente)
5.2.1.	Fare click con il tasto destro sul file MainClassServer.java nel package “WQServer” e selezionare la voce “Properties”
5.2.2.	Nella finestra che segue, cliccare su “Run/Debug settings”->New e successivamente su “Java Application”
5.2.3.	Nel tab intitolato “Main” lasciare “MainClassServer” come Main Class, mentre nel tab intitolato “Arguments”, alla voce “Program Arguments” inserire come parametro i numeri 1899,6789 separati da una virgola.
5.2.4.	Cliccare su “Apply and close”
5.2.5.	Ripetere lo stesso procedimento per il file MainClassClient.java nel package WordQuizzleClient.
5.2.6.	Eseguire i server ed i client desiderati facendo click con il tasto destro su ognuno di essi e selezionando dal menù a tendina che compare “Run As->Java Application”.




4.2

MODALITA’ LOGIN
 
PREMESSA: L’area avvisi si occupa di mostrare all’utente tutti i messaggi o le notifiche di errore inviate dal programma, i primi vengono visualizzati in verde, le seconde in rosso.
-Registrazione al servizio: 
1.	Inserire il nome utente desiderato nel campo nickname
2.	Inserire la password desiderata nel campo password
3.	Fare click sul bottone registrazione
4.	Nel caso tutto vada a buon fine, comparirà un messaggio in verde nell’area avvisi, altrimenti comparirà una notifica di errore in rosso nella stessa area
N.B: Qualsiasi messaggio comparso nell’area avvisi può essere cancellato semplicemente cliccando su di essa.
-Login:
1.	Inserire il proprio nome utente nel campo nickname
2.	Inserire la propria password nel campo password
3.	Fare click sul bottone login
4.	Nel caso tutto vada a buon fine, il semaforo, che si occupa di ricordare all’utente il suo stato (se è connesso o meno) diventerà verde e comparirà un opportuno messaggio nell’area avvisi.
-Logout:
1.	Mentre si è online, cliccare sul bottone logout.
2.	Il semaforo diventerà rosso e riceverete un messaggio o una notifica di errore nell’area avvisi.
-Richiesta di amicizia:
1.	Mentre si è online,inserire il nickname dell’amico nel campo nickname.
2.	Cliccare sul bottone richiesta amicizia
3.	Una volta finito, riceverete un messaggio o una notifica di errore nell’area avvisi.
-Visualizzazione lista amici:
1.	Mentre si è online, cliccare sul bottone lista amici
2.	La lista verrà visualizzata nell’area liste e classifiche, al di sopra della quale comparirà la scritta “LISTA AMICI” a delineare la funzione appena utilizzata.
3.	Nel caso si verifichi un errore, riceverete una notifica nell’area avvisi.
-Visualizzazione classifica:
1.	Mentre si è online, cliccare sul bottone classifica.
2.	La lista verrà visualizzata nell’area liste e classifiche, al di sopra della quale comparirà la scritta “CLASSIFICA” a delineare la funzione appena utilizzata.
3.	Verrà visualizzata una classifica calcolata in base al punteggio totalizzato nelle varie sfide in ordine decrescente, il proprio nickname verrà sostituito dalla parola “*TU*”.
4.	Nel caso si verifichi un errore, riceverete una notifica nell’area avvisi.
-Visualizzazione punteggio:
1.	Mentre si è online, cliccare sul bottone punteggio.
2.	Il proprio punteggio verrà visualizzato all’interno del campo punteggio
3.	Nel caso si verifichi un errore, riceverete una notifica nell’area avvisi.
-Invio richiesta di sfida:
1.	Mentre si è online, digitare il nome dell’amico da sfidare nel campo centrale.
2.	Cliccare sul bottone centrale su dove è scritto “SFIDA”.
3.	Attendere la risposta dell’utente, verrete notificati se l’utente risponde o se la risposta non viene recapitata mediante un messaggio opportuno all’interno dell’area avvisi.

MODALITA’ SFIDA
 
-Arrivo di una richiesta di sfida:
1.	Una volta arrivata la richiesta, compariranno due pulsanti con su scritto “ACCETTA” e “RIFIUTA”, questi sono rispettivamente il bottone accetta sfida ed il bottone rifiuta sfida.
2.	Avete cinque secondi di tempo per rispondere alla sfida, dopodiché i pulsanti scompariranno e la sfida non sarà considerata accettata.
-Rifiuto di una sfida:
1.	Cliccare prima dello scadere del tempo, sul bottone rifiuta sfida.
2.	Un messaggio comparirà sull’area avvisi, non è di fondamentale importanza al proseguimento della sessione.
-Accettazione di una sfida:
1.	Cliccare prima dello scadere del tempo, sul bottone accetta sfida.
2.	Se non è scaduto il tempo, la sfida avrà inizio ed il programma entrerà in modalità sfida, il bottone centrale sarà caratterizzato dalla scritta “Invia Traduzione”, mentre l’area avvisi segnerà il tempo rimanente e, all’invio di un nuovo vocabolo da tradurre, il numero dei vocaboli tradotti e di quelli complessivi selezionati per questa sfida.

-Invio traduzione:
1.	Attendere che al centro della finestra compaia, scritta in bianco, la parola da tradurre.
2.	Digitare all’interno del campo traduzioni la traduzione che si desidera proporre
3.	Cliccare sul bottone invio traduzioni.
4.	Se sono rimasti vocaboli da tradurre, vi verrà inviato un nuovo vocabolo, altrimenti vi verrà chiesto di attendere la terminazione della sfida.
-Abbandono sfida:
1.	Nel caso dobbiate abbandonare la sfida, basta cliccare sul bottone abbandono sfida (caratterizzato dalla scritta “LOGOUT”)
2.	L’avversario verrà notificato, alla prossima azione, del vostro abbandono, e gli verranno assegnati tre punti extra oltre a quelli guadagnati.
3.	Comparirà un messaggio sull’area avvisi, voi sarete liberi dalla sfida e potrete anche sfidare altri utenti.
-Fine sfida ed assegnazione punteggio:
1.	Nel caso il tempo a disposizione scada, comparirà un messaggio che vi dirà se siete usciti vincitori o vinti dalla sfida e dichiarerà il vostro punteggio in relazione a quello dell’avversario, i punti vengono assegnati come segue:
a.	La parola tradotta correttamente fa guadagnare due punti
b.	La parola tradotta erratamente toglie un punto
c.	La parola non tradotta (per mancanza di tempo) non fa guadagnare alcun punto.
d.	Al vincitore verranno assegnati tre punti extra.
2.	Uscirete dalla sfida e potrete giocare con altri utenti.
BUON DIVERTIMENTO!
