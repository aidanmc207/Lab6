-- IF0009 Lab 5 "ExpresoFast" (Parte I) | Carne C4H845 | DDL base de SQL Server: ejecutar completo en SSMS.

IF DB_ID('ExpresoFastC4H845_II2026') IS NULL
BEGIN
    CREATE DATABASE ExpresoFastC4H845_II2026;
END
GO

USE ExpresoFastC4H845_II2026;
GO

/* ---------- Limpieza idempotente (respetando dependencias de FK) -------- */
IF OBJECT_ID('dbo.BitacoraEnvio', 'U')     IS NOT NULL DROP TABLE dbo.BitacoraEnvio;  -- depende de Envio (creada en el Lab 6)
IF OBJECT_ID('dbo.Envio', 'U')             IS NOT NULL DROP TABLE dbo.Envio;
IF OBJECT_ID('dbo.Vehiculo', 'U')          IS NOT NULL DROP TABLE dbo.Vehiculo;
IF OBJECT_ID('dbo.Conductor', 'U')         IS NOT NULL DROP TABLE dbo.Conductor;
IF OBJECT_ID('dbo.EmpresaLogistica', 'U')  IS NOT NULL DROP TABLE dbo.EmpresaLogistica;
GO

/* ============================ 1. EmpresaLogistica ====================== */
CREATE TABLE dbo.EmpresaLogistica (
    empresa_id      INT             IDENTITY(1,1) NOT NULL,
    nombre          VARCHAR(100)    NOT NULL,
    cedula_juridica VARCHAR(20)     NOT NULL,
    telefono        VARCHAR(20)     NOT NULL,
    fecha_registro  DATETIME        NOT NULL,
    CONSTRAINT PK_EmpresaLogistica        PRIMARY KEY (empresa_id),
    CONSTRAINT UQ_Empresa_nombre          UNIQUE (nombre),
    CONSTRAINT UQ_Empresa_cedula_juridica UNIQUE (cedula_juridica)
);
GO

/* ================================ 2. Vehiculo ========================== */
CREATE TABLE dbo.Vehiculo (
    vehiculo_id  INT            IDENTITY(1,1) NOT NULL,
    placa        VARCHAR(15)    NOT NULL,
    capacidad_kg DECIMAL(10,2)  NOT NULL,
    estado       VARCHAR(20)    NOT NULL,
    empresa_id   INT            NULL,
    CONSTRAINT PK_Vehiculo       PRIMARY KEY (vehiculo_id),
    CONSTRAINT UQ_Vehiculo_placa UNIQUE (placa),
    CONSTRAINT CK_Vehiculo_estado CHECK (estado IN ('DISPONIBLE', 'EN_RUTA', 'MANTENIMIENTO')),
    CONSTRAINT FK_Vehiculo_Empresa FOREIGN KEY (empresa_id)
        REFERENCES dbo.EmpresaLogistica (empresa_id)
);
GO

/* =============================== 3. Conductor ========================== */
CREATE TABLE dbo.Conductor (
    conductor_id INT          IDENTITY(1,1) NOT NULL,
    nombre       VARCHAR(50)  NOT NULL,
    apellidos    VARCHAR(50)  NOT NULL,
    licencia     VARCHAR(20)  NOT NULL,
    telefono     VARCHAR(20)  NOT NULL,
    CONSTRAINT PK_Conductor          PRIMARY KEY (conductor_id),
    CONSTRAINT UQ_Conductor_licencia UNIQUE (licencia)
);
GO

