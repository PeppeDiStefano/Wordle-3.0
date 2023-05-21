import java.rmi.Remote;
import java.rmi.RemoteException;

public interface InterfacciaRemota extends Remote {
     int registrazione(String username, String password) throws RemoteException;
}
