-- =========================================================
-- SIMPL – databaseskema i PostgreSQL (uge 40)
-- Kør hele scriptet på én gang i pgAdmin Query Tool (F5) på databasen "Simpl" (stort S – Postgres skelner mellem store og små bogstaver i navne i anførselstegn).
-- 20 tabeller = de 20 kasser i domænemodellen (doc/static/Domænemodel1.puml).
-- Konventioner:
--   - primærnøgle hedder id i alle tabeller; fremmednøgler hedder <tabel>_id
--   - INT GENERATED ALWAYS AS IDENTITY = databasen giver id'et
--   - typer (fx treatment_type) har hver deres tabel (id, name); andre tabeller peger på dem med <type>_id.
--     name matcher vores enums i Java. Fremmednøglen gør, at ugyldige værdier afvises af databasen
--   - ON DELETE CASCADE: slettes patienten (kontoen), slettes alt under den (NFR2)
-- Normaliseret til 3NF: ingen gentagne grupper, ingen afledte kolonner, stamdata (medication og typerne) i egne tabeller.
-- =========================================================

-- Slet eksisterende tabeller, så scriptet kan køres igen (CASCADE sletter også views, der bygger på dem, fx round_overview)
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
-- "brugernavn er taget" håndhæves af databasen med et unikt indeks på LOWER(username) lige under tabellen.
CREATE TABLE patient (
    id            INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,         -- gem et hash (BCrypt), aldrig klartekst
    first_name    VARCHAR(50)  NOT NULL,
    last_name     VARCHAR(50)  NOT NULL,
    date_of_birth DATE NOT NULL
);

-- Unikt på LOWER(username) i stedet for UNIQUE på kolonnen: så kan "Anna" og "anna" ikke begge oprettes.
-- PatientMapper.findByUsername sammenligner også med LOWER, så login er ligeglad med store/små bogstaver
CREATE UNIQUE INDEX patient_username_unique ON patient (LOWER(username));

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
    UNIQUE (fertility_journey_id, round_number),
    CHECK (round_number > 0),                              -- runder tælles fra 1
    CHECK (end_date IS NULL OR end_date >= start_date)     -- en runde kan ikke slutte, før den starter
);

-- Højst én runde i gang pr. forløb – samme idé som one_active_journey_per_patient ovenfor
CREATE UNIQUE INDEX one_active_round_per_journey
    ON round(fertility_journey_id)
    WHERE end_date IS NULL;   -- NULL = runden er i gang

-- VIEW round_overview — en gemt SELECT med et navn. Indeholder ingen data selv, men kører forespørgslen hver gang.
-- Samler runden med navnene på behandlingstype og resultat, så RoundMapper ikke skal gentage joinet.
-- JOIN treatment_type: altid udfyldt (NOT NULL). LEFT JOIN result: NULL, mens runden er i gang – ellers forsvandt de runder.
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
    value        NUMERIC(10,2) NOT NULL CHECK (value >= 0),   -- en hormonværdi kan ikke være negativ
    unit         VARCHAR(10) NOT NULL
        CHECK (unit IN ('pmol/L', 'IU/L', 'nmol/L', 'pg/mL', 'ng/mL', 'mIU/mL'))   -- samme liste som UNITS i HormoneService
);

-- ---------- Medicin ----------

-- Medication — stamdata (3NF: navnet og enheden står ét sted, ikke i hver log-række). Tilhører ingen patient.
-- unit ligger her og ikke på medication_log: enheden er fast pr. præparat (svar fra fertilitetsklinik, okt 2026) –
-- det er kun dosis, der varierer fra patient til patient. Enheder: IU (internationale enheder), mg og µg (mikrogram).
CREATE TABLE medication (
    id          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(100),
    unit        VARCHAR(10)  NOT NULL
);

-- De fire første beholder id 1–4, så testdata (medication_id) stadig passer.
-- Grupperet efter brug: stimulering, forhindrer ægløsning, ægløsningssprøjte, progesteron, tabletter
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


-- MedicationLog — én planlagt dosis i en runde.
-- medication ON DELETE RESTRICT: stamdata kan ikke slettes, mens de bruges (association, ikke komposition).
CREATE TABLE medication_log (
    id                  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    round_id            INT NOT NULL REFERENCES round(id) ON DELETE CASCADE,
    medication_id       INT NOT NULL REFERENCES medication(id) ON DELETE RESTRICT,
    scheduled_date_time TIMESTAMP NOT NULL,
    dose                NUMERIC(10,2) NOT NULL CHECK (dose > 0),   -- en dosis skal være større end 0
    taken               BOOLEAN NOT NULL DEFAULT FALSE
);

-- ---------- Indekser på fremmednøgler ----------
-- Et indeks er ligesom stikordsregistret bag i en bog: databasen kan slå direkte op i det
-- i stedet for at læse hele tabellen igennem. PostgreSQL laver selv indeks på PRIMARY KEY og UNIQUE,
-- men IKKE på fremmednøgler. Vores mappers søger næsten altid på fremmednøglen (WHERE patient_id = ?,
-- WHERE round_id = ?), og ON DELETE CASCADE skal også finde rækkerne – derfor et indeks på hver af dem.
-- round.fertility_journey_id mangler med vilje: UNIQUE (fertility_journey_id, round_number) giver allerede et indeks, der starter med den.
CREATE INDEX fertility_journey_patient_id_idx ON fertility_journey (patient_id);   -- det partielle indeks dækker kun ACTIVE forløb
CREATE INDEX diagnosis_patient_id_idx         ON diagnosis (patient_id);
CREATE INDEX diary_entry_patient_id_idx       ON diary_entry (patient_id);
CREATE INDEX document_patient_id_idx          ON document (patient_id);
CREATE INDEX notification_patient_id_idx      ON notification (patient_id);
CREATE INDEX appointment_journey_id_idx       ON appointment (fertility_journey_id);
CREATE INDEX event_round_id_idx               ON event (round_id);
CREATE INDEX hormone_log_round_id_idx         ON hormone_log (round_id);
CREATE INDEX medication_log_round_id_idx      ON medication_log (round_id);
