package org.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;
import java.util.Properties;
import org.junit.jupiter.api.Test;

public class SQLiteConfigTest {

    @Test
    public void toProperites() throws SQLException {
        SQLiteConfig config = new SQLiteConfig();

        config.setReadOnly(true);
        config.setDateStringFormat("yyyy/mm/dd");
        config.setDatePrecision("seconds");
        config.setDateClass("real");

        Properties properties = config.toProperties();

        assertEquals(
                "yyyy/mm/dd",
                properties.getProperty(SQLiteConfig.Pragma.DATE_STRING_FORMAT.getPragmaName()));
        assertEquals(
                SQLiteConfig.DatePrecision.SECONDS.name(),
                properties.getProperty(SQLiteConfig.Pragma.DATE_PRECISION.getPragmaName()));
        assertEquals(
                SQLiteConfig.DateClass.REAL.name(),
                properties.getProperty(SQLiteConfig.Pragma.DATE_CLASS.getPragmaName()));
    }

    @Test
    public void busyTimeout() {
        SQLiteConfig config = new SQLiteConfig();

        assertEquals(3000, config.getBusyTimeout());
        config.setBusyTimeout(1500);
        assertEquals(1500, config.getBusyTimeout());
    }
}
