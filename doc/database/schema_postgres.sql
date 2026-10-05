-- =========================================================
-- SIMPL – databaseskema i PostgreSQL (uge 40)
-- Kør hele scriptet på én gang i pgAdmin Query Tool (F5) på databasen "simpl".
-- 20 tabeller = de 20 kasser i domænemodellen (doc/static/Domænemodel1.puml).
-- Konventioner:
--   - primærnøgle hedder id i alle tabeller; fremmednøgler hedder <tabel>_id
--   - INT GENERATED ALWAYS AS IDENTITY = databasen giver id'et
--   - typer (fx treatment_type) har hver deres tabel (id, name); andre tabeller peger på dem med <type>_id.
--     name matcher vores enums i Java. Fremmednøglen gør, at ugyldige værdier afvises af databasen
--   - ON DELETE CASCADE: slettes patienten (kontoen), slettes alt under den (NFR2)
-- Normaliseret til 3NF: ingen gentagne grupper, ingen afledte kolonner, stamdata (medication og typerne) i egne tabeller.
-- =========================================================

-- Slet eksisterende tabeller, så scriptet kan køres igen
DROP TABLE IF EXISTS medication_log, medication, event, hormone_log, document,
    round, appointment, diary_entry, notification, fertility_journey,
    diagnosis, patient,
    journey_status, treatment_type, result, appointment_type, event_type, hormone_type, document_type, notification_type CASCADE;

-- ---------- Typer (faste værdilister) ----------
-- Hver type har sin egen tabel med id og name, ligesom medication. id'erne står i kommentaren, fordi testdata bruger dem.

CREATE TABLE journey_status (
    id   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);
INSERT INTO journey_status (name) VALUES ('ACTIVE'), ('COMPLETED');   -- 1 = ACTIVE, 2 = COMPLETED

CREATE TABLE treatment_type (
    id   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);
INSERT INTO treatment_type (name) VALUES ('IVF'), ('ICSI'), ('IUI'), ('FET');   -- 1 = IVF, 2 = ICSI, 3 = IUI, 4 = FET

CREATE TABLE result (
    id   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);
INSERT INTO result (name) VALUES ('POSITIVE'), ('NEGATIVE');   -- 1 = POSITIVE, 2 = NEGATIVE

CREATE TABLE appointment_type (
    id   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);
INSERT INTO appointment_type (name) VALUES ('CONSULTATION'), ('SCANNING'), ('BLOOD_TEST'), ('EGG_RETRIEVAL'), ('EMBRYO_TRANSFER'), ('PREGNANCY_TEST');   -- 1 = CONSULTATION, 2 = SCANNING, 3 = BLOOD_TEST, 4 = EGG_RETRIEVAL, 5 = EMBRYO_TRANSFER, 6 = PREGNANCY_TEST

CREATE TABLE event_type (
    id   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);
INSERT INTO event_type (name) VALUES ('STIMULATION_START'), ('EGG_RETRIEVAL'), ('FERTILISATION'), ('EMBRYO_TRANSFER'), ('PREGNANCY_TEST');   -- 1 = STIMULATION_START, 2 = EGG_RETRIEVAL, 3 = FERTILISATION, 4 = EMBRYO_TRANSFER, 5 = PREGNANCY_TEST

CREATE TABLE hormone_type (
    id   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);
INSERT INTO hormone_type (name) VALUES ('FSH'), ('LH'), ('E2_OESTRADIOL'), ('PROGESTERONE'), ('AMH');   -- 1 = FSH, 2 = LH, 3 = E2_OESTRADIOL, 4 = PROGESTERONE, 5 = AMH

CREATE TABLE document_type (
    id   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);
INSERT INTO document_type (name) VALUES ('REFERRAL'), ('BLOOD_TEST_RESULT'), ('TREATMENT_PLAN'), ('OTHER');   -- 1 = REFERRAL, 2 = BLOOD_TEST_RESULT, 3 = TREATMENT_PLAN, 4 = OTHER

CREATE TABLE notification_type (
    id   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);
