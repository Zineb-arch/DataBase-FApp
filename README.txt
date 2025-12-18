Place the PostgreSQL JDBC driver JAR or H2 JDBC JAR in this folder.

PostgreSQL JDBC:
1. Download: https://jdbc.postgresql.org/download/
2. Example filename: postgresql-42.x.x.jar

H2 JDBC (embedded fallback - useful if you don't want to run PostgreSQL):
1. Download: https://www.h2database.com/html/main.html
2. Example filename: h2-2.x.x.jar

Put the required JAR(s) in this folder (rename if necessary to match run.bat or tasks.json).

Then run:
- Windows: run.bat
- VS Code: Tasks -> Run Task -> "Compile Java (with JDBC)" then "Run HostLinkApp (with JDBC)"