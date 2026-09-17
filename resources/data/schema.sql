-- ============================================================
-- schema.sql — Simpl databaseskema (2. semester)
-- Afspejler domænemodellen 1:1 (doc/static/Domænemodel1.puml):
--   hver kasse = en tabel, hver attribut = en kolonne,
--   hver streg = en *_id-kolonne med FOREIGN KEY på "mange"-siden.
-- Konventioner:
--   - tabel- og kolonnenavne i snake_case
--   - id INTEGER PRIMARY KEY AUTOINCREMENT på alle tabeller (teknisk nøgle, ikke i domænemodellen)
--   - datoer og tidspunkter gemmes som ISO-tekst (fx 2026-08-27, 2026-08-27T14:30) — sorterer korrekt i SQLite
--   - ON DELETE CASCADE på alle fremmednøgler, så sletning af en konto fjerner alle patientens data (NFR2)
-- Filen læses og køres af DatabaseInitializer ved opstart.
-- CREATE TABLE IF NOT EXISTS: ændres skemaet, slettes simpl.db og bygges forfra.
-- ============================================================

-- Login-oplysninger, adskilt fra persondata.
-- password_hash: adgangskoden gemmes aldrig i klartekst (NFR1).
-- username UNIQUE: databasen håndhæver "brugernavn er taget" (US6a).
CREATE TABLE IF NOT EXISTS user_account (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL
);


-- Patient — persondata, adskilt fra login.
-- user_account_id UNIQUE: én konto hører til præcis én patient (1–1 i domænemodellen).
-- ON DELETE CASCADE: slettes kontoen, slettes patienten (og via CASCADE alt under den — NFR2).
CREATE TABLE IF NOT EXISTS patient (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_account_id INTEGER NOT NULL UNIQUE,
    name            TEXT NOT NULL,
    date_of_birth   TEXT NOT NULL,
    FOREIGN KEY (user_account_id) REFERENCES user_account(id) ON DELETE CASCADE
);

-- Diagnosis — patientens egne registrerede diagnoser (US7).
-- patient_id uden UNIQUE: én patient kan have flere diagnoser (1–mange i domænemodellen).
-- description må være tom — navnet alene er nok.
CREATE TABLE IF NOT EXISTS diagnosis (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    patient_id  INTEGER NOT NULL,
    name        TEXT NOT NULL,
    description TEXT,
    FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE CASCADE
);

-- FertilityJourney — patientens overordnede forløb (US1). Kan indeholde flere runder.
-- status: ACTIVE eller COMPLETED — CHECK afviser alle andre værdier.
-- Forretningsregel "højst ét ACTIVE forløb pr. patient" håndhæves i service-laget, ikke her.
CREATE TABLE IF NOT EXISTS fertility_journey (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    patient_id INTEGER NOT NULL,
    start_date TEXT NOT NULL,
    status     TEXT NOT NULL CHECK (status IN ('ACTIVE', 'COMPLETED')),
    FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE CASCADE
);

-- Round — ét behandlingsforsøg i et forløb (US10a/10b). Ny i v2: tidligere lå rundedata i fertility_journey.
-- treatment_type: IVF, ICSI, IUI eller FET (ordlisten).
-- status: IN_PROGRESS ved start, COMPLETED når runden afsluttes (UC8/UC14).
-- end_date og result er NULL, indtil runden afsluttes — så "tomt resultat" er ægte tomt, ikke 'PENDING'.
-- Forretningsregel "højst én IN_PROGRESS runde pr. forløb" håndhæves i service-laget.
CREATE TABLE IF NOT EXISTS round (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    fertility_journey_id INTEGER NOT NULL,
    round_number        INTEGER NOT NULL,
    treatment_type      TEXT NOT NULL CHECK (treatment_type IN ('IVF', 'ICSI', 'IUI', 'FET')),
    start_date          TEXT NOT NULL,
    end_date            TEXT,
    status              TEXT NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    result              TEXT CHECK (result IN ('POSITIVE', 'NEGATIVE')),
    FOREIGN KEY (fertility_journey_id) REFERENCES fertility_journey(id) ON DELETE CASCADE
);

-- ---------- Ting på forløbet (US3, US4) ----------

-- Appointment — aftaler ligger på forløbet, ikke runden: første konsultation sker før nogen runde.
-- date_time som ISO-tekst (2026-09-01T10:30) — sorterer korrekt med ORDER BY.
CREATE TABLE IF NOT EXISTS appointment (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    fertility_journey_id INTEGER NOT NULL,
    date_time            TEXT NOT NULL,
    appointment_type     TEXT NOT NULL CHECK (appointment_type IN ('CONSULTATION', 'SCANNING', 'BLOOD_TEST', 'EGG_RETRIEVAL', 'EMBRYO_TRANSFER', 'PREGNANCY_TEST')),
    location             TEXT NOT NULL,
    FOREIGN KEY (fertility_journey_id) REFERENCES fertility_journey(id) ON DELETE CASCADE
);

