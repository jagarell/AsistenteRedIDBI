# Evidencias flexibles: «toma la foto y di qué es»

## 1. Flujo
1. El técnico toma o sube una foto en cualquier momento del Chat IA (o en el Asistente IA).
2. La IA propone a qué pertenece: «¿Es el selftest de IMP. Cocina?». El técnico confirma, elige otra opción o escribe/dicta su etiqueta («AnyDesk de la PC del tomador de pedidos 1», «punto de red de la pared»).
3. La IA lee todo lo que pueda de la imagen y lo muestra en «Esto leí en la foto» para confirmar:
   - selftest: marca, modelo, SN, IP, MAC, máscara y puerta de enlace;
   - AnyDesk: código;
   - router: marca, modelo y puertos libres/ocupados;
   - escáner de IP: equipos, IP y MAC;
   - switch: marca, modelo, número de puertos y si es administrable.
4. Con la etiqueta, la foto va sola a su grupo del anexo y se recorta a los datos clave (ver API `03-minuta-pdf-y-anexo.md`).

## 2. Evidencias obligatorias (regla nueva)
| Equipo | Obligatorio | Opcional |
|---|---|---|
| Impresora | **Selftest** | Etiqueta y conexiones (se piden solo si el selftest no se lee o la impresora es USB) |
| Router | **1 foto** con etiqueta y puertos | Ubicación |
| Equipo con el sistema de impresiones | **AnyDesk** | — |
| Otros equipos de caja | — | AnyDesk |
| Red | Speedtest, ipconfig, escáner de IP | — |
| Local | Frontis | Ambientes |

Lo que no es obligatorio queda a elección del técnico; todo lo que suba se usa.

## 3. Criterios de aceptación
- Dada una impresora con selftest legible, cuando el técnico no sube etiqueta ni conexiones, entonces la evaluación continúa sin pendientes de esa impresora.
- Dado un selftest ilegible, entonces el Chat IA pide la foto de la etiqueta.
- Dada una foto etiquetada «AnyDesk de la PC del tomador de pedidos 1», entonces aparece en el grupo AnyDesk del anexo con el código leído.
