package entities;

import java.time.LocalDate;

// Patienten — den centrale entitet (tabel patient). Login (brugernavn + kodeord) og persondata i ét kort
// (UserAccount er samlet ind i Patient, uge 41: de var 1-1 og blev altid oprettet sammen).
public class Patient {

    private int id;
    private String username;
    private String passwordHash;   // gem et hash (BCrypt senere), aldrig klartekst
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;

    // konstruktør: id = 0 når kortet er nyt (databasen giver det rigtige id ved save)
    public Patient(int id, String username, String passwordHash, String firstName, String lastName, LocalDate dateOfBirth) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
    }

    // gettere: læs felterne. setId bruges af mapperen efter save; resten kan ikke ændres udefra
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

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    // fulde navn – bruges i views (${patient.name}); gemmes ikke, sættes sammen her
    public String getName() {
        return firstName + " " + lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    // Redigér profil (UC2 / US6b): navn og fødselsdato kan ændres
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
}
