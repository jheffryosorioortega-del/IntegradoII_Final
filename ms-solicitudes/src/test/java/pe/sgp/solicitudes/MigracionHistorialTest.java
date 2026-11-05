package pe.sgp.solicitudes;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

/** V3 no debe perder solicitudes anteriores: quedan sin técnico y con su línea de historial "CREACION". */
class MigracionHistorialTest {
    @Test void migraV2ConservandoSolicitudesYCreandoSuHistorial() throws Exception {
        String url = "jdbc:h2:mem:upgrade_historial;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration/h2").target("2").load().migrate();
        try (var c = DriverManager.getConnection(url, "sa", ""); var st = c.createStatement()) {
            st.executeUpdate("""
                INSERT INTO solicitud (codigo,cliente_id,cliente_nombre,cliente_telefono,categoria,urgencia,estado,fecha_creacion)
                VALUES ('SOL-ANTIGUA',1,'Ana Prueba','987654321','BOMBAS','MEDIA','COTIZADA',TIMESTAMP '2026-01-15 09:30:00')
                """);
        }
        Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration/h2").load().migrate();
        try (var c = DriverManager.getConnection(url, "sa", ""); var st = c.createStatement();
             var filas = st.executeQuery("""
                SELECT s.codigo, s.estado, s.tecnico_correo, h.accion, h.usuario, h.nuevo, h.fecha
                FROM solicitud s JOIN solicitud_historial h ON h.solicitud_id = s.id
                """)) {
            assertTrue(filas.next());
            assertEquals("SOL-ANTIGUA", filas.getString(1));
            assertEquals("COTIZADA", filas.getString(2));      // el estado actual no se toca
            assertNull(filas.getString(3));                     // sin técnico
            assertEquals("CREACION", filas.getString(4));
            assertEquals("sistema", filas.getString(5));
            assertEquals("REGISTRADA", filas.getString(6));
            assertEquals("2026-01-15 09:30:00.0", filas.getTimestamp(7).toString());   // con la fecha original
            assertFalse(filas.next());
        }
    }
}
