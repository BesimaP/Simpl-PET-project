# Tasks per user story

*Opdateret 9. okt 2026: integrationstests mod PostgreSQL er lavet og kørt – 256 JUnit-tests (mappers, services, entiteter og exceptions) mod schemaet `test` i databasen `Simpl`, alle grønne.*

*Opdateret 7. okt 2026 (aften): rettelser efter kodegennemgang – afslut forløb, ret fødselsdato, slet dokument, aftalepåmindelser, kommende medicin, sider der virker uden JavaScript, fælles fejlside og menu som Thymeleaf-fragment. Testene er flyttet til `src/test/java` og sat op mod PostgreSQL (se "Tekniske krav").*

*Opdateret 7. okt 2026: hele appen kører nu på PostgreSQL. DAO-klasserne er omdøbt til mappers i `persistence`, og alle får en fælles `ConnectionPool` gennem konstruktøren (Main → RouteConfig → Controller → Service → Mapper). Henvisninger til `…DAO` nedenfor svarer nu til `…Mapper`.*

*Opdateret 25. sep 2026: [x] = lavet. "Vis"-punkter er krydset, når siden er en Thymeleaf-template med data fra databasen. Gem-punkter er krydset, når formularen gemmer via controller → service → DAO. "Test"-punkter er krydset, når der er JUnit-tests på servicen (110 tests i src/test/java/services – i dag 256 tests, se noten øverst).*

*Ældre note (22. sep): [x] = lavet. Gem-punkter er krydset, når formularen gemmer i databasen via controller → service → DAO. "Vis fra databasen" og "Test" venter på templates/session.*

## User story 1 – Oprette fertilitetsforløb
- [x]  Lav layout til at oprette et nyt fertilitetsforløb *(HTML/CSS lavet – tom-tilstanden i dashboard.html; knappen opretter forløb og går videre til start-runde)*
- [x]  Gem det nye forløb i databasen med startdato *(POST /opret-forloeb → DashboardService.createJourney – startdato er forudfyldt med dags dato og kan ændres, jf. AC2, 7. okt)*
- [x]  Vis det nye forløb på patientens oversigt *(Thymeleaf: GET /dashboard viser forløb/runde – tre tilstande med th:if, 25. sep)*
- [x]  Afslut forløb, så patienten kan oprette et nyt senere ("flere forløb over tid") *(GET/POST /afslut-forloeb → DashboardService.endJourney – status COMPLETED; afvises med ROUND_IN_PROGRESS, hvis en runde er i gang, 7. okt)*
- [x]  Test at oprettelsen virker og bliver synlig *(DashboardServiceTest – også afslut forløb og nyt forløb bagefter)*

## User story 2 – Tidslinje (Round)
- [x]  Lav layout til tidslinjevisningen for en runde *(Thymeleaf: GET /tidslinje, th:each over events)*
- [x]  Opret et Event, når der gemmes noget på runden *(TimelineService.addEvent: start runde → STIMULATION_START; aftale af typen ægudtagning/oplægning/graviditetstest → tilsvarende event, hvis en runde er i gang, 25. sep)*
- [x]  Hent og sortér hændelser (Event) efter dato fra databasen *(EventDAO.findByRound ORDER BY date_time, TimelineService.getEvents)*
- [x]  Sørg for at tidslinjen opdateres automatisk, når en ny hændelse tilføjes *(siden renderes fra databasen ved hver visning – AC2 er omformuleret til "næste gang tidslinjen åbnes", 7. okt aften)*
- [x]  Test sorteringen og oprettelsen *(TimelineServiceTest, AppointmentServiceTest)*

## User story 3 – Aftaler (Journey)
- [x]  Lav layout til aftaleoversigten *(Thymeleaf: GET /aftaler, kommende/tidligere fra databasen)*
- [x]  Hent og sortér aftaler efter dato (nærmeste først) *(AppointmentService.getUpcoming/getPast, DAO sorterer)*
- [x]  Vis dato, type og lokation for hver aftale *(Thymeleaf: GET /aftaler, th:each over kommende/tidligere; typen vises med dansk label via AppointmentType.getLabel)*
- [x]  Lav layout til at oprette en aftale (dato, type som dropdown, lokation) og gem den på det aktive forløb *(POST /aftaler → AppointmentService)*
- [x]  Sørg for korrekt tilknytning til det rigtige forløb, hvis patienten har flere *(aftaler gemmes og vises altid på det AKTIVE forløb – der kan kun være ét ad gangen, afsluttede forløb har status COMPLETED, 7. okt)*
- [x]  Test sortering og korrekt tilknytning *(AppointmentServiceTest)*

