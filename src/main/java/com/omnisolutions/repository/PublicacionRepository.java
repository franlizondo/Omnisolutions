package com.omnisolutions.repository;

import com.omnisolutions.model.EstadoPublicacion;
import com.omnisolutions.model.Publicacion;
import com.omnisolutions.model.TipoPublicacion;
import com.omnisolutions.model.Usuario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublicacionRepository extends JpaRepository<Publicacion, Long> {

    /** Feed: publicaciones con el estado indicado, filtro opcional por tipo y texto. */
    @Query("""
           SELECT p FROM Publicacion p JOIN FETCH p.usuario
           WHERE p.estado = :estado
             AND (:tipo IS NULL OR p.tipo = :tipo)
             AND (:texto IS NULL
                  OR LOWER(p.titulo) LIKE :texto
                  OR LOWER(p.descripcion) LIKE :texto
                  OR LOWER(p.ubicacion) LIKE :texto)
           ORDER BY p.fechaPublicacion DESC, p.id DESC
           """)
    List<Publicacion> buscarFeed(@Param("estado") EstadoPublicacion estado,
                                 @Param("tipo") TipoPublicacion tipo,
                                 @Param("texto") String texto);

    @Query("SELECT p FROM Publicacion p JOIN FETCH p.usuario WHERE p.usuario = :usuario "
         + "ORDER BY p.fechaPublicacion DESC, p.id DESC")
    List<Publicacion> buscarPorUsuario(@Param("usuario") Usuario usuario);
}
