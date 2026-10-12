"""Referencia del puntaje de Conectividad (reglas de negocio IDPos, 2026-10-08 y 2026-10-10).

Entradas (del chat de 76 nodos):
  conexion         P14 (FIBRA, COAXIAL, CHIP, NO_TIENE, NO_SE)
  plan_mbps        P15 velocidad contratada
  areas            P11 áreas del local
  areas_sin_wifi   P46 zonas sin señal WiFi
  hay_camaras      P47 = Sí  o  el escáner E8 muestra cámaras

Confirmado por el equipo:
  - Sin internet (P14 = No tiene o No sé) → 0 %.
  - Tramos: hasta 399 → 30 · 400–699 → 60 · 700–899 → 80 · 900 o más → 100.
  - Si falta WiFi en alguna área de atención → −10 puntos UNA sola vez + OBSERVADO.
  - Se necesitan 200 Mbps por salón; si el plan no alcanza → OBSERVADO + subir el plan.
  - Cámaras → sugerir 1000 Mbps y separar la red de cámaras.
Supuestos (por confirmar):
  - La Terraza cuenta como salón.
  - Planes menores a 200 Mbps también valen 30 %.
  - El caso "200–300 Mbps con un salón por repetir" queda en 20 % (30 − 10), ya no en 10 %.
"""
from typing import Dict, List

SALONES = {"SALON_1", "SALON_2", "SALON_3", "TERRAZA"}          # Terraza: supuesto
AREAS_ATENCION = SALONES | {"CAJA"}
TRAMOS = [(0, 399, 30), (400, 699, 60), (700, 899, 80), (900, 10**6, 100)]
MBPS_POR_SALON = 200
PLAN_MIN_CON_REPETIDOR = 500
PLAN_CON_CAMARAS = 1000


def puntaje_plan(plan_mbps: int) -> int:
    for desde, hasta, pts in TRAMOS:
        if desde <= plan_mbps <= hasta:
            return pts
    return 0


def conectividad(conexion: str, plan_mbps: int, areas: List[str], areas_sin_wifi: List[str], hay_camaras: bool) -> Dict:
    if conexion in ("NO_TIENE", "NO_SE"):
        return {"conectividad": 0, "estado": "OBSERVADO",
                "recomendaciones": ["Sin internet: hacer la prueba de chip y, si hay comandas, router con chip."]}

    score = puntaje_plan(plan_mbps)
    observado, recs = False, []
    salones = [a for a in areas if a in SALONES] or ["SALON_1"]
    sin_wifi = [a for a in areas_sin_wifi if a in AREAS_ATENCION]
    requerido = MBPS_POR_SALON * len(salones)

    if sin_wifi:
        score, observado = max(score - 10, 0), True
        recs.append("Implementar repetidor, mesh o access point en: " + ", ".join(sin_wifi) + ".")
    if plan_mbps < requerido:
        observado = True
        objetivo = max(requerido, PLAN_MIN_CON_REPETIDOR if sin_wifi else requerido)
        recs.append(f"Subir el plan a {objetivo} Mbps o más ({MBPS_POR_SALON} Mbps por salón × {len(salones)}).")
    if hay_camaras:
        if plan_mbps < PLAN_CON_CAMARAS:
            observado = True
            recs.append("Contratar un plan de 1000 Mbps.")
        recs.append("Separar la red de cámaras de la red de PC, sistema de impresiones e impresoras de red.")

    return {"conectividad": score, "estado": "OBSERVADO" if observado else "Según demás reglas", "recomendaciones": recs}


if __name__ == "__main__":
    casos = [
        ("Sin internet", "NO_TIENE", 0, ["SALON_1", "CAJA"], [], False),
        ("1 salón con caja, WiFi en todo", "FIBRA", 200, ["SALON_1", "CAJA"], [], False),
        ("2 salones, Salón 2 sin WiFi", "FIBRA", 1000, ["SALON_1", "SALON_2", "CAJA"], ["SALON_2"], False),
        ("2 salones, Salón 2 sin WiFi", "FIBRA", 300, ["SALON_1", "SALON_2", "CAJA"], ["SALON_2"], False),
        ("2 salones + terraza, uno sin WiFi", "FIBRA", 600, ["SALON_1", "SALON_2", "TERRAZA", "CAJA"], ["TERRAZA"], False),
        ("2 salones + terraza, WiFi en todo", "FIBRA", 400, ["SALON_1", "SALON_2", "TERRAZA", "CAJA"], [], False),
        ("2 salones, Salón 2 sin WiFi", "FIBRA", 800, ["SALON_1", "SALON_2", "CAJA"], ["SALON_2"], False),
        ("1 salón, WiFi en todo, con cámaras", "FIBRA", 600, ["SALON_1", "CAJA"], [], True),
    ]
    for nombre, con, plan, areas, sin, cam in casos:
        r = conectividad(con, plan, areas, sin, cam)
        print(f"{nombre:36} {plan:5} Mbps -> {r['conectividad']:3} %  {r['estado']:18} | " + " ".join(r["recomendaciones"]))
