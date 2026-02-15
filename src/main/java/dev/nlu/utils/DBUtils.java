package dev.nlu.utils;

import dev.nlu.model.User;
import org.sqlite.core.DB;

import java.sql.*;
import java.util.Optional;

public class DBUtils {
    private final static String URL = "jdbc:sqlite:data.db";

    public DBUtils() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static Optional<User> validateUser(String username, String password){
        String sql = "select * from user where username=? and password=?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(
                            User.builder()
                                    .id(rs.getInt("id"))
                                    .username(rs.getString("username"))
                                    .password(rs.getString("password"))
                                    .build()
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Validate user failed: " + e.getMessage());
        }

        return Optional.empty();
    }

    public Optional<User> findById(int id){
        String sql = "select * from user where id=?";
        try (Connection conn = getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(User.builder()
                            .id(rs.getInt("id"))
                            .username(rs.getString("username"))
                            .password(rs.getString("password"))
                            .build());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return Optional.empty();
    }

    public  Optional<User> findByUsername(String username) {
        String sql = "select * from user where username=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
        ) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(User.builder()
                            .id(rs.getInt("id"))
                            .username(rs.getString("username"))
                            .password(rs.getString("password"))
                            .build());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return Optional.empty();
    }
}
