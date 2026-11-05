package pe.sgp.usuarios.model;

/** Roles del sistema (RF01). Este enum existe también en ms-frontend: cada
 *  microservicio es dueño de sus propios tipos, no hay un "paquete compartido".
 *  Si en el futuro se agrega un rol, hay que actualizarlo en ambos lugares;
 *  por eso en un proyecto real conviene una librería de contratos compartida. */
public enum Rol {
    ADMINISTRADOR, COORDINADOR, TECNICO
}
