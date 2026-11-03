package org.paloma.repositorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import java.sql.Date;

import org.paloma.modelo.Escritor;
import org.paloma.modelo.Libro;
import org.paloma.persistencia.ConexionBD;

/**
 * Repositorio de la raíz agregada {@link Escritor}.
 * Sus libros se cargan en modo lectura; la autoría la persiste {@link LibroRepositorio}.
 */
public class EscritorRepositorio implements IRepositorioExtend<Escritor, Integer> {

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM escritor";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getLong(1) : 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error al contar los escritores", e);
        }
    }

    @Override
    public void deleteById(Integer id) {
        validarId(id);
        String sql = "DELETE FROM escritor WHERE idEscritor = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar con ID: " + id, e);
        }
    }

    @Override
    public void deleteAll() {
        String sql = "DELETE FROM escritor";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar los escritores", e);
        }
    }

    @Override
    public boolean existsById(Integer id) {
        validarId(id);
        String sql = "SELECT 1 FROM escritor WHERE idEscritor = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al comprobar existencia por ID: " + id, e);
        }
    }

    @Override
    public Escritor findById(Integer id) {
        return findByIdOptional(id).orElse(null);
    }

    @Override
    public Optional<Escritor> findByIdOptional(Integer id) {
        validarId(id);
        List<Escritor> lista = buscarVarios("SELECT * FROM escritor WHERE idEscritor = ?", id,
                "Error al buscar por ID: " + id);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    @Override
    public List<Escritor> findAllToList() {
        return buscarVarios("SELECT * FROM escritor", null, "Error al obtener los escritores");
    }

    @Override
    public Iterable<Escritor> findAll() {
        return findAllToList();
    }

    @Override
    public <S extends Escritor> S save(S entity) {
        if (entity == null) {
            throw new IllegalArgumentException("La entidad no puede ser nula");
        }
        // Insertar si es nuevo (id 0); si trae id, debe existir previamente (buscar -> modificar -> guardar)
        boolean esNuevo = entity.getIdEscritor() == 0;
        if (!esNuevo && !existsById(entity.getIdEscritor())) {
            throw new IllegalArgumentException("No existe ningún escritor con id " + entity.getIdEscritor());
        }

        String sql = esNuevo
                ? "INSERT INTO escritor (nombre, biografia) VALUES (?, ?)"
                : "UPDATE escritor SET nombre = ?, biografia = ? WHERE idEscritor = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, entity.getNombre());
            ps.setString(2, entity.getBiografia());

            if (!esNuevo) {
                ps.setInt(3, entity.getIdEscritor());
            }

            ps.executeUpdate();

            if (esNuevo) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        entity.setIdEscritor(rs.getInt(1));
                    }
                }
            }
            return entity;

        } catch (SQLException e) {
            throw new RuntimeException(
                    esNuevo ? "Error al insertar" : "Error al actualizar", e);
        }
    }

    /** Devuelve los escritores cuyo nombre contiene el texto indicado. */
    public List<Escritor> findByNombreContaining(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("El texto no puede ser nulo");
        }
        return buscarVarios("SELECT * FROM escritor WHERE nombre LIKE ?", "%" + texto + "%",
                "Error al buscar por nombre: " + texto);
    }

    /** Devuelve los escritores de un libro (coautorías incluidas). */
    public List<Escritor> findByLibro(Integer idLibro) {
        validarId(idLibro);
        return buscarVarios(
                "SELECT e.* FROM escritor e JOIN autoria a ON a.idEscritor = e.idEscritor WHERE a.idLibro = ?",
                idLibro, "Error al buscar escritores del libro: " + idLibro);
    }

   
    private List<Escritor> buscarVarios(String sql, Object parametro, String mensajeError) {
        List<Escritor> lista = new ArrayList<>();

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (parametro != null) {
                ps.setObject(1, parametro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            for (Escritor escritor : lista) {
                cargarLibros(conn, escritor);
            }

        } catch (SQLException e) {
            throw new RuntimeException(mensajeError, e);
        }
        return lista;
    }

    private void cargarLibros(Connection conn, Escritor escritor) throws SQLException {
        String sql = "SELECT l.idLibro, l.ISBN, l.titulo, l.genero, l.fecha_publicacion FROM libro l "
                + "JOIN autoria a ON a.idLibro = l.idLibro WHERE a.idEscritor = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, escritor.getIdEscritor());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date fecha = rs.getDate("fecha_publicacion");
                    Libro libro = new Libro(rs.getInt("idLibro"), rs.getString("ISBN"), rs.getString("titulo"),
                            rs.getString("genero"), fecha != null ? fecha.toLocalDate() : null);
                    escritor.getLibros().add(libro);
                }
            }
        }
    }

    private Escritor mapear(ResultSet rs) throws SQLException {
        Escritor escritor = new Escritor();
        escritor.setIdEscritor(rs.getInt("idEscritor"));
        escritor.setNombre(rs.getString("nombre"));
        escritor.setBiografia(rs.getString("biografia"));
        return escritor;
    }

    private void validarId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("El id no puede ser nulo");
        }
    }
}
