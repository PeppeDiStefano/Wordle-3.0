import java.util.ArrayList;
import java.util.List;

public class Utente implements Comparable<Utente> {
    public String username;
    public String password;
    public boolean haPartecipato = false; //variabile per vedere se l'utente ha partecipato ad indovinare la parola estratta
    public boolean puoGiocare = false; //variabile per vedere se l'utente può giocare. Deve prima digitare "play wordle" e poi "send word"
    public boolean isLogged = false; //variabile per vedere se l'utente è connesso/loggato
    public boolean partitaTerminata = false; //variabile per vedere se l'utente ha terminato la partita
    public boolean haVinto = false; //variabile per vedere se l'utente ha vinto o ha perso
    public int tentativi; //numero di tentativi effettuati per la parola corrente
    public int partiteGiocate; //numero di partite giocate
    public double mediaGuessDistribution; //distribuzione di tentativi impiegati per arrivare alla soluzione del gioco,in ogni partita vinta dal giocatore
    public int vittorie; //numero di vittorie totali
    List<Integer> guessDistribution ; //array che contiene tutti i tentativi fatti dall'utente
    public double punteggio; //punteggio del giocatore

    public Utente(String username, String password){
        this.username = username;
        this.password = password;
        this.tentativi = 0;
        this.partiteGiocate = 0;
        this.mediaGuessDistribution = 0;
        this.vittorie = 0;
        this.guessDistribution = new ArrayList<>();
        this.punteggio = 0;
    }

    //Metodi getter
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public int getTentativiFatti() {
        return tentativi;
    }
    public int getPartiteGiocate() {
        return partiteGiocate;
    }
    public double getMediaGuessDistribution() {
        return mediaGuessDistribution;
    }
    public boolean getHaGiaPartecipato() {
        return haPartecipato;
    }
    public List<Integer> getGuessDistribution() {
        return guessDistribution;
    }
    public int getVittorie(){return vittorie;}
    public double getPunteggio(){return Math.floor(punteggio * 100)/100;}
    

    //Metodo per vedere se un utente ha partecipato
    public void partecipa() {
        if(!haPartecipato) {
            partiteGiocate++;
            haPartecipato = true;
            puoGiocare = true;
        }
    }

    //Metodo per aggiungere i tentativi effettuati nell'array
    public void addTentativoToArray(int tentativo) {
        this.guessDistribution.add(tentativo);
    }


    //Metodo che calcola la media della guessDistribution
    public double calcolaMediaGuessDistribution(){
        Integer sum = 0;
        for(int i = 0; i < guessDistribution.size(); i++)
            sum += guessDistribution.get(i);
        return ((double) sum /  (double) guessDistribution.size() );
    }

    //Metodo per calcolar il punteggio = num.vittorie * media di tentativi per arrivare alla soluzione
    public double calcolaPunteggio(){
        Integer sum = 0;
        for(int i = 0; i < guessDistribution.size(); i++)
            sum += guessDistribution.get(i);
        return ((double) vittorie) * ((double) sum /  (double) guessDistribution.size());
    }

    //Metodo per confrontare gli utenti
    public int compareTo(Utente u){
        if(this.equals(u)) {
            return 0;
        }
        else {
            return -1;
        }
    }

    //Metodo che passa da oggetto Utente a Stringa
    public String toString() {
        return " {" + username + "," + partiteGiocate + "," + vittorie + "," + punteggio + "} " ;
    }

}