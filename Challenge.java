/*
PROGETTO FINALE DI LABORATORIO DI RETI A.A 2019-2020
NOME PROGETTO: WORD QUIZZLE

AUTORE: Enrico Tomasi
NUMERO MATRICOLA: 503527

OVERVIEW: Implementazione di un sistema di sfide di traduzione italiano-inglese
tra utenti registrati secondo paradigma client-server
 */
package WQServer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import org.json.simple.parser.ParseException;

/*
    @CLASS Challenge
    @OVERVIEW Classe che implementa un thread che rappresenta la sfida tra
              due utenti
*/
public class Challenge implements Runnable
{
    /*----NICKNAME DEI GIOCATORI E TITOLO DELLA SFIDA----*/
    String Player1 = "--";
    String Player2 = "--";
    String ChallengeName = "--";
    
    /*----CHIAVI DEI GIOCATORI PER IL SELECTOR----*/
    SelectionKey Key1 = null;
    SelectionKey Key2 = null;
    
    Selector OriginalSelector;
    
    //BUFFER SU CUI AVVENGONO LE COMUNICAZIONI DURANTE LA SFIDA
    ByteBuffer ChallengeBuffer = ByteBuffer.allocate(1024);

    //SERVIZIO WORDQUIZZLE ASSOCIATO
    WordQuizzleServer Service;
    
    long Score1;//PUNTEGGIO DEL GIOCATORE 1
    long Score2;//PUNTEGGIO DEL GIOCATORE 2

    boolean Termination;//FLAG DI TERMINAZIONE DELLA SFIDA

    /*
        @METHOD Challenge
        @OVERVIEW Metodo costruttore di un generico thread di sfida
    
        @PAR WQ Servizio WordQuizzle che gestisce la sfida
        @PAR Name Titolo della sfida
        @PAR Origin Selettore del server a cui sono collegati i due sfidanti
        
        @RETURN ch Nuova Challenge
    */
    public Challenge(WordQuizzleServer WQ,String Name,Selector Origin)
    {
        ChallengeName = Name;
        Service = WQ;
        Score1 = 0;
        Score2 = 0;
        Termination = false;
        OriginalSelector = Origin;
    }
    
    /*
        @METHOD AddPlayer
        @OVERVIEW Metodo che aggiunge un giocatore alla sfida
    
        @PAR PlayerKey Chiave di selezione del giocatore
        @PAR NickName Nick del giocatore
    */
    public void AddPlayer(SelectionKey PlayerKey,String NickName)
    {
        if(!Player1.equals("--"))
        {
            Player2 = NickName;
            Key2 = PlayerKey;
        }
        else
        {
            Player1 = NickName;
            Key1 = PlayerKey;
        }
    }

      /*
        @METHOD SendMessage
        @OVERVIEW Metodo per inviare un messaggio TCP al client associato
        al suo SocketChannel, questo metodo setta il buffer in modalità lettura
        converte il messaggio in input in un array di byte in modo da inserirlo 
        nel buffer ed infine resetta il buffer in modalità scrittura e lo invia al server
    
        @PAR Dest SocketChannel del client a cui inviare il messaggio
        @PAR buffer ByteBuffer su cui leggere e scrivere
        @PAR text Stringa che rappresenta il messaggio da inserire nel buffer
    
        @THROWS IOException nel caso si verifichi un errore in scrittura o in lettura
    */
     public void SendMessage(SocketChannel Dest,ByteBuffer buffer, String text) throws IOException
    {
        buffer.clear();
            
        byte[] message = text.getBytes();

        buffer.put(message);

        buffer.flip();

        while(buffer.hasRemaining())
        {
            Dest.write(buffer);
        }
    }
    
    /*
        @METHOD ReceiveMessage
        @OVERVIEW Metodo per ricevere un messaggio TCP dal client associato
        al suo SocketChannel leggendo i dati in arrivo da esso
    
        @PAR Dest SocketChannel del client da cui ricevere il messaggio
        @PAR buffer ByteBuffer su cui leggere

        @THROWS IOException nel caso si verifichi un errore in scrittura o in lettura
    */
    public String ReceiveMessage(SocketChannel Sender,ByteBuffer buffer) throws IOException
    {
        buffer.clear();
        
        Sender.read(buffer);

        String content = new String(buffer.array()).trim();

        return content;
    }
    
