-- =========================================================
-- Simpl – databaseskema til PostgreSQL (uge 40)
-- Køres som ét script på databasen "Simpl" (med stort S).
-- 20 tabeller, én for hver klasse i domænemodellen (doc/static/Domænemodel1.puml).
-- Vi har brugt de her regler:
--   - primærnøglen hedder id, fremmednøgler hedder <tabel>_id
--   - id'er laves af databasen (GENERATED ALWAYS AS IDENTITY)
--   - typerne (fx treatment_type) har egne tabeller, og name passer med vores enums i Java,
--     så databasen selv afviser ugyldige værdier
--   - ON DELETE CASCADE: sletter man patienten, forsvinder alle data under den (NFR2)
-- Normaliseret til 3NF.
-- =========================================================

-- Sletter tabellerne først, så scriptet kan køres igen. CASCADE tager også viewet round_overview med
DROP TABLE IF EXISTS medication_log, medication, event, hormone_log, document,
    round, appointment, diary_entry, notification, fertility_journey,
    diagnosis, patient,
    journey_status, treatment_type, result, appointment_type, event_type, hormone_type, document_type, notification_type CASCADE;

-- ---------- Typer (faste værdilister) ----------
-- id'erne står ved hver INSERT, fordi testdata bruger dem.

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

-- Patient – login og persondata. Vi har slået UserAccount og Patient sammen, fordi de var 1-til-1
-- og altid blev oprettet sammen.
CREATE TABLE patient (
    id            INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,         -- BCrypt-hash, aldrig klartekst
    first_name    VARCHAR(50)  NOT NULL,
    last_name     VARCHAR(50)  NOT NULL,
    date_of_birth DATE NOT NULL
);

-- Unikt på LOWER(username), så "Anna" og "anna" ikke kan oprettes begge to.
-- PatientMapper.findByUsername bruger også LOWER, så login ikke skelner mellem store og små bogstaver
CREATE UNIQUE INDEX patient_username_unique ON patient (LOWER(username));

-- ---------- Hører til patienten ----------

-- Diagnosis – patientens diagnoser (1-til-mange).
CREATE TABLE diagnosis (
    id          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_id  INT NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    description TEXT
);

-- DiaryEntry – private noter. Ligger på patienten og ikke forløbet, så dagbogen også kan bruges før og mellem forløb.
CREATE TABLE diary_entry (
    id         INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_id INT NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    date_time  TIMESTAMP NOT NULL,
    title      VARCHAR(100) NOT NULL,
    content    TEXT NOT NULL
);

-- Document – ligger på patienten, fordi fx henvisning og blodprøvesvar kommer før første runde.
-- Vi gemmer kun stien, selve filen ligger på disken. upload_date bruges til at vise dokumentet under den rigtige runde.
CREATE TABLE document (
    id            INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_id    INT NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    upload_date   DATE NOT NULL,
    title         VARCHAR(100) NOT NULL,
    document_type_id INT NOT NULL REFERENCES document_type(id),
    file_path     VARCHAR(255) NOT NULL
);

-- Notification – påmindelser, som systemet laver.
CREATE TABLE notification (
    id                INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_id        INT NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    date_time         TIMESTAMP NOT NULL,
    notification_type_id INT NOT NULL REFERENCES notification_type(id),
    title             VARCHAR(100) NOT NULL,
    message           TEXT NOT NULL,
    is_read           BOOLEAN NOT NULL DEFAULT FALSE
);

-- ---------- Forløb og runde ----------

-- FertilityJourney – hele forløbet hos klinikken fra første kontakt.
CREATE TABLE fertility_journey (
    id         INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_id INT NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    journey_status_id INT NOT NULL DEFAULT 1 REFERENCES journey_status(id)   -- 1 = ACTIVE
);

