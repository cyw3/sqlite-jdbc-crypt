package org.sqlite;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import org.junit.jupiter.api.Test;
import org.sqlite.mc.*;

public class SQLiteMCPragmaTest {

    private static final String SQL_TABLE =
            "CREATE TABLE IF NOT EXISTS warehouses ("
                    + "	id integer PRIMARY KEY,"
                    + "	name text NOT NULL,"
                    + "	capacity real"
                    + ");";

    public String createFile() throws IOException {
        File tmpFile = File.createTempFile("tmp-sqlite", ".db");
        tmpFile.deleteOnExit();
        return tmpFile.getAbsolutePath();
    }

    public boolean databaseIsReadable(Connection connection) {
        if (connection == null) return false;
        try {
            Statement st = connection.createStatement();
            ResultSet resultSet = st.executeQuery("SELECT count(*) as nb FROM sqlite_master");
            resultSet.next();
            // System.out.println("The out is : " + resultSet.getString("nb"));
            assertEquals(
                    "1",
                    resultSet.getString("nb"),
                    "When reading the database, the result should contain the number 1");
            return true;
        } catch (SQLException e) {
            // System.out.println(e.getMessage());
            return false;
        }
    }

    public void applySchema(Connection connection) throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.execute(SQL_TABLE);
    }

    public void plainDatabaseCreate(String dbPath) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:file:" + dbPath);
        applySchema(conn);
        conn.close();
    }

    public void cipherDatabaseCreate(SQLiteMCConfig config, String dbPath, String key)
            throws SQLException {
        Connection connection = config.withKey(key).createConnection("jdbc:sqlite:file:" + dbPath);
        applySchema(connection);
        connection.close();
    }

    public Connection plainDatabaseOpen(String dbPath) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:file:" + dbPath);
    }

    @Test
    public void plainDatabaseTest() throws IOException, SQLException {
        String path = createFile();
        // 1.  Open + Write
        plainDatabaseCreate(path);

        // 2. Ensure another Connection can read the databse written
        Connection c = plainDatabaseOpen(path);
        assertTrue(databaseIsReadable(c), "The plain database should be always readable");
        c.close();
    }

    public Connection cipherDatabaseOpen(SQLiteMCConfig config, String dbPath, String key)
            throws SQLException {
        try {
            return config.withKey(key).createConnection("jdbc:sqlite:file:" + dbPath);
        } catch (SQLiteException e) {
            return null;
        }
    }

    public void genericDatabaseTest(SQLiteMCConfig config) throws IOException, SQLException {
        String path = createFile();
        // 1. Open + Write + cipher with "Key1" key
        String Key1 = "Key1";
        String Key2 = "Key2";

        cipherDatabaseCreate(config, path, Key1);

        // 2. Ensure db is readable with good Password
        Connection c = cipherDatabaseOpen(config, path, Key1);
        assertTrue(
                databaseIsReadable(c),
                String.format(
                        "1. Be sure the database with config %s can be read with the key '%s'",
                        config.getClass().getSimpleName(), Key1));
        c.close();

        // 3. Ensure db is not readable without the good password (Using Key2 as password)
        c = cipherDatabaseOpen(config, path, Key2);
        assertNull(
                c,
                String.format(
                        "2 Be sure the database with config %s cannot be read with the key '%s' (good key is %s)",
                        config.getClass().getSimpleName(), Key2, Key1));

        // 4. Rekey the database
        c = cipherDatabaseOpen(config, path, Key1);
        assertTrue(
                databaseIsReadable(c),
                String.format(
                        "3. Be sure the database with config %s can be read before rekeying with the key '%s' (replacing %s with %s)",
                        config.getClass().getSimpleName(), Key2, Key1, Key2));
        c.createStatement().execute(String.format("PRAGMA rekey=%s", Key2));
        assertTrue(
                databaseIsReadable(c), "4. Be sure the database is still readable after rekeying");
        c.close();

        // 5. Should now be readable with Key2
        c = cipherDatabaseOpen(config, path, Key2);
        assertTrue(
                databaseIsReadable(c),
                String.format(
                        "5. Should now be able to open the database with config %s and the new key '%s'",
                        config.getClass().getSimpleName(), Key2));
        c.close();
    }

    @Test
    public void chacha20DatabaseTest() throws SQLException, IOException {
        genericDatabaseTest(SQLiteMCChacha20Config.getDefault());
    }

    @Test
    public void aes128cbcDatabaseTest() throws IOException, SQLException {
        genericDatabaseTest(SQLiteMCWxAES128Config.getDefault());
    }

    @Test
    public void aes256cbcDatabaseTest() throws IOException, SQLException {
        genericDatabaseTest(SQLiteMCWxAES256Config.getDefault());
    }

    @Test
    public void sqlCipherDatabaseTest() throws IOException, SQLException {
        genericDatabaseTest(SQLiteMCSqlCipherConfig.getDefault());
    }

    @Test
    public void RC4DatabaseTest() throws IOException, SQLException {
        genericDatabaseTest(SQLiteMCRC4Config.getDefault());
    }

    @Test
    public void defaultCihperDatabaseTest() throws IOException, SQLException {
        genericDatabaseTest(new SQLiteMCConfig());
    }

    @Test
    public void defaultCihperDatabaseWithSpecialKeyTest() throws IOException, SQLException {
        SQLiteMCConfig config = new SQLiteMCConfig();
        String path = createFile();
        // 1. Open + Write + cipher with "Key1" key
        String Key1 = "Key1&az=uies%63";
        String Key2 = "Key1";

        cipherDatabaseCreate(config, path, Key1);

        // 2. Ensure db is readable with good Password
        Connection c = cipherDatabaseOpen(config, path, Key1);
        assertTrue(
                databaseIsReadable(c),
                String.format(
                        "1. Be sure the database with config %s can be read with the key '%s'",
                        config.getClass().getSimpleName(), Key1));
        c.close();

        // 3. Ensure db is not readable without the good password (Using Key2 as password)
        c = cipherDatabaseOpen(config, path, Key2);
        assertNull(
                c,
                String.format(
                        "2 Be sure the database with config %s cannot be read with the key '%s' (good key is %s)",
                        config.getClass().getSimpleName(), Key2, Key1));

        // 4. Rekey the database
        c = cipherDatabaseOpen(config, path, Key1);
        assertTrue(
                databaseIsReadable(c),
                String.format(
                        "3. Be sure the database with config %s can be read before rekeying with the key '%s' (replacing %s with %s)",
                        config.getClass().getSimpleName(), Key2, Key1, Key2));
        c.createStatement().execute(String.format("PRAGMA rekey=%s", Key2));
        assertTrue(
                databaseIsReadable(c), "4. Be sure the database is still readable after rekeying");
        c.close();

        // 5. Should now be readable with Key2
        c = cipherDatabaseOpen(config, path, Key2);
        assertTrue(
                databaseIsReadable(c),
                String.format(
                        "5. Should now be able to open the database with config %s and the new key '%s'",
                        config.getClass().getSimpleName(), Key2));
        c.close();
    }

    @Test
    public void crossCipherAlgorithmTest() throws IOException, SQLException {
        String dbfile = createFile();
        String key = "key";
        cipherDatabaseCreate(new SQLiteMCConfig(), dbfile, key);

        Connection c = cipherDatabaseOpen(new SQLiteMCConfig(), dbfile, key);
        assertTrue(databaseIsReadable(c), "Crosstest : Should be able to read the base db");
        c.close();

        c = cipherDatabaseOpen(SQLiteMCRC4Config.getDefault(), dbfile, key);
        assertNull(c, "Should not be readable with RC4");
        //        c.close();

        c = cipherDatabaseOpen(SQLiteMCSqlCipherConfig.getDefault(), dbfile, key);
        assertNull(c, "Should not be readable with SQLCipher");
        //        c.close();

        c = cipherDatabaseOpen(SQLiteMCWxAES128Config.getDefault(), dbfile, key);
        assertNull(c, "Should not be readable with Wx128bit");
        //        c.close();

        c = cipherDatabaseOpen(SQLiteMCWxAES256Config.getDefault(), dbfile, key);
        assertNull(c, "Should not be readable with Wx256");
        //        c.close();

        c = cipherDatabaseOpen(SQLiteMCChacha20Config.getDefault(), dbfile, key);
        assertTrue(databaseIsReadable(c), "Should be readable with Chacha20 as it is default");
        //        c.close();
    }

    @Test
    public void closeDeleteTest() throws IOException, SQLException {
        String dbfile = createFile();
        String key = "key";
        cipherDatabaseCreate(new SQLiteMCConfig(), dbfile, key);

        Connection c = cipherDatabaseOpen(new SQLiteMCConfig(), dbfile, key);
        assertTrue(databaseIsReadable(c), "Should be able to read the base db");
        c.close();

        c = cipherDatabaseOpen(SQLiteMCRC4Config.getDefault(), dbfile, key);
        assertNull(c, "Should not be readable with RC4");
        assertTrue(new File(dbfile).delete(), "Connection must be closed, should be deleted");
    }
}