    /*
        @METHOD CleanBuffer
        @OVERVIEW Metodo per resettare il contenuto di un buffer inserendovi al 
        suo interno un nuovo array di byte
    
        @PAR buffer ByteBuffer da resettare
    
        @THROWS IOException nel caso si verifichi un errore durante l'invio del messaggio
    */
     public void CleanBuffer(ByteBuffer buffer)
    {
        buffer.clear();
        buffer.put(new byte[1024]);
    }

     /*
        @METHOD run
        @OVERVIEW Metodo che implementa il ciclo di vita di un thread sfida
                  durante il quale il servizio sceglie dieci parole da tradurre
                  e dopo il thread apre un selettore per le due chiavi registrate
                  gestendo l'arrivo e l'invio di messaggi in TCP da entrambi i client
     
       @THROWS IOException se si verificano errori durante la selezione dei canali
                mediante le chiavi
     */
    @Override
    public void run() 
    {
        /*----SCELTA DELLE PAROLE DA TRADURRE----*/
        Set<Entry<String,String>> Words = Service.ChooseWords(10);
        
        Vector<Entry<String,String>> WordsDisposed = new Vector<Entry<String,String>>();
        
        WordsDisposed.addAll(Words);
        
        System.out.println("\n");
        System.out.println("-----SFIDA "+ChallengeName+"\n--PAROLE DA TRADURRE--\n");
        
        //VISUALIZZAZIONE DELLE PAROLE DA TRADURRE
        for(Entry<String,String> e : WordsDisposed)
        {
            System.out.println("PAROLA: "+e.getKey() +"| TRADUZIONE: "+ e.getValue().toString().toLowerCase());
        }
             
        //INIZIALIZZAZIONE DEL SELETTORE E REGISTRAZIONE DELLE CHIAVI DEGLI SFIDANTI
        try 
        {
            Selector ChallengeSelector;
            
            ChallengeSelector = Selector.open();
            
            SocketChannel Channel1 = (SocketChannel) Key1.channel();
            SocketChannel Channel2 = (SocketChannel) Key2.channel();
            
            Channel1.register(ChallengeSelector,SelectionKey.OP_READ);
            Channel2.register(ChallengeSelector,SelectionKey.OP_READ);
            
            while(!Termination)
                {
                    int c = ChallengeSelector.select();
                    if(c > 0)
                        {   
                            Set<SelectionKey> SelectableKeys = ChallengeSelector.selectedKeys();
                            Iterator<SelectionKey> IteratorOnKeys = SelectableKeys.iterator();

                            while(IteratorOnKeys.hasNext())
                            {
                                SelectionKey Key = IteratorOnKeys.next();

                                //Rimozione dal Selected Set
                                IteratorOnKeys.remove();

                                if(Key.isAcceptable())
                                {
                                }
                                if(Key.isConnectable())
                                {
                                }
                                if(Key.isReadable())
                                {
                                    Key.attach(ChallengeBuffer);

                                    SocketChannel Channel = (SocketChannel) Key.channel();

                                    Channel.configureBlocking(false);

                                    ChallengeRead(Key,ChallengeBuffer,Service,WordsDisposed);
                                }
                                if(Key.isWritable())
                                {

                                }
                            }

                        }
                    }
            
                    /*----TERMINAZIONE DEL THREAD SFIDA----*/
                    
                    System.out.println("CHALLENGE "+ChallengeName+" ENDS NOW");

                    Key1.interestOps(SelectionKey.OP_READ);
                    Key2.interestOps(SelectionKey.OP_READ);
                    
                    //Fa sì che il selettore del server legga nuovamente le chiavi degli sfidanti
                    OriginalSelector.wakeup();
                }
                catch (IOException ex)
                {
                    Key1.cancel();
                    Key2.cancel();
                    Termination = true;
                }
    }     
    
