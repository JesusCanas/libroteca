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

INSERT INTO escritor (nombre, biografia) VALUES
('J. R. R. Tolkien',      'Escritor y filólogo británico, autor de El Señor de los Anillos y El Silmarillion.'),
('J. K. Rowling',         'Escritora británica, creadora de la saga Harry Potter.'),
('C. S. Lewis',           'Escritor británico, autor de Las crónicas de Narnia.'),
('Suzanne Collins',       'Escritora estadounidense, autora de la trilogía Los juegos del hambre.'),
('Fiódor Dostoyevski',    'Novelista ruso, uno de los grandes autores de la literatura universal.'),
('George Orwell',         'Escritor y periodista británico, autor de 1984 y Rebelión en la granja.'),
('Brandon Sanderson',     'Escritor estadounidense de fantasía, autor de la saga Nacidos de la bruma.'),
('Jane Austen', 'Novelista británica, autora de Orgullo y prejuicio y Sentido y sensibilidad.');

INSERT INTO libro (ISBN, titulo, genero, fecha_publicacion) VALUES
('978-8445077528', 'El Silmarillion',                          'Fantasía épica',   '1977-09-15'),
('978-8445071410', 'El Señor de los Anillos',                  'Fantasía épica',   '1954-07-29'),
('978-8478884452', 'Harry Potter y la piedra filosofal',       'Fantasía',         '1997-06-26'),
('978-8408165453', 'Las crónicas de Narnia',                   'Fantasía',         '1950-10-16'),
('978-8427202146', 'Los juegos del hambre: En llamas',         'Ciencia ficción',  '2009-09-01'),
('978-8491050353', 'El idiota',                                'Novela clásica',   '1869-01-01'),
('978-8499890944', '1984',                                     'Distopía',         '1949-06-08'),
('978-8499890951', 'Rebelión en la granja',                    'Sátira política',  '1945-08-17'),
('978-8466658911', 'Nacidos de la bruma: El imperio final',    'Fantasía épica',   '2006-07-17'),
('978-8466658928', 'El pozo de la ascensión',                  'Fantasía épica',   '2007-08-21'),
('978-8491051121', 'Orgullo y prejuicio',     'Novela romántica', '1813-01-28'),
('978-8491051138', 'Sentido y sensibilidad',  'Novela romántica', '1811-10-30');

INSERT INTO autoria (idEscritor, idLibro) VALUES
((SELECT idEscritor FROM escritor WHERE nombre = 'J. R. R. Tolkien'),   (SELECT idLibro FROM libro WHERE ISBN = '978-8445077528')),
((SELECT idEscritor FROM escritor WHERE nombre = 'J. R. R. Tolkien'),   (SELECT idLibro FROM libro WHERE ISBN = '978-8445071410')),
((SELECT idEscritor FROM escritor WHERE nombre = 'J. K. Rowling'),      (SELECT idLibro FROM libro WHERE ISBN = '978-8478884452')),
((SELECT idEscritor FROM escritor WHERE nombre = 'C. S. Lewis'),        (SELECT idLibro FROM libro WHERE ISBN = '978-8408165453')),
((SELECT idEscritor FROM escritor WHERE nombre = 'Suzanne Collins'),    (SELECT idLibro FROM libro WHERE ISBN = '978-8427202146')),
((SELECT idEscritor FROM escritor WHERE nombre = 'Fiódor Dostoyevski'), (SELECT idLibro FROM libro WHERE ISBN = '978-8491050353')),
((SELECT idEscritor FROM escritor WHERE nombre = 'George Orwell'),      (SELECT idLibro FROM libro WHERE ISBN = '978-8499890944')),
((SELECT idEscritor FROM escritor WHERE nombre = 'George Orwell'),      (SELECT idLibro FROM libro WHERE ISBN = '978-8499890951')),
((SELECT idEscritor FROM escritor WHERE nombre = 'Brandon Sanderson'),  (SELECT idLibro FROM libro WHERE ISBN = '978-8466658911')),
((SELECT idEscritor FROM escritor WHERE nombre = 'Brandon Sanderson'),  (SELECT idLibro FROM libro WHERE ISBN = '978-8466658928')),
((SELECT idEscritor FROM escritor WHERE nombre = 'Jane Austen'), (SELECT idLibro FROM libro WHERE ISBN = '978-8491051121')),
((SELECT idEscritor FROM escritor WHERE nombre = 'Jane Austen'), (SELECT idLibro FROM libro WHERE ISBN = '978-8491051138'));

INSERT INTO usuarios (nombre, email, fecha_inscripcion) VALUES
('Marta Sánchez', 'marta@example.com', '2025-06-10'),
('Pablo Díaz',    'pablo@example.com', '2025-09-21');

INSERT INTO resenas (idUsuario, idLibro, comentarios, valoracion) VALUES
((SELECT idUsuario FROM usuarios WHERE email = 'laura@example.com'),  (SELECT idLibro FROM libro WHERE ISBN = '978-8445071410'), 'Una aventura épica inolvidable.',5),
((SELECT idUsuario FROM usuarios WHERE email = 'carlos@example.com'), (SELECT idLibro FROM libro WHERE ISBN = '978-8445071410'), 'Muy buena, pero algo lenta al inicio.', 4),
((SELECT idUsuario FROM usuarios WHERE email = 'marta@example.com'),  (SELECT idLibro FROM libro WHERE ISBN = '978-8445077528'), 'Denso, pero fascinante para fans de Tolkien.', 4),
((SELECT idUsuario FROM usuarios WHERE email = 'pablo@example.com'),  (SELECT idLibro FROM libro WHERE ISBN = '978-8478884452'), 'Perfecto para empezar la saga.',5),
((SELECT idUsuario FROM usuarios WHERE email = 'laura@example.com'),  (SELECT idLibro FROM libro WHERE ISBN = '978-8499890944'), 'Inquietante y muy actual.',5),
((SELECT idUsuario FROM usuarios WHERE email = 'marta@example.com'),  (SELECT idLibro FROM libro WHERE ISBN = '978-8499890944'), 'Una lectura imprescindible.',5),
((SELECT idUsuario FROM usuarios WHERE email = 'carlos@example.com'), (SELECT idLibro FROM libro WHERE ISBN = '978-8499890951'), 'Corto pero muy contundente.',4),
((SELECT idUsuario FROM usuarios WHERE email = 'pablo@example.com'),  (SELECT idLibro FROM libro WHERE ISBN = '978-8491050353'), 'Profundo, aunque exigente.',3),
((SELECT idUsuario FROM usuarios WHERE email = 'laura@example.com'),  (SELECT idLibro FROM libro WHERE ISBN = '978-8466658911'), 'Un sistema de magia muy original.',5),
((SELECT idUsuario FROM usuarios WHERE email = 'carlos@example.com'), (SELECT idLibro FROM libro WHERE ISBN = '978-8466658928'), 'Mejora aún más que el primer libro.', 5),
((SELECT idUsuario FROM usuarios WHERE email = 'marta@example.com'),  (SELECT idLibro FROM libro WHERE ISBN = '978-8427202146'), 'Muy adictivo, lo leí del tirón.', 4),
((SELECT idUsuario FROM usuarios WHERE email = 'pablo@example.com'),  (SELECT idLibro FROM libro WHERE ISBN = '978-8408165453'), 'Un clásico para todas las edades.',4),
((SELECT idUsuario FROM usuarios WHERE email = 'laura@example.com'), (SELECT idLibro FROM libro WHERE ISBN = '978-8491051121'), 'Un clásico encantador y muy ingenioso.', 5),
((SELECT idUsuario FROM usuarios WHERE email = 'marta@example.com'), (SELECT idLibro FROM libro WHERE ISBN = '978-8491051138'), 'Preciosa historia sobre dos hermanas.',  4);
