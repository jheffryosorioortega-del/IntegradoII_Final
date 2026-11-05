"""Pruebas reales de validación y seguridad de sgp-microservicios (Gateway :8080, Front-End :8090).
Cada caso envía una petición real y compara lo obtenido con lo esperado."""
import json, os, re, sys, threading
import requests

GW = "http://localhost:8080"
FE = "http://localhost:8090"
PW = os.environ.get("SGP_PW", "")
# Clave compartida de la API (SGP_API_KEY de .env): el Gateway rechaza con 403 toda petición /api que no la traiga.
API_KEY = {"X-SGP-Api-Key": os.environ.get("SGP_API_KEY", "")}


def token_de(correo):
    """Inicia sesión por el Gateway (ruta pública) y devuelve el JWT que firma ms-usuarios."""
    r = requests.post(GW + "/api/usuarios/login", json={"correo": correo, "contrasena": PW}, headers=API_KEY, timeout=15)
    return r.json().get("token", "") if r.status_code == 200 else ""


# Las llamadas directas a la API (clientes, solicitudes) exigen la clave compartida Y un JWT con rol suficiente:
# se usa el del Administrador.
API = {**API_KEY, "Authorization": "Bearer " + token_de("admin@sgp.local")}
BROWSER = {"Accept": "text/html,*/*;q=0.8", "User-Agent": "Mozilla/5.0 (pruebas-SGP)"}
R = []


def reg(id_, desc, esperado, obtenido, ok):
    R.append(dict(id=id_, caso=desc, esperado=esperado, obtenido=obtenido, estado="Aprobado" if ok else "Rechazado"))
    print(("OK   " if ok else "FALLA"), id_, desc, "->", obtenido)


def csrf(html):
    m = re.search(r'name="_csrf"\s+value="([^"]+)"', html) or re.search(r'value="([^"]+)"\s+name="_csrf"', html)
    return m.group(1) if m else None


def cliente_ok(**kw):
    d = dict(nombre="Ana Torres", telefono="912345678", correo="", direccion="Av. Lima 123", distrito="Lima")
    d.update(kw)
    return d


def post_cli(**kw):
    return requests.post(GW + "/api/clientes", json=cliente_ok(**kw), headers=API, timeout=15)


def sol_ok(**kw):
    d = dict(nombre="Luis Gómez", telefono="923456789", correo="", direccion="Jr. Unión 456", distrito="Cercado",
             categoria="CISTERNAS", descripcion="Limpieza de cisterna de cinco metros cúbicos", urgencia="MEDIA")
    d.update(kw)
    return d


def post_sol(**kw):
    return requests.post(GW + "/api/solicitudes", json=sol_ok(**kw), headers=API, timeout=15)


def campos(r):
    try:
        return r.json().get("errores", {})
    except Exception:
        return {}


# ================= API: ms-clientes (vía Gateway) =================
casos = [
    ("V-01", "Nombre con números ('Juan123')", dict(nombre="Juan123"), "nombre"),
    ("V-02", "Nombre solo números ('12345')", dict(nombre="12345"), "nombre"),
    ("V-03", "Nombre con etiqueta HTML", dict(nombre="<script>alert(1)</script>"), "nombre"),
    ("V-04", "Nombre de 1 letra", dict(nombre="A"), "nombre"),
    ("V-05", "Nombre de 61 letras", dict(nombre="a" * 61), "nombre"),
    ("V-06", "Nombre con símbolos ('Ana@Torres')", dict(nombre="Ana@Torres"), "nombre"),
    ("V-07", "Teléfono con letras ('abcdefghi')", dict(telefono="abcdefghi"), "telefono"),
    ("V-08", "Teléfono mezcla letras y números ('98765abcd')", dict(telefono="98765abcd"), "telefono"),
    ("V-09", "Teléfono con signo + ('+51987654321')", dict(telefono="+51987654321"), "telefono"),
    ("V-10", "Teléfono con espacios ('987 654 321')", dict(telefono="987 654 321"), "telefono"),
    ("V-11", "Teléfono de 8 dígitos que empieza con 9", dict(telefono="98765432"), "telefono"),
    ("V-12", "Teléfono de 9 dígitos que empieza con 8", dict(telefono="887654321"), "telefono"),
    ("V-13", "Teléfono de 10 dígitos", dict(telefono="9876543210"), "telefono"),
    ("V-14", "Correo sin arroba", dict(correo="no-es-correo"), "correo"),
    ("V-15", "Dirección con etiqueta HTML", dict(direccion="<img src=x onerror=alert(1)>"), "direccion"),
    ("V-16", "Distrito con números ('Lima2')", dict(distrito="Lima2"), "distrito"),
]
for id_, desc, kw, campo in casos:
    r = post_cli(**kw)
    e = campos(r)
    reg(id_, f"API clientes: {desc}", f"HTTP 400 con error en «{campo}»", f"HTTP {r.status_code}; campos con error: {list(e.keys())}",
        r.status_code == 400 and campo in e)

