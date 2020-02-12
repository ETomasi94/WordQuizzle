/*
PROGETTO FINALE DI LABORATORIO DI RETI A.A 2019-2020
NOME PROGETTO: WORD QUIZZLE

AUTORE: Enrico Tomasi
NUMERO MATRICOLA: 503527

OVERVIEW: Implementazione di un sistema di sfide di traduzione italiano-inglese
tra utenti registrati secondo paradigma client-server
 */
package WordQuizzleClient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/*
    @CLASS TCPWaiter
    @OVERVIEW Classe che implementa un thread che si mette in attesa, nel caso
              venga lanciata o ricevuta una richiesta di sfida, della risposta
              a quest'ultime
*/
public class TCPWaiter implements Runnable
{
    //Client a cui è associato il thread
    MainClassClient Client;
    //Connessione TCP del client
    SocketChannel Connection;
    //Buffer su cui avviene la scrittura e la lettura dei messaggi dle Client via TCP
    ByteBuffer ClientBuffer;
    
    //Flag per la terminazione del programma
    boolean Termination;
    
    //Nome dello sfidante
    String ChallengerName;
    //Nome dello sfidato
    String OpponentName;
    //Modalità del client associato al thread
    String WaitingMode;

    /*
        @METHOD TCPWaiter
        @OVERVIEW Metodo costruttore di un generico thread TCPWaiter
    
        @PAR ClientChannel SocketChannel del client associato
        @PAR AssociatedClient Client associato
        @PAR Opponent Nome dell'avversario
        @PAR Challenger Nome dello sfidante
        @PAR Buffer Buffer dentro cui avvengono lettura e scrittura in TCP
    
        @RETURNS Waiter Nuovo TCPWaiter
    */
    public TCPWaiter(SocketChannel ClientChannel,MainClassClient AssociatedClient,ByteBuffer Buffer)
    {
        Client = AssociatedClient;
        Connection = ClientChannel;
        ClientBuffer = Buffer;
    }
    
    public void SetChallengerName(String challenger)
    {
        ChallengerName = challenger;
    }
    
    public void SetOpponentName(String opponent)
    {
        OpponentName = opponent;
    }
    
    
    /*
        @METHOD SendMessage
        @OVERVIEW Metodo per inviare un messaggio TCP al server, questo metodo
        setta il buffer in modalità lettura, converte il messaggio in input
        in un array di byte in modo da inserirlo nel buffer ed infine resetta 
        il buffer in modalità scrittura e lo invia al server
    
        @PAR buffer ByteBuffer su cui leggere e scrivere
        @PAR text Stringa che rappresenta il messaggio da inserire nel buffer
    
        @THROWS IOException nel caso si verifichi un errore in scrittura o in lettura
    */
    public void SendMessage(ByteBuffer Buffer, String Text) throws IOException
    {
        Buffer.clear();
            
        byte[] message = Text.getBytes();

        Buffer.put(message);

        Buffer.flip();

        while(Buffer.hasRemaining())
        {
            Connection.write(Buffer);
        }
    }
    
      /*
        @METHOD ReceiveMessage
        @OVERVIEW Metodo per ricevere un messaggio via TCP dal server leggendo 
        i dati in arrivo da esso
    
        @PAR buffer ByteBuffer su cui ricvere i dati in arrivo dal server
    
        @RETURNS content Stringa rappresentante il messaggio letto dal buffer
    
        @THROWS IOException nel caso si verifichi un errore in scrittura o in lettura
    */
    public String ReceiveMessage(ByteBuffer Buffer) throws IOException
    {
        Buffer.clear();
        
        Connection.read(Buffer);

        String content = new String(Buffer.array()).trim();
        
        return content;
    }
    
    /*
        @METHOD CleanBuffer
        @OVERVIEW Metodo per resettare il contenuto di un buffer inserendovi al 
        suo interno un nuovo array di byte
    
        @PAR buffer ByteBuffer da resettare
    
        @THROWS IOException nel caso si verifichi un errore durante l'invio del messaggio
    */
    public void CleanBuffer(ByteBuffer Buffer)
    {
        Buffer.clear();
        Buffer.put(new byte[1024]);
    }
    
    /*
        @METHOD CatchChallenge
        @OVERVIEW Metodo che inizializza il TCPWaiter in questione mettendolo
                  in ascolto di messaggi su connessione TCP e decidendo la risposta
                  del client in base all'esito della richiesta di sfida lanciata o ricevuta
                  
        @THROWS IOException nel caso si verifchino errori nella ricezione del risultato della sfida
    */
    public void CatchChallengeAnswer()
    {
        //Stringa che contiene l'esito della richiesta
        String Decision;
            
            try 
            {               
                CleanBuffer(ClientBuffer);
                Decision = ReceiveMessage(ClientBuffer);
                
                switch (Decision) 
                {
                    /*
                        Nel caso avvenga un timeout, il client viene notificato
                        e la richiesta di sfida rimossa
                    */
                    case "TIMEOUT":
                        System.out.println("DECISION: "+Decision);
                        
                        Client.ErrorMessage("Non è stato possibile mandare la richiesta di sfida");
                        
                        SendMessage(ClientBuffer,"REMOVAL--"+ChallengerName);
                        CleanBuffer(ClientBuffer);
                        String NewString = ReceiveMessage(ClientBuffer);
                        
                        System.out.println(NewString);
                        
                        Client.LoginMode();
                        
                        Client.Listener.ResetArrival();
                        
                        break;
                    /*
                        Nel caso la sfida venga rifiutata, il client viene notificato
                        e la richiesta di sfida rimossa
                    */
                    case "REJECTED":
                        Client.ErrorMessage("Ci dispiace, l'utente "+OpponentName+" ha rifiutato la sfida");
                        
                        SendMessage(ClientBuffer,"REMOVAL--"+ChallengerName);
                        CleanBuffer(ClientBuffer);
                        ReceiveMessage(ClientBuffer);
                        
                        Client.LoginMode();
                        
                        Client.Listener.ResetArrival();
                        
                        break;
                    /*
                        Nel caso la sfida venga accettata, il client viene notificato e la 
                        sfida ha inizio
                    */
                    case "ACCEPTED":
                        Client.SuccessMessage("Sfida accettata!");
                        Client.ChallengeMode(OpponentName);

                        SendMessage(ClientBuffer,"SENDMEAWORD--"+Client.name);
                        CleanBuffer(ClientBuffer);
                        String content = ReceiveMessage(ClientBuffer);
                        
                        if(content.contains("STARTTOTRANSLATEBY--"))
                        {
                            String[] credentials = content.split("--",3);
                            String WordToTranslate = credentials[1];
                            int NumWords = Integer.parseInt(credentials[2]);
                            
                            Client.TranslatedWords = 0;
                            Client.RightAnswers = 0;
                            
                            Client.HandleTranslationReceived(WordToTranslate.toUpperCase(),0,NumWords);
                        }
                        
                        break;
                    default:
                        break;
                }
                
            } 
            catch (IOException ex) 
            {
                Client.ErrorMessage("Errore nella lettura del risultato della sfida");
                Client.Disconnect();
                Client.LoginMode();
                
                Termination = true;
            }
    }
    
    /*
        @METHOD ShutDown
        @OVERVIEW Metodo che setta il flag di terminazione a true in modo da
                  terminare l'esecuzione del thread TCPWaiter
    */
    public void ShutDown()
    {
        Termination = true;
    }
    
    /*
        @METHOD run
        @OVERVIEW Metodo che implementa il ciclo di vita di un thread TCPWaiter
    */
    @Override
    public void run() 
    {  
            CatchChallengeAnswer();
    }
}
    
