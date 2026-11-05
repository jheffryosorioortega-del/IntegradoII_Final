import os, re, requests
FE = "http://localhost:8090"
PW = os.environ["SGP_PW"]
H = {"Accept": "text/html,*/*;q=0.8", "User-Agent": "Mozilla/5.0 (pruebas-SGP)"}

def csrf(t):
    m = re.search(r'name="_csrf"\s+value="([^"]+)"', t) or re.search(r'value="([^"]+)"\s+name="_csrf"', t)
    return m.group(1) if m else None

s = requests.Session(); s.headers.update(H)
h = s.get(FE + "/login")
s.post(FE + "/login", data={"username": "coordinador@sgp.local", "password": PW, "_csrf": csrf(h.text)}, allow_redirects=False)

r = s.get(FE + "/panel/clientes")
print("Panel Clientes con ms-clientes caído   -> HTTP", r.status_code, "| aviso amigable:", "Servicio no disponible" in r.text, "| traza técnica:", "Exception" in r.text)

r = s.get(FE + "/panel")   # el tablero usa ms-solicitudes (sigue vivo)
print("Tablero (usa ms-solicitudes, vivo)      -> HTTP", r.status_code)

p = requests.Session(); p.headers.update(H)
hh = p.get(FE + "/contacto")
r = p.post(FE + "/contacto", data=dict(nombre="Prueba Caida", telefono="911222333", correo="", direccion="Av. Prueba 123", distrito="Lima",
           categoria="BOMBAS", descripcion="Probando con el servicio de clientes apagado", urgencia="MEDIA", _csrf=csrf(hh.text)), allow_redirects=False)
print("Enviar solicitud con ms-clientes caído  -> HTTP", r.status_code, "| aviso amigable:", "Servicio no disponible" in r.text)

r = requests.get(FE + "/", headers=H)
print("Portal público (no depende de clientes) -> HTTP", r.status_code)
