-- =====================================================
-- Base de datos: biblioteca (libros, escritores, usuarios y reseñas)
-- Compatible con MySQL / MariaDB
-- =====================================================

DROP DATABASE IF EXISTS libroteca;
CREATE DATABASE libroteca CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE biblioteca;

-- -----------------------------------------------------
-- Tabla USUARIOS
-- -----------------------------------------------------
CREATE TABLE usuarios (
    idUsuario         INT AUTO_INCREMENT PRIMARY KEY,
    nombre            VARCHAR(100) NOT NULL,
    email             VARCHAR(150) NOT NULL UNIQUE,
    fecha_inscripcion DATE NOT NULL DEFAULT (CURRENT_DATE)
);

-- -----------------------------------------------------
-- Tabla ESCRITOR
-- -----------------------------------------------------
CREATE TABLE escritor (
    idEscritor INT AUTO_INCREMENT PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL,
    biografia  TEXT
);

-- -----------------------------------------------------
-- Tabla LIBRO
-- -----------------------------------------------------
CREATE TABLE libro (
    idLibro           INT AUTO_INCREMENT PRIMARY KEY,
    ISBN              VARCHAR(20) NOT NULL UNIQUE,
    titulo            VARCHAR(200) NOT NULL,
    genero            VARCHAR(80),
    fecha_publicacion DATE
);

-- -----------------------------------------------------
-- Tabla AUTORIA (relación N:M entre ESCRITOR y LIBRO)
-- Un escritor puede escribir muchos libros y un libro
-- puede tener varios escritores.
-- -----------------------------------------------------
CREATE TABLE autoria (
    idEscritor INT NOT NULL,
    idLibro    INT NOT NULL,
    PRIMARY KEY (idEscritor, idLibro),
    CONSTRAINT fk_autoria_escritor
        FOREIGN KEY (idEscritor) REFERENCES escritor(idEscritor)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_autoria_libro
        FOREIGN KEY (idLibro) REFERENCES libro(idLibro)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- -----------------------------------------------------
-- Tabla RESEÑAS
-- Un usuario escribe muchas reseñas (1:M)
-- Un libro tiene muchas reseñas (1:M)
-- -----------------------------------------------------
CREATE TABLE resenas (
    idResena   INT AUTO_INCREMENT PRIMARY KEY,
    idUsuario  INT NOT NULL,
    idLibro    INT NOT NULL,
    comentarios TEXT,
    valoracion TINYINT NOT NULL,
    CONSTRAINT chk_valoracion CHECK (valoracion BETWEEN 1 AND 5),
    CONSTRAINT fk_resenas_usuario
        FOREIGN KEY (idUsuario) REFERENCES usuarios(idUsuario)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_resenas_libro
        FOREIGN KEY (idLibro) REFERENCES libro(idLibro)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- -----------------------------------------------------
-- Índices para acelerar consultas frecuentes
-- -----------------------------------------------------
CREATE INDEX idx_resenas_usuario ON resenas(idUsuario);
CREATE INDEX idx_resenas_libro   ON resenas(idLibro);
CREATE INDEX idx_libro_titulo    ON libro(titulo);

-- =====================================================
-- Datos de ejemplo
-- =====================================================
INSERT INTO usuarios (nombre, email, fecha_inscripcion) VALUES
('Laura Gómez',  'laura@example.com',  '2025-01-15'),
('Carlos Ruiz',  'carlos@example.com', '2025-03-02');

INSERT INTO escritor (nombre, biografia) VALUES
('Gabriel García Márquez', 'Escritor colombiano, Premio Nobel de Literatura 1982.'),
('Isabel Allende',         'Escritora chilena, autora de La casa de los espíritus.');

INSERT INTO libro (ISBN, titulo, genero, fecha_publicacion) VALUES
('978-0307474728', 'Cien años de soledad',      'Realismo mágico', '1967-05-30'),
('978-8401337208', 'La casa de los espíritus',  'Novela',          '1982-01-01');

INSERT INTO autoria (idEscritor, idLibro) VALUES (1, 1), (2, 2);

INSERT INTO resenas (idUsuario, idLibro, comentarios, valoracion) VALUES
(1, 1, 'Una obra maestra de la literatura.', 5),
(2, 1, 'Muy buena, aunque algo densa.',       4),
(1, 2, 'Me encantó la historia familiar.',    5);

-- =====================================================
-- Consulta de ejemplo: libros con autor y valoración media
-- =====================================================
-- SELECT l.titulo, e.nombre AS autor, ROUND(AVG(r.valoracion), 2) AS media
-- FROM libro l
-- JOIN autoria a  ON a.idLibro = l.idLibro
-- JOIN escritor e ON e.idEscritor = a.idEscritor
-- LEFT JOIN resenas r ON r.idLibro = l.idLibro
-- GROUP BY l.idLibro, e.idEscritor;
