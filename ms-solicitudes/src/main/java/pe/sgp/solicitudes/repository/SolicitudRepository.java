package pe.sgp.solicitudes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sgp.solicitudes.model.EstadoSolicitud;
import pe.sgp.solicitudes.model.Solicitud;

import java.util.List;
import java.util.Optional;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    List<Solicitud> findAllByOrderByFechaCreacionDesc();
    List<Solicitud> findByEstadoOrderByFechaCreacionDesc(EstadoSolicitud estado);
    List<Solicitud> findTop5ByOrderByFechaCreacionDesc();
    long countByEstado(EstadoSolicitud estado);

    /** La solicitud más nueva (la de id más alto): sirve para saber si llegó algo desde la última revisión. */
    Optional<Solicitud> findTopByOrderByIdDesc();

    /** Las solicitudes que llegaron después de la última que el panel ya conocía (máximo 10). */
    List<Solicitud> findTop10ByIdGreaterThanOrderByIdAsc(Long id);
}
