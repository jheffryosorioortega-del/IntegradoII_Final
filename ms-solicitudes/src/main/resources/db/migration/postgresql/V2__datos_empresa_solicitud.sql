-- Extension aditiva: conserva las solicitudes existentes como PERSONA.
ALTER TABLE solicitud ADD COLUMN tipo_solicitante VARCHAR(10) NOT NULL DEFAULT 'PERSONA';
ALTER TABLE solicitud ADD COLUMN ruc VARCHAR(11);
ALTER TABLE solicitud ADD COLUMN razon_social VARCHAR(250);
ALTER TABLE solicitud ADD COLUMN direccion_fiscal VARCHAR(300);
ALTER TABLE solicitud ADD COLUMN cargo VARCHAR(80);
ALTER TABLE solicitud ADD COLUMN requiere_factura BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE solicitud ADD COLUMN persona_contacto VARCHAR(60);
ALTER TABLE solicitud ADD COLUMN correo_contacto VARCHAR(120);
ALTER TABLE solicitud ADD CONSTRAINT ck_solicitud_tipo CHECK (tipo_solicitante IN ('PERSONA', 'EMPRESA'));
ALTER TABLE solicitud ADD CONSTRAINT ck_solicitud_empresa CHECK (tipo_solicitante <> 'EMPRESA' OR (ruc IS NOT NULL AND razon_social IS NOT NULL AND persona_contacto IS NOT NULL AND correo_contacto IS NOT NULL));