## User story 4 – Dagbogsnoter (Journey)
- [x]  Lav layout til at skrive og gemme en note *(HTML/CSS lavet)*
- [x]  Gem noten i databasen med dato, titel og patient-tilknytning *(POST /dagbog → DiaryService.saveEntry. Ændret uge 40: noter ligger på patienten, ikke forløbet – dagbog kan bruges før, mellem og efter forløb)*
- [x]  Vis listen af tidligere noter til patienten *(Thymeleaf: GET /dagbog, th:each)*
- [x]  JavaScript: dagens dato sættes automatisk i dato-feltet *(js/dagbog.js)*
- [x]  JavaScript: antal noter tælles og vises under listen *(js/dagbog.js)*
- [x]  Slet en note (POST /dagbog/slet → DiaryService.deleteEntry – kun egne noter) *(25. sep)*
- [x]  Test hele flowet fra start til slut *(DiaryServiceTest – inkl. at patienter ikke kan se eller slette hinandens noter)*

## User story 5 – Login
- [x]  Lav layout til login-skærmen *(HTML/CSS lavet)*
- [x]  Tjek brugernavn/adgangskode mod databasen *(POST /login → AuthService.login → PatientDAO.findByUsername)*
- [x]  Håndter fejlscenariet: bruger uden profil henvises til oprettelse (User story 6a) *(fejlbeskeden siger bevidst ikke, OM brugernavnet findes – det er god sikkerhedsskik; "Opret profil"-linket står lige under. 25. sep)*
- [x]  Håndter fejlscenariet: forkert brugernavn/adgangskode giver fejlbesked *(UserNotFoundException → ${error} på login-siden)*
- [x]  Test både succesfuldt login og fejlscenarierne *(AuthServiceTest)*

## User story 6a – Oprette profil
- [x]  Lav layout til profiloprettelse (navn, fødselsdato, brugernavn, adgangskode) *(HTML/CSS lavet)*
- [x]  Gem den nye profil i databasen (Patient med brugernavn + adgangskode), adgangskode gemmes som hash *(POST /opretprofil → AuthService.createProfile – BCrypt.hashpw (5. okt). Brugeren logges ind direkte efter oprettelse, 25. sep)*
- [x]  Tilføj validering: brugernavn allerede taget / manglende felter *(ALREADY_EXISTS / INVALID_INPUT i AuthService.createProfile)*
- [x]  Test både succesfuld oprettelse og fejlmeddelelser *(AuthServiceTest)*

## User story 6b – Redigér profil og slet konto
- [x]  Lav layout til at redigere profiloplysninger (navn, fødselsdato) *(HTML/CSS lavet)*
- [x]  JavaScript: de to nye adgangskoder skal være ens, ellers fejlbesked og formularen sendes ikke *(js/min-profil.js)*
- [x]  Gem ændringer i databasen *(POST /min-profil → ProfileService.updateProfile – navn OG fødselsdato, ikke i fremtiden, 7. okt; POST /skift-kodeord → ProfileService.changePassword, mindst 8 tegn)*
- [x]  Implementér "slet konto" med bekræftelse, der fjerner alle patientens data *(bekræftelse i js/min-profil.js – uden JavaScript bekræft-siden GET /slet-konto; POST /slet-konto → ProfileService.deleteAccount, CASCADE i schema_postgres.sql; uploadede filer slettes også fra uploads/, 7. okt)*
- [x]  Test redigering og sletning *(ProfileServiceTest)*

## User story 7 – Diagnoser
- [x]  Lav layout til at registrere en ny diagnose (navn, beskrivelse) *(HTML/CSS lavet)*
- [x]  Gem diagnosen i databasen, tilknyttet patienten *(POST /diagnoser → DiagnosisService.addDiagnosis)*
- [x]  Lav layout til at vise alle patientens registrerede diagnoser *(Thymeleaf: GET /diagnoser, th:each)*
- [x]  Slet en diagnose (POST /diagnoser/slet → DiagnosisService.deleteDiagnosis – kun egne, med bekræftelse) *(7. okt aften – js/diagnoser.js er slettet: den fandt ikke sit felt, og antallet skrives allerede af Thymeleaf)*
- [x]  Test at flere diagnoser kan registreres og vises samtidig *(DiagnosisServiceTest)*

