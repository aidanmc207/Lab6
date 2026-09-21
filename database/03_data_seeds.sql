-- IF0009 Lab 6 "ExpresoFast" (Parte II) | Carne C4H845 | Semillas de roles y usuarios con hash BCrypt: ejecutar despues de 02_schema_lab6_extension.sql.

USE ExpresoFastC4H845_II2026;
GO

/* ---------- Limpieza idempotente de las semillas ---------- */
DELETE FROM dbo.BitacoraEnvio;
DELETE FROM dbo.UsuarioRol;
DELETE FROM dbo.Usuario;
DELETE FROM dbo.Rol;
GO

-- El RESEED solo aplica si la tabla ya recibio inserciones.
IF EXISTS (SELECT 1 FROM sys.identity_columns
           WHERE object_id = OBJECT_ID('dbo.Usuario') AND last_value IS NOT NULL)
    DBCC CHECKIDENT ('dbo.Usuario', RESEED, 0) WITH NO_INFOMSGS;

IF EXISTS (SELECT 1 FROM sys.identity_columns
           WHERE object_id = OBJECT_ID('dbo.Rol') AND last_value IS NOT NULL)
    DBCC CHECKIDENT ('dbo.Rol', RESEED, 0) WITH NO_INFOMSGS;

IF EXISTS (SELECT 1 FROM sys.identity_columns
           WHERE object_id = OBJECT_ID('dbo.BitacoraEnvio') AND last_value IS NOT NULL)
    DBCC CHECKIDENT ('dbo.BitacoraEnvio', RESEED, 0) WITH NO_INFOMSGS;
GO

/* ============================= 1. Roles =================================== */
INSERT INTO dbo.Rol (nombre_rol) VALUES
    ('ROLE_ADMIN'),
    ('ROLE_OPERADOR'),
    ('ROLE_CONDUCTOR');
GO

/* ===================== 2. Usuarios de prueba (BCrypt) ===================== */
-- Contrasena en claro: 'admin123'
INSERT INTO dbo.Usuario (username, password_hash, nombre_completo, email, activo)
VALUES ('admin',
        '$2a$10$jQZMHbqhkBTbK/X7q0MeOelzT7ILg4.sEkmUCnab3okfr0ykrt1VG',
        'Carlos Alvarado Mora', 'admin@expresofast.cr', 1);

-- Contrasena en claro: 'oper123'
INSERT INTO dbo.Usuario (username, password_hash, nombre_completo, email, activo)
VALUES ('operador1',
        '$2a$10$JP1JDAGBQgyA1N/FEZPwlevrvF5HQ6utj33nei0r9F9UFsVU334lG',
        'Adriana Vargas Solis', 'operador1@expresofast.cr', 1);

-- Contrasena en claro: 'cond123'. Queda ligado a su ficha de Conductor para que
-- la consola solo le muestre los envios asignados a su vehiculo.
INSERT INTO dbo.Usuario (username, password_hash, nombre_completo, email, activo, conductor_id)
SELECT 'conductor1',
       '$2a$10$y4G/amGpyKb3k2E7KrqSIeEvWQszxZ0ABZX2cTkU7Mg8V2Vy1epyG',
       'Luis Mora Jimenez', 'conductor1@expresofast.cr', 1, c.conductor_id
FROM dbo.Conductor c
WHERE c.licencia = 'B1-778899';
GO

/* ================== 3. Asignacion de roles (RBAC) ========================= */
-- Se resuelven los id por nombre para no depender del orden de los IDENTITY.
INSERT INTO dbo.UsuarioRol (usuario_id, rol_id)
SELECT u.usuario_id, r.rol_id
FROM dbo.Usuario u
    JOIN dbo.Rol r ON r.nombre_rol = 'ROLE_ADMIN'
WHERE u.username = 'admin';

INSERT INTO dbo.UsuarioRol (usuario_id, rol_id)
SELECT u.usuario_id, r.rol_id
FROM dbo.Usuario u
    JOIN dbo.Rol r ON r.nombre_rol = 'ROLE_OPERADOR'
WHERE u.username = 'operador1';

INSERT INTO dbo.UsuarioRol (usuario_id, rol_id)
SELECT u.usuario_id, r.rol_id
FROM dbo.Usuario u
    JOIN dbo.Rol r ON r.nombre_rol = 'ROLE_CONDUCTOR'
WHERE u.username = 'conductor1';
GO

/* ============ 4. Bitacora inicial de los envios del Lab 5 ================= */
-- Deja el historial coherente: cada envio de prueba nace con su registro de alta.
INSERT INTO dbo.BitacoraEnvio
    (envio_id, estado_anterior, estado_nuevo, fecha_cambio, usuario_id, observaciones)
SELECT e.envio_id, 'PENDIENTE', e.estado_envio, GETDATE(), u.usuario_id,
       'Carga inicial de datos del Laboratorio 5'
FROM dbo.Envio e
    CROSS JOIN (SELECT usuario_id FROM dbo.Usuario WHERE username = 'admin') u;
GO

/* ====================== 5. Verificacion rapida ============================ */
SELECT u.username, u.nombre_completo, u.email, u.activo, r.nombre_rol
FROM dbo.Usuario u
    LEFT JOIN dbo.UsuarioRol ur ON ur.usuario_id = u.usuario_id
    LEFT JOIN dbo.Rol r         ON r.rol_id      = ur.rol_id
ORDER BY u.usuario_id;

SELECT b.bitacora_id, e.codigo_rastreo, b.estado_anterior, b.estado_nuevo,
       b.fecha_cambio, u.username, b.observaciones
FROM dbo.BitacoraEnvio b
    JOIN dbo.Envio e   ON e.envio_id   = b.envio_id
    JOIN dbo.Usuario u ON u.usuario_id = b.usuario_id
ORDER BY b.bitacora_id;
GO
