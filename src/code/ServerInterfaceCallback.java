import java.rmi.Remote;
import java.rmi.RemoteException;

//Lato Server
public interface ServerInterfaceCallback extends Remote {

    //Metodo per registrarsi a ricevere notifiche
    void registerForCallback(NotifyEventInterface ClientInterface) throws RemoteException;
    //Metodo per rimuoversi dalla ricezione di notifiche
    void unregisterForCallback(NotifyEventInterface ClientInterface) throws RemoteException;
}
