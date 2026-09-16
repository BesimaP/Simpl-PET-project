# Simpl-PET-project
# Simpl

Simpl er et system til patienter, der gennemgår et fertilitetsforløb (fx IVF, ICSI, IUI eller FET), hvor de kan holde styr på deres forløb, diagnoser, aftaler, medicin, hormonværdier, dagbogsnoter, dokumenter og notifikationer.

## Om projektet

Simpl er vores **Pet Project for 2. semester** (Datamatiker, Systemudvikling I). Projektet er en videreudvikling af vores SP4-projekt fra 1. semester, hvor det oprindeligt blev bygget som en JavaFX-desktopapplikation.

**Gruppe:** Besima & Louise

Projektet videreføres i dette semester med planer om at udvide det til et fuldt fullstack-system med en webbaseret frontend, i tråd med semesterets krav om, at Pet Project skal kunne udvikles som et fullstack system med en hjemmeside som frontend.

## Målgruppe

Patienter der er i gang med et fertilitetsforløb, og som har behov for overblik over status, aftaler, medicin og egne noter/observationer gennem forløbet.

## Nuværende funktioner (fra 1. semester)

- **Brugerkonti** — oprette profil, logge ind, genoptage sit forløb
- **Dashboard** — overblik med nøgletal
- **Hormonlog** — registrering af hormonværdier over tid
- **Medicinlog** — registrering af medicin, dosis og om den er taget
- **Aftaler** — planlægning og oversigt over kommende aftaler
- **Dagbog** — daglige dagbogsnoter
- **Tidslinje** — kronologisk overblik over hændelser i en runde
- **Rundehåndtering** — start/afslut en fertilitetsrunde med resultat
- **Rundehistorik** — se tidligere gennemførte runder

## Planer for 2. semester

- Udvide til en **webbaseret frontend** (HTML/CSS/JavaScript) i stedet for/som supplement til JavaFX
- Eksponere backend-logikken via en API, så en webfrontend kan tilgå samme data og logik
- Videreudvikle datamodellen med **UserAccount**, **Diagnosis**, **Medication** (stamdata), **Document** og **Notification**, som indgår i domænemodellen
- Adskille **Round** som selvstændig entitet fra selve forløbet (FertilityJourney), så et forløb korrekt kan indeholde flere runder
- Registrering af æg- og embryodata på Round er planlagt som senere udvidelse (se "Fremtidige features" i `doc/dynamic/UsecaseDescription.md`)

## Tech stack (nuværende, JavaFX-version)

- **Java 21**
- **JavaFX 21** — brugergrænseflade
- **SQLite** (via `sqlite-jdbc`) — lokal database
- **Maven** — byggeværktøj og afhængighedsstyring

## Arkitektur

Projektet følger **MVC**-mønsteret (Model-View-Controller):

```
 src/
├── Main.java              # Applikationens indgangspunkt (starter Javalin)
├── model/                 # Dataklasser (én per tabel i schema.sql)
├── dao/                   # Databaseadgang (én DAO per tabel) + DatabaseConnection
├── controller/            # Javalin-ruter: modtager formularer, kalder DAO'er, viser sider
└── enums/                 # Enums (AppointmentType, HormoneType, TreatmentType …)
web/
├── *.html                 # Siderne (login, dashboard, hormoner …)
├── css/                   # tokens, base, layout, components, pages
├── js/                    # common.js + én fil per side
└── img/                   # logo (spire.svg)

data/
└── schema.sql             # Databasens tabeller
```

## Database

SQLite-databasen (`simpl.db`) oprettes automatisk i projektets rodmappe, første gang applikationen køres. Databasestrukturen er defineret i `data/schema.sql` og initialiseres ved opstart af `DatabaseInitializer.java`.

**Nuværende tabeller (JavaFX-versionen):**
- `patient` — brugerkonti
- `journey` — patientens overordnede fertilitetsforløb
- `fertility_journey` — rundenummer, udtagne æg, befrugtede æg, resultat
- `event` — hændelser på tidslinjen
- `appointment` — planlagte aftaler
- `medication_log` — medicinregistreringer
- `hormone_log` — hormonværdi-registreringer
- `diary_entry` — dagbogsnoter

**Planlagt udvidelse (jf. domænemodellen):** `user_account`, `diagnosis`, `round` (adskilt fra `fertility_journey`), `medication` (stamdata), `document`, `notification`.

## Kom i gang

### Forudsætninger

- Java 21 (JDK)
- Maven 3.x

### Kør projektet

Åbn en terminal i projektmappen og skriv `mvn javafx:run`.

## Dokumentation

Dokumentationen findes i `doc/`-mappen:

- `doc/dynamic/` — idébeskrivelse, VPC, krav, entiteter, user stories med acceptkriterier, tasks, use case-beskrivelser, use case-diagram (`Usecase.puml`), navigationsdiagram (`Navigation.puml`) og sekvensdiagrammer for UC1–UC14 (`UC1 - LogIn.puml` … `UC14 - EndRound.puml`)
- `doc/static/` — domænemodel (`Domænemodel1.puml`) og klassediagram

Alle diagrammer er skrevet i PlantUML og gemt som PNG ved siden af kildefilen, så de kan ses uden at klone projektet.

### Domænemodel

![Domænemodel](doc/static/Domænemodel1.png)

### Use case-diagram

![Use case-diagram](doc/dynamic/Usecase.png)

### Navigationsdiagram

![Navigationsdiagram](doc/dynamic/Navigation.png)
