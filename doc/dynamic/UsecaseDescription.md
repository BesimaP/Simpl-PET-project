# Use case-beskrivelser

*Forudsætning for UC3–UC15: patienten er logget ind (UC1). Forudsætning for UC8–UC15: patienten har et forløb med status ACTIVE.

*Opdateret 7. okt 2026 (aften), så beskrivelserne passer til koden og sekvensdiagrammerne – inkl. UC15 EndJourney.* Hver use case dækker én eller flere user stories (angivet i parentes).*
- Note på en aftale (fx "husk fastende") – valgt fra af scope-hensyn: kræver kolonne, entity, mapper og service

## UC1: LogIn (US5)
Systemet starter og viser en login-skærm.
Brugeren indtaster brugernavn og adgangskode og klikker Log ind.
Systemet slår patienten op på brugernavnet og tjekker adgangskoden mod den gemte BCrypt-hash.
Patientens id og navn gemmes i sessionen, og dashboardet vises.
Hvis patienten endnu ikke har et aktivt forløb, viser dashboardet "Start dit forløb" (UC3).
Fra login-skærmen kan brugeren vælge "Opret profil" (UC2).

Regnvejrsdag:
- Databasen kan ikke læses: Fejlbesked vises, brugeren kan prøve igen.
- Forkert brugernavn eller adgangskode: Systemet viser en fejlbesked og logger ikke ind.
- Felter er tomme: Systemet viser samme fejlbesked som ved forkert login og logger ikke ind.


## UC2: ManageProfile (US6a, US6b)
Systemet viser en skærm med felter til navn, fødselsdato, brugernavn og adgangskode (mindst 8 tegn), og et valg om patienten allerede er i et forløb (med startdato).
Brugeren udfylder felterne og klikker Opret profil.
Systemet opretter en Patient med login og persondata (adgangskoden gemmes som BCrypt-hash) og gemmer den i databasen. Har brugeren valgt "ja" til forløb, oprettes forløbet samtidig (som i UC3).
Brugeren logges ind med det samme og sendes til dashboardet.
En logget-ind bruger kan efterfølgende åbne profilen for at rette fornavn, efternavn og fødselsdato, skifte adgangskode, eller slette sin konto og alle tilknyttede data efter bekræftelse (ON DELETE CASCADE – og de uploadede filer slettes fra disken). Bekræftelsen er en dialog; uden JavaScript en bekræft-side.

Regnvejrsdag:
- Et eller flere påkrævede felter er tomme: Systemet viser en fejlbesked og gemmer ikke.
- Fødselsdatoen er ugyldig eller i fremtiden, eller adgangskoden er under 8 tegn: Systemet viser en fejlbesked og gemmer ikke.
- Brugernavn er allerede i brug: Systemet viser en fejlbesked og gemmer ikke.
- Brugeren fortryder sletning i bekræftelsesdialogen: Intet slettes.


## UC3: CreateJourney (US1)
Dashboardet viser "Start dit forløb" med et datofelt, når patienten ikke har et aktivt forløb. Datofeltet er udfyldt med dags dato.
Brugeren beholder datoen eller vælger en tidligere startdato og klikker Start forløb.
Systemet opretter et nyt FertilityJourney med status ACTIVE og den valgte startdato, og gemmer det i databasen.
Dashboardet vises igen med beskeden "Dit forløb er oprettet".

Regnvejrsdag:
- Patienten har allerede et forløb med status ACTIVE: Systemet opretter ikke et nyt og viser dashboardet med det eksisterende forløb.
- Datoen mangler eller er ugyldig: Systemet viser en fejlbesked og opretter ikke forløbet.
- Forløbet kan ikke oprettes pga. en databasefejl: Systemet viser en fejlbesked.


## UC4: RegisterDiagnosis (US7)
Systemet viser en skærm med patientens registrerede diagnoser.
Brugeren klikker Tilføj Diagnose og udfylder navn og beskrivelse.
Brugeren klikker Gem. Systemet gemmer diagnosen i databasen, tilknyttet patienten, og opdaterer listen.

Regnvejrsdag:
- Navn er tomt: Systemet viser en fejlbesked og gemmer ikke.


