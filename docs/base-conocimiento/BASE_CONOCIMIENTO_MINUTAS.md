# Base de conocimiento de minutas manuales

Última actualización: 2026-10-10 (18 minutas cargadas). El cambio al flujo de la sección 4b **ya está implementado y desplegado**; lo que sigue propuesto son las tablas `kb_*`, el importador y la fase 2 (recomendaciones desde la base).

Este documento explica cómo pasar a la base de datos las **minutas técnicas hechas a mano (PDF)**. El objetivo es que el motor de recomendaciones aprenda de visitas reales.

La entrada es el Excel [`Base_Conocimiento_Minutas.xlsx`](Base_Conocimiento_Minutas.xlsx). Ya viene cargado con:
- 18 minutas manuales (MH-001 a MH-018), de 2023 a 2026:
  - Sicilia, Don Oscar, Barrio Pesquero y Rock & Burgers.
  - Dorcher San Miguel (dos visitas), Malala y Hotel La Confianza.
  - Siete sedes de Rikoton (Banchero, Bayóvar, Calle 8, Hacienda, Mariscal, Villa Flores y Zárate).
  - Tres sedes de Gelato Alore (Miguel Dasso, Ejército y 2 de Mayo).
- 52 reglas de recomendación y 13 acciones estándar.
- Las tablas de decisión de impresión e internet y el catálogo de áreas de preparación (sección 4b).
- 10 reglas de validación.
- 11 equipos recomendables.
- 15 umbrales.

---

## ⛔ Regla de oro: nada a producción

> **No se sube nada a producción.** La versión de la app que está en uso (APK 0.0.3 y backends desplegados en Railway) no debe cambiar.

- **No hacer `push` a `main`** en ninguno de los 3 repos. En Railway, cada push a `main` despliega solo.
- Trabajar en una rama local (`feature/base-conocimiento-minutas`). No abrir ni mergear PRs a `main` sin autorización explícita del equipo.
- **No conectarse a la base de producción.** La carga se hace solo en Postgres local o en H2 (cómo levantarlos: `notas-de-desarrollo/project_flow_engine_76_nodos.md`).
- No cambiar variables de entorno ni configuración en Railway.
- No generar un APK nuevo ni cambiar `BASE_URL`.

Por qué importa:
- El gateway usa `ddl-auto=update`. Si un cambio con entidades nuevas llega a producción, Hibernate crea las tablas en la base real.
- Además, la base de producción tiene restricciones en los enums que `update` no migra (ver `project_estado_apk_0_0_2.md`).

---

## 1. Qué hay en el Excel

| Hoja | Tabla destino | Qué contiene |
|---|---|---|
| `LEEME` | — | Instrucciones de llenado |
| `resumen` | — | Conteos con fórmulas. Incluye **recomendaciones sin regla vinculada**, que debe estar en 0 antes de cargar |
| `kb_minuta` | `kb_minuta` | Una fila por minuta: cliente, local, dirección, fecha, motivo, negocio, internet, estado, asistentes, archivo PDF |
| `kb_minuta_respuesta` | `kb_minuta_respuesta` | Lo que la minuta responde de las preguntas del flujo (P01…P54, U1…U8), con alcance y confianza (EXPLICITO / INFERIDO) |
| `kb_minuta_hallazgo` | `kb_minuta_hallazgo` | Antecedentes, desarrollo y observaciones, vinculados a pregunta y regla |
| `kb_minuta_recomendacion` | `kb_minuta_recomendacion` | Recomendaciones y siguientes acciones tal como están en el PDF, cada una con su `regla_id` |
| `kb_minuta_equipo` | `kb_minuta_equipo` | Equipos vistos (router, PC, impresoras, repetidores…) con marca, modelo, IP, conexión y área |
| `kb_minuta_evidencia` | `kb_minuta_evidencia` | Referencia a fotos, mapas y videos de la minuta. Solo referencia: no se suben archivos |
| `kb_regla_recomendacion` | `kb_regla_recomendacion` | Catálogo de reglas: disparador, condición JSON, hallazgo AS-IS, recomendación TO-BE, responsable, prioridad, equipo y fuente. Tiene columnas calculadas de cuántas minutas la respaldan |
| `kb_regla_validacion` | `kb_regla_validacion` | Reglas V01–V10 de `app/chat/checks.py` (referencia) |
| `kb_equipo` | `kb_equipo` | Equipos recomendables |
| `kb_umbral` | `kb_umbral` | Umbrales hoy fijos en `proposal.py` y `checks.py` |
| `kb_decision_impresion` | `kb_decision_impresion` | Tabla de decisión de impresión IDPos (sección 4b) |
| `kb_decision_internet` | `kb_decision_internet` | Tabla de decisión de internet (fibra/coaxial, comandas, señal de chip) |
| `kb_area_preparacion` | `kb_area_preparacion` | Qué áreas son de preparación (definen si hay comandas) |
| `kb_requisito_equipo` | `kb_requisito_equipo` | Qué necesita cerca cada equipo (punto de red y/o energía) |
| `kb_texto_minuta` | `kb_texto_minuta` | Textos fijos de la minuta (aviso de viabilidad) |
| `revisar_vs_regla` | — (no se carga) | Minutas cuya recomendación no coincide con la regla de impresión |
| `ref_preguntas` | — (no se carga) | Preguntas del flujo, para elegir `pregunta_id` |
| `catalogos` | — (no se carga) | Valores válidos de las listas desplegables. Son los mismos códigos del flujo |
| `diccionario` | — | Columnas, tipos y claves |

Convenciones del Excel:
- **Amarillo:** filas vacías para llenar.
- **Gris:** fórmula. No se escribe ni se carga.
- **Encabezado azul:** nombre de columna en la base.

## 2. Modelo de datos propuesto (Postgres)

Tablas nuevas con prefijo `kb_`, **separadas de `minutas`**. La tabla `minutas` es de la app: está ligada a evaluaciones y técnicos y tiene estados con restricción.

