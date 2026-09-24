package entities;

// Login-oplysninger, adskilt fra persondata (tabel user_account).
// passwordHash: adgangskoden gemmes aldrig i klartekst (NFR1) — hashing sker i service-laget.
public class UserAccount {

    private int id;
    private String username;
    private String passwordHash;

    // konstruktør: id = 0 når kortet er nyt (databasen giver det rigtige id ved save)
    public UserAccount(int id, String username, String passwordHash) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    // gettere: læs felterne. setId bruges af DAO'en efter save; resten kan ikke ændres udefra
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
