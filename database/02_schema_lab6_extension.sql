-- IF0009 Lab 6 "ExpresoFast" (Parte II) | Carne C4H845 | Extension de seguridad y bitacora: ejecutar despues de 01_schema_lab5.sql.

USE ExpresoFastC4H845_II2026;
GO

/* ---------- Limpieza idempotente (respetando dependencias de FK) ---------- */
IF OBJECT_ID('dbo.BitacoraEnvio', 'U') IS NOT NULL DROP TABLE dbo.BitacoraEnvio;
IF OBJECT_ID('dbo.UsuarioRol', 'U')    IS NOT NULL DROP TABLE dbo.UsuarioRol;
IF OBJECT_ID('dbo.Rol', 'U')           IS NOT NULL DROP TABLE dbo.Rol;
IF OBJECT_ID('dbo.Usuario', 'U')       IS NOT NULL DROP TABLE dbo.Usuario;
GO

/* ================================ 5. Usuario ============================== */
CREATE TABLE dbo.Usuario (
    usuario_id      INT           IDENTITY(1,1) NOT NULL,
    username        VARCHAR(50)   NOT NULL,
    password_hash   VARCHAR(255)  NOT NULL,   -- hash BCrypt, nunca texto plano
    nombre_completo VARCHAR(100)  NOT NULL,
    email           VARCHAR(100)  NOT NULL,
    activo          BIT           NOT NULL,   -- 1: activo, 0: inactivo
    CONSTRAINT PK_Usuario          PRIMARY KEY (usuario_id),
    CONSTRAINT UQ_Usuario_username UNIQUE (username),
    CONSTRAINT UQ_Usuario_email    UNIQUE (email)
);
GO

/* ================================== 6. Rol ================================ */
CREATE TABLE dbo.Rol (
    rol_id     INT         IDENTITY(1,1) NOT NULL,
    nombre_rol VARCHAR(30) NOT NULL,
    CONSTRAINT PK_Rol           PRIMARY KEY (rol_id),
    CONSTRAINT UQ_Rol_nombre    UNIQUE (nombre_rol),
    CONSTRAINT CK_Rol_nombre    CHECK (nombre_rol IN ('ROLE_ADMIN', 'ROLE_OPERADOR', 'ROLE_CONDUCTOR'))
);
GO

/* ===================== 7. UsuarioRol (tabla intermedia) =================== */
CREATE TABLE dbo.UsuarioRol (
    usuario_id INT NOT NULL,
    rol_id     INT NOT NULL,
    CONSTRAINT PK_UsuarioRol PRIMARY KEY (usuario_id, rol_id),   -- clave primaria compuesta
    CONSTRAINT FK_UsuarioRol_Usuario FOREIGN KEY (usuario_id)
        REFERENCES dbo.Usuario (usuario_id) ON DELETE CASCADE,
    CONSTRAINT FK_UsuarioRol_Rol FOREIGN KEY (rol_id)
        REFERENCES dbo.Rol (rol_id) ON DELETE CASCADE
);
GO

/* ============================ 8. BitacoraEnvio ============================ */
CREATE TABLE dbo.BitacoraEnvio (
    bitacora_id     INT          IDENTITY(1,1) NOT NULL,
    envio_id        INT          NOT NULL,
    estado_anterior VARCHAR(20)  NOT NULL,
    estado_nuevo    VARCHAR(20)  NOT NULL,
    fecha_cambio    DATETIME     NOT NULL,
    usuario_id      INT          NOT NULL,
    observaciones   VARCHAR(250) NULL,
    CONSTRAINT PK_BitacoraEnvio PRIMARY KEY (bitacora_id),
    CONSTRAINT FK_Bitacora_Envio   FOREIGN KEY (envio_id)   REFERENCES dbo.Envio (envio_id),
    CONSTRAINT FK_Bitacora_Usuario FOREIGN KEY (usuario_id) REFERENCES dbo.Usuario (usuario_id),
    CONSTRAINT CK_Bitacora_estado_anterior
        CHECK (estado_anterior IN ('PENDIENTE', 'EN_TRANSITO', 'ENTREGADO', 'CANCELADO')),
    CONSTRAINT CK_Bitacora_estado_nuevo
        CHECK (estado_nuevo    IN ('PENDIENTE', 'EN_TRANSITO', 'ENTREGADO', 'CANCELADO'))
);
GO

/* Indices de apoyo para el modal de bitacora (filtra por envio y ordena por fecha) */
CREATE INDEX IX_Bitacora_envio_id     ON dbo.BitacoraEnvio (envio_id);
CREATE INDEX IX_Bitacora_fecha_cambio ON dbo.BitacoraEnvio (fecha_cambio DESC);
CREATE INDEX IX_Bitacora_usuario_id   ON dbo.BitacoraEnvio (usuario_id);
GO

PRINT 'Extension del Lab 6 creada: Usuario, Rol, UsuarioRol y BitacoraEnvio.';
GO
