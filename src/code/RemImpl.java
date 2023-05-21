import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.Map;

public class RemImpl extends RemoteServer implements InterfacciaRemota {
    final Map<String, Utente> utenti;

    public RemImpl(Map<String,Utente> utenti){
        this.utenti = utenti;
    }

    public int registrazione(String username, String password) throws RemoteException{
        if(username.isEmpty() || password.isEmpty()){
            return 1; //parametri sbagliati
        }

        Utente u = new Utente(username,password);
        if(utenti.putIfAbsent(username,u)!=null){
            return 2; //utente già registrato
        }
        else{
            return 0; //posso registrare l'utente
        }
    }

}