## User story 8 – Medicin
- [x]  Opret Medication-stamdata (navn, beskrivelse) som kan genbruges på tværs af registreringer *(INSERT i schema_postgres.sql)*
- [x]  Lav layout til at registrere medicinindtag (vælg medicin, dosis, tidspunkt) på en aktiv runde *(HTML/CSS lavet – medicin.html)*
- [x]  Gem registreringen i databasen, med reference til den valgte Medication *(POST /medicin → MedicationService.logDose)*
- [x]  Lav layout til medicinlisten, der viser tidligere registreringer *(Thymeleaf: GET /medicin, i dag/tidligere fra databasen)*
- [x]  Implementér "markér som taget" på en planlagt dosis (taken) *(POST /medicin/taget → MedicationService.markTaken)*
- [x]  Vis kommende doser (fra i morgen og frem) *(MedicationService.getUpcomingLogs, listen "Kommende" på /medicin; dosis vises uden ".0", 7. okt)*
- [x]  Slet en dosis, fx en fejlindtastning (POST /medicin/slet → MedicationService.deleteDose – kun egne) *(MedicationServiceTest)*
- [x]  Test at data gemmes og vises korrekt, inkl. korrekt reference til Medication og taget-status *(MedicationServiceTest)*

## User story 9 – Hormonlog
- [x]  Lav layout til at registrere hormontype, værdi, enhed og dato *(HTML/CSS lavet)*
- [x]  JavaScript: dagens dato sættes automatisk i dato-feltet *(js/hormoner.js)*
- [x]  JavaScript: enheden vælges automatisk ud fra hormonet *(js/hormoner.js)*
- [x]  Gem målingen i databasen, tilknyttet den aktive Round *(POST /hormoner → HormoneService.saveLog)*
- [x]  Implementér logik til at finde og vise den seneste måling *(HormoneService.getLogs nyeste først; vises på /hormoner og dashboard med dansk label via HormoneType.getLabel)*
- [x]  Slet en måling (POST /hormoner/slet → HormoneService.deleteLog – kun egne) og kurve som SVG med eget gennemsnit *(25. sep)*
- [x]  Test registrering og "seneste værdi"-visning *(HormoneServiceTest)*

## User story 10a – Start og afslut runde
- [x]  Lav layout til at starte en ny runde (behandlingstype som dropdown: IVF, ICSI, IUI, FET, og startdato – rundenummeret finder systemet selv) *(HTML/CSS lavet)*
- [x]  JavaScript: dagens dato sættes automatisk i startdato-feltet *(js/start-runde.js, fælles funktion i common.js)*
- [x]  Lav layout til at afslutte en runde med et resultat (Positiv / Negativ / Ikke afgjort endnu) *(HTML/CSS lavet)*
- [x]  Gem resultatet på den specifikke Round, når den afsluttes *(POST /afslut-runde → RoundService.endRound → RoundDAO.endRound)*
- [x]  "Afslut runde" virker også uden JavaScript *(knappen er et link til bekræft-siden GET /afslut-runde; med JavaScript åbner dialogen som før, 7. okt)*
- [x]  Test start og afslutning *(RoundServiceTest)*

## User story 10b – Rundehistorik
- [x]  Lav layout til rundehistorik, der viser alle Rounds tilknyttet forløbet *(Thymeleaf: GET /rundehistorik)*
- [x]  Vis detaljer for en valgt runde *(Thymeleaf: GET /rundehistorik viser nummer, type, datoer, status og resultat per runde)*
- [x]  Test historikvisning *(RoundServiceTest: getRoundsPerJourney, roundsFromEndedJourneyAreStillInHistory)*

