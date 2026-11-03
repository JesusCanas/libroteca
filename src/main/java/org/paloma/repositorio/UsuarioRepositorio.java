package org.paloma.repositorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.paloma.modelo.Usuario;
import org.paloma.persistencia.ConexionBD;

public class UsuarioRepositorio implements IRepositorioExtend<Usuario, Integer> {

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM usuarios";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getLong(1) : 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error al contar los usuarios", e);
        }
    }

    @Override
    public void deleteById(Integer id) {
        validarId(id);
        String sql = "DELETE FROM usuarios WHERE idUsuario = ?";

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
        String sql = "DELETE FROM usuarios";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar los usuarios", e);
        }
    }

    @Override
    public boolean existsById(Integer id) {
        validarId(id);
        String sql = "SELECT 1 FROM usuarios WHERE idUsuario = ?";

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
    public Usuario findById(Integer id) {
        return findByIdOptional(id).orElse(null);
    }

    @Override
    public Optional<Usuario> findByIdOptional(Integer id) {
        validarId(id);
        String sql = "SELECT * FROM usuarios WHERE idUsuario = ?";

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
    public List<Usuario> findAllToList() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT * FROM usuarios";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener los usuarios", e);
        }
        return lista;
    }

    @Override
    public Iterable<Usuario> findAll() {
        return findAllToList();
    }

    @Override
    public <S extends Usuario> S save(S entity) {
        if (entity == null) {
            throw new IllegalArgumentException("La entidad no puede ser nula");
        }
        boolean esNuevo = entity.getIdUsuario() == 0;
        // fecha_inscripcion es NOT NULL: si no viene informada se usa hoy
        LocalDate fecha = entity.getFechaInscripcion() != null ? entity.getFechaInscripcion() : LocalDate.now();

        String sql = esNuevo
                ? "INSERT INTO usuarios (nombre, email, fecha_inscripcion) VALUES (?, ?, ?)"
                : "UPDATE usuarios SET nombre = ?, email = ?, fecha_inscripcion = ? WHERE idUsuario = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, entity.getNombre());
            ps.setString(2, entity.getEmail());
            ps.setObject(3, fecha);

            if (!esNuevo) {
                ps.setInt(4, entity.getIdUsuario());
            }

            ps.executeUpdate();

            if (esNuevo) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    entity.setIdUsuario(rs.getInt(1));
                }
            }
            return entity;

        } catch (SQLException e) {
            throw new RuntimeException(
                    esNuevo ? "Error al insertar" : "Error al actualizar", e);
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(rs.getInt("idUsuario"));
        usuario.setNombre(rs.getString("nombre"));
        usuario.setEmail(rs.getString("email"));
        Date fecha = rs.getDate("fecha_inscripcion");
        usuario.setFechaInscripcion(fecha != null ? fecha.toLocalDate() : null);
        return usuario;
    }

    private void validarId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("El id no puede ser nulo");
        }
    }
}