-- Højst ét aktivt forløb pr. patient (noten i domænemodellen). Det partielle indeks gør, at databasen selv håndhæver det
CREATE UNIQUE INDEX one_active_journey_per_patient
    ON fertility_journey(patient_id)
    WHERE journey_status_id = 1;   -- 1 = ACTIVE

-- Round – ét behandlingsforsøg. end_date og result_id er NULL, indtil runden afsluttes.
-- Vi har ikke en status-kolonne: runden er i gang, så længe end_date er NULL (ellers en afledt kolonne).
CREATE TABLE round (
    id                   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fertility_journey_id INT NOT NULL REFERENCES fertility_journey(id) ON DELETE CASCADE,
    round_number         INT NOT NULL,
    treatment_type_id    INT NOT NULL REFERENCES treatment_type(id),
    start_date           DATE NOT NULL,
    end_date             DATE,
    result_id            INT REFERENCES result(id),   -- NULL indtil runden afsluttes
    UNIQUE (fertility_journey_id, round_number),
    CHECK (round_number > 0),                              -- runder tælles fra 1
    CHECK (end_date IS NULL OR end_date >= start_date)     -- en runde kan ikke slutte, før den starter
);

-- Højst én runde i gang pr. forløb, samme idé som indekset ovenfor
CREATE UNIQUE INDEX one_active_round_per_journey
    ON round(fertility_journey_id)
    WHERE end_date IS NULL;   -- NULL = runden er i gang

-- round_overview samler runden med navnene på behandlingstype og resultat, så RoundMapper ikke skal joine hver gang.
-- LEFT JOIN på result, fordi result_id er NULL mens runden er i gang - ellers ville de runder ikke komme med.
CREATE VIEW round_overview AS
SELECT round.id,
       round.fertility_journey_id,
       round.round_number,
       treatment_type.name AS treatment_type,
       round.start_date,
       round.end_date,
       result.name AS result
FROM round
JOIN treatment_type ON round.treatment_type_id = treatment_type.id
LEFT JOIN result ON round.result_id = result.id;

-- Appointment – ligger på forløbet, fordi første konsultation er før første runde.
CREATE TABLE appointment (
    id                   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fertility_journey_id INT NOT NULL REFERENCES fertility_journey(id) ON DELETE CASCADE,
    date_time            TIMESTAMP NOT NULL,
    appointment_type_id  INT NOT NULL REFERENCES appointment_type(id),
    location             VARCHAR(100) NOT NULL
);

-- ---------- Hører til runden ----------

-- Event – trin i runden, bruges til tidslinjen. Systemet opretter dem selv.
CREATE TABLE event (
    id          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    round_id    INT NOT NULL REFERENCES round(id) ON DELETE CASCADE,
    date_time   TIMESTAMP NOT NULL,
    event_type_id INT NOT NULL REFERENCES event_type(id),
    description TEXT
);

-- HormoneLog – én måling i en runde. unit gemmes pr. måling, fordi klinikker bruger forskellige enheder.
CREATE TABLE hormone_log (
    id           INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    round_id     INT NOT NULL REFERENCES round(id) ON DELETE CASCADE,
    date_time    TIMESTAMP NOT NULL,
    hormone_type_id INT NOT NULL REFERENCES hormone_type(id),
    value        NUMERIC(10,2) NOT NULL CHECK (value >= 0),   -- en hormonværdi kan ikke være negativ
    unit         VARCHAR(10) NOT NULL
        CHECK (unit IN ('pmol/L', 'IU/L', 'nmol/L', 'pg/mL', 'ng/mL', 'mIU/mL'))   -- samme liste som UNITS i HormoneService
);

-- ---------- Medicin ----------

-- Medication – stamdata, så navn og enhed kun står ét sted (3NF). Hører ikke til en patient.
-- unit ligger her, fordi enheden er fast pr. præparat (svar fra fertilitetsklinik, okt. 2026). Det er kun dosis, der varierer.
CREATE TABLE medication (
    id          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(100),
    unit        VARCHAR(10)  NOT NULL
);

