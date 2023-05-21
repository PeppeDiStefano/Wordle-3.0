import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;

//Lato Client
public class NotifyEventImpl extends RemoteObject implements NotifyEventInterface {
    public ArrayList<String> podio;
    public NotifyEventImpl(){
        super();
        podio = new ArrayList<>();
    }

    //aggiorniamo il client sulla situazione dei primi 3 in classifica
    @Override
    public void notifyEvent(String value) throws RemoteException {
        String returnMessage = value;
        podio.add(returnMessage);
        System.out.println("Update Ricevuti:\n" + podio);
    }
}
