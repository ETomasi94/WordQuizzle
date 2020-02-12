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
import static java.lang.Thread.sleep;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/*
    @CLASS UDPListener
    @OVERVIEW Classe che implementa un task listener che si mette in ascolto di datagrammi  
    UDP contenenti richieste di sfida inviati al client
*/
public class UDPListener implements Runnable
{
    //Connessione su cui il task è in ascolto
    DatagramSocket connection;
    //Client a cui è collegato il listener
    MainClassClient Client;

    //Flag per terminare il task
    public boolean Termination;
    
    //Flag per consentire o negare l'arrivo di pacchetti
    public boolean NoMorePackets;

    //Array di byte utile per contenere il messaggio
    byte[] Message;
    
    /*
        @METHOD UDPListener
        @OVERVIEW Metodo costruttore del task 
        
        @PARAM client Client a cui è collegato e che gestisce l'accettazione o il rifiuto della sfida
        @PARAM UDPConn Connessione su cui mettersi in ascolto    
    */
    public UDPListener(MainClassClient client,DatagramSocket UDPConn)
    {
        connection = UDPConn;
        Client = client;
        Termination = false;
        Message = new byte[1024];
    }
    
    /*
        @METHOD ArrivalHalt
        @OVERVIEW Metodo che setta il flag NoMorePackets a true impedendo che arrivino
                  ulteriori pacchetti UDP da altri utenti
    */
    public void ArrivalHalt()
    {
        NoMorePackets = true;
    }
    
    /*
        @METHOD ArrivalHalt
        @OVERVIEW Metodo che setta il flag NoMorePackets a false consentendo che arrivino
                  ulteriori pacchetti UDP da altri utenti
    */
    public void ResetArrival()
    {
        NoMorePackets = false;
    }

    /*
        @METHOD ShutDown
        @OVERVIEW Metodo che termina il task settando il flag Termination a true
    */
    public void ShutDown()
    {
        Termination = true;
    }
    
    public void CatchChallengeRequests()
    {   
        NoMorePackets = false;
        String content;
        
        while(!Termination)
        {                                            
            try 
            {     
                /*
                    Il listener riceve i pacchetti soltanto se l'utente non sta già
                    sfidando un altro giocatore
                */
                if(!NoMorePackets)
                {
                    DatagramPacket Packet = new DatagramPacket(Message,1024);

                    connection.receive(Packet);

                    content = new String(Packet.getData());

                    /*
                        Un pacchetto UDP è valido solo se il suo payload contiene
                        una stringa del tipo "CHALLENGE--<NomeSfidante>", altrimenti
                        viene ignorato
                    */
                    if(content.contains("CHALLENGE"))
                    {              
                        ArrivalHalt();
                        
                        String[] M = content.split("--",2);
                        String ChallengeSender = M[1].trim();
                        
                        //Gestione della richiesta ad opera del client
                        Client.HandleChallengeLaunched(ChallengeSender,Packet);
                        
                        sleep(5000);
                    }
                } 
            }
            catch (IOException | InterruptedException ex) 
            {
                Client.Disconnect();
            } 
        
        }
    }

    /*
        @METHOD run
        @OVERVIEW Metodo di esecuzione del UDPListener che mette in ascolto il
        thread a cui è stato assegnato sulla connessione UDP connection finché
        non arriva un pacchetto contenente una richiesta di sfida
    */
    @Override
    public void run() 
    {
      CatchChallengeRequests();
    }
}
