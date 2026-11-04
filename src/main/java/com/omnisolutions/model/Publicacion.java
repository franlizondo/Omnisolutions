package com.omnisolutions.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "publicaciones")
public class Publicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_publicacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, columnDefinition = "varchar(20)")
    private TipoPublicacion tipo;

    @Column(name = "titulo", nullable = false, length = 120)
    private String titulo;

    @Column(name = "descripcion", nullable = false, length = 2000)
    private String descripcion;

    @Column(name = "ubicacion", length = 120)
    private String ubicacion;

    @Column(name = "fecha_publicacion", nullable = false, updatable = false)
    private LocalDateTime fechaPublicacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, columnDefinition = "varchar(20)")
    private EstadoPublicacion estado = EstadoPublicacion.ACTIVA;

    @PrePersist
    void alCrear() {
        if (fechaPublicacion == null) {
            fechaPublicacion = LocalDateTime.now().withNano(0);
        }
    }

    public boolean esDe(Usuario otro) {
        return otro != null && usuario.getId().equals(otro.getId());
    }

    public boolean estaActiva() {
        return estado == EstadoPublicacion.ACTIVA;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public TipoPublicacion getTipo() { return tipo; }
    public void setTipo(TipoPublicacion tipo) { this.tipo = tipo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
    public LocalDateTime getFechaPublicacion() { return fechaPublicacion; }
    public EstadoPublicacion getEstado() { return estado; }
    public void setEstado(EstadoPublicacion estado) { this.estado = estado; }
}
