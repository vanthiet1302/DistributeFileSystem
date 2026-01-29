package dev.nlu.utils;

import dev.nlu.model.User;

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

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public void createTable() throws SQLException {
        String users = """
                create table if not exists users (
                    id integer primary key autoincrement,
                    username text not null,
                    password text not null,
                    role text not null
                );
                """;

        String files = """
                create table if not exists files (
                    id integer primary key autoincrement,
                    user_id integer not null references users(id) on delete cascade, 
                    file_name text not null,
                    file_part text not null,
                    size integer
                );
                """;

        String logs = """
                create table if not exists logs (
                    id integer primary key autoincrement,
                    action text not null,
                    timestamp datetime default current_timestamp
                );
                """;

        try (Connection connection = getConnection();
             Statement st = connection.createStatement();
        ) {
            // Turn on FK constraint
            st.execute("pragma foreign_keys = on");

            st.execute(users);
            st.execute(files);
            st.execute(logs);

            System.out.println("Tables created successfully.");
        } catch (SQLException e) {
            System.err.println("Create table failed: " + e.getMessage());
        }
    }

    public Optional<User> validateUser(String username, String password) throws SQLException {
        String sql = "select * from users where username=? and password=?";
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
                                    .role(rs.getString("role"))
                                    .build()
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Validate user failed: " + e.getMessage());
        }

        return Optional.empty();
    }

    public Optional<User> getUserById(int id) throws SQLException {
        String sql = "select * from users where id=?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(User.builder()
                            .id(rs.getInt("id"))
                            .username(rs.getString("username"))
                            .password(rs.getString("password"))
                            .role(rs.getString("role"))
                            .build());
                }
            }
        } catch (SQLException e) {
            System.err.println("Get user failed: " + e.getMessage());
        }

        return Optional.empty();
    }

    public Optional<User> getUserByUsername(String username) throws SQLException {
        String sql = "select * from users where id=?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(User.builder()
                            .id(rs.getInt("id"))
                            .username(rs.getString("username"))
                            .password(rs.getString("password"))
                            .role(rs.getString("role"))
                            .build());
                }
            }
        } catch (SQLException e) {
            System.err.println("Get user failed: " + e.getMessage());
        }

        return Optional.empty();
    }
}
