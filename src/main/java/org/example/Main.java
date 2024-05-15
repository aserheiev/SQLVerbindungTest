package org.example;

import com.mysql.cj.jdbc.MysqlDataSource;

import javax.xml.crypto.Data;
import java.sql.*;
import java.util.*;

public class Main {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";

    public static void main(String[] args) throws RuntimeException, SQLException {
        // general notes: add a lot of checks.
        // currently it's possible to try and open a table in an empty DB, which isn't good
        // there's an input mismatch somewhere when opening DBs
        // in general it's just sanitizing inputs everywhere

        // old deprecated connection method, leaving it for reference purposes
        // String url = "jdbc:mysql://localhost:3306/kunden";
        // String username = "testuser";
        // String password = "12345";
        // Connection connection = EstablishContact(url, username, password);
        boolean loggedin = false;
        MysqlDataSource dataSource = EstablishContact(null);

        while (!loggedin) {
            try {
                GetStatement(dataSource, null);
                loggedin = true;
                System.out.println(ANSI_GREEN + "Connection established! System entering normal mode." + ANSI_RESET);
                System.out.println(ANSI_GREEN + "The current server is: " + ANSI_YELLOW + dataSource.getServerName() + ANSI_RESET);
            } catch (Exception e) {
                System.out.println(ANSI_RED + "Login failed. Please make another attempt." + ANSI_RESET);
                dataSource = EstablishContact(null);
            }
        }

        // col. jankowski is here to serve
        while (true) {
            MainMenu(dataSource);
        }
    }

    public static boolean MainMenu(MysqlDataSource dataSource) throws SQLException {
        Scanner input = new Scanner(System.in);
        int command;
        boolean selectionValid = false;
        dataSource.setDatabaseName(null);
        System.out.println(ANSI_GREEN + "Welcome to the DB, Manager. What would you like to do?" + ANSI_RESET);

        System.out.println("1. Open a DB");
        System.out.println("2. Create a new DB");
        System.out.println("3. Delete a DB");
        System.out.println("4. Exit");

        while (!selectionValid) {
            command = input.nextInt();

            switch (command) {
                case 1:
                    selectionValid = true;
                    GetDBList(dataSource);
                    break;
                case 2:
                    selectionValid = true;
                    CreateDB(dataSource);
                    break;
                case 3:
                    selectionValid = true;
                    DeleteDB(dataSource);
                    break;
                case 4:
                    System.exit(0);
                    break;
                default:
                    System.out.println(ANSI_RED + "Make a valid selection please" + ANSI_RESET);
                    break;
            }
        }
        return false;
    }

    public static void CreateDB(MysqlDataSource dataSource) throws SQLException {
        Scanner input = new Scanner(System.in);
        System.out.println("Please enter the DB name to be created.");
        String dbname = input.nextLine();
        boolean succeeded = true;

        Statement stmt = GetStatement(dataSource, null);

        try {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbname);
        } catch (Exception e) {
            // needs better exceptions
            System.out.println("FAILURE. You probably don't have the admin privileges required to do that, or it already exists.");
            succeeded = false;
        }