## UC5: ManageAppointments (US3)
Systemet viser en skærm med kommende og tidligere aftaler tilknyttet det aktive forløb, sorteret efter dato.
Brugeren klikker Tilføj Aftale og udfylder dato/tidspunkt, type (konsultation, scanning, blodprøve, ægudtagning, ægoplægning, graviditetstest) og sted.
Brugeren klikker Gem. Systemet gemmer aftalen i databasen og opdaterer listen.
Er aftalen en ægudtagning, ægoplægning eller graviditetstest, og er en runde i gang, tilføjer systemet også en hændelse på tidslinjen (UC11).
Dashboardet viser de kommende aftaler.

Regnvejrsdag:
- Et eller flere påkrævede felter er tomme: Systemet viser en fejlbesked og gemmer ikke.
- Patienten har intet aktivt forløb: Systemet viser en besked om at starte et forløb først og gemmer ikke.


## UC6: WriteDiaryEntry (US4)
Systemet viser en skærm med patientens tidligere dagbogsnoter.
Brugeren klikker Ny note og udfylder dato, titel og indhold.
Brugeren klikker Gem. Systemet gemmer noten i databasen, tilknyttet patienten, og opdaterer listen.

Regnvejrsdag:
- Titel er tom: Systemet viser en fejlbesked og gemmer ikke.
- Indhold er tomt: Systemet viser en fejlbesked og gemmer ikke.
- Dato er ikke valgt: Systemet viser en fejlbesked og gemmer ikke.


## UC7: ViewNotifications (US12)
Systemet viser en skærm med patientens notifikationer, nyeste først, med titel, besked og læst-status.
Systemet opretter selv notifikationer, hver gang dashboardet åbnes (uden dubletter): MEDICATION_REMINDER for dagens planlagte doser, der ikke er taget, og APPOINTMENT_REMINDER for aftaler i dag og i morgen.
Brugeren kan markere én notifikation som læst, eller markere alle som læst på én gang (isRead).

Regnvejrsdag:
- Ingen notifikationer findes: Systemet viser en besked om, at listen er tom.


## UC8: StartRound (US10a)
Systemet viser en skærm til ny runde med dagens dato som startdato.
Brugeren vælger behandlingstype (IVF, ICSI, IUI, FET) og startdato og klikker Start runde.
Systemet finder selv næste rundenummer, opretter runden (i gang = ingen slutdato) med tomt resultat og gemmer den i databasen, tilknyttet det aktive forløb.
Systemet tilføjer hændelsen STIMULATION_START på tidslinjen (UC11).
Dashboardet vises med den nye runde.

Regnvejrsdag:
- Type eller startdato mangler: Systemet viser en fejlbesked og opretter ikke en ny runde.
- Patienten har intet aktivt forløb: Dashboardet viser "Start dit forløb" (UC3).
- Der er allerede en runde i gang: Systemet viser en besked om, at den igangværende runde skal afsluttes først (UC14).


## UC9: LogHormoneValue (US9)
Systemet viser en skærm med hormonværdier for den aktive runde, med den seneste måling fremhævet.
Brugeren klikker Tilføj Værdi og udfylder hormontype (FSH, LH, østradiol, progesteron, AMH), værdi, enhed og dato. Enheden gemmes pr. måling, fordi den afhænger af laboratoriet (udenlandske laboratorier bruger andre enheder).
Kurven over et hormon bruger enheden fra den nyeste måling. Målinger i en anden enhed kan ikke tegnes på samme akse, så de vises kun i listen, og siden skriver, hvor mange der ikke er med på kurven.
Brugeren klikker Gem. Systemet gemmer hormonværdien i databasen, tilknyttet runden, og opdaterer listen.

Regnvejrsdag:
- Værdien er ikke et tal: Systemet viser en fejlbesked og gemmer ikke.


## UC10: LogMedication (US8)
Systemet viser en skærm med medicinregistreringer for den aktive runde som en tjekliste.
Brugeren klikker Tilføj Medicin, vælger en medicin fra stamdata (listen viser præparatets enhed, fx "Gonal-F (IU)") og udfylder dosis, dato og tidspunkt (og evt. at den allerede er taget). Enheden vælges ikke – den er fast pr. præparat og står på Medication.
Brugeren klikker Gem. Systemet gemmer registreringen i databasen med reference til Medication og opdaterer listen.
Brugeren kan markere en registrering som taget, hvorved taken sættes – og fortryde igen.
Listen er delt i I dag, Kommende (fra i morgen og frem) og Tidligere.
Brugeren kan slette en dosis, fx hvis den er tastet forkert.

