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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.json.simple.parser.ParseException;

/*
    @CLASS MainClass
    @OVERVIEW Classe che implementa il ciclo di vita di un server che gestisce
    le richieste in TCP mediante il servizio WordQuizzle sovrastante ed inoltre offre un servizio
    di registrazione in remoto e la gestione in UDP delle sfide mandate ai giocatori.
*/
public class MainClass 
{        
    private static final int DefaultRemotePort = 6789;
    private static final int DefaultTCP_PORT = 1899;
    /*
        @METHOD main    
        @OVERVIEW Metodo che implementa il ciclo di vita del server di WordQuizzle
    
        @PAR TCP_PORT Intero che rappresenta il numero di porta su cui avviene la connessione TCP
        @PAR REMOTE_PORT Intero che rappresenta il numero di porta su cui avviene la connessione del server RMI
    
        @THROWS RemoteException nel caso si verifichino errori durante la connessione
                al servizio in remoto
    */
   public static void main(String args[]) throws RemoteException
   {
        /*Nel caso non arrivano parametri validi, indichiamo all'utente come
        utilizzare questo programma*/
        if(args.length == 0)
        {
            System.out.println("Immettere un numero di porta per la connessione TCP ed uno per la RMI\n");
            
            return;
        }
        
        int TCP_PORT;
        int remoteport;
        
        try
        {
            //Leggiamo il nome dell'host da input
            TCP_PORT = Integer.parseInt(args[0]);
            remoteport = Integer.parseInt(args[1]);
        }
        catch(RuntimeException ex)
        {
            //Nel caso vi sia un errore, assegnamo al server la porta di default
            TCP_PORT = DefaultTCP_PORT;
            remoteport = DefaultRemotePort;
        }
       
        /*
            Tabella hash che associa ad ogni giocatore la sua chiave di selezione
        */
        ConcurrentHashMap<String,SelectionKey> SelectionMap = new ConcurrentHashMap<String,SelectionKey>();

        //Buffer di invio e ricezione
        ByteBuffer buffer = ByteBuffer.allocate(1024);
    
        WordQuizzleServer Service = new WordQuizzleServer();

        //Esportazione oggetto remoto.
        RemoteRequests STUB = (RemoteRequests) UnicastRemoteObject.exportObject(Service, 0);

        //Lancio del registro sulla porta.
        LocateRegistry.createRegistry(remoteport);

        //Restituzione riferimento al registro RMI sulla porta.
        Registry Reg = LocateRegistry.getRegistry(remoteport);

        //Registrazione dello stub
        Reg.rebind("WORD_QUIZZLE_SERVICE",STUB);

        System.out.println("SERVER IN ESECUZIONE");
        
        //Selettore dei canali per la connessione
        Selector selector;
    
        try
        {
            /*----CONFIGURAZIONE INIZIALE DELLA CONNESSIONE----*/
            ServerSocketChannel ConnectionChannel = ServerSocketChannel.open();
            
            ServerSocket ConnectionServerSocket = ConnectionChannel.socket();
            
            ConnectionChannel.configureBlocking(false);
            
            SocketAddress Address = new InetSocketAddress(TCP_PORT);
            
            ConnectionServerSocket.bind(Address);
            /*------------------------------------------------*/
            
            //INIZIALIZZAZIONE DEL SELETTORE E REGISTRAZIONE DEI CANALI
            selector = Selector.open();
            
            ConnectionChannel.register(selector,SelectionKey.OP_ACCEPT);
            
            //CICLO DI VITA DEL SELETTORE
            while(true)
            {
                int n = selector.select();
     
                if(n > 0)
                {
                    Set<SelectionKey> SelectableKeys = selector.selectedKeys();
                    Iterator<SelectionKey> ITKeys = SelectableKeys.iterator();
                    
                    //SCANSIONE DEI SOCKETCHANNEL DA PARTE DEL SELETTORE
                    while(ITKeys.hasNext())
                    {
                       SelectionKey Key = ITKeys.next();
                       
                       ITKeys.remove();
                       
                       SocketChannel Client = null;

                       Player player = null;
                       
                       try
                       {   
                           if(Key.isAcceptable())
                           {
                               Client = ConnectionChannel.accept();

                               Client.configureBlocking(false);
                               
                               SelectionKey ClientKey = Client.register(selector, SelectionKey.OP_READ);

                               ClientKey.attach(buffer);
                               
                               System.out.println("HOST "+Client.getRemoteAddress()+" WENT ONLINE");
                           }
                           else if(Key.isReadable())
                           {                        
                              //GESTIONE DI UNA RICHIESTA TCP
                              ReadHandle(Key,SelectionMap,buffer,player,Service,selector);

                              CleanBuffer(buffer);
                           }
                           else if(Key.isWritable())
                           {         
                           }
                       }
                       catch(IOException e)
                       {
                           System.out.println("CONNESSIONE CON L'HOST INTERROTTA");
                           
                           Key.cancel();
                           Key.channel().close();
                           
                           if(Client != null)
                           {
                                Client.close();
                           }
                       }
                    }
                 
                }
            }
        }
        catch(IOException e)
        {
           e.printStackTrace();
        }
   }
    
