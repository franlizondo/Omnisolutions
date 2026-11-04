/* Detalle de una publicación: acciones del propietario, postulación y contacto. */
(async () => {
    const { api, esc, avatar, fecha, fechaLarga, badge, etiqueta, icono, param, toast } = Omni;
    await Omni.iniciarPagina('');
    const id = param('id');
    const contenedor = document.getElementById('detalle');
    const seccionPostulaciones = document.getElementById('seccion-postulaciones');
    const listaPostulaciones = document.getElementById('lista-postulaciones');

    if (!id) {
        contenedor.innerHTML = '<div class="card alert alert-error">Falta indicar la publicación.</div>';
        return;
    }

    function accionesHtml(p) {
        if (p.propia) {
            return `<a class="btn btn-sm" href="/publicar.html?id=${p.id}">Editar</a>
                ${p.estado === 'ACTIVA' ? '<button class="btn btn-sm" data-accion="cerrar">Cerrar publicación</button>' : ''}
                <button class="btn btn-danger btn-sm" data-accion="eliminar">Eliminar</button>`;
        }
        if (p.puedePostularse) {
            return '<button class="btn btn-primary" data-accion="postular">Postularme a este trabajo</button>';
        }
        if (p.miPostulacion) {
            return `<span class="small muted">Tu postulación:</span> ${badge(p.miPostulacion)}
                ${p.miPostulacion === 'ACEPTADA'
                    ? '<button class="btn btn-success btn-sm" data-accion="contacto">Ver teléfono del empleador</button>' : ''}`;
        }
        if (p.tipo === 'TRABAJO' && p.estado === 'CERRADA') {
            return '<span class="small muted">Este trabajo está cerrado y no recibe nuevas postulaciones.</span>';
        }
        return '';
    }

    function render(p) {
        document.title = `${p.titulo} | Omni Solutions`;
        contenedor.innerHTML = `<article class="card">
            <div class="post-header">
                ${avatar(p.autorNombre)}
                <div>
                    <div class="post-author">${esc(p.autorNombre)}</div>
                    <div class="post-meta">${esc(etiqueta(p.autorTipo))} · ${esc(fecha(p.fechaPublicacion))}</div>
                </div>
                ${badge(p.tipo)}
            </div>
            <h1 class="post-title">${esc(p.titulo)}</h1>
            <div class="post-info">
                <span>${icono('ubicacion')}${esc(p.ubicacion || 'Ubicación no especificada')}</span>
                <span>${icono('reloj')}Publicado el ${esc(fechaLarga(p.fechaPublicacion))}</span>
                ${p.cantidadPostulaciones != null ? `<span>${icono('personas')}${p.cantidadPostulaciones} postulación(es)</span>` : ''}
                <span>${badge(p.estado)}</span>
            </div>
            <hr class="sep">
            <div class="post-desc">${esc(p.descripcion)}</div>
            <div class="post-actions" id="acciones">${accionesHtml(p)}</div>
            <div class="contacto" id="contacto" hidden></div>
        </article>`;

        const autor = document.getElementById('autor');
        autor.hidden = false;
        autor.innerHTML = `<h2 class="card-title">Publicado por</h2>
            <div class="post-header">${avatar(p.autorNombre)}
                <div><div class="post-author">${esc(p.autorNombre)}</div>
                <div class="post-meta">${esc(etiqueta(p.autorTipo))}</div></div></div>
            <p class="small muted">El teléfono de contacto se comparte solo cuando una postulación es aceptada.</p>`;

        contenedor.querySelector('#acciones').addEventListener('click', ev => {
            const boton = ev.target.closest('button[data-accion]');
            if (boton) ejecutar(boton.dataset.accion, boton, p);
        });
    }

    async function ejecutar(accion, boton, p) {
        if (accion === 'postular') {
            if (await Omni.postular(p.id, boton)) await cargar();
        } else if (accion === 'contacto') {
            await Omni.mostrarContacto(p.miPostulacionId, document.getElementById('contacto'), boton);
        } else if (accion === 'cerrar') {
            if (!confirm('¿Cerrar la publicación? Dejará de aparecer en el feed y no recibirá nuevas postulaciones.')) return;
            boton.disabled = true;
            try {
                await api(`/api/publicaciones/${p.id}/cerrar`, { method: 'POST' });
                toast('Publicación cerrada', 'success');
                await cargar();
            } catch (e) { toast(e.message, 'error'); boton.disabled = false; }
        } else if (accion === 'eliminar') {
            if (!confirm('¿Eliminar la publicación? También se eliminarán sus postulaciones. No se puede deshacer.')) return;
            boton.disabled = true;
            try {
                await api(`/api/publicaciones/${p.id}`, { method: 'DELETE' });
                window.location.href = '/mis-publicaciones.html';
            } catch (e) { toast(e.message, 'error'); boton.disabled = false; }
        }
    }

    async function cargarPostulaciones(p) {
        const lista = await api('/api/postulaciones/recibidas?publicacionId=' + p.id);
        const pendientes = lista.filter(x => x.estado === 'PENDIENTE').length;
        document.getElementById('resumen-postulaciones').textContent = `${lista.length} en total · ${pendientes} pendiente(s)`;
        listaPostulaciones.innerHTML = lista.length
            ? lista.map(x => Omni.postulacionRecibidaHtml(x, { mostrarPublicacion: false })).join('')
            : '<div class="empty">Todavía no hay postulaciones para este trabajo.</div>';
    }

    async function cargar() {
        try {
            const p = await api('/api/publicaciones/' + encodeURIComponent(id));
            render(p);
            seccionPostulaciones.hidden = !(p.propia && p.tipo === 'TRABAJO');
            if (!seccionPostulaciones.hidden) await cargarPostulaciones(p);
        } catch (e) {
            contenedor.innerHTML = `<div class="card"><div class="alert alert-error">${esc(e.message)}</div>
                <a class="btn btn-sm" href="/inicio.html">Volver al inicio</a></div>`;
        }
    }

    Omni.conectarPostulacionesRecibidas(listaPostulaciones, cargar);
    await cargar();
})();
