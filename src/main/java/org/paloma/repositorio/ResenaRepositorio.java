package org.paloma.repositorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.paloma.modelo.Libro;
import org.paloma.modelo.Resena;
import org.paloma.modelo.Usuario;
import org.paloma.persistencia.ConexionBD;

/**
 * Repositorio de {@link Resena}. Al leerla se carga también el usuario y el libro asociados.
 */
public class ResenaRepositorio implements IRepositorioExtend<Resena, Integer> {

    private static final String SELECT_BASE =
            "SELECT r.idResena, r.comentarios, r.valoracion, "
            + "u.idUsuario, u.nombre AS u_nombre, u.email AS u_email, u.fecha_inscripcion AS u_fecha, "
            + "l.idLibro, l.ISBN AS l_isbn, l.titulo AS l_titulo, l.genero AS l_genero, "
            + "l.fecha_publicacion AS l_fecha "
            + "FROM resenas r "
            + "JOIN usuarios u ON u.idUsuario = r.idUsuario "
            + "JOIN libro l ON l.idLibro = r.idLibro";

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM resenas";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getLong(1) : 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error al contar las reseñas", e);
        }
    }

    @Override
    public void deleteById(Integer id) {
        validarId(id);
        String sql = "DELETE FROM resenas WHERE idResena = ?";

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
        String sql = "DELETE FROM resenas";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar las reseñas", e);
        }
    }

    @Override
    public boolean existsById(Integer id) {
        validarId(id);
        String sql = "SELECT 1 FROM resenas WHERE idResena = ?";

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
    public Resena findById(Integer id) {
        return findByIdOptional(id).orElse(null);
    }

    @Override
    public Optional<Resena> findByIdOptional(Integer id) {
        validarId(id);
        List<Resena> lista = buscarVarios(SELECT_BASE + " WHERE r.idResena = ?", id,
                "Error al buscar por ID: " + id);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    @Override
    public List<Resena> findAllToList() {
        return buscarVarios(SELECT_BASE, null, "Error al obtener las reseñas");
    }

    @Override
    public Iterable<Resena> findAll() {
        return findAllToList();
    }

    @Override
    public <S extends Resena> S save(S entity) {
        if (entity == null) {
            throw new IllegalArgumentException("La entidad no puede ser nula");
        }
        // Insertar si es nueva (id 0); si trae id, debe existir previamente (buscar -> modificar -> guardar)
        boolean esNuevo = entity.getIdResena() == 0;
        if (!esNuevo && !existsById(entity.getIdResena())) {
            throw new IllegalArgumentException("No existe ninguna reseña con id " + entity.getIdResena());
        }
        if (entity.getUsuario() == null || entity.getLibro() == null) {
            throw new IllegalArgumentException("La reseña necesita usuario y libro");
        }

        String sql = esNuevo
                ? "INSERT INTO resenas (idUsuario, idLibro, comentarios, valoracion) VALUES (?, ?, ?, ?)"
                : "UPDATE resenas SET idUsuario = ?, idLibro = ?, comentarios = ?, valoracion = ? WHERE idResena = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, entity.getUsuario().getIdUsuario());
            ps.setInt(2, entity.getLibro().getIdLibro());
            ps.setString(3, entity.getComentarios());
            ps.setInt(4, entity.getValoracion());

            if (!esNuevo) {
                ps.setInt(5, entity.getIdResena());
            }

            ps.executeUpdate();

            if (esNuevo) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        entity.setIdResena(rs.getInt(1));
                    }
                }
            }
            return entity;

        } catch (SQLException e) {
            throw new RuntimeException(
                    esNuevo ? "Error al insertar" : "Error al actualizar", e);
        }
    }

    
    /** Devuelve las reseñas de un libro. */
    public List<Resena> findByLibro(Integer idLibro) {
        validarId(idLibro);
        return buscarVarios(SELECT_BASE + " WHERE r.idLibro = ?", idLibro,
                "Error al buscar reseñas del libro: " + idLibro);
    }

    /** Devuelve las reseñas escritas por un usuario. */
    public List<Resena> findByUsuario(Integer idUsuario) {
        validarId(idUsuario);
        return buscarVarios(SELECT_BASE + " WHERE r.idUsuario = ?", idUsuario,
                "Error al buscar reseñas del usuario: " + idUsuario);
    }

    /** Devuelve las reseñas con una valoración mayor o igual que la indicada (1 a 5). */
    public List<Resena> findByValoracionMinima(int valoracion) {
        if (valoracion < 1 || valoracion > 5) {
            throw new IllegalArgumentException("La valoración debe estar entre 1 y 5");
        }
        return buscarVarios(SELECT_BASE + " WHERE r.valoracion >= ?", valoracion,
                "Error al buscar reseñas por valoración");
    }

  

    private List<Resena> buscarVarios(String sql, Object parametro, String mensajeError) {
        List<Resena> lista = new ArrayList<>();

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

        } catch (SQLException e) {
            throw new RuntimeException(mensajeError, e);
        }
        return lista;
    }

    private Resena mapear(ResultSet rs) throws SQLException {
        Date fechaUsuario = rs.getDate("u_fecha");
        Usuario usuario = new Usuario(rs.getInt("idUsuario"), rs.getString("u_nombre"),
                rs.getString("u_email"), fechaUsuario != null ? fechaUsuario.toLocalDate() : null);

        Date fechaLibro = rs.getDate("l_fecha");
        Libro libro = new Libro(rs.getInt("idLibro"), rs.getString("l_isbn"), rs.getString("l_titulo"),
                rs.getString("l_genero"), fechaLibro != null ? fechaLibro.toLocalDate() : null);

        return new Resena(rs.getInt("idResena"), rs.getString("comentarios"),
                rs.getInt("valoracion"), usuario, libro);
    }

    private void validarId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("El id no puede ser nulo");
        }
    }
}
