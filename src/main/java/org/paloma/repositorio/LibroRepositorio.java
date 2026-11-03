package org.paloma.repositorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.paloma.modelo.Libro;
import org.paloma.persistencia.ConexionBD;

public class LibroRepositorio implements IRepositorioExtend<Libro, Integer> {

    @Override
    public long count() {
         String sql = "select count(*) from libro";
        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
             return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
           throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteById(Integer id) {
         String sql = "DELETE FROM libro WHERE id = ?";
            try (Connection conn = ConexionBD.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
             ps.setInt(1,id);
             ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace(); 
            throw new RuntimeException("Error al eliminar el registro con ID: " + id, e);
    }
    }

    @Override
    public void deleteAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteAll'");
    }

    @Override
    public boolean existsById(Integer id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'existsById'");
    }

    @Override
    public Libro findById(Integer id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findById'");
    }

    @Override
    public Iterable<Libro> findAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }

    @Override
    public <S extends Libro> S save(S entity) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'save'");
    }

    @Override
    public List<Libro> findAllToList() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAllToList'");
    }

    @Override
    public Optional<Libro> findByIdOptional(Integer id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findByIdOptional'");
    }
    
}
