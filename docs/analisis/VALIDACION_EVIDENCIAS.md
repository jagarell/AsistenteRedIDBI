# Dataset de evidencias de referencia y validación de fotos

Versión 2026-10-11 · Asistente Red IDBI

## 1. Para qué sirve

Cuando el técnico sube o toma una foto en el chat, la app la compara con las fotos de referencia del tipo pedido. Si no se parece, el asistente responde:

> **La imagen no corresponde a lo solicitado ({nombre}): se parece a {tipo detectado}. Vuelve a tomar o subir la foto, o salta la evidencia.**

En las fotos obligatorias (frontis, lugares de preparación, zonas, cajas) no hay "salta la evidencia": el mensaje termina en "Vuelve a subir la foto".

## 2. Contenido de la carpeta `dataset_evidencias/`

| Archivo | Qué es |
|---|---|
| `imagenes/<tipo>/` | Fotos de referencia sacadas de las minutas, clasificadas por tipo (43 en total, 2 contraejemplos) |
| `_imagenes.json` | Una fila por foto: tipo, minuta de origen, página, si se enmascaró |
| `catalogo_evidencias.json` | Por tipo: código en la app, código en la KB, qué debe mostrar, palabras para OCR, rasgos visuales, confusiones típicas, umbral |
| `evidencias_kb_por_tipo.csv` | Las 110 evidencias registradas en la base de conocimiento, cada una con su tipo |
| `imagenes/_contraejemplo/` | Fotos que **no** son evidencia válida (despensa, pared). Sirven para probar que la app las rechaza |

Las credenciales se ocultaron antes de guardar: credenciales y QR de la etiqueta del router, código de AnyDesk e IDs de dispositivo de Windows. Las fotos nuevas que se agreguen deben pasar por el mismo enmascarado (sección 5).

## 3. Tipos y su código en la app

| Tipo | Paso en la app | Código KB | Referencias |
|---|---|---|---|
| speedtest | E1 / E1b | E1 | 2 |
| ipconfig | E1c | E4 | 1 |
| escaner_ip | E1d | E8 | 1 |
| router_ubicacion | E2u | E2 | 4 |
| router_puertos | E2 | E2 | 1 |
| router_etiqueta | E2e | E3 | 1 |
| impresora_etiqueta | E5a | E5 | 2 |
| impresora_conexiones | E5b | E5 | 1 |
| impresora_selftest | E5 | E5 | 1 |
| frontis | E9a | E9 | 1 |
| zona_atencion | E9b (zona) | E9 | 5 |
| lugar_preparacion | E9b (preparación) | E9 | 2 |
| caja | E9b (caja) | E9 | 4 |
| anydesk | E7 | OTRA | 2 |
| equipo_caja_datos, tablet_datos, gabinete, cableado_energia, equipo_red_en_mano, access_point_techo, camaras_monitor | opcionales | E6 / OTRA | 1–3 |
| topologia | la genera la app | MAPA | 3 (solo referencia) |

**Límite:** solo 4 de las 44 minutas tienen PDF con imágenes disponibles (sicilia, donoscar, barrio, rock). Hay tipos con 1 sola referencia. Hay que sumar fotos validadas (sección 5) antes de usar umbrales estrictos.

## 4. Cómo valida la app (orden de las comprobaciones)

1. **Legible:** desenfoque (varianza del Laplaciano) y brillo. Si falla, el mensaje es "La imagen no es legible…".
2. **Capturas de pantalla** (speedtest, ipconfig, escaner_ip, anydesk, datos de equipo o tablet, selftest):
   - Se hace OCR y se cuenta cuántas `palabras_ocr` del tipo aparecen.
   - Con 2 o más coincidencias, corresponde.
   - Si las palabras coinciden más con otro tipo (por ejemplo, una sola IP en lugar de una lista), no corresponde y el mensaje dice a qué se parece.
3. **Fotos** (router, impresora, frontis, zonas, preparación, caja):
   - Se calcula el embedding de la imagen con un modelo de visión (CLIP/SigLIP en el backend).
   - Se compara por similitud coseno contra las referencias de **todos** los tipos.
   - Corresponde si el tipo pedido está entre los 2 más parecidos **y** supera `umbral_similitud`.
   - Si el más parecido es otro tipo o un contraejemplo, no corresponde, y `{tipo detectado}` es el nombre de ese tipo.
4. **Lectura de datos** (puede ir junto con el paso 3, con un modelo de visión con instrucciones):
   - Se piden los campos de READ_FIELDS: velocidades, IP, marca y modelo, SN, puertos, etc.
   - Si la foto corresponde pero no se leen los campos, se trata como no legible.
5. **Respuesta** con las plantillas `mensaje_no_corresponde` y `mensaje_no_legible` de `catalogo_evidencias.json`.

Si el servicio de visión no responde, la foto se acepta con la marca "sin validar" y queda pendiente en la revisión. No se debe bloquear al técnico en campo.

## 5. Cómo crece el dataset

- Cuando una minuta queda **validada**, sus fotos aceptadas se agregan como referencia del tipo correspondiente: `origen = minuta validada`.
- Las que el técnico subió y luego cambió porque no correspondían se agregan como contraejemplos de ese tipo.
- **Antes de guardar se enmascaran las credenciales:**
  - etiquetas de router: usuario, contraseña, SSID, clave WiFi y QR;
  - código de AnyDesk;
  - IDs de Windows.
  Las credenciales solo viven en su tabla cifrada.
- Se recalculan los embeddings y se revisan los umbrales con los contraejemplos. La meta es menos de 5 % de rechazos falsos medidos en las fotos validadas.

## 6. Cambios en la app (resumen para desarrollo)

- **Mensaje "no corresponde":**
  - Pasa de "parece una silla" (texto fijo) a "La imagen no corresponde a lo solicitado ({nombre}): se parece a {tipo detectado}".
  - En el simulador, `{tipo detectado}` es la confusión típica de cada paso.
- **Fotos obligatorias:** mismo mensaje, con el nombre del lugar (Frontis, Cocina, Caja 1…).
- **Backend:**
  - Servicio `POST /evidencias/validar` que recibe `{tipo, imagen}` y devuelve `{corresponde, legible, tipo_detectado, similitud, campos}`.
  - Tabla `kb_evidencia_referencia` con: tipo, ruta, embedding, origen, minuta_id, enmascarado, es_contraejemplo, fecha.
