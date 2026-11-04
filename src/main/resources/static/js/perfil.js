/* Perfil propio: consulta y modificación de nombre y teléfono. */
(async () => {
    const { api, esc, avatar, badge, etiqueta, fechaLarga } = Omni;
    let yo = await Omni.iniciarPagina('perfil');
    const form = document.getElementById('form-perfil');
    const alerta = document.getElementById('alerta');

    function render() {
        document.getElementById('cabecera').innerHTML = `
            ${avatar(yo.nombre, 'avatar-xl')}
            <h1>${esc(yo.nombre)}</h1>
            <div class="muted">@${esc(yo.usuario)} · ${badge(yo.tipoUsuario)}</div>
            <dl class="detail-grid">
                <dt>Teléfono</dt><dd>${yo.telefono ? esc(yo.telefono) : '<span class="muted">No proporcionado</span>'}</dd>
                <dt>Miembro desde</dt><dd>${esc(fechaLarga(yo.fechaRegistro))}</dd>
            </dl>`;
        form.nombre.value = yo.nombre;
        form.telefono.value = yo.telefono || '';
        form.querySelector('#usuario').value = yo.usuario;
        form.querySelector('#rol').value = etiqueta(yo.tipoUsuario);
    }

    function mostrar(mensaje, tipo) {
        alerta.hidden = !mensaje;
        alerta.className = 'alert alert-' + tipo;
        alerta.textContent = mensaje || '';
    }

    form.addEventListener('submit', async ev => {
        ev.preventDefault();
        const boton = form.querySelector('button[type=submit]');
        boton.disabled = true;
        try {
            yo = await api('/api/usuarios/me', {
                method: 'PUT',
                body: { nombre: form.nombre.value.trim(), telefono: form.telefono.value.trim() || null }
            });
            Omni.mostrarErroresCampos(form, null);
            render();
            mostrar('Perfil actualizado correctamente.', 'success');
        } catch (e) {
            Omni.mostrarErroresCampos(form, e);
            mostrar(e.message, 'error');
        } finally {
            boton.disabled = false;
        }
    });

    render();
})();