## User story 11 – Dokumenter
- [x]  Lav layout til dokumentlisten (titel, type) *(HTML/CSS lavet)*
- [x]  Lav layout til at tilføje et dokument (titel, dokumenttype som dropdown: Henvisning, Blodprøvesvar, Behandlingsplan, Andet, filvalg) *(HTML/CSS lavet)*
- [x]  JavaScript: filtype (PDF/JPG/PNG) og størrelse (maks 10 MB) tjekkes, når filen vælges; forkert fil afvises med fejlbesked; "Fjern fil"-knap *(js/dokumenter.js)*
- [x]  Implementér gemning af dokumenter med filePath *(POST /dokumenter → DocumentService.uploadDocument, filen gemmes i uploads/, stien i databasen – Louise, 25. sep)*
- [x]  Lav layout til at åbne og vise et valgt dokument *(GET /dokumenter/{id} sender filen til browseren – kun patientens egne, 25. sep)*
- [x]  Vis upload-dato på listen (AC1: titel, type og upload-dato) *(7. okt)*
- [x]  Slet et dokument – både rækken og filen på disken *(POST /dokumenter/slet → DocumentService.deleteDocument – kun egne, 7. okt)*
- [x]  Test at dokumenter kan gemmes, listes, åbnes og slettes korrekt *(DocumentServiceTest)*

## User story 12 – Notifikationer
- [x]  Implementér logik der genererer en notifikation for dagens planlagte medicindoser, når appen åbnes (MEDICATION_REMINDER) *(NotificationService.createMedicationReminders kaldes fra GET /dashboard; ingen dubletter; rød prik på klokken ved ulæste, 25. sep)*
- [x]  Lav layout til notifikationslisten (titel, besked, isRead-status) *(Thymeleaf: GET /notifikationer, th:each med is-read)*
- [x]  Generér påmindelser om aftaler i dag og i morgen (APPOINTMENT_REMINDER) *(NotificationService.createAppointmentReminders kaldes fra GET /dashboard; ingen dubletter, 7. okt)*
- [x]  Implementér markering af en notifikation som læst *(POST /notifikationer/laest → NotificationService.markRead)*
- [x]  Test at notifikationer genereres korrekt og kan markeres som læst *(NotificationServiceTest, 9 tests)*

## Tekniske krav fra undervisningen (24. sep 2026)
- [x]  Thymeleaf: templates + GET-ruter med `ctx.render` på siderne, der viser data *(25. sep: alle 11 sider – login, diagnoser, dagbog, aftaler, hormoner, medicin, rundehistorik, min-profil, dashboard, tidslinje, notifikationer. Nu 16 templates i alt)*
- [x]  Sessionsscope: `ctx.sessionAttribute("patientId", …)` og `patientName` sættes ved login (accountId fjernet, uge 41: UserAccount er samlet ind i Patient); alle controllere læser fra sessionen og sender til /login, hvis den er tom
- [x]  Requestscope: data til siden via `ctx.attribute(...)` før `ctx.render` (som i undervisningen) – fx fejlbesked på login, lister på alle sider
- [x]  Exception: `UserNotFoundException` kastes i `AuthService.login`, fanges i `LoginController` *(24. sep – desuden NoActiveJourneyException, NoActiveRoundException og DatabaseException med samlet handler i ExceptionConfig)*
- [x]  Automatiserede tests: 110 JUnit-tests mod in-memory SQLite (`DatabaseConnection.useTestDatabase`) *(25. sep – SQLite er udskiftet, se næste punkt)*
- [x]  Integrationstests mod en testdatabase i PostgreSQL *(8.–9. okt: testene kører mod schemaet `test` i databasen `Simpl`. `TestDatabase` (mappers) og `TestData` (services) kører `schema_postgres.sql` ind i `test`, så de rigtige data i `public` aldrig røres. 256 tests: 100 mapper-, 141 service-, 13 entitets- og 2 exception-tests – alle grønne)*
- [x]  Skift fra SQLite til PostgreSQL: `ConnectionPool` (HikariCP), mappers i `persistence`, try-with-resources, typetabeller med `SELECT id … WHERE name = ?` og `JOIN` *(7. okt)*

