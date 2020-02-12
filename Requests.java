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
import java.util.List;
import org.json.simple.parser.ParseException;

/*
    @INTERFACE Requests
    @OVERVIEW Interfaccia che espone i metodi di gestione delle operazioni invocati
    dal server Word Quizzle
*/
public interface Requests 
{
    public String Login(String nickUtente,String password);
    
    public String Logout(String nickUtente);
    
    public int Aggiunta_amico(String NickA, String NickB);
    
    public List<String> Lista_amici(String nickUtente) throws FileNotFoundException, IOException, ParseException ;
    
    public String Sfida(String nickUtente,String nickAmico);
    
    public long Mostra_Punteggio(String nickUtente);
    
    public String Mostra_Classifica(String nickUtente);   
}
