package org.paloma.repositorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.paloma.modelo.Libro;
import org.paloma.modelo.Resena;
import org.paloma.modelo.Usuario;
import org.paloma.persistencia.ConexionBD;

public class ResenaRepositorio implements IRepositorioExtend<Resena, Integer> {

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
            ResultSet rs = ps.executeQuery();
            return rs.next();

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
        String sql = "SELECT * FROM resenas WHERE idResena = ?";

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
    public List<Resena> findAllToList() {
        List<Resena> lista = new ArrayList<>();
        String sql = "SELECT * FROM resenas";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener las reseñas", e);
        }
        return lista;
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
        boolean esNuevo = entity.getIdResena() == 0;
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
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    entity.setIdResena(rs.getInt(1));
                }
            }
            return entity;

        } catch (SQLException e) {
            throw new RuntimeException(
                    esNuevo ? "Error al insertar" : "Error al actualizar", e);
        }
    }

    private Resena mapear(ResultSet rs) throws SQLException {
        Resena resena = new Resena();
        resena.setIdResena(rs.getInt("idResena"));
        resena.setComentarios(rs.getString("comentarios"));
        resena.setValoracion(rs.getInt("valoracion"));

        // Solo se rellena el id de las FK; para el objeto completo usar UsuarioRepositorio / LibroRepositorio
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(rs.getInt("idUsuario"));
        resena.setUsuario(usuario);

        Libro libro = new Libro();
        libro.setIdLibro(rs.getInt("idLibro"));
        resena.setLibro(libro);
        return resena;
    }

    private void validarId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("El id no puede ser nulo");
        }
    }
}