## Teknisk gæld (fundet ved kodegennemgang 22. sep 2026)
- [x]  Tjek `DatabaseConnection.getConnection()`: åbnes der en ny forbindelse ved hvert DAO-kald uden at lukke den? (risiko for forbindelseslæk) *(løst 7. okt: `DatabaseConnection` er slettet; mappers låner en forbindelse fra `ConnectionPool` i try-with-resources, så den altid afleveres igen)*
- [x]  `AuthService.createProfile`: tilføj blank-tjek på name/username/password → INVALID_INPUT (som i DiagnosisService) *(lavet 22. sep)*
- [x]  `LocalDate.parse(dateOfBirth)` kaster exception ved tom/ugyldig dato → skal give INVALID_INPUT i stedet for 500-fejl *(try/catch i createProfile, 22. sep – forløbs-startdato tages, når "forløb ved oprettelse" laves)*
- [x]  Kodeord gemmes i klartekst → BCrypt-hash inden aflevering (AuthService.login/createProfile, ProfileService.changePassword) *(jbcrypt 0.4 i pom; hashpw ved opret profil og skift kodeord, checkpw ved login og skift kodeord; ProfileServiceTest tjekker, at kodeordet ikke står i klartekst – 5. okt)*
- [x]  `enums/Result.java` ser ud til at være en rest ved siden af `ServiceResult` → slet eller forklar *(ikke en rest: `Result` = rundens resultat POSITIVE/NEGATIVE, bruges i RoundDAO.endRound. `ServiceResult` = svaret fra service til controller. To forskellige ting)*
- [x]  Thymeleaf/OGNL: skriv aldrig ét enkelt tegn i enkelte anførselstegn (`'d'`) – det bliver en `char`. Brug `#temporals.day(...)` eller `'dd'` *(fundet 25. sep – en huskeregel, ikke en opgave)*
- [x]  `/logout`-rute: "Log ud" sletter sessionen (LoginController.logout) *(25. sep)*

## Rettelser og oprydning (7. okt 2026)
- [x]  Testdata: alle testbrugere har et rigtigt BCrypt-hash – kodeord `test1234` (fx mette1990 / test1234). Et ødelagt hash giver "forkert kodeord" i stedet for en 500-fejl
- [x]  try/catch om `Integer.parseInt` og `Result.valueOf` på id'er og værdier fra formularer – ugyldigt input giver en fejlbesked i stedet for en 500-fejl
- [x]  Fælles fejlside `templates/fejl.html`: `ExceptionConfig` fanger både `DatabaseException` og alle andre exceptions
- [x]  Sider virker uden JavaScript: afslut runde, afslut forløb og slet konto har en bekræft-side (`templates/bekraeft.html`); klokken linker til /notifikationer; opret profil er en Thymeleaf-template med fejlbesked fra serveren
- [x]  Menuen er ét Thymeleaf-fragment (`fragments/menu.html`) med det aktive menupunkt som parameter – `menu('medicin')`
- [x]  Dashboard: dag-ringen får sin procent (`progressPercent`); hormonværdier og doser vises uden ".0"
- [x]  Klassediagrammer opdateret: 4a (entities/enums), 4b (mappers) og nyt 4c (controllers + services, delt i 4c-1, 4c-2 og 4c-3) + 4c-oversigt – i sort/hvid
- [x]  Use cases og sekvensdiagrammer opdateret: UC2 (fødselsdato, slet filer), UC3 (forudfyldt dato), UC7 (aftalepåmindelser), UC10 (kommende doser), UC12 (slet dokument), UC14 (bekræft-side) og ny UC15 EndJourney – alle i sort/hvid
- [x]  3NF: `unit` flyttet fra `medication_log` til `medication` – enheden er fast pr. præparat (svar fra fertilitetsklinik). 17 præparater i stamdata (IU / mg / µg); dropdownen på /medicin hentes fra databasen med `th:each`. `hormone_log.unit` bliver, fordi enheden afhænger af laboratoriet
- [x]  Hormonkurven tegner kun målinger i samme enhed som den nyeste – målinger i en anden enhed (fx fra et udenlandsk laboratorie) står i listen, og siden skriver "X målinger i en anden enhed … er ikke med på kurven" (HormoneService.getCurve, HormoneServiceTest)
- [x]  Afsluttede forløb kan ses: rundehistorik viser alle forløb med deres runder (FertilityJourneyMapper.findByPatient, RoundService.getRoundsPerJourney); aftaler kan vises for et valgt forløb (`/aftaler?forloeb=id`) og tidslinjen for en valgt runde (`/tidslinje?runde=id`) – kun patientens egne (RoundServiceTest, AppointmentServiceTest, TimelineServiceTest)

