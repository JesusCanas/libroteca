package org.paloma.persistencia;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConexionBD {

    private static final String RUTA_CONFIG = "db.properties"; // relativa a la raíz del proyecto

    private ConexionBD() {
    }

    public static Connection getConexion() throws SQLException {
        Properties config = new Properties();
        try (FileInputStream fis = new FileInputStream(RUTA_CONFIG)) {
            config.load(fis);
        } catch (IOException e) {
            throw new SQLException("No se pudo leer " + RUTA_CONFIG, e);
        }

        String url = "jdbc:mysql://" + config.getProperty("servidor") + ":"
                + config.getProperty("puerto") + "/" + config.getProperty("nombreBD");

        Properties props = new Properties();
        props.setProperty("user", config.getProperty("usuario"));
        props.setProperty("password", config.getProperty("contrasenna"));
        props.setProperty("serverTimezone", "UTC");

        return DriverManager.getConnection(url, props);
    }
}