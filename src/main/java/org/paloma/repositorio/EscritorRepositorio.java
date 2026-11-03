package org.paloma.repositorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.paloma.modelo.Escritor;
import org.paloma.persistencia.ConexionBD;

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
            ResultSet rs = ps.executeQuery();
            return rs.next();

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
        String sql = "SELECT * FROM escritor WHERE idEscritor = ?";

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
    public List<Escritor> findAllToList() {
        List<Escritor> lista = new ArrayList<>();
        String sql = "SELECT * FROM escritor";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener los escritores", e);
        }
        return lista;
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
        boolean esNuevo = entity.getIdEscritor() == 0;

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
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    entity.setIdEscritor(rs.getInt(1));
                }
            }
            return entity;

        } catch (SQLException e) {
            throw new RuntimeException(
                    esNuevo ? "Error al insertar" : "Error al actualizar", e);
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