    /*
        @METHOD EndGame
        @OVERVIEW Metodo che decide l'esito della sfida e notifica i due sfidanti
                  con un messaggio inviato via TCP
    
       @PAR motivation Stringa che rappresenta il motivo per cui la partita è finita
                       (Timeout oppure abbandono di uno dei due sfidanti)
       @PAR WordsNum Intero che rappresenta il numero di parole totali
       @PAR WQ Servizio WordQuizzleServer che gestisce l'aggiornamento del punteggio dei file utente
       @PAR Score1 Intero lungo che rappresenta il punteggio ottenuto dallo sfidante numero 1
       @PAR Score2 Intero lungo che rappresenta il punteggio ottenuto dallo sfidante numero 2
    */
    public void EndGame(String motivation,int WordsNum,WordQuizzleServer WQ,long Score1,long Score2)
    {
        try 
        {
            String response = "";
            
            /*Nel caso la sfida sia terminata perché il timeout è scaduto,
              allora il server confronta i punteggi ottenuti ed invia via TCP
              ad ognuno dei due sfidanti una stringa con il seguente formato:
            
                ENDCHALLENGE--<NomeVincitore>--<PunteggioGiocatore1>--<PunteggioGiocatore2>--<NumeroDiParoleTotali>
            
                N.B: Nel caso vi sia un pareggio, <NomeVincitore> = DRAW
            */
            if(motivation.contains("CHALLENGEOVER"))
            {
                //Nel caso vi sia un vincitore, a quest'ultimo vengono assegnati tre punti extra
                if(Score1 > Score2)
                {
                    response = ("ENDCHALLENGE--"+Player1);
                    Score1 += 3;
                }
                else if(Score1 == Score2)
                {
                    response = ("ENDCHALLENGE--DRAW");
                }
                else if(Score1 < Score2)
                {
                    response = ("ENDCHALLENGE--"+Player2);
                    Score2 +=3;
                }
                
                WQ.UpdateScore(Player1, Score1);
                WQ.UpdateScore(Player2, Score2);
                
                
            }
            /*Nel caso la sfida sia terminata perché uno dei due sfidanti l'ha abbandonata,
              allora il server confronta i punteggi ottenuti ed invia via TCP
              ad ognuno dei due sfidanti una stringa con il seguente formato:
            
              ENDCHALLENGE--<NomeRinunciante>--<PunteggioGiocatore1>--<PunteggioGiocatore2>--<NumeroDiParoleTotali>
            */
            else if(motivation.contains("OPPONENTLEFT"))
            {
                String[] credentials = motivation.split("--",2);
                String quitter = credentials[1];
                
                //Il server assegna tre punti extra al giocatore che è rimasto nella partita dopo l'abbandono
                if(quitter.equals((Player1)))
                {
                    response = ("CHALLENGEQUITTED--"+quitter);
                    Score2 +=3;
                    
                    WQ.UpdateScore(Player2, Score2);
                }
                else
                {
                    response = ("CHALLENGEQUITTED--"+quitter);
                    Score1 += 3;
                    
                    WQ.UpdateScore(Player1, Score1);
                }
            }
            
            SendMessage((SocketChannel) Key1.channel(),(ByteBuffer) Key1.attachment(),response+"--"+Score1+"--"+Score2+"--"+WordsNum);
            CleanBuffer((ByteBuffer) Key1.attachment());
            
  
            SendMessage((SocketChannel) Key2.channel(),(ByteBuffer) Key2.attachment(),response+"--"+Score2+"--"+Score1+"--"+WordsNum);
            CleanBuffer((ByteBuffer) Key2.attachment());
            
            Termination = true;
        } 
        catch (IOException ex) 
        {
            Termination = true;
        }       
    }
    
