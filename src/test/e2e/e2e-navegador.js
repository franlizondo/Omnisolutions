/*
 * Prueba de punta a punta en navegador (Chromium + Playwright) sobre la aplicacion en ejecucion.
 * Requisitos: Node.js y el paquete "playwright" con Chromium instalado.
 * Uso: con la aplicacion corriendo y una base SIN los usuarios laura, martin y sofia:
 *   node src/test/e2e/e2e-navegador.js        (OMNI_URL permite cambiar la URL base)
 * Las capturas se guardan en OMNI_SHOTS (por defecto, carpeta temporal del sistema).
 */
const { chromium } = require('playwright');
const B = process.env.OMNI_URL || 'http://localhost:8080';
const SHOT = (process.env.OMNI_SHOTS || require('os').tmpdir() + '/omni-e2e') + '/';
const errores = [];
const ok = (cond, msg) => { if (!cond) throw new Error('FALLO: ' + msg); console.log('  ✓ ' + msg); };

async function nuevaPagina(browser, opts = {}) {
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 860 }, ...opts });
  const page = await ctx.newPage();
  page.on('dialog', d => d.accept());
  page.on('console', m => { if (m.type() === 'error' && !m.text().includes('status of 401')) errores.push(m.text()); });
  page.on('pageerror', e => errores.push(e.message));
  return page;
}
async function registrar(page, nombre, usuario, rol, tel) {
  await page.goto(B + '/index.html');
  await page.click('button[data-tab=registro]');
  await page.fill('#reg-nombre', nombre);
  await page.fill('#reg-usuario', usuario);
  await page.fill('#reg-contrasena', 'clave123');
  await page.check(`input[name=tipoUsuario][value=${rol}]`, { force: true });
  if (tel) await page.fill('#reg-telefono', tel);
  await page.click('#form-registro button[type=submit]');
  await page.waitForURL('**/inicio.html');
  await page.waitForSelector('.profile-name');
}
async function login(page, usuario) {
  await page.goto(B + '/index.html');
  await page.fill('#login-usuario', usuario);
  await page.fill('#login-contrasena', 'clave123');
  await page.click('#form-login button[type=submit]');
  await page.waitForURL('**/inicio.html');
  await page.waitForSelector('.profile-name');
}
async function salir(page) {
  await page.click('#btn-salir');
  await page.waitForURL('**/index.html');
}
async function publicar(page, titulo, desc, ubic) {
  await page.goto(B + '/publicar.html');
  await page.fill('#titulo', titulo);
  await page.fill('#descripcion', desc);
  if (ubic) await page.fill('#ubicacion', ubic);
  await page.click('#btn-guardar');
  await page.waitForURL('**/detalle.html?id=*');
  await page.waitForSelector('h1.post-title');
  return Number(new URL(page.url()).searchParams.get('id'));
}