Regnvejrsdag:
- Et eller flere påkrævede felter er tomme: Systemet viser en fejlbesked og gemmer ikke.


## UC11: ViewTimeline (US2)
Systemet viser en skærm med alle hændelser (Event) for den aktive runde i kronologisk rækkefølge.
Hver hændelse vises med dato, type og beskrivelse.
Systemet opretter selv hændelserne: når en runde startes (UC8), og når der oprettes en aftale om ægudtagning, ægoplægning eller graviditetstest (UC5).

Regnvejrsdag:
- Ingen hændelser findes for den aktive runde: Systemet viser en besked om, at tidslinjen er tom.


## UC12: ManageDocuments (US11)
Systemet viser en skærm med patientens dokumenter med titel, type og upload-dato.
Brugeren klikker Tilføj Dokument, udfylder titel, vælger dokumenttype (henvisning, blodprøvesvar, behandlingsplan, andet) og vælger en fil.
Brugeren klikker Gem. Systemet gemmer dokumentets titel, type og filsti i databasen og opdaterer listen.
Brugeren kan vælge et dokument for at åbne det via den gemte filePath.
Brugeren kan slette et dokument. Systemet sletter både rækken i databasen og filen på disken.

Regnvejrsdag:
- Patienten har ingen dokumenter: Systemet viser en besked om, at listen er tom.
- Filen kan ikke findes/åbnes, eller dokumentet tilhører en anden patient: Systemet svarer med 404 (ikke fundet).
- Filen er ikke PDF/JPG/PNG eller er større end 10 MB: Systemet viser en fejlbesked og gemmer ikke.
- Titel eller fil mangler ved tilføjelse: Systemet viser en fejlbesked og gemmer ikke.


## UC13: ViewRoundHistory (US10b)
Systemet viser en skærm med alle runder for det aktive forløb, i rækkefølge efter rundenummer.
For hver runde vises rundenummer, behandlingstype, start- og slutdato, status (i gang/afsluttet) og resultat.

Regnvejrsdag:
- Ingen runder findes: Systemet viser en besked om, at der ingen historik er.


## UC14: EndRound (US10a)
Systemet viser en mulighed for at afslutte den aktive runde.
Brugeren vælger et resultat (positiv, negativ eller "ikke afgjort endnu") og bekræfter – i en dialog, eller på en bekræft-side, hvis JavaScript er slået fra.
Systemet sætter rundens slutdato til dagens dato (så er runden afsluttet) og gemmer resultatet i databasen.
Brugeren sendes til rundehistorikken (UC13) og kan derefter starte en ny runde (UC8) under samme forløb.

Regnvejrsdag:
- Der er ingen runde i gang: Systemet viser en fejlbesked.


## UC15: EndJourney (US1)
Når der ikke er en runde i gang, viser dashboardet linket "Afslut forløb".
Brugeren klikker Afslut forløb og bekræfter på bekræft-siden.
Systemet sætter forløbets status til COMPLETED. Runder, logs og noter gemmes.
Dashboardet viser "Dit forløb er afsluttet" og igen "Start dit forløb", så patienten senere kan oprette et nyt forløb (UC3).

Regnvejrsdag:
- Der er en runde i gang: Systemet viser en besked om, at runden skal afsluttes først (UC14), og afslutter ikke forløbet.
- Patienten har intet aktivt forløb: Dashboardet vises uden ændringer.


## Fremtidige features
Følgende features er identificeret, men ligger uden for denne version og har derfor ingen user story:
- Humør-felt på dagbogsnoter (UC6) — for at give patienten et nemt overblik over sit følelsesmæssige forløb
- Redigér, aflys og slet aftaler samt markér aftale som gennemført (UC5) — kræver status på Appointment
- Antal udtagne æg, embryoner og oplagte embryoner ved End Round (UC14) — kræver nye attributter på Round
- Medicinplan, der automatisk opretter alle planlagte doser i en periode (UC10)
- Note på en aftale, fx "husk fastende" (UC5) — valgt fra af scope-hensyn: kræver kolonne, entity, mapper og service
