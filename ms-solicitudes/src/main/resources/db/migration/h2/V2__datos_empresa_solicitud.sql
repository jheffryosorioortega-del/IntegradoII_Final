-- Extensión aditiva: conserva las solicitudes existentes como PERSONA.
ALTER TABLE solicitud ADD tipo_solicitante VARCHAR(10) NOT NULL DEFAULT 'PERSONA';
ALTER TABLE solicitud ADD ruc VARCHAR(11) NULL;
ALTER TABLE solicitud ADD razon_social NVARCHAR(250) NULL;
ALTER TABLE solicitud ADD direccion_fiscal NVARCHAR(300) NULL;
ALTER TABLE solicitud ADD cargo NVARCHAR(80) NULL;
ALTER TABLE solicitud ADD requiere_factura BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE solicitud ADD persona_contacto NVARCHAR(60) NULL;
ALTER TABLE solicitud ADD correo_contacto NVARCHAR(120) NULL;
ALTER TABLE solicitud ADD CONSTRAINT ck_solicitud_tipo CHECK (tipo_solicitante IN ('PERSONA', 'EMPRESA'));
ALTER TABLE solicitud ADD CONSTRAINT ck_solicitud_empresa CHECK (tipo_solicitante <> 'EMPRESA' OR (ruc IS NOT NULL AND razon_social IS NOT NULL AND persona_contacto IS NOT NULL AND correo_contacto IS NOT NULL));
