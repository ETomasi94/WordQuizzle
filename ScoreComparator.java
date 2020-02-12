/*
PROGETTO FINALE DI LABORATORIO DI RETI A.A 2019-2020
NOME PROGETTO: WORD QUIZZLE

AUTORE: Enrico Tomasi
NUMERO MATRICOLA: 503527

OVERVIEW: Implementazione di un sistema di sfide di traduzione italiano-inglese
tra utenti registrati secondo paradigma client-server
 */
package WQServer;

import java.util.Comparator;

/*
    @CLASS ScoreComparator
    @OVERVIEW Classe di supporto che implementa il confronto di punteggi al fine
    di ordinarli secondo ordine decrescente
*/
public class ScoreComparator implements Comparator<Long>
{
    /*
        @METHOD compare
        @OVERVIEW Metodo di confronto tra due punteggi
    
        @PARAM score1 Primo punteggio nel confronto 
        @PARAM score2  Secondo punteggio nel confronto
    
        @RETURN Intero result che indica il risultato del confronto
    */
    @Override
    public int compare(Long score1,Long score2) 
    {
        return (int) (score2 - score1);
    }
    
}
