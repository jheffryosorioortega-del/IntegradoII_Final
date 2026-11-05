package pe.sgp.solicitudes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sgp.solicitudes.model.SolicitudHistorial;

import java.util.List;

public interface SolicitudHistorialRepository extends JpaRepository<SolicitudHistorial, Long> {
    List<SolicitudHistorial> findBySolicitudIdOrderByFechaAscIdAsc(Long solicitudId);
}
