package pe.sgp.solicitudes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.sgp.solicitudes.dto.FotoResumen;
import pe.sgp.solicitudes.model.FotoSolicitud;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FotoSolicitudRepository extends JpaRepository<FotoSolicitud, Long> {

    long countBySolicitudId(Long solicitudId);

    Optional<FotoSolicitud> findByIdAndSolicitudId(Long id, Long solicitudId);

    /** Lista las fotos de una solicitud sin traer el contenido de cada una. */
    @Query("select new pe.sgp.solicitudes.dto.FotoResumen(f.id, f.nombre, f.tipo, f.tamano) "
         + "from FotoSolicitud f where f.solicitudId = :solicitudId order by f.id")
    List<FotoResumen> resumenes(@Param("solicitudId") Long solicitudId);

    /** Cuántas fotos tiene cada solicitud, en una sola consulta (para las listas). */
    @Query("select f.solicitudId, count(f) from FotoSolicitud f where f.solicitudId in :ids group by f.solicitudId")
    List<Object[]> contarPorSolicitud(@Param("ids") Collection<Long> ids);
}
