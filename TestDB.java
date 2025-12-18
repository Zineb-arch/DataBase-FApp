public class TestDB {
    public static void main(String[] args) {
        java.sql.Connection conn = DBConnection.connect();
        if (conn == null) {
            System.out.println("Result: null connection (failed to connect)");
            return;
        }
        try {
            System.out.println("Connected to DB: " + conn.getMetaData().getDatabaseProductName());
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}