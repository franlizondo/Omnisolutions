/* Alta y modificación de publicaciones. Con ?id=N edita una publicación propia. */
(async () => {
    const { api, badge, param, toast } = Omni;
    const yo = await Omni.iniciarPagina('publicar');
    const form = document.getElementById('form-publicacion');
    const alerta = document.getElementById('alerta');
    const contador = document.getElementById('contador');
    const id = param('id');
    const tipo = yo.tipoUsuario === 'EMPLEADOR' ? 'TRABAJO' : 'SERVICIO';

    document.getElementById('tipo-badge').innerHTML = badge(tipo);
    document.getElementById('explicacion').textContent = tipo === 'TRABAJO'
        ? 'Como empleador, publicás trabajos. Los empleados podrán postularse mientras la publicación esté activa.'
        : 'Como empleado, publicás servicios que ofrecés. Aparecerán en el feed mientras estén activos.';

    const actualizarContador = () => { contador.textContent = form.descripcion.value.length; };
    form.descripcion.addEventListener('input', actualizarContador);

    if (id) {
        document.getElementById('titulo-pagina').textContent = 'Editar publicación';
        document.getElementById('btn-guardar').textContent = 'Guardar cambios';
        document.getElementById('btn-cancelar').href = '/detalle.html?id=' + encodeURIComponent(id);
        document.title = 'Editar publicación | Omni Solutions';
        try {
            const p = await api('/api/publicaciones/' + encodeURIComponent(id));
            if (!p.propia) {
                form.hidden = true;
                return mostrar('Solo podés editar tus propias publicaciones.');
            }
            form.titulo.value = p.titulo;
            form.descripcion.value = p.descripcion;
            form.ubicacion.value = p.ubicacion || '';
            actualizarContador();
        } catch (e) {
            form.hidden = true;
            return mostrar(e.message);
        }
    }

    function mostrar(mensaje) {
        alerta.hidden = !mensaje;
        alerta.className = 'alert alert-error';
        alerta.textContent = mensaje || '';
    }

    form.addEventListener('submit', async ev => {
        ev.preventDefault();
        const datos = {
            titulo: form.titulo.value.trim(),
            descripcion: form.descripcion.value.trim(),
            ubicacion: form.ubicacion.value.trim() || null
        };
        const boton = document.getElementById('btn-guardar');
        boton.disabled = true;
        try {
            const p = id
                ? await api('/api/publicaciones/' + encodeURIComponent(id), { method: 'PUT', body: datos })
                : await api('/api/publicaciones', { method: 'POST', body: datos });
            toast(id ? 'Cambios guardados' : 'Publicación creada', 'success');
            window.location.href = '/detalle.html?id=' + p.id;
        } catch (e) {
            Omni.mostrarErroresCampos(form, e);
            mostrar(e.message);
            boton.disabled = false;
        }
    });
})();
