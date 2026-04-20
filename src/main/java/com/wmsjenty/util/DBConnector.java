package com.wmsjenty.util;
import java.sql.*;

public class DBConnector {
    private static final String URL = "jdbc:mysql://localhost:3306/wms-jenty?useUnicode=true&characterEncoding=UTF-8";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "Hetfield123!";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}