```sql
CREATE TABLE kb_equipo (
  equipo_id      VARCHAR(20) PRIMARY KEY,
  categoria      VARCHAR(30) NOT NULL,
  nombre         VARCHAR(120) NOT NULL,
  descripcion    TEXT,
  ejemplos_en_minutas TEXT
);

CREATE TABLE kb_regla_recomendacion (
  regla_id       VARCHAR(10) PRIMARY KEY,          -- R01…, A01…
  categoria      VARCHAR(40) NOT NULL,
  nombre         VARCHAR(150) NOT NULL,
  disparador     TEXT,
  condicion_json JSONB,                             -- misma sintaxis que el flujo ($P16, eq, in, or…)
  hallazgo_as_is TEXT,
  recomendacion_to_be TEXT NOT NULL,
  responsable    VARCHAR(10) NOT NULL,              -- ID | Cliente
  prioridad      VARCHAR(10) NOT NULL,              -- Alta | Media | Baja
  equipo_id      VARCHAR(20) REFERENCES kb_equipo(equipo_id),
  fuente         TEXT,
  activa         BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE kb_regla_validacion (
  validacion_id  VARCHAR(10) PRIMARY KEY,
  regla          TEXT NOT NULL,
  evidencias     VARCHAR(40),
  condicion      TEXT,
  estados        VARCHAR(80),
  texto_corto    TEXT
);

CREATE TABLE kb_umbral (
  clave          VARCHAR(60) PRIMARY KEY,
  valor          NUMERIC(12,3) NOT NULL,
  unidad         VARCHAR(20),
  descripcion    TEXT,
  origen         VARCHAR(80)
);

CREATE TABLE kb_minuta (
  minuta_id      VARCHAR(20) PRIMARY KEY,           -- MH-001…
  nombre_cliente VARCHAR(150) NOT NULL,
  nombre_local   VARCHAR(150),
  direccion      VARCHAR(255),
  distrito       VARCHAR(80),
  fecha_visita   DATE NOT NULL,
  hora           TIME,
  motivo_visita  VARCHAR(30),
  motivo_detalle TEXT,
  tipo_negocio   VARCHAR(30),
  pisos          INT,
  cajas          INT,
  proveedor_internet VARCHAR(30),
  tipo_conexion  VARCHAR(30),
  velocidad_contratada_mbps NUMERIC(8,2),
  bajada_mbps    NUMERIC(8,2),
  subida_mbps    NUMERIC(8,2),
  segmento_red   VARCHAR(40),
  estado_final   VARCHAR(20),                       -- APTO | OBSERVADO | NO_APTO | SIN_DATO
  asistentes_id  TEXT,
  contacto_cliente VARCHAR(150),
  requiere_segunda_visita BOOLEAN,
  archivo_pdf    VARCHAR(255),
  observaciones_carga TEXT,
  origen         VARCHAR(20) NOT NULL DEFAULT 'MANUAL_PDF',
  cargado_en     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE kb_minuta_respuesta (
  id BIGSERIAL PRIMARY KEY,
  minuta_id   VARCHAR(20) NOT NULL REFERENCES kb_minuta(minuta_id) ON DELETE CASCADE,
  pregunta_id VARCHAR(10) NOT NULL,
  alcance     VARCHAR(40) NOT NULL DEFAULT '',
  valor       TEXT NOT NULL,
  confianza   VARCHAR(10) NOT NULL,                 -- EXPLICITO | INFERIDO
  fuente_en_minuta TEXT
);

CREATE TABLE kb_minuta_hallazgo (
  id BIGSERIAL PRIMARY KEY,
  minuta_id   VARCHAR(20) NOT NULL REFERENCES kb_minuta(minuta_id) ON DELETE CASCADE,
  orden       INT NOT NULL,
  seccion     VARCHAR(20) NOT NULL,                 -- ANTECEDENTE | DESARROLLO | OBSERVACION
  texto       TEXT NOT NULL,
  pregunta_id VARCHAR(10),
  regla_id    VARCHAR(10) REFERENCES kb_regla_recomendacion(regla_id)
);

CREATE TABLE kb_minuta_recomendacion (
  id BIGSERIAL PRIMARY KEY,
  minuta_id   VARCHAR(20) NOT NULL REFERENCES kb_minuta(minuta_id) ON DELETE CASCADE,
  orden       INT NOT NULL,
  tipo        VARCHAR(20) NOT NULL,                 -- RECOMENDACION | ACCION_ID | ACCION_CLIENTE
  regla_id    VARCHAR(10) NOT NULL REFERENCES kb_regla_recomendacion(regla_id),
  texto_original TEXT NOT NULL
);

CREATE TABLE kb_minuta_equipo (
  id BIGSERIAL PRIMARY KEY,
  minuta_id   VARCHAR(20) NOT NULL REFERENCES kb_minuta(minuta_id) ON DELETE CASCADE,
  tipo_equipo VARCHAR(20) NOT NULL,
  marca VARCHAR(60), modelo VARCHAR(80), ip VARCHAR(45),
  conexion VARCHAR(20), area VARCHAR(30), cantidad INT, nota TEXT
);

CREATE TABLE kb_minuta_evidencia (
  id BIGSERIAL PRIMARY KEY,
  minuta_id   VARCHAR(20) NOT NULL REFERENCES kb_minuta(minuta_id) ON DELETE CASCADE,
  codigo_evidencia VARCHAR(10) NOT NULL,            -- E1…E9, EU-*, MAPA, VIDEO, OTRA
  descripcion TEXT, archivo_o_pagina VARCHAR(120), nota TEXT
);

CREATE TABLE kb_decision_impresion (
  caso_id              VARCHAR(5) PRIMARY KEY,      -- D1…D6
  necesita_comandas    BOOLEAN NOT NULL,
  equipo_caja          VARCHAR(30) NOT NULL,        -- PC o laptop | No hay | Cualquiera
  impresoras           VARCHAR(80) NOT NULL,
  que_imprime          VARCHAR(80) NOT NULL,
  requiere_controlador BOOLEAN NOT NULL,
  donde_va_el_controlador VARCHAR(40),
  recomendacion        TEXT NOT NULL,
  regla_id             VARCHAR(10) NOT NULL REFERENCES kb_regla_recomendacion(regla_id)
);

CREATE TABLE kb_decision_internet (
  caso_id               VARCHAR(5) PRIMARY KEY,     -- I1…I4
  puede_fibra_o_coaxial VARCHAR(5) NOT NULL,
  necesita_comandas     VARCHAR(15) NOT NULL,
  senal_chip            VARCHAR(30),
  recomendacion         TEXT NOT NULL,
  regla_id              VARCHAR(10) NOT NULL REFERENCES kb_regla_recomendacion(regla_id)
);

CREATE TABLE kb_area_preparacion (
  area           VARCHAR(30) PRIMARY KEY,           -- mismos códigos que P11
  etiqueta       VARCHAR(60) NOT NULL,
  es_preparacion VARCHAR(10) NOT NULL               -- SI | NO | PREGUNTAR
);

CREATE TABLE kb_requisito_equipo (
  equipo                   VARCHAR(40) PRIMARY KEY,
  necesita_punto_red_cerca VARCHAR(40) NOT NULL,
  necesita_energia_cerca   VARCHAR(10) NOT NULL,
  nota                     TEXT
);

CREATE TABLE kb_texto_minuta (
  clave             VARCHAR(40) PRIMARY KEY,      -- AVISO_VIABILIDAD
  seccion_minuta    VARCHAR(80) NOT NULL,
  texto             TEXT NOT NULL,
  cuando_se_muestra TEXT
);

-- Cuántas minutas reales respaldan cada regla (reemplaza las columnas calculadas del Excel)
CREATE VIEW v_kb_regla_frecuencia AS
SELECT r.regla_id, r.nombre, r.prioridad,
       COUNT(mr.id)                  AS veces_en_minutas,
       COUNT(DISTINCT mr.minuta_id)  AS minutas_distintas,
       (SELECT COUNT(*) FROM kb_minuta_hallazgo h WHERE h.regla_id = r.regla_id) AS veces_como_hallazgo
FROM kb_regla_recomendacion r
LEFT JOIN kb_minuta_recomendacion mr ON mr.regla_id = r.regla_id
GROUP BY r.regla_id, r.nombre, r.prioridad;
```

