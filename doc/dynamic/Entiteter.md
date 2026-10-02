# Simpl - Pet Project (2. semester)

*Entiteterne svarer 1:1 til domænemodellen (`doc/static/Domænemodel1.puml`). Placeringsregel: Patient ejer det, der følger patienten på tværs af forløb. FertilityJourney ejer det, der hører til hele forløbet. Round ejer det, der kun giver mening i én runde.*

**Entities:**

- **UserAccount**
  Håndterer login, adskilt fra selve patientdataen, så adgangskoden ikke ligger sammen med persondata.
  Important attributes: username, password (gemmes som hash, aldrig i klartekst).
  Relation: 1 UserAccount – 1 Patient.

- **Patient**
  Den centrale entitet i systemet. Repræsenterer personen, der gennemgår et fertilitetsforløb.
  Important attributes: firstName, lastName, dateOfBirth.
  Navnet er delt i for- og efternavn (1NF: atomare værdier), så fornavnet kan bruges alene ("Hej, Mette") og efternavnet til søgning/sortering senere.
  Relation: 1 Patient – 0..* Diagnosis, 0..* FertilityJourney, 0..* Notification, 0..* DiaryEntry.

- **Diagnosis**
  Patientens egne registrerede diagnoser med navn og beskrivelse, i stedet for et enkelt tekstfelt på Patient.
  Important attributes: name, description.

- **FertilityJourney**
  Patientens overordnede fertilitetsforløb — kan strække sig over flere runder over måneder eller år. En patient har højst ét forløb med status ACTIVE ad gangen.
  Important attributes: startDate, status (ACTIVE / COMPLETED).
  Relation: 1 FertilityJourney – 0..* Round, 0..* Appointment.

- **Round**
  Ét komplet behandlingsforsøg inden i et FertilityJourney, fra stimulation til graviditetstest. En patient kan have flere runder under samme forløb.
  Important attributes: roundNumber, treatmentType (IVF / ICSI / IUI / FET), startDate, endDate, status (IN_PROGRESS / COMPLETED), result (POSITIVE / NEGATIVE — tom indtil runden er afsluttet).
  Relation: 1 Round – 0..* Event, 0..* MedicationLog, 0..* HormoneLog, 0..* Document.

- **Appointment**
  Aftaler tilknyttet forløbet — scanning, konsultation, ægudtagning, ægoplægning m.m. Ligger på forløbet, fordi fx den første konsultation finder sted, før der er nogen runde.
  Important attributes: dateTime, appointmentType (CONSULTATION / SCANNING / BLOOD_TEST / EGG_RETRIEVAL / EMBRYO_TRANSFER / PREGNANCY_TEST), location.

- **DiaryEntry**
  Patientens private rum til at skrive noter om tanker, følelser eller spørgsmål til lægen — knyttet til patienten, ikke et forløb (ændret uge 40: man skal kunne skrive dagbog før første kontakt med klinikken, mellem to forløb og efter). Alt om *personen* ligger på Patient (diagnoser, dagbog, notifikationer); alt om *behandlingen* ligger på forløb/runde.
  Important attributes: dateTime, title, content.

- **Event**
  Et konkret trin i runden (fx "Stimulation startet", "Æg udtaget", "Ægoplægning") — bruges til at bygge rundens tidslinje.
  Important attributes: dateTime, eventType (STIMULATION_START / EGG_RETRIEVAL / FERTILISATION / EMBRYO_TRANSFER / PREGNANCY_TEST), description.

- **Medication**
  Stamdata for et lægemiddel (navn og beskrivelse), adskilt fra registreringen af, at det er taget, så samme medicin kan genbruges på tværs af registreringer.
  Important attributes: name, description.
  Relation: 1 Medication – 0..* MedicationLog.

- **MedicationLog**
  Registrering af en planlagt dosis af en bestemt Medication i en Round, og om den er taget.
  Important attributes: scheduledDateTime, dose, unit, taken.

- **HormoneLog**
  Registrering af en hormonmåling under en specifik Round — hormonniveauer måles typisk flere gange under stimulationsperioden.
  Important attributes: dateTime, hormoneType (FSH / LH / E2_OESTRADIOL / PROGESTERONE / AMH), value, unit.

- **Document**
  Dokumenter tilknyttet en runde, fx blodprøvesvar eller behandlingsplan. Selve filen ligger på disken; systemet gemmer stien.
  Important attributes: title, documentType (BLOOD_TEST_RESULT / TREATMENT_PLAN / OTHER), filePath.

- **Notification**
  Påmindelser til patienten, som systemet selv genererer ud fra kommende medicindoser og aftaler.
  Important attributes: dateTime, notificationType (MEDICATION_REMINDER / APPOINTMENT_REMINDER), title, message, isRead.

## Planlagte udvidelser (ikke i domænemodellen endnu)

- Antal udtagne æg, embryoner og oplagte embryoner på Round
- Status og aflysning på Appointment
- Medicinplan, der automatisk genererer MedicationLog-rækker
- Humør på DiaryEntry