# válidos que SÍ deben aceptarse
for id_, desc, kw in [("V-17", "Celular válido de 9 dígitos con nombre con tilde y ñ", dict(nombre="Ñusta Pérez-Rojas", telefono="965432101")),
                      ("V-18", "Teléfono fijo válido de 7 dígitos", dict(nombre="Carlos O'Brien", telefono="2345671")),
                      ("V-19", "Teléfono fijo válido de 8 dígitos", dict(nombre="María del Pilar", telefono="12345678"))]:
    r = post_cli(**kw)
    reg(id_, f"API clientes: {desc}", "HTTP 201 (aceptado)", f"HTTP {r.status_code}", r.status_code == 201)

r = post_cli(nombre="Duplicado Prueba", telefono="965432101")
reg("V-20", "API clientes: teléfono repetido", "HTTP 409", f"HTTP {r.status_code}", r.status_code == 409)

r = requests.post(GW + "/api/clientes", data="no es json", headers={"Content-Type": "application/json", **API}, timeout=15)
reg("V-21", "API clientes: cuerpo que no es JSON", "HTTP 400 (no 500)", f"HTTP {r.status_code}", r.status_code == 400)

# ================= API: ms-solicitudes (vía Gateway) =================
r = post_sol()
cod = r.json().get("codigo") if r.status_code == 201 else None
reg("V-22", "API solicitudes: solicitud válida", "HTTP 201 con código SOL-xxxxxx", f"HTTP {r.status_code}; código: {cod}",
    r.status_code == 201 and bool(cod) and re.fullmatch(r"SOL-\d{6}", cod) is not None)

for id_, desc, kw, campo in [
    ("V-23", "nombre con números", dict(nombre="Pedro 99", telefono="934567890"), "nombre"),
    ("V-24", "teléfono con letras", dict(telefono="abc123456"), "telefono"),
    ("V-25", "descripción demasiado corta", dict(descripcion="corto"), "descripcion"),
    ("V-26", "descripción con < >", dict(descripcion="<script>alert(document.cookie)</script> por favor"), "descripcion"),
    ("V-27", "dirección con símbolos peligrosos", dict(direccion="Av. X'; DROP TABLE solicitud;--"), "direccion"),
]:
    r = post_sol(**kw)
    e = campos(r)
    reg(id_, f"API solicitudes: {desc}", f"HTTP 400 con error en «{campo}»", f"HTTP {r.status_code}; campos con error: {list(e.keys())}",
        r.status_code == 400 and campo in e)

r = requests.post(GW + "/api/solicitudes", json=sol_ok(categoria="NO_EXISTE"), headers=API, timeout=15)
reg("V-28", "API solicitudes: categoría que no existe", "HTTP 400 (no 500)", f"HTTP {r.status_code}", r.status_code == 400)

# concurrencia: 10 solicitudes a la vez con el MISMO teléfono nuevo -> un solo cliente, ningún error 5xx
tel = "945612378"
resultados = []
def lanzar():
    resultados.append(post_sol(nombre="Concurrente Prueba", telefono=tel).status_code)
hilos = [threading.Thread(target=lanzar) for _ in range(10)]
[h.start() for h in hilos]; [h.join() for h in hilos]
lista = requests.get(GW + "/api/clientes", headers=API, timeout=15).json()
n_cli = sum(1 for c in lista if c["telefono"] == tel)
reg("V-29", "10 solicitudes simultáneas con el mismo teléfono nuevo", "Todas HTTP 201 y un solo cliente creado",
    f"códigos: {sorted(set(resultados))} ({len(resultados)} respuestas); clientes con ese teléfono: {n_cli}",
    all(c == 201 for c in resultados) and len(resultados) == 10 and n_cli == 1)

# ================= Front-End: formularios =================
def form_contacto(**kw):
    s = requests.Session(); s.headers.update(BROWSER)
    h = s.get(FE + "/contacto")
    d = dict(nombre="Rosa Medina", telefono="956781234", correo="", direccion="Calle Los Pinos 210", distrito="San Borja",
             categoria="BOMBAS", descripcion="La bomba de agua no presuriza y hace ruido", urgencia="ALTA", _csrf=csrf(h.text))
    d.update(kw)
    return s, s.post(FE + "/contacto", data=d, allow_redirects=False)

n_antes = len(requests.get(GW + "/api/solicitudes", headers=API, timeout=15).json())
s, r = form_contacto(nombre="Pedro 123")
n_desp = len(requests.get(GW + "/api/solicitudes", headers=API, timeout=15).json())
reg("V-30", "Pantalla Solicitar servicio: nombre con números", "Se queda en el formulario con el error y no crea nada",
    f"HTTP {r.status_code}; mensaje del nombre visible: {'solo admite letras' in r.text}; marcado is-invalid: {'is-invalid' in r.text}; solicitudes {n_antes}→{n_desp}",
    r.status_code == 200 and "solo admite letras" in r.text and "is-invalid" in r.text and n_antes == n_desp)

s, r = form_contacto(telefono="abcdefghi")
reg("V-31", "Pantalla Solicitar servicio: teléfono con letras", "Error de teléfono visible", f"HTTP {r.status_code}; mensaje visible: {'escribe solo números' in r.text}",
    r.status_code == 200 and "escribe solo números" in r.text)

s, r = form_contacto()
fin = s.get(FE + "/contacto/enviado")
m = re.search(r"SOL-\d{6}", fin.text)
reg("V-32", "Pantalla Solicitar servicio: datos válidos", "Redirige a la confirmación con código", f"HTTP {r.status_code}; código: {m.group(0) if m else None}",
    r.status_code == 302 and bool(m))

def sesion(correo):
    s = requests.Session(); s.headers.update(BROWSER)
    h = s.get(FE + "/login")
    r = s.post(FE + "/login", data={"username": correo, "password": PW, "_csrf": csrf(h.text)}, allow_redirects=False)
    return s, r

sc, rc = sesion("coordinador@sgp.local")
reg("V-33", "Login del Coordinador (ms-usuarios con su base de datos)", "Redirige a /panel", f"HTTP {rc.status_code} → {rc.headers.get('Location','').replace(FE,'')}",
    rc.status_code == 302 and rc.headers.get("Location", "").endswith("/panel"))

s0 = requests.Session(); s0.headers.update(BROWSER)
h = s0.get(FE + "/login")
r0 = s0.post(FE + "/login", data={"username": "coordinador@sgp.local", "password": "incorrecta-123", "_csrf": csrf(h.text)}, allow_redirects=False)
reg("V-34", "Login con contraseña incorrecta", "Redirige a /login?error", f"HTTP {r0.status_code} → {r0.headers.get('Location','').replace(FE,'')}",
    r0.status_code == 302 and r0.headers.get("Location", "").endswith("/login?error"))

def form_cliente(**kw):
    t = csrf(sc.get(FE + "/panel/clientes/nuevo").text)
    d = dict(nombre="Hugo Salas", telefono="978123456", correo="", direccion="Jr. Cusco 55", distrito="Breña", _csrf=t)
    d.update(kw)
    return sc.post(FE + "/panel/clientes", data=d, allow_redirects=False)

r = form_cliente(nombre="Hugo 55")
reg("V-35", "Panel Nuevo cliente: nombre con números", "Error del nombre", f"HTTP {r.status_code}; mensaje visible: {'solo admite letras' in r.text}", r.status_code == 200 and "solo admite letras" in r.text)
r = form_cliente(telefono="12ab34cd5")
reg("V-36", "Panel Nuevo cliente: teléfono con letras", "Error del teléfono", f"HTTP {r.status_code}; mensaje visible: {'escribe solo números' in r.text}", r.status_code == 200 and "escribe solo números" in r.text)
r = form_cliente()
reg("V-37", "Panel Nuevo cliente: datos válidos", "Guarda y redirige al listado", f"HTTP {r.status_code} → {r.headers.get('Location','').replace(FE,'')}",
    r.status_code == 302 and r.headers.get("Location", "").endswith("/panel/clientes"))
r = form_cliente(nombre="Otro Nombre")
reg("V-38", "Panel Nuevo cliente: teléfono repetido", "Aviso de duplicado", f"HTTP {r.status_code}; aviso visible: {'Ya existe un cliente' in r.text}", r.status_code == 200 and "Ya existe un cliente" in r.text)

# ================= Seguridad del Front-End =================
h = requests.get(FE + "/", headers=BROWSER)
csp = h.headers.get("Content-Security-Policy", "")
reg("V-39", "Cabeceras de seguridad en las respuestas", "CSP, X-Frame-Options, nosniff y Referrer-Policy presentes",
    f"CSP: {'sí' if 'default-src' in csp else 'no'}; X-Frame-Options: {h.headers.get('X-Frame-Options')}; nosniff: {h.headers.get('X-Content-Type-Options')}; Referrer-Policy: {h.headers.get('Referrer-Policy')}",
    "script-src 'self'" in csp and h.headers.get("X-Frame-Options") == "DENY" and h.headers.get("X-Content-Type-Options") == "nosniff" and bool(h.headers.get("Referrer-Policy")))

ck = [c for c in rc.headers.get("Set-Cookie", "").split(",")] if False else None
raw = rc.raw.headers.get_all("Set-Cookie") if hasattr(rc.raw.headers, "get_all") else []
cook = " | ".join(raw)
reg("V-40", "Cookie de sesión protegida", "HttpOnly y SameSite=Lax", f"Set-Cookie: {cook[:160]}",
    "HttpOnly" in cook and "SameSite=Lax" in cook)

for id_, ruta in [("V-41", "/webjars/bootstrap/5.3.3/css/bootstrap.min.css"), ("V-42", "/webjars/bootstrap/5.3.3/js/bootstrap.bundle.min.js"),
                  ("V-43", "/webjars/bootstrap-icons/1.11.3/font/bootstrap-icons.min.css"),
                  ("V-44", "/webjars/bootstrap-icons/1.11.3/font/fonts/bootstrap-icons.woff2"),
                  ("V-45", "/css/site.css"), ("V-46", "/js/validacion.js")]:
    r = requests.get(FE + ruta, timeout=15)
    reg(id_, f"Recurso estático {ruta.split('/')[-1]}", "HTTP 200", f"HTTP {r.status_code}; {len(r.content)} bytes", r.status_code == 200 and len(r.content) > 500)

st, rt = sesion("tecnico@sgp.local")
r = st.get(FE + "/panel/clientes")
reg("V-47", "Técnico abre Clientes: página de error amigable", "HTTP 403 con mensaje claro (no la página técnica de Spring)",
    f"HTTP {r.status_code}; mensaje amigable: {'No tienes permiso' in r.text}; página técnica 'Whitelabel': {'Whitelabel' in r.text}",
    r.status_code == 403 and "No tienes permiso" in r.text and "Whitelabel" not in r.text)

r = requests.get(FE + "/ruta-que-no-existe", headers=BROWSER)
reg("V-48", "Ruta inexistente: página de error amigable", "HTTP 404 con mensaje claro", f"HTTP {r.status_code}; mensaje amigable: {'No encontramos' in r.text}",
    r.status_code == 404 and "No encontramos" in r.text)

# ================= Atributos HTML de los campos (primera barrera en el navegador) =================
def campo(html, id_):
    m = re.search(r'<(?:input|textarea|select)[^>]*\bid="%s"[^>]*>' % id_, html)
    return m.group(0) if m else ""

pag = requests.get(FE + "/contacto", headers=BROWSER).text
tel, nom = campo(pag, "telefono"), campo(pag, "nombre")
reg("V-49", "Formulario público: campo Teléfono con límites del navegador", "inputmode numérico, maxlength 9, pattern y filtro de teclas",
    f"inputmode=numeric: {'inputmode=\"numeric\"' in tel}; maxlength=9: {'maxlength=\"9\"' in tel}; pattern: {'pattern=' in tel}; data-filtro=telefono: {'data-filtro=\"telefono\"' in tel}",
    'inputmode="numeric"' in tel and 'maxlength="9"' in tel and "pattern=" in tel and 'data-filtro="telefono"' in tel)
reg("V-50", "Formulario público: campo Nombre con límites del navegador", "pattern, maxlength 60 y filtro de teclas",
    f"pattern: {'pattern=' in nom}; maxlength=60: {'maxlength=\"60\"' in nom}; data-filtro=nombre: {'data-filtro=\"nombre\"' in nom}",
    "pattern=" in nom and 'maxlength="60"' in nom and 'data-filtro="nombre"' in nom)
pag2 = sc.get(FE + "/panel/clientes/nuevo").text
tel2, nom2 = campo(pag2, "telefono"), campo(pag2, "nombre")
reg("V-51", "Panel Nuevo cliente: campos Nombre y Teléfono con límites del navegador", "Mismos límites que el formulario público",
    f"teléfono con inputmode/maxlength/filtro: {'inputmode=\"numeric\"' in tel2 and 'maxlength=\"9\"' in tel2 and 'data-filtro=\"telefono\"' in tel2}; nombre con filtro: {'data-filtro=\"nombre\"' in nom2}",
    'inputmode="numeric"' in tel2 and 'maxlength="9"' in tel2 and 'data-filtro="telefono"' in tel2 and 'data-filtro="nombre"' in nom2)

