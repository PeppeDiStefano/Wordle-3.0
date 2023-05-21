import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class WordleServerMain {

    public static ConcurrentHashMap<String,Utente> utenti;
    public static final String configFile = "../src/server.properties.txt";
    public static int port;
    public static int multicast_port;
    public static String multicast_host;
    public static String register_name;
    public static int registry_port;
    public static String callback_name;
    public static int callback_port;
    public static int n_thread;
    public static int keep_alive_time;
    public static int termination_time;
    public static int time_between_words;
    public static AtomicReference<String> secret_word = new AtomicReference<>("initialKeyWord");//parola da indovinare, la inizializzo altrimenti dà errore
    public static ServerCallbackImpl implCall;
    public static SortedSet<Utente> classifica;

    public static void main(String[] args){

        //leggo il file di configurazione
        try {
            readConfigServer();
        }catch (Exception e){
            e.printStackTrace();
        }

        //creo la ConcurrentHashMap dove andrò a salvare gli utenti
        //prendendo il file JSON che ha tutti i dati degli utenti
        try{
            String file = "../src/raccoltaUtenti.json";
            String jsonString = readFileAsString(file);
            if(jsonString.equals("") || jsonString.equals(null)){ //inizializzo la mappa, se il file è null
                System.out.println("Inizializzo la mappa -> " + utenti);
                utenti = new ConcurrentHashMap<>();
            } else { //altrimenti se ho una mappa già esistente che avevo salvato al precedente spegnimento, la ripristino
                utenti = new Gson().fromJson(jsonString, new TypeToken<ConcurrentHashMap<String, Utente>>() {}.getType());
                System.out.println("Mappa ripristinata dal WordleMainServer -> " + utenti);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        //Creo la classifica
        try {
            String file = "../src/classifica.json";
            String jsonString = readFileAsString(file);
            if(jsonString.equals("") || jsonString.equals(null)){   //inizializzo la classifica, se il file è null
                System.out.println("Inizializzo la classifica -> " + classifica );
                UtenteComparator comparator = new UtenteComparator();
                classifica = Collections.synchronizedSortedSet(new TreeSet<>(comparator));
            }else{  //altrimenti la ripristino
                UtenteComparator comparator = new UtenteComparator();
                TreeSet<Utente> treeSet = new Gson().fromJson(jsonString, new TypeToken<TreeSet<Utente>>() {}.getType());
                classifica = Collections.synchronizedSortedSet(new TreeSet<>(comparator));
                classifica.addAll(treeSet);
                System.out.println("Classifica ripristinata dal WordleMainServer -> " + classifica);
            }
        }catch (Exception e){
            e.printStackTrace();
        }


        implCall = new ServerCallbackImpl();
        //inizializzo RMI
        initializeRMI();


        try(ServerSocket serverSocket = new ServerSocket(port);
            RandomAccessFile randAccesFile = new RandomAccessFile("../src/words.txt", "r");
            MulticastSocket multicastSocket = new MulticastSocket())
        {

            System.out.println("Server pronto");
            System.out.println("In ascolto sulla porta " + port);

            //creo il threadpool
            BlockingQueue<Runnable> coda = new LinkedBlockingQueue<>();
            ExecutorService pool = new ThreadPoolExecutor(n_thread, n_thread, keep_alive_time, TimeUnit.MILLISECONDS, coda, new ThreadPoolExecutor.AbortPolicy());

            //utilizzo thread di terminazione,per salvare la mappa nel file json
            Runtime.getRuntime().addShutdownHook(new TerminationThread(termination_time, pool, serverSocket, utenti));

            //Timer che ogni tot secondi reimposta la possibilità di giocare e aggiorna la Secret Word
            Timer timer = new Timer();
            TimerTask task = new TimerTask() {
                public void run(){
                    //pesco la stringa da usare come parola da indovinare
                    byte[] buffer = new byte[10];
                    int n = ThreadLocalRandom.current().nextInt(0, 30824 + 1);  //genero un offset casuale
                    try{
                        //il file parte dal byte 0, la seconda parola si trova all'11° byte e così via (parole di 10 lettere)
                        randAccesFile.seek(n * 11);
                        randAccesFile.read(buffer);
                    } catch (IOException e) {
                        System.out.println("Errore lettura Secret Word.");
                    }
                    // Stampo e setto la nuova stringa letta
                    String newString = new String(buffer);
                    secret_word.set(newString);
                    System.out.println("Secret Word ---> " + secret_word.toString());

                    //setto tutte le variabili degli utenti (che magari hanno già partecipato ad una parola precedente)
                    // a false in modo che possano rigiocare con la nuova parola
                    for(Map.Entry<String,Utente> entry : utenti.entrySet()) {
                        Utente utente = entry.getValue();
                        utente.tentativi = 0;
                        utente.haPartecipato = false;
                        utente.partitaTerminata = false;
                        utente.puoGiocare = false;
                        utente.haVinto = false;
                    }
                }
            }; //aspetto 2 minuti per l'estrazione di una nuova Secret Word
            timer.scheduleAtFixedRate(task, 0, time_between_words);

            while(true){
                Socket socket;
                try{ //accetto le richieste provenienti dai client
                    socket = serverSocket.accept();
                }catch (SocketException e){
                    break;
                }
                //avvio il pool di thread
                pool.execute(new Handler(socket, multicastSocket, multicast_host, multicast_port, utenti, secret_word, classifica, implCall));
            }

        }catch (Exception e){
            System.err.println("Errore in WordleServerMain");
            e.printStackTrace();
            System.exit(1);
        }

    }

    //Metodo per leggere il file di configurazione(Lato Server)
    public static void readConfigServer() throws Exception{
        InputStream input = new FileInputStream(configFile);
        Properties prop = new Properties();
        prop.load(input);
        port = Integer.parseInt(prop.getProperty("port"));
        multicast_port = Integer.parseInt(prop.getProperty("multicastPort"));
        multicast_host = prop.getProperty("multicastHost");
        register_name = prop.getProperty("registerName");
        registry_port = Integer.parseInt(prop.getProperty("registryPort"));
        n_thread = Integer.parseInt(prop.getProperty("nThread"));
        keep_alive_time = Integer.parseInt(prop.getProperty("keepAliveTime"));
        termination_time = Integer.parseInt(prop.getProperty("terminationTime"));
        time_between_words = Integer.parseInt(prop.getProperty("timeBetweenWords"));
        callback_name = prop.getProperty("callbackName");
        callback_port = Integer.parseInt(prop.getProperty("callbackPort"));
        input.close();
    }

    //Metodo per inizializzare RMI
    public static void initializeRMI(){
        RemImpl implRem = new RemImpl(utenti);
        try{
            //RMI registrazione
            InterfacciaRemota regStub = (InterfacciaRemota) UnicastRemoteObject.exportObject(implRem,0);
            //registry per RMI registrazione
            LocateRegistry.createRegistry(registry_port);
            Registry registry = LocateRegistry.getRegistry(registry_port);
            registry.rebind(register_name, regStub);

            //RMI callback
            ServerInterfaceCallback callbackStub = (ServerInterfaceCallback) UnicastRemoteObject.exportObject(implCall, 39000);
            //registry per callback
            LocateRegistry.createRegistry(callback_port);
            Registry r = LocateRegistry.getRegistry(callback_port);
            r.bind(callback_name, callbackStub);
        }catch (RemoteException e){
            System.err.println("Errore nell'inizializzazione del RMI" + e.getMessage());
            System.exit(1);
        }catch (AlreadyBoundException ex){
            System.err.println("Errore nell'inizializzazione del RMI" + ex.getMessage());
            System.exit(1);
        }

    }

    //Meotodo per leggere il file come una stringa
    public static String readFileAsString(String file)throws Exception{
        return new String(Files.readAllBytes(Paths.get(file)));
    }

}