/*
PROGETTO FINALE DI LABORATORIO DI RETI A.A 2019-2020
NOME PROGETTO: WORD QUIZZLE

AUTORE: Enrico Tomasi
NUMERO MATRICOLA: 503527

OVERVIEW: Implementazione di un sistema di sfide di traduzione italiano-inglese
tra utenti registrati secondo paradigma client-server
*/
package WQServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/*
    @CLASS WQService
    @OVERVIEW Classe che implementa il servizio WordQuizzle e le operazioni
    che esso mette a disposizione del giocatore
*/
class WordQuizzleServer extends RemoteServer implements Requests,RemoteRequests
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 100;

	//Tabella hash che memorizza i giocatori online indicizzandoli per nome
    public ConcurrentHashMap<String,Player> AvailablePlayers;
    
    /*
        Tabella hash che memorizza lo stato di tutte le sfide indicizzate in base
        alla SelectionKey dello sfidante
    */
    public ConcurrentHashMap<SelectionKey,String> PendingChallenges = new ConcurrentHashMap<SelectionKey,String>();
    
    //Dizionario caricato in memoria
    public Vector<String> Dictionary;
    
    //Threadpool contenente le sfide in corso
    ExecutorService Challenges = Executors.newCachedThreadPool();
    
    /*
        @METHOD WQService
        @OVERVIEW Costruttore del WQService che inizializza la tabella degli
        utenti online
    
        @THROWS RemoteException Se l'inizializzazione del metodo remoto Registra_utente
        fallisce
    */
    public WordQuizzleServer() throws RemoteException
    {
        AvailablePlayers = new ConcurrentHashMap<String,Player>();
        Dictionary = this.LoadDictionary();
    }
 
    /*
        @METHOD Registra_utente
        @OVERVIEW Metodo di registrazione da remoto di un utente, riceve in input i suoi
        dati di registrazoine e li memorizza in formato JSON
        
        @PARAM password La stringa contenente la password decisa dall'utente in questione
        @PARAM nickname La stringa contenente il nickname deciso dall'utente in questione
    */
    @Override
    public int Registra_utente(String password, String nickname) throws RemoteException
    {
       //Se la password è vuota, errore
       if(password.isEmpty())
       {
           System.out.println("ERRORE: Inserita una password vuota");
           return -1;
       }
       
       //Se il giocatore esiste già, errore
       if(FindPlayer(nickname))
       {
           System.out.println("ERRORE: Il giocatore "+nickname+" esiste già");
           return -2;
       }
       
       //Istanziazione del giocatore mediante password e nickname
       Player guy = new Player(password,nickname);
       
       //Scrittura dei dati di registrazione in formato JSON
       WritePlayerData(guy);
       
       return 1;
    }

    /*
        @METHOD Login
        @OVERVIEW Metodo che implementa il login al server di un utente
    
        @PARAM nickUtente Il nickname dell'utente
        @PARAM password La password dell'utente
    
        @RETURNS response Stringa rappresentante la risposta da inviare in TCP
        al client che ha inviato la richiesta
    */
    @Override
    public String Login(String nickUtente,String password) 
    {
        String response;
            
          /*
             Controlla che il giocatore sia registrato al servizio, successivamente
             verifica la sua password sia giusta ed invia un messaggio a seconda del risultato.
        
             Se tutto va a buon fine, crea un nuovo giocatore con il nome utente e la password
             ricevuti e lo inserisce nella tabella dei giocatori online
          */
          if(FindPlayer(nickUtente))
             {
                String check = CheckPassword(nickUtente, password);
                
                if(AvailablePlayers.containsKey(nickUtente))
                {  
                    response = "ALREADYLOGGED";
                }
                else if(check.equals("OKPASSWORD".trim()))
                 {
                    response = ("OKLOGIN");
                     
                    Player user = new Player(password,nickUtente);
                    AvailablePlayers.put(nickUtente, user);
                 }
                 else
                 {
                     response = ("PASSWORDERROR");
                 }
             }
             else
             {
                  response = ("NOSUCHPLAYER");
             }
          
          return response;
    }
    
    /*
        @METHOD Logout
        @OVERVIEW Metodo che implementa il logout dal servizio
        @PARAM nickUtente Stringa che rappresenta il nickname dell'utente
    
        @RETURNS response Stringa rappresentante la risposta da inviare in TCP
        al client che ha inviato la richiesta
    */
    @Override
    public String Logout(String nickUtente) 
    {
        Player user = AvailablePlayers.get(nickUtente);
        
        if(user != null)
        {
           AvailablePlayers.remove(nickUtente);
        }
        
        String response = "DISCONNECTIONOK";
        
        return response;
    }
    
    /*
        @METHOD Aggiunta_Amico  
        @OVERVIEW Metodo che implementa l'aggiunta di un amico da parte di un utente A
        verso un utente B e vicevers
        
        @PARAM NickA Nick dell'utente richiedente
        @PARAM NickB Nick dell'utente destinatario
    
        @RETURNS Un intero r che indica se l'operazione è andata a buon fine (r=0) oppure vi è 
        stato un errore, nello specifico:
            r = -1 Indica che è non è stato possibile trovare uno dei giocatori
            r = -2 Indica che non è stato possibile recuperare la lista amici di uno dei due giocatori
            per completare la richiesta o si è verificato un errore nell'aggiornamento di queste ultime
            r = -3 Indica che i due giocatori sono già amici       
    */
    @Override
    public int Aggiunta_amico(String NickA, String NickB) 
    {       
        if((!FindPlayer(NickA)) || !FindPlayer(NickB))
        {
            System.out.println("ERRORE: Impossibile trovare uno dei due giocatori\n");
            return -1;
        }
        
        //Caricamento dei riferimenti ai file relativi agli utenti
        File InfoA = LoadPlayerInfo(NickA);
        File InfoB = LoadPlayerInfo(NickB);

        //Caricamento dei file JSON relativi agli utenti
        JSONObject PlayerDataA = GetPlayerJSON(InfoA);
        JSONObject PlayerDataB = GetPlayerJSON(InfoB);
        
        if(PlayerDataA == null || PlayerDataB == null || NickA.equals(NickB))
        {
            System.out.println("Errore nel recupero della lista amici di uno dei due giocatori");
            return -2;
        }
        
        //Caricamento delle liste di amicizia degli utenti (campo "AMICI" nel file JSON)
        JSONArray FriendListA = (JSONArray) PlayerDataA.get("AMICI");
        JSONArray FriendListB = (JSONArray) PlayerDataB.get("AMICI");
        
        if((FriendListA.contains(NickB)) || FriendListB.contains((NickA)))
        {
            System.out.println(NickA+" e "+NickB+" sono già amici");
            return -3;        
        }
        
        //Aggiunta reciproca dei due amici
        FriendListA.add(NickB);
        FriendListB.add(NickA);
        
        //Scrittura sui file JSON relativi ai due amici per aggiornamento
        try 
        {
            FileWriter WriterA = new FileWriter(InfoA);
            FileWriter WriterB = new FileWriter(InfoB);
            
            WriterA.write(PlayerDataA.toJSONString());
            WriterB.write(PlayerDataB.toJSONString());
            
            WriterA.close();
            WriterB.close();            
        }
        catch (IOException ex) 
        {
            //Errore nell'aggiornamento delle liste di amicizia
            return -2;
        }
   

        return 0;             
    }
    
     /*
        @METHOD Lista_Amici
        @OVERVIEW Metodo che recupera la lista amici del richiedente dal suo file JSON e la restituisce al
        programma main come Lista di stringhe
        
        @PARAM nickUtente Stringa rappresentante il nickname dell'utente

        @RETURNS Friends Lista di stringhe rappresentante la lista amici dell'utente, null altrimenti
    
        @THROWS FileNotFoundException se non è stato possibile recuperare le informazioni sull'utente
                
                IOException se si è verificato un errore in lettura o in scrittura sui file JSON
                
                ParseException se il parsing in JSON delle informazioni utente (all'interno del metodo
                GetPlayerJSON) non è andato a buon fine
    */
    @Override
    public List<String> Lista_amici(String nickUtente) throws FileNotFoundException, IOException, ParseException 
    {
        File InfoPlayers = new File(System.getProperty("user.dir")+"\\"+nickUtente.trim()+".JSON");
        
        //Se il giocatore non esiste, errore
        if(!InfoPlayers.exists())
        {
            System.out.println("Il giocatore "+nickUtente.trim()+" non è ancora stato registrato\n");
            return null;
        }

        JSONObject PlayerData = GetPlayerJSON(InfoPlayers);
        
        List<String>  Friends = (List<String>) PlayerData.get("AMICI");
        
        return Friends;
    }
    
     /*
        @METHOD CheckFriendship
        @OVERVIEW Metodo che controlla se due utenti PlayerA e PlayerB sono amici
        
        @PARAM PlayerA Utente A
        @PARAM PlayerB Utente B
    
        @RETURNS Booleano b che indica il risultato del controllo
    */
    public boolean CheckFriendship(String PlayerA,String PlayerB)
    {
        File DataOfPlayerA = LoadPlayerInfo(PlayerA);
        File DataOfPlayerB = LoadPlayerInfo(PlayerB);
        
        JSONObject JSONA = GetPlayerJSON(DataOfPlayerA);
        JSONObject JSONB = GetPlayerJSON(DataOfPlayerB);
        
        JSONArray FriendsOfA = (JSONArray) JSONA.get("AMICI");
        JSONArray FriendsOfB = (JSONArray) JSONB.get("AMICI");
        
        if(FriendsOfA.contains(PlayerB) && FriendsOfB.contains(PlayerA))
        {
            return true;
        }
        else
        {
            return false;
        }       
    }

     /*
        @METHOD Sfida
        @OVERVIEW Metodo che implementa la sfida di un utente A verso un utente B
        
        @PARAM nickUtente
        @PARAM nickAmico
    
        @RETURNS response Stringa rappresentante la risposta da dare in TCP al richiedente
    */
    @Override
    public String Sfida(String nickUtente, String nickAmico) 
    {
        AvailablePlayers.get(nickUtente);
        Player Opponent = AvailablePlayers.get(nickAmico);

        if(nickUtente.equals(nickAmico))
        {
            return "NOSUCHPLAYERONLINE";
        }
        
        
        if(Opponent != null)
        {         
            if (CheckFriendship(nickUtente,nickAmico))
            {
                return "CHALLENGESENT";
            }
            else
            {
                return "ISNOTFRIENDOFYOURS";
            }
            
        }
        else
        {
            return "NOSUCHPLAYERONLINE";
        }
    }
    
    
    /*
        @METHOD CheckOnline
        @OVERVIEW Metodo che controlla che un utente sia disponibile o meno verificando
        se la tabella degli utenti disponibili contiene una entry indicizzata dal suo nome
    
        @PAR friend Stringa che rappresenta il nickname che indicizza l'utente da trovare
    */
    public boolean CheckOnline(String friend)
    {
        return AvailablePlayers.containsKey(friend);
    }

     /*
        @METHOD Mostra_Punteggio
        @OVERVIEW Metodo che restituisce il punteggio di un utente
        
        @PARAM nickUtente Nickname dell'utente
    
        @RETURNS score Intero lungo che rappresenta il punteggio dell'utente
    */
    @Override
    public long Mostra_Punteggio(String nickUtente) 
    {
        File InfoPlayer = LoadPlayerInfo(nickUtente);
        
        JSONObject PlayerData = GetPlayerJSON(InfoPlayer);
        
        long score = (long) PlayerData.get("PUNTEGGIO");
        
        return score;
    }

     /*
        @METHOD Mostra_Classifica
        @OVERVIEW Metodo che restituisce sotto forma di stringa la classifica del gioci
        calcolata in base ai punteggi ottenuti dal giocatore che ha mandato la richiesta
        e dai suoi amici
        
        @PARAM nickUtente Nickname del giocatore richiedente
    
        @RETURNS Rank stringa rappresentante la classifica
    */
    @Override
    public String Mostra_Classifica(String nickUtente) 
    {
        File InfoPlayer = LoadPlayerInfo(nickUtente);
        
        JSONObject PlayerData = GetPlayerJSON(InfoPlayer);
        
        List<String> FriendList = (List<String>) PlayerData.get("AMICI");
        
        /*Ordiniamo gli utenti in ordine decrescente in base al punteggio 
         (istanza di ScoreComparator) ed inseriamoli nella ScoreMap, successivamente
         creiamo un set ordinato secondo punteggio ed inseriamo ogni suo elemento
         all'interno della stringa rank 
        */
        SortedMap<Long,String> ScoreMap = new TreeMap<Long,String>(new ScoreComparator());

        for(String player : FriendList)
        {
            long player_score = Mostra_Punteggio(player);
            
            ScoreMap.put(player_score, player);
        }
        
        long score_personal = Mostra_Punteggio(nickUtente);
        
        ScoreMap.put(score_personal,"*TU*");
        
        //Ordiniamo 
        Set<Entry<Long,String>> Rnk = ScoreMap.entrySet();
        
        String Rank = "";
        
        int i = 1;
        
        for(Entry<Long,String> e : Rnk)
        {
            Rank = Rank.concat(""+i+"-"+e.getValue()+" |PUNTI: "+e.getKey()+"\n");
            i++;
        }

        return Rank;
    }
    
     /*
        @METHOD UpdateScore
        @OVERVIEW Metodo che aggiorna il punteggio di un utente scrivendolo
        su file JSON
        
        @PARAM nick Nickname dell'utente
        @PARAM score
    
        @RETURNS Intero i che rappresenta il risultato dell'operazione, 0 se è
        andata a buon fine, -1 se si sono verificati errori in scrittura o in 
        lettura sul file JSON
    */
    public int UpdateScore(String nick,long score)
    {
        FileWriter writer = null;
        try 
        {
            File InfoPlayers = LoadPlayerInfo(nick);
            
            JSONObject PlayerData = GetPlayerJSON(InfoPlayers);
            
            long ActualScore = ((long) PlayerData.get("PUNTEGGIO") + score);
                 
            PlayerData.put("PUNTEGGIO", ActualScore);
            
            writer = new FileWriter(InfoPlayers);
            
            writer.write(PlayerData.toJSONString());
            
            writer.close();
            
        } 
        catch (IOException ex) 
        {
           return -1;
        } 
        finally 
        {
            try 
            {
                writer.close();
            } 
            catch (IOException ex) 
            {
                return -1;
            }
        }
        
        return 0;
    }
    
     /*
        @METHOD FindPlayer
        @OVERVIEW Metodo che verifica che un determinato giocatore sia registrato
        al servizio (e che quindi esista il suo file JSON personale) o meno
        
        @PARAM nick Stringa che rappresenta il nickname dell'utente
      
        @RETURNS Booleano b che rappresenta il risultato dell'operazione
    */
    public boolean FindPlayer(String nick)
    {
         File InfoPlayers = new File(System.getProperty("user.dir")+"\\"+nick.trim()+".JSON");
         
         return InfoPlayers.exists();
    }
    
     /*
        @METHOD CheckPassword
    
        @OVERVIEW Metodo che verifica che la password che è stata immessa dall'utente
        richiedente sia corretta confrontandola con quella del rispettivo file JSON
        e formnulando una stringa di risposta a seconda del risultato
        
        @PARAM nick Stringa che rappresenta il nickname dell'utente
        @PARAM Pword Stringa che rappresenta la password che è stata immessa
    
        @RETURNS Stringa result che sintetizza l'esito dell'operazione
    */
    public String CheckPassword(String nick,String Pword)
    {
         File InfoPlayers = LoadPlayerInfo(nick);
         
         JSONParser parser = new JSONParser();
         
         try
         {
             JSONObject PlayerData = (JSONObject) parser.parse(new FileReader(InfoPlayers));
             String result = (String) PlayerData.get("PASSWORD");
             
             if(Pword.equals(result))
             {
                return "OKPASSWORD";
             }
             else
             {
                 return "PASSWORDERROR";
             }
         } 
         catch (FileNotFoundException ex) 
         {
            System.out.println("ERRORE: File "+InfoPlayers.getPath()+" non trovato");
         } 
         catch (IOException ex) 
        {
            System.out.println("ERRORE di I/O riscontrato");
        } catch (ParseException ex) 
        {
            System.out.println("ERRORE nel parsing del file in formato JSON");
        }
        
         return "PASSWORDERROR";
    }
       
     /*
        @METHOD WritePlayerData
        @OVERVIEW Metodo che scrive i dati di registrazione di un giocatore in un file JSON
        
        @PARAM guy Giocatore da cui prendere i dati di registrazione
    */
    public void WritePlayerData(Player guy)
    {
        //CARICAMENTO DELLEINFOUTENTI
        File InfoPlayers = new File(System.getProperty("user.dir")+"\\"+guy.GetNick().trim()+".JSON");
       
        if(InfoPlayers.exists())
        {
            System.out.println("IL GIOCATORE "+guy.GetNick()+" ESISTE GIA'\n");
            return;
        }

        //Trasformazione dei dati di registrazione di un giocatore in formato JSON
        JSONObject Data = guy.PlayerToJSON();
        
        try(FileWriter fw = new FileWriter(InfoPlayers))
        {
            fw.write(Data.toJSONString());
            fw.close();
        } 
        catch (IOException ex) 
        {
           System.out.println("ERRORE: Impossibile scrivere file JSON relativo al giocatore "+guy.GetNick()+"\n");
        }
    }
    
     /*
        @METHOD GetTranslation  
        @OVERVIEW Metodo che contatta il servizio online di traduzione mediante
        connessione HTTP per ricevere la traduzione della parola data in input
        
        @PARAM word Stringa rappresentante la parola da tradurre

        @RETURNS Translation Stringa rappresentante la traduzione della parola data in input
    */
    public static String GetTranslation(String word)
    {
        String request = ("https://api.mymemory.translated.net/get?q="+word+"&langpair=it|en");
        
        URL url = null;
        
        try 
        {
            url = new URL(request);

        } 
        catch (MalformedURLException ex) 
        {
            System.out.println("ERRORE: Il sistema non è riuscito ad inviare una richiesta di traduzione via URL valida\n");
        }
        
        HttpURLConnection connection = null;
        
        try 
        {
            connection = (HttpURLConnection) url.openConnection();
        } 
        catch (IOException ex) 
        {
            System.out.println("ERRORE: Impossibile aprire una connessione con il sito per tradurre la parola desiderata\n");
        }
        
        try 
        {
            connection.setRequestMethod("GET");
        } 
        catch (ProtocolException ex) 
        {
            System.out.println("ERRORE: Impossibile utilizzare il protocollo per la connessione");
        }
        
        
        BufferedReader in = null;
        try 
        {
            in = new BufferedReader
                (new InputStreamReader(connection.getInputStream()));
        } 
        catch (IOException ex) 
        {
           System.out.println("ERRORE: Impossibile inizializzare uno stream di lettura da canale di connessione");
        }
        
        String inputLine;
            
        StringBuilder content = new StringBuilder();
        
        /*
            Leggendo in input dalla connessione HTTP, il servizio ricava il
            contenuto della risposta (in formato JSON) e lo trasforma in stringa
            e successivamente analizza i campi "responseData" e "translatedText" 
            per ricavare la traduzione e trasformarla in stringa
        */
        try 
        {
            while ((inputLine = in.readLine()) != null)
            {
                content.append(inputLine);
            }
            
            in.close();
        } 
        catch (IOException ex) 
        {
           System.out.println("ERRORE: Impossibile leggere dati dalla connessione con il sito");
        }
      
        connection.disconnect();
        
        String ct = content.toString();
        
        JSONParser Parser = new JSONParser();
        
        String Translation = "";

        try 
        {
            JSONObject Obj = (JSONObject) Parser.parse(ct);
                           
            JSONObject ResponseData = (JSONObject) Obj.get("responseData");
        
            Translation = (String) ResponseData.get("translatedText");
        } 
        catch (ParseException ex) 
        {
            System.out.println("ERRORE: Impossibile effettuare il parsing della richiesta in oggetto JSON");
        }
        
        return Translation;
    }
    
     /*
        @METHOD LoadDictionary
    
        @OVERVIEW Metodo che carica il dizionario (presente nel file Dizionario.txt all'interno
        della cartella del progetto) in memoria
    
        @RETURNS Dizionario ArrayList di stringhe rappresentante un dizionario
    */
    public Vector<String> LoadDictionary()
    {
        //CARICAMENTO DEL DIZIONARIO
        File dictionary = new File(System.getProperty("user.dir")+"\\Dizionario.txt");
        
        FileReader frd = null;
        
        try 
        {
            frd = new FileReader(dictionary);
        } 
        catch (FileNotFoundException ex) 
        {
            System.out.println("IMPOSSIBILE TROVARE IL FILE CONTENENTE IL DIZIONARIO\n");
        }
        
        BufferedReader rd = new BufferedReader(frd);
        
        Vector<String> Dizionario = new Vector<String>();
        
        String Vocabolo;
        
        try 
        {
            while((Vocabolo = rd.readLine()) != null)
            {
                Vocabolo = Vocabolo.trim();
                Dizionario.add(Vocabolo);
            }
        } 
        catch (IOException ex) 
        {
           System.out.println("ERRORE: Impossibile leggere dal file contenente il dizionario\n");
        }
        
        return Dizionario;
    }
    
     /*
        @METHOD ChooseWords
        @OVERVIEW Metodo che sceglie K parole da un dizionario e restituisce una HashMap
        in cui ad ognuna di esse è associata la traduzione
        
        @PARAM Dictionary ArrayList di stringhe rappresentante un dizionario
        @PARAM K intero rappresentante il numero di parole da tradurre
    
        @RETURNS Result HashMap che associa ad ogni vocabolo la sua traduzione
    */
    public Set<Entry<String,String>> ChooseWords(int K)
    {
        Random r = new Random();
       
        ConcurrentHashMap<String,String> Words = new ConcurrentHashMap<String,String>();
        
        for(int index=0; index<K; index++)
        {
            int i = r.nextInt(Dictionary.size());
          
            String word = Dictionary.get(i);
            String translation = GetTranslation(word);
            
            Words.put(word,translation);
        }
        
        Set<Entry<String,String>> Result = Words.entrySet();
        
        return Result;               
    }
    
     /*
        @METHOD LoadPlayerInfo
        @OVERVIEW Metodo che restituisce il riferimento al file JSON di un utente
        
        @PARAM NickName stringa che rappresenta il nickname dell'utente
    
        @RETURNS InfoPlayers riferimento al file JSON dell'utente
    */
    public File LoadPlayerInfo(String NickName)
    {
        //CARICAMENTO DELLEINFOUTENTI
        File InfoPlayers = new File(System.getProperty("user.dir")+"\\"+NickName.trim()+".JSON");
        
        if(!InfoPlayers.exists())
        {
            System.out.println("Il giocatore "+NickName.trim()+" non è ancora stato registrato\n");
            return null;
        }
        
        return InfoPlayers;
    }
    
     /*
        @METHOD GetPlayerJSON
        @OVERVIEW Metodo che dato in input il riferimento al file JSON relativo ad un utente
        ne restituisce il JSONObjetc associato
        
        @PARAM InfoPlayers riferimento al file JSON
    
        @RETURNS PlayerData JSONObject contenente tutte le informazioni di registrazione di un utente
    */
        public JSONObject GetPlayerJSON(File InfoPlayers)
    {
        try 
        {
            JSONParser parser = new JSONParser();
                  
            JSONObject PlayerData = (JSONObject) parser.parse(new FileReader(InfoPlayers));
            
            return PlayerData;
        } 
        catch (FileNotFoundException ex) 
        {
            return null;
        } 
        catch (IOException | ParseException ex)
        {
            return null;
        }
    }
        
    /*
        @METHOD SetChallenge
        @OVERVIEW Metodo che dichiara una sfida lanciata in sospeso associando
                  alla chiave utente dello sfidante (che la indicizza) lo status "WAITING"
        
        @PAR key SelectionKey dello sfidante
    */
    public void SetChallenge(SelectionKey key)
    {
        PendingChallenges.put(key,"WAITING");
    }
    
        /*
        @METHOD AnswerPendingChallenge
        @OVERVIEW Metodo che risponde ad una sfida lanciata in sospeso associando
                  alla chiave utente dello sfidante (che la indicizza) la risposta 
                  data via UDP, che può essere:
                  
                  -"ACCEEPTED" Se l'utente sfidato ha ricevuto ed accettato la sfida
                  -"REJECTED" Se l'utente sfidato ha ricevuto e rifiutato la sfida
                  -"TIMEOUT" Se l'utente sfidato non ha ricevuto la sfida in tempo
                    e quindi se non ha risposto o il pacchetto UDP relativo è andato perduto
        
        @PAR key SelectionKey dello sfidante
    */
    public void AnswerPendingChallenge(SelectionKey key,String Answer)
    {
        PendingChallenges.put(key,Answer);
    }
    
    /*
        @METHOD RemovePendingChallenge
        @OVERVIEW Metodo che rimuove una sfida lanciata in sospeso utilizzando 
                  la chiave utente dello sfidante (che la indicizza) 
    
        @PAR key SelectionKey dello sfidante
    
        @RETURNS result Stringa che indica l'esito dell'operazione,ovvero:
                 - REMOVALFAIL Se la sfida non è stata mai inserita o è stata
                   già rimossa
                 -REMOVALSUCCESS SE la sfida è stata rimossa con successo
    */
    public String RemovePendingChallenge(SelectionKey key)
    {
        String result = "REMOVALFAIL";
        
        if(PendingChallenges.containsKey(key))
        {
            PendingChallenges.remove(key);
            
            result = "REMOVALSUCCESS";
        }
        
        return result;
    }
    
    /*
        @METHOD GetChallengeResponse
        @OVERVIEW Metodo che recupera la risposta alla richiesta di una sfida utilizzando 
                  la chiave di selezione dello sfidante (che la indicizza)
    
        @PAR key SelectionKey dello sfidante
    
        @RETURNS Stringa Response che rappresenta l'esito dell'operazione, quindi:
                    -La risposta alla sfida se questa è stata trovata  
                    -"NOCHALLENGEPENDING" altrimenti
    */
    public String GetChallengeResponse(SelectionKey key)
    {
        if(PendingChallenges.containsKey(key))
        {
            return PendingChallenges.get(key);
        }
        else
        {
            return "NOCHALLENGEPENDING";
        }
    }
    
    /*
        @METHOD StargChallenge
        @OVERVIEW Metodo che inizializza una sfida sottomettendone il task
                  al threadpool delle sfide
    
        @PAR Player1 SelectionKey dell'utente numero uno
        @PAR NickName1 NickName dell'utente numero uno
        @PAR Player2 SelectionKey dell'utente numero due
        @PAR NickName2 NickName dell'utente numero due
        @PAR Selector Origin selettore del server di provenienza
    */
    public void StartChallenge(SelectionKey Player1,String NickName1,SelectionKey Player2, String NickName2,Selector Origin)
    {
        String challengename = NickName1 + "--" + NickName2;
        
        Challenge challenge = new Challenge(this,challengename,Origin);
        
        challenge.AddPlayer(Player1, NickName1);
        challenge.AddPlayer(Player2, NickName2);
        
        Challenges.submit(challenge);
    }
 }

