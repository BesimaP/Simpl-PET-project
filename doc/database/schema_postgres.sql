-- =========================================================
-- SIMPL – databaseskema i PostgreSQL (uge 40)
-- Kør hele scriptet på én gang i pgAdmin Query Tool (F5) på databasen "simpl".
-- 13 tabeller = de 13 entiteter i domænemodellen (doc/static/Domænemodel1.puml).
-- Konventioner:
--   - primærnøgle hedder id i alle tabeller; fremmednøgler hedder <tabel>_id
--   - INT GENERATED ALWAYS AS IDENTITY = databasen giver id'et
--   - CHECK (… IN (…)) matcher vores enums, så ugyldige værdier afvises af databasen
--   - ON DELETE CASCADE: slettes kontoen, slettes alt under patienten (NFR2)
-- Normaliseret til 3NF: ingen gentagne grupper, ingen afledte kolonner, stamdata (medication) i egen tabel.
-- =========================================================

-- Slet eksisterende tabeller, så scriptet kan køres igen
DROP TABLE IF EXISTS medication_log, medication, event, hormone_log, document,
    round, appointment, diary_entry, notification, fertility_journey,
    diagnosis, patient, user_account CASCADE;

-- ---------- Bruger og patient ----------

-- Login-oplysninger, adskilt fra persondata. username UNIQUE = "brugernavn er taget" håndhæves af databasen.
CREATE TABLE user_account (
    id            INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL          -- gem et hash (BCrypt), aldrig klartekst
);

-- Patient — persondata. user_account_id UNIQUE = 1-til-1 mellem konto og patient.
CREATE TABLE patient (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_account_id INT NOT NULL UNIQUE REFERENCES user_account(id) ON DELETE CASCADE,
    first_name      VARCHAR(50) NOT NULL,
    last_name       VARCHAR(50) NOT NULL,
    date_of_birth   DATE NOT NULL
);

-- ---------- Tilknyttet patienten (alt om PERSONEN) ----------

-- Diagnosis — patientens egne diagnoser (1–mange).
CREATE TABLE diagnosis (
    id          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_id  INT NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    description TEXT
);

-- DiaryEntry — private noter. Ligger på patienten (ikke forløbet): dagbog kan bruges før, mellem og efter forløb.
CREATE TABLE diary_entry (
    id         INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_id INT NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    date_time  TIMESTAMP NOT NULL,
    title      VARCHAR(100) NOT NULL,
    content    TEXT NOT NULL
);

-- Notification — påmindelser genereret af systemet.
CREATE TABLE notification (
    id                INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_id        INT NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    date_time         TIMESTAMP NOT NULL,
    notification_type VARCHAR(30) NOT NULL CHECK (notification_type IN ('MEDICATION_REMINDER', 'APPOINTMENT_REMINDER')),
    title             VARCHAR(100) NOT NULL,
    message           TEXT NOT NULL,
    is_read           BOOLEAN NOT NULL DEFAULT FALSE
);

-- ---------- Forløb og runde (alt om BEHANDLINGEN) ----------

-- FertilityJourney — det samlede forløb hos klinikken, fra første kontakt.
CREATE TABLE fertility_journey (
    id         INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_id INT NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'COMPLETED'))
);

-- Højst ét ACTIVE forløb pr. patient (noten i domænemodellen) – et partielt unikt indeks håndhæver reglen i databasen
CREATE UNIQUE INDEX one_active_journey_per_patient
    ON fertility_journey(patient_id)
    WHERE status = 'ACTIVE';

