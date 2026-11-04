-- =====================================================================
-- Omni Solutions - datos de prueba
--   mysql -u omni_app -p omni_solutions < database/02-datos-prueba.sql
--
-- Todos los usuarios de prueba tienen la contraseña: Omni2026
-- (guardada como hash BCrypt). Al ejecutarlo nuevamente se eliminan y
-- recrean SOLO estos usuarios de prueba (y en cascada sus datos).
-- =====================================================================
SET NAMES utf8mb4;
USE omni_solutions;

DELETE FROM usuarios WHERE usuario IN
    ('panaderia.sol', 'constructora.norte', 'lucia.fernandez', 'diego.morales', 'camila.ortiz');

-- ---------------------------------------------------------------- usuarios
INSERT INTO usuarios (nombre, usuario, contrasena, tipo_usuario, telefono, fecha_registro) VALUES
    ('Panadería El Sol',   'panaderia.sol',      '$2a$10$ENs0Gwvyb.5MgVw67XSdqOzMlGo2mzcxQfh45TJ5dK/7IqKJb8x0C', 'EMPLEADOR', '351 400-1000', NOW() - INTERVAL 20 DAY),
    ('Constructora Norte', 'constructora.norte', '$2a$10$ENs0Gwvyb.5MgVw67XSdqOzMlGo2mzcxQfh45TJ5dK/7IqKJb8x0C', 'EMPLEADOR', NULL,           NOW() - INTERVAL 18 DAY),
    ('Lucía Fernández',    'lucia.fernandez',    '$2a$10$ENs0Gwvyb.5MgVw67XSdqOzMlGo2mzcxQfh45TJ5dK/7IqKJb8x0C', 'EMPLEADO',  '351 600-2000', NOW() - INTERVAL 15 DAY),
    ('Diego Morales',      'diego.morales',      '$2a$10$ENs0Gwvyb.5MgVw67XSdqOzMlGo2mzcxQfh45TJ5dK/7IqKJb8x0C', 'EMPLEADO',  '351 600-3000', NOW() - INTERVAL 12 DAY),
    ('Camila Ortiz',       'camila.ortiz',       '$2a$10$ENs0Gwvyb.5MgVw67XSdqOzMlGo2mzcxQfh45TJ5dK/7IqKJb8x0C', 'EMPLEADO',  NULL,           NOW() - INTERVAL 10 DAY);

-- ---------------------------------------------------------------- publicaciones
INSERT INTO publicaciones (id_usuario, tipo, titulo, descripcion, ubicacion, fecha_publicacion, estado)
SELECT u.id_usuario, d.tipo, d.titulo, d.descripcion, d.ubicacion, NOW() - INTERVAL d.horas HOUR, d.estado
FROM usuarios u
JOIN (
    SELECT 'panaderia.sol' AS usuario, 'TRABAJO' AS tipo, 'Ayudante de panadería - turno mañana' AS titulo,
           'Buscamos ayudante para elaboración y atención al público. Horario de 6 a 12 h, de lunes a sábado. No se requiere experiencia previa.' AS descripcion,
           'Córdoba Capital' AS ubicacion, 50 AS horas, 'ACTIVA' AS estado
    UNION ALL SELECT 'panaderia.sol', 'TRABAJO', 'Repartidor/a con moto propia',
           'Reparto de pedidos en zona centro y Nueva Córdoba. Se abona por jornada más combustible.',
           'Córdoba Capital', 30, 'ACTIVA'
    UNION ALL SELECT 'panaderia.sol', 'TRABAJO', 'Cajero/a para temporada de fiestas',
           'Puesto temporal para diciembre y enero. Manejo de caja y atención al cliente.',
           NULL, 200, 'CERRADA'
    UNION ALL SELECT 'constructora.norte', 'TRABAJO', 'Albañil con experiencia',
           'Obra de vivienda unifamiliar. Se requiere experiencia comprobable en mampostería y revoques.',
           'Villa María', 20, 'ACTIVA'
    UNION ALL SELECT 'lucia.fernandez', 'SERVICIO', 'Clases particulares de matemática',
           'Profesora de matemática. Clases para nivel secundario y ingreso universitario, presenciales u online.',
           'Córdoba Capital', 40, 'ACTIVA'
    UNION ALL SELECT 'diego.morales', 'SERVICIO', 'Plomería y gasistería',
           'Reparaciones de pérdidas, instalación de grifería y termotanques. Presupuestos sin cargo.',
           'Alta Gracia', 10, 'ACTIVA'
) d ON d.usuario = u.usuario;

-- ---------------------------------------------------------------- postulaciones
INSERT INTO postulaciones (id_usuario, id_publicacion, fecha_postulacion, estado)
SELECT e.id_usuario, p.id_publicacion, NOW() - INTERVAL d.horas HOUR, d.estado
FROM (
    SELECT 'lucia.fernandez' AS empleado, 'Ayudante de panadería - turno mañana' AS titulo, 45 AS horas, 'ACEPTADA' AS estado
    UNION ALL SELECT 'camila.ortiz',    'Ayudante de panadería - turno mañana', 40, 'ACEPTADA'
    UNION ALL SELECT 'diego.morales',   'Ayudante de panadería - turno mañana', 35, 'PENDIENTE'
    UNION ALL SELECT 'camila.ortiz',    'Repartidor/a con moto propia',         25, 'PENDIENTE'
    UNION ALL SELECT 'diego.morales',   'Albañil con experiencia',              15, 'RECHAZADA'
    UNION ALL SELECT 'lucia.fernandez', 'Cajero/a para temporada de fiestas',  190, 'ACEPTADA'
) d
JOIN usuarios e ON e.usuario = d.empleado
JOIN publicaciones p ON p.titulo = d.titulo AND p.tipo = 'TRABAJO'
JOIN usuarios o ON o.id_usuario = p.id_usuario AND o.usuario IN ('panaderia.sol', 'constructora.norte');
