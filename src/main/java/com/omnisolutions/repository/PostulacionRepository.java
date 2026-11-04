package com.omnisolutions.repository;

import com.omnisolutions.model.Postulacion;
import com.omnisolutions.model.Publicacion;
import com.omnisolutions.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostulacionRepository extends JpaRepository<Postulacion, Long> {

    boolean existsByUsuarioAndPublicacion(Usuario usuario, Publicacion publicacion);

    Optional<Postulacion> findByUsuarioAndPublicacion(Usuario usuario, Publicacion publicacion);

    long countByPublicacion(Publicacion publicacion);

    /** Postulaciones realizadas por un empleado. */
    @Query("""
           SELECT po FROM Postulacion po
           JOIN FETCH po.publicacion pu JOIN FETCH pu.usuario
           WHERE po.usuario = :usuario
           ORDER BY po.fechaPostulacion DESC, po.id DESC
           """)
    List<Postulacion> buscarRealizadas(@Param("usuario") Usuario usuario);

    /** Postulaciones recibidas en las publicaciones de un empleador (filtro opcional por publicacion). */
    @Query("""
           SELECT po FROM Postulacion po
           JOIN FETCH po.publicacion pu JOIN FETCH po.usuario
           WHERE pu.usuario = :empleador
             AND (:idPublicacion IS NULL OR pu.id = :idPublicacion)
           ORDER BY po.fechaPostulacion DESC, po.id DESC
           """)
    List<Postulacion> buscarRecibidas(@Param("empleador") Usuario empleador,
                                      @Param("idPublicacion") Long idPublicacion);

    /** Ids de publicaciones a las que el usuario ya se postulo, con su estado (para el feed). */
    @Query("SELECT po.publicacion.id, po.estado FROM Postulacion po WHERE po.usuario = :usuario")
    List<Object[]> buscarEstadosPorUsuario(@Param("usuario") Usuario usuario);

    @Query("SELECT po.publicacion.id, COUNT(po) FROM Postulacion po "
         + "WHERE po.publicacion.usuario = :empleador GROUP BY po.publicacion.id")
    List<Object[]> contarPorPublicacionDe(@Param("empleador") Usuario empleador);

    @Modifying
    @Query("DELETE FROM Postulacion po WHERE po.publicacion = :publicacion")
    void eliminarPorPublicacion(@Param("publicacion") Publicacion publicacion);
}
