import com.google.gson.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class Handler implements Runnable{
    final private Socket socket;
    public MulticastSocket multicastSocket;
    public String multicast_host;
    public int multicast_port;
    public ConcurrentHashMap<String, Utente> utenti;
    final private AtomicReference<String> secret_word;
    InetAddress group;  //gruppo sociale
    public Utente utenteCorrente = null;
    public String risultatoPartita;
    public SortedSet<Utente> classifica;
    public ServerCallbackImpl implCall;

    public Handler(Socket socket, MulticastSocket multicastSocket, String multicast_host, int multicast_port, ConcurrentHashMap<String, Utente> utenti, AtomicReference<String> secret_word, SortedSet<Utente>  classifica, ServerCallbackImpl implCall){
        this.socket = socket;
        this.multicastSocket = multicastSocket;
        this.multicast_host = multicast_host;
        this.multicast_port = multicast_port;
        this.utenti = utenti;
        this.secret_word = secret_word;
        this.classifica = classifica;
        this.implCall = implCall;
    }

    public void run(){
        System.out.println("Apertura client " + socket.getPort());

        //schema di indirizzamento per identificare il multicast
        try{
            group = InetAddress.getByName(multicast_host);
        }catch (UnknownHostException e){
            System.out.println("Il server non è connesso al gruppo di multicast corretto");
        }

        //stampo la mappa letta dal file json, che mi ha memorizzato tutto
        System.out.println("Mappa all'inizio -> " + utenti);

        try(Scanner inputFromClient = new Scanner(socket.getInputStream());  //input dal client
            PrintWriter outputToClient = new PrintWriter(socket.getOutputStream(),true)//output del server
        )
        {
            while(inputFromClient.hasNextLine()){
                end:
                switch (inputFromClient.nextLine()){

                    case "login":
                        //al primo login il mio user sarà null, se faccio dei login successivi mi basta che nessun altro sia loggato al momento
                        if(utenteCorrente == null || (utenteCorrente != null && !utenteCorrente.isLogged)){
                            outputToClient.println("Digitare username");
                            String username = inputFromClient.nextLine();
                            boolean utenteEsiste = false;

                            for(Map.Entry<String,Utente> entry : utenti.entrySet()) {
                                Utente utente = entry.getValue();
                                if(utente.getUsername().equalsIgnoreCase(username) && !utenteEsiste){
                                    //se un utente nella mappa è uguale all'username che ho inserito, controllo la password
                                    utenteEsiste = true;

                                    outputToClient.println("Digitare password");
                                    String password = inputFromClient.nextLine();
                                    //controllo la password, se combacia il login è effettuato con successo
                                    if(utente.getPassword().equalsIgnoreCase(password)){
                                        outputToClient.println("Utente loggato con successo");
                                        //inizializzo l'utente che userò in quel momento
                                        utenteCorrente = utente;
                                        utenteCorrente.isLogged = true;
                                        break end;
                                    } else {
                                        outputToClient.println("Password errata, rieffettuare il login");
                                        break end;
                                    }
                                }
                            }
                            if(!utenteEsiste){
                                outputToClient.println("Utente non esistente, devi effettuare la register o controllare l'username");
                                break;
                            }
                            outputToClient.println("Inserisci prossima azione");
                            break;
                        } else {
                            outputToClient.println("Sei loggato con un altro account. Fai il logout");
                            break;
                        }

                    case "logout":
                        try {
                            if(utenteCorrente != null && utenteCorrente.isLogged) {
                                //setto che non sono più loggato con nessun utente
                                utenteCorrente.isLogged = false;
                                //ripristino la verifica se le partite son terminate
                                utenteCorrente.partitaTerminata = false;
                                outputToClient.println("Logout effettuato con successo");
                                break;
                            } else {
                                outputToClient.println("Non sei loggato con nessun account");
                                break;
                            }
                        } catch (NullPointerException e) {
                            break;
                        }

                    case "play wordle":
                        if(utenteCorrente != null && utenteCorrente.isLogged){ //controllo che sia loggato
                            if(!(utenteCorrente.getHaGiaPartecipato())){ //se non ha già partecipato alla parola data
                                utenteCorrente.partecipa();
                                outputToClient.println("L'utente " + utenteCorrente.getUsername() + " inizia a giocare a WORDLE 3.0");
                                break;
                            }else{  //se ha già partecipato alla parola corrente
                                outputToClient.println("L'utente " + utenteCorrente.getUsername() + " ha già partecipato per la parola corrente");
                                break;
                            }
                        }else{ //l'utente non è loggato
                            outputToClient.println("Errore, devi prima loggarti");
                            break;
                        }

                    case "send word":
                        if(utenteCorrente != null && utenteCorrente.isLogged){ //controllo che sia loggato
                            if(utenteCorrente.puoGiocare && utenteCorrente.getTentativiFatti()<12){ //se l'utente può giocare e ha fatto meno di 12 tentativi
                                outputToClient.println("Inserisci la parola che pensi sia corretta");
                                String guessed_word = inputFromClient.nextLine(); //parola digitata dall'utente
                                if(guessed_word.equals(secret_word.toString())){ //se indovina
                                    outputToClient.println("Vittoria! La parola da indovinare era " + secret_word);

                                    //rimuovo l'utente dalla classifica
                                    Utente utenteDaAggiornare;
                                    utenteDaAggiornare = utenti.get(utenteCorrente.getUsername());
                                    String classifica_path = "../src/classifica.json";

                                    //controllo se era presente precedentemente e lo rimuovo
                                    if(classifica.contains(utenteDaAggiornare)){
                                        classifica.remove(utenteDaAggiornare);
                                    }

                                    //aggiorno le statistiche
                                    utenteCorrente.vittorie++;
                                    utenteCorrente.tentativi++;
                                    utenteCorrente.addTentativoToArray(utenteCorrente.tentativi); //salvo i tentativi fatti nell'array
                                    utenteCorrente.mediaGuessDistribution = utenteCorrente.calcolaMediaGuessDistribution();
                                    utenteCorrente.punteggio = utenteCorrente.calcolaPunteggio();

                                    //aggiungo l'utente con le statistiche aggiornate
                                    classifica.add(utenti.get(utenteCorrente.getUsername()));

                                    //aggiorno il file JSON che contiene la classifica
                                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                                    try{
                                        FileOutputStream fos = new FileOutputStream(classifica_path);
                                        OutputStreamWriter ow = new OutputStreamWriter(fos);
                                        String classificaString = gson.toJson(classifica);
                                        ow.write(classificaString);
                                        ow.flush();
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }

                                    //controllo se le prime 3 posizioni sono cambiate
                                    Iterator<Utente> iterator = classifica.iterator();
                                    int i =1;
                                    while(iterator.hasNext() && i <= 3){
                                        Utente u = utenti.get(utenteCorrente.getUsername());
                                        if( (iterator.next().equals(u)) ){ //allora vuol dire che sono cambiate le prime 3 posizioni
                                            implCall.update(utenteCorrente.getUsername() + " è in " + i + " posizione con " + u.getPunteggio() + " punti");
                                        }
                                        i++;
                                    }

                                    utenteCorrente.puoGiocare = false; //non posso pù giocare per questa parola
                                    utenteCorrente.haVinto = true;
                                    utenteCorrente.partitaTerminata = true;
                                    break;
                                } else if (!(check(guessed_word)) || (guessed_word.length() != 10)){ //se la parola non è presente nel vocabolario dato o non è lunga 10
                                    outputToClient.println("Attenzione la parola " + guessed_word + " non è presente nel vocabolario, Tentativi effettuati " + utenteCorrente.tentativi);
                                    break;
                                }else { //se la parola esiste ma non è corretta
                                    utenteCorrente.tentativi++;

                                    //produco l'indizio
                                    char[] hint= produciIndizio(guessed_word, secret_word.toString());

                                    outputToClient.println("Sbagliato ecco l'indizio " + Arrays.toString(hint) + " Tentativi effettuati " + utenteCorrente.tentativi); //mando l'indizio
                                    break;
                                }
                            }else if(utenteCorrente.getTentativiFatti()==12){ //utente ha fatto 12 tentativi
                                outputToClient.println("Errore, hai effettuato il numero massimo di tentativi");
                                //aggiorno statistiche
                                utenteCorrente.addTentativoToArray(12);
                                utenteCorrente.partitaTerminata = true;
                                utenteCorrente.haVinto = false;
                                break;
                            }else {
                                outputToClient.println("Errore, digitare prima play wordle e poi send word");
                                break;
                            }
                        }else{  //utente non loggato
                            outputToClient.println("Errore, devi prima loggarti");
                            break;
                        }

                    case "translate":
                        if(utenteCorrente != null && utenteCorrente.partiteGiocate !=0 && utenteCorrente.isLogged && utenteCorrente.partitaTerminata){ //controllo che l'utente sia loggato, che abbia terminato la partita
                            String traduction = traduzione(secret_word);
                            if(traduction != null){
                                outputToClient.println("La traduzione della secret word è " + traduction);
                                break;
                            }else{
                                outputToClient.println("Errore nella traduzione della secret word");
                            }
                        }else{
                            outputToClient.println("Errore, devi essere loggato e aver terminato la partita");
                            break;
                        }

                    case "send me statistics":
                        if(utenteCorrente != null && utenteCorrente.partiteGiocate != 0 && utenteCorrente.isLogged){ //controllo se l'utente sia loggato ed abbia disputato partite
                            try{
                                outputToClient.println("Statistiche di " + utenteCorrente.getUsername() + ": Partite giocate " + utenteCorrente.getPartiteGiocate()  + ", Vittorie " + utenteCorrente.getVittorie()+ ", GuessDistribution " + utenteCorrente.getGuessDistribution().toString() + ", Punteggio " + utenteCorrente.getPunteggio()+ ", Media dei tentativi impiegati " + utenteCorrente.getMediaGuessDistribution());
                                break;
                            }catch (NullPointerException e){
                                outputToClient.println("Errore nel mostrare nel statistiche");
                                break;
                            }
                        }else{
                            outputToClient.println("Errore, non puoi vedere le statistiche se non hai disputato partite o se non sei loggato");
                            break;
                        }

                    case "share":
                        if(utenteCorrente != null && utenteCorrente.partiteGiocate != 0 && utenteCorrente.isLogged && utenteCorrente.partitaTerminata){ //controllo che l'utente sia loggato, che abbia terminato la partita
                            System.out.println("Condivido risultati ultima partita di " + utenteCorrente.getUsername());

                            if(utenteCorrente.haVinto){ //se utente ha vinto
                                risultatoPartita = "vinto";
                            }else{  //altrimenti
                                risultatoPartita = "perso";
                            }

                            //creo la stringa UDP da mandare ai client che hanno joinato il multicast
                            String stringSendUDP = "L'utente " + utenteCorrente.getUsername() + " ha " + risultatoPartita;
                            //mando il risultato della partita ai client che hanno joinato la multicast e sono loggati
                            byte[] buf = stringSendUDP.getBytes();
                            DatagramPacket shareRes = new DatagramPacket(buf, buf.length, group, multicast_port);
                            multicastSocket.send(shareRes);
                            outputToClient.println("Risultato condiviso con successo");
                            break;
                        }else {
                            outputToClient.println("Errore nella condivisione dei risultati");
                            break;
                        }

                    case "show me sharing":
                        if(utenteCorrente != null && utenteCorrente.isLogged){ //controllo che l'utente sia loggato
                            outputToClient.println("Mostra condivisioni avvenuta con successo");
                            break;
                        }else {
                            outputToClient.println("Errore, devi prima loggarti");
                            break;
                        }

                    case "show me ranking":
                        if(utenteCorrente != null && utenteCorrente.isLogged){ //controllo che l'utente sia loggato
                            outputToClient.println("La classifica è " + classifica.toString());
                            break;
                        }else {
                            outputToClient.println("Errore, devi prima loggarti");
                            break;
                        }

                    case "help":
                        break;

                    case "exit":
                        System.out.println("Chiusura client " + socket.getPort());
                        break;

                    default:
                        outputToClient.println("Comando non riconosciuto");
                        break;
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    //Metodo che controlla se una data parola che gli passo è nel file words.txt
    public boolean check(String s) throws FileNotFoundException {
        if (new Scanner(new File("../src/words.txt")).useDelimiter("\\Z").next().contains(s)) {
            return true;
        } else {
            return false;
        }
    }

    //Metodo che produce l'indizio
    public char[] produciIndizio(String guessed_word, String secret_word){
        char[] a = guessed_word.toCharArray();
        char[] b = secret_word.toCharArray();
        char[] hint = new char[10];

        //costruisco l'indizio comparando i due array e fornendone un terzo come indizio
        for (int i = 0; i < a.length; i++) {
            if(a[i] == b[i]){   //se i caratteri sono uguali...
                hint[i] = '+'; //...metto "+"
            } else {
                for (int j = 0; j < b.length; j++) {
                    if (a[i] == b[j]) { //se il carattere è presente ma non è nella posizione giusta...
                        hint[i] = '?'; //...metto "?"
                        break;
                    }
                }
            }
        }
        for (int k = 0; k < hint.length; k++) {
            if(hint[k] != '+' && hint[k] != '?'){ //se il carattere è sbagliato completamente...
                hint[k] = 'X'; //...// metto "X"
            }
        }
        return hint;
    }

    //Metodo che effettua la traduzione della parola collegandosi al servizio mymemory
    public String traduzione(AtomicReference<String> secret_word) throws Exception{
        URL url = new URL("https://api.mymemory.translated.net/get?q=" + secret_word + "&langpair=en|it");
        String s = "";
        try(BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))){
            StringBuilder inputLine = new StringBuilder();
            String reader;
            while((reader = in.readLine()) != null){
                inputLine.append(reader);
            }

            //creazione degli oggetti JSON
            JSONObject jsonObject;
            JSONParser parser = new JSONParser();

            try{
                jsonObject = (JSONObject) parser.parse(inputLine.toString()); //parso il JSON ricevuto
                JSONArray array = (JSONArray) jsonObject.get("matches");
                //prendo il JSON ricevuto e per ogni elemento prendo solamente la traduzione e la inserisco nella ArrayList
                for (Object o : array) {
                    JSONObject obj = (JSONObject) o;
                    s = (String) obj.get("translation");
                }
            }catch (Exception e){
                return null;
            }
        }catch (MalformedURLException e){
            return null;
        }
        return s;
    }

}
