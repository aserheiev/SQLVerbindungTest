package org.example;

import com.mysql.cj.jdbc.MysqlDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class Main {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";

    public static void main(String[] args) throws RuntimeException, SQLException {
        String currentdb;
        String currentTable;

        // old deprecated connection method, leaving it for reference purposes
        // String url = "jdbc:mysql://localhost:3306/kunden";
        // String username = "testuser";
        // String password = "12345";
        // Connection connection = EstablishContact(url, username, password);

        while (true) {
            MainMenu();
        }
    }

    public static boolean MainMenu() throws SQLException {
        Scanner input = new Scanner(System.in);
        int command;
        boolean selectionValid = false;
        boolean exiting = false;

        System.out.println(ANSI_GREEN + "Welcome to the DB manager. What would you like to do?" + ANSI_RESET);
        System.out.println("1. Look at the list of existing DBs");
        System.out.println("2. Create a new DB");
        System.out.println("3. Open an existing DB");
        System.out.println("4. Delete a DB");
        System.out.println("5. Exit");

        while (!selectionValid) {
            command = input.nextInt();

            switch (command) {
                case 1:
                    selectionValid = true;
                    GetDBList();
                    break;
                case 2:
                    System.out.println("Not implemented");
                    break;
                case 3:
                    System.out.println("Not implemented");
                    break;
                case 4:
                    System.out.println("Not implemented");
                    break;
                case 5:
                    System.exit(0);
                    break;
                default:
                    System.out.println(ANSI_RED + "Make a valid selection please" + ANSI_RESET);
                    break;
            }
        }
        return false;
    }

    public static void DeleteDB() {

    }

    public static void CreateDB() {

    }

    public static String GetDBList() throws SQLException {
        // Zeigt alle Datenbanken, und dann nach Auswahl zeigt ihre Tabellen
        // und DANN nach Auswahl gibt ihre Inhalte aus

        int auswahl;
        Scanner input = new Scanner(System.in);
        String dbname = null;

        Statement stmt = GetStatement(null);

        System.out.println("Currently accessible DBs are:");

        ResultSet results = stmt.executeQuery("SHOW DATABASES");

        OneColumnResults(results);

        System.out.println("1. Open DB");
        System.out.println("2. Rename DB");
        System.out.println("3. Delete DB");
        System.out.println("4. Back");

        auswahl = input.nextInt();

        switch (auswahl) {
            case 1:
                System.out.println("Oh yeah, which one?");
                auswahl = input.nextInt();
                results.absolute(auswahl);
                dbname = results.getString(1);
                ShowTables(dbname);
                break;
            case 2:
                System.out.println("Not implemented");
                break;
            case 3:
                System.out.println("Not implemented");
                break;
            case 4:
                System.out.println("Not implemented");
                break;
            default:
                System.out.println("Make a valid selection please");
                break;
        }

        return dbname;
    }

    public static void ShowTables(String dbname) throws SQLException {
        Scanner input = new Scanner(System.in);
        Statement stmt = GetStatement(dbname);
        ResultSet results = stmt.executeQuery("SHOW TABLES");

        OneColumnResults(results);

        System.out.println("Now select the table: ");
        int auswahl = input.nextInt();

        DisplayTableContents(dbname, auswahl);
    }

    public static Statement GetStatement(String dbname) throws SQLException {
        // Die Funktion, die die Verbindung erstellt und

        MysqlDataSource mysqlDataSource = EstablishContact(dbname);
        Connection conn = mysqlDataSource.getConnection();

        return conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
    }

    public static int OneColumnResults(ResultSet results) {
        // einfache Ausgabe von 1-spaltigen Tabellen

        int i = 0;

        try {
            while (!results.isLast()) {
                results.next();
                System.out.println("Table " + (i + 1) + ": " + results.getObject(1));
                i++;
            }
        } catch(Exception e) {
            System.out.println("No tables to be found.");
        }

        return i - 1;
    }

    public static MysqlDataSource EstablishContact(String dbname) {
        // veraltet
        // return DriverManager.getConnection(url, username, password);

        // Die Funktion, die die Verbindung zu dem Datenbank etabliert und ein Data Source zurückgibt

        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setUser("testuser");
        dataSource.setPassword("12345");
        dataSource.setServerName("localhost");
        dataSource.setDatabaseName(dbname);

        return dataSource;
    }

    public static void DisplayTableContents(String dbName, int tableNumber) throws SQLException {
        Statement stmt = GetStatement(dbName);
        String format = "%-20s";

        ResultSet results = stmt.executeQuery("SHOW TABLES");

        results.absolute(tableNumber);

        String tableName = results.getString(1);

        System.out.println("Ausgewahlte Tabelle: " + tableName + ".");

        // Beide dimensionen erfahren
        results = stmt.executeQuery("SELECT COUNT(*) from " + tableName);
        results.next();
        int rows = results.getInt(1);

        results = stmt.executeQuery("SELECT COUNT(*) FROM information_schema.columns WHERE table_name = '" + tableName + "'");
        results.next();
        int columns = results.getInt(1);

        // Spaltenamen Ausgabe
        results = stmt.executeQuery("SELECT COLUMN_NAME\n" +
                "FROM INFORMATION_SCHEMA.COLUMNS\n" +
                "WHERE TABLE_NAME = '" + tableName + "';");


        for (int j = 1; j <= columns; j++) {
            results.next();
            System.out.print(ANSI_YELLOW);
            System.out.printf(format, results.getObject(1));
            System.out.print(ANSI_RESET);
        }

        // newline before the table contents
        System.out.println("");

        // Tabelleninhalte Ausgabe
        results = stmt.executeQuery("SELECT * from " + tableName);

        for (int i = 0; i < rows; i++) {
            results.next();
            for (int j = 1; j <= columns; j++) {
                System.out.printf(format, results.getObject(j) );
            }
            System.out.println("");
        }
    }
}