/*
PROGETTO FINALE DI LABORATORIO DI RETI A.A 2019-2020
NOME PROGETTO: WORD QUIZZLE

AUTORE: Enrico Tomasi
NUMERO MATRICOLA: 503527

OVERVIEW: Implementazione di un sistema di sfide di traduzione italiano-inglese
tra utenti registrati secondo paradigma client-server
 */
package WordQuizzleClient;

import java.rmi.Remote;
import java.rmi.RemoteException;

/*
    @INTERFACE RemoteRequests
    @OVERVIEW Interfaccia che espone i metodi accessibili da remoto per la
    registrazione di un utente
*/
public interface RemoteRequests extends Remote
{
    public int Registra_utente(String password, String nickname) throws RemoteException;
    
}
