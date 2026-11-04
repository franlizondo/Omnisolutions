/* Administración de publicaciones propias: ver, editar, cerrar y eliminar. */
(async () => {
    const { api, esc, fecha, badge, icono, toast } = Omni;
    await Omni.iniciarPagina('mias');
    const lista = document.getElementById('lista');
    const filtros = document.getElementById('filtros');
    let publicaciones = [];
    let estado = '';

    function fila(p) {
        return `<div class="list-item" data-id="${p.id}">
            <div class="list-main">
                <div>${badge(p.tipo)} ${badge(p.estado)}</div>
                <a class="title" href="/detalle.html?id=${p.id}">${esc(p.titulo)}</a>
                <div class="post-info">
                    <span>${icono('reloj')}${esc(fecha(p.fechaPublicacion))}</span>
                    <span>${icono('ubicacion')}${esc(p.ubicacion || 'Sin ubicación')}</span>
                    ${p.tipo === 'TRABAJO' ? `<span>${icono('personas')}
                        <a href="/postulaciones.html?publicacion=${p.id}">${p.cantidadPostulaciones} postulación(es)</a></span>` : ''}
                </div>
                <div class="list-actions">
                    <a class="btn btn-sm" href="/detalle.html?id=${p.id}">Ver</a>
                    <a class="btn btn-sm" href="/publicar.html?id=${p.id}">Editar</a>
                    ${p.estado === 'ACTIVA' ? '<button class="btn btn-sm" data-accion="cerrar">Cerrar</button>' : ''}
                    <button class="btn btn-danger btn-sm" data-accion="eliminar">Eliminar</button>
                </div>
            </div></div>`;
    }

    function render() {
        const visibles = publicaciones.filter(p => !estado || p.estado === estado);
        lista.innerHTML = visibles.length ? visibles.map(fila).join('') : `<div class="empty">
            <strong>No hay publicaciones${estado ? ' en este estado' : ''}</strong>
            <a href="/publicar.html">Crear una publicación</a></div>`;
    }

    async function cargar() {
        try {
            publicaciones = await api('/api/publicaciones/mias');
            render();
        } catch (e) {
            lista.innerHTML = `<div class="alert alert-error">${esc(e.message)}</div>`;
        }
    }

    filtros.addEventListener('click', ev => {
        const chip = ev.target.closest('.chip');
        if (!chip) return;
        estado = chip.dataset.estado;
        filtros.querySelectorAll('.chip').forEach(c => c.classList.toggle('active', c === chip));
        render();
    });

    lista.addEventListener('click', async ev => {
        const boton = ev.target.closest('button[data-accion]');
        if (!boton) return;
        const id = boton.closest('[data-id]').dataset.id;
        const cerrar = boton.dataset.accion === 'cerrar';
        const pregunta = cerrar
            ? '¿Cerrar la publicación? Dejará de aparecer en el feed y no recibirá nuevas postulaciones.'
            : '¿Eliminar la publicación? También se eliminarán sus postulaciones. No se puede deshacer.';
        if (!confirm(pregunta)) return;
        boton.disabled = true;
        try {
            await api(cerrar ? `/api/publicaciones/${id}/cerrar` : `/api/publicaciones/${id}`,
                      { method: cerrar ? 'POST' : 'DELETE' });
            toast(cerrar ? 'Publicación cerrada' : 'Publicación eliminada', 'success');
            await cargar();
        } catch (e) {
            toast(e.message, 'error');
            boton.disabled = false;
        }
    });

    await cargar();
})();
