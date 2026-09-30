-- ============================================================
-- schema_postgres.sql — Simpl databaseskema i PostgreSQL (uge 40)
-- Samme 13 tabeller som SQLite-udgaven (src/main/resources/data/schema.sql) og domænemodellen
-- (doc/static/Domænemodel1.puml) – men med rigtige PostgreSQL-typer:
--   INTEGER PRIMARY KEY AUTOINCREMENT  ->  SERIAL PRIMARY KEY
--   TEXT-datoer                        ->  DATE / TIMESTAMP
--   INTEGER 0/1                        ->  BOOLEAN
--   REAL                               ->  NUMERIC(10,2)
--   INSERT OR IGNORE                   ->  INSERT ... ON CONFLICT DO NOTHING
-- Kør hele filen i pgAdmin (Query Tool) på en tom database "simpl".
-- Normaliseret (3NF): ingen gentagne grupper, ingen afledte kolonner, medicin-stamdata i egen tabel.
-- ============================================================

-- Login-oplysninger, adskilt fra persondata. username UNIQUE = "brugernavn er taget" håndhæves af databasen.
CREATE TABLE user_account (
    id            SERIAL PRIMARY KEY,
    username      VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
);

-- Patient — persondata. user_account_id UNIQUE = 1–1 mellem konto og patient.
CREATE TABLE patient (
    id              SERIAL PRIMARY KEY,
    user_account_id INTEGER NOT NULL UNIQUE REFERENCES user_account(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    date_of_birth   DATE NOT NULL
);

-- Diagnosis — patientens egne diagnoser (1–mange).
CREATE TABLE diagnosis (
    id          SERIAL PRIMARY KEY,
    patient_id  INTEGER NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    description TEXT
);

-- FertilityJourney — det overordnede forløb. Højst ét ACTIVE pr. patient (håndhæves i service-laget).
CREATE TABLE fertility_journey (
    id         SERIAL PRIMARY KEY,
    patient_id INTEGER NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    status     VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'COMPLETED'))
);

-- Round — ét behandlingsforsøg i et forløb. end_date og result er NULL, indtil runden afsluttes.
CREATE TABLE round (
    id                   SERIAL PRIMARY KEY,
    fertility_journey_id INTEGER NOT NULL REFERENCES fertility_journey(id) ON DELETE CASCADE,
    round_number         INTEGER NOT NULL,
    treatment_type       VARCHAR(10) NOT NULL CHECK (treatment_type IN ('IVF', 'ICSI', 'IUI', 'FET')),
    start_date           DATE NOT NULL,
    end_date             DATE,
    status               VARCHAR(20) NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    result               VARCHAR(10) CHECK (result IN ('POSITIVE', 'NEGATIVE'))
);

-- Appointment — ligger på forløbet (første konsultation sker før nogen runde).
CREATE TABLE appointment (
    id                   SERIAL PRIMARY KEY,
    fertility_journey_id INTEGER NOT NULL REFERENCES fertility_journey(id) ON DELETE CASCADE,
    date_time            TIMESTAMP NOT NULL,
    appointment_type     VARCHAR(20) NOT NULL CHECK (appointment_type IN ('CONSULTATION', 'SCANNING', 'BLOOD_TEST', 'EGG_RETRIEVAL', 'EMBRYO_TRANSFER', 'PREGNANCY_TEST')),
    location             VARCHAR(100) NOT NULL
);

-- DiaryEntry — private noter. Ligger på PATIENTEN, ikke forløbet: dagbog skal kunne bruges før, mellem og efter forløb.
CREATE TABLE diary_entry (
    id         SERIAL PRIMARY KEY,
    patient_id INTEGER NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    date_time  TIMESTAMP NOT NULL,
    title      VARCHAR(100) NOT NULL,
    content    TEXT NOT NULL
);

-- Event — trin i runden, bygger tidslinjen. Oprettes automatisk (start runde, ægudtagning, oplægning, graviditetstest).
CREATE TABLE event (
    id          SERIAL PRIMARY KEY,
    round_id    INTEGER NOT NULL REFERENCES round(id) ON DELETE CASCADE,
    date_time   TIMESTAMP NOT NULL,
    event_type  VARCHAR(20) NOT NULL CHECK (event_type IN ('STIMULATION_START', 'EGG_RETRIEVAL', 'FERTILISATION', 'EMBRYO_TRANSFER', 'PREGNANCY_TEST')),
    description TEXT
);

-- Medication — stamdata (normalisering: navnet står ét sted, ikke i hver log-række).
CREATE TABLE medication (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(100)
);

INSERT INTO medication (name, description) VALUES
    ('GONAL_F', 'Gonal-F'),
    ('ORGALUTRAN', 'Orgalutran'),
    ('MENOPUR', 'Menopur'),
    ('OVITRELLE', 'Ovitrelle')
ON CONFLICT (name) DO NOTHING;

-- MedicationLog — én planlagt dosis i en runde. medication ON DELETE RESTRICT: stamdata kan ikke slettes, mens de bruges.
CREATE TABLE medication_log (
    id                  SERIAL PRIMARY KEY,
    round_id            INTEGER NOT NULL REFERENCES round(id) ON DELETE CASCADE,
    medication_id       INTEGER NOT NULL REFERENCES medication(id) ON DELETE RESTRICT,
    scheduled_date_time TIMESTAMP NOT NULL,
    dose                NUMERIC(10,2) NOT NULL,
    unit                VARCHAR(10) NOT NULL,
    taken               BOOLEAN NOT NULL DEFAULT FALSE
);

-- HormoneLog — en måling i en runde.
CREATE TABLE hormone_log (
    id           SERIAL PRIMARY KEY,
    round_id     INTEGER NOT NULL REFERENCES round(id) ON DELETE CASCADE,
    date_time    TIMESTAMP NOT NULL,
    hormone_type VARCHAR(20) NOT NULL CHECK (hormone_type IN ('FSH', 'LH', 'E2_OESTRADIOL', 'PROGESTERONE', 'AMH')),
    value        NUMERIC(10,2) NOT NULL,
    unit         VARCHAR(10) NOT NULL
);

-- Document — dokumenter på en runde. Kun stien gemmes; filen ligger på disken.
CREATE TABLE document (
    id            SERIAL PRIMARY KEY,
    round_id      INTEGER NOT NULL REFERENCES round(id) ON DELETE CASCADE,
    title         VARCHAR(100) NOT NULL,
    document_type VARCHAR(20) NOT NULL CHECK (document_type IN ('BLOOD_TEST_RESULT', 'TREATMENT_PLAN', 'OTHER')),
    file_path     VARCHAR(255) NOT NULL
);

-- Notification — påmindelser genereret af systemet. Ligger på patienten.
CREATE TABLE notification (
    id                SERIAL PRIMARY KEY,
    patient_id        INTEGER NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    date_time         TIMESTAMP NOT NULL,
    notification_type VARCHAR(30) NOT NULL CHECK (notification_type IN ('MEDICATION_REMINDER', 'APPOINTMENT_REMINDER')),
    title             VARCHAR(100) NOT NULL,
    message           TEXT NOT NULL,
    is_read           BOOLEAN NOT NULL DEFAULT FALSE
);
