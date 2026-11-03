package org.paloma.persistencia;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConexionBD {

    private static final String RUTA_CONFIG = "db.properties"; // en src/main/resources

    private ConexionBD() {
    }

    public static Connection getConexion() throws SQLException {
        Properties config = new Properties();
        try (InputStream is = ConexionBD.class.getClassLoader().getResourceAsStream(RUTA_CONFIG)) {
            if (is == null) {
                throw new SQLException("No se encontró " + RUTA_CONFIG + " en el classpath");
            }
            config.load(is);
        } catch (IOException e) {
            throw new SQLException("No se pudo leer " + RUTA_CONFIG, e);
        }

        return DriverManager.getConnection(
                config.getProperty("db.url"),
                config.getProperty("db.user"),
                config.getProperty("db.password"));
    }
}
