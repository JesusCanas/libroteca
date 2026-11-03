package org.paloma.repositorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.paloma.modelo.Libro;
import org.paloma.persistencia.ConexionBD;

public class LibroRepositorio implements IRepositorioExtend<Libro, Integer> {

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM libro";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getLong(1) : 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error al contar los libros", e);
        }
    }

    @Override
    public void deleteById(Integer id) {
        validarId(id);
        String sql = "DELETE FROM libro WHERE idLibro = ?";

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
        String sql = "DELETE FROM libro";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar los libros", e);
        }
    }

    @Override
    public boolean existsById(Integer id) {
        validarId(id);
        String sql = "SELECT 1 FROM libro WHERE idLibro = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            throw new RuntimeException("Error al comprobar existencia por ID: " + id, e);
        }
    }

    @Override
    public Libro findById(Integer id) {
        return findByIdOptional(id).orElse(null);
    }

    @Override
    public Optional<Libro> findByIdOptional(Integer id) {
        validarId(id);
        String sql = "SELECT * FROM libro WHERE idLibro = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return Optional.of(mapear(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar por ID: " + id, e);
        }
    }

    @Override
    public List<Libro> findAllToList() {
        List<Libro> lista = new ArrayList<>();
        String sql = "SELECT * FROM libro";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener los libros", e);
        }
        return lista;
    }

    @Override
    public Iterable<Libro> findAll() {
        return findAllToList();
    }

    @Override
    public <S extends Libro> S save(S entity) {
        if (entity == null) {
            throw new IllegalArgumentException("La entidad no puede ser nula");
        }
        boolean esNuevo = entity.getIdLibro() == 0;

        String sql = esNuevo
                ? "INSERT INTO libro (ISBN, titulo, genero, fecha_publicacion) VALUES (?, ?, ?, ?)"
                : "UPDATE libro SET ISBN = ?, titulo = ?, genero = ?, fecha_publicacion = ? WHERE idLibro = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, entity.getIsbn());
            ps.setString(2, entity.getTitulo());
            ps.setString(3, entity.getGenero());
            ps.setObject(4, entity.getFechaPublicacion());

            if (!esNuevo) {
                ps.setInt(5, entity.getIdLibro());
            }

            ps.executeUpdate();

            if (esNuevo) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    entity.setIdLibro(rs.getInt(1));
                }
            }
            return entity;

        } catch (SQLException e) {
            throw new RuntimeException(
                    esNuevo ? "Error al insertar" : "Error al actualizar", e);
        }
    }

    private Libro mapear(ResultSet rs) throws SQLException {
        Libro libro = new Libro();
        libro.setIdLibro(rs.getInt("idLibro"));
        libro.setIsbn(rs.getString("ISBN"));
        libro.setTitulo(rs.getString("titulo"));
        libro.setGenero(rs.getString("genero"));
        Date fecha = rs.getDate("fecha_publicacion");
        libro.setFechaPublicacion(fecha != null ? fecha.toLocalDate() : null);
        return libro;
    }

    private void validarId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("El id no puede ser nulo");
        }
    }
}
