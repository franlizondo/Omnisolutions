/* =========================================================
   Omni Solutions - utilidades comunes del frontend
   (API con sesión + CSRF, barra superior, formato, componentes compartidos)
   ========================================================= */
const Omni = (() => {

    // ---------------------------------------------------------------- API
    function cookie(nombre) {
        const par = document.cookie.split('; ').find(c => c.startsWith(nombre + '='));
        return par ? decodeURIComponent(par.split('=')[1]) : null;
    }

    async function tokenCsrf() {
        let token = cookie('XSRF-TOKEN');
        if (!token) {
            const res = await fetch('/api/auth/csrf', { credentials: 'same-origin' });
            token = (await res.json()).token;
        }
        return token;
    }

    /**
     * Llama a la API. Lanza un Error con .status y .data si la respuesta no es exitosa.
     * Si la sesión expiró (401) redirige a la pantalla de acceso.
     */
    async function api(url, { method = 'GET', body, form, redirigir401 = true } = {}) {
        const opciones = { method, credentials: 'same-origin', headers: { Accept: 'application/json' } };
        if (method !== 'GET') {
            opciones.headers['X-XSRF-TOKEN'] = await tokenCsrf();
        }
        if (form) {
            opciones.body = new URLSearchParams(form);
        } else if (body !== undefined) {
            opciones.headers['Content-Type'] = 'application/json';
            opciones.body = JSON.stringify(body);
        }
        const res = await fetch(url, opciones);
        const texto = await res.text();
        let datos = null;
        if (texto) {
            try { datos = JSON.parse(texto); } catch { datos = { mensaje: texto }; }
        }
        if (res.status === 401 && redirigir401) {
            window.location.href = '/index.html';
            throw new Error('Sesión no iniciada');
        }
        if (!res.ok) {
            const error = new Error((datos && datos.mensaje) || 'No se pudo completar la operación');
            error.status = res.status;
            error.data = datos;
            throw error;
        }
        return datos;
    }

    // ---------------------------------------------------------------- formato
    function esc(valor) {
        return String(valor ?? '').replace(/[&<>"']/g, c => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
        }[c]));
    }

    function iniciales(nombre) {
        const partes = String(nombre || '?').trim().split(/\s+/);
        return ((partes[0] || '?')[0] + (partes.length > 1 ? partes[partes.length - 1][0] : '')).toUpperCase();
    }

    function colorAvatar(texto) {
        let h = 0;
        for (const c of String(texto || '')) h = (h * 31 + c.charCodeAt(0)) >>> 0;
        return 'av-' + (h % 6);
    }

    function avatar(nombre, tam = '') {
        return `<div class="avatar ${tam} ${colorAvatar(nombre)}" aria-hidden="true">${esc(iniciales(nombre))}</div>`;
    }

    function fecha(iso) {
        if (!iso) return '';
        const d = new Date(iso);
        const seg = Math.round((Date.now() - d.getTime()) / 1000);
        if (seg < 60) return 'hace un momento';
        if (seg < 3600) return `hace ${Math.floor(seg / 60)} min`;
        if (seg < 86400) return `hace ${Math.floor(seg / 3600)} h`;
        if (seg < 86400 * 7) return `hace ${Math.floor(seg / 86400)} d`;
        return fechaLarga(iso);
    }

    function fechaLarga(iso) {
        if (!iso) return '';
        return new Date(iso).toLocaleDateString('es-AR', { day: 'numeric', month: 'long', year: 'numeric' });
    }

    const ETIQUETAS = {
        TRABAJO: 'Trabajo', SERVICIO: 'Servicio', EMPLEADOR: 'Empleador', EMPLEADO: 'Empleado',
        ACTIVA: 'Activa', CERRADA: 'Cerrada', PENDIENTE: 'Pendiente', ACEPTADA: 'Aceptada', RECHAZADA: 'Rechazada'
    };

    function etiqueta(valor) {
        return ETIQUETAS[valor] || valor;
    }

    function badge(valor) {
        return `<span class="badge badge-${esc(String(valor).toLowerCase())}">${esc(etiqueta(valor))}</span>`;
    }

    function param(nombre) {
        return new URLSearchParams(window.location.search).get(nombre);
    }

    // ---------------------------------------------------------------- avisos
    function toast(mensaje, tipo = '') {
        let zona = document.querySelector('.toast-zone');
        if (!zona) {
            zona = document.createElement('div');
            zona.className = 'toast-zone';
            zona.setAttribute('role', 'status');
            document.body.appendChild(zona);
        }
        const t = document.createElement('div');
        t.className = 'toast' + (tipo ? ' toast-' + tipo : '');
        t.textContent = mensaje;
        zona.appendChild(t);
        setTimeout(() => t.remove(), 4000);
    }

    function mostrarErroresCampos(formulario, error) {
        formulario.querySelectorAll('.field-error').forEach(e => e.remove());
        formulario.querySelectorAll('.input-error').forEach(e => e.classList.remove('input-error'));
        const errores = error && error.data && error.data.errores;
        if (!errores) return;
        for (const [campo, mensaje] of Object.entries(errores)) {
            const input = formulario.querySelector(`[name="${campo}"]`);
            if (!input) continue;
            input.classList.add('input-error');
            const div = document.createElement('div');
            div.className = 'field-error';
            div.textContent = mensaje;
            (input.closest('.form-group') || input.parentElement).appendChild(div);
        }
    }

    // ---------------------------------------------------------------- barra superior
    const ICONOS = {
        inicio: '<path d="M12 3 2 11h3v9h5v-6h4v6h5v-9h3L12 3z"/>',
        publicar: '<path d="M19 11h-6V5h-2v6H5v2h6v6h2v-6h6z"/>',
        mias: '<path d="M4 4h16v4H4zm0 6h16v4H4zm0 6h10v4H4z"/>',
        postulaciones: '<path d="M10 4h4a2 2 0 0 1 2 2v1h4a1 1 0 0 1 1 1v11a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V8a1 1 0 0 1 1-1h4V6a2 2 0 0 1 2-2zm0 3h4V6h-4v1z"/>',
        perfil: '<path d="M12 12a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9zm0 2c-4.4 0-8 2.2-8 5v2h16v-2c0-2.8-3.6-5-8-5z"/>',
        salir: '<path d="M10 17l1.4 1.4L17.8 12l-6.4-6.4L10 7l4 4H3v2h11l-4 4zM19 3h-6v2h6v14h-6v2h6a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2z"/>',
        ubicacion: '<path d="M12 2a7 7 0 0 0-7 7c0 5.2 7 13 7 13s7-7.8 7-13a7 7 0 0 0-7-7zm0 9.5A2.5 2.5 0 1 1 12 6.5a2.5 2.5 0 0 1 0 5z"/>',
        reloj: '<path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm1 10.4 3.5 2.1-.8 1.3L11 13V7h2v5.4z"/>',
        personas: '<path d="M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm8 0a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM9 13c-3.3 0-7 1.7-7 4v2h14v-2c0-2.3-3.7-4-7-4zm8 0c-.5 0-1 0-1.5.1 1.5 1 2.5 2.3 2.5 3.9v2h5v-2c0-2.2-3.4-4-6-4z"/>'
    };

    function icono(nombre) {
        return `<svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">${ICONOS[nombre]}</svg>`;
    }

    const LOGO = `<svg class="brand-mark" viewBox="0 0 40 40" aria-hidden="true">
        <rect width="40" height="40" rx="9" fill="#1a5fb4"/>
        <circle cx="20" cy="20" r="10.5" fill="none" stroke="#fff" stroke-width="4.5"/>
        <circle cx="29" cy="11" r="3.2" fill="#9cc7ef"/></svg>`;

    function marca() {
        return `<a class="brand" href="/inicio.html">${LOGO}<span class="brand-name">Omni <span>Solutions</span></span></a>`;
    }

    function renderTopbar(activo) {
        const enlaces = [
            ['inicio', '/inicio.html', 'Inicio'],
            ['publicar', '/publicar.html', 'Publicar'],
            ['mias', '/mis-publicaciones.html', 'Mis publicaciones'],
            ['postulaciones', '/postulaciones.html', 'Postulaciones'],
            ['perfil', '/perfil.html', 'Perfil']
        ];
        const header = document.createElement('header');
        header.className = 'topbar';
        header.innerHTML = `<div class="topbar-inner">
            ${marca()}
            <form class="topbar-search" role="search" action="/inicio.html">
                <input type="search" name="q" placeholder="Buscar publicaciones" aria-label="Buscar publicaciones"
                       value="${esc(activo === 'inicio' ? (param('q') || '') : '')}">
            </form>
            <nav class="nav" aria-label="Navegación principal">
                ${enlaces.map(([clave, href, texto]) => `<a href="${href}" class="${clave === activo ? 'active' : ''}"
                    title="${texto}">${icono(clave)}<span class="label">${texto}</span></a>`).join('')}
                <button type="button" id="btn-salir" title="Cerrar sesión">${icono('salir')}<span class="label">Salir</span></button>
            </nav></div>`;
        document.body.prepend(header);
        header.querySelector('#btn-salir').addEventListener('click', cerrarSesion);
    }

    async function cerrarSesion() {
        try {
            await api('/api/auth/logout', { method: 'POST', redirigir401: false });
        } finally {
            window.location.href = '/index.html';
        }
    }

    /** Verifica la sesión, dibuja la barra superior y devuelve el usuario actual. */
    async function iniciarPagina(activo) {
        const usuario = await api('/api/usuarios/me');
        renderTopbar(activo);
        return usuario;
    }

    // ---------------------------------------------------------------- acciones compartidas
    async function postular(idPublicacion, boton) {
        if (boton) boton.disabled = true;
        try {
            await api(`/api/publicaciones/${idPublicacion}/postulaciones`, { method: 'POST' });
            toast('¡Postulación enviada! El empleador la revisará.', 'success');
            return true;
        } catch (e) {
            toast(e.message, 'error');
            if (boton) boton.disabled = false;
            return false;
        }
    }

    /** Solicita el teléfono de la contraparte de una postulación aceptada y lo muestra en el contenedor. */
    async function mostrarContacto(idPostulacion, contenedor, boton) {
        if (boton) boton.disabled = true;
        try {
            const c = await api(`/api/postulaciones/${idPostulacion}/contacto`);
            contenedor.hidden = false;
            contenedor.className = 'contact-box' + (c.telefonoDisponible ? '' : ' sin-telefono');
            contenedor.innerHTML = c.telefonoDisponible
                ? `Teléfono de ${esc(c.nombre)}: <strong>${esc(c.telefono)}</strong>`
                : `${esc(c.nombre)} no proporcionó un número de teléfono.`;
            if (boton) boton.hidden = true;
        } catch (e) {
            toast(e.message, 'error');
            if (boton) boton.disabled = false;
        }
    }

    /** Fila de una postulación recibida (vista del empleador) con sus acciones. */
    function postulacionRecibidaHtml(p, { mostrarPublicacion = true } = {}) {
        return `<div class="list-item" data-postulacion="${p.id}">
            ${avatar(p.postulanteNombre, 'avatar-sm')}
            <div class="list-main">
                <div class="title">${esc(p.postulanteNombre)}</div>
                ${mostrarPublicacion ? `<div class="small">Para <a href="/detalle.html?id=${p.publicacionId}">${esc(p.publicacionTitulo)}</a>
                    ${p.publicacionEstado === 'CERRADA' ? badge('CERRADA') : ''}</div>` : ''}
                <div class="small muted">Postulado ${esc(fecha(p.fechaPostulacion))}</div>
                <div class="list-actions">
                    ${badge(p.estado)}
                    ${p.estado === 'PENDIENTE' ? `
                        <button class="btn btn-success btn-sm" data-accion="aceptar">Aceptar</button>
                        <button class="btn btn-danger btn-sm" data-accion="rechazar">Rechazar</button>` : ''}
                    ${p.estado === 'ACEPTADA' ? `<button class="btn btn-sm" data-accion="contacto">Ver teléfono</button>` : ''}
                </div>
                <div class="contacto" hidden></div>
            </div></div>`;
    }

    /** Conecta los botones de las filas de postulaciones recibidas. alCambiar() se llama tras aceptar/rechazar. */
    function conectarPostulacionesRecibidas(contenedor, alCambiar) {
        contenedor.addEventListener('click', async ev => {
            const boton = ev.target.closest('button[data-accion]');
            if (!boton) return;
            const fila = boton.closest('[data-postulacion]');
            const id = fila.dataset.postulacion;
            const accion = boton.dataset.accion;
            if (accion === 'contacto') {
                return mostrarContacto(id, fila.querySelector('.contacto'), boton);
            }
            const texto = accion === 'aceptar'
                ? '¿Aceptar esta postulación? Ambas partes podrán ver sus teléfonos.'
                : '¿Rechazar esta postulación? Esta acción no se puede deshacer.';
            if (!confirm(texto)) return;
            boton.disabled = true;
            try {
                await api(`/api/postulaciones/${id}/${accion}`, { method: 'POST' });
                toast(accion === 'aceptar' ? 'Postulación aceptada' : 'Postulación rechazada', 'success');
                await alCambiar();
            } catch (e) {
                toast(e.message, 'error');
                boton.disabled = false;
            }
        });
    }

    return {
        api, esc, avatar, fecha, fechaLarga, etiqueta, badge, param, toast, icono, marca,
        mostrarErroresCampos, iniciarPagina, postular, mostrarContacto,
        postulacionRecibidaHtml, conectarPostulacionesRecibidas
    };
})();
