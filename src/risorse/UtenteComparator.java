import java.util.Comparator;

public class UtenteComparator implements Comparator<Utente> {

    public int compare(Utente u1, Utente u2){
        if(u1.getUsername().equals(u2.getUsername())){
            return u1.getPunteggio() > u2.getPunteggio() ? -1 : u1.getPunteggio() < u2.getPunteggio() ? 1 : 0;
        }else{
            return u1.getPunteggio() > u2.getPunteggio() ? -1 : u1.getPunteggio() < u2.getPunteggio() ? 1 : -1;
        }
    }
}
