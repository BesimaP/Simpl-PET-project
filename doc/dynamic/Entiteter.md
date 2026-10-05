# Simpl - Pet Project (2. semester)

*Entiteterne svarer 1:1 til domænemodellen (`doc/static/Domænemodel1.puml`). Placeringsregel: Patient ejer det, der følger patienten på tværs af forløb. FertilityJourney ejer det, der hører til hele forløbet. Round ejer det, der kun giver mening i én runde.*

**Entities:**

- **Patient**
  Den centrale entitet i systemet. Repræsenterer personen, der gennemgår et fertilitetsforløb, og rummer også login (ændret uge 41: UserAccount er samlet ind i Patient, fordi de var 1–1 og altid blev oprettet sammen).
  Important attributes: username, password (gemmes som hash, aldrig i klartekst), firstName, lastName, dateOfBirth.
  Navnet er delt i for- og efternavn (1NF: atomare værdier), så fornavnet kan bruges alene ("Hej, Mette") og efternavnet til søgning/sortering senere.
  Relation: 1 Patient – 0..* Diagnosis, 0..* FertilityJourney, 0..* Notification, 0..* DiaryEntry, 0..* Document.

- **Diagnosis**
  Patientens egne registrerede diagnoser med navn og beskrivelse, i stedet for et enkelt tekstfelt på Patient.
  Important attributes: name, description.

- **FertilityJourney**
  Patientens overordnede fertilitetsforløb — kan strække sig over flere runder over måneder eller år. En patient har højst ét forløb med status ACTIVE ad gangen.
  Important attributes: startDate.
  Type: JourneyStatus (ACTIVE / COMPLETED).
  Relation: 1 FertilityJourney – 0..* Round, 0..* Appointment.

- **Round**
  Ét komplet behandlingsforsøg inden i et FertilityJourney, fra stimulation til graviditetstest. En patient kan have flere runder under samme forløb.
  Important attributes: roundNumber, startDate, endDate. Ingen status (ændret uge 41): en runde er i gang, så længe endDate er tom.
  Type: TreatmentType (IVF / ICSI / IUI / FET), Result (POSITIVE / NEGATIVE — tom indtil runden er afsluttet).
  Relation: 1 Round – 0..* Event, 0..* MedicationLog, 0..* HormoneLog.

- **Appointment**
  Aftaler tilknyttet forløbet — scanning, konsultation, ægudtagning, ægoplægning m.m. Ligger på forløbet, fordi fx den første konsultation finder sted, før der er nogen runde.
  Important attributes: dateTime, location.
  Type: AppointmentType (CONSULTATION / SCANNING / BLOOD_TEST / EGG_RETRIEVAL / EMBRYO_TRANSFER / PREGNANCY_TEST).

- **DiaryEntry**
  Patientens private rum til at skrive noter om tanker, følelser eller spørgsmål til lægen — knyttet til patienten, ikke et forløb (ændret uge 40: man skal kunne skrive dagbog før første kontakt med klinikken, mellem to forløb og efter). Alt om *personen* ligger på Patient (diagnoser, dagbog, notifikationer); alt om *behandlingen* ligger på forløb/runde.
  Important attributes: dateTime, title, content.

- **Event**
  Et konkret trin i runden (fx "Stimulation startet", "Æg udtaget", "Ægoplægning") — bruges til at bygge rundens tidslinje.
  Important attributes: dateTime, description.
  Type: EventType (STIMULATION_START / EGG_RETRIEVAL / FERTILISATION / EMBRYO_TRANSFER / PREGNANCY_TEST).

- **Medication**
  Stamdata for et lægemiddel (navn og beskrivelse), adskilt fra registreringen af, at det er taget, så samme medicin kan genbruges på tværs af registreringer.
  Important attributes: name, description.
  Relation: 1 Medication – 0..* MedicationLog.

- **MedicationLog**
  Registrering af en planlagt dosis af en bestemt Medication i en Round, og om den er taget.
  Important attributes: scheduledDateTime, dose, unit, taken.

- **HormoneLog**
  Registrering af en hormonmåling under en specifik Round — hormonniveauer måles typisk flere gange under stimulationsperioden.
  Important attributes: dateTime, value, unit.
  Type: HormoneType (FSH / LH / E2_OESTRADIOL / PROGESTERONE / AMH).

- **Document**
  Patientens dokumenter, fx henvisning, blodprøvesvar eller behandlingsplan. Knyttet til patienten, ikke runden (ændret uge 41: fx henvisning og blodprøvesvar kommer før første runde). Selve filen ligger på disken; systemet gemmer stien. uploadDate gør, at dokumenter kan vises under den rigtige runde via datoen.
  Important attributes: uploadDate, title, filePath.
  Type: DocumentType (REFERRAL / BLOOD_TEST_RESULT / TREATMENT_PLAN / OTHER).

- **Notification**
  Påmindelser til patienten, som systemet selv genererer ud fra kommende medicindoser og aftaler.
  Important attributes: dateTime, title, message. (Læst/ulæst er ikke en attribut i domænemodellen, men et acceptkriterie på US12 — kolonnen is_read findes kun i databasen.)
  Type: NotificationType (MEDICATION_REMINDER / APPOINTMENT_REMINDER).

- **Typerne** (JourneyStatus, TreatmentType, Result, AppointmentType, EventType, HormoneType, DocumentType, NotificationType)
  Faste værdilister, som hver har deres egen kasse/tabel (ændret uge 41, efter vejledning: alle typer skal have en tabel for sig — samme mønster som Medication). De står som "Type:" under den entitet, de beskriver.
  Important attributes: name.
  Relation: 1 Type – 0..* af den entitet, den beskriver (Result: 0..1 – 0..*, fordi en runde først får et resultat, når den afsluttes).

## Planlagte udvidelser (ikke i domænemodellen endnu)

- Antal udtagne æg, embryoner og oplagte embryoner på Round
- Status og aflysning på Appointment
- Medicinplan, der automatisk genererer MedicationLog-rækker
- Humør på DiaryEntry