Dónde crear las tablas:
- El gateway es el único dueño de la base. Las entidades JPA van en un paquete nuevo, `com.upc.idbi.gateway.kb`.
- Alternativa: si el equipo usa scripts SQL, ponerlas en una migración (Flyway o un `.sql` versionado).
- **No** se deben crear en producción hasta que el equipo lo apruebe.

## 3. Importador del Excel

Se propone un **script local**, no un endpoint público: `AsistenteRedIDBI-API/tools/import_kb_minutas.py` (openpyxl + psycopg).

1. **Entrada.**
   - `--xlsx Base_Conocimiento_Minutas.xlsx`
   - `--db-url postgresql://…@localhost…`
   - **El script se niega a correr si la URL no es `localhost`, `127.0.0.1` o H2**, salvo que se pase `--permitir-remoto` (no usarlo).
2. **`--dry-run` por defecto.** Valida e imprime un reporte. Solo escribe con `--aplicar`.
3. **Validaciones.** Si falla alguna, el script aborta sin escribir nada:
   - `minuta_id` único. Cada hijo apunta a una minuta existente.
   - `pregunta_id` existe en `flujo_asistente_red.json`. Si es CHOICE o MULTI_SELECT, el `valor` es un código válido de esa pregunta.
   - `regla_id` existe. Ninguna recomendación queda sin regla.
   - Códigos de catálogo válidos (hoja `catalogos`).
   - `fecha_visita` es una fecha válida.
   - **No se cargan credenciales:** se rechazan textos con patrones como `contraseña`, `password`, `clave`, `anydesk` seguidos de un valor.
4. **Idempotente.**
   - Catálogos (`kb_equipo`, `kb_regla_*`, `kb_umbral`): upsert por PK.
   - Por cada `minuta_id`: upsert de la cabecera; luego se borran sus hijos y se reinsertan. Todo en una sola transacción.
5. **Ignora** las columnas grises (calculadas) y las hojas `LEEME`, `resumen`, `ref_preguntas`, `catalogos` y `diccionario`.
6. **Pruebas.** Un test con el Excel de ejemplo debe cargar 18 minutas, 264 respuestas, 72 hallazgos, 106 recomendaciones o acciones, 74 equipos y 70 evidencias en una base H2 o Postgres de prueba.

## 4. Cómo nutre las recomendaciones (fase 2)

Esto se hace después de la carga, también en rama local:

1. **Reglas desde la base.**
   - El motor (`proposal.py`) lee `kb_regla_recomendacion` (activas) y evalúa `condicion_json` con el mismo evaluador de condiciones del flujo (`flow_engine.py`).
   - Las condiciones `{"manual": …}` y `{"validacion": …}` se activan con el estado de las reglas V01–V10 o con una marca del técnico.
2. **Orden.** Primero por prioridad (Alta → Baja), luego por `veces_en_minutas` de `v_kb_regla_frecuencia`. Una regla vista en más visitas reales sube.
3. **Texto.**
   - Hallazgo: `hallazgo_as_is`. Recomendación: `recomendacion_to_be`.
   - Si la regla tiene `equipo_id`, se agrega a `equipment`.
   - Las reglas `A0x` van a *Siguientes acciones*, separadas por responsable (ID / Cliente).
4. **Umbrales.** `kb_umbral` reemplaza las constantes de `proposal.py` y `checks.py`. Las constantes actuales quedan como valor por defecto si la tabla está vacía.
5. **Respaldo.** Si la base no responde, se usa el motor actual tal cual. Nunca se debe dejar al técnico sin propuesta.
6. **Trazabilidad.** Cada recomendación generada guarda su `regla_id`. Así se puede mostrar "visto en N minutas" y medir qué reglas acepta el equipo.

Correspondencia con lo que ya existe:
- Las reglas del motor actual son R17–R22 y R28.
- Las del flujo son R02, R04, R06, R25 y R26.
- Las de las validaciones son R11, R13, R14, R16, R23 y R24.
- Las nuevas de minutas manuales son R01, R03, R05, R07–R10, R12, R15, R27 y R29–R39.
- Las acciones estándar son A01–A12.

Lo que más se repite en las 18 minutas reales:
- **Raspberry para impresiones (R02):** 6 minutas.
- **Switch para ampliar un punto de red (R05):** 6 minutas, más 3 veces como hallazgo.
- **Punto de red para la impresora (R06):** 4 minutas.
- **Respaldo 4G (R01):** 3 minutas.

