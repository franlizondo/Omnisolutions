-- =====================================================================
-- Omni Solutions - creación de la base de datos (MySQL 8)
-- Ejecutar con un usuario administrador, por ejemplo:
--   mysql -u root -p < database/01-crear-base-datos.sql
--
-- IMPORTANTE: reemplazar 'cambiar_esta_contrasena' por una contraseña
-- propia y usar los mismos valores en el archivo .env (DB_USER / DB_PASSWORD).
-- El script es idempotente: puede ejecutarse más de una vez sin borrar datos.
-- =====================================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS omni_solutions
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'omni_app'@'localhost' IDENTIFIED BY 'cambiar_esta_contrasena';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
    ON omni_solutions.* TO 'omni_app'@'localhost';
FLUSH PRIVILEGES;

USE omni_solutions;


CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario      BIGINT       NOT NULL AUTO_INCREMENT,
    nombre          VARCHAR(100) NOT NULL,
    usuario         VARCHAR(50)  NOT NULL,
    contrasena      VARCHAR(100) NOT NULL,
    tipo_usuario    VARCHAR(20)  NOT NULL,
    telefono        VARCHAR(20)  NULL,
    fecha_registro  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuarios PRIMARY KEY (id_usuario),
    CONSTRAINT uk_usuarios_usuario UNIQUE (usuario),
    CONSTRAINT ck_usuarios_tipo CHECK (tipo_usuario IN ('EMPLEADOR', 'EMPLEADO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS publicaciones (
    id_publicacion     BIGINT        NOT NULL AUTO_INCREMENT,
    id_usuario         BIGINT        NOT NULL,
    tipo               VARCHAR(20)   NOT NULL,
    titulo             VARCHAR(120)  NOT NULL,
    descripcion        VARCHAR(2000) NOT NULL,
    ubicacion          VARCHAR(120)  NULL,
    fecha_publicacion  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado             VARCHAR(20)   NOT NULL DEFAULT 'ACTIVA',
    CONSTRAINT pk_publicaciones PRIMARY KEY (id_publicacion),
    CONSTRAINT fk_publicaciones_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuarios (id_usuario) ON DELETE CASCADE,
    CONSTRAINT ck_publicaciones_tipo CHECK (tipo IN ('TRABAJO', 'SERVICIO')),
    CONSTRAINT ck_publicaciones_estado CHECK (estado IN ('ACTIVA', 'CERRADA')),
    INDEX idx_publicaciones_usuario (id_usuario),
    INDEX idx_publicaciones_feed (estado, tipo, fecha_publicacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS postulaciones (
    id_postulacion     BIGINT      NOT NULL AUTO_INCREMENT,
    id_usuario         BIGINT      NOT NULL,
    id_publicacion     BIGINT      NOT NULL,
    fecha_postulacion  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado             VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    CONSTRAINT pk_postulaciones PRIMARY KEY (id_postulacion),
    CONSTRAINT fk_postulaciones_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuarios (id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_postulaciones_publicacion FOREIGN KEY (id_publicacion)
        REFERENCES publicaciones (id_publicacion) ON DELETE CASCADE,
    CONSTRAINT uk_postulaciones_usuario_publicacion UNIQUE (id_usuario, id_publicacion),
    CONSTRAINT ck_postulaciones_estado CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA')),
    INDEX idx_postulaciones_publicacion (id_publicacion, estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
