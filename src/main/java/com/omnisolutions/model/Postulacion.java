package com.omnisolutions.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "postulaciones",
       uniqueConstraints = @UniqueConstraint(name = "uk_postulaciones_usuario_publicacion",
                                             columnNames = {"id_usuario", "id_publicacion"}))
public class Postulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_postulacion")
    private Long id;

    /** Empleado que se postula. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_publicacion", nullable = false)
    private Publicacion publicacion;

    @Column(name = "fecha_postulacion", nullable = false, updatable = false)
    private LocalDateTime fechaPostulacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, columnDefinition = "varchar(20)")
    private EstadoPostulacion estado = EstadoPostulacion.PENDIENTE;

    @PrePersist
    void alCrear() {
        if (fechaPostulacion == null) {
            fechaPostulacion = LocalDateTime.now().withNano(0);
        }
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Publicacion getPublicacion() { return publicacion; }
    public void setPublicacion(Publicacion publicacion) { this.publicacion = publicacion; }
    public LocalDateTime getFechaPostulacion() { return fechaPostulacion; }
    public EstadoPostulacion getEstado() { return estado; }
    public void setEstado(EstadoPostulacion estado) { this.estado = estado; }
}