En ninguna minuta manual aparece todavía el UPS (R04), aunque el flujo lo pregunta.

## 4b. Reglas de negocio IDPos: comandas, impresión e internet (2026-10-06)

### 1. ¿El negocio necesita comandas?

No se le pregunta al técnico: **se deduce de las áreas (P11)**. Necesita comandas si hay al menos un **área de preparación fuera de caja**, porque ahí van las impresoras. Hoja `kb_area_preparacion`:

| Áreas de preparación (comandas) | Áreas que no son de preparación |
|---|---|
| Bar/barra, Cocina, Pizza, Brasas, Jugos (ya están en P11) | Caja, Salón 1, Salón 2, Terraza, Despacho, Almacén, Oficina |
| **Cafetería, Parrillas, Makis, Ramen, Panadería** (proponer agregarlas a P11) | Otro: preguntar si es de preparación |

- Además, el asistente **siempre** pregunta **"¿En caja saldrán comandas?"** (P12b).
  - Si no hay áreas de preparación y la comanda sale en caja → **Sunmi** (caso D7).
  - Si no hay áreas de preparación y en caja tampoco salen comandas → solo pre-cuentas y comprobantes (casos D4 y D5).
  - Si hay áreas de preparación, P12b indica si la impresora de caja también imprimirá comandas.
- **Las impresoras de áreas de preparación son siempre de red, por cable** (R51, caso D9). Si son USB o Bluetooth no sirven, y si son WiFi hay que pasarlas a cable.

### 2. Impresión (hoja `kb_decision_impresion`)

El **controlador de impresiones** se instala en la PC o laptop de caja. Si no hay ninguna, va en un **Raspberry, solo con impresoras de red**: una impresora USB solo funciona con laptop o PC.

**Si en caja hay una tablet, o el negocio solo trabaja con tablets, el Raspberry es obligatorio.**

| Caso | Comandas | Equipo en caja | Impresoras | Qué imprimen | Recomendación | Regla |
|---|---|---|---|---|---|---|
| D1 | En áreas de preparación | PC o laptop | USB en caja | Comandas, pre-cuentas y comprobantes | Controlador en la PC o laptop | R40 |
| D2 | En áreas de preparación | PC o laptop | De red en otras áreas | Comandas, pre-cuentas y comprobantes | Controlador en la PC o laptop | R40 |
| D3 | En áreas de preparación | No / No sé / Otro | Solo de red | Comandas, pre-cuentas y comprobantes | **Kit Raspberry** con el controlador (cotizar con Desarrollo de Negocios) | R02 |
| D3b | En áreas de preparación | No / No sé / Otro | **USB en caja** | Comandas, pre-cuentas y comprobantes | **No sirve Raspberry:** laptop o PC con el controlador (o cambiar la USB por una de red y usar Raspberry) | R43 |
| D9 | En áreas de preparación | Cualquiera | Impresora del área de preparación | Comandas | **Siempre de red por cable**; USB o Bluetooth no sirven | R51 |
| D8 | Cualquiera | **Tablet en caja (o solo tablets)** | De red | Comandas, pre-cuentas y comprobantes | **Raspberry obligatorio**; impresoras de red. Cotizar con Desarrollo de Negocios | R47 |
| D7 | **Solo en caja** | Cualquiera | Sunmi (impresora integrada) | Comprobantes, pre-cuentas y comandas | **Sunmi con chip**. Validar la señal del chip y cotizar con Desarrollo de Negocios | R44 |
| D4 | No | PC o laptop | USB en caja | Pre-cuentas y comprobantes | USB directa, sin controlador | R42 |
| D5 | No | No / No sé / Otro | USB en caja | Pre-cuentas y comprobantes | Laptop o PC básica con USB, sin controlador | R41 |
| D6 | No | Cualquiera | Solo de red | Pre-cuentas y comprobantes | No hace falta impresora de red | R42 |

### 3. Internet (hoja `kb_decision_internet`)

Se usa la respuesta de **P14 (tipo de conexión)**, con las opciones nuevas **Fibra óptica / Cable coaxial / No sé / No tiene / Internet con chip**. **Si el técnico responde "No sé", se toma como que el local no tiene internet.**

| Caso | Conexión actual (P14) | Comandas | Señal del chip (prueba con celular) | Recomendación | Regla |
|---|---|---|---|---|---|
| I1 | Fibra óptica o Cable coaxial | Cualquiera | — (solo si va Sunmi) | Mantener la conexión | R33 |
| I1b | **Internet con chip** | Cualquiera | Se mide con el speedtest E1 | Si necesita comandas, el chip debe ir en un router, no en un celular compartiendo datos | R45 |
| I2 | **No tiene o No sé** (= sin internet) | **Sí** | Entel, Claro o ambos | **Router con chip** del proveedor con señal | R45 |
| I3 | No tiene o No sé (= sin internet) | No | — | No se recomienda router con chip (si va Sunmi, validar igual la señal) | R44 |
| I4 | No tiene o No sé (= sin internet) | Cualquiera | Ninguno | Sin conectividad viable: evaluar satelital y segunda visita *(propuesta mía, no indicada por el equipo)* | R46 |

Con "No tiene" o "No sé", el asistente salta la velocidad contratada (P15) y el speedtest (E1), porque no hay qué medir, y pasa directo a la prueba del chip.

**Cómo se valida la señal:**
- Se pone el chip (Entel o Claro) en un celular dentro del local y se hace un speedtest. La captura queda como evidencia E1b.
- El **Sunmi** funciona con chip, así que esta prueba se hace siempre que se recomiende un Sunmi, aunque el local tenga fibra.

**Compras con Desarrollo de Negocios.** El kit **Raspberry** y el **Sunmi** se cotizan con el equipo de Desarrollo de Negocios; el Sunmi se compra directamente a ese equipo. Esto genera la acción **A13**.

### Cambios en la base de conocimiento

