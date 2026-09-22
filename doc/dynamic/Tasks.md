# Tasks per user story

*Opdateret 22. sep 2026: [x] = lavet. Gem-punkter er krydset, når formularen gemmer i databasen via controller → service → DAO. "Vis fra databasen" og "Test" venter på templates/session.*

## User story 1 – Oprette fertilitetsforløb
- [x]  Lav layout til at oprette et nyt fertilitetsforløb *(HTML/CSS lavet – tom-tilstanden i dashboardtom.html; knappen opretter forløb og går videre til start-runde)*
- [x]  Gem det nye forløb i databasen med startdato sat automatisk *(POST /opret-forloeb → DashboardService.createJourney – startdato vælges i formularen, ikke automatisk)*
- [ ]  Vis det nye forløb på patientens oversigt
- [ ]  Test at oprettelsen virker og bliver synlig

## User story 2 – Tidslinje (Round)
- [ ]  Lav layout til tidslinjevisningen for en runde
- [ ]  Hent og sortér hændelser (Event) efter dato fra databasen
- [ ]  Sørg for at tidslinjen opdateres automatisk, når en ny hændelse tilføjes
- [ ]  Test sorteringen og live-opdateringen

## User story 3 – Aftaler (Journey)
- [ ]  Lav layout til aftaleoversigten
- [ ]  Hent og sortér aftaler efter dato (nærmeste først)
- [ ]  Vis dato, type og lokation for hver aftale
- [x]  Lav layout til at oprette en aftale (dato, type som dropdown, lokation) og gem den på det aktive forløb *(POST /aftaler → AppointmentService)*
- [ ]  Sørg for korrekt tilknytning til det rigtige forløb, hvis patienten har flere
- [ ]  Test sortering og korrekt tilknytning

## User story 4 – Dagbogsnoter (Journey)
- [x]  Lav layout til at skrive og gemme en note *(HTML/CSS lavet)*
- [x]  Gem noten i databasen med dato, titel og forløbs-tilknytning *(POST /dagbog → DiaryService.saveEntry)*
- [x]  Vis listen af tidligere noter til patienten *(HTML/CSS lavet)*
- [x]  JavaScript: dagens dato sættes automatisk i dato-feltet *(js/dagbog.js)*
- [x]  JavaScript: antal noter tælles og vises under listen *(js/dagbog.js)*
- [ ]  Test hele flowet fra start til slut

## User story 5 – Login
- [x]  Lav layout til login-skærmen *(HTML/CSS lavet)*
- [x]  Tjek brugernavn/adgangskode mod databasen *(POST /login → AuthService.login → UserAccountDAO.findByUsername)*
- [ ]  Håndter fejlscenariet: bruger uden profil henvises til oprettelse (User story 6a)
- [x]  Håndter fejlscenariet: forkert brugernavn/adgangskode giver fejlbesked *(sendes tilbage til login.html – selve fejlteksten kommer med templates)*
- [ ]  Test både succesfuldt login og fejlscenarierne

## User story 6a – Oprette profil
- [x]  Lav layout til profiloprettelse (navn, fødselsdato, brugernavn, adgangskode) *(HTML/CSS lavet)*
- [x]  Gem den nye profil i databasen (UserAccount + Patient), adgangskode gemmes som hash *(POST /opretprofil → AuthService.createProfile – hash venter, se teknisk gæld)*
- [x]  Tilføj validering: brugernavn allerede taget / manglende felter *(ALREADY_EXISTS / INVALID_INPUT i AuthService.createProfile)*
- [ ]  Test både succesfuld oprettelse og fejlmeddelelser

## User story 6b – Redigér profil og slet konto
- [x]  Lav layout til at redigere profiloplysninger (navn, fødselsdato) *(HTML/CSS lavet)*
- [x]  JavaScript: de to nye adgangskoder skal være ens, ellers fejlbesked og formularen sendes ikke *(js/min-profil.js)*
- [x]  Gem ændringer i databasen *(POST /min-profil og /skift-kodeord → ProfileService)*
- [x]  Implementér "slet konto" med bekræftelse, der fjerner alle patientens data *(bekræftelse i js/min-profil.js, POST /slet-konto → ProfileService.deleteAccount, CASCADE i schema.sql)*
- [ ]  Test redigering og sletning

## User story 7 – Diagnoser
- [x]  Lav layout til at registrere en ny diagnose (navn, beskrivelse) *(HTML/CSS lavet)*
- [x]  Gem diagnosen i databasen, tilknyttet patienten *(POST /diagnoser → DiagnosisService.addDiagnosis)*
- [x]  Lav layout til at vise alle patientens registrerede diagnoser *(HTML/CSS lavet)*
- [x]  JavaScript: ny diagnose tilføjes til listen med det samme, tæller opdateres *(js/diagnoser.js)*
- [ ]  Test at flere diagnoser kan registreres og vises samtidig

