-- =============================================
-- Base de datos: micro-notificaciones (Azure SQL)
-- =============================================

-- Tabla principal de notificaciones
CREATE TABLE Notificaciones (
    id INT PRIMARY KEY IDENTITY(1,1),
    usuario_id INT NOT NULL,
    titulo VARCHAR(100),
    mensaje TEXT,
    tipo VARCHAR(50), -- 'PUSH', 'EMAIL', 'SISTEMA'
    estado VARCHAR(20) DEFAULT 'PENDIENTE', -- PENDIENTE, ENVIADO, LEIDO, FALLIDO
    fecha_creacion DATETIME DEFAULT GETDATE(),
    fecha_envio DATETIME NULL,
    leida BIT DEFAULT 0
);

-- Tokens de recuperación de contraseña
CREATE TABLE TokenRecuperacion (
    id INT PRIMARY KEY IDENTITY(1,1),
    token VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    fecha_expiracion DATETIME NOT NULL,
    usado BIT DEFAULT 0
);

-- Preferencias de usuario para recibir notificaciones
CREATE TABLE PreferenciasNotificacion (
    usuario_id INT PRIMARY KEY,
    recibir_push BIT DEFAULT 1,
    recibir_email BIT DEFAULT 1,
    hora_resumen_diario TIME NULL
);

-- Tokens de dispositivos (FCM) para notificaciones PUSH
CREATE TABLE DispositivosUsuario (
    id INT PRIMARY KEY IDENTITY(1,1),
    usuario_id INT NOT NULL,
    token_fcm VARCHAR(500) NOT NULL,
    tipo_dispositivo VARCHAR(50), -- 'ANDROID', 'IOS', 'WEB'
    fecha_registro DATETIME DEFAULT GETDATE(),
    activo BIT DEFAULT 1
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