## Rettelser efter gennemgang (9. okt 2026)
- [x]  Findes der allerede et aktivt forløb, viser dashboardet "Du har allerede et aktivt forløb" i stedet for "Dit forløb er oprettet ✓" (DashboardController, besked.html)
- [x]  Et forløb kan ikke starte i fremtiden – hverken på dashboardet eller ved opret profil (DashboardService, AuthService, `max` på datofeltet, DashboardServiceTest)
- [x]  Ægudtagning, oplægning og graviditetstest kommer kun på rundens tidslinje, hvis aftalen ligger på eller efter rundens startdato (AppointmentService, AppointmentServiceTest)
- [x]  Hormonmålinger og doser kan ikke ligge før rundens startdato (HormoneService, MedicationService, `min` på datofeltet via RoundService.getActiveRoundStart, tests)
- [x]  View `round_overview` i `schema_postgres.sql` (runde + behandlingstype + resultat) – RoundMapper læser fra viewet, så joinet kun står ét sted

## Rettelser efter gennemgang (7. okt 2026, aften)
- [x]  Én aktiv runde pr. forløb håndhæves i databasen (`one_active_round_per_journey`) og CHECK-regler på `round` (rundenummer > 0, slutdato ≥ startdato), `hormone_log.value` (≥ 0) og `medication_log.dose` (> 0)
- [x]  Længdetjek i services (navne 50 tegn, titler/steder 100 tegn) og `maxlength` i formularerne – for lang tekst giver en fejlbesked i stedet for en 500-fejl
- [x]  Hormonværdi og dosis skal være et rigtigt tal (ikke "NaN"/"Infinity") og kunne være i NUMERIC(10,2); hormonenheden skal være en af dropdownens
- [x]  Bekræftelse før sletning (common.js spørger på alle slet-formularer); "Afslut runde"-dialogen lover ikke længere, at man kan fortryde
- [x]  Mobil: avataren vises også på mobil, så Min profil, Diagnoser, Dokumenter og Log ud kan nås
- [x]  Aftaler uden forløb viser "Intet forløb" i stedet for en formular, der altid fejler
- [x]  JavaScript: `setTodayIn` bruger den lokale dato (ikke UTC) og tåler et manglende felt; medicin.js sætter dagens dato; dokumenter.js tåler "Annullér" i filvælgeren
- [x]  Død kode fjernet: ubrugte mapper-metoder (`AppointmentMapper/EventMapper/NotificationMapper.delete`, `MedicationMapper.save`) og entitet-metoder, der aldrig blev kaldt. 9. okt: også `RoundService.getRounds`, `TimelineService.getEvents(int)`, `AppointmentService.getPast(int)`, `DocumentService.deleteAllFiles` og `HormoneCurve.getMax/getAverage` fjernet – testene bruger nu de samme metoder som appen
- [x]  Dokumentation: README (opsætning af database), UC4/UC6/UC9 (slet), US2 AC2, US1 AC3, US7 AC3, klassediagrammer og domænemodel i sort/hvid
- [x]  Slet note/diagnose/dokument og "markér som læst" tjekker ejeren i SQL'en (`WHERE id = ? AND patient_id = ?`) – mapperen svarer true/false, så services ikke længere løber patientens liste igennem
- [x]  Dokumenter: filen gemmes som UUID + endelse; `FileStorageException` (ny, i `exceptions`) i stedet for RuntimeException; fejler INSERT, slettes filen igen; ved sletning slettes rækken før filen; slet konto henter dokumenterne → sletter patienten → sletter filerne (`DocumentService.deleteFiles`)
- [x]  "Åbn" dokument virker, selvom `probeContentType` giver null (typen tages fra endelsen); upload-fejl giver beskeden om fil (`?fejl=fil`)
- [x]  Log ud er nu en knap (POST /logout) i stedet for et link (GET); ved forkert login står brugernavnet der stadig
- [x]  Start runde: startdatoen må ikke ligge før forløbets start eller i fremtiden (INVALID_INPUT)
- [x]  Dropdowns på aftaler, dokumenter, hormoner og start-runde bygges med `th:each` af enum-værdierne (`TreatmentType` har fået `getLabel()`) – ny type = enum-værdi + række i typetabellen (NFR5)
- [x]  Medicinnavn falder tilbage til `name`, når `description` er tom; RouteConfig laver kun én NotificationService og springer /css, /js og /img over
- [x]  Dokumentation opdateret: klassediagram 4a/4b/4c, UC2/UC4/UC6/UC7/UC8/UC12, UsecaseDescription (UC8, UC12), Krav NFR5, README
