import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;

public class TerminationThread extends Thread {

    public ConcurrentHashMap<String, Utente> utenti;

    private ServerSocket serverSocket;
    private int termination_time; //aspetterà il pool prima di interrompere bruscamente
    private ExecutorService pool;

    public TerminationThread(int termination_time, ExecutorService pool, ServerSocket serverSocket, ConcurrentHashMap<String, Utente> utenti) {
        this.termination_time = termination_time;
        this.pool = pool;
        this.serverSocket = serverSocket;
        this.utenti = utenti;
    }

    public void run() {
        System.out.println("Avvio proceduta di terminazione del server...");

        //chiudo la ServerSocket in modo tale da non accettare piu' nuove richieste
        try {
            serverSocket.close();
        }
        catch (IOException e) {
            System.err.println("Errore: " + e.getMessage());
        }

        //faccio terminare il pool di thread
        pool.shutdown();
        try {
            if (!pool.awaitTermination(termination_time, TimeUnit.MILLISECONDS))
                pool.shutdownNow();
        }
        catch (InterruptedException e) {pool.shutdownNow();}

        System.out.println("Server Terminato!");

        //trasformo la mappa in un json e lo metto nel file raccoltaUtenti.json
        System.out.println("Mappa salvata dal TerminationThread -> " + utenti);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter fileWriter = new FileWriter("../src/raccoltaUtenti.json")) {
            gson.toJson(utenti, fileWriter);
            try {
                fileWriter.close();
            } catch (IOException e1) {
                System.err.println("Errore chiusura FileWriter");
            }
        } catch (JsonIOException | IOException e2) {
            System.err.println("Errore trasformazione mappa in json");
        }
    }

}