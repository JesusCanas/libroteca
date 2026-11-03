package org.paloma.repositorio;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.paloma.modelo.Escritor;
import org.paloma.modelo.Libro;
import org.paloma.modelo.Resena;
import org.paloma.modelo.Usuario;
import org.paloma.persistencia.ConexionBD;

/**
 * Repositorio de la raíz agregada {@link Libro}.
 * El agregado incluye sus reseñas (1:M) y la relación de autoría con los escritores (N:M).
 * Este repositorio es el propietario de la tabla AUTORIA: al guardar un libro se sincroniza.
 */
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
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

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
        return buscarUno("SELECT * FROM libro WHERE idLibro = ?", id, "Error al buscar por ID: " + id);
    }

    @Override
    public List<Libro> findAllToList() {
        return buscarVarios("SELECT * FROM libro", null, "Error al obtener los libros");
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
        // Insertar si es nuevo (id 0); si trae id, debe existir previamente (buscar -> modificar -> guardar)
        boolean esNuevo = entity.getIdLibro() == 0;
        if (!esNuevo && !existsById(entity.getIdLibro())) {
            throw new IllegalArgumentException("No existe ningún libro con id " + entity.getIdLibro());
        }

        String sql = esNuevo
                ? "INSERT INTO libro (ISBN, titulo, genero, fecha_publicacion) VALUES (?, ?, ?, ?)"
                : "UPDATE libro SET ISBN = ?, titulo = ?, genero = ?, fecha_publicacion = ? WHERE idLibro = ?";

        try (Connection conn = ConexionBD.getConexion()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, entity.getIsbn());
                ps.setString(2, entity.getTitulo());
                ps.setString(3, entity.getGenero());
                ps.setObject(4, entity.getFechaPublicacion());

                if (!esNuevo) {
                    ps.setInt(5, entity.getIdLibro());
                }

                ps.executeUpdate();

                if (esNuevo) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            entity.setIdLibro(rs.getInt(1));
                        }
                    }
                }

                guardarAutoria(conn, entity);
                conn.commit();
                return entity;

            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    esNuevo ? "Error al insertar" : "Error al actualizar", e);
        }
    }

    // ------------------------------------------------------------------
    // Métodos propios del repositorio de libros
    // ------------------------------------------------------------------

    /** Devuelve el libro con ese ISBN (único) o Optional#empty() si no existe. */
    public Optional<Libro> findByIsbn(String isbn) {
        if (isbn == null) {
            throw new IllegalArgumentException("El ISBN no puede ser nulo");
        }
        return buscarUno("SELECT * FROM libro WHERE ISBN = ?", isbn, "Error al buscar por ISBN: " + isbn);
    }

    /** Devuelve los libros de un género. */
    public List<Libro> findByGenero(String genero) {
        if (genero == null) {
            throw new IllegalArgumentException("El género no puede ser nulo");
        }
        return buscarVarios("SELECT * FROM libro WHERE genero = ?", genero, "Error al buscar por género: " + genero);
    }

    /** Devuelve los libros cuyo título contiene el texto indicado (sin distinguir mayúsculas). */
    public List<Libro> findByTituloContaining(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("El texto no puede ser nulo");
        }
        return buscarVarios("SELECT * FROM libro WHERE titulo LIKE ?", "%" + texto + "%",
                "Error al buscar por título: " + texto);
    }

    /** Devuelve los libros escritos por un escritor. */
    public List<Libro> findByEscritor(Integer idEscritor) {
        validarId(idEscritor);
        return buscarVarios(
                "SELECT l.* FROM libro l JOIN autoria a ON a.idLibro = l.idLibro WHERE a.idEscritor = ?",
                idEscritor, "Error al buscar libros del escritor: " + idEscritor);
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private Optional<Libro> buscarUno(String sql, Object parametro, String mensajeError) {
        List<Libro> libros = buscarVarios(sql, parametro, mensajeError);
        return libros.isEmpty() ? Optional.empty() : Optional.of(libros.get(0));
    }

    /** Ejecuta la consulta (con 0 o 1 parámetro) y devuelve los libros con su agregado cargado. */
    private List<Libro> buscarVarios(String sql, Object parametro, String mensajeError) {
        List<Libro> lista = new ArrayList<>();

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
            for (Libro libro : lista) {
                cargarEscritores(conn, libro);
                cargarResenas(conn, libro);
            }

        } catch (SQLException e) {
            throw new RuntimeException(mensajeError, e);
        }
        return lista;
    }

    private void cargarEscritores(Connection conn, Libro libro) throws SQLException {
        String sql = "SELECT e.idEscritor, e.nombre, e.biografia FROM escritor e "
                + "JOIN autoria a ON a.idEscritor = e.idEscritor WHERE a.idLibro = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, libro.getIdLibro());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    libro.getEscritores().add(
                            new Escritor(rs.getInt("idEscritor"), rs.getString("nombre"), rs.getString("biografia")));
                }
            }
        }
    }

    private void cargarResenas(Connection conn, Libro libro) throws SQLException {
        String sql = "SELECT r.idResena, r.comentarios, r.valoracion, "
                + "u.idUsuario, u.nombre, u.email, u.fecha_inscripcion "
                + "FROM resenas r JOIN usuarios u ON u.idUsuario = r.idUsuario WHERE r.idLibro = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, libro.getIdLibro());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date fecha = rs.getDate("fecha_inscripcion");
                    Usuario usuario = new Usuario(rs.getInt("idUsuario"), rs.getString("nombre"),
                            rs.getString("email"), fecha != null ? fecha.toLocalDate() : null);
                    Resena resena = new Resena(rs.getInt("idResena"), rs.getString("comentarios"),
                            rs.getInt("valoracion"), usuario, null);
                    libro.agregarResena(resena);
                }
            }
        }
    }

    /** Sincroniza la tabla AUTORIA con la lista de escritores del libro. */
    private void guardarAutoria(Connection conn, Libro libro) throws SQLException {
        try (PreparedStatement borrar = conn.prepareStatement("DELETE FROM autoria WHERE idLibro = ?")) {
            borrar.setInt(1, libro.getIdLibro());
            borrar.executeUpdate();
        }
        try (PreparedStatement insertar = conn.prepareStatement(
                "INSERT INTO autoria (idEscritor, idLibro) VALUES (?, ?)")) {
            for (Escritor escritor : libro.getEscritores()) {
                if (escritor.getIdEscritor() == 0) {
                    throw new IllegalArgumentException(
                            "El escritor '" + escritor.getNombre() + "' debe guardarse antes de asignarlo a un libro");
                }
                insertar.setInt(1, escritor.getIdEscritor());
                insertar.setInt(2, libro.getIdLibro());
                insertar.executeUpdate();
            }
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
