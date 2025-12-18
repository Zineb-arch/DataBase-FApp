# HostLink Project — Run & JDBC setup

## Quick purpose
This project uses PostgreSQL. The app needs the PostgreSQL JDBC driver (JAR) on the Java classpath to connect.

## Steps to run on Windows (recommended)
A. Start a local PostgreSQL docker instance (recommended for a reproducible dev environment):

1. Ensure Docker is installed and running on your machine.
2. From the project root run `run-db.bat` (this uses `docker-compose.yml` and will initialize the DB using `hostlinkDB.sql`).
   - The DB will be available as `localhost:5432` with user `postgres`, password `507729`, database `hostlinkDB`.

B. If you can't run Docker: use a local PostgreSQL installation or the included embedded fallback (H2)

1. Local PostgreSQL: install PostgreSQL server and run `run-db.bat` and choose the psql option to initialize `hostlinkDB` using password `507729`.

2. Embedded H2 fallback (no external DB required):
   - Download the H2 JDBC JAR from: https://www.h2database.com/html/main.html
     - Example filename: `h2-2.x.x.jar`
   - Place the downloaded JAR into the project's `lib/` directory.
   - The app will automatically fall back to an embedded H2 database stored in `./data/hostlinkDB` when PostgreSQL is unavailable. The schema/data from `hostlinkDB.sql` will be applied automatically (with minor compatibility transforms).

C. Add JDBC drivers and run the Java app:

1. For PostgreSQL, download the PostgreSQL JDBC driver from: https://jdbc.postgresql.org/download/
   - Example filename: `postgresql-42.x.x.jar`
2. Place the downloaded JAR into the project's `lib/` directory.
3. The app reads DB config from environment variables (defaults match Docker):
   - `DB_HOST` (default: `localhost`), `DB_PORT` (default: `5432`), `DB_NAME` (default: `hostlinkDB`)
   - `DB_USER` (default: `postgres`), `DB_PASSWORD` (default: `507729`)
4. From the project root, compile and run (Command Prompt):
   - Compile (include whichever JDBC driver you need):
     javac -cp ".;lib\*" *.java
   - Run:
     java -cp ".;lib\*" HostLinkGUI

> Note: Replace `postgresql-42.x.x.jar` and `h2-2.x.x.jar` with the actual downloaded filenames.



## VS Code (Java) — Run using tasks
- Use the included VS Code tasks (see `.vscode/tasks.json`) to compile or run with the JAR on the classpath.

## Maven / Gradle (recommended for larger projects)
- Maven dependency:
```xml
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
  <version>42.x.x</version>
</dependency>
```
- Gradle (Groovy):
```
implementation 'org.postgresql:postgresql:42.x.x'
```

## Quick DB checks
- Ensure PostgreSQL is running and DB `hostlinkDB` exists.
- Test with psql:
  `psql -U postgres -d hostlinkDB`

## Troubleshooting
- "PostgreSQL JDBC Driver not found": the driver JAR is missing from the classpath.
- If connect returns null, check printed stack traces and verify credentials and URL.

## Helper files included
- `run.bat` — compile and run with `lib\postgresql-42.x.x.jar` (edit filename to match your JAR).
- `.vscode/tasks.json` — VS Code tasks to compile and run with the jar.
- `lib/README.txt` — instructions to place the driver JAR.

---
If you want, I can add a Maven `pom.xml` to fully manage the dependency and run tasks automatically.