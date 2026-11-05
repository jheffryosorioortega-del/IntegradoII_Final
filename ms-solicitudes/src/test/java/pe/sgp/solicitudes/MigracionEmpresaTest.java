package pe.sgp.solicitudes;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import static org.junit.jupiter.api.Assertions.*;

class MigracionEmpresaTest {
    @Test void migraV1SinPerderSolicitudAnterior() throws Exception {
        String url = "jdbc:h2:mem:upgrade_empresa;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url,"sa","").locations("classpath:db/migration/h2").target("1").load().migrate();
        try (var connection = DriverManager.getConnection(url,"sa",""); var statement = connection.createStatement()) {
            statement.executeUpdate("""
                INSERT INTO solicitud (codigo,cliente_id,cliente_nombre,cliente_telefono,categoria,urgencia,estado,fecha_creacion)
                VALUES ('SOL-ANTERIOR',1,'Ana Prueba','987654321','BOMBAS','MEDIA','REGISTRADA',CURRENT_TIMESTAMP)
                """);
        }
        Flyway.configure().dataSource(url,"sa","").locations("classpath:db/migration/h2").load().migrate();
        try (var connection = DriverManager.getConnection(url,"sa",""); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT codigo, tipo_solicitante, requiere_factura FROM solicitud")) {
            assertTrue(rows.next()); assertEquals("SOL-ANTERIOR",rows.getString(1));
            assertEquals("PERSONA",rows.getString(2)); assertFalse(rows.getBoolean(3)); assertFalse(rows.next());
        }
    }
}
