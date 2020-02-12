/*
PROGETTO FINALE DI LABORATORIO DI RETI A.A 2019-2020
NOME PROGETTO: WORD QUIZZLE

AUTORE: Enrico Tomasi
NUMERO MATRICOLA: 503527

OVERVIEW: Implementazione di un sistema di sfide di traduzione italiano-inglese
tra utenti registrati secondo paradigma client-server
 */
package WordQuizzleClient;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.swing.*;
import java.awt.event.*;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import static java.lang.Thread.sleep;
import java.net.DatagramPacket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import WQServer.RemoteRequests;
import org.netbeans.lib.awtextra.AbsoluteLayout;

/*
    @CLASS MainClassClient
    
    @OVERVIEW Classe che implementa il ciclo di vita di un client con interfaccia
    grafica collegata
*/
public class MainClassClient extends JFrame
{
    /**
	 * 
	 */
	private static final long serialVersionUID = -8578176456123054317L;
	
	//DICHIARAZIONE COMPONENTI DELL'INTERFACCIA GRAFICA
    private JButton AddFriendButton;
    private JTextArea Advertising;
    private JLabel Background;
    private JButton CentralButton;
    private JTextField CentralTextField;
    private JLabel ChallengeCheckLabel;
    private JPanel Container;
    private JButton FriendListButton;
    private JPanel ListFunctionPanel;
    private JScrollPane ListPanel;
    private JLabel ListPlayerFunction;
    private JButton LoginButton;
    private JButton LogoutButton;
    private JLabel OnlineAsLabel;
    private JTextArea OnlineList;
    private JLabel OnlineNameLabel;
    private JPanel OnlinePanel;
    private JLabel OpponentName;
    private JLabel OrderTextLabel;
    private JLabel Password;
    private JTextField PasswordField;
    private JButton RegisterButton;
    private JButton ScoreButton;
    private JButton ScoreTableButton;
    private JTextField ScoreTextField;
    private JLabel TITLE;
    private JLabel TrafficLight;
    private JPanel TranslationPanel;
    private JPanel UserArea;
    private JLabel Username;
    private JTextField UsernameField;
    private JLabel WordToTranslateArea;
    private JButton RejectButton;
    private JButton AcceptButton;
    private Timer ChallengeTimer;

    static int DefaultRemotePort = 6789;//Porta connessione di default
    static int DefaultTCP_Port = 1899;//Porta RMI di default
    
    static int remoteport = 6789;//Porta RMI di default
    static int tcp_port = 1899;//Porta connessione di default
    
    int RightAnswers;//Numero di parole tradotte correttamente
    int TimeChallenge;//Tempo della sfida
    int TranslatedWords;//Parole tradotte

    String password;//Password dell'utente
    String name;//NickName dell'utente
    String opponent = "";//NickName dell'avversario
    String mode;//Modalità di esecuzione del client (LOGIN o CHALLENGE)
    String Word = "";//Paola da tradurre
    
    ByteBuffer ConnectionBuffer = ByteBuffer.allocate(1024);
        
    RemoteRequests RequestManager;//Oggetto che implementa i metodi dell'interfaccia remota
    Remote RMObject;//Oggetto RMI
    
    InetSocketAddress clientaddress = new InetSocketAddress("127.0.0.1",tcp_port);  
    SocketChannel client;   
    DatagramSocket ClientUDP;
    
    UDPListener Listener;   
    ExecutorService StartListener;//Executor per un thread ListenerUDP   
    
    TCPWaiter Waiter;
    ExecutorService StartWaiter;//Executor per un thread TCPWaiter

    
    DatagramPacket UDPChallengeRequest = null;//Datagramma ricevuto dal ListenerUDP
    byte[] UDPMessage = new byte[1024];//Messaggio incapsulato nel datagramma UDP
    
    public MainClassClient() 
    {
        InitGUI();
        LoginMode();
    }
    
