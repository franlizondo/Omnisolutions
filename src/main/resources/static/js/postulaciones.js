/*
 * Postulaciones: el empleador ve las RECIBIDAS (aceptar / rechazar / ver teléfono);
 * el empleado ve las REALIZADAS (estado y teléfono del empleador si fue aceptada).
 */
(async () => {
    const { api, esc, avatar, fecha, badge, param } = Omni;
    const yo = await Omni.iniciarPagina('postulaciones');
    const esEmpleador = yo.tipoUsuario === 'EMPLEADOR';
    const lista = document.getElementById('lista');
    const filtroEstado = document.getElementById('filtro-estado');
    const filtroPublicacion = document.getElementById('filtro-publicacion');
    let postulaciones = [];
    let estado = '';

    document.getElementById('titulo').textContent = esEmpleador ? 'Postulaciones recibidas' : 'Mis postulaciones';
    document.getElementById('descripcion').textContent = esEmpleador
        ? 'Personas que se postularon a tus trabajos. Cada postulación se acepta o rechaza por separado; podés aceptar a varias para un mismo trabajo.'
        : 'Trabajos a los que te postulaste. Si el empleador acepta tu postulación, vas a poder ver su teléfono.';

    function filaRealizada(p) {
        return `<div class="list-item" data-postulacion="${p.id}">
            ${avatar(p.empleadorNombre, 'avatar-sm')}
            <div class="list-main">
                <a class="title" href="/detalle.html?id=${p.publicacionId}">${esc(p.publicacionTitulo)}</a>
                ${p.publicacionEstado === 'CERRADA' ? badge('CERRADA') : ''}
                <div class="small">${esc(p.empleadorNombre)}</div>
                <div class="small muted">Te postulaste ${esc(fecha(p.fechaPostulacion))}</div>
                <div class="list-actions">
                    ${badge(p.estado)}
                    ${p.estado === 'ACEPTADA' ? '<button class="btn btn-success btn-sm" data-accion="contacto">Ver teléfono del empleador</button>' : ''}
                </div>
                <div class="contacto" hidden></div>
            </div></div>`;
    }

    function render() {
        const idPublicacion = filtroPublicacion.value;
        const visibles = postulaciones.filter(p =>
            (!estado || p.estado === estado) && (!idPublicacion || String(p.publicacionId) === idPublicacion));
        if (!visibles.length) {
            lista.innerHTML = `<div class="empty"><strong>No hay postulaciones para mostrar</strong>
                ${esEmpleador ? 'Cuando alguien se postule a tus trabajos aparecerá aquí.'
                              : '<a href="/inicio.html">Buscá trabajos en el inicio</a>'}</div>`;
            return;
        }
        lista.innerHTML = visibles.map(p => esEmpleador ? Omni.postulacionRecibidaHtml(p) : filaRealizada(p)).join('');
    }

    async function cargar() {
        try {
            postulaciones = await api(esEmpleador ? '/api/postulaciones/recibidas' : '/api/postulaciones/realizadas');
            render();
        } catch (e) {
            lista.innerHTML = `<div class="alert alert-error">${esc(e.message)}</div>`;
        }
    }

    if (esEmpleador) {
        const trabajos = (await api('/api/publicaciones/mias')).filter(p => p.tipo === 'TRABAJO');
        filtroPublicacion.innerHTML = '<option value="">Todos mis trabajos</option>'
            + trabajos.map(t => `<option value="${t.id}">${esc(t.titulo)}${t.estado === 'CERRADA' ? ' (cerrado)' : ''}</option>`).join('');
        filtroPublicacion.value = trabajos.some(t => String(t.id) === param('publicacion')) ? param('publicacion') : '';
        filtroPublicacion.hidden = false;
        filtroPublicacion.addEventListener('change', render);
        Omni.conectarPostulacionesRecibidas(lista, cargar);
    } else {
        lista.addEventListener('click', ev => {
            const boton = ev.target.closest('button[data-accion="contacto"]');
            if (!boton) return;
            const fila = boton.closest('[data-postulacion]');
            Omni.mostrarContacto(fila.dataset.postulacion, fila.querySelector('.contacto'), boton);
        });
    }

    filtroEstado.addEventListener('click', ev => {
        const chip = ev.target.closest('.chip');
        if (!chip) return;
        estado = chip.dataset.estado;
        filtroEstado.querySelectorAll('.chip').forEach(c => c.classList.toggle('active', c === chip));
        render();
    });

    await cargar();
})();
