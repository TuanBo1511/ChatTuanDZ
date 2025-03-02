/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package appchattuandz;

/**
 *
 * @author dokha
 */
import java.sql.*;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=ChatAppxdd";
    private static final String DB_USER = "sa"; // Thay bằng username SQL Server của bạn
    private static final String DB_PASSWORD = "123"; // Thay bằng password SQL Server của bạn

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver"); // Load JDBC Driver
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                System.out.println("Database connection established.");
            } catch (ClassNotFoundException e) {
                System.err.println("JDBC Driver not found: " + e.getMessage());
                throw new SQLException("JDBC Driver not found", e);
            } catch (SQLException e) {
                System.err.println("Connection failed: " + e.getMessage());
                throw e; // Re-throw the exception to be handled by the caller
            }
        }
        return connection;
    }


    public static void saveMessage(Message message) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO messages (sender_id, receiver_id, content) VALUES ((SELECT id FROM users WHERE username = ?), (SELECT id FROM users WHERE username = ?), ?)"
             )) {

            pstmt.setString(1, message.getSender());
            pstmt.setString(2, message.getReceiver());
            pstmt.setString(3, message.getContent());
            pstmt.executeUpdate();
            System.out.println("Message saved to database.");

        } catch (SQLException e) {
            System.err.println("Error saving message to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }

    // Các phương thức khác để lấy tin nhắn, thêm người dùng, v.v.
}