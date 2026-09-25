# Tasks per user story

*Opdateret 25. sep 2026: [x] = lavet. "Vis"-punkter er krydset, når siden er en Thymeleaf-template med data fra databasen. Gem-punkter er krydset, når formularen gemmer via controller → service → DAO. "Test"-punkter er krydset, når der er JUnit-tests på servicen (103 tests i src/test/java/services).*

*Ældre note (22. sep): [x] = lavet. Gem-punkter er krydset, når formularen gemmer i databasen via controller → service → DAO. "Vis fra databasen" og "Test" venter på templates/session.*

## User story 1 – Oprette fertilitetsforløb
- [x]  Lav layout til at oprette et nyt fertilitetsforløb *(HTML/CSS lavet – tom-tilstanden i dashboard.html; knappen opretter forløb og går videre til start-runde)*
- [x]  Gem det nye forløb i databasen med startdato sat automatisk *(POST /opret-forloeb → DashboardService.createJourney – startdato vælges i formularen, ikke automatisk)*
- [x]  Vis det nye forløb på patientens oversigt *(Thymeleaf: GET /dashboard viser forløb/runde – tre tilstande med th:if, 25. sep)*
- [x]  Test at oprettelsen virker og bliver synlig *(DashboardServiceTest)*

## User story 2 – Tidslinje (Round)
- [x]  Lav layout til tidslinjevisningen for en runde *(Thymeleaf: GET /tidslinje, th:each over events)*
- [x]  Opret et Event, når der gemmes noget på runden *(TimelineService.addEvent: start runde → STIMULATION_START; aftale af typen ægudtagning/oplægning/graviditetstest → tilsvarende event, hvis en runde er i gang, 25. sep)*
- [x]  Hent og sortér hændelser (Event) efter dato fra databasen *(EventDAO.findByRound ORDER BY date_time, TimelineService.getEvents)*
- [x]  Sørg for at tidslinjen opdateres automatisk, når en ny hændelse tilføjes *(siden renderes fra databasen ved hver visning)*
- [x]  Test sorteringen og oprettelsen *(TimelineServiceTest, AppointmentServiceTest)*

## User story 3 – Aftaler (Journey)
- [x]  Lav layout til aftaleoversigten *(Thymeleaf: GET /aftaler, kommende/tidligere fra databasen)*
- [x]  Hent og sortér aftaler efter dato (nærmeste først) *(AppointmentService.getUpcoming/getPast, DAO sorterer)*
- [x]  Vis dato, type og lokation for hver aftale *(Thymeleaf: GET /aftaler, th:each over kommende/tidligere; typen vises med dansk label via AppointmentType.getLabel)*
- [x]  Lav layout til at oprette en aftale (dato, type som dropdown, lokation) og gem den på det aktive forløb *(POST /aftaler → AppointmentService)*
- [ ]  Sørg for korrekt tilknytning til det rigtige forløb, hvis patienten har flere
- [x]  Test sortering og korrekt tilknytning *(AppointmentServiceTest)*

## User story 4 – Dagbogsnoter (Journey)
- [x]  Lav layout til at skrive og gemme en note *(HTML/CSS lavet)*
- [x]  Gem noten i databasen med dato, titel og forløbs-tilknytning *(POST /dagbog → DiaryService.saveEntry)*
- [x]  Vis listen af tidligere noter til patienten *(Thymeleaf: GET /dagbog, th:each)*
- [x]  JavaScript: dagens dato sættes automatisk i dato-feltet *(js/dagbog.js)*
- [x]  JavaScript: antal noter tælles og vises under listen *(js/dagbog.js)*
- [x]  Test hele flowet fra start til slut *(DiaryServiceTest – inkl. at patienter ikke kan se hinandens noter)*

## User story 5 – Login
- [x]  Lav layout til login-skærmen *(HTML/CSS lavet)*
- [x]  Tjek brugernavn/adgangskode mod databasen *(POST /login → AuthService.login → UserAccountDAO.findByUsername)*
- [ ]  Håndter fejlscenariet: bruger uden profil henvises til oprettelse (User story 6a)
- [x]  Håndter fejlscenariet: forkert brugernavn/adgangskode giver fejlbesked *(UserNotFoundException → ${error} på login-siden)*
- [x]  Test både succesfuldt login og fejlscenarierne *(AuthServiceTest)*

## User story 6a – Oprette profil
- [x]  Lav layout til profiloprettelse (navn, fødselsdato, brugernavn, adgangskode) *(HTML/CSS lavet)*
- [x]  Gem den nye profil i databasen (UserAccount + Patient), adgangskode gemmes som hash *(POST /opretprofil → AuthService.createProfile – hash venter, se teknisk gæld. Brugeren logges ind direkte efter oprettelse, 25. sep)*
- [x]  Tilføj validering: brugernavn allerede taget / manglende felter *(ALREADY_EXISTS / INVALID_INPUT i AuthService.createProfile)*
- [x]  Test både succesfuld oprettelse og fejlmeddelelser *(AuthServiceTest)*

## User story 6b – Redigér profil og slet konto
- [x]  Lav layout til at redigere profiloplysninger (navn, fødselsdato) *(HTML/CSS lavet)*
- [x]  JavaScript: de to nye adgangskoder skal være ens, ellers fejlbesked og formularen sendes ikke *(js/min-profil.js)*
- [x]  Gem ændringer i databasen *(POST /min-profil og /skift-kodeord → ProfileService)*
- [x]  Implementér "slet konto" med bekræftelse, der fjerner alle patientens data *(bekræftelse i js/min-profil.js, POST /slet-konto → ProfileService.deleteAccount, CASCADE i schema.sql)*
- [x]  Test redigering og sletning *(ProfileServiceTest)*

## User story 7 – Diagnoser
- [x]  Lav layout til at registrere en ny diagnose (navn, beskrivelse) *(HTML/CSS lavet)*
- [x]  Gem diagnosen i databasen, tilknyttet patienten *(POST /diagnoser → DiagnosisService.addDiagnosis)*
- [x]  Lav layout til at vise alle patientens registrerede diagnoser *(Thymeleaf: GET /diagnoser, th:each)*
- [x]  JavaScript: ny diagnose tilføjes til listen med det samme, tæller opdateres *(js/diagnoser.js)*
- [x]  Test at flere diagnoser kan registreres og vises samtidig *(DiagnosisServiceTest)*

## User story 8 – Medicin
- [x]  Opret Medication-stamdata (navn, beskrivelse) som kan genbruges på tværs af registreringer *(INSERT i schema.sql)*
- [x]  Lav layout til at registrere medicinindtag (vælg medicin, dosis, tidspunkt) på en aktiv runde *(HTML/CSS lavet – medicin.html)*
- [x]  Gem registreringen i databasen, med reference til den valgte Medication *(POST /medicin → MedicationService.logDose)*
- [x]  Lav layout til medicinlisten, der viser tidligere registreringer *(Thymeleaf: GET /medicin, i dag/tidligere fra databasen)*
- [x]  Implementér "markér som taget" på en planlagt dosis (taken) *(POST /medicin/taget → MedicationService.markTaken)*
- [x]  Test at data gemmes og vises korrekt, inkl. korrekt reference til Medication og taget-status *(MedicationServiceTest)*

## User story 9 – Hormonlog
- [x]  Lav layout til at registrere hormontype, værdi, enhed og dato *(HTML/CSS lavet)*
- [x]  JavaScript: dagens dato sættes automatisk i dato-feltet *(js/hormoner.js)*
- [x]  JavaScript: enheden vælges automatisk ud fra hormonet *(js/hormoner.js)*
- [x]  Gem målingen i databasen, tilknyttet den aktive Round *(POST /hormoner → HormoneService.saveLog)*
- [x]  Implementér logik til at finde og vise den seneste måling *(HormoneService.getLogs nyeste først; vises på /hormoner og dashboard med dansk label via HormoneType.getLabel)*
- [x]  Test registrering og "seneste værdi"-visning *(HormoneServiceTest)*

## User story 10a – Start og afslut runde
- [x]  Lav layout til at starte en ny runde (rundenummer, behandlingstype som dropdown: IVF, ICSI, IUI, FET) *(HTML/CSS lavet)*
- [x]  JavaScript: dagens dato sættes automatisk i startdato-feltet *(js/start-runde.js, fælles funktion i common.js)*
- [x]  Lav layout til at afslutte en runde med et resultat (POSITIVE / NEGATIVE) *(HTML/CSS lavet)*
- [x]  Gem resultatet på den specifikke Round, når den afsluttes *(POST /afslut-runde → RoundService.endRound → RoundDAO.endRound)*
- [x]  Test start og afslutning *(RoundServiceTest)*

## User story 10b – Rundehistorik
- [x]  Lav layout til rundehistorik, der viser alle Rounds tilknyttet forløbet *(Thymeleaf: GET /rundehistorik)*
- [x]  Vis detaljer for en valgt runde *(Thymeleaf: GET /rundehistorik viser nummer, type, datoer, status og resultat per runde)*
- [x]  Test historikvisning *(RoundServiceTest.getRounds…)*

## User story 11 – Dokumenter
- [x]  Lav layout til dokumentlisten for en runde (titel, type) *(HTML/CSS lavet)*
- [x]  Lav layout til at tilføje et dokument (titel, dokumenttype som dropdown: Blodprøvesvar, Behandlingsplan, Andet, filvalg) *(HTML/CSS lavet)*
- [x]  JavaScript: filtype (PDF/JPG/PNG) og størrelse (maks 10 MB) tjekkes, når filen vælges; forkert fil afvises med fejlbesked; "Fjern fil"-knap *(js/dokumenter.js)*
- [x]  Implementér gemning af dokumenter med filePath *(POST /dokumenter → DocumentService.uploadDocument, filen gemmes i uploads/, stien i databasen – Louise, 25. sep)*
- [x]  Lav layout til at åbne og vise et valgt dokument *(GET /dokumenter/{id} sender filen til browseren – kun patientens egne, 25. sep)*
- [x]  Test at dokumenter kan gemmes, listes og åbnes korrekt *(DocumentServiceTest, 8 tests)*

## User story 12 – Notifikationer
- [x]  Implementér logik der genererer en notifikation for dagens planlagte medicindoser, når appen åbnes (MEDICATION_REMINDER) *(NotificationService.createMedicationReminders kaldes fra GET /dashboard; ingen dubletter; rød prik på klokken ved ulæste, 25. sep)*
- [x]  Lav layout til notifikationslisten (titel, besked, isRead-status) *(Thymeleaf: GET /notifikationer, th:each med is-read)*
- [x]  Implementér markering af en notifikation som læst *(POST /notifikationer/laest → NotificationService.markRead)*
- [x]  Test at notifikationer genereres korrekt og kan markeres som læst *(NotificationServiceTest, 9 tests)*

## Tekniske krav fra undervisningen (24. sep 2026)
- [x]  Thymeleaf: templates + GET-ruter med `ctx.render` på siderne, der viser data *(25. sep: alle 11 sider – login, diagnoser, dagbog, aftaler, hormoner, medicin, rundehistorik, min-profil, dashboard, tidslinje, notifikationer)*
- [x]  Sessionsscope: `ctx.sessionAttribute("patientId", …)` og `accountId` sættes ved login; alle controllere læser fra sessionen og sender til /login, hvis den er tom
- [x]  Requestscope: data til siden via `ctx.attribute(...)` før `ctx.render` (som i undervisningen) – fx fejlbesked på login, lister på alle sider
- [x]  Exception: `UserNotFoundException` kastes i `AuthService.login`, fanges i `LoginController` *(24. sep – desuden NoActiveJourneyException, NoActiveRoundException og DatabaseException med samlet handler i ExceptionConfig)*
- [x]  Automatiserede tests: 103 JUnit-tests mod in-memory SQLite (`DatabaseConnection.useTestDatabase`) *(25. sep)*

## Teknisk gæld (fundet ved kodegennemgang 22. sep 2026)
- [ ]  Tjek `DatabaseConnection.getConnection()`: åbnes der en ny forbindelse ved hvert DAO-kald uden at lukke den? (risiko for forbindelseslæk)
- [x]  `AuthService.createProfile`: tilføj blank-tjek på name/username/password → INVALID_INPUT (som i DiagnosisService) *(lavet 22. sep)*
- [x]  `LocalDate.parse(dateOfBirth)` kaster exception ved tom/ugyldig dato → skal give INVALID_INPUT i stedet for 500-fejl *(try/catch i createProfile, 22. sep – forløbs-startdato tages, når "forløb ved oprettelse" laves)*
- [ ]  Kodeord gemmes i klartekst → BCrypt-hash inden aflevering (AuthService.login/createProfile, ProfileService.changePassword)
- [x]  `enums/Result.java` ser ud til at være en rest ved siden af `ServiceResult` → slet eller forklar *(ikke en rest: `Result` = rundens resultat POSITIVE/NEGATIVE, bruges i RoundDAO.endRound. `ServiceResult` = svaret fra service til controller. To forskellige ting)*
- [ ]  Thymeleaf/OGNL: skriv aldrig ét enkelt tegn i enkelte anførselstegn (`'d'`) – det bliver en `char`. Brug `#temporals.day(...)` eller `'dd'` *(fundet 25. sep)*
- [x]  `/logout`-rute: "Log ud" sletter sessionen (LoginController.logout) *(25. sep)*
