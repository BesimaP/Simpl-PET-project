package entities;

import java.time.LocalDate;

// Patienten (tabel patient). Login og persondata ligger samme sted, fordi de altid oprettes sammen.
public class Patient {

    private int id;
    private String username;
    private String passwordHash;   // BCrypt-hash, aldrig klartekst
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;

    // id = 0 for en ny række – databasen giver det rigtige id ved save
    public Patient(int id, String username, String passwordHash, String firstName, String lastName, LocalDate dateOfBirth) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
    }

    // setId bruges af mapperen efter save. Resten kan kun læses.
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

    // fulde navn til visning – gemmes ikke i databasen
    public String getName() {
        return firstName + " " + lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

}