/* ================================= 4. Envio ============================ */
CREATE TABLE dbo.Envio (
    envio_id           INT            IDENTITY(1,1) NOT NULL,
    codigo_rastreo     VARCHAR(30)    NOT NULL,
    direccion_destino  VARCHAR(200)   NOT NULL,
    peso_kg            DECIMAL(10,2)  NOT NULL,
    costo              DECIMAL(10,2)  NOT NULL,
    estado_envio       VARCHAR(20)    NOT NULL,
    vehiculo_id        INT            NULL,
    conductor_id       INT            NULL,
    fecha_creacion     DATETIME       NULL,  -- @CreatedDate (auditoria JPA)
    fecha_modificacion DATETIME       NULL,  -- @LastModifiedDate (auditoria JPA)
    CONSTRAINT PK_Envio                PRIMARY KEY (envio_id),
    CONSTRAINT UQ_Envio_codigo_rastreo UNIQUE (codigo_rastreo),
    CONSTRAINT CK_Envio_estado CHECK (estado_envio IN ('PENDIENTE', 'EN_TRANSITO', 'ENTREGADO', 'CANCELADO')),
    CONSTRAINT FK_Envio_Vehiculo  FOREIGN KEY (vehiculo_id)  REFERENCES dbo.Vehiculo (vehiculo_id),
    CONSTRAINT FK_Envio_Conductor FOREIGN KEY (conductor_id) REFERENCES dbo.Conductor (conductor_id)
);
GO

/* Indices de apoyo para el filtrado por estado y el JOIN FETCH */
CREATE INDEX IX_Envio_estado_envio ON dbo.Envio (estado_envio);
CREATE INDEX IX_Envio_vehiculo_id  ON dbo.Envio (vehiculo_id);
CREATE INDEX IX_Envio_conductor_id ON dbo.Envio (conductor_id);
GO

/* ========================== Datos de prueba ============================ */
INSERT INTO dbo.EmpresaLogistica (nombre, cedula_juridica, telefono, fecha_registro) VALUES
    ('ExpresoFast Central',  '3-101-556677', '2574-1100', GETDATE()),
    ('Cargas del Atlantico', '3-101-889900', '2556-2200', GETDATE());
GO

INSERT INTO dbo.Vehiculo (placa, capacidad_kg, estado, empresa_id) VALUES
    ('CRC-1001', 1500.00, 'DISPONIBLE',     1),
    ('CRC-1002',  800.50, 'EN_RUTA',        1),
    ('CRC-2001', 3200.00, 'DISPONIBLE',     2),
    ('CRC-2002',  450.00, 'MANTENIMIENTO',  2);
GO

INSERT INTO dbo.Conductor (nombre, apellidos, licencia, telefono) VALUES
    ('Luis',    'Mora Jimenez',    'B1-778899', '8811-4455'),
    ('Adriana', 'Vargas Solis',    'B2-334455', '8722-9911'),
    ('Kevin',   'Rodriguez Alpizar','A3-112233', '8633-7788');
GO

INSERT INTO dbo.Envio
    (codigo_rastreo, direccion_destino, peso_kg, costo, estado_envio, vehiculo_id, conductor_id, fecha_creacion, fecha_modificacion)
VALUES
    ('EXP-9001', 'Paraiso, Cartago, 200m sur de la iglesia', 12.50, 3500.00, 'PENDIENTE',   1, 1, GETDATE(), GETDATE()),
    ('EXP-9002', 'Turrialba, Cartago, Barrio Recope',        45.00, 7200.00, 'EN_TRANSITO', 2, 2, GETDATE(), GETDATE()),
    ('EXP-9003', 'San Pedro, Montes de Oca, San Jose',        8.75, 2100.00, 'ENTREGADO',   3, 3, GETDATE(), GETDATE()),
    ('EXP-9004', 'Liberia, Guanacaste, Centro',             120.00, 15400.00,'PENDIENTE',   3, 1, GETDATE(), GETDATE()),
    ('EXP-9005', 'Perez Zeledon, San Isidro',                30.20, 5600.00, 'CANCELADO',   1, 2, GETDATE(), GETDATE());
GO

/* Verificacion rapida del JOIN que replica el JPQL con JOIN FETCH */
SELECT e.codigo_rastreo, e.estado_envio, v.placa, emp.nombre AS empresa,
       c.nombre + ' ' + c.apellidos AS conductor
FROM dbo.Envio e
    LEFT JOIN dbo.Vehiculo v          ON v.vehiculo_id = e.vehiculo_id
    LEFT JOIN dbo.EmpresaLogistica emp ON emp.empresa_id = v.empresa_id
    LEFT JOIN dbo.Conductor c          ON c.conductor_id = e.conductor_id
ORDER BY e.envio_id DESC;
GO
