package config;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class SetupDatabase {
    private static final String URL = ConfigLoader.get("db.url");
    private static final String USER = ConfigLoader.get("db.user");
    private static final String PASSWORD = ConfigLoader.get("db.password");

    public static void main(String[] args) throws Exception {
        System.out.println("Setting up Le Festin database...");
        
        executeSqlFile("sql/le_festin_schema.sql");
        System.out.println("✓ Schema created");
        
        executeSqlFile("sql/le_festin_seed.sql");
        System.out.println("✓ Seed data loaded");
        
        System.out.println("Done!");
    }
    
    private static void executeSqlFile(String filePath) throws Exception {
        String sql = new String(Files.readAllBytes(Paths.get(filePath)));
        Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
        // Connection conn = DBConnection.getInstance().getConnection();
        Statement stmt = conn.createStatement();
        
        // Split by semicolon and execute each statement
        for (String sqlStatement : sql.split(";")) {
            String trimmed = sqlStatement.trim();
            if (!trimmed.isEmpty()) {
                stmt.execute(trimmed);
            }
        }
        stmt.close();
    }
}