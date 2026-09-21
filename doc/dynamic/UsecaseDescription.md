# Use case-beskrivelser

*Forudsætning for UC3–UC14: patienten er logget ind (UC1). Forudsætning for UC8–UC14: patienten har et forløb med status ACTIVE. Hver use case dækker én eller flere user stories (angivet i parentes).*
- Note på en aftale (fx "husk fastende") – valgt fra af scope-hensyn: kræver kolonne, entity, DAO og service

## UC1: LogIn (US5)
Systemet starter og viser en login-skærm.
Brugeren indtaster brugernavn og adgangskode og klikker Log ind.
Systemet validerer oplysningerne mod UserAccount og indlæser patientdata fra databasen.
Hvis patienten har et forløb med status ACTIVE, vises dashboardet.
Hvis patienten endnu ikke har et aktivt forløb, sendes brugeren til UC3: CreateJourney.
Fra login-skærmen kan brugeren vælge "Opret profil" (UC2).

Regnvejrsdag:
- Databasen kan ikke læses: Fejlbesked vises, brugeren kan prøve igen.
- Forkert brugernavn eller adgangskode: Systemet viser en fejlbesked og logger ikke ind.
- Felter er tomme: Systemet viser en fejlbesked og logger ikke ind.


## UC2: ManageProfile (US6a, US6b)
Systemet viser en skærm med felter til navn, fødselsdato, brugernavn og adgangskode.
Brugeren udfylder felterne og klikker Gem.
Systemet opretter en UserAccount (adgangskoden gemmes som hash) og en tilknyttet Patient, og gemmer dem i databasen.
Brugeren sendes videre til UC3: CreateJourney.
En logget-ind bruger kan efterfølgende åbne profilen for at redigere navn og fødselsdato, eller slette sin konto og alle tilknyttede data efter bekræftelse.

Regnvejrsdag:
- Et eller flere påkrævede felter er tomme: Systemet viser en fejlbesked og gemmer ikke.
- Brugernavn er allerede i brug: Systemet viser en fejlbesked og gemmer ikke.
- Brugeren fortryder sletning i bekræftelsesdialogen: Intet slettes.


## UC3: CreateJourney (US1)
Systemet viser en skærm med knappen "Start nyt forløb".
Brugeren klikker Start nyt forløb.
Systemet opretter et nyt FertilityJourney med status ACTIVE og dagens dato som startdato, og gemmer det i databasen.
Journey-id gemmes i Session, og brugeren sendes videre til dashboardet.

Regnvejrsdag:
- Patienten har allerede et forløb med status ACTIVE: Systemet viser en besked om, at det aktive forløb skal afsluttes først, og opretter ikke et nyt.
- Forløbet kan ikke oprettes pga. en databasefejl: Systemet viser en fejlbesked.


## UC4: RegisterDiagnosis (US7)
Systemet viser en skærm med patientens registrerede diagnoser.
Brugeren klikker Tilføj Diagnose og udfylder navn og beskrivelse.
Brugeren klikker Gem. Systemet gemmer diagnosen i databasen, tilknyttet patienten, og opdaterer listen.

Regnvejrsdag:
- Navn er tomt: Systemet viser en fejlbesked og gemmer ikke.


## UC5: ManageAppointments (US3)
Systemet viser en skærm med kommende aftaler tilknyttet det aktive forløb, sorteret efter dato med nærmeste først.
Brugeren klikker Tilføj Aftale og udfylder dato/tidspunkt, type (konsultation, scanning, blodprøve, ægudtagning, ægoplægning, graviditetstest) og sted.
Brugeren klikker Gem. Systemet gemmer aftalen i databasen og opdaterer listen.
Dashboardet viser den næste kommende aftale.

Regnvejrsdag:
- Et eller flere påkrævede felter er tomme: Systemet viser en fejlbesked og gemmer ikke.
- Datoen er i fortiden: Systemet viser en advarsel og beder brugeren bekræfte, inden der gemmes.


## UC6: WriteDiaryEntry (US4)
Systemet viser en skærm med patientens tidligere dagbogsnoter for det aktive forløb.
Brugeren klikker Ny note og udfylder dato, titel og indhold.
Brugeren klikker Gem. Systemet gemmer noten i databasen, tilknyttet det aktive forløb, og opdaterer listen.

Regnvejrsdag:
- Titel er tom: Systemet viser en fejlbesked og gemmer ikke.
- Indhold er tomt: Systemet viser en fejlbesked og gemmer ikke.
- Dato er ikke valgt: Systemet viser en fejlbesked og gemmer ikke.


## UC7: ViewNotifications (US12)
Systemet viser en skærm med patientens notifikationer, nyeste først, med titel, besked og læst-status.
Systemet opretter selv notifikationer af typen MEDICATION_REMINDER ud fra kommende planlagte medicindoser.
Brugeren kan åbne en notifikation, hvorved den markeres som læst (isRead).

Regnvejrsdag:
- Ingen notifikationer findes: Systemet viser en besked om, at listen er tom.


