# Simpl

Simpl er et system til patienter, der gennemgår et fertilitetsforløb (fx IVF, ICSI, IUI eller FET), hvor de kan holde styr på deres forløb, diagnoser, aftaler, medicin, hormonværdier, dagbogsnoter, dokumenter og notifikationer.

## Om projektet

Simpl er vores **Pet Project for 2. semester** (Datamatiker, Systemudvikling I). Projektet er en videreudvikling af vores SP4-projekt fra 1. semester, hvor det oprindeligt blev bygget som en JavaFX-desktopapplikation.

**Gruppe:** Besima & Louise

I dette semester bygges det om til et fullstack-system med en hjemmeside som frontend (HTML/CSS/JavaScript) og Javalin som backend, i tråd med semesterets krav til Pet Project. Datamodellen er udvidet med UserAccount, Diagnosis, Medication, Document og Notification, og Round er adskilt fra FertilityJourney, så et forløb kan indeholde flere runder.

## Målgruppe

Patienter der er i gang med et fertilitetsforløb, og som har behov for overblik over status, aftaler, medicin og egne noter/observationer gennem forløbet.

## Funktioner

- **Brugerkonti** — opret profil, log ind, genoptag sit forløb
- **Forløb og runder** — opret forløb, start/afslut runder med resultat, se rundehistorik
- **Dashboard** — overblik over aktiv runde, dagens medicin og nøgletal
- **Hormonlog** — registrering af hormonværdier over tid
- **Medicinlog** — planlagte doser og om de er taget
- **Aftaler** — kommende og tidligere aftaler på klinikken
- **Dagbog** — patientens egne noter
- **Diagnoser** — patientens registrerede diagnoser
- **Dokumenter** — upload af fx blodprøvesvar og behandlingsplaner
- **Tidslinje** — kronologisk overblik over trin i en runde
- **Notifikationer** — påmindelser om dagens medicin

## Status (september 2026)

- Frontend: alle 15 sider er bygget i HTML/CSS med lidt JavaScript (dato, enheder, tællere, fejlbeskeder)
- Database: `schema.sql` med 13 tabeller, alle entity- og DAO-klasser er skrevet
- Backend: Javalin kører og serverer siderne; login virker hele vejen fra formular til database
- Næste: resten af ruterne i controllerne, session (hvem er logget ind), templates til at vise data, skift fra SQLite til PostgreSQL

## Tech stack

- **Java 21**
- **Javalin 6** — webserver/backend (ruter, formularer, statiske filer)
- **HTML / CSS / JavaScript** — frontend i `resources/public`
- **SQLite** (via `sqlite-jdbc`) — lokal database *(skiftes til PostgreSQL senere på semestret)*
- **Maven** — byggeværktøj og afhængighedsstyring

## Arkitektur

Projektet følger **MVC** (Model-View-Controller) med *separation of concerns*: hvert lag har sit eget ansvar og taler kun med naboen.

```
src/
├── Main.java              # Starter Javalin (port 7070) og melder controllerne til
├── controller/            # Koordinatoren: modtager formularer, kalder DAO'er, sender svar (én per side)
├── entities/              # Model: dataklasser, én per tabel i schema.sql
├── dao/                   # Model: databaseadgang (én DAO per tabel) + DatabaseConnection/-Initializer
└── enums/                 # Enums (AppointmentType, HormoneType, TreatmentType …) – matcher CHECK i schema.sql

resources/
├── public/                # View: siderne (login.html, dashboard.html …), css/, js/, img/
├── templates/             # Thymeleaf-skabeloner til sider med data fra databasen (kommer)
└── data/schema.sql        # Databasens tabeller
```

Flow for én handling, fx "Gem måling": `hormoner.html` sender formularen (POST) → Javalin finder ruten → `HormoneController` læser felterne og bygger en `HormoneLog` → `HormoneLogDAO.save()` skriver i databasen → controlleren sender brugeren videre.

## Database

SQLite-databasen (`simpl.db`) oprettes automatisk i projektets rodmappe, første gang applikationen køres. Strukturen er defineret i `resources/data/schema.sql` og køres af `DatabaseInitializer` ved opstart. `simpl.db` er ikke i git.

**Tabeller (13):**
- `user_account` — login (brugernavn, kodeord-hash)
- `patient` — persondata, knyttet 1–1 til en konto
- `diagnosis` — patientens diagnoser
- `fertility_journey` — patientens overordnede forløb (kun ét aktivt ad gangen)
- `round` — én behandlingsrunde i et forløb (IVF/ICSI/IUI/FET, status, resultat)
- `appointment` — aftaler på forløbet
- `diary_entry` — dagbogsnoter på forløbet
- `event` — trin i en runde (vises på tidslinjen)
- `medication` — stamdata for lægemidler
- `medication_log` — planlagte doser i en runde, og om de er taget
- `hormone_log` — hormonmålinger i en runde
- `document` — dokumenter på en runde (kun stien gemmes)
- `notification` — påmindelser til patienten

Gyldige værdier (fx hormontyper, aftaletyper) er låst med `CHECK` i `schema.sql` og matcher enum-klasserne i `src/enums` og `value` i HTML-dropdowns.

## Kom i gang

### Forudsætninger

- Java 21 (JDK)
- Maven 3.x

### Kør projektet

1. Åbn projektet i IntelliJ og lad Maven hente afhængighederne (Javalin, slf4j, sqlite-jdbc).
2. Kør `Main`. Konsollen skriver, at Javalin lytter på port 7070.
3. Åbn `http://localhost:7070` i browseren – du lander på login-siden.

Databasen oprettes automatisk. Der er ingen brugere fra start; opret en via "Opret profil" (når ruten er lavet) eller i terminalen:
`sqlite3 simpl.db "INSERT INTO user_account (username, password_hash) VALUES ('test','1234');"`

## Dokumentation

Dokumentationen findes i `doc/`-mappen:

- `doc/dynamic/` — idébeskrivelse, VPC, krav, entiteter, user stories med acceptkriterier, tasks, use case-beskrivelser, use case-diagram (`Usecase.puml`), navigationsdiagram (`Navigation.puml`) og sekvensdiagrammer for UC1–UC14 (`UC1 - LogIn.puml` … `UC14 - EndRound.puml`)
- `doc/static/` — domænemodel (`Domænemodel1.puml`), klassediagrammer (`Klassediagram4a` model/enums, `Klassediagram4b` DAO-laget) og gruppekontrakt

Sekvensdiagrammer og klassediagrammer er tegnet før backend og opdateres, når controllerne er færdige.

Alle diagrammer er skrevet i PlantUML og gemt som PNG ved siden af kildefilen, så de kan ses uden at klone projektet.

### Domænemodel

![Domænemodel](doc/static/Domænemodel1.png)

### Use case-diagram

![Use case-diagram](doc/dynamic/Usecase.png)

### Navigationsdiagram

![Navigationsdiagram](doc/dynamic/Navigation.png)
