import java.sql.*;

public class DBConnection {
    // Update these values to match your local PostgreSQL setup
    // Read DB config from environment variables with secure defaults (works with the Docker Compose below)
    private static final String DB_HOST = System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String DB_PORT = System.getenv().getOrDefault("DB_PORT", "5432");
    private static final String DB_NAME = System.getenv().getOrDefault("DB_NAME", "hostlinkDB");
    private static final String DB_USER = System.getenv().getOrDefault("DB_USER", "postgres");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "507729");
    private static final String URL = String.format("jdbc:postgresql://%s:%s/%s", DB_HOST, DB_PORT, DB_NAME);

    public static Connection connect() {
        Connection conn = null;
        // Diagnostic info for troubleshooting classpath/lib issues
        System.out.println("Runtime user.dir: " + System.getProperty("user.dir"));
        System.out.println("Runtime java.class.path: " + System.getProperty("java.class.path"));
        java.io.File libDirCheck = new java.io.File("lib");
        if (libDirCheck.exists() && libDirCheck.isDirectory()) {
            System.out.println("lib/ contents:");
            for (java.io.File f : libDirCheck.listFiles()) System.out.println(" - " + f.getName() + " (" + f.getAbsolutePath() + ")");
        } else {
            System.out.println("lib/ directory not found at runtime");
        }
        try {
            // Try to load the driver normally first
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException cnfe) {
                System.err.println("PostgreSQL JDBC Driver not found on classpath. Attempting to load from ./lib/...");
                // Try to find a postgresql jar in ./lib and load it dynamically
                try {
                    java.io.File libDir = new java.io.File("lib");
                    if (libDir.exists() && libDir.isDirectory()) {
                        java.io.File[] jars = libDir.listFiles((d, name) -> name.toLowerCase().startsWith("postgresql-") && name.toLowerCase().endsWith(".jar"));
                        if (jars != null && jars.length > 0) {
                            java.net.URL[] urls = new java.net.URL[jars.length];
                            for (int i = 0; i < jars.length; i++) urls[i] = jars[i].toURI().toURL();
                            java.net.URLClassLoader loader = new java.net.URLClassLoader(urls, DBConnection.class.getClassLoader());
                            Class<?> drvClass = Class.forName("org.postgresql.Driver", true, loader);
                            java.sql.Driver drv = (java.sql.Driver) drvClass.getDeclaredConstructor().newInstance();
                            // Register a shim so DriverManager can use it
                            java.sql.DriverManager.registerDriver(new DriverShim(drv));
                            System.out.println("Loaded PostgreSQL JDBC driver from: " + jars[0].getPath());
                        } else {
                            System.err.println("No PostgreSQL JDBC jar found in ./lib. Please download and place it there (see README).");
                            return null;
                        }
                    } else {
                        System.err.println("./lib directory not found. Please create a lib/ and put the PostgreSQL JDBC jar there (see README).");
                        return null;
                    }
                } catch (Exception ex) {
                    System.err.println("Failed to load PostgreSQL JDBC driver from ./lib/: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                }
            }

            System.out.println("Attempting to connect to: " + URL + " as " + DB_USER);
            conn = DriverManager.getConnection(URL, DB_USER, DB_PASSWORD);
            System.out.println("Connected to the PostgreSQL server successfully.");
        } catch (SQLException e) {
            System.err.println("Connection failure: " + e.getMessage());
            e.printStackTrace();
            // Helpful hints
            if (e.getMessage().toLowerCase().contains("connection refused") || e.getMessage().toLowerCase().contains("could not connect")) {
                System.err.println("Hint: Is the PostgreSQL server running? Try running run-db.bat (Docker) or ensure the service is started.");
            }
        }

        // If PostgreSQL is unavailable or driver not present, fallback to embedded H2 DB
        if (conn == null) {
            System.out.println("Falling back to embedded H2 database (no external DB required)");
            try {
                try {
                    Class.forName("org.h2.Driver");
                } catch (ClassNotFoundException cnfe) {
                    System.err.println("H2 JDBC Driver not found on classpath. Attempting to load from ./lib/...");
                    // Try to load h2 jar from lib
                    try {
                        java.io.File libDir = new java.io.File("lib");
                        if (libDir.exists() && libDir.isDirectory()) {
                            java.io.File[] jars = libDir.listFiles((d, name) -> name.toLowerCase().startsWith("h2") && name.toLowerCase().endsWith(".jar"));
                            if (jars != null && jars.length > 0) {
                                java.net.URL[] urls = new java.net.URL[jars.length];
                                for (int i = 0; i < jars.length; i++) urls[i] = jars[i].toURI().toURL();
                                java.net.URLClassLoader loader = new java.net.URLClassLoader(urls, DBConnection.class.getClassLoader());
                                Class<?> drvClass = Class.forName("org.h2.Driver", true, loader);
                                java.sql.Driver drv = (java.sql.Driver) drvClass.getDeclaredConstructor().newInstance();
                                java.sql.DriverManager.registerDriver(new DriverShim(drv));
                                System.out.println("Loaded H2 JDBC driver from: " + jars[0].getPath());
                            } else {
                                System.err.println("No H2 JDBC jar found in ./lib. Please download and place it there (see README).");
                                return null;
                            }
                        } else {
                            System.err.println("./lib directory not found. Please create a lib/ and put the H2 JDBC jar there (see README).");
                            return null;
                        }
                    } catch (Exception ex) {
                        System.err.println("Failed to load H2 JDBC driver from ./lib/: " + ex.getMessage());
                        ex.printStackTrace();
                        return null;
                    }
                }

                // H2 DB file location
                java.io.File dataDir = new java.io.File("data");
                if (!dataDir.exists()) dataDir.mkdirs();
                String h2Url = "jdbc:h2:file:./data/hostlinkDB;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
                System.out.println("Attempting to connect to H2 DB at: " + h2Url);
                conn = DriverManager.getConnection(h2Url, "sa", "");
                System.out.println("Connected to H2 DB.");

                // Check if tables exist; if not, initialize schema
                try (Statement s = conn.createStatement()) {
                    ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='USERACCOUNT'");
                    boolean needInit = true;
                    if (rs.next()) {
                        int cnt = rs.getInt(1);
                        needInit = (cnt == 0);
                    }
                    if (needInit) {
                        System.out.println("Initializing embedded H2 schema from hostlinkDB.sql (transformed)");
                        initializeH2Schema(conn);
                    }
                }

            } catch (Exception ex) {
                System.err.println("H2 fallback failed: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            }
        }

        return conn;
    }

    // Helper shim to register drivers loaded by a custom classloader
    public static class DriverShim implements java.sql.Driver {
        private final java.sql.Driver driver;
        public DriverShim(java.sql.Driver d) { this.driver = d; }
        public java.sql.Connection connect(String url, java.util.Properties info) throws java.sql.SQLException { return driver.connect(url, info); }
        public boolean acceptsURL(String url) throws java.sql.SQLException { return driver.acceptsURL(url); }
        public java.sql.DriverPropertyInfo[] getPropertyInfo(String url, java.util.Properties info) throws java.sql.SQLException { return driver.getPropertyInfo(url, info); }
        public int getMajorVersion() { return driver.getMajorVersion(); }
        public int getMinorVersion() { return driver.getMinorVersion(); }
        public boolean jdbcCompliant() { return driver.jdbcCompliant(); }
        public java.util.logging.Logger getParentLogger() throws java.sql.SQLFeatureNotSupportedException { return driver.getParentLogger(); }
    }

    // Initialize H2 schema by transforming the project's SQL file to H2-friendly statements
    private static void initializeH2Schema(Connection conn) throws Exception {
        java.io.File sqlFile = new java.io.File("hostlinkDB.sql");
        if (!sqlFile.exists()) {
            System.err.println("hostlinkDB.sql not found in project root; skipping initialization.");
            return;
        }
        String sql = new String(java.nio.file.Files.readAllBytes(sqlFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);

        // Simple transformations for H2 compatibility
        sql = sql.replaceAll("(?i)DROP TABLE IF EXISTS\\s+([a-zA-Z0-9_]+)\\s+CASCADE;", "DROP TABLE IF EXISTS $1;");
        sql = sql.replaceAll("(?i)DROP TABLE IF EXISTS\\s+([a-zA-Z0-9_]+);", "DROP TABLE IF EXISTS $1;");
        sql = sql.replaceAll("(?i)RETURNING\\s+User_id", "");
        sql = sql.replaceAll("(?i)SERIAL", "BIGINT AUTO_INCREMENT");
        sql = sql.replaceAll("--.*?\\r?\\n", "\n"); // remove SQL comments

        // Split statements by semicolon
        String[] statements = sql.split(";\\s*\\n");
        try (Statement st = conn.createStatement()) {
            for (String stmt : statements) {
                String s = stmt.trim();
                if (s.isEmpty()) continue;
                // H2 doesn't like certain CREATE VIEW placements; ensure statements end with semicolon removed
                if (s.toUpperCase().startsWith("CREATE VIEW") || s.toUpperCase().startsWith("DROP VIEW")) {
                    // execute as single statement
                    System.out.println("Executing view statement: " + (s.length() > 60 ? s.substring(0,60)+"..." : s));
                    st.execute(s);
                } else {
                    try {
                        st.execute(s);
                    } catch (Exception ex) {
                        System.err.println("Failed to execute statement (ignored): " + ex.getMessage());
                        // print statement for debugging
                        System.err.println(s);
                    }
                }
            }
        }
    }
}
