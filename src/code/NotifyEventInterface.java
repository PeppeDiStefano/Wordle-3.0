import java.rmi.Remote;
import java.rmi.RemoteException;

//Lato Client
public interface NotifyEventInterface extends Remote {

    //Metodo esportato dal client che viene utilizzato dal server per la notifica
    void notifyEvent(String value) throws RemoteException;
}