- **R02 (Raspberry):** se recomienda solo si hay áreas de preparación, no hay PC ni laptop y la impresora de caja no es USB. Incluye la cotización con Desarrollo de Negocios.
- **R16 (impresora USB):** funciona solo con laptop o PC; con el controlador imprime todo.
- **Reglas nuevas:**
  - R40: controlador en la PC o laptop.
  - R41: laptop o PC básica.
  - R42: sin comandas, USB directa.
  - R43: USB en caja sin PC, se necesita laptop o PC.
  - R44: Sunmi.
  - R45: router con chip.
  - R46: sin conectividad.
  - R47: tablet en caja, Raspberry obligatorio.
  - R51: impresoras de áreas de preparación, siempre de red.
  - R52: confirmar el equipo de caja cuando P22 = No sé u Otro.
- **Acción nueva:** A13 (cotizar Raspberry o Sunmi con Desarrollo de Negocios).
- **Equipos nuevos:** `EQ-SUNMI` y `EQ-PC-BASICA`.

### 4. Puertos, puntos de red, energía y WiFi

**Puertos del router (R48).** Se cuentan los equipos que van por cable: impresoras de red, PC o laptop por cable, Raspberry, y access point o repetidor principal. Si son más que los puertos LAN libres del router, que **la IA cuenta en la foto del router (E2)** (puertos totales y ocupados, que el técnico confirma; la pregunta P18a queda solo de respaldo si la foto no se puede leer):

```
equipos_cableados = impresoras de red + PCs/laptops por cable + Raspberry + (access point o 1.er repetidor)
faltan            = equipos_cableados − puertos_libres_router
si faltan > 0:
  switch          = el menor de 5 / 8 / 16 / 24 puertos que cubra (faltan + 1 puerto para unirlo al router)
  puntos de red   = un cable por cada equipo que no tiene punto cerca:
                    un extremo en el router/switch (la central) y el otro junto al equipo
```

El asistente le indica al cliente **cuántos puntos de red** debe implementar y **de qué tamaño es el switch**.

**Qué necesita cerca cada equipo (hoja `kb_requisito_equipo`, R49):**

| Equipo | Punto de red cerca | Punto de energía cerca |
|---|---|---|
| Switch | Sí (cable al router) | Sí |
| Raspberry | Sí | Sí |
| Impresora de red | Sí | Sí |
| Impresora USB | No (USB a la PC o laptop) | Sí |
| Access point | Sí | Sí |
| Repetidor principal (1.º) | Sí | Sí |
| Repetidor secundario (por WiFi) | **No** | Sí |
| PC o laptop de caja | Recomendado (R11) | Sí |
| Sunmi | No (chip o WiFi) | Sí |

Si un equipo **no tiene toma cerca**, se indica que necesitará un **punto de energía adicional o una extensión**. Esto se cruza con R25, que pide retirar las extensiones de los equipos de red: la extensión sirve para empezar, pero conviene reemplazarla luego por una toma o un UPS.

**Repetidores y access points (R19, R50):**
- Si el técnico indica que hay zonas sin señal (P46), se recomienda un **repetidor o un access point**.
- El access point y el primer repetidor necesitan red y energía. El segundo repetidor, que se conecta por WiFi al primero, solo necesita energía.
- **Todo el local debe tener WiFi.** Los meseros comandan desde celular, tablet, PC, laptop o Sunmi, y en una zona sin señal no pueden acceder al sistema. Por eso R19 ahora es de prioridad Alta.

**Minutas que pasaron a R48** porque el switch se recomendó por falta de puertos:
- RKT Villa Flores: router sin puertos libres.
- Gelato Alore Ejército: access point sin puertos para el Raspberry.
- Malala: el TP-Link de 3 puertos dejó fuera la barra de makis.

### Aviso de viabilidad en la minuta (hoja `kb_texto_minuta`)

Todo lo anterior se presenta como **recomendaciones**. Si la minuta tiene al menos una, debe mostrar este aviso en *Siguientes acciones* y en el *Resumen ejecutivo*:

> **Si no se siguen las recomendaciones de esta minuta, no es viable la implementación del punto de venta en el local.**

Implementación sugerida:
- En `minuta_doc.py`, agregar el texto cuando `recommendations` no esté vacío.
- En la plantilla Thymeleaf de `MinutaPdfService`, mostrarlo destacado: recuadro y negrita.

### Cambio al flujo: qué está hecho y qué falta

**Implementado y desplegado el 2026-10-10** (FastAPI, gateway y APK 0.0.3). Detalle en [`../flujo/FLUJO_NODOS.md`](../flujo/FLUJO_NODOS.md), sección 15:
- **P22** con *Sí / No / Tablet / No sé / Otro*, con P22f ("¿qué equipo hay en caja?") y aviso de Raspberry obligatorio para la tablet.
- **P12b** "¿En caja saldrán comandas?" como pregunta fija para todos los negocios, con el aviso del Sunmi.
- **P14** con *Fibra óptica / Cable coaxial / No sé / No tiene / Internet con chip*. Sin internet se salta P15 y el speedtest E1.
- **Áreas de preparación nuevas** en P11 (Cafetería, Parrillas, Makis, Ramen, Panadería).
- **Regla R51:** si un área de preparación necesita impresora, tiene que ser de red por cable. Genera alerta y acción en el chat y una regla en la validación de la minuta.
- Motor: operador `includesAny`, referencias `$item` y `$prepAreas`.

- **P14b + E1b** (prueba de la señal del chip), **puertos del router** leídos en E2 (con P18a de respaldo), **switch y puntos de red** (`G_PUERTOS`), **decisión de impresión** (`G_IMPRESION`), **WiFi por zona** (P46a), el **aviso de viabilidad** en la minuta y los **valores derivados** calculados por el motor.

Diferencias con el diseño de referencia de abajo: el aviso del Sunmi sale una sola vez después de P12b (no dentro del loop de cajas); la alerta de Raspberry de cada caja se quitó y la recomendación se decide al final del bloque E; el número de equipos por cable cuenta el access point si hay zonas sin señal o P45 = Sí, y los puntos de red faltantes salen de las respuestas U1 = No (o de los puertos que faltan).

**Sigue propuesto (no implementado):** las tablas `kb_*`, el importador del Excel y que las recomendaciones salgan de la base de conocimiento (fase 2). Las reglas siguen fijas en el código.

**Cambio propuesto al flujo (diseño completo)**