## User story 8 – Medicin
- [x]  Opret Medication-stamdata (navn, beskrivelse) som kan genbruges på tværs af registreringer *(INSERT i schema.sql)*
- [ ]  Lav layout til at registrere medicinindtag (vælg medicin, dosis, tidspunkt) på en aktiv runde
- [x]  Gem registreringen i databasen, med reference til den valgte Medication *(POST /medicin → MedicationService.logDose)*
- [ ]  Lav layout til medicinlisten, der viser tidligere registreringer
- [ ]  Implementér "markér som taget" på en planlagt dosis (taken)
- [ ]  Test at data gemmes og vises korrekt, inkl. korrekt reference til Medication og taget-status

## User story 9 – Hormonlog
- [x]  Lav layout til at registrere hormontype, værdi, enhed og dato *(HTML/CSS lavet)*
- [x]  JavaScript: dagens dato sættes automatisk i dato-feltet *(js/hormoner.js)*
- [x]  JavaScript: enheden vælges automatisk ud fra hormonet *(js/hormoner.js)*
- [x]  Gem målingen i databasen, tilknyttet den aktive Round *(POST /hormoner → HormoneService.saveLog)*
- [ ]  Implementér logik til at finde og vise den seneste måling
- [ ]  Test registrering og "seneste værdi"-visning

## User story 10a – Start og afslut runde
- [x]  Lav layout til at starte en ny runde (rundenummer, behandlingstype som dropdown: IVF, ICSI, IUI, FET) *(HTML/CSS lavet)*
- [x]  JavaScript: dagens dato sættes automatisk i startdato-feltet *(js/start-runde.js, fælles funktion i common.js)*
- [x]  Lav layout til at afslutte en runde med et resultat (POSITIVE / NEGATIVE) *(HTML/CSS lavet)*
- [x]  Gem resultatet på den specifikke Round, når den afsluttes *(POST /afslut-runde → RoundService.endRound → RoundDAO.endRound)*
- [ ]  Test start og afslutning

## User story 10b – Rundehistorik
- [x]  Lav layout til rundehistorik, der viser alle Rounds tilknyttet forløbet *(HTML/CSS lavet)*
- [ ]  Vis detaljer for en valgt runde
- [ ]  Test historikvisning

## User story 11 – Dokumenter
- [x]  Lav layout til dokumentlisten for en runde (titel, type) *(HTML/CSS lavet)*
- [x]  Lav layout til at tilføje et dokument (titel, dokumenttype som dropdown: Blodprøvesvar, Behandlingsplan, Andet, filvalg) *(HTML/CSS lavet)*
- [x]  JavaScript: filtype (PDF/JPG/PNG) og størrelse (maks 10 MB) tjekkes, når filen vælges; forkert fil afvises med fejlbesked; "Fjern fil"-knap *(js/dokumenter.js)*
- [ ]  Implementér gemning af dokumenter med filePath
- [ ]  Lav layout til at åbne og vise et valgt dokument
- [ ]  Test at dokumenter kan gemmes, listes og åbnes korrekt

## User story 12 – Notifikationer
- [ ]  Implementér logik der genererer en notifikation for dagens planlagte medicindoser, når appen åbnes (MEDICATION_REMINDER)
- [ ]  Lav layout til notifikationslisten (titel, besked, isRead-status)
- [ ]  Implementér markering af en notifikation som læst
- [ ]  Test at notifikationer genereres korrekt og kan markeres som læst

## Teknisk gæld (fundet ved kodegennemgang 22. sep 2026)
- [ ]  Tjek `DatabaseConnection.getConnection()`: åbnes der en ny forbindelse ved hvert DAO-kald uden at lukke den? (risiko for forbindelseslæk)
- [x]  `AuthService.createProfile`: tilføj blank-tjek på name/username/password → INVALID_INPUT (som i DiagnosisService) *(lavet 22. sep)*
- [x]  `LocalDate.parse(dateOfBirth)` kaster exception ved tom/ugyldig dato → skal give INVALID_INPUT i stedet for 500-fejl *(try/catch i createProfile, 22. sep – forløbs-startdato tages, når "forløb ved oprettelse" laves)*
- [ ]  Kodeord gemmes i klartekst → BCrypt-hash inden aflevering (AuthService.login/createProfile, ProfileService.changePassword)
- [x]  `enums/Result.java` ser ud til at være en rest ved siden af `ServiceResult` → slet eller forklar *(ikke en rest: `Result` = rundens resultat POSITIVE/NEGATIVE, bruges i RoundDAO.endRound. `ServiceResult` = svaret fra service til controller. To forskellige ting)*
