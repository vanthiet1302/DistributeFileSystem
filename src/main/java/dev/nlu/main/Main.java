package dev.nlu.main;

import dev.nlu.utils.DBUtils;
import java.sql.SQLException;

public class Main {
    public static void main() throws SQLException {
//        String url = "jdbc:sqlite:test.db";
        DBUtils dbUtils = new DBUtils();
        dbUtils.createTable();
    }
}