    /*
        @METHOD InitGUI
        @OVERVIEW Metodo di inizializzazione delle componenti dell'interfaccia grafica
    */
    public void InitGUI()
    {
        /*----DICHIARAZIONE COMPONENTI DELL'INTERFACCIA GRAFICA----*/
        Container = new JPanel();
        UserArea = new JPanel();
        Username = new JLabel();
        Password = new JLabel();
        UsernameField = new JTextField();
        PasswordField = new JTextField();
        RegisterButton = new JButton();
        LoginButton = new JButton();
        LogoutButton = new JButton();
        AddFriendButton = new JButton();
        TranslationPanel = new JPanel();
        OrderTextLabel = new JLabel();
        WordToTranslateArea = new JLabel();
        CentralTextField = new JTextField();
        CentralButton = new JButton();
        ScoreButton = new JButton();
        ScoreTextField = new JTextField();
        Advertising = new JTextArea();
        TITLE = new JLabel();
        OnlinePanel = new JPanel();
        TrafficLight = new JLabel();
        OnlineAsLabel = new JLabel();
        OnlineNameLabel = new JLabel();
        ChallengeCheckLabel = new JLabel();
        OpponentName = new JLabel();
        RejectButton = new JButton();
        AcceptButton = new JButton();
        ListFunctionPanel = new JPanel();
        ListPlayerFunction = new JLabel();
        ListPanel = new JScrollPane();
        OnlineList = new JTextArea();
        ScoreTableButton = new JButton();
        FriendListButton = new JButton();
        Background = new JLabel();

        /*----INIZIALIZZAZIONE INTERFACCIA GRAFICA----*/
        
        //Finestra (JFrame)
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setTitle("WORDQUIZZLE");
        
        this.setBounds(new Rectangle(0, 0, 800, 800));
        this.setLocation(new Point(100, 100));
        this.setName("ClientGUI");
        this.setResizable(false);
        this.setSize(new Dimension(800, 800));
        
        //La finestra invoca il metodo WindowClosing alla chiusura
        this.addWindowListener(new WindowAdapter() 
        {
            @Override
            public void windowClosing(WindowEvent evt) 
            {
                WindowClosing(evt);
            }
        });

        //Contenitore (JPanel) principale
        Container.setFont(new Font("Tahoma", 0, 18));
        Container.setName("Container");
        Container.setPreferredSize(new Dimension(800, 800));
        Container.setLayout(new AbsoluteLayout());

        //Contenitore area utente (credenziali utente ed amici per registrazione,login,logout e richieste di amicizia)
        UserArea.setOpaque(false);
        UserArea.setPreferredSize(new Dimension(400, 110));

        //Titolo campo del nickname
        Username.setFont(new java.awt.Font("Calisto MT", 1, 18));
        Username.setForeground(new Color(255, 0, 0));
        Username.setText("NOME UTENTE");

        //Titolo campo della password
        Password.setFont(new java.awt.Font("Calisto MT", 1, 18));
        Password.setForeground(new Color(255, 0, 0));
        Password.setText("PASSWORD");

        //Campo del nickname
        UsernameField.setFont(new java.awt.Font("Tahoma", 1, 14));

        //Campo della password
        PasswordField.setFont(new java.awt.Font("Tahoma", 1, 14));

        //Bottone (JButton) per la registrazione da remoto
        RegisterButton.setFont(new Font("Calisto MT", 0, 18));
        RegisterButton.setForeground(new Color(0, 136, 0));
        RegisterButton.setText("REGISTRATI");
        
        /*Il bottone per la registrazione da remoto invoca il metodo
          RegisterButtonMousePressed quando viene cliccato
        */
        RegisterButton.addMouseListener(new MouseAdapter() 
        {
            @Override
            public void mousePressed(MouseEvent evt) 
            {
                RegisterButtonMousePressed(evt);
            }
        });

        //Bottone (JButton) per il login
        LoginButton.setFont(new Font("Calisto MT", 0, 16));
        LoginButton.setText("LOGIN");
        LoginButton.setPreferredSize(new Dimension(95, 35)); 
        
        /*Il bottone per il login invoca il metodo
          LoginButtonMousePressed quando viene cliccato
        */
        LoginButton.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent evt) 
            {
                LoginButtonMousePressed(evt);
            }
        });

        //Bottone (JButton) per il logout
        LogoutButton.setFont(new Font("Calisto MT", 0, 18));
        LogoutButton.setForeground(new Color(255, 0, 0));
        LogoutButton.setText("LOGOUT");      
        
        /*Il bottone per il logout invoca il metodo
          LoginButtonMousePressed quando viene cliccato
        */
        LogoutButton.addMouseListener(new MouseAdapter() 
        {
            @Override
            public void mousePressed(MouseEvent evt) 
            {
                LogoutButtonMousePressed(evt);
            }
        });

        //Bottone (JButton) per le richieste di amicizia
        AddFriendButton.setFont(new Font("Tahoma", 1, 18)); // NOI18N
        AddFriendButton.setText("AGGIUNGI AMICO");
        
        /*Il bottone per le richieste di amicizia invoca il metodo
          AddFriendMousePressed quando viene cliccato
        */
        AddFriendButton.addMouseListener(new MouseAdapter() 
        {
            @Override
            public void mouseClicked(MouseEvent evt) 
            {
                AddFriendButtonMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout UserAreaLayout = new GroupLayout(UserArea);
        UserArea.setLayout(UserAreaLayout);
        UserAreaLayout.setHorizontalGroup(
            UserAreaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(UserAreaLayout.createSequentialGroup()
                .addGroup(UserAreaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(UserAreaLayout.createSequentialGroup()
                        .addGap(30, 30, 30)
                        .addGroup(UserAreaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(Username)
                            .addComponent(Password))
                        .addGap(15, 15, 15))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, UserAreaLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(RegisterButton, javax.swing.GroupLayout.PREFERRED_SIZE, 157, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addGroup(UserAreaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(UserAreaLayout.createSequentialGroup()
                        .addComponent(LoginButton, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(LogoutButton)
                        .addContainerGap())
                    .addComponent(UsernameField)
                    .addComponent(PasswordField)))
            .addGroup(UserAreaLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(AddFriendButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        UserAreaLayout.setVerticalGroup(
            UserAreaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(UserAreaLayout.createSequentialGroup()
                .addGroup(UserAreaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(UserAreaLayout.createSequentialGroup()
                        .addGap(5, 5, 5)
                        .addComponent(Username))
                    .addComponent(UsernameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(UserAreaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Password)
                    .addComponent(PasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(UserAreaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(LogoutButton)
                    .addComponent(LoginButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(RegisterButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(AddFriendButton)
                .addContainerGap())
        );

        Container.add(UserArea, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 120, 400, 160));

        TranslationPanel.setFont(new java.awt.Font("Calisto MT", 0, 14));
        TranslationPanel.setOpaque(false);
        TranslationPanel.setPreferredSize(new Dimension(2200, 210));

        OrderTextLabel.setFont(new java.awt.Font("Tahoma", 0, 24));
        OrderTextLabel.setText("Mi traduca, gentilmente, la parola");

        WordToTranslateArea.setFont(new Font("Calisto MT", 1, 24));
        WordToTranslateArea.setForeground(new Color(255, 255, 255));
        WordToTranslateArea.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        WordToTranslateArea.setText("Supercallifragili");
        WordToTranslateArea.setPreferredSize(new Dimension(400, 100));
        
        CentralTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        CentralTextField.setPreferredSize(new Dimension(400, 40));

        CentralButton.setFont(new java.awt.Font("Monospaced", 0, 18));
        CentralButton.setText("INVIA TRADUZIONE");
        CentralButton.setPreferredSize(new Dimension(400, 30));
        CentralButton.addMouseListener(new MouseAdapter() 
        {
            @Override
            public void mouseClicked(MouseEvent evt) {
                try 
                {
                    CentralButtonMouseClicked(evt);
                } 
                catch (IOException ex) 
                {
                    Logger.getLogger(MainClassClient.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        
        TranslationPanel.add(OrderTextLabel);
        TranslationPanel.add(WordToTranslateArea);
        TranslationPanel.add(CentralTextField);


        ScoreButton.setText("MOSTRA IL TUO PUNTEGGIO");
        ScoreButton.setPreferredSize(new Dimension(200, 30));
        ScoreButton.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent evt)
            {
                ScoreButtonMouseClicked(evt);
            }
        });


        ScoreTextField.setHorizontalAlignment(JTextField.CENTER);
        ScoreTextField.setPreferredSize(new java.awt.Dimension(190, 30));

        Advertising.setEditable(false);
        Advertising.setBackground(new java.awt.Color(0, 0, 0));
        Advertising.setColumns(20);
        Advertising.setRows(5);
        Advertising.setFont(new java.awt.Font("Monospaced", 0, 15));
        Advertising.setForeground(new java.awt.Color(0, 255, 0));
        Advertising.setLineWrap(true);
        Advertising.setWrapStyleWord(true);
        Advertising.setPreferredSize(new java.awt.Dimension(400, 200));
        Advertising.addMouseListener(new MouseAdapter() 
        {
            @Override
            public void mousePressed(MouseEvent evt) 
            {
                AdvertisingMousePressed(evt);
            }
        });
        
        TranslationPanel.add(CentralButton);
        TranslationPanel.add(ScoreButton);
        TranslationPanel.add(ScoreTextField);
        TranslationPanel.add(Advertising);

        Container.add(TranslationPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 320, 430, 470));

        TITLE.setFont(new java.awt.Font("Tahoma", 1, 54));
        TITLE.setForeground(new Color(255,150,50));
        TITLE.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        TITLE.setText("WORD QUIZZLE");
        TITLE.setToolTipText("");
        Container.add(TITLE, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 10, 570, 80));

        OnlinePanel.setForeground(new java.awt.Color(0, 255, 0));
        OnlinePanel.setFont(new java.awt.Font("Tahoma", 0, 14));
        OnlinePanel.setOpaque(false);
        OnlinePanel.setPreferredSize(new java.awt.Dimension(64, 90));

        TrafficLight.setFont(new java.awt.Font("Tahoma", 0, 10));
        TrafficLight.setIcon(new javax.swing.ImageIcon(getClass().getResource("/WordQuizzleClient/Offline.png"))); // NOI18N
        TrafficLight.setToolTipText("");

        OnlineAsLabel.setFont(new java.awt.Font("Tahoma", 1, 15));
        OnlineAsLabel.setForeground(new java.awt.Color(255, 0, 0));
        OnlineAsLabel.setVerticalAlignment(SwingConstants.TOP);

        OnlineNameLabel.setFont(new java.awt.Font("Tahoma", 1, 14));
        OnlineNameLabel.setForeground(new java.awt.Color(0, 255, 0));
        OnlineNameLabel.setHorizontalAlignment(SwingConstants.CENTER);

        ChallengeCheckLabel.setFont(new java.awt.Font("Tahoma", 0, 18));
        ChallengeCheckLabel.setForeground(new java.awt.Color(255, 127, 0));
        ChallengeCheckLabel.setText("Nessuna sfida in corso");

        OpponentName.setFont(new java.awt.Font("Tahoma", 1, 14));
        OpponentName.setForeground(new Color(177, 3, 255));
        OpponentName.setText("OpponentName");
        OpponentName.setVisible(false);

        RejectButton.setBackground(new Color(102, 102, 102));
        RejectButton.setFont(new Font("Tahoma", 1, 12)); 
        RejectButton.setForeground(new Color(255, 0, 0));
        RejectButton.setText("RIFIUTA");
        RejectButton.setVisible(false);
        RejectButton.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e) 
            {
               RejectButtonMousePressed(e);
            }
        });

        AcceptButton.setBackground(new Color(204, 204, 204));
        AcceptButton.setFont(new Font("Tahoma", 1, 12)); 
        AcceptButton.setForeground(new Color(0, 255, 0));
        AcceptButton.setText("ACCETTA");
        AcceptButton.setVisible(false);
        AcceptButton.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e) 
            {
               AcceptButtonMousePressed(e);
            }
        });
            
        javax.swing.GroupLayout OnlinePanelLayout = new javax.swing.GroupLayout(OnlinePanel);
        OnlinePanel.setLayout(OnlinePanelLayout);
        OnlinePanelLayout.setHorizontalGroup(
            OnlinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(OnlinePanelLayout.createSequentialGroup()
                .addGroup(OnlinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(OnlineAsLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(OnlineNameLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(ChallengeCheckLabel, javax.swing.GroupLayout.Alignment.CENTER, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(OpponentName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(OnlinePanelLayout.createSequentialGroup()
                        .addGap(46, 46, 46)
                        .addGroup(OnlinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(OnlinePanelLayout.createSequentialGroup()
                                .addComponent(TrafficLight)
                                .addGap(17, 17, 17))
                            .addGroup(OnlinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(RejectButton, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(AcceptButton)))
                        .addGap(0, 0, Short.MAX_VALUE)))));
        
        
        OnlinePanelLayout.setVerticalGroup(
            OnlinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(OnlinePanelLayout.createSequentialGroup()
                .addComponent(TrafficLight, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(OnlineAsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(OnlineNameLabel)
                .addGap(35, 35, 35)
                .addComponent(ChallengeCheckLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(OpponentName, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(AcceptButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(RejectButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 29, Short.MAX_VALUE)));

        Container.add(OnlinePanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 370, 180, 400));

        ListFunctionPanel.setOpaque(false);

        ListPlayerFunction.setFont(new java.awt.Font("Tahoma", 1, 14));
        ListPlayerFunction.setForeground(new java.awt.Color(0, 193, 25));
        ListPlayerFunction.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        ListPlayerFunction.setPreferredSize(new java.awt.Dimension(141, 20));
        ListPlayerFunction.setText("");
        
        OnlineList.setEditable(false);
        OnlineList.setColumns(30);
        OnlineList.setFont(new java.awt.Font("Monospaced", 1, 12));
        OnlineList.setLineWrap(true);
        OnlineList.setRows(8);
        OnlineList.setWrapStyleWord(true);

        ListPanel.setViewportView(OnlineList);

        ListFunctionPanel.add(ListPanel);
        ListFunctionPanel.add(ListPlayerFunction);

        ScoreTableButton.setText("CLASSIFICA");
        ScoreTableButton.setPreferredSize(new java.awt.Dimension(140, 25));
        ScoreTableButton.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent evt) 
            {
                ScoreTableButtonMouseClicked(evt);
            }
        });
            
        ListFunctionPanel.add(ScoreTableButton);

        FriendListButton.setText("LISTA AMICI");
        FriendListButton.setPreferredSize(new java.awt.Dimension(140, 25));
        FriendListButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) 
            {
                FriendListButtonMousePressed(evt);
            }
        });

        ListFunctionPanel.add(FriendListButton);

        Container.add(ListFunctionPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 90, 240, 230));

        Background.setFont(new Font("Tahoma", 1, 48));
        Background.setIcon(new javax.swing.ImageIcon(getClass().getResource("/WordQuizzleClient/Background.png")));
        Container.add(Background, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 800, -1));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(Container, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(Container, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }

    /*
        @METHOD SuccessMessage
        @OVERVIEW Metodo per visualizzare un messaggio che notifica l'esito positivo
        di un operazione all'interno dell'area di testo principale
    
        @PAR txt Stringa che rappresenta il messaggio da visualizzare
    */
    public void SuccessMessage(String txt)
    {
        Advertising.setForeground(Color.green);
        Advertising.setText(txt);
    }
    
    /*
        @METHOD ErrorMessage
        @OVERVIEW Metodo per visualizzare un messaggio di errore
        all'interno dell'area di testo principale
    
        @PAR txt Stringa che rappresenta il messaggio da visualizzare
    */
    public void ErrorMessage(String txt)
    {
        Advertising.setForeground(Color.red);
        Advertising.setText(txt);
    }
    
    /*
        @METHOD ResetTextContainer
        @OVERVIEW Metodo che resetta il contenuto di un area di testo
    
        @PAR A Area di testo (JTextArea) da resettare
    */
    public void ResetTextContainer(JTextArea A)
    {
        A.setText("");
    }
    
    /*
        @METHOD ResetTextContainer
        @OVERVIEW Metodo che resetta il contenuto di un campo testuale
    
        @PAR F Campo testuale(JTextField) da resettare
    */
    public void ResetTextField(JTextField F)
    {
        F.setText("");
    }
    
    /*
        @METHOD ResetAllText
        @OVERVIEW Metodo che resetta il contenuto di tutte le aree di testo
        e di tutti i campi testuali
    */
    public void ResetAllText()
    {
        Advertising.setText("");
        UsernameField.setText("");
        PasswordField.setText("");
        OnlineList.setText("");
        OnlineAsLabel.setText("");
        OnlineNameLabel.setText("");
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
    public void SendMessage(ByteBuffer buffer, String text) throws IOException
    {
        
        buffer.clear();
            
        byte[] message = text.getBytes();

        buffer.put(message);

        buffer.flip();

        while(buffer.hasRemaining())
        {
            client.write(buffer);
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
    public String ReceiveMessage(ByteBuffer buffer) throws IOException
    {
        buffer.clear();
        
        client.read(buffer);

        String content = new String(buffer.array()).trim();
        
        return content;
    }
    
    /*
        @METHOD DisconnectionMessage
        @OVERVIEW Metodo per inviare un messaggio di disconnessione al server 
        per disconnettere il client (non è un metodo specifico per il logout)
    
        @PAR buffer ByteBuffer mediante cui viene inviato il messaggio
    
        @THROWS IOException nel caso si verifichi un errore durante l'invio del messaggio
    */
    public void DisconnectionMessage(ByteBuffer buffer) throws IOException
    {
        CleanBuffer(buffer);
        SendMessage(buffer,"DISCONNECT");
    }
    
        /*
        @METHOD DisconnectUserMessage
        @OVERVIEW Metodo per inviare un messaggio di disconnessione specifico
        per il logout al server 
    
        @PAR buffer ByteBuffer mediante cui viene inviato il messaggio
        @PAR nick NickName dell'utente che effettua il logout
    
        @THROWS IOException nel caso si verifichi un errore durante l'invio del messaggio
    */
    public void DisconnectUserMessage(ByteBuffer buffer,String nick) throws IOException
    {
        CleanBuffer(buffer);
        SendMessage(buffer,"DISCONNECTUSER--"+nick);
    }
    
    /*
        @METHOD OKMessage
        @OVERVIEW Metodo per inviare un messaggio di avvenuta ricezione (ACK)
        al server
    
        @PAR buffer ByteBuffer mediante cui viene inviato il messaggio
    
        @THROWS IOException nel caso si verifichi un errore durante l'invio del messaggio
    */
    public void OkMessage(ByteBuffer buffer) throws IOException
    {
        CleanBuffer(buffer);
        SendMessage(buffer,"OK");
    }
   
    /*
        @METHOD EntranceMessage
        @OVERVIEW Metodo per inviare una richiesta di login al server
    
        @PAR buffer ByteBuffer mediante cui viene inviata la richiesta
        @PAR nickname NickName dell'utente che effettua il login
        @PAR password Password dell'utente che effettua il login
    
        @THROWS IOException nel caso si verifichi un errore durante l'invio del messaggio
    */
    public void EntranceMessage(ByteBuffer buffer,String nickname,String password) throws IOException
    {
        CleanBuffer(buffer);
        SendMessage(buffer,"ENTRANCE--"+nickname+"--"+password);
    }
    
    /*
        @METHOD FriendListMessage
        @OVERVIEW Metodo per inviare una richiesta di recupero della lista amici
        al server
    
        @PAR buffer ByteBuffer mediante cui viene inviata la richiesta
        @PAR nickname NickName dell'utente che vuole recuperare la lista dei suoi amici
    
        @THROWS IOException nel caso si verifichi un errore durante l'invio del messaggio
    */
    public void FriendListMessage(ByteBuffer buffer,String nickname) throws IOException
    {
        CleanBuffer(buffer);
        SendMessage(buffer,"FRIENDLIST--"+nickname);
    }
    
    /*
        @METHOD FriendRequest
        @OVERVIEW Metodo per inviare una richiesta di amicizia ad un altro utente
        mediante il server
    
          @PAR buffer ByteBuffer mediante cui viene inviata la richiesta
        @PAR nickname NickName dell'utente che invia la richiesta
        @PAR friend NickName dell'utente a cui è destinata la richiesta
    
        @THROWS IOException nel caso si verifichi un errore durante l'invio del messaggio
    */
    public void FriendRequest(ByteBuffer buffer,String nickname,String friend) throws IOException
    {
        CleanBuffer(buffer);
        SendMessage(buffer,"ADDFRIEND--"+nickname+"--"+friend);
    }
    
    /*
        @METHOD SendChallengeRequest
        @OVERVIEW Metodo per inviare una richiesta di sfida ad un altro utente
        mediante il server
    
        @PAR buffer ByteBuffer mediante cui viene inviata la richiesta
        @PAR nickname NickName dell'utente che invia la richiesta
        @PAR friend NickName dell'utente a cui è destinata la richiesta
    
        @THROWS IOException nel caso si verifichi un errore durante l'invio del messaggio
    */
    public void SendChallengeRequest(ByteBuffer buffer,String nickname,String friend) throws IOException
    {
        CleanBuffer(buffer);
        SendMessage(buffer,"CHALLENGE--"+nickname+"--"+friend);
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
        @METHOD CloseUDPConnection
        @OVERVIEW Metodo di chiusura della connessione UDP precedentemente aperta
    */
    public void CloseUDPConnection()
    {
        if(ClientUDP != null)
        {
            ClientUDP.close();
        }
        
        ClientUDP = null;
    }
    
    /*
        @METHOD UDPShutDown
        @OVERVIEW Metodo di chiusura del listener UDP e della connessione UDP
    */
    private void UDPShutDown()
    { 
       Listener.ShutDown();
        
       if(ClientUDP != null && client != null)
       {
            if(!ClientUDP.isClosed())
            {
                ClientUDP.close();
            }
            else
            {
                ClientUDP = null;
            }
        }
    }
    
    /*
        @METHOD Connect
        @OVERVIEW Metodo che connette l'utente aprendo una connessione TCP ed una connessione UDP
        allo stesso indirizzo della connessione TCP ed inizializzando infine il listener UDP
    
        @THROWS IOException nel caso si verifichino errori nell'apertura della connessione
    */
    public void Connect()
    {        
        if(client == null || !client.isOpen())
        {   
            try 
            {
                client = SocketChannel.open(clientaddress);
                ClientUDP = new DatagramSocket(client.getLocalAddress());
            
                Listener = new UDPListener(this,ClientUDP);  
                StartListener = Executors.newSingleThreadExecutor();
                
                StartListener.execute(Listener);
            } 
            catch (IOException ex) 
            {
                ErrorMessage("Errore nell'apertura della connessione verso Word Quizzle");
                
                tcp_port = DefaultTCP_Port;
           
                UDPShutDown();
                LoginMode();
                TrafficLight();
            }
        }
    }
    
    /*
        @METHOD Disconnect
        @OVERVIEW Metodo che effettua la disconnessione del client
        o il logout, in base al caso,aggiornando le variabili d'istanza
    */
    public void Disconnect()
    {
         if(CheckConnection())
        {
            try 
            {
                if(client != null)
                {
                    System.out.println("CHIUSURA DELLA CONNESSIONE");
                    
                    if(name == null)
                    {
                        DisconnectionMessage(ConnectionBuffer);
                        TrafficLight();
                    }
                    else
                    {
                        DisconnectUserMessage(ConnectionBuffer,name);
                        TrafficLight();
                    }
                    ReceiveMessage(ConnectionBuffer);
                    
                    client.close();
                }
                
                name = "";
                password = "";

                SuccessMessage("Ti sei disconnesso dal servizio");
            } 
            catch (IOException ex) 
            {
                System.out.println("ERRORE NELLA CHIUSURA DELLA CONNESSIONE");
            }
            
            TrafficLight();
            UDPShutDown();
            
        }
    }

    /*
        @METHOD CheckConnection
        @OVERVIEW Metodo che controlla se vi è una connessione TCP aperta o meno
        per l'utilizzo di alcuni bottoni
    */
    public boolean CheckConnection()
    {
        if(client != null)
        {
            if(client.isOpen())
            {
                return client.isConnected();
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }
    
    /*
        @METHOD TrafficLight
        @OVERVIEW Metodo che aggiorna l'icona del semaforo in base allo stato
        della connessione, rendendola verde se si è connessi o rossa altrimenti
    */
    public void TrafficLight()
    {
        if(!CheckConnection())
        {
              TrafficLight.setIcon(new javax.swing.ImageIcon(getClass().getResource("/WordQuizzleClient/Offline.png")));
        }
        else
        {
              TrafficLight.setIcon(new javax.swing.ImageIcon(getClass().getResource("/WordQuizzleClient/Online.png")));
        }      
    }
    
    /*
        @METHOD LoginMode
        @OVERVIEW Metodo che setta il client in modalità login impostando le varie
        aree di testo nel modo opportuno
    */
    public void LoginMode()
    {
        AcceptButton.setVisible(false);
        RejectButton.setVisible(false);
        ListPlayerFunction.setText("");
        OrderTextLabel.setText("Si registri o effettui, gentilmente, il login");
        WordToTranslateArea.setText("E scelga il suo avversario");
        CentralButton.setText("SFIDA");
        OpponentName.setText("");
        Word = "";
        mode = "LOGIN";
        
        UDPChallengeRequest = null;
    }
    
    /*
        @METHOD ChallengeMode
        @OVERVIEW Metodo che setta il client in modalità sfida impostando le varie
        aree di testo nel modo opportuno ed inizializzando un timer di due minuti
    */
    public void ChallengeMode(String opponent)
    {
        AcceptButton.setVisible(false);
        RejectButton.setVisible(false);
        OrderTextLabel.setText("Mi traduca, gentilmente, la parola: ");
        CentralButton.setText("INVIA TRADUZIONE");
        WordToTranslateArea.setText("-");
        
        ChallengeCheckLabel.setText("SFIDA CONTRO");
        ChallengeCheckLabel.setVisible(true);
        OpponentName.setText(opponent);
        OpponentName.setVisible(true);
        
        mode = "CHALLENGE";
        
        Listener.ArrivalHalt();
        
        StartTimer(120);
    }
   
    /*
        @METHOD SendFriendRequest
        @OVERVIEW Metodo che incapsula l'invio di una richiesta di amicizia e
        la ricezione della risposta con gestione del risultato della richiesta
        in base al messaggio ricevuto
    
        @PAR friend Utente a cui inviare la richiesta
    */
    public void SendFriendRequest(String friend)
    {           
        try 
        {
            FriendRequest(ConnectionBuffer,name,friend);
            
            CleanBuffer(ConnectionBuffer);
            
            String rcv = ReceiveMessage(ConnectionBuffer);
            
            switch(rcv)
            {
                case "ALREADYFRIENDS":
                   ErrorMessage("Tu e "+friend+" siete già amici");
                   break;
                 case "IOERROR":
                   ErrorMessage("Impossibile completare la richiesta di amicizia");
                   break;
                 case "NOSUCHPLAYER":
                    ErrorMessage("Il giocatore "+friend+" non è registrato al servizio");
                    break;
                 case "NEWFRIENDOK":
                    SuccessMessage("Tu e "+friend+" ora siete amici!");
                    break;
                 default:
                     break;
            }
            
            CleanBuffer(ConnectionBuffer);
        } 
        catch (IOException ex) 
        {
            ErrorMessage("Impossibile completare la richiesta di amicizia");
        }
    }
    
    /*
        @METHOD RegisterUser
        @OVERVIEW Metodo che incapsula la registrazione dell'utente mediante
        metodo remoto, individuando gli oggetti in remoto e gestendo, a seconda
        del risultato ottenuto, la risposta del client
    
        @THROWS RemoteException nel caso si verifichino errori durante la connessione
                al servizio in remoto
                
                NotBoundException se la porta di connessione non è valida
    */
    public void RegisterUser()
    { 
        try 
        {
            //Reperimento del registro in remoto.
            Registry reg = LocateRegistry.getRegistry(remoteport);
            
            //Ricerca dell'oggetto remoto.
            RMObject = reg.lookup("WORD_QUIZZLE_SERVICE");
            
            //Inizializzazione dell'oggetto remoto.
            RequestManager = (RemoteRequests) RMObject;
            
            password = PasswordField.getText();
            name = UsernameField.getText();
            
            int result;

            result = RequestManager.Registra_utente(password, name);
            
            switch (result) 
            {
                case -1:
                    ErrorMessage("Password vuota, registrazione non valida!");
                    break;
                case 1:
                    SuccessMessage("Registrazione avvenuta correttamente!");
                    break;
                case -2:
                    ErrorMessage("L'utente "+name+" è già registrato al servizio");
                    break;
                default:
                    break;
            }
        } 
        catch (RemoteException ex) 
        {
            ErrorMessage("Errore di connessione al servizio di registrazione in remoto");
            remoteport = DefaultRemotePort;
        } 
        catch (NotBoundException ex) 
        {
            ErrorMessage("Porta di connessione non valida");
            remoteport = DefaultRemotePort;
        }
        
        ResetTextField(PasswordField);
        ResetTextField(UsernameField);
    }
    
    /*
        @METHOD Login
        @OVERVIEW Metodo che incapsula il login di un utente con gestione degli
        errori e della risposta del client in base a quanto ricevuto
    
        @THROWS IOException nel caso si verifichino errori durante il login
    */
    public void Login()
    {
        if(!CheckConnection() && (mode.equals("LOGIN")))
            {
            if(UsernameField.getText().contains("--") || PasswordField.getText().contains(("--")))
            {
                ErrorMessage("Il carattere '--' è riservato, non utilizzarlo per le credenziali");
                Disconnect();
            }
            else if(UsernameField.getText().isEmpty() || PasswordField.getText().isEmpty())
            {
                ErrorMessage("Password o Username vuoti, quindi non validi");
                Disconnect();
            }
            else
            {
                Connect();
                try {

                    name = UsernameField.getText().trim();
                    password = PasswordField.getText().trim();

                    String result = ("LOGIN--"+name+"--"+password);

                    SendMessage(ConnectionBuffer,result);

                    CleanBuffer(ConnectionBuffer);

                    String x = ReceiveMessage(ConnectionBuffer);

                    switch (x)
                    {
                        case "NOSUCHPLAYER":
                            CleanBuffer(ConnectionBuffer);
                            Disconnect();
                            ErrorMessage("Il giocatore "+name+" non è registrato al servizio");
                            break;
                        case "PASSWORDERROR":
                            CleanBuffer(ConnectionBuffer);
                            Disconnect();
                            ErrorMessage("La password digitata non è valida");
                            break;
                        case "ALREADYLOGGED":
                            CleanBuffer(ConnectionBuffer);
                            Disconnect();
                            ErrorMessage("L'utente è già connesso al servizio");
                            break;
                        default:
                            SuccessMessage("Login effettuato correttamente!");
                            OnlineAsLabel.setText("SEI CONNESSO COME");
                            OnlineNameLabel.setText(name);
                            CleanBuffer(ConnectionBuffer);
                            break;
                    }

                    ConnectionBuffer.clear();
                    ResetTextField(PasswordField);
                    ResetTextField(UsernameField);
                } 
                catch (IOException ex) 
                {
                     ErrorMessage("Impossibile aprire la connessione verso il Server");
                }
            }
            TrafficLight();
        }  
    }
    
    /*
        @METHOD Logout
        @OVERVIEW Metodo che implementa il logout di un utente e la risposta del
        client in base al messaggio ricevuto dal server
    
        @THROWS NullPointerException se il nome utente è vuoto e quindi l'utente
                non ha effettuato il login
    
                IOException se si verificano errori durante il logout
    */
    public void Logout()
    {
        if(CheckConnection())
       {
        try
        {
            CleanBuffer(ConnectionBuffer);
            
            DisconnectUserMessage(ConnectionBuffer,name);
            ReceiveMessage(ConnectionBuffer);
            
            SuccessMessage("Ti sei disconnesso dal servizio");
            client.close();
        }
        catch(NullPointerException e)
        {
            ErrorMessage("Non hai effettuato il login");
        } 
        catch (IOException ex) 
        {
            ErrorMessage("Errore nel tentativo di logout");
        }
        UDPShutDown();
        TrafficLight();
       }
        
        LoginMode();
        ResetAllText();
    }
    
    /*
        @METHOD LeaveChallenge
        @OVERVIEW Metodo che implementa la gestione dell'abbandono di una partita
                  durante il suo svolgimento
    
        @THROWS IOException se si verificano errori durante l'abbandono della partita
    */
    public void LeaveChallenge()
    {
        try 
        {
            SendMessage(ConnectionBuffer,"LEAVECHALLENGE--"+name);
            CleanBuffer(ConnectionBuffer);
            
            String content = ReceiveMessage(ConnectionBuffer);
            
            ChallengeTimer.stop();
            
            EndGame(content);
        } 
        catch (IOException ex) 
        {
            ErrorMessage("Errore durante l'abbandono della partita");
            Disconnect();
        }
    }
    
    /*
        @METHOD EndGame
        @OVERVIEW Metodo che implementa l'invio dei risultati di una partita appena
                  conclusa e la dichiarazione del vincitore
    
        @PAR content Messaggio inviato dal server da cui vengono ricavati:
                     - Il motivo della terminazione della sfida (Timeout o Abbandono)
                     - Il vincitore della partita (DRAW nel caso di pareggio)
                     - Il punteggio del client
                     - Il punteggio dell'avversario
                     - Il numero di parole totali inviate ai due sfidanti
    */
    public void EndGame(String content)
    {
            String ChallengeResults[] =  content.split("--",5);
            
            String motivation = ChallengeResults[0];
            
            switch(motivation)
            {
                case("ENDCHALLENGE"):
                    String Winner = ChallengeResults[1];
                    
                    int Words = Integer.parseInt(ChallengeResults[4]);
                    
                    long YourScore = Long.parseLong(ChallengeResults[2]);
                    
                    long OpponentScore = Long.parseLong(ChallengeResults[3]);
                    
                    DeclareResult(Winner,Words,YourScore,OpponentScore);
                    
                    break;
                case("CHALLENGEQUITTED"):
                    String Quitter = ChallengeResults[1];
                    
                    int WordsTotal = Integer.parseInt(ChallengeResults[4]);
                    
                    long ScoreUntil = Long.parseLong(ChallengeResults[2]);
                    
                    long OpponentScoreUntil = Long.parseLong(ChallengeResults[3]);
                    
                    ChallengeQuitted(Quitter,WordsTotal,ScoreUntil,OpponentScoreUntil);
                    
                    break;        
        } 
          
        Listener.ResetArrival();    
        LoginMode();
    }
    
    /*
        @METHOD DeclareResult
        @OVERVIEW Metodo che stabilisce il risultato di una sfida nel caso questa
                  sia terminata correttamente
    
        @PAR Winnner Stringa che rappresenta il nome del vincitore
        @PAR WordsTotal Numero totale di parole inviate ai due client
        @PAR Score Punteggio ottenuto dal client
        @PAR ScoreOpponent Punteggio ottenuto dall'avversario
    */
    public void DeclareResult(String Winner,int WordsTotal,long Score,long ScoreOpponent)
    {
        SuccessMessage("Parole tradotte correttamente: "+RightAnswers+"\n");
        
        Advertising.append("Parole tradotte erroneamente: "+(TranslatedWords - RightAnswers)+"\n");
        Advertising.append("Parole non tradotte: "+(WordsTotal-TranslatedWords)+"\n");
        
        if(Winner.equals(name))
        {
            Advertising.append("Punteggio: "+(Score-3)+"\n");
            Advertising.append("Punteggio dell'avversario: "+ScoreOpponent+"\n");
            Advertising.append("\nCongratulazioni, hai vinto! Ti assegnamo tre punti extra");
        }
        else if(Winner.equals("DRAW"))
        {
            Advertising.append("Punteggio: "+Score+"\n");
            Advertising.append("Punteggio dell'avversario: "+ScoreOpponent+"\n");
            Advertising.append("\nTu ed il tuo avversario avete pareggiato, sfidatevi un'altra volta");
        }
        else 
        {
            Advertising.append("Punteggio: "+Score+"\n");
            Advertising.append("Punteggio dell'avversario: "+(ScoreOpponent-3)+"\n");
            Advertising.append("Ci dispiace,hai perso, la prossima volta sarai più fortunato");
        }
    }
    
    /*
        @METHOD ChallengeQuitted
        @OVERVIEW Metodo che stabilisce il risultato di una sfida nel caso questa
                  sia terminata per abbandono
    
        @PAR Quitter Stringa che rappresenta il nome dell'utente che ha abbandonato la sfida
        @PAR WordsTotal Numero totale di parole inviate ai due client
        @PAR Score Punteggio ottenuto dal client
        @PAR ScoreOpponent Punteggio ottenuto dall'avversario
    */
    public void ChallengeQuitted(String Quitter,int WordsTotal,long Score,long ScoreOpponent)
    {
        if(!Quitter.equals(name))
        {
            ErrorMessage("Parole tradotte correttamente: "+RightAnswers+"\n");
        
            Advertising.append("Parole tradotte erroneamente: "+(TranslatedWords - RightAnswers)+"\n");
            Advertising.append("Punteggio: "+Score+"\n");
            Advertising.append("Il tuo avversario ha lasciato la sfida prima dello scadere del tempo, quindi al tuo punteggio verranno sommati tre punti extra");
        }
        else
        {
            ErrorMessage("Ci dispiace che hai abbandonato la sfida, la vittoria viene data all'avversario, e con essa i punti conseguiti");
        }
    }
    
    /*
        @METHOD ViewScore
        @OVERVIEW Metodo che incapsula l'invio di una richiesta di visione punteggio
                  e la gestione della risposta del client in base al messaggio ricevuto
                  dal server
    
        @THROWS IOException nel caso si verifichino errori durante 
                il soddisfacimento della richiesta
    */
    public void ViewScore()
    {
        if(CheckConnection())
       {
           try 
           {
               SendMessage(ConnectionBuffer,"SCORE--"+name);
               
               CleanBuffer(ConnectionBuffer);
               
               String score = ReceiveMessage(ConnectionBuffer);
               
               ScoreTextField.setText(score);
           } 
           catch (IOException ex) 
           {
               ErrorMessage("Impossibile completare la richiesta");
           }
       }   
    }
    
     /*
        @METHOD GetFriendList
        @OVERVIEW Metodo che incapsula l'invio di una richiesta di visione lista amici
                  e la gestione della risposta del client in base al messaggio ricevuto
                  dal server
    
        @THROWS IOException nel caso si verifichino errori durante 
                il soddisfacimento della richiesta
    */
    public void GetFriendList()
    {
         if(CheckConnection())
        {
            ListPlayerFunction.setForeground(Color.green);
            ListPlayerFunction.setText("LISTA DEGLI AMICI");
            try 
            {   
               FriendListMessage(ConnectionBuffer,name);
               
               CleanBuffer(ConnectionBuffer);
               
               String content = ReceiveMessage(ConnectionBuffer);
               
                switch (content) 
                {    
                    case "FILENOTFOUND":
                        ErrorMessage("Impossibile trovare la lista degli amici");
                        break;
                    case "PARSEXCEPTION":
                        ErrorMessage("Errore nella scansione della lista degli amici");
                        break;
                    default:
                        OnlineList.setText(content);
                        break;
                }
            } 
            catch (IOException ex) 
            {
               ErrorMessage("Impossibile visualizzare la lista degli amici");
            }  
        }
    }
    
     /*
        @METHOD ViewScore
        @OVERVIEW Metodo che incapsula l'invio di una richiesta di visione classifica
                  e la gestione della risposta del client in base al messaggio ricevuto
                  dal server
    
        @THROWS IOException nel caso si verifichino errori durante 
                il soddisfacimento della richiesta
    */
    public void ViewScoreTable()
    {
        if(CheckConnection())
        {
            try 
            {
                OnlineList.setText("");
                
                String rank;
                
                ListPlayerFunction.setForeground(new Color(255,128,0));
                ListPlayerFunction.setText("CLASSIFICA");
                
                SendMessage(ConnectionBuffer,"RANK--"+name);
                
                CleanBuffer(ConnectionBuffer);
                
                rank = ReceiveMessage(ConnectionBuffer);
               
                OnlineList.setText(rank);
            } 
            catch (IOException ex) 
            {
                ErrorMessage("Impossibile completare la richiesta");
            }
        }
        
    }
    
    /*
        @METHOD ChallengeRequest
    
        @OVERVIEW Metodo che incapsula l'invio di una richiesta di sfida
                  e la gestione della risposta del client in base al messaggio ricevuto
                  dal server
    
        @THROWS IOException nel caso si verifichino errori durante 
                il soddisfacimento della richiesta
    */
    public void ChallengeRequest(String friend)
    {
        if(!friend.isEmpty())
        {
            try
            {
                SendChallengeRequest(ConnectionBuffer,name,friend);
                
                ConnectionBuffer.compact();
                
                CleanBuffer(ConnectionBuffer);
                
                String response;
                
                response = ReceiveMessage(ConnectionBuffer);

                switch(response)
                {
                    case "CHALLENGESENT":
                        SuccessMessage("Richiesta di sfida inviata, in attesa di risposta...");

                        SendMessage(ConnectionBuffer,"WAITING--"+name+"--"+friend);
                        CleanBuffer(ConnectionBuffer);
                        
                        Waiter = new TCPWaiter(client,this,ConnectionBuffer);
                        
                        Waiter.SetChallengerName(name);
                        Waiter.SetOpponentName(friend);
                        
                        StartWaiter = Executors.newSingleThreadExecutor();
                        StartWaiter.execute(Waiter);

                        break;
                    case "ISNOTFRIENDOFYOURS":
                        ErrorMessage("Il giocatore "+friend+" non è tuo amico");
                        break;
                    case "NOSUCHPLAYERONLINE":
                        ErrorMessage("Il giocatore "+friend+" non è disponibile oppure non è registrato al servizio");
                        break;
                }
            }
            catch (IOException ex) 
            {
                ErrorMessage("Impossibile completare la richiesta");
                System.out.println("IO");
                LoginMode();
            }
        }
    }
    
    /*
        @METHOD Timeout
        @OVERVIEW Metodo che implementa l'invio, allo scadere del tempo
                  fornito ai giocatori per la traduzione dei vocaboli
                  di un messaggio che richiede di concludere la partita
                  e stabilire un verdetto
    
        @THROWS IOException se si verifica un errore durante l'invio del messaggio
    */
    public void Timeout()
    {
        try 
        {
            SendMessage(ConnectionBuffer,"TIMEOUT--"+name);
            CleanBuffer(ConnectionBuffer);
            
            String content = ReceiveMessage(ConnectionBuffer);
            
            EndGame(content);
        } 
        catch (IOException ex) 
        {
            Disconnect();
            ErrorMessage("Si è verificato un errore e sei stato disconnesso");
        }
        
    }

    /*
        @METHOD PrintBuffer
        @OVERVIEW Metodo utilizzato durante il debug per stampare il contenuto
                  del buffer del client
    
        @PAR buf ByteBuffer di cui stampare il messaggio contenuto
    */
    public static void PrintBuffer(ByteBuffer buf)//DEBUG
    {
        String x = new String(buf.array()).trim();
        System.out.println("CLIENT: "+x);
    }    
    
    /*
        @METHOD HandleChallengeLaunched
        @OVERVIEW Metodo di gestione dell'arrivo di una richiesta di sfida
                  via UDP che setta le aree di testo in modo opportuno
                  e mette il cliente parzialmente in modalità CHALLENGE
    */
    public void HandleChallengeLaunched(String opponentname,DatagramPacket Packet)
    {
        opponent = opponentname;
        
        ChallengeCheckLabel.setText("SFIDA LANCIATA DA");
        OpponentName.setVisible(true);
        OpponentName.setText(opponentname);
        AcceptButton.setVisible(true);
        RejectButton.setVisible(true);
        
        UDPChallengeRequest = Packet;//Passiamo al client il pacchetto ai fini della sua gestione
        
        /*Non invochiamo il metodo ChallengeMode() perché scomparirebbero i bottoni 
        con i titoli "ACCETTA" e "RIFIUTA" e perchè effettivamente la sfida non è
        ancora iniziata ed ogni pacchetto UDP, in questa modalità, viene ignorato*/
        mode = "CHALLENGE";
    }
    
     /*
        @METHOD HandleTranslationReceived
        @OVERVIEW Metodo di gestione dell'arrivo di una parola da tradurre
                  via TCP he setta le aree di testo in modo opportuno
    */
    public void HandleTranslationReceived(String WordToTranslate,int WordsTranslated,int WordsTotal)
    {
        WordToTranslateArea.setText(WordToTranslate);
        Word = WordToTranslate;
        
        SuccessMessage("PAROLA "+(WordsTranslated+1)+"/"+WordsTotal);
    }

    /*
        @METHOD RejectChallenge
        @OVERVIEW Metodo per rifiutare una richiesta di sfida in arrivo
                  che setta le aree di testo in modo opportuno, rende invisibili
                  i pulsanti di accettazione e rifiuto sfida, notifica il server
                  del rifiuto avvenuto e mette il client in modalità LOGIN
    
        @THROWS IOException se si verificano errori durante il soddisfacimento
                della richiesta
    */
    public void RejectChallenge()
    {
            ErrorMessage("Ci dispiace che tu abbia rifiutato la sfida");
    
            ChallengeCheckLabel.setText("Nessuna sfida in corso");
            OpponentName.setVisible(false);
            OpponentName.setText("");
            AcceptButton.setVisible(false);
            RejectButton.setVisible(false);

            CleanBuffer(ConnectionBuffer);
            
            byte[] m;
            m = ("CHALLENGEREJECT--"+name+"").getBytes();
            UDPChallengeRequest.setData(m);
            
            Listener.ResetArrival();
        try 
        {
            ClientUDP.send(UDPChallengeRequest);
            Listener.ResetArrival();
            LoginMode();          
        } 
        catch (IOException ex) 
        {
            Disconnect();
            ErrorMessage("Si è verificato un errore durante l'invio della risposta, verrai disconnesso dal servizio");

        }

    }
    
      /*
        @METHOD AcceptChallenge
        @OVERVIEW Metodo per accettare una richiesta di sfida in arrivo
                  che setta le aree di testo in modo opportuno, rende invisibili
                  i pulsanti di accettazione e rifiuto sfida, notifica il server
                  dell'avvenuta accettazione e mette il client in modalità CHALLENGE
                  inizializzando il waiter per ricevere notifica sullo stato della sfida
    
        @THROWS IOException se si verificano errori durante il soddisfacimento
                della richiesta
    */
     public void AcceptChallenge()
    {
        try 
        {
            ClientUDP.send(UDPChallengeRequest);
        } 
        catch (IOException ex) 
        {
            Disconnect();
            ErrorMessage("Si è verificato un errore durante l'invio della risposta, verrai disconnesso dal servizio");
        }
        
        AcceptButton.setVisible(false);
        RejectButton.setVisible(false);  
        
        String friend = OpponentName.getText();
        
        //Originariamente la sfida è stata generata dall'avversario
        Waiter = new TCPWaiter(client,this,ConnectionBuffer);
        
        Waiter.SetChallengerName(name);
        Waiter.SetOpponentName(friend);
        
        StartWaiter = Executors.newSingleThreadExecutor();
        StartWaiter.execute(Waiter);   
        
        Listener.ArrivalHalt();
    }
     
    /*
        @METHOD StartTimer
        @OVERVIEW Metodo che inizializza il timer della sfida, inviando un messaggio
                  di TimeOut al server una volta che il tempo scade
     
        @THROW InterruptedException se durante l'intervallo di attesa del caricamento   
                delle parole da tradurre si verifica un'interruzione anomala
   
     */
    public void StartTimer(int TimeOut)
    {
        TimeChallenge = TimeOut;
        
        ActionListener ChallengeTimeOut = new ActionListener() 
        {
            @Override
            public void actionPerformed(ActionEvent e) 
            {
               if(TimeChallenge == 0)
               {
                   ChallengeTimer.stop();
                   SuccessMessage("TEMPO SCADUTO!");
                   
                   Timeout();
               }
               else
               {
                   TimeChallenge--;

                   Advertising.setForeground(Color.white);
                   Advertising.setText("TEMPO RIMASTO: "+TimeChallenge+" SECONDI");
               }
            }
        };
        
        ChallengeTimer = new Timer(1000,ChallengeTimeOut);
        ChallengeTimer.setInitialDelay(1000);
        
        try 
        {
            sleep(10000);//Tempo medio per il caricamento di dieci parole
        } 
        catch (InterruptedException ex) 
        {
            ChallengeTimer.stop();
            Disconnect();
        }
        
        ChallengeTimer.start();   
    }
    
    /*
        @METHOD SendTranslation
        @OVERVIEW Metodo per l'invio, quando il client è in modalità CHALLENGE,
                  di una traduzione
    
        @THROWS IOException nel caso si verifichino errori durante l'invio 
                della traduzione
    */
    public void SendTranslation() throws IOException
    {
        
        String content;

        TranslatedWords++;
        String Translation = CentralTextField.getText();

        String Message = "SENDTRANSLATION--"+name+"--"+Translation+"--"+TranslatedWords;
        SendMessage(ConnectionBuffer,Message);
        CleanBuffer(ConnectionBuffer);

        content = ReceiveMessage(ConnectionBuffer);

        System.out.println("CONTENT: "+content);

        if(content.contains("CORRECT!"))
        {
            SuccessMessage("Risposta corretta!");
            RightAnswers++;
        }
        else if(content.contains("WRONG!"))
        {
            ErrorMessage("Risposta errata!");
        }

        if(content.contains("CHALLENGEQUITTED--"))
        {
            ChallengeTimer.stop();

            EndGame(content);
        }
        else if(content.contains("WAITFORCHALLENGETOFINISH"))
        {
            SuccessMessage("Hai tradotto tutte le parole, attendi che la sfida sia finita");
        }
        else
        {
            String NewWords[]  = content.split("--",3);
            content = NewWords[1].trim();
            int NumberOfWords = Integer.parseInt(NewWords[2]);

            HandleTranslationReceived(content,TranslatedWords,NumberOfWords);
        }
    }
    
    /*
        @METHOD RegisterButtonMousePressed
        @OVERVIEW Metodo che gestisce il click del bottone intitolato "REGISTRATI"
    */
    private void RegisterButtonMousePressed(MouseEvent evt) 
    {
        RegisterUser();
    }

    /*
        @METHOD AdvertisingMousePressed
        @OVERVIEW Metodo che gestisce il click dell'area di testo principale resettandonla
    */
    private void AdvertisingMousePressed(MouseEvent evt) 
    {
        Advertising.setText("");
    }

    /*
        @METHOD LoginButtonMousePressed
        @OVERVIEW Metodo che gestisce il click del bottone intitolato "LOGIN"
    */
    private void LoginButtonMousePressed(MouseEvent evt) 
    {
        Login();
    }

    /*
        @METHOD WindowClosing
        @OVERVIEW Metodo che gestisce la chiusura della finestra
    */
    private void WindowClosing(WindowEvent evt) 
    {
       if(mode.equals("LOGIN"))
       {
           Logout();
           Disconnect();  
       }
       else
       {
           LeaveChallenge();
           Logout();
           Disconnect();
       }
       
    }

      /*
        @METHOD LogoutButtonMousePressed
        @OVERVIEW Metodo che gestisce il click del bottone intitolato "LOGOUT"
                  distinguendo in base alla modalità del client in modo da 
                  implementare il logout vero e proprio o l'abbandono di una sfida
    */
    private void LogoutButtonMousePressed(MouseEvent evt) 
    {
       if(mode.equals("LOGIN"))
       {
            Logout();
       }
       else
       {
           LeaveChallenge();
       }
    }
    
    /*
        @METHOD FriendListButtonMousePressed
        @OVERVIEW Metodo che gestisce il click del bottone intitolato "LISTA AMICI"
    */
    private void FriendListButtonMousePressed(MouseEvent evt) 
    {
        GetFriendList();
    }

    /*
        @METHOD ScoreTableButtonMousePressed
        @OVERVIEW Metodo che gestisce il click del bottone intitolato "CLASSIFICA"
    */
    private void ScoreTableButtonMouseClicked(MouseEvent evt) 
    {
        ViewScoreTable();
    }

    /*
        @METHOD AddFriendButtonMousePressed
        @OVERVIEW Metodo che gestisce il click del bottone intitolato "AGGIUNGI AMICO"
    */
    private void AddFriendButtonMouseClicked(MouseEvent evt) 
    {
        String friend = UsernameField.getText();
        
        SendFriendRequest(friend);
    }

    /*
        @METHOD ScoreButtonMousePressed
        @OVERVIEW Metodo che gestisce il click del bottone intitolato "MOSTRA PUNTEGGIO"
    */
    private void ScoreButtonMouseClicked(MouseEvent evt) 
    {
       ViewScore();
    }

    /*
        @METHOD CentralButtonMouseClicked
        @OVERVIEW Metodo che gestisce il click del bottone intitolato "SFIDA"
                  in modalità LOGIN e "INVIA TRADUZIONE" in modalità CHALLENGE
    */
    private void CentralButtonMouseClicked(MouseEvent evt) throws IOException 
    {
       if(CheckConnection())
       {
           if(CentralTextField != null && (!CentralTextField.getText().isEmpty()))
           {
            if(mode.equals("LOGIN"))
            {
               String friend = CentralTextField.getText();

               ChallengeRequest(friend);
               
               CleanBuffer(ConnectionBuffer);
            }
            else
            {
                SendTranslation();
            }
           }
       }
       else
       {
           ErrorMessage("Non sei connesso, non è possibile inviare la sfida");
       }
        ResetTextField(CentralTextField);
    }

    /*
        @METHOD RejectButtonMousePressed
        @OVERVIEW Metodo che gestisce il click del bottone intitolato "RIFIUTA"
    */
    public void RejectButtonMousePressed(MouseEvent e)
    {
        RejectChallenge();
    }
        
    /*
        @METHOD AcceptButtonMousePressed
        @OVERVIEW Metodo che gestisce il click del bottone intitolato "ACCETTA"
    */
    public void AcceptButtonMousePressed(MouseEvent e)
    {
       AcceptChallenge();
    }
    

    /*
        @METHOD main
        @OVERVIEW Metodo che implementa l'avvio di un client con GUI
    */
    public static void main(String args[])
    {
    	if(args.length == 0)
    	{
    		System.out.println("Immettere da riga di comando, nell'ordine, un numero di porta per la connessione TCP ed uno per la RMI");
    	}
    	
        try
        {
            //Leggiamo il nome dell'host da input
            tcp_port = Integer.parseInt(args[0]);
            remoteport = Integer.parseInt(args[1]);
        }
        catch(RuntimeException ex)
        {
            //Nel caso vi sia un errore, assegnamo al server la porta di default
            tcp_port = DefaultTCP_Port;
            remoteport = DefaultRemotePort;
        }
        
        MainClassClient c = new MainClassClient();
        c.setVisible(true);
    }
}

