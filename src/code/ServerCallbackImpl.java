import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//Lato Server
public class ServerCallbackImpl extends RemoteServer implements ServerInterfaceCallback {

    public List<NotifyEventInterface> clients;
    public ServerCallbackImpl(){
        super();
        clients = new ArrayList<NotifyEventInterface>();
    }

    @Override
    public void registerForCallback(NotifyEventInterface ClientInterface) throws RemoteException {
        if (!clients.contains(ClientInterface)) {
            clients.add(ClientInterface);
            System.out.println("Client registrato per le notifiche callback");
        }
    }

    @Override
    public void unregisterForCallback(NotifyEventInterface ClientInterface) throws RemoteException {
        if(clients.remove(ClientInterface)){
            System.out.println("Client rimosso dagli utenti registrati per le notifiche callback");
        }else{
            System.out.println("Client non era registrato per le notifiche callback");
        }
    }

    public void update(String value) throws RemoteException {
        doCallbacks(value);
    }

    private synchronized void doCallbacks(String value) throws RemoteException {
        System.out.println("Starting callbacks for " + clients.toString());
        Iterator i = clients.iterator( );
        while (i.hasNext()) {
            NotifyEventInterface client = (NotifyEventInterface) i.next();
            client.notifyEvent(value);
        }
        System.out.println("Callbacks complete");}


}