INSERT INTO notification_type (name) VALUES ('MEDICATION_REMINDER'), ('APPOINTMENT_REMINDER');   -- 1 = MEDICATION_REMINDER, 2 = APPOINTMENT_REMINDER

-- ---------- Patient ----------

-- Patient — login og persondata i én tabel (UserAccount og Patient er samlet: de var 1-til-1 og oprettes altid sammen).
-- username UNIQUE = "brugernavn er taget" håndhæves af databasen.
CREATE TABLE patient (
    id            INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,         -- gem et hash (BCrypt), aldrig klartekst
    first_name    VARCHAR(50)  NOT NULL,
    last_name     VARCHAR(50)  NOT NULL,
    date_of_birth DATE NOT NULL
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

-- Document — ligger på patienten (ikke runden): fx henvisning og blodprøvesvar kommer før første runde.
-- Kun stien gemmes; filen ligger på disken. upload_date gør, at dokumenter kan vises under den rigtige runde (via dato).
CREATE TABLE document (
    id            INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_id    INT NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    upload_date   DATE NOT NULL,
    title         VARCHAR(100) NOT NULL,
    document_type_id INT NOT NULL REFERENCES document_type(id),
    file_path     VARCHAR(255) NOT NULL
);

-- Notification — påmindelser genereret af systemet.
CREATE TABLE notification (
    id                INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_id        INT NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    date_time         TIMESTAMP NOT NULL,
    notification_type_id INT NOT NULL REFERENCES notification_type(id),
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
    journey_status_id INT NOT NULL DEFAULT 1 REFERENCES journey_status(id)   -- 1 = ACTIVE
);

-- Højst ét ACTIVE forløb pr. patient (noten i domænemodellen) – et partielt unikt indeks håndhæver reglen i databasen
CREATE UNIQUE INDEX one_active_journey_per_patient
    ON fertility_journey(patient_id)
    WHERE journey_status_id = 1;   -- 1 = ACTIVE

-- Round — ét behandlingsforsøg i et forløb. end_date og result_id er NULL, indtil runden afsluttes.
-- Ingen status-kolonne: en runde er i gang, så længe end_date er NULL (status ville være en afledt kolonne).
-- UNIQUE (fertility_journey_id, round_number): rundenummeret kan ikke gentages i samme forløb.
CREATE TABLE round (
    id                   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fertility_journey_id INT NOT NULL REFERENCES fertility_journey(id) ON DELETE CASCADE,
    round_number         INT NOT NULL,
    treatment_type_id    INT NOT NULL REFERENCES treatment_type(id),
    start_date           DATE NOT NULL,
    end_date             DATE,
    result_id            INT REFERENCES result(id),   -- NULL indtil runden afsluttes
    UNIQUE (fertility_journey_id, round_number)
);

-- Appointment — ligger på forløbet (første konsultation sker før nogen runde).
CREATE TABLE appointment (
    id                   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fertility_journey_id INT NOT NULL REFERENCES fertility_journey(id) ON DELETE CASCADE,
    date_time            TIMESTAMP NOT NULL,
    appointment_type_id  INT NOT NULL REFERENCES appointment_type(id),
    location             VARCHAR(100) NOT NULL
);

-- ---------- Tilknyttet runden ----------

-- Event — trin i runden, bygger tidslinjen. Oprettes automatisk af systemet.
CREATE TABLE event (
    id          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    round_id    INT NOT NULL REFERENCES round(id) ON DELETE CASCADE,
    date_time   TIMESTAMP NOT NULL,
    event_type_id INT NOT NULL REFERENCES event_type(id),
    description TEXT
);

-- HormoneLog — en måling i en runde. unit gemmes per måling: klinikker bruger forskellige enheder.
CREATE TABLE hormone_log (
    id           INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    round_id     INT NOT NULL REFERENCES round(id) ON DELETE CASCADE,
    date_time    TIMESTAMP NOT NULL,
    hormone_type_id INT NOT NULL REFERENCES hormone_type(id),
    value        NUMERIC(10,2) NOT NULL,
    unit         VARCHAR(10) NOT NULL
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