-- Round — ét behandlingsforsøg i et forløb. end_date og result er NULL, indtil runden afsluttes.
-- UNIQUE (fertility_journey_id, round_number): rundenummeret kan ikke gentages i samme forløb.
CREATE TABLE round (
    id                   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fertility_journey_id INT NOT NULL REFERENCES fertility_journey(id) ON DELETE CASCADE,
    round_number         INT NOT NULL,
    treatment_type       VARCHAR(10) NOT NULL CHECK (treatment_type IN ('IVF', 'ICSI', 'IUI', 'FET')),
    start_date           DATE NOT NULL,
    end_date             DATE,
    status               VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    result               VARCHAR(10) CHECK (result IN ('POSITIVE', 'NEGATIVE')),
    UNIQUE (fertility_journey_id, round_number)
);

-- Appointment — ligger på forløbet (første konsultation sker før nogen runde).
CREATE TABLE appointment (
    id                   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fertility_journey_id INT NOT NULL REFERENCES fertility_journey(id) ON DELETE CASCADE,
    date_time            TIMESTAMP NOT NULL,
    appointment_type     VARCHAR(20) NOT NULL CHECK (appointment_type IN ('CONSULTATION', 'SCANNING', 'BLOOD_TEST', 'EGG_RETRIEVAL', 'EMBRYO_TRANSFER', 'PREGNANCY_TEST')),
    location             VARCHAR(100) NOT NULL
);

-- ---------- Tilknyttet runden ----------

-- Event — trin i runden, bygger tidslinjen. Oprettes automatisk af systemet.
CREATE TABLE event (
    id          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    round_id    INT NOT NULL REFERENCES round(id) ON DELETE CASCADE,
    date_time   TIMESTAMP NOT NULL,
    event_type  VARCHAR(20) NOT NULL CHECK (event_type IN ('STIMULATION_START', 'EGG_RETRIEVAL', 'FERTILISATION', 'EMBRYO_TRANSFER', 'PREGNANCY_TEST')),
    description TEXT
);

-- HormoneLog — en måling i en runde. unit gemmes per måling: klinikker bruger forskellige enheder.
CREATE TABLE hormone_log (
    id           INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    round_id     INT NOT NULL REFERENCES round(id) ON DELETE CASCADE,
    date_time    TIMESTAMP NOT NULL,
    hormone_type VARCHAR(20) NOT NULL CHECK (hormone_type IN ('FSH', 'LH', 'E2_OESTRADIOL', 'PROGESTERONE', 'AMH')),
    value        NUMERIC(10,2) NOT NULL,
    unit         VARCHAR(10) NOT NULL
);

-- Document — kun stien gemmes; filen ligger på disken.
CREATE TABLE document (
    id            INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    round_id      INT NOT NULL REFERENCES round(id) ON DELETE CASCADE,
    title         VARCHAR(100) NOT NULL,
    document_type VARCHAR(20) NOT NULL CHECK (document_type IN ('BLOOD_TEST_RESULT', 'TREATMENT_PLAN', 'OTHER')),
    file_path     VARCHAR(255) NOT NULL
);

-- ---------- Medicin ----------

-- Medication — stamdata (3NF: navnet står ét sted, ikke i hver log-række). Tilhører ingen patient.
CREATE TABLE medication (
    id          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(100)
);

INSERT INTO medication (name, description) VALUES
    ('GONAL_F',    'Gonal-F'),
    ('ORGALUTRAN', 'Orgalutran'),
    ('MENOPUR',    'Menopur'),
    ('OVITRELLE',  'Ovitrelle');

-- MedicationLog — én planlagt dosis i en runde.
-- medication ON DELETE RESTRICT: stamdata kan ikke slettes, mens de bruges (association, ikke komposition).
CREATE TABLE medication_log (
    id                  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    round_id            INT NOT NULL REFERENCES round(id) ON DELETE CASCADE,
    medication_id       INT NOT NULL REFERENCES medication(id) ON DELETE RESTRICT,
    scheduled_date_time TIMESTAMP NOT NULL,
    dose                NUMERIC(10,2) NOT NULL,
    unit                VARCHAR(10) NOT NULL,
    taken               BOOLEAN NOT NULL DEFAULT FALSE
);
