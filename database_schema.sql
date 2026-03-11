-- =============================================
-- Base de datos: micro-notificaciones (Azure SQL)
-- Script actualizado para coincidir con entidades JPA
-- =============================================

-- Tabla de Usuarios (Si este microservicio maneja su propia réplica u origen)
CREATE TABLE usuarios (
    id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    email VARCHAR(255) NOT NULL UNIQUE,
    nombre VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(50) DEFAULT 'USUARIO'
);

-- Tabla principal de notificaciones
CREATE TABLE Notificaciones (
    id INT PRIMARY KEY IDENTITY(1,1),
    usuario_id UNIQUEIDENTIFIER NOT NULL,
    titulo VARCHAR(100),
    mensaje TEXT,
    tipo VARCHAR(50), -- 'PUSH', 'EMAIL', 'SISTEMA'
    estado VARCHAR(20) DEFAULT 'PENDIENTE', -- PENDIENTE, ENVIADO, LEIDO, FALLIDO
    fecha_creacion DATETIME2 DEFAULT CURRENT_TIMESTAMP,
    fecha_envio DATETIME2 NULL,
    leida BIT DEFAULT 0,
    CONSTRAINT FK_Notificaciones_Usuarios FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- Tokens de recuperación de contraseña
CREATE TABLE TokenRecuperacion (
    id INT PRIMARY KEY IDENTITY(1,1),
    token VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    fecha_expiracion DATETIME2 NOT NULL,
    usado BIT DEFAULT 0
);

-- Preferencias de usuario para recibir notificaciones (Opcional)
CREATE TABLE PreferenciasNotificacion (
    usuario_id UNIQUEIDENTIFIER PRIMARY KEY,
    recibir_push BIT DEFAULT 1,
    recibir_email BIT DEFAULT 1,
    hora_resumen_diario TIME NULL,
    CONSTRAINT FK_Preferencias_Usuarios FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- Tokens de dispositivos (FCM) para notificaciones PUSH (Opcional)
CREATE TABLE DispositivosUsuario (
    id INT PRIMARY KEY IDENTITY(1,1),
    usuario_id UNIQUEIDENTIFIER NOT NULL,
    token_fcm VARCHAR(500) NOT NULL,
    tipo_dispositivo VARCHAR(50), -- 'ANDROID', 'IOS', 'WEB'
    fecha_registro DATETIME2 DEFAULT CURRENT_TIMESTAMP,
    activo BIT DEFAULT 1,
    CONSTRAINT FK_Dispositivos_Usuarios FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- Plantillas para correos o mensajes predefinidos (Opcional)
CREATE TABLE PlantillasNotificacion (
    id INT PRIMARY KEY IDENTITY(1,1),
    nombre VARCHAR(100) NOT NULL,
    asunto VARCHAR(200),
    contenido_html TEXT,
    tipo VARCHAR(50) -- 'EMAIL', 'PUSH'
);

-- Índices para mejorar rendimiento
CREATE INDEX idx_notificaciones_usuario ON Notificaciones(usuario_id);
CREATE INDEX idx_notificaciones_estado ON Notificaciones(estado);
CREATE INDEX idx_dispositivos_usuario ON DispositivosUsuario(usuario_id);
CREATE INDEX idx_token_recuperacion ON TokenRecuperacion(token);