**La mayoría de los datos se deduce de las 54 preguntas y 13 evidencias actuales.** Solo hacen falta 4 ajustes, y casi todos aparecen únicamente en ciertos casos.

| Dato | Cómo se obtiene |
|---|---|
| ¿Necesita comandas? | **Deducido**: P11 (áreas de preparación), confirmado con P25 |
| ¿La impresora de caja es USB o de red? | **Deducido**: P27 + P30 + E5. La decisión del Raspberry se toma al final del bloque de impresoras |
| Equipos por cable, tamaño del switch, puntos de red que faltan | **Deducido**: P23, P30, P32c, P22c, P45 |
| Puertos libres del router | **Deducido de la foto del router (E2)**: la IA cuenta los puertos LAN totales y ocupados, y el técnico confirma. P18a solo si la foto no se puede leer |
| Cobertura WiFi, zonas sin señal y cantidad de repetidores | **Deducido**: P46 + P11 (un repetidor o AP por zona) |
| ¿Repetidor o access point? | **Deducido y confirmado**: si la zona tiene punto de red (P38/P38a), access point; si no, repetidor (P46a confirma) |
| ¿Hay fibra o coaxial? | **Deducido**: P14. "No sé" y "No tiene" = sin internet |
| Equipo de caja | **Ajuste 1:** P22 con las opciones **Sí / No / Tablet / No sé / Otro**. Con "Otro" se pregunta qué equipo es (P22f). "No sé" y "Otro" se calculan como "No" y quedan por confirmar (R52) |
| ¿Salen comandas en caja? | **Ajuste 2:** P12b **"¿En caja saldrán comandas?"**, pregunta fija del bloque B |
| Señal del chip | **Ajuste 3:** P14b + captura E1b, solo si P14 = No tiene o No sé, o si va Sunmi |
| Tipo de conexión | **Ajuste 4:** P14 con las opciones **Fibra óptica / Cable coaxial / No sé / No tiene / Internet con chip** |
| Impresoras de áreas de preparación | **Deducido**: P27 + P30. Si no son de red por cable → aviso R51 |

**Se descartaron, por ser deducibles:** P12a (era pregunta, ahora es dato derivado), P14a, P22e y P46b.