## UC8: StartRound (US10a)
Systemet viser en skærm til ny runde med dagens dato som startdato.
Brugeren udfylder rundenummer og vælger behandlingstype (IVF, ICSI, IUI, FET) og klikker Start Round.
Systemet opretter runden med status IN_PROGRESS og tomt resultat, gemmer den i databasen, tilknyttet det aktive forløb, og sætter den som aktiv runde i Session.
Dashboardet opdateres med den nye runde.

Regnvejrsdag:
- Rundenummer er tomt: Systemet viser en fejlbesked og opretter ikke en ny runde.
- Der er allerede en runde med status IN_PROGRESS: Systemet viser en besked om, at den igangværende runde skal afsluttes først (UC14).


## UC9: LogHormoneValue (US9)
Systemet viser en skærm med hormonværdier for den aktive runde, med den seneste måling fremhævet.
Brugeren klikker Tilføj Værdi og udfylder hormontype (FSH, LH, østradiol, progesteron, AMH), værdi, enhed og dato.
Brugeren klikker Gem. Systemet gemmer hormonværdien i databasen, tilknyttet runden, og opdaterer listen.

Regnvejrsdag:
- Værdien er ikke et tal: Systemet viser en fejlbesked og gemmer ikke.


## UC10: LogMedication (US8)
Systemet viser en skærm med medicinregistreringer for den aktive runde som en tjekliste.
Brugeren klikker Tilføj Medicin, vælger en medicin fra stamdata (eller opretter en ny) og udfylder dosis, enhed og planlagt tidspunkt.
Brugeren klikker Gem. Systemet gemmer registreringen i databasen med reference til Medication og opdaterer listen.
Brugeren kan markere en registrering som taget, hvorved taken sættes.

Regnvejrsdag:
- Et eller flere påkrævede felter er tomme: Systemet viser en fejlbesked og gemmer ikke.


## UC11: ViewTimeline (US2)
Systemet viser en skærm med alle hændelser (Event) for den aktive runde i kronologisk rækkefølge.
Brugeren kan klikke Tilføj Hændelse og udfylde dato, hændelsestype og beskrivelse; tidslinjen opdateres med det samme uden genindlæsning.
Brugeren kan klikke på en hændelse for at se detaljer.
Systemet opretter selv hændelser, når en runde startes (UC8) og afsluttes (UC14).

Regnvejrsdag:
- Ingen hændelser findes for den aktive runde: Systemet viser en besked om, at tidslinjen er tom.
- Dato eller type mangler ved tilføjelse: Systemet viser en fejlbesked og gemmer ikke.


## UC12: ManageDocuments (US11)
Systemet viser en skærm med dokumenter tilknyttet den aktive runde med titel og type.
Brugeren klikker Tilføj Dokument, udfylder titel, vælger dokumenttype (blodprøvesvar, behandlingsplan, andet) og vælger en fil.
Brugeren klikker Gem. Systemet gemmer dokumentets titel, type og filsti i databasen og opdaterer listen.
Brugeren kan vælge et dokument for at åbne det via den gemte filePath.

Regnvejrsdag:
- Ingen dokumenter findes for runden: Systemet viser en besked om, at listen er tom.
- Filen kan ikke findes/åbnes: Systemet viser en fejlbesked.
- Titel eller fil mangler ved tilføjelse: Systemet viser en fejlbesked og gemmer ikke.


## UC13: ViewRoundHistory (US10b)
Systemet viser en skærm med alle runder for det aktive forløb, nyeste først.
Brugeren vælger en runde, og systemet viser detaljer: rundenummer, behandlingstype, start- og slutdato, status og resultat.

Regnvejrsdag:
- Ingen runder findes: Systemet viser en besked om, at der ingen historik er.


## UC14: EndRound (US10a)
Systemet viser en mulighed for at afslutte den aktive runde.
Brugeren vælger et resultat (POSITIVE eller NEGATIVE) og klikker End Round.
Systemet sætter rundens status til COMPLETED, slutdato til dagens dato og gemmer resultatet i databasen.
Patienten kan derefter starte en ny runde (UC8) under samme forløb.

Regnvejrsdag:
- Intet resultat er valgt: Systemet viser en fejlbesked og afslutter ikke runden.


## Fremtidige features
Følgende features er identificeret, men ligger uden for denne version og har derfor ingen user story:
- Humør-felt på dagbogsnoter (UC6) — for at give patienten et nemt overblik over sit følelsesmæssige forløb
- Redigér, aflys og slet aftaler samt markér aftale som gennemført (UC5) — kræver status på Appointment
- Antal udtagne æg, embryoner og oplagte embryoner ved End Round (UC14) — kræver nye attributter på Round
- Medicinplan, der automatisk opretter alle planlagte doser i en periode (UC10)
- Automatisk generering af APPOINTMENT_REMINDER-notifikationer (UC7), ud over MEDICATION_REMINDER
- Hormonværdier vist som graf (UC9)
- Note på en aftale, fx "husk fastende" (UC5) — valgt fra af scope-hensyn: kræver kolonne, entity, DAO og service
