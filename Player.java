package WQServer;

import java.io.Serializable;
import java.util.LinkedList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/*
PROGETTO FINALE DI LABORATORIO DI RETI A.A 2019-2020
NOME PROGETTO: WORD QUIZZLE

AUTORE: Enrico Tomasi
NUMERO MATRICOLA: 503527

OVERVIEW: Implementazione di un sistema di sfide di traduzione italiano-inglese
tra utenti registrati secondo paradigma client-server
 */

/*
    @CLASS Player
    @OVERVIEW Classe che implementa un generico giocatore di WordQuizzle e le
    sue caratteristiche
*/
public class Player implements Serializable
{
    private static final long serialVersionUID = -2025819022328641126L;
    private final String NickName;
    private final String Password;
    private int score;
    private LinkedList<String> Friends;
        
    public Player(String pw,String nck)
    {
        Password = pw;
        NickName = nck;
        score = 0;
        Friends = new LinkedList<String>();
    }
    
    /*
        @METHOD GetPassword
        @OVERVIEW Metodo getter che restituisce la password del giocatore
    
        @RETURNS Password del giocatore rappresentata da una stringa
    */
    public String GetPassword()
    {
        return this.Password;
    }
    
    /*
        @METHOD GetNick
        @OVERVIEW Medoto getter che restituisce il nickname del giocatore
    
        @RETURNS Nickname del giocatore rappresentato da una stringa
    */
    public String GetNick()
    {
        return this.NickName;
    }
    
    /*
        @METHOD SetScore
        @OVERVIEW Metodo setter che modifica il punteggio del giocatore
    */
    public void SetScore(int num)
    {
        score += num;
    }
    
    /*
        @METHOD GetScore
        @OVERVIEW Metodo getter che restituisce il punteggio del giocatore
    
        @RETURNS Punteggio del giocatore
    */
    public int GetScore()
    {
        return this.score;
    }
    
    /*
        @METHOD AddFriend
        @OVERVIEW Metodo che aggiunge un amico alla lista amici del giocatore
        @PAR Friend Giocatore da aggiungere alla lista
    */
    public Player AddFriend(String FriendNick)
    {
        if(FriendNick.equals(NickName))
        {
            System.out.println("ERRORE: Un utente non può aggiungere sè stesso in quanto amico\n");
        }

        this.Friends.add(FriendNick);
        
        return this;
        
    }
    
    public void GetFriendList()
    {
        for(String s : Friends)
        {
            System.out.println(s+"\n");
        }
    }
    
    /*
        @METHOD PlayerToJSON
        @OVERVIEW Metodo di serializzazione dei dati di un giocatore in un file
        in formato JSON
    
        @RETURNS PlayerData documento in formato JSON contenente info del giocatore
    */
    public JSONObject PlayerToJSON()
    {
        JSONObject PlayerData = new JSONObject();
        JSONArray PlayerFriends = new JSONArray();
        
        PlayerFriends.addAll(Friends);
        
        PlayerData.put("NOME",NickName);
        PlayerData.put("PASSWORD",Password);
        PlayerData.put("PUNTEGGIO", score);
        PlayerData.put("AMICI",PlayerFriends);
        
        return PlayerData;
    }
}