```jsonc
// 1) P11: agregar opciones de preparación
//    CAFETERIA = Cafetería · PARRILLA = Parrillas · MAKIS = Makis · RAMEN = Ramen · PANADERIA = Panadería
// 2) P14: opciones  FIBRA = Fibra óptica · COAXIAL = Cable coaxial · NO_SE = No sé · NO_TIENE = No tiene · CHIP = Internet con chip
//    ("No sé" y "No tiene" = sin internet)
// 3) P22: opciones  SI = Sí · NO = No · TABLET = Tablet · NO_SE = No sé · OTRO = Otro (indicar cuál es el equipo)
{ "id": "P22f", "kind": "question", "block": "D", "inputType": "TEXT",
  "text": "Caja {i}: ¿qué equipo hay en caja?", "field": "cajas[{i}].equipoOtro",
  "showIf": {"eq": ["$P22", "OTRO"]}, "next": "@loop.next" }   // se calcula como "No" y queda por confirmar (R52)
// 4) E2 (foto del router): agregar a aiExtract los campos de puertos LAN
"aiExtract": ["marca", "modelo", "ubicacionVisual", "puertosLanTotales", "puertosLanOcupados", "puertosLanLibres"]

// 5) Datos derivados (el motor los recalcula en cada paso; no se muestran como preguntas)
{ "derived": {
    "necesitaComandas": {"includesAny": ["$P11", ["BAR","COCINA","PIZZA","BRASAS","JUGOS","CAFETERIA","PARRILLA","MAKIS","RAMEN","PANADERIA"]]},
    "sinInternet": {"in": ["$P14", ["NO_TIENE","NO_SE"]]},
    "impresoraEnAreaPreparacion": "la impresora de P27 incluye alguna área de preparación",
    "conexionImpresoraCaja": "P30 de la impresora cuyo P27 incluye CAJA (USB | CABLE_RED | WIFI)",
    "puertosLibresRouter": "E2.puertosLanLibres confirmado por el técnico; si no se pudo leer, P18a",
    "equiposCableados": "count(P30 = CABLE_RED) + count(P32c = CABLE_RED) + count(P23 = CABLE_RED) + count(Raspberry) + (P46 no vacío ? 1 : 0)",
    "tamanoSwitch": "menor de [5, 8, 16, 24] que cubra (equiposCableados - puertosLibresRouter + 1)",
    "puntosRedFaltantes": "equipos por cable sin punto cercano (U1 = No)"
} }

// 6) P12 → P12b (pregunta fija)
{ "id": "P12b", "kind": "question", "block": "B", "inputType": "YES_NO",
  "text": "¿En caja saldrán comandas?",
  "field": "negocio.comandaEnCaja",
  "next": "P13" }

// 7) P14: si es No tiene o No sé, se saltan P15 y E1 y se hace la prueba del chip
"next": { "rules": [
    { "if": {"eq": ["$derived.sinInternet", true]}, "goto": "P14b" },
    { "if": {"and": [{"eq": ["$P12b", "SI"]}, {"eq": ["$derived.necesitaComandas", false]}]}, "goto": "P14b" } ],
  "default": "P15" }
{ "id": "P14b", "kind": "question", "block": "C", "inputType": "CHOICE",
  "text": "Pon el chip en un celular dentro del local. ¿Qué chip tiene señal?", "field": "internet.senalChip",
  "options": [ {"value":"ENTEL","label":"Entel"}, {"value":"CLARO","label":"Claro"}, {"value":"AMBOS","label":"Ambos"}, {"value":"NINGUNO","label":"Ninguno"} ],
  "next": { "rules": [ { "if": {"ne": ["$P14b","NINGUNO"]}, "goto": "E1b" } ], "default": "P16" },
  "effects": [
    { "if": {"and": [{"eq":["$derived.sinInternet",true]},{"eq":["$derived.necesitaComandas",true]},{"ne":["$P14b","NINGUNO"]}]},
      "addAction": "Recomendar router con chip {P14b.label} como internet del local." },
    { "if": {"and": [{"eq":["$derived.sinInternet",true]},{"eq":["$P14b","NINGUNO"]}]},
      "addAlert": "Sin fibra, cable coaxial ni señal de chip: evaluar internet satelital y agendar segunda visita." } ] }
{ "id": "E1b", "kind": "evidence", "evidenceCode": "E1b", "block": "C", "inputType": "EVIDENCE",
  "text": "Sube la captura del speedtest hecho desde el celular con el chip.", "field": "evidencias.speedtestChip",
  "aiExtract": ["bajadaMbps","subidaMbps","pingMs","proveedor"],
  "next": { "rules": [ { "if": {"eq": ["$derived.sinInternet", true]}, "goto": "P16" } ], "default": "P15" } }

// 8) E2: confirmar los puertos leídos; si la foto no se pudo leer, preguntar P18a
{ "id": "P18a", "kind": "question", "block": "C", "inputType": "NUMBER",
  "text": "No se pudieron leer los puertos en la foto del router. ¿Cuántos puertos LAN libres tiene?",
  "field": "internet.puertosLibresRouter", "showIf": {"eq": ["$E2.puertosLanLibres", null]}, "next": "P21" }

// 9) Cajas: P22 con las opciones nuevas
"next": { "rules": [
    { "if": {"and":[{"eq":["$P12b","SI"]},{"eq":["$derived.necesitaComandas",false]}]}, "goto": "A_SUNMI" },
    { "if": {"eq":["$P22","TABLET"]}, "goto": "A_RASPBERRY_TABLET" },
    { "if": {"eq":["$P22","OTRO"]},   "goto": "P22f" },
    { "if": {"eq":["$P22","SI"]},     "goto": "P23" } ],
  "default": "@loop.next" }   // No / No sé / Otro: la decisión se toma en G_IMPRESION (paso 10); No sé y Otro quedan por confirmar (R52)
{ "id": "A_RASPBERRY_TABLET", "kind": "alert", "block": "D", "severity": "warning",
  "text": "Caja {i}: la caja es una tablet. Es obligatorio un kit Raspberry para el controlador de impresiones y las impresoras deben ser de red. Cotízalo con Desarrollo de Negocios.",
  "effects": [{"addAction": "Caja {i}: adquirir kit Raspberry (obligatorio por usar tablet). Cotizar con Desarrollo de Negocios."}],
  "next": "P22a" }
{ "id": "A_SUNMI", "kind": "alert", "block": "D",
  "text": "La comanda sale solo en caja. Recomienda un Sunmi con chip (imprime comprobantes, pre-cuentas y comandas). Se compra al equipo de Desarrollo de Negocios.",
  "effects": [{"addAction": "Pedir la cotización del Sunmi al equipo de Desarrollo de Negocios."}], "next": "@loop.next" }

// 10) Al terminar el bloque E (impresoras): decisión de impresión y de puertos
{ "id": "G_IMPRESION", "kind": "router", "block": "E",
  "next": { "rules": [
      { "if": {"and":[{"in":["$P22",["NO","NO_SE","OTRO"]]},{"eq":["$derived.necesitaComandas",false]},{"eq":["$P12b","NO"]}]}, "goto": "A_PC_BASICA" },
      { "if": {"and":[{"in":["$P22",["NO","NO_SE","OTRO"]]},{"eq":["$derived.conexionImpresoraCaja","USB"]}]}, "goto": "A_PC_CONTROLADOR" },
      { "if": {"and":[{"in":["$P22",["NO","NO_SE","OTRO"]]},{"eq":["$derived.necesitaComandas",true]}]}, "goto": "A_RASPBERRY" } ],
    "default": "G_PUERTOS" } }
// En P30/P32c, si la impresora es de un área de preparación y no es "Cable de red":
//   aviso R51 "Las impresoras de áreas de preparación deben ser de red, por cable".
{ "id": "A_RASPBERRY", "kind": "alert", "block": "E",
  "text": "No hay PC ni laptop en caja y las impresoras son de red. Recomienda un kit Raspberry para el controlador de impresiones. Cotízalo con Desarrollo de Negocios.",
  "effects": [{"addAction": "Cotizar el kit Raspberry con el equipo de Desarrollo de Negocios."}], "next": "G_PUERTOS" }
{ "id": "A_PC_CONTROLADOR", "kind": "alert", "block": "E",
  "text": "La impresora de caja es USB y solo funciona con laptop o PC, no con Raspberry. Recomienda una laptop o PC para el controlador de impresiones.",
  "effects": [{"addAction": "El cliente adquirirá una laptop o PC para el controlador de impresiones."}], "next": "G_PUERTOS" }
{ "id": "A_PC_BASICA", "kind": "alert", "block": "E", "severity": "info",
  "text": "No hay áreas de preparación ni comanda. Recomienda una laptop o PC básica con USB para pre-cuentas y comprobantes; sin controlador.",
  "effects": [{"addAction": "El cliente adquirirá una laptop o PC básica con USB."}], "next": "G_PUERTOS" }
{ "id": "G_PUERTOS", "kind": "router", "block": "E",
  "next": { "rules": [ { "if": {"gt":["$derived.equiposCableados","$derived.puertosLibresRouter"]}, "goto": "A_FALTAN_PUERTOS" } ], "default": "P33" } }
{ "id": "A_FALTAN_PUERTOS", "kind": "alert", "block": "E", "severity": "warning",
  "text": "El router tiene {derived.puertosLibresRouter} puertos libres y hay {derived.equiposCableados} equipos por cable. Recomienda un switch de {derived.tamanoSwitch} puertos e implementar {derived.puntosRedFaltantes} puntos de red (un extremo en el router/switch y el otro junto a cada equipo).",
  "effects": [{"addAction": "Implementar switch de {derived.tamanoSwitch} puertos y {derived.puntosRedFaltantes} puntos de red."}],
  "next": "U_SWITCH" }
{ "id": "U_SWITCH", "kind": "subflow_call", "block": "E", "subflow": "U",
  "params": {"area": "$P18", "equipo": "Switch nuevo", "needsNetwork": false, "needsPower": true},
  "next": "P33" }
// El último nodo del bloque E (P32 o el loop de impresoras) debe apuntar a G_IMPRESION en vez de P33.

// 11) Zonas sin señal: por zona, repetidor o access point (1.º con red y energía; los siguientes solo energía)
{ "id": "P46a", "kind": "question", "block": "H", "inputType": "CHOICE",
  "text": "Sugerencia: {derived.sugerenciaWifi}. ¿Qué se instalará en las zonas sin señal?", "field": "wifi.solucion",
  "options": [ {"value":"REPETIDOR","label":"Repetidor"}, {"value":"ACCESS_POINT","label":"Access point"}, {"value":"AMBOS","label":"Ambos"} ],
  "showIf": {"notEmpty": "$P46"}, "next": "L_ZONAS_SIN_SENAL" }
{ "id": "L_ZONAS_SIN_SENAL", "kind": "loop", "block": "H", "over": {"list": "$P46"},
  "itemLabel": "{item.label}", "body": "U_WIFI", "exit": "P47" }
{ "id": "U_WIFI", "kind": "subflow_call", "block": "H", "subflow": "U",
  "params": { "area": "{item.value}", "equipo": "Repetidor/AP – {item.label}",
              "needsNetwork": {"eq":["$loop.index",1]}, "needsPower": true },
  "next": "@loop.next" }
// En el subflujo U: si U5 = No, agregar "necesitará un punto de energía adicional o una extensión" (R49).

// 12) P23 (hay PC o laptop): agregar el efecto del controlador
"effects": [{ "if": {"eq":["$derived.necesitaComandas",true]}, "addAction": "Caja {i}: instalar el controlador de impresiones en la PC o laptop." }]
```