    /*
        @METHOD CheckTranslation
        @OVERVIEW Metodo che confronta la traduzione di una parola inviata dal server con quella 
                  fornita dal servizio di traduzioni online, ritornando come risposta
                  
                  -"CORRECT!" nel caso la traduzione inviata sia giusta
                  -"WRONG!" altrimenti
        @PAR Translation Traduzione inviata
        @PAR WordsVec Vettore di parole da cui recuperare la traduzione associata
        @PAR Mark Indice che segna quante parole ha tradotto lo sfidante finore in modo da recuperare
             la traduzione associata all'ultima
        
        @RETURNS Response Risposta fornita in base all'esito del confronto
    */
    public String CheckTranslation(String Translation,Vector<Entry<String,String>> WordsVec, int Mark)
    {
        String T = Translation.toLowerCase().trim();
        String O = WordsVec.get(Mark-1).getValue().toLowerCase().trim();

        System.out.println("TRANSLATION: "+T+" ORIGINAL WORD: "+O);

        String Response;

        if(T.equals(O))
        {
            Response = "CORRECT!";
        }
        else
        {
            Response = "WRONG!";
        }

        return Response;
    }
    
    /*
        @METHOD ChallengeRead
        @OVERVIEW Metodo di lettura e gestione delle richieste in TCP inviate
                  dagli sfidanti via TCP
        
        @PAR Key Chiave di selezione dello sfidante che ha mandato la richiesta
        @PAR buffer ByteBuffer da cui leggere e scrivere i messaggi
        @PAR WQ Servizio WordQuizzle che implementa le operazioni non relative 
                alla sfida
        @PAR WordsVec Vector che contiene le parole da tradurre

        @THROWS IOException nel caso si verifichino errori durante il tentativo
                di completamento della richiesta
    */
    public void ChallengeRead(SelectionKey Key,ByteBuffer buffer,WordQuizzleServer WQ
               ,Vector<Entry<String,String>> WordsVec) throws IOException
   {     
       /*----RICEZIONE E STAMPA DEL CONTENUTO DEL MESSAGGIO RICEVUTO*---*/
       System.out.println("-----------------------------------\n");
       
       SocketChannel KeyClient = (SocketChannel) Key.channel();
       
       buffer.clear();
       
       String content;
       content = ReceiveMessage(KeyClient,buffer);
       
       System.out.println("CHALLENGE: "+ChallengeName);
       System.out.println("MESSAGE: "+content);

       System.out.println("-----------------------------------\n");

       //RICHIESTA DI ABBANDONO DELLA SFIDA
       if(content.contains("LEAVECHALLENGE--"))
       {
             //SUDDIVISIONE DEL MESSAGGIO IN ARRIVO PER RICAVARE LE CREDENZIALI
             String[] credentials = content.split("--",2);
             String nick = credentials[1].trim();
             
             EndGame("OPPONENTLEFT--"+nick,WordsVec.size(),Service,Score1,Score2);
                      
             buffer.compact();
           
             CleanBuffer(buffer);
       }//MESSAGGIO DI RICHIESTA DI AMICIZIA
       else if(content.contains("ADDFRIEND--"))
       {
             String[] Friends = content.split("--",3);
             
             String A = Friends[1].trim();
             String B = Friends[2].trim();
             
             int ResponseCode = WQ.Aggiunta_amico(A, B);
             
             switch(ResponseCode)
             {
                 case -3:
                   content = "ALREADYFRIENDS";
                   break;
                 case -2:
                   content = "IOERROR";
                   break;
                 case -1:
                    content = "NOSUCHPLAYER";
                    break;
                 case 0:
                    content = "NEWFRIENDOK";
                    break;
                 default:
                     break;
             }
             
             buffer.compact();
             
             SendMessage(KeyClient,buffer,content);
             
             CleanBuffer(buffer);
       }//MESSAGGIO DI RICHIESTA RECUPERO LISTA AMICI
       else if(content.contains("FRIENDLIST--"))
       {
           String[] credentials = content.split("--",2);
           String nick = credentials[1];
            try 
            {
                List<String> friendlist = WQ.Lista_amici(nick);

                //WRAPPER UTILE A CREARE UNA LISTA THREAD-SAFE
                List<String> SynList = Collections.synchronizedList(friendlist);
                
                String x = "";
                
                for(String s : SynList)
                {
                    x = x.concat(s+"\n");
                }

                SendMessage(KeyClient,buffer,x);
                
                CleanBuffer(buffer);
            }
            catch (FileNotFoundException ex) 
            {
                SendMessage(KeyClient,buffer,"FILENOTFOUND");
            } 
            catch (ParseException ex) 
            {
                SendMessage(KeyClient,buffer,"PARSEXCEPTION");
            }
       }//MESSAGGIO DI RECUPERO PUNTEGGIO
       else if(content.contains("SCORE--"))
       {
           String[] credentials = content.split("--",2);
           String nick = credentials[1];
           
           long x = WQ.Mostra_Punteggio(nick);
          
           content = (""+x);
           
           CleanBuffer(buffer);
           
           SendMessage(KeyClient,buffer,content);        
       }//MESSAGGIO DI RECUPERO CLASSIFICA
       else if(content.contains("RANK--"))
        {
           String[] credentials = content.split("--",2);
           String nick = credentials[1];
                     
           String Rank;
            
           Rank = WQ.Mostra_Classifica(nick);
           
           SendMessage(KeyClient,buffer,Rank);
           
           CleanBuffer(buffer);
        }//MESSAGGIO DI BENVENUTO
       else if(content.contains("WELCOME--"))
       {
           String[] credentials = content.split("--",2);
           String nick = credentials[1];
           
           String response = "HI--"+nick;
           
           SendMessage(KeyClient,buffer,response);
           CleanBuffer(buffer);
       }//RICHIESTA DI INVIO PAROLA DA TRADURRE INIZIALE DELLA SFIDA
       else if(content.contains("SENDMEAWORD--"))
       {
            String response = WordsVec.get(0).getKey();
            
            response = response.toUpperCase();
           
            SendMessage(KeyClient,buffer,"STARTTOTRANSLATEBY--"+response+"--"+WordsVec.size());
            CleanBuffer(buffer);            
       }//RICHIESTA INVIO ULTERIORE PAROLA DA TRADURRE
       else if(content.contains("SENDTRANSLATION--"))
       {
            int Score;
           
            String[] answer = content.split("--",4);
            String nick = answer[1].trim();
            String translation = answer[2].trim();

            /*Posizione da cui il giocatore deve leggere nel vector contenente i vocaboli
            da tradurre*/
            int PlayerMark = Integer.parseInt(answer[3]);
            
            /*Vocaboli da tradurre rimasti*/
            int Left = ((WordsVec.size()) - PlayerMark);

            String response;
            
            response = CheckTranslation(translation,WordsVec,PlayerMark);
            
            /*
                Nel caso la tracuzione sia corretta si assegnano due punti, altrimenti
                se ne sottrae uno
            */
            if(response.equals("CORRECT!"))
            {
                Score = +2;
            }
            else
            {
                Score = -1; 
            }

            /*
                Verifica del nick in modo da stabilire se assegnare i punti allo sfidante numero uno
                o allo sfidante numero due
            */
            if(nick.equals(Player1.trim()))
            {
                Score1 += Score;
            }
            else
            {
                Score2 += Score;
            }
            
            /*
                Nel caso in cui lo sfidante abbia altre parole da tradurre, le si inviano
                via TCP, altrimenti gli si chiede di aspettare
            */
            if(Left > 0)
            {
                String NewWord = WordsVec.get(PlayerMark).getKey();
                
                NewWord = NewWord.toUpperCase();
            
                SendMessage(KeyClient,buffer,response+"--"+NewWord+"--"+WordsVec.size());
                CleanBuffer(buffer);       
            }          
            else
            {   
                SendMessage(KeyClient,buffer,response+"--WAITFORCHALLENGETOFINISH");
                CleanBuffer(buffer);
            }         
        }//MESSAGGIO DI TIMEOUT E QUINDI RICHIESTA DI CONCLUSIONE SFIDA
       else if(content.contains("TIMEOUT--"))
       {
            EndGame("CHALLENGEOVER",WordsVec.size(),Service,Score1,Score2);
       }

       buffer.clear();
       
       CleanBuffer(buffer);
   }
}