-- DiaryEntry — patientens private noter, knyttet til forløbet (ikke en runde).
-- content NOT NULL: UC6 afviser tomme noter.
CREATE TABLE IF NOT EXISTS diary_entry (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    fertility_journey_id INTEGER NOT NULL,
    date_time            TEXT NOT NULL,
    title                TEXT NOT NULL,
    content              TEXT NOT NULL,
    FOREIGN KEY (fertility_journey_id) REFERENCES fertility_journey(id) ON DELETE CASCADE
);

-- ---------- Ting på runden (US2, US8, US9, US11) ----------

-- Event — et konkret trin i runden, bygger tidslinjen (UC11).
-- Oprettes af patienten eller automatisk ved start/slut af runde (UC8/UC14).
CREATE TABLE IF NOT EXISTS event (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    round_id    INTEGER NOT NULL,
    date_time   TEXT NOT NULL,
    event_type  TEXT NOT NULL CHECK (event_type IN ('STIMULATION_START', 'EGG_RETRIEVAL', 'FERTILISATION', 'EMBRYO_TRANSFER', 'PREGNANCY_TEST')),
    description TEXT,
    FOREIGN KEY (round_id) REFERENCES round(id) ON DELETE CASCADE
);

-- Medication — stamdata for et lægemiddel, genbruges på tværs af registreringer og patienter.
-- Ingen fremmednøgle: medicin tilhører ikke en patient. name UNIQUE, så "Gonal-F" kun findes én gang.
CREATE TABLE IF NOT EXISTS medication (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL UNIQUE,
    description TEXT
);

-- MedicationLog — én planlagt dosis af en Medication i en runde (US8).
-- taken: 0 = ikke taget, 1 = taget (SQLite har ingen boolean).
-- medication_id ON DELETE RESTRICT: en medicin kan ikke slettes, mens der findes registreringer af den.
CREATE TABLE IF NOT EXISTS medication_log (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    round_id            INTEGER NOT NULL,
    medication_id       INTEGER NOT NULL,
    scheduled_date_time TEXT NOT NULL,
    dose                REAL NOT NULL,
    unit                TEXT NOT NULL,
    taken               INTEGER NOT NULL DEFAULT 0 CHECK (taken IN (0, 1)),
    FOREIGN KEY (round_id) REFERENCES round(id) ON DELETE CASCADE,
    FOREIGN KEY (medication_id) REFERENCES medication(id) ON DELETE RESTRICT
);

-- HormoneLog — en hormonmåling i en runde (US9). value REAL, da værdier er decimaltal.
CREATE TABLE IF NOT EXISTS hormone_log (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    round_id     INTEGER NOT NULL,
    date_time    TEXT NOT NULL,
    hormone_type TEXT NOT NULL CHECK (hormone_type IN ('FSH', 'LH', 'E2_OESTRADIOL', 'PROGESTERONE', 'AMH')),
    value        REAL NOT NULL,
    unit         TEXT NOT NULL,
    FOREIGN KEY (round_id) REFERENCES round(id) ON DELETE CASCADE
);

-- Document — dokumenter på en runde (US11). Selve filen ligger på disken; kun stien gemmes.
CREATE TABLE IF NOT EXISTS document (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    round_id      INTEGER NOT NULL,
    title         TEXT NOT NULL,
    document_type TEXT NOT NULL CHECK (document_type IN ('BLOOD_TEST_RESULT', 'TREATMENT_PLAN', 'OTHER')),
    file_path     TEXT NOT NULL,
    FOREIGN KEY (round_id) REFERENCES round(id) ON DELETE CASCADE
);

-- ---------- Notifikationer (US12) ----------

-- Notification — påmindelser til patienten, genereret af systemet. Ligger på patienten, ikke forløbet.
-- is_read: 0 = ulæst, 1 = læst.
CREATE TABLE IF NOT EXISTS notification (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    patient_id        INTEGER NOT NULL,
    date_time         TEXT NOT NULL,
    notification_type TEXT NOT NULL CHECK (notification_type IN ('MEDICATION_REMINDER', 'APPOINTMENT_REMINDER')),
    title             TEXT NOT NULL,
    message           TEXT NOT NULL,
    is_read           INTEGER NOT NULL DEFAULT 0 CHECK (is_read IN (0, 1)),
    FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE CASCADE
);