Notas para implementar:
- **Motor:** se necesitan el operador `includesAny`, la condición `showIf` para preguntas condicionales y un bloque `derived` que se recalcule en cada paso.
- **Validación de puertos con la IA:** en la tarjeta de E2, el técnico confirma o corrige los puertos leídos, igual que con las demás evidencias.
- **Bloque E:** si una impresora queda USB y el equipo de caja es un Raspberry, avisar "La impresora USB no funciona con Raspberry".
- **Área "Otro" en P11:** preguntar si es de preparación.
- **Minuta y mapa:** deben mostrar dónde va el controlador, si va Sunmi, el resultado de la prueba del chip y los puntos de red y energía que faltan.

### Minutas que no coinciden con las reglas (hoja `revisar_vs_regla`)

- **RKT Banchero y Villa Flores:** se recomendó impresora de red para caja aunque hay PC de caja. Con la regla, la USB conectada a la PC con controlador imprime todo.
- **Gelato Alore (3 sedes):** ya coinciden. La caja es una tablet, así que el Raspberry es obligatorio (R47). Sus recomendaciones quedaron unidas a R47.

## 5. Cómo cargar nuevas minutas en PDF

1. Copiar el PDF a `docs/base-conocimiento/minutas-pdf/` (solo en local; **no** se versionan si tienen datos sensibles).
2. Llenar el Excel siguiendo `LEEME`. También se le puede pasar el PDF a Claude para que proponga las filas; una persona las revisa.
3. Revisar la hoja `resumen`: **recomendaciones sin regla vinculada = 0**.
4. Correr `python tools/import_kb_minutas.py --xlsx … --db-url <local>` (dry-run) y revisar el reporte.
5. Correr de nuevo con `--aplicar`, solo en local.

## 6. Prompt para Claude Code

```
Lee docs/base-conocimiento/BASE_CONOCIMIENTO_MINUTAS.md y el Excel docs/base-conocimiento/Base_Conocimiento_Minutas.xlsx.

REGLAS OBLIGATORIAS:
- NO subas nada a producción. No hagas push a main en ningún repo (Railway despliega main solo), no abras ni mergees PRs a main, no toques variables de Railway, no te conectes a la base de producción y no generes un APK nuevo.
- Trabaja en la rama local feature/base-conocimiento-minutas y prueba solo con Postgres local o H2.
- Al terminar, muéstrame el diff y los resultados de los tests. No hagas commit hasta que te lo diga.

TAREAS:
1. Crea las tablas kb_* y la vista v_kb_regla_frecuencia de la sección 2 (entidades JPA en el gateway, paquete kb, o migración SQL; propón cuál según el proyecto).
2. Implementa el importador local de la sección 3 (dry-run por defecto, se niega a correr contra hosts remotos, validaciones y transacción) con su test.
3. Carga el Excel en la base local y muéstrame el reporte y SELECT * FROM v_kb_regla_frecuencia.
4. Propón (sin implementar todavía) el cambio de la sección 4 en proposal.py.
```

## 7. Supuestos y pendientes

- **Motivos inferidos.** En Sicilia y Rock & Burgers, `motivo_visita = IMPLEMENTACION` es inferido: las minutas no lo dicen tal cual. Está marcado como INFERIDO.
- **Rock & Burgers.** Se cargó con los datos del PDF manual, no con los del documento generado por la app (`rock_burgers_document.json`). Por eso no tiene velocidad contratada ni cajas.
- **Credenciales.** Varias minutas traen datos de acceso: la clave de acceso remoto en Rock & Burgers, el ID de AnyDesk en RKT Hacienda y códigos QR de WiFi en Gelato Alore. **No se cargaron**, y el importador debe rechazarlos.
- **Don Oscar.** La versión del PDF que llegó el 2026-10-06 tiene el mismo contenido que MH-002; no se duplicó.
- **Proveedores fuera del catálogo** (Yachay Telecomunicaciones, WOW, Hughesnet): se cargaron como `OTRO`, con el nombre en `observaciones_carga`.
- **Tipo de conexión (P14):** cuando la minuta no lo dice, queda **en blanco**, no "No sé", porque "No sé" ahora significa sin internet.
  - Malala quedó como "No tiene".
  - Hotel La Confianza (satelital, Hughesnet) quedó en blanco: P14 no tiene opción para satelital.
- **Equipo de caja (P22):** las respuestas se pasaron a los códigos nuevos. PC pasó a "Sí", y Gelato Alore quedó como "Tablet".
- **Fotos.** `kb_minuta_evidencia` solo referencia las fotos y videos, no los sube. Si más adelante se quieren las imágenes, deben ir a un almacenamiento local de pruebas, nunca al de producción.
