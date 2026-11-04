/* Feed principal: perfil lateral, publicaciones activas y acciones. */
(async () => {
    const { api, esc, avatar, fecha, badge, etiqueta, icono, param, postular } = Omni;
    const yo = await Omni.iniciarPagina('inicio');
    const esEmpleador = yo.tipoUsuario === 'EMPLEADOR';
    const feed = document.getElementById('feed');
    const filtros = document.getElementById('filtros');
    let tipoActual = '';

    // ---------------- columna izquierda
    async function cargarPerfilLateral() {
        const [mias, postulaciones] = await Promise.all([
            api('/api/publicaciones/mias'),
            api(esEmpleador ? '/api/postulaciones/recibidas' : '/api/postulaciones/realizadas')
        ]);
        const activas = mias.filter(p => p.estado === 'ACTIVA').length;
        const pendientes = postulaciones.filter(p => p.estado === 'PENDIENTE').length;
        const aceptadas = postulaciones.filter(p => p.estado === 'ACEPTADA').length;
        document.getElementById('perfil-lateral').innerHTML = `
            <div class="profile-cover"></div>
            <div class="profile-body">
                ${avatar(yo.nombre, 'avatar-lg')}
                <div class="profile-name">${esc(yo.nombre)}</div>
                <div class="small muted">@${esc(yo.usuario)}</div>
                <div class="small">${badge(yo.tipoUsuario)}</div>
            </div>
            <a class="stat-row" href="/mis-publicaciones.html">Publicaciones activas <strong>${activas}</strong></a>
            <a class="stat-row" href="/postulaciones.html">${esEmpleador ? 'Postulaciones por revisar' : 'Postulaciones pendientes'}
                <strong>${pendientes}</strong></a>
            <a class="stat-row" href="/postulaciones.html">Postulaciones aceptadas <strong>${aceptadas}</strong></a>
            <div class="card-section"><a class="btn btn-block btn-sm" href="/perfil.html">Ver mi perfil</a></div>`;
    }

    // ---------------- composer y consejos
    document.getElementById('composer').innerHTML = `
        ${avatar(yo.nombre)}
        <a class="btn-ghost-round" href="/publicar.html">
            ${esEmpleador ? 'Publicar un trabajo para encontrar personal' : 'Ofrecer un servicio'}</a>`;
    document.getElementById('acceso-publicar').textContent = esEmpleador ? 'Publicar un trabajo' : 'Ofrecer un servicio';
    const consejos = esEmpleador
        ? ['Describí con claridad las tareas, el horario y la ubicación.',
           'Revisá las postulaciones a diario: podés aceptar a varias personas para un mismo trabajo.',
           'Cerrá la publicación cuando ya no busques personal.',
           'Cargá tu teléfono en el perfil para que los empleados aceptados puedan contactarte.']
        : ['Postulate solo a trabajos que se ajusten a tu disponibilidad.',
           'Publicá tus servicios con una descripción clara de lo que ofrecés.',
           'Cargá tu teléfono en el perfil: el empleador lo verá solo si te acepta.',
           'Seguí el estado de tus postulaciones desde la sección Postulaciones.'];
    document.getElementById('consejos').innerHTML = consejos.map(c => `<li>${esc(c)}</li>`).join('');

    // ---------------- feed
    function tarjeta(p) {
        const descripcion = p.descripcion.length > 280 ? p.descripcion.slice(0, 280).trimEnd() + '…' : p.descripcion;
        let accion = '';
        if (p.puedePostularse) {
            accion = `<button class="btn btn-primary btn-sm" data-postular="${p.id}">Postularme</button>`;
        } else if (p.miPostulacion) {
            accion = `<span class="small muted">Tu postulación:</span> ${badge(p.miPostulacion)}`;
        } else if (p.propia) {
            accion = `<span class="badge">Tu publicación</span>` + (p.tipo === 'TRABAJO'
                ? ` <a class="small" href="/postulaciones.html?publicacion=${p.id}">${p.cantidadPostulaciones} postulación(es)</a>` : '');
        }
        return `<article class="card">
            <div class="post-header">
                ${avatar(p.autorNombre)}
                <div>
                    <div class="post-author">${esc(p.autorNombre)}</div>
                    <div class="post-meta">${esc(etiqueta(p.autorTipo))} · ${esc(fecha(p.fechaPublicacion))}</div>
                </div>
                ${badge(p.tipo)}
            </div>
            <h3 class="post-title"><a href="/detalle.html?id=${p.id}">${esc(p.titulo)}</a></h3>
            <div class="post-desc">${esc(descripcion)}</div>
            <div class="post-info">
                <span>${icono('ubicacion')}${esc(p.ubicacion || 'Ubicación no especificada')}</span>
            </div>
            <div class="post-actions">
                <a class="btn btn-sm" href="/detalle.html?id=${p.id}">Ver detalle</a>
                ${accion}
            </div>
        </article>`;
    }

    async function cargarFeed() {
        const q = filtros.q.value.trim();
        const params = new URLSearchParams();
        if (tipoActual) params.set('tipo', tipoActual);
        if (q) params.set('q', q);
        feed.innerHTML = '<div class="loading">Cargando publicaciones…</div>';
        try {
            const lista = await api('/api/publicaciones?' + params);
            feed.innerHTML = lista.length ? lista.map(tarjeta).join('') : `<div class="card empty">
                <strong>No hay publicaciones para mostrar</strong>
                ${q || tipoActual ? 'Probá con otros filtros.' : 'Todavía nadie publicó. ¡Sé el primero!'}</div>`;
        } catch (e) {
            feed.innerHTML = `<div class="alert alert-error">${esc(e.message)}</div>`;
        }
    }

    feed.addEventListener('click', async ev => {
        const boton = ev.target.closest('button[data-postular]');
        if (!boton) return;
        if (await postular(boton.dataset.postular, boton)) {
            await Promise.all([cargarFeed(), cargarPerfilLateral()]);
        }
    });

    filtros.addEventListener('click', ev => {
        const chip = ev.target.closest('.chip');
        if (!chip) return;
        tipoActual = chip.dataset.tipo;
        filtros.querySelectorAll('.chip').forEach(c => c.classList.toggle('active', c === chip));
        cargarFeed();
    });
    filtros.addEventListener('submit', ev => { ev.preventDefault(); cargarFeed(); });
    let espera;
    filtros.q.addEventListener('input', () => { clearTimeout(espera); espera = setTimeout(cargarFeed, 350); });

    filtros.q.value = param('q') || '';
    await Promise.all([cargarFeed(), cargarPerfilLateral()]);
})();