-- De fire første skal blive på id 1-4, ellers passer medication_id i testdata ikke.
INSERT INTO medication (name, description, unit) VALUES
    ('GONAL_F',    'Gonal-F',    'IU'),   -- 1  stimulering
    ('ORGALUTRAN', 'Orgalutran', 'mg'),   -- 2  forhindrer ægløsning (0,25 mg)
    ('MENOPUR',    'Menopur',    'IU'),   -- 3  stimulering
    ('OVITRELLE',  'Ovitrelle',  'µg'),   -- 4  ægløsningssprøjte (250 µg)
    ('PUREGON',    'Puregon',    'IU'),   -- 5  stimulering
    ('PERGOVERIS', 'Pergoveris', 'IU'),   -- 6  stimulering
    ('REKOVELLE',  'Rekovelle',  'µg'),   -- 7  stimulering
    ('ELONVA',     'Elonva',     'µg'),   -- 8  stimulering (langtidsvirkende)
    ('FYREMADEL',  'Fyremadel',  'mg'),   -- 9  forhindrer ægløsning (0,25 mg)
    ('CETROTIDE',  'Cetrotide',  'mg'),   -- 10 forhindrer ægløsning (0,25 mg)
    ('PREGNYL',    'Pregnyl',    'IU'),   -- 11 ægløsningssprøjte (5000 IE)
    ('LUTINUS',    'Lutinus',    'mg'),   -- 12 progesteron (100 mg)
    ('CRINONE',    'Crinone',    'mg'),   -- 13 progesteron (gel, 90 mg)
    ('CYCLOGEST',  'Cyclogest',  'mg'),   -- 14 progesteron (400 mg)
    ('LETROZOL',   'Letrozol',   'mg'),   -- 15 tablet (2,5 mg)
    ('PERGOTIME',  'Pergotime',  'mg'),   -- 16 tablet, clomifen (50 mg)
    ('ESTRADIOL',  'Estradiol',  'mg');   -- 17 tablet (2 mg)


-- MedicationLog – én planlagt dosis i en runde.
-- RESTRICT på medication: et præparat kan ikke slettes, så længe det bliver brugt (association, ikke komposition).
CREATE TABLE medication_log (
    id                  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    round_id            INT NOT NULL REFERENCES round(id) ON DELETE CASCADE,
    medication_id       INT NOT NULL REFERENCES medication(id) ON DELETE RESTRICT,
    scheduled_date_time TIMESTAMP NOT NULL,
    dose                NUMERIC(10,2) NOT NULL CHECK (dose > 0),   -- en dosis skal være større end 0
    taken               BOOLEAN NOT NULL DEFAULT FALSE
);

-- ---------- Indekser på fremmednøgler ----------
-- PostgreSQL laver selv indeks på PRIMARY KEY og UNIQUE, men ikke på fremmednøgler.
-- Vores mappers søger næsten altid på fremmednøglen, og ON DELETE CASCADE skal også finde rækkerne.
-- round mangler med vilje, fordi UNIQUE (fertility_journey_id, round_number) allerede giver et indeks.
CREATE INDEX fertility_journey_patient_id_idx ON fertility_journey (patient_id);   -- det partielle indeks dækker kun aktive forløb
CREATE INDEX diagnosis_patient_id_idx         ON diagnosis (patient_id);
CREATE INDEX diary_entry_patient_id_idx       ON diary_entry (patient_id);
CREATE INDEX document_patient_id_idx          ON document (patient_id);
CREATE INDEX notification_patient_id_idx      ON notification (patient_id);
CREATE INDEX appointment_journey_id_idx       ON appointment (fertility_journey_id);
CREATE INDEX event_round_id_idx               ON event (round_id);
CREATE INDEX hormone_log_round_id_idx         ON hormone_log (round_id);
CREATE INDEX medication_log_round_id_idx      ON medication_log (round_id);
