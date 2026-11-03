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

import org.paloma.modelo.Libro;
import org.paloma.modelo.Resena;
import org.paloma.modelo.Usuario;
import org.paloma.persistencia.ConexionBD;

/**
 * Repositorio de la raíz agregada {@link Usuario}.
 * Sus reseñas se cargan en modo lectura; se guardan con {@link ResenaRepositorio}.
 */
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
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

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
        List<Usuario> lista = buscarVarios("SELECT * FROM usuarios WHERE idUsuario = ?", id,
                "Error al buscar por ID: " + id);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    @Override
    public List<Usuario> findAllToList() {
        return buscarVarios("SELECT * FROM usuarios", null, "Error al obtener los usuarios");
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
        // Insertar si es nuevo (id 0); si trae id, debe existir previamente (buscar -> modificar -> guardar)
        boolean esNuevo = entity.getIdUsuario() == 0;
        if (!esNuevo && !existsById(entity.getIdUsuario())) {
            throw new IllegalArgumentException("No existe ningún usuario con id " + entity.getIdUsuario());
        }
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
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        entity.setIdUsuario(rs.getInt(1));
                    }
                }
            }
            return entity;

        } catch (SQLException e) {
            throw new RuntimeException(
                    esNuevo ? "Error al insertar" : "Error al actualizar", e);
        }
    }

    // ------------------------------------------------------------------
    // Métodos propios del repositorio de usuarios
    // ------------------------------------------------------------------

    /** Devuelve el usuario con ese email (único) o Optional#empty() si no existe. */
    public Optional<Usuario> findByEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("El email no puede ser nulo");
        }
        List<Usuario> lista = buscarVarios("SELECT * FROM usuarios WHERE email = ?", email,
                "Error al buscar por email: " + email);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    /** Devuelve los usuarios inscritos en la fecha indicada o posteriormente. */
    public List<Usuario> findByInscritoDesde(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula");
        }
        return buscarVarios("SELECT * FROM usuarios WHERE fecha_inscripcion >= ?", fecha,
                "Error al buscar usuarios inscritos desde " + fecha);
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    /** Ejecuta la consulta (con 0 o 1 parámetro) y devuelve los usuarios con sus reseñas cargadas. */
    private List<Usuario> buscarVarios(String sql, Object parametro, String mensajeError) {
        List<Usuario> lista = new ArrayList<>();

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
            for (Usuario usuario : lista) {
                cargarResenas(conn, usuario);
            }

        } catch (SQLException e) {
            throw new RuntimeException(mensajeError, e);
        }
        return lista;
    }

    private void cargarResenas(Connection conn, Usuario usuario) throws SQLException {
        String sql = "SELECT r.idResena, r.comentarios, r.valoracion, "
                + "l.idLibro, l.ISBN, l.titulo, l.genero, l.fecha_publicacion "
                + "FROM resenas r JOIN libro l ON l.idLibro = r.idLibro WHERE r.idUsuario = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuario.getIdUsuario());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date fecha = rs.getDate("fecha_publicacion");
                    Libro libro = new Libro(rs.getInt("idLibro"), rs.getString("ISBN"), rs.getString("titulo"),
                            rs.getString("genero"), fecha != null ? fecha.toLocalDate() : null);
                    Resena resena = new Resena(rs.getInt("idResena"), rs.getString("comentarios"),
                            rs.getInt("valoracion"), null, libro);
                    usuario.agregarResena(resena);
                }
            }
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