        if (succeeded) {
            System.out.println(ANSI_BLUE + "GREAT SUCCESS" + ANSI_RESET);
        }
    }

    public static void DeleteDB(MysqlDataSource dataSource) throws SQLException {
        Scanner input = new Scanner(System.in);
        System.out.println("Please enter the DB name to be deleted.");
        String dbname = input.nextLine();
        boolean succeeded = true;

        Statement stmt = GetStatement(dataSource, null);

        // needs a check to see whether it exists (or see how I can get the SQL response that says that)

        try {
            stmt.executeUpdate("DROP DATABASE " + dbname);
        } catch (Exception e) {
            System.out.println("FAILURE. You probably don't have the admin privileges required to do that, or it doesn't exist.");
            succeeded = false;
        }

        if (succeeded) {
            System.out.println(ANSI_BLUE + "GREAT SUCCESS" + ANSI_RESET);
        }
    }

    public static void RenameDB() throws SQLException {
        System.out.println("Not in MySQL I'm afraid :)");
        System.out.println("A solution is coming someday somehow");
        // apparently impossible in MariaDB? My god
        // create a new DB with the same exact charset/collation, move all tables, and then delete the old?
        // will this nuke any other settings?
        // I'll save this for the end
    }


    public static String GetDBList(MysqlDataSource dataSource) throws SQLException {
        // Zeigt alle Datenbanken, und dann nach Auswahl zeigt ihre Tabellen
        // und DANN nach Auswahl gibt ihre Inhalte aus

        int auswahl;
        Scanner input = new Scanner(System.in);
        String dbname = null;

        Statement stmt = GetStatement(dataSource, null);

        System.out.println("Currently accessible DBs are:");

        ResultSet results = stmt.executeQuery("SHOW DATABASES");

        OneColumnResults(results);

        System.out.println("1. Open DB");
        System.out.println("2. Rename DB");
        System.out.println("3. Delete DB");
        System.out.println("4. Back");

        auswahl = GetValue();

        switch (auswahl) {
            case 1:
                System.out.println("Oh yeah, which one?");
                auswahl = input.nextInt();
                results.absolute(auswahl);
                dbname = results.getString(1);
                ShowTables(dataSource, dbname);
                break;
            case 2:
                RenameDB();
                break;
            case 3:
                DeleteDB(dataSource);
                break;
            case 4:
                break;
            default:
                System.out.println("Make a valid selection please");
                break;
        }

        return dbname;
    }

    public static void ShowTables(MysqlDataSource dataSource, String dbname) throws SQLException {
        Scanner input = new Scanner(System.in);
        Statement stmt = GetStatement(dataSource, dbname);
        ResultSet results = stmt.executeQuery("SHOW TABLES");
        boolean validAnswer = false;
        OneColumnResults(results);

        System.out.println("But what now: \n1. Open table\n2. Create new table\n3. Delete table\n4. Back");


        while (!validAnswer) {
            int auswahl = GetValue();
            switch (auswahl) {
                case 1:
                    validAnswer = true;
                    System.out.println("Ok but which one: ");
                    int chosenTable = GetValue();
                    DisplayTableContents(dataSource, dbname, chosenTable);
                    break;
                case 2:
                    validAnswer = true;
                    CreateTable(dataSource, dbname);
                    break;
                case 3:
                    validAnswer = true;
                    break;
                case 4:
                    validAnswer = true;
                    break;
                default:
                    System.out.println("Does not compute");
                    break;
            }
        }
    }

    public static Statement GetStatement(MysqlDataSource dataSource, String dbname) throws SQLException {
        // Die Funktion, die die Verbindung erstellt und

        dataSource.setDatabaseName(dbname);
        Connection conn = dataSource.getConnection();

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
        } catch (Exception e) {
            System.out.println("No tables to be found.");
        }

        return i - 1;
    }

    public static MysqlDataSource EstablishContact(String dbname) {
        // veraltet
        // return DriverManager.getConnection(url, username, password);

        // Die Funktion, die die Verbindung zu dem Datenbank etabliert und ein Data Source zurückgibt
        Scanner input = new Scanner(System.in);
        System.out.println(ANSI_GREEN + "Welcome to the DB test program v. 0.1." + ANSI_RESET + "\n1. My database is remote\n2. My database is local");
        int auswahl = 0;
        boolean validAnswer = false;
        String username;

        // this is probably a terrible idea
        String password;

        String serverName = "";

        while (!validAnswer) {
            auswahl = GetValue();
            switch (auswahl) {
                case 1:
                    validAnswer = true;
                    break;
                case 2:
                    validAnswer = true;
                    serverName = "localhost";
                    break;
                default:
                    System.out.println("Does not compute");
                    break;
            }
        }

        if (auswahl == 1) {
            System.out.println("Enter server address");
            serverName = input.nextLine();
        }

        System.out.println("Enter your username:");
        username = input.nextLine();
        System.out.println("Enter your password:");
        password = input.nextLine();

        System.out.println("");

        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setUser(username);
        dataSource.setPassword(password);
        dataSource.setServerName(serverName);
        // dataSource.setDatabaseName(dbname);

        return dataSource;
    }

    public static void DisplayTableContents(MysqlDataSource dataSource, String dbName, int tableNumber) throws SQLException {
        Statement stmt = GetStatement(dataSource, dbName);
        String format = "%-30s";

        ResultSet results = stmt.executeQuery("SHOW TABLES");

        results.absolute(tableNumber);

        String tableName = results.getString(1);

        System.out.println("Ausgewahlte Tabelle: " + tableName + ".");

        // Beide dimensionen erfahren
        results = stmt.executeQuery("SELECT COUNT(*) from " + tableName);
        results.next();
        int rows = results.getInt(1);

        System.out.println("ROWS: " + rows);

        results = stmt.executeQuery("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = '" + dbName + "' AND table_name = '" + tableName + "'");
        results.next();
        int columns = results.getInt(1);

        System.out.println("COLUMNS: " + columns);

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
                System.out.printf(format, results.getObject(j));
            }
            System.out.println("");
        }
    }

    public static void CreateTable(MysqlDataSource dataSource, String dbName) throws SQLException {

        enum Charset {

            ascii("ASCII"),
            utf8("UTF8"),
            utf16("UTF16"),
            utf32("UTF32");

            final String label;

            // I need these constructors to have the string descriptions
            Charset(String label) {
                this.label = label;
            }
        }

        enum Datatype {
            INT("Integer", false),
            VARCHAR("String", true),
            BOOLEAN("Boolean", false),
            FLOAT("Float", false),
            DOUBLE("Double", false),
            DECIMAL("Decimal", true);

            final String label;
            final boolean hasLength;

            Datatype (String label, boolean hasLength) {
                this.label = label;
                this.hasLength = hasLength;
            }
        }

        String comment = "";

        Scanner input = new Scanner(System.in);
        String tableName;
        String primaryKey = "";
        boolean primaryKeyDefined = false;

        Hashtable<String, String> columns = new Hashtable<String, String>();

        int menuAuswahl;
        boolean doneCreating = false;
        Charset currentCharset = Charset.utf8;



        System.out.println("Enter table name: ");

        // add checks?
        tableName = input.nextLine();

        while (!doneCreating) {
            System.out.println("TABLE CREATION MENU");

            System.out.println(ANSI_BLUE + "Current Table: ");
            System.out.println("Table name: " + tableName);
            System.out.println("Charset: " + currentCharset.label);
            System.out.println("Collation: Default collation" + ANSI_RESET);

            // here I will print out the current table structure
            // nullables?
            Enumeration<String> enumeration = columns.keys();
            while (enumeration.hasMoreElements()) {
                String currentColumn = enumeration.nextElement();

                if (currentColumn.equals(primaryKey)) {
                    System.out.println(ANSI_YELLOW + currentColumn + ANSI_RESET + ANSI_BLUE + "(" + columns.get(currentColumn) + ")" + ANSI_RED + " - PRIMARY KEY" + ANSI_RESET);
                } else {
                    System.out.println(ANSI_YELLOW + currentColumn + ANSI_RESET + ANSI_BLUE + "(" + columns.get(currentColumn) + ")" + ANSI_RESET);
                }
            }

            System.out.println("1. Add column");
            System.out.println("2. Remove column");
            System.out.println("3. Change character set");

            if (!primaryKeyDefined) {
                System.out.println("4. Define or change primary key" + ANSI_RED + " (MANDATORY!)" + ANSI_RESET);
            } else {
                System.out.println("4. Define or change primary key");
            }

            System.out.println("5. Confirm selection and create table");
            System.out.println("6. Change table name");
            System.out.println("7. Add or replace comment for the table");
            System.out.println("8. Cancel and quit");

            menuAuswahl = GetValue();

            switch (menuAuswahl) {
                case 1:
                    System.out.println("Enter column name: ");
                    String columnName = input.nextLine();
                    int typeLength = 0;
                    String dataTypeString;

                    System.out.println("Select data type: ");
                    int jank = 0;
                    Datatype selectedType = Datatype.INT;
                    for (Datatype info : EnumSet.allOf(Datatype.class)) {
                        jank++;
                        System.out.println(jank + ". " + info.label);
                    }

                    int typAuswahl = GetValue();
                    jank = 0;

                    // this is horrible and needs to go in the next iteration
                    // also needs range checks above
                    for (Datatype info : EnumSet.allOf(Datatype.class)) {
                        jank++;

                        if (typAuswahl == jank) {
                            selectedType = info;
                        }
                    }

                    System.out.println("You have selected the data type: " + selectedType.label);

                    if (selectedType.hasLength) {
                        System.out.println("Enter desired length: ");
                        typeLength = GetValue();
                        // range checks for types
                    }

                    if (typeLength > 0) {
                        dataTypeString = selectedType.name() + "(" + typeLength + ")";
                    } else {
                        dataTypeString = selectedType.name();
                    }

                    columns.put(columnName, dataTypeString);
                    break;

                case 2:
                    System.out.println("Which column should be removed?");

                    // don't do this kind of naming kids
                    String theLastColumnThatShallBeDestroyed = input.nextLine();
                    columns.remove(theLastColumnThatShallBeDestroyed);
                    break;
                case 4:
                    System.out.println("Please select which column(s) should be used as a primary key.");
                    primaryKey = input.nextLine();
                    if (columns.containsKey(primaryKey)) {
                        System.out.println("Got it");
                        primaryKeyDefined = true;
                    } else {
                        System.out.println("YOU FOOL");
                    }
                    break;
                case 5:
                    doneCreating = true;
                    break;
                case 6:
                    System.out.println("Please enter the new table name: ");
                    tableName = input.nextLine();
                    break;
                case 7:
                    System.out.println("Please enter the comment:");
                    comment = input.nextLine();
                    break;
                case 8:
                    doneCreating = true;
                    break;
                default:
                    System.out.println("Try again");
                    break;
            }
        }

        Statement stmt = GetStatement(dataSource, dbName);

        String columnQuery = "CREATE TABLE `" + dbName + "`.`" + tableName + "` (\n";

        Enumeration<String> enumeration = columns.keys();

        while (enumeration.hasMoreElements()) {
            String currentColumn = enumeration.nextElement();

            if (currentColumn.equals(primaryKey)) {
                columnQuery += "`" + currentColumn + "` " + columns.get(currentColumn) + " NOT NULL,\n";
            } else {
                columnQuery += "`" + currentColumn + "` " + columns.get(currentColumn) + " NULL,\n";
            }
        }

        columnQuery += "PRIMARY KEY (`" + primaryKey + "`))\n" +
                "ENGINE = InnoDB\n" +
                "DEFAULT CHARACTER SET = " + currentCharset.name() + "\n" +
                "COMMENT = '" + comment + "';";

            stmt.executeUpdate(columnQuery);

    }

    public static int GetValue() {
        int zahl = 0;
        Scanner input = new Scanner(System.in);
        boolean valid = false;

        while (!valid) {
            try {
                zahl = Integer.parseInt(input.nextLine());
                return zahl;
            } catch (Exception e) {
                // Zahl ist keine Zahl
                System.out.println("Give me a real number chief");
            }
        }

        // divine light severed
        return zahl;
    }
}