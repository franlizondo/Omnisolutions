/* Pantalla de acceso: inicio de sesión y registro. */
(async () => {
    document.getElementById('marca').innerHTML = Omni.marca();

    // Si ya hay una sesión activa, ir directo al inicio.
    try {
        await Omni.api('/api/usuarios/me', { redirigir401: false });
        window.location.replace('/inicio.html');
        return;
    } catch { /* sin sesión: mostrar formularios */ }

    const alerta = document.getElementById('alerta');
    const formLogin = document.getElementById('form-login');
    const formRegistro = document.getElementById('form-registro');

    function mostrarAlerta(mensaje, tipo = 'error') {
        alerta.hidden = !mensaje;
        alerta.className = 'alert alert-' + tipo;
        alerta.textContent = mensaje || '';
    }

    function cambiarPestania(nombre) {
        document.querySelectorAll('.tab').forEach(t => {
            const activa = t.dataset.tab === nombre;
            t.classList.toggle('active', activa);
            t.setAttribute('aria-selected', activa);
        });
        formLogin.hidden = nombre !== 'login';
        formRegistro.hidden = nombre !== 'registro';
        mostrarAlerta('');
    }

    document.querySelectorAll('.tab').forEach(t => t.addEventListener('click', () => cambiarPestania(t.dataset.tab)));

    async function iniciarSesion(usuario, contrasena) {
        await Omni.api('/api/auth/login', { method: 'POST', form: { usuario, contrasena }, redirigir401: false });
        window.location.href = '/inicio.html';
    }

    formLogin.addEventListener('submit', async ev => {
        ev.preventDefault();
        const usuario = formLogin.usuario.value.trim();
        const contrasena = formLogin.contrasena.value;
        if (!usuario || !contrasena) {
            return mostrarAlerta('Ingresá tu usuario y contraseña.');
        }
        const boton = formLogin.querySelector('button[type=submit]');
        boton.disabled = true;
        try {
            await iniciarSesion(usuario, contrasena);
        } catch (e) {
            mostrarAlerta(e.message);
            boton.disabled = false;
        }
    });

    formRegistro.addEventListener('submit', async ev => {
        ev.preventDefault();
        const rol = formRegistro.querySelector('input[name=tipoUsuario]:checked');
        const datos = {
            nombre: formRegistro.nombre.value.trim(),
            usuario: formRegistro.usuario.value.trim(),
            contrasena: formRegistro.contrasena.value,
            tipoUsuario: rol ? rol.value : null,
            telefono: formRegistro.telefono.value.trim() || null
        };
        if (!datos.tipoUsuario) {
            return mostrarAlerta('Elegí si sos empleador o empleado.');
        }
        const boton = formRegistro.querySelector('button[type=submit]');
        boton.disabled = true;
        try {
            await Omni.api('/api/auth/registro', { method: 'POST', body: datos, redirigir401: false });
            Omni.mostrarErroresCampos(formRegistro, null);
            await iniciarSesion(datos.usuario, datos.contrasena);
        } catch (e) {
            Omni.mostrarErroresCampos(formRegistro, e);
            mostrarAlerta(e.message);
            boton.disabled = false;
        }
    });
})();