(async () => {
  require('fs').mkdirSync(SHOT, { recursive: true });
  const browser = await chromium.launch();
  const page = await nuevaPagina(browser);

  console.log('1. Acceso y protección');
  await page.goto(B + '/inicio.html');
  ok(page.url().endsWith('/index.html'), 'sin sesión, inicio.html redirige a index.html');
  await page.screenshot({ path: SHOT + '01-acceso.png' });
  await page.fill('#login-usuario', 'nadie'); await page.fill('#login-contrasena', 'mala');
  await page.click('#form-login button[type=submit]');
  await page.waitForSelector('#alerta:not([hidden])');
  ok((await page.textContent('#alerta')).includes('incorrectos'), 'login incorrecto muestra error');

  console.log('2. Empleador registra y publica');
  await registrar(page, 'Laura Gómez', 'laura', 'EMPLEADOR', '351 555-0101');
  ok((await page.textContent('.profile-name')) === 'Laura Gómez', 'registro + login automático, perfil lateral');
  const t1 = await publicar(page, 'Mozo/a para eventos de fin de semana', 'Buscamos mozo/a con experiencia para eventos sociales los sábados.\nHorario: 20 a 04 h.', 'Córdoba Capital');
  ok((await page.textContent('#acciones')).includes('Cerrar publicación'), 'detalle propio muestra Editar/Cerrar/Eliminar');
  const t2 = await publicar(page, 'Ayudante de cocina', 'Se necesita ayudante de cocina para restaurante, turno mañana.', null);
  await page.goto(B + '/publicar.html?id=' + t2);
  await page.waitForFunction(() => document.querySelector('#titulo').value !== '');
  await page.fill('#ubicacion', 'Villa Carlos Paz');
  await page.click('#btn-guardar');
  await page.waitForURL('**/detalle.html?id=' + t2);
  await page.waitForSelector('h1.post-title');
  ok((await page.textContent('.post-info')).includes('Villa Carlos Paz'), 'edición de publicación propia');
  await salir(page);

  console.log('3. Empleados se registran, publican servicio y se postulan');
  await registrar(page, 'Martín Ruiz', 'martin', 'EMPLEADO', '351 555-0202');
  ok(await page.locator('article').count() === 2, 'feed muestra los 2 trabajos');
  await publicar(page, 'Electricista matriculado', 'Instalaciones eléctricas domiciliarias y reparaciones en general.', 'Córdoba');
  await page.goto(B + '/inicio.html');
  await page.waitForSelector('article');
  const tarjetaT1 = page.locator('article', { hasText: 'Mozo/a para eventos' });
  await tarjetaT1.locator('button[data-postular]').click();
  await page.waitForSelector('article:has-text("Mozo/a para eventos") .badge-pendiente');
  ok(true, 'postulación desde el feed queda PENDIENTE');
  ok(await page.locator('article:has-text("Electricista") button[data-postular]').count() === 0, 'no hay botón Postularme en servicios');
  await page.screenshot({ path: SHOT + '02-feed-empleado.png', fullPage: true });
  await salir(page);

  await registrar(page, 'Sofía Díaz', 'sofia', 'EMPLEADO', null);
  await page.goto(B + '/detalle.html?id=' + t1);
  await page.click('button[data-accion=postular]');
  await page.waitForSelector('#acciones .badge-pendiente');
  ok(true, 'postulación desde el detalle');
  await page.goto(B + '/detalle.html?id=' + t2);
  await page.click('button[data-accion=postular]');
  await page.waitForSelector('#acciones .badge-pendiente');
  await salir(page);

  console.log('4. Empleador gestiona postulaciones');
  await login(page, 'laura');
  await page.goto(B + '/detalle.html?id=' + t1);
  await page.waitForSelector('#lista-postulaciones .list-item');
  ok(await page.locator('#lista-postulaciones .list-item').count() === 2, 'detalle propio lista 2 postulaciones');
  await page.screenshot({ path: SHOT + '03-detalle-empleador.png', fullPage: true });
  await page.goto(B + '/postulaciones.html');
  await page.waitForSelector('.list-item');
  ok(await page.locator('.list-item').count() === 3, 'postulaciones recibidas: 3');
  const filaMartin = () => page.locator('.list-item', { hasText: 'Martín Ruiz' });
  const filaSofiaT1 = () => page.locator('.list-item', { hasText: 'Sofía Díaz' }).filter({ hasText: 'Mozo/a' });
  const filaSofiaT2 = () => page.locator('.list-item', { hasText: 'Sofía Díaz' }).filter({ hasText: 'Ayudante' });
  await filaMartin().locator('button[data-accion=aceptar]').click();
  await page.waitForSelector('.list-item:has-text("Martín Ruiz") .badge-aceptada');
  ok(await filaSofiaT1().locator('.badge-pendiente').count() === 1, 'aceptar a Martín no cambia la de Sofía');
  await filaSofiaT1().locator('button[data-accion=aceptar]').click();
  await page.waitForFunction(() => [...document.querySelectorAll('.list-item')].filter(e => e.textContent.includes('Mozo/a') && e.querySelector('.badge-aceptada')).length === 2);
  ok(true, 'dos postulaciones aceptadas para el mismo trabajo');
  await filaSofiaT2().locator('button[data-accion=rechazar]').click();
  await page.waitForFunction(() => document.querySelectorAll('.badge-rechazada').length === 1);
  ok(await filaSofiaT2().locator('button[data-accion]').count() === 0, 'postulación rechazada sin acciones');
  await filaMartin().locator('button[data-accion=contacto]').click();
  await page.waitForSelector('.list-item:has-text("Martín Ruiz") .contact-box');
  ok((await filaMartin().locator('.contact-box').textContent()).includes('351 555-0202'), 'empleador ve teléfono del empleado aceptado');
  await filaSofiaT1().locator('button[data-accion=contacto]').click();
  await page.waitForSelector('.contact-box.sin-telefono');
  ok((await filaSofiaT1().locator('.contact-box').textContent()).includes('no proporcionó'), 'mensaje de teléfono no proporcionado');
  await page.screenshot({ path: SHOT + '04-postulaciones-empleador.png', fullPage: true });

  console.log('5. Cierre de publicación y perfil');
  await page.goto(B + '/mis-publicaciones.html');
  await page.waitForSelector('.list-item');
  await page.locator('.list-item', { hasText: 'Ayudante' }).locator('button[data-accion=cerrar]').click();
  await page.waitForSelector('.list-item:has-text("Ayudante") .badge-cerrada');
  ok(true, 'cerrar publicación desde Mis publicaciones');
  await page.screenshot({ path: SHOT + '05-mis-publicaciones.png', fullPage: true });
  await page.goto(B + '/perfil.html');
  await page.waitForSelector('.profile-head h1');
  await page.fill('#telefono', '351 999-0000');
  await page.click('#form-perfil button[type=submit]');
  await page.waitForSelector('#alerta.alert-success');
  ok((await page.textContent('.profile-head')).includes('351 999-0000'), 'actualización de teléfono');
  await page.screenshot({ path: SHOT + '06-perfil.png', fullPage: true });
  await salir(page);

  console.log('6. Empleado ve resultado y contacto');
  await login(page, 'martin');
  ok(await page.locator('article', { hasText: 'Ayudante' }).count() === 0, 'trabajo cerrado no aparece en el feed');
  await page.goto(B + '/detalle.html?id=' + t2);
  await page.waitForSelector('h1.post-title');
  ok(await page.locator('button[data-accion=postular]').count() === 0, 'trabajo cerrado no permite postularse');
  await page.goto(B + '/postulaciones.html');
  await page.waitForSelector('.list-item');
  await page.click('button[data-accion=contacto]');
  await page.waitForSelector('.contact-box');
  ok((await page.textContent('.contact-box')).includes('351 999-0000'), 'empleado ve teléfono (actualizado) del empleador');
  await page.screenshot({ path: SHOT + '07-postulaciones-empleado.png', fullPage: true });
  await page.goto(B + '/inicio.html');
  await page.waitForSelector('article');
  await page.screenshot({ path: SHOT + '08-inicio-empleado.png' });

  console.log('7. Vista móvil');
  const movil = await nuevaPagina(browser, { viewport: { width: 390, height: 844 } });
  await login(movil, 'laura');
  await movil.waitForSelector('article');
  const anchoDoc = await movil.evaluate(() => document.documentElement.scrollWidth);
  ok(anchoDoc <= 390, 'sin scroll horizontal en móvil (ancho ' + anchoDoc + ')');
  await movil.screenshot({ path: SHOT + '09-movil.png' });

  await browser.close();
  console.log('Errores de consola:', errores.length ? errores : 'ninguno');
  if (errores.length) process.exit(1);
  console.log('E2E OK');
})().catch(e => { console.error(e); console.log('Errores de consola:', errores); process.exit(1); });