   /*
        @METHOD ReadHandle
        @OVERVIEW Metodo che legge le richieste in arrivo via TCP dai client e le gestisce
        inviando una risposta adeguata in base all'esito delle varie operazioni
   
        @PAR Key SelectionKey correntemente selezionata
        @SMap Tabella Hash che indicizza ogni SelectionKey con il NickName del corrispondente giocatore
        @PAR buffer Buffer mediante cui avviene la comunicazione
        @PAR user Giocatore associato all'utente
        @PAR WQ Servizio WordQuizzle sovrastante
        @PAR sl Selettore originale da passare come parametro ai thread sfida creati
   */
   public static void ReadHandle(SelectionKey Key,ConcurrentHashMap<String,SelectionKey> SMap,
    ByteBuffer buffer,Player user,WordQuizzleServer WQ,Selector sl) throws IOException
   {     
       System.out.println("-----------------------------------\n");
       
       SocketChannel KeyClient = (SocketChannel) Key.channel();
       
       String ID = KeyClient.getRemoteAddress().toString();
       
       buffer.clear();
       
       String content;
       content = ReceiveMessage(KeyClient,buffer);
       
       System.out.println("HOST: "+ID+" | "+"REQUEST: "+content);
       
       System.out.println("-----------------------------------\n");

       //RICHIESTA DI LOGIN
       if(content.contains("LOGIN"))
        {
            //SUDDIVISIONE DEL MESSAGGIO IN ARRIVO PER RICAVARE LE CREDENZIALI
             String[] credentials = content.split("--",3);
             String nick = credentials[1].trim();
             String pass = credentials[2].trim();
             
             String response = WQ.Login(nick, pass);
             
             if(response.equals("OKLOGIN"))
             {
                 SMap.put(nick,Key);
             }

             buffer.compact();

             SendMessage(KeyClient,buffer,response);

             CleanBuffer(buffer);
        }
       //RICHIESTA DI DISCONNESSIONE
       else if(content.equals("DISCONNECT"))
       {
           System.out.println("DISCONNETTIAMO L'HOST "+ID);

           Key.cancel();
           Key.channel().close();
           
           SendMessage(KeyClient,buffer,"DISCONNECTIONOK");
           
           buffer.compact();
           
           CleanBuffer(buffer);
       }
       //RICHIESTA DI LOGOUT
       else if(content.contains("DISCONNECTUSER"))
       {
             //SUDDIVISIONE DEL MESSAGGIO IN ARRIVO PER RICAVARE LE CREDENZIALI
             String[] credentials = content.split("--",2);
             String nick = credentials[1].trim();
             
             String response = WQ.Logout(nick);
             
             SendMessage(KeyClient,buffer,response);
             
             Key.cancel();
             Key.channel().close();
            
             buffer.compact();
           
             CleanBuffer(buffer);
       }
       //RICHIESTA DI AMICIZIA
       else if(content.contains("ADDFRIEND"))
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
       }
       //RICHIESTA DI VISUALIZZAZIONE LISTA AMICI
       else if(content.contains("FRIENDLIST"))
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
       }
       //RICHIESTA DI VISUALIZZAZIONE PUNTEGGIO
       else if(content.contains("SCORE"))
       {
           String[] credentials = content.split("--",2);
           String nick = credentials[1];
           
           long x = WQ.Mostra_Punteggio(nick);
          
           content = (""+x);
           
           CleanBuffer(buffer);
           
           SendMessage(KeyClient,buffer,content);        
       }
       //RICHIESTA DI VISUALIZZAZIONE CLASSIFICA
       else if(content.contains("RANK"))
        {
           String[] credentials = content.split("--",2);
           String nick = credentials[1];
                     
           String Rank;
            
           Rank = WQ.Mostra_Classifica(nick);
           
           SendMessage(KeyClient,buffer,Rank);
           
           CleanBuffer(buffer);
        }
       //RICHIESTA DI SFIDA
       else if(content.contains("CHALLENGE"))
       {
           WQ.SetChallenge(Key);
           
           String[] credentials = content.split("--",3);
           String challenger = credentials[1];
           String opponent = credentials[2];
           
           if(WQ.CheckOnline(opponent))
           {
                String ChallengeResponse = WQ.Sfida(challenger,opponent); 
           
                SendMessage(KeyClient,buffer,ChallengeResponse);
                CleanBuffer(buffer);   

                if(ChallengeResponse.equals("CHALLENGESENT"))
                {
                    SelectionKey Challenged = SMap.get(opponent);          
           
                    String Decision;
           
                    Decision = UDPMessage(Challenged,challenger);
           
                    WQ.AnswerPendingChallenge(Key,Decision);
                }
           }
           else
           {
               SendMessage(KeyClient,buffer,"NOSUCHPLAYERONLINE");
           }
       }
       //MESSAGGIO DI ATTESA DI RISPOSTA AD UNA RICHIESTA DI SFIDA
       else if(content.contains("WAITING"))
       { 
          String[] credentials = content.split("--",3);
          String challenger = credentials[1];
          String opponent = credentials[2];
       
          String decision = WQ.GetChallengeResponse(Key);
          
          SelectionKey OpponentKey = SMap.get(opponent);
          SocketChannel OpponentChannel = (SocketChannel) OpponentKey.channel();
          ByteBuffer OpponentBuffer = (ByteBuffer) OpponentKey.attachment();
          
          SendMessage(KeyClient,buffer,decision);
          SendMessage(OpponentChannel,OpponentBuffer,decision);
          
          /*Se la sfida è stata accettata, allora si setta l'
            
          
          */
          if(decision.equals("ACCEPTED"))
          {
              WQ.RemovePendingChallenge(Key);
              Key.interestOps(0);
              OpponentKey.interestOps(0);
              WQ.StartChallenge(Key,challenger, OpponentKey,opponent,sl);
          }
          
          CleanBuffer(buffer);
       }
       //RICHIESTA DI RIMOZIONE DI UNA SFIDA DALLA TABELLA DELLE SFIDE IN SOSPESO
       else if(content.contains("REMOVAL"))
       {
           String response;
           
           response = WQ.RemovePendingChallenge(Key);

           SendMessage(KeyClient,buffer,response);
           
           CleanBuffer(buffer);
       }

       buffer.clear();
       
       CleanBuffer(buffer);
   }
   
   /*
        @METHOD UDPMessage
        @OVERVIEW Metodo per l'invio di un datagramma UDP contenente una richiesta
                  di sfia al client dell'utente sfidato
   
        @PAR Challenged SelectionKey dell'utente sfidato
        @PAR ChallengerName Nome dello sfidante
   
        @THROWS IOException nel caso si verifichino errori durante l'invio
                del datagramma
   */
   public static String UDPMessage(SelectionKey Challenged,String ChallengerName) throws IOException
   {
       String response = "";
       
       try 
       {
           SocketChannel ChallengedChannel = (SocketChannel) Challenged.channel();
           
           ByteBuffer ChallengedBuffer = (ByteBuffer) Challenged.attachment();

           CleanBuffer(ChallengedBuffer);
           
           DatagramSocket Socket = new DatagramSocket();
           
           Socket.setSoTimeout(5000);

           SocketAddress Address = ChallengedChannel.getRemoteAddress();
           
           byte[] Message;
           
           Message = ("CHALLENGESENTBY--"+ChallengerName).getBytes();
           
           DatagramPacket Packet = new DatagramPacket(Message,Message.length,Address);

           Socket.send(Packet);

           Socket.receive(Packet);
           
           String content;
           
           content = new String(Packet.getData());
   
           if(content.contains("CHALLENGEREJECT"))
           {
               response = "REJECTED";
           }
           else
           {
               response = "ACCEPTED";
           }
           
           Socket.close();
       } 
       catch(SocketTimeoutException e)
       {
          response =  "TIMEOUT";
       }
       catch (IOException ex) 
       {
           Challenged.cancel();
       }

       return response;
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
    public static void SendMessage(SocketChannel Dest,ByteBuffer buffer, String text) throws IOException
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
    public static String ReceiveMessage(SocketChannel Sender,ByteBuffer buffer) throws IOException
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
    public static void CleanBuffer(ByteBuffer buffer)
    {
        buffer.clear();
        buffer.put(new byte[1024]);
    }
    

    /*
        @METHOD PrintBuffer
        @OVERVIEW Metodo utilizzato durante il debug per stampare il contenuto
                  del buffer del server
    
        @PAR buf ByteBuffer di cui stampare il messaggio contenuto
    */    
    public static void PrintBuffer(ByteBuffer buf)//DEBUG
    {
        String x = new String(buf.array()).trim();
        System.out.println("IL SERVER HA RICEVUTO: "+x);
    }
}