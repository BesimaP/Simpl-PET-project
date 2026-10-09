package entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

// UNITTEST af Patient: kun ren Java-logik – ingen database. Det eneste med logik i klassen er getName().
class PatientTest {

    @Test
    void getNameCombinesFirstAndLastName() {
        Patient patient = new Patient(1, "anna", "hash1", "Anna", "Jensen", LocalDate.of(1990, 5, 1));
        assertEquals("Anna Jensen", patient.getName());   // vises fx i topbaren
    }

    @Test
    void setIdChangesId() {
        // mapperen kalder setId efter INSERT, når databasen har givet patienten et id
        Patient patient = new Patient(0, "anna", "hash1", "Anna", "Jensen", LocalDate.of(1990, 5, 1));
        patient.setId(7);
        assertEquals(7, patient.getId());
    }
}
