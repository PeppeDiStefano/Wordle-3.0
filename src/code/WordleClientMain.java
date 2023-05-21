import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class WordleClientMain {
    public static final String configFile = "../src/client.properties.txt";
    public static String hostname;
    public static int port;
    public static int multicast_port;
    public static String multicast_host;
    public static String register_name;
    public static int registry_port;
    public static String callback_name;
    public static int callback_port;
    private static boolean end = false;  //variabile per uscire dall'ascolto sulla multicastSocket dopo una logout
    public static ServerInterfaceCallback server;
    public static NotifyEventInterface stub;

    public static void main(String[] args){

        try {//leggo il file di configurazione
            readConfigClient();
        }catch (Exception e){
            e.printStackTrace();
        }

        //lista delle notifiche del gruppo sociale
        List<String> notifiche = (List<String>) Collections.synchronizedList(new ArrayList<String>());

        try (Socket clientSocket = new Socket(hostname, port); //client crea socket verso server, richiesta connessione
             Scanner scanner = new Scanner(System.in); //input da tastiera
             Scanner inputFromServer = new Scanner(clientSocket.getInputStream()); //input dal server
             MulticastSocket multicastSocket = new MulticastSocket(multicast_port); //socket per gruppo sociale
             PrintWriter outputToServer = new PrintWriter(clientSocket.getOutputStream(), true) //output verso il server
        ) {

            System.out.println("Connessione all' host " + hostname + " sulla porta " + port);
            System.out.println("L'utente può eseguire i seguenti comandi: \n1) register \n2) login \n3) logout \n4) play wordle \n5) send word \n6) translate \n7) send me statistics \n8) share \n9) show me sharing \n10) show me ranking\n10) help \n11) exit");

            //Thread che inizializzo non appena il client fa la join
            Thread threadListener = new Thread(() -> {
                System.out.println("Il client si mette in attesa di share dagli altri utenti");
                while(!end){
                    //se ricevo condivisioni tramite UDP, le stampo nel client che ha mandato il comando show me sharing
                    DatagramPacket response = new DatagramPacket(new byte[1024], 1024);
                    try {
                        multicastSocket.receive(response);
                    } catch (IOException e){
                    }
                    String responseUDPfromServer = new String(response.getData(), 0, response.getLength());
                    notifiche.add(responseUDPfromServer);
                }
            });

            while(true){

                String s = scanner.nextLine(); //prendo il comando dal terminale

                //se è uguale a exit, esco
                if(s.contentEquals("exit")){
                    outputToServer.println(s); //mando l'input al server
                    end = true;
                    System.out.println("Bye bye client");
                    System.exit(0);
                }

                //se è uguale a register, uso RMI
                if(s.contentEquals("register")){

                    Registry registry = LocateRegistry.getRegistry(hostname,registry_port);
                    InterfacciaRemota remota = (InterfacciaRemota) registry.lookup(register_name);

                    System.out.println("Digitare username");
                    String username = scanner.nextLine();
                    System.out.println("Digitare password");
                    String password = scanner.nextLine();

                    int result;
                    result = remota.registrazione(username, password);
                    switch (result) {
                        case 0 -> System.out.println("Utente " + username + " registrato correttamente");
                        case 1 -> System.out.println("Username o password non validi");
                        case 2 -> System.out.println("Utente " + username + " è già registrato");
                        default -> System.err.println("Errore riprova");
                    }
                    continue;
                }

                //se è uguale a help, stampo la lista dei comandi
                if(s.contentEquals("help")){
                    System.out.println("L'utente può eseguire i seguenti comandi: \n1) register \n2) login \n3) logout \n4) play wordle \n5) send word \n6) translate \n7) send me statistics \n8) share \n9) show me sharing \n10) show me ranking\n11) help \n12) exit");
                    continue;
                }

                outputToServer.println(s); //mando l'input al server
                String risposta = inputFromServer.nextLine(); //risposta del server
                System.out.println(risposta); //stampo la risposta

                if(risposta.equals("Utente loggato con successo")){

                    //mi registro per le callback
                    Registry r = LocateRegistry.getRegistry(callback_port);
                    server = (ServerInterfaceCallback) r.lookup(callback_name);
                    NotifyEventInterface callbackObj = new NotifyEventImpl();
                    stub = (NotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj,0);
                    server.registerForCallback(stub);

                    notifiche.clear();
                    end = false; //resetto eventualmente a false per riavviare l'ascolto sul gruppo
                    InetAddress group = InetAddress.getByName(multicast_host); //stesso gruppo del server
                    InetSocketAddress address = new InetSocketAddress(group, multicast_port);
                    NetworkInterface networkInterface = NetworkInterface.getByInetAddress(group);
                    try { //una volta loggato con successo, il client joina il gruppo
                        multicastSocket.joinGroup(address, networkInterface);
                        System.out.println("Il client entra a far parte del gruppo sociale " + multicast_host);
                        threadListener.start(); //si mette in ascolto di info condivise dagli altri utenti da mettere nell'array di notifiche
                    } catch (SocketException e) {
                        //se il client aveva già precedentemente joinato il gruppo
                        System.out.println("Il client è entrato a far parte del gruppo sociale precedentemente");
                        continue;
                    }
                }

                if(risposta.equals("Logout effettuato con successo")){
                    multicastSocket.close();
                    server.unregisterForCallback(stub);
                    end = true;
                    System.out.println("Client uscito dal gruppo sociale e non riceve più notifiche riguardanti la classifica");
                    continue;
                }

                if(risposta.equals("Mostra condivisioni avvenuta con successo")){
                    System.out.println("Condivisioni ricevute " + notifiche.toString());
                }
            }

        } catch (Exception e){
            System.out.println("Errore nel client");
            e.printStackTrace();
            System.exit(1);
        }
    }

    //Metodo per leggere il file di configurazione
    public static  void readConfigClient()throws Exception{
        InputStream input = new FileInputStream(configFile);
        Properties prop = new Properties();
        prop.load(input);
        hostname = prop.getProperty("hostname");
        port = Integer.parseInt(prop.getProperty("port"));
        multicast_port = Integer.parseInt(prop.getProperty("multicastPort"));
        multicast_host = prop.getProperty("multicastHost");
        register_name = prop.getProperty("registerName");
        registry_port = Integer.parseInt(prop.getProperty("registryPort"));
        callback_name = prop.getProperty("callbackName");
        callback_port = Integer.parseInt(prop.getProperty("callbackPort"));
        input.close();
    }

}