# ================= Clave compartida de la API (Gateway y microservicios) =================
MALA = {"X-SGP-Api-Key": "clave-incorrecta"}
r1 = requests.get(GW + "/api/clientes", timeout=15)
r2 = requests.get(GW + "/api/solicitudes", timeout=15)
r3 = requests.put(GW + "/api/solicitudes/1/estado?estado=CERRADA", timeout=15)
reg("V-52", "Gateway sin clave: listar clientes, listar solicitudes y cambiar estado", "HTTP 403 en las tres",
    f"{r1.status_code} / {r2.status_code} / {r3.status_code}", {r1.status_code, r2.status_code, r3.status_code} == {403})
r = requests.get(GW + "/api/clientes", headers=MALA, timeout=15)
reg("V-53", "Gateway con clave incorrecta", "HTTP 403", f"HTTP {r.status_code}", r.status_code == 403)
directos = {p: requests.get(f"http://localhost:{p}{ruta}", timeout=15).status_code
            for p, ruta in [(8081, "/api/usuarios/login"), (8082, "/api/clientes"), (8083, "/api/solicitudes")]}
reg("V-54", "Microservicios llamados directo (sin pasar por el Gateway) y sin clave", "HTTP 403 en 8081, 8082 y 8083",
    str(directos), all(v == 403 for v in directos.values()))
r = requests.get(GW + "/api/clientes", headers=API, timeout=15)
reg("V-55", "Gateway con la clave correcta", "HTTP 200", f"HTTP {r.status_code}", r.status_code == 200)

# ================= JWT: identidad y roles en la API (Gateway) =================
TEC = {**API_KEY, "Authorization": "Bearer " + token_de("tecnico@sgp.local")}
SIN = API_KEY
r1 = requests.get(GW + "/api/solicitudes", headers=SIN, timeout=15)
r2 = requests.get(GW + "/api/clientes", headers=SIN, timeout=15)
r3 = requests.put(GW + "/api/solicitudes/1/estado?estado=CERRADA", headers=SIN, timeout=15)
reg("V-56", "Con la clave de API pero SIN token: solicitudes, clientes y cambio de estado", "HTTP 401 en las tres",
    f"{r1.status_code} / {r2.status_code} / {r3.status_code}", {r1.status_code, r2.status_code, r3.status_code} == {401})
r1 = requests.get(GW + "/api/clientes", headers=TEC, timeout=15)
r2 = requests.put(GW + "/api/solicitudes/1/estado?estado=CERRADA", headers=TEC, timeout=15)
r3 = requests.get(GW + "/api/solicitudes", headers=TEC, timeout=15)
reg("V-57", "Técnico: no gestiona clientes ni cambia estados, pero sí ve solicitudes", "403 / 403 / 200",
    f"{r1.status_code} / {r2.status_code} / {r3.status_code}", (r1.status_code, r2.status_code, r3.status_code) == (403, 403, 200))
partes = TEC["Authorization"].split(" ")[1].split(".")
import base64
cuerpo = json.loads(base64.urlsafe_b64decode(partes[1] + "=" * (-len(partes[1]) % 4)))
cuerpo["rol"] = "ADMINISTRADOR"
falso = partes[0] + "." + base64.urlsafe_b64encode(json.dumps(cuerpo).encode()).decode().rstrip("=") + "." + partes[2]
r = requests.get(GW + "/api/clientes", headers={**API_KEY, "Authorization": "Bearer " + falso}, timeout=15)
reg("V-58", "Token alterado (Técnico se pone rol ADMINISTRADOR sin conocer el secreto)", "HTTP 401", f"HTTP {r.status_code}", r.status_code == 401)
r = requests.get(GW + "/api/clientes", headers={"Authorization": API["Authorization"]}, timeout=15)
reg("V-59", "Token de Administrador válido pero SIN la clave de API", "HTTP 403", f"HTTP {r.status_code}", r.status_code == 403)
r = requests.post(GW + "/api/solicitudes", json={}, headers=SIN, timeout=15)
reg("V-60", "Ruta pública (formulario del portal) sin token: llega al servicio", "HTTP 400 por datos vacíos (no 401 ni 403)",
    f"HTTP {r.status_code}", r.status_code == 400)

json.dump(R, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "resultados_ms.json"), "w", encoding="utf8"), ensure_ascii=False, indent=1)
print("\nTOTAL", len(R), "| Aprobados", sum(1 for x in R if x["estado"] == "Aprobado"), "| Rechazados", sum(1 for x in R if x["estado"] != "Aprobado"))
