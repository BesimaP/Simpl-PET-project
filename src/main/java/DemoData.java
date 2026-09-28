import dao.DatabaseConnection;
import dao.DatabaseInitializer;
import dao.PatientDAO;
import dao.RoundDAO;
import dao.UserAccountDAO;
import enums.Result;
import enums.ServiceResult;
import services.*;

import java.time.LocalDate;

// Lægger en realistisk demo-patient i databasen – til præsentationen. Kør den ÉN gang (Run 'DemoData.main()'), før Main startes.
// Alt går gennem services, præcis som når en bruger klikker – så det er også en test af, at hele kæden virker.
// Datoerne regnes ud fra "i dag", så demoen ser rigtig ud, uanset hvilken dag den køres.
//
//   Bruger:    mette   Kodeord: simpl1234
//
public class DemoData {

    public static void main(String[] args) {
        DatabaseInitializer.initialize();   // sikrer, at tabellerne findes

        LocalDate today = LocalDate.now();

        // 1. profil + forløb (forløbet startede for ca. 2 måneder siden)
        ServiceResult created = new AuthService().createProfile(
                "Mette Jensen", "1991-03-14", "mette", "simpl1234", "yes", today.minusDays(56).toString());
        if (created == ServiceResult.ALREADY_EXISTS) {
            System.out.println("Demo-brugeren 'mette' findes allerede – slet simpl.db, hvis du vil starte forfra.");
            return;
        }
        int accountId = new UserAccountDAO(DatabaseConnection.getConnection()).findByUsername("mette").getId();
        int patientId = new PatientDAO(DatabaseConnection.getConnection()).findByUserAccount(accountId).getId();

        // 2. diagnose
        new DiagnosisService().addDiagnosis(patientId, "Nedsat ægreserve", "AMH under normalområdet – konstateret ved første konsultation.");

        // 3. runde 1: IVF for ~7 uger siden, afsluttet negativ efter 3 uger (sat direkte via DAO, så slutdatoen ligger i fortiden)
        RoundService rounds = new RoundService();
        rounds.startRound(patientId, "IVF", today.minusDays(49).toString());
        int journeyId = 0;
        try {
            journeyId = new DashboardService().findActiveJourney(patientId).getId();
            int round1 = new RoundDAO(DatabaseConnection.getConnection()).findActiveByJourney(journeyId).getId();
            new RoundDAO(DatabaseConnection.getConnection()).endRound(round1, today.minusDays(28), Result.NEGATIVE);
        } catch (Exception e) {
            throw new RuntimeException("Kunne ikke afslutte runde 1", e);
        }

        // 4. runde 2: IVF, startede for 8 dage siden (= dag 9 i dag)
        rounds.startRound(patientId, "IVF", today.minusDays(8).toString());

        // 5. hormonmålinger i runde 2 – østradiol stiger, som den skal under stimulation
        HormoneService hormones = new HormoneService();
        hormones.saveLog(patientId, "FSH", "8.1", "IU/L", today.minusDays(8).toString());
        hormones.saveLog(patientId, "E2_OESTRADIOL", "180", "pmol/L", today.minusDays(8).toString());
        hormones.saveLog(patientId, "E2_OESTRADIOL", "410", "pmol/L", today.minusDays(5).toString());
        hormones.saveLog(patientId, "E2_OESTRADIOL", "790", "pmol/L", today.minusDays(3).toString());
        hormones.saveLog(patientId, "LH", "3.2", "IU/L", today.minusDays(3).toString());
        hormones.saveLog(patientId, "E2_OESTRADIOL", "1240", "pmol/L", today.minusDays(1).toString());

        // 6. medicin: Gonal-F hver aften siden rundens start (taget), Orgalutran om morgenen de sidste 3 dage. I dag: planlagt
        MedicationService meds = new MedicationService();
        for (int d = 8; d >= 1; d--) {
            meds.logDose(patientId, "GONAL_F", "150", "IU", today.minusDays(d).toString(), "20:00", true);
        }
        for (int d = 3; d >= 1; d--) {
            meds.logDose(patientId, "ORGALUTRAN", "0.25", "mg", today.minusDays(d).toString(), "08:00", true);
        }
        meds.logDose(patientId, "ORGALUTRAN", "0.25", "mg", today.toString(), "08:00", true);   // taget i morges
        meds.logDose(patientId, "GONAL_F", "150", "IU", today.toString(), "20:00", false);      // i aften – planlagt (giver en påmindelse)

        // 7. aftaler: to i fortiden, tre i fremtiden (ægudtagning + graviditetstest kommer også på tidslinjen)
        AppointmentService appts = new AppointmentService();
        appts.addAppointment(patientId, "CONSULTATION", "Vitanova", today.minusDays(52).toString(), "10:00");
        appts.addAppointment(patientId, "SCANNING", "Vitanova", today.minusDays(3).toString(), "09:15");
        appts.addAppointment(patientId, "SCANNING", "Vitanova", today.plusDays(2).toString(), "09:30");
        appts.addAppointment(patientId, "EGG_RETRIEVAL", "Vitanova", today.plusDays(5).toString(), "08:00");
        appts.addAppointment(patientId, "PREGNANCY_TEST", "Egen læge", today.plusDays(21).toString(), "08:30");

        // 8. dagbog
        DiaryService diary = new DiaryService();
        diary.saveEntry(patientId, today.minusDays(8).toString(), "Runde 2 er i gang", "Første sprøjte i aften. Nervøs, men klar. Denne gang tager vi det én dag ad gangen.");
        diary.saveEntry(patientId, today.minusDays(3).toString(), "Scanning", "6 follikler på højre side, 4 på venstre. Lægen var tilfreds. Lidt oppustet, ellers ok.");
        diary.saveEntry(patientId, today.minusDays(1).toString(), "Træt", "Sov dårligt. Østradiol er steget meget – det er godt, siger de. Ægudtagning om få dage.");

        System.out.println("Demo-patient oprettet: mette / simpl1234 (patientId " + patientId + ")");
    }
}
