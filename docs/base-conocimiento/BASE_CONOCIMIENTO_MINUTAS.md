# Base de conocimiento de minutas manuales

Última actualización: 2026-10-11 (44 minutas cargadas, perfiles por rubro y canales de atención). El cambio al flujo de la sección 4b **ya está implementado y desplegado** (APK 0.0.3). Lo demás de este documento sigue **propuesto**.

Este documento explica cómo pasar a la base de datos las **minutas técnicas hechas a mano (PDF y Word)**. El objetivo es que el motor de recomendaciones aprenda de visitas reales.

La entrada es el Excel [`Base_Conocimiento_Minutas.xlsx`](Base_Conocimiento_Minutas.xlsx). Ya viene cargado con:
- **44 minutas manuales (MH-001 a MH-044)**, de 2023 a 2026, con el rubro confirmado por el equipo:

| Rubro | Visitas | Negocios |
|---|---|---|
| Restaurante – Comida oriental | 10 | Shimaya (7), Maki Sam (2), Omakase |
| Pollería | 7 | Rikoton (7) |
| Restaurante – Pescados y mariscos | 6 | Don Oscar, Barrio Pesquero, El Chito, Modonese, Caleta, Don Chalán |
| Fast food | 5 | Rock & Burgers, ALF Sandwiches, Bros, Saltao, Belgofre |
| Restaurante (otro tipo de comida) | 4 | Sicilia, Huambra, Verbena, La Carpita del Sabor |
| Restobar | 4 | Dorcher (2 visitas), Malala, Los Juárez |
| Heladería | 3 | Gelato Alore (3) |
| Discoteca o bar | 2 | Mala Vida, Soma |
| Cafetería | 1 | Gourmety |
| Hotel | 1 | Hotel La Confianza |
| Retail – Supermercado | 1 | Flora y Fauna |

- 64 reglas de recomendación (R01–R64) y 13 acciones estándar. **R58–R64 son propuestas**, por confirmar con el equipo.
- Las tablas de decisión de impresión e internet y el catálogo de áreas (sección 4b).
- **Los perfiles de preguntas por rubro** (sección 4c).
- 10 reglas de validación, 13 equipos recomendables y 19 umbrales.

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
| `kb_minuta` | `kb_minuta` | Una fila por minuta: cliente, local, dirección, fecha, estado del local, antecedentes, rubro, internet, estado, asistentes, archivo PDF |
| `kb_minuta_respuesta` | `kb_minuta_respuesta` | Lo que la minuta responde de las preguntas del flujo (P01…P54, U1…U8), con alcance y confianza (EXPLICITO / INFERIDO) |
| `kb_minuta_hallazgo` | `kb_minuta_hallazgo` | Antecedentes, desarrollo y observaciones, vinculados a pregunta y regla |
| `kb_minuta_recomendacion` | `kb_minuta_recomendacion` | Recomendaciones y siguientes acciones tal como están en el PDF, cada una con su `regla_id` |
| `kb_minuta_equipo` | `kb_minuta_equipo` | Equipos vistos (router, PC, impresoras, repetidores…) con marca, modelo, IP, conexión y área |
| `kb_minuta_evidencia` | `kb_minuta_evidencia` | Referencia a fotos, mapas y videos de la minuta. Solo referencia: no se suben archivos |
| `kb_minuta_credencial` 🔒 | `kb_minuta_credencial` | **CONFIDENCIAL** (pestaña roja). Claves WiFi, router, AnyDesk y usuario de equipo leídos de la evidencia. Se cargan cifrados |
| `kb_regla_recomendacion` | `kb_regla_recomendacion` | Catálogo de reglas: disparador, condición JSON, hallazgo AS-IS, recomendación TO-BE, responsable, prioridad, equipo y fuente. Tiene columnas calculadas de cuántas minutas la respaldan |
| `kb_regla_validacion` | `kb_regla_validacion` | Reglas V01–V10 de `app/chat/checks.py` (referencia) |
| `kb_equipo` | `kb_equipo` | Equipos recomendables |
| `kb_umbral` | `kb_umbral` | Umbrales hoy fijos en `proposal.py` y `checks.py` |
| `kb_decision_impresion` | `kb_decision_impresion` | Tabla de decisión de impresión IDPos (sección 4b) |
| `kb_decision_internet` | `kb_decision_internet` | Tabla de decisión de internet (fibra/coaxial, comandas, señal de chip) |
| `kb_area_preparacion` | `kb_area_preparacion` | Qué áreas son de preparación (definen si hay comandas) |
| `kb_requisito_equipo` | `kb_requisito_equipo` | Qué necesita cerca cada equipo (punto de red y/o energía) |
| `kb_texto_minuta` | `kb_texto_minuta` | Textos fijos de la minuta (aviso de viabilidad) |
| `kb_perfil_rubro` | `kb_perfil_rubro` | Un perfil por rubro (P09): áreas sugeridas, caja compartida, preguntas que se ocultan o agregan, reglas que se activan. Incluye el modificador de patio de comidas (sección 4c) |
| `kb_perfil_como_funciona` | — (no se carga) | Las 7 reglas de funcionamiento de los perfiles |
| `kb_accion_siguiente` | `kb_accion_siguiente` | Pasos a seguir que se generan solos (cliente / ID) y cuándo se agregan (sección 4d) |
| `kb_observacion_opcion` | `kb_observacion_opcion` | Opciones del apartado de observaciones y orden de la minuta |
| `kb_minuta_observacion` | `kb_minuta_observacion` | Observaciones que traen las minutas (método de pago, viabilidad…) |
| `archivos_no_cargados` | — (no se carga) | Archivos recibidos que no se cargaron y por qué |
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
  estado_local   VARCHAR(30),                       -- EN_OPERACION, EN_OBRA, POR_INAUGURAR, SISTEMA_ANTERIOR, OTRO
  antecedentes   TEXT,
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

-- CONFIDENCIAL: credenciales leídas de la evidencia (WiFi, router, AnyDesk, usuario de equipo).
-- La clave va cifrada con pgcrypto; la llave está en una variable de entorno (KB_CRED_KEY), nunca en el código.
-- Solo el rol ADMIN puede leerla; no se expone en endpoints públicos ni en logs.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE TABLE kb_minuta_credencial (
  id BIGSERIAL PRIMARY KEY,
  minuta_id     VARCHAR(20) NOT NULL REFERENCES kb_minuta(minuta_id) ON DELETE CASCADE,
  tipo          VARCHAR(20) NOT NULL CHECK (tipo IN ('WIFI','ROUTER_ADMIN','ACCESO_REMOTO','USUARIO_EQUIPO','PUERTO_SERVICIO')),
  equipo_o_area VARCHAR(80),
  usuario_o_ssid VARCHAR(120),
  clave_o_id    BYTEA,                               -- pgp_sym_encrypt(valor, KB_CRED_KEY)
  evidencia     VARCHAR(120), nota TEXT
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
   - **Credenciales solo en su hoja:** las de `kb_minuta_credencial` se cargan cifradas (`pgp_sym_encrypt` con `KB_CRED_KEY`). En las demás hojas se rechazan textos con patrones como `contraseña`, `password`, `clave`, `anydesk` seguidos de un valor.
   - El reporte del dry-run nunca imprime las claves: solo cuántas hay por minuta.
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
- **Respaldo 4G (R01):** 3 minutas. *(Desde 2026-10-10, unificada en R56.)*

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
| I1b | **Internet con chip** | Cualquiera | Se mide con el speedtest E1 | Si necesita comandas, el chip debe ir en un router, no en un celular compartiendo datos | R56 |
| I2 | **No tiene o No sé** (= sin internet) | **Sí** | Entel, Claro o ambos | **Router con chip** del proveedor con señal | R56 |
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
  - R45: router con chip. *(Desde 2026-10-10, unificada en R56.)*
  - R46: sin conectividad.
  - R47: tablet en caja, Raspberry obligatorio.
  - R51: impresoras de áreas de preparación, siempre de red.
  - R52: confirmar el equipo de caja cuando P22 = No sé u Otro.
  - R53: cámaras, plan de 1000 Mbps y red separada.
  - R54 (actualizada 2026-10-11): mínimo por equipo (10 Mbps por equipo WiFi, 5 Mbps por PC por cable) y recomendado ~150 Mbps por zona de atención. Ver `analisis/LOGICA_ANALISIS.md` 2.5.
  - R55: impresoras en el segmento del router (validación V04 contra la puerta de enlace del ipconfig E4).
- **Hojas nuevas:** `kb_tramo_plan` y `kb_puntaje_conectividad`, con la lógica de Conectividad. El detalle está en `../analisis/LOGICA_ANALISIS.md`.
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

**Sigue propuesto (no implementado):** las tablas `kb_*`, el importador del Excel y que las recomendaciones salgan de la base de conocimiento (fase 2). Las reglas siguen fijas en el código. También todo lo de las secciones 4c y 4d (rubros, canales, estado del local, pasos a seguir, observaciones, R55–R64) y lo de [`../analisis/`](../analisis/). El resumen de qué está hecho y qué falta está en [`../analisis/ESTADO_VERSION_0_0_3.md`](../analisis/ESTADO_VERSION_0_0_3.md).

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
| Tipo de negocio | **Ajuste 5:** P09 con rubro y tipo de comida (15 opciones) y perfiles de preguntas por rubro (sección 4c) |
| Proveedor "Otro" | **Ajuste 6:** P13a "¿Cuál es el nombre del proveedor?" (texto), solo si P13 = Otro |
| Áreas múltiples | **Ajuste 7:** si P09 = Discoteca o bar, o Restobar, P11 suma Barra 1–3 y Cocina 1–2. Con "Otro" se escribe el área (P11b, por ejemplo "Cocina 2") y se pregunta si es de preparación (P11c) |
| Ubicación de cada área | **Ajuste 8:** P11a ofrece Piso 1…N, Terraza u Otro. Con "Otro" se escribe la ubicación (P11d) |
| Barra → caja | **Ajuste 9:** solo si hay más de una caja, por cada barra se pregunta a qué caja está asociada (P12c: Caja 1…N u Otro, y con "Otro" se escribe en P12d) |
| Segmento de impresoras | **Deducido:** E5 frente a la puerta de enlace de E4 (o E3/E8). Si no coincide, aviso R55 al subir el autotest |
| Impresoras de áreas de preparación | **Deducido**: P27 + P30. Si no son de red por cable → aviso R51 |
| **Toda opción "Otro"** | **Ajuste 10 (regla general):** si se elige "Otro" u "Otra", el chat pide escribir cuál es. El texto se guarda y se usa en la minuta, la evaluación y las preguntas siguientes (ver "Regla general: opción Otro") |

#### Regla general: opción "Otro" (confirmada 2026-10-10)

En **toda** pregunta de opción única o múltiple que tenga "Otro" u "Otra", actual o futura:

1. **Se pide el texto en el chat:** "Escribe cuál es ({pregunta})". El técnico lo escribe y no puede dejarlo vacío.
2. **Se guarda como respuesta registrada:** en el campo `{campo}Otro` y en la respuesta como `OTRO: <texto>` (por ejemplo, `OTRO: Cocina 2`).
3. **Se usa en:**
   - **La minuta:** se muestra el texto escrito, no la palabra "Otro".
   - **La evaluación:** el texto pasa al análisis. Si no encaja en ninguna regla, el dato queda "por confirmar" y no se descarta.
   - **Las preguntas siguientes:** lo escrito aparece como opción más adelante. Por ejemplo, un área escrita en P11 aparece en P27 (áreas que imprime), P28 (ubicación de la impresora) y P46 (zonas sin WiFi).

| Pregunta con "Otro" | Pregunta de texto | Dónde se usa después |
|---|---|---|
| P08 Estado del local | P08a "Escribe cuál es el estado del local" | Minuta, pasos a seguir (seguimiento si está en obra o por inaugurar) |
| P09 Tipo de negocio | **P09a** "¿Qué tipo de negocio es?" | Minuta; opciones de áreas de P11 |
| P11 Áreas del local | P11b (+ P11c ¿es de preparación?) | P11a, P27, P28, P46, comandas y Conectividad |
| P11a Ubicación del área | P11d | Minuta y mapa |
| P12c Barra → caja | P12d | Minuta y mapa |
| P13 Proveedor de internet | P13a | Minuta (sección internet) |
| P22 Equipo de caja | P22f | Decisión de impresión (queda por confirmar, R52) |
| P28 Ubicación de la impresora | **P28a** "¿En qué área o ubicación está la impresora?" | Minuta, mapa y validación de segmento (R55) |
| P29 Marca de la impresora | **P29a** "¿Cuál es la marca de la impresora?" | Minuta e inventario de equipos |

En el Excel, cuando la minuta manual trae un valor fuera del catálogo, se escribe `OTRO: texto` en la columna del valor.

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
  "text": "Caja {i}: el dispositivo que usan en caja es una tablet. Es obligatorio un kit Raspberry para el controlador de impresiones y las impresoras deben ser de red. Cotízalo con Desarrollo de Negocios.",
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

// 12) P09: opciones por rubro y tipo de comida (ver sección 4c)
// 13) P13: si eligen Otro, escribir el nombre
{ "id": "P13a", "kind": "question", "block": "C", "inputType": "TEXT",
  "text": "¿Cuál es el nombre del proveedor de internet?", "field": "internet.proveedorOtro",
  "showIf": {"eq": ["$P13", "OTRO"]}, "next": "P14" }

// 14) P11: opciones según el tipo de negocio y áreas escritas a mano
"options": { "from": "areasBase",
             "plusIf": [ { "if": {"in": ["$P09", ["DISCOTECA_BAR","RESTOBAR"]]},
                           "add": [ {"value":"BARRA_1","label":"Barra 1"}, {"value":"BARRA_2","label":"Barra 2"},
                                    {"value":"BARRA_3","label":"Barra 3"}, {"value":"COCINA_1","label":"Cocina 1"},
                                    {"value":"COCINA_2","label":"Cocina 2"} ] } ] }
{ "id": "P11b", "kind": "question", "block": "B", "inputType": "TEXT", "repeatable": true,
  "text": "Escribe el nombre del área (ej. Cocina 2, Barra 2, Barra 3). Puedes agregar varias.",
  "field": "negocio.areasOtras", "showIf": {"includes": ["$P11", "OTRO"]}, "next": "P11c" }
{ "id": "P11c", "kind": "question", "block": "B", "inputType": "YES_NO",
  "text": "{area}: ¿es un área de preparación (salen comandas ahí)?", "field": "negocio.areaPreparacion[{area}]",
  "loopOver": "$P11b", "next": "L_PISOS" }

// 15) P11a (piso de cada área, incluidas las escritas a mano): Piso 1…N + Terraza + Otro
"options": { "range": {"from": 1, "to": "$P10", "label": "Piso {n}"},
             "plus": [ {"value":"TERRAZA","label":"Terraza"}, {"value":"OTRO","label":"Otro (escribir la ubicación)"} ] }
{ "id": "P11d", "kind": "question", "block": "B", "inputType": "TEXT",
  "text": "¿Dónde está {item.label}? Escribe la ubicación.", "field": "negocio.areaUbicacionOtra[{item.value}]",
  "showIf": {"eq": ["$P11a", "OTRO"]}, "next": "@loop.next" }

// 16) Barra → caja (solo si hay más de una caja)
{ "id": "L_BARRAS", "kind": "loop", "block": "B", "over": {"list": "$derived.barras"},
  "showIf": {"gt": ["$P12", 1]}, "body": "P12c", "exit": "P13" }
{ "id": "P12c", "kind": "question", "block": "B", "inputType": "CHOICE",
  "text": "¿A qué caja está asociada {item.label}?", "field": "negocio.barraCaja[{item.value}]",
  "options": { "range": {"from": 1, "to": "$P12", "label": "Caja {n}"},
               "plus": [ {"value":"OTRO","label":"Otro (escribir)"} ] },
  "next": { "rules": [ { "if": {"eq":["$P12c","OTRO"]}, "goto": "P12d" } ], "default": "@loop.next" } }
{ "id": "P12d", "kind": "question", "block": "B", "inputType": "TEXT",
  "text": "¿A qué está asociada {item.label}?", "field": "negocio.barraCajaOtra[{item.value}]", "next": "@loop.next" }

// 17) E5 (autotest de impresora): validar el segmento contra el router al subir la foto
"effects": [ { "crossCheck": "segmento(E5.ip) == segmento(E4.puertaEnlace || E3.ipGestion || E8.gateway)",
               "onFalse": "La impresora de {P28.label} está en {segmento(E5.ip)} y el router en {segmento(router)}. Cambia su IP a una del segmento del router y vuelve a subir el autotest." } ]
// E8: con la IP del router (E4) se marca cada dispositivo como dentro o fuera del segmento.

// 18) P23 (hay PC o laptop): agregar el efecto del controlador
"effects": [{ "if": {"eq":["$derived.necesitaComandas",true]}, "addAction": "Caja {i}: instalar el controlador de impresiones en la PC o laptop." }]

// 19) Regla general "Otro": el motor la aplica a toda pregunta CHOICE o MULTI_SELECT con opción OTRO/OTRA
"defaults": { "otherText": { "enabled": true, "required": true,
              "prompt": "Escribe cuál es ({question.label})",
              "saveAs": "{field}Otro",            // y la respuesta queda como "OTRO: <texto>"
              "reuseAsOption": true } }             // el texto aparece como opción en las preguntas siguientes
{ "id": "P09a", "kind": "question", "block": "B", "inputType": "TEXT",
  "text": "¿Qué tipo de negocio es?", "field": "negocio.tipoOtro",
  "showIf": {"eq": ["$P09", "OTRO"]}, "next": "P10" }
{ "id": "P28a", "kind": "question", "block": "E", "inputType": "TEXT",
  "text": "Impresora {i}: ¿en qué área o ubicación está?", "field": "impresoras[{i}].areaOtra",
  "showIf": {"eq": ["$P28", "OTRA"]}, "next": "P29" }
{ "id": "P29a", "kind": "question", "block": "E", "inputType": "TEXT",
  "text": "Impresora {i}: ¿cuál es la marca?", "field": "impresoras[{i}].marcaOtra",
  "showIf": {"eq": ["$P29", "OTRO"]}, "next": "P30" }
```

Notas para implementar:
- **Motor:** se necesitan el operador `includesAny`, la condición `showIf` para preguntas condicionales y un bloque `derived` que se recalcule en cada paso.
- **Validación de puertos con la IA:** en la tarjeta de E2, el técnico confirma o corrige los puertos leídos, igual que con las demás evidencias.
- **Bloque E:** si una impresora queda USB y el equipo de caja es un Raspberry, avisar "La impresora USB no funciona con Raspberry".
- **Área "Otro" en P11:** preguntar si es de preparación.
- **Opción "Otro" en general:** con `otherText` en los valores por defecto del motor, las preguntas nuevas con "Otro" piden el texto sin tener que crear un nodo a mano. La minuta y `legacy_adapter.py` deben leer `{campo}Otro` y mostrar el texto escrito.
- **Minuta y mapa:** deben mostrar dónde va el controlador, si va Sunmi, el resultado de la prueba del chip y los puntos de red y energía que faltan.

### Minutas que no coinciden con las reglas (hoja `revisar_vs_regla`)

- **RKT Banchero y Villa Flores:** se recomendó impresora de red para caja aunque hay PC de caja. Con la regla, la USB conectada a la PC con controlador imprime todo.
- **Gelato Alore (3 sedes):** ya coinciden. La caja es una tablet, así que el Raspberry es obligatorio (R47). Sus recomendaciones quedaron unidas a R47.

## 4c. Preguntas por rubro: perfiles (confirmado 2026-10-10)

**Objetivo:** que el técnico termine la minuta más rápido. El rubro se elige al inicio y el chat carga un **perfil** que sugiere las áreas, oculta lo que no aplica y arma las impresoras.

### P09: rubro y tipo de comida en una sola pregunta

Restaurante – Pescados y mariscos · Restaurante – Comida oriental (makis, ramen, sushi) · Restaurante (otro tipo de comida) · Chifa · Pizzería · Parrilla / Anticuchería · Pollería · Fast food · Restobar · Discoteca o bar · Cafetería · Heladería · Hotel · Retail – Supermercado · Otro (escribir).

- **"Otro"** pide escribir el rubro (P09a, regla general de "Otro").
- **El patio de comidas no se pregunta:** se deduce de la dirección o ubicación (P03), que el técnico siempre escribe. Ver "Red independiente con TP-Link y chip (R56)".

### Cómo funciona el perfil

1. El técnico elige el rubro (P09) y con eso se carga el perfil. Si la dirección (P03) menciona el patio de comidas de un centro comercial grande, se suma el modificador de patio sin preguntar.
2. Las áreas sugeridas se confirman con un toque (P11e). Con 'Agregar o quitar' se ve la lista completa, con Otro para escribir.
3. P12e: ¿la caja comparte lugar con alguna área de preparación? Opciones: No (aparte), las áreas de preparación del perfil y Otro. El perfil deja marcada la más común.
4. Las impresoras vienen armadas: una por área de preparación más la de caja (o una sola si la caja comparte área). Solo se pregunta marca, conexión y autotest.
5. Las preguntas ocultas se guardan como 'No aplica (por rubro)': la minuta y la evaluación no las cuentan como faltantes.
6. Si el técnico agrega algo fuera del perfil (ej. Barra 2 en un fast food), vuelven a aparecer las preguntas relacionadas.
7. Nunca se ocultan: evidencias principales (speedtest, router, ipconfig, autotest, escáner), energía, puntos de red, decisión de impresión y cierre.

### Perfiles

| Rubro | Áreas de preparación sugeridas | Zonas de atención sugeridas | La caja comparte área con (P12e) | Se oculta | Se agrega / se activa |
|---|---|---|---|---|---|
| **Restaurante – Pescados y mariscos** | Cocina, Barra | Salón 1, Para llevar, Delivery | Barra (Caja-Barra, una impresora) | Barras múltiples (Barra 1–3), barra → caja (P12c/P12d) | P11e, P12e; — |
| **Restaurante – Comida oriental (makis, ramen, sushi)** | Cocina, Makis, Ramen, Frituras, Salteados | Salón 1, Barra, Para llevar, Delivery | Aparte | Barras múltiples | P11e, P12e; impresoras ya armadas (una por área); R58 (switch de 1000 Mbps), R20/R53 (cámaras en el mismo switch) |
| **Restaurante (otro tipo de comida)** | Cocina, Barra | Salón 1, Salón 2, Terraza | Aparte | — | P11e, P12e; — |
| **Chifa** | Cocina (wok), Barra de bebidas | Salón 1, Salón 2, Para llevar, Delivery | Aparte | Barras múltiples, barra → caja | P11e, P12e; — | *(supuesto, sin minutas)*
| **Pizzería** | Cocina, Horno o zona de pizza (PIZZA), Barra opcional | Salón, Para llevar, Delivery | Aparte | Barras múltiples, barra → caja | P11e, P12e; — | *(supuesto, sin minutas)*
| **Parrilla / Anticuchería** | Parrilla, Cocina, Barra opcional | Salón 1, Terraza, Para llevar, Delivery | Parrilla (Caja-Parrilla); puede ser Caja-Barra | Barras múltiples | P11e, P12e; R10 (calor y grasa en parrilla y cocina, también router y Raspberry), R57 |
| **Pollería** | Cocina, Brasas, Barra de bebidas | Salón 1, Salón 2, Para llevar, Delivery | Aparte | Barras múltiples, barra → caja | P11e, P12e; R10 (calor y grasa en la zona de brasas) |
| **Fast food** | Cocina, Barra opcional | Salón, Para llevar, Delivery | Aparte | P10 (pisos), P11a (piso por área), barras múltiples, barra → caja; P45/P46 si P09c = No | P11e, P12e, P09c (mozos); R44 (Sunmi si la comanda sale solo en caja) |
| **Restobar** | Cocina, Barra 1, Barra 2 | Salones, Terraza, Barras | Aparte | — (flujo completo) | P11e, P12e; P12c barra → caja si hay más de una caja; — |
| **Discoteca o bar** | Barra 1, Barra 2, Barra 3, Cocina opcional | Pista, Salones, Barras | Aparte | Brasas, despacho | P11e, P12e, P12c (barra → caja), P22g (PC o laptop permanente); R60 (PC o laptop temporal → Raspberry) |
| **Cafetería** | Barra de café, Cocina opcional | Salón, Para llevar, Delivery | Aparte | P10, P11a, barras múltiples; P45/P46 si P09c = No | P11e, P12e, P09c (mozos); R47 (tablet en caja → Raspberry) |
| **Heladería** | Barra de helados, Cocina opcional | Salón, Para llevar, Delivery | Aparte | P10, P11a, barras múltiples; P45/P46 si P09c = No | P11e, P12e, P09c (mozos); R47 (tablet en caja → Raspberry) |
| **Hotel** | Cocina, Bar | Restaurante (salón), Terraza, Áreas comunes | Recepción | — (flujo completo) | P11e, P12e; R56 (TP-Link con chip si no hay proveedor o hay caídas) |
| **Retail – Supermercado** | Ninguna (solo cajas) | Ninguna (venta en caja) | — | P11 de preparación, P12b, P25, impresoras de preparación, decisión del Raspberry, P45/P46 | P22h (periféricos por caja); R63 (periféricos y pruebas); impresora USB con PC (R42) |
| **Otro (escribir)** | — | — | Se pregunta | Nada (flujo completo) | P09a (escribir el rubro); — |

**Patio de comidas de un centro comercial grande (se deduce de la dirección), se suma a cualquier rubro:**
- **Se oculta:** P13, P14, P15, P16, P17 (internet del patio); foto, etiqueta y speedtest del router del patio; P45/P46 (WiFi por zona).
- **Se agrega:** P14b (señal del chip), E1b (speedtest del chip), ubicación del TP-Link.
- **Se activa:** R56: independizar la red con TP-Link con chip y plan básico, solo para las impresoras y el equipo del sistema de impresiones.
- **Minutas:** Sicilia, Bros y Soma (El Refugio), Saltao (Larcomar), Maki Sam (Real Plaza).

**Caja compartida (P12e).** La caja puede estar en un área de preparación: Caja-Barra (pescados y mariscos: Don Chalán, El Chito), Caja-Parrilla (Anticuchería Susy) u otra. El chat pregunta "¿La caja comparte lugar con alguna área de preparación?" con las opciones: No (aparte), las áreas de preparación del perfil y Otro. El perfil deja marcada la más común. Si comparte, se arma **una sola impresora** para esa área y la caja: comandas, pre-cuentas y comprobantes.

**Canales y zonas de atención (glosario, confirmado 2026-10-11).** Primero se eligen los **canales de atención** (selección múltiple) y luego, por cada canal, sus **zonas de atención** (selección múltiple con sugeridas + **Otro**, que se puede usar varias veces y combinar con las sugeridas):

| Canal | Zonas sugeridas (todas con **Otro (escribir)**) |
|---|---|
| **Mesa** | Las del perfil del rubro: Salón 1, Salón 2, Terraza, Balcón, Privado, Patio, Pista, Barra… |
| **Recojo** | Caja, Mostrador, Ventanilla |
| **Para llevar** | Mostrador, Ventanilla, Caja |
| **Backoffice** | Administración, Almacén |
| **Delivery** | Despacho, Ventanilla, Caja |
| **Otro** (canal escrito) | Solo Otro |

- Los canales sirven para **configurar el punto de venta** y ordenar la minuta: "Canales de atención: Mesa (Salón Principal, Terraza), Delivery (Despacho) y Recojo (Caja)".
- **La cobertura WiFi no depende del canal:** se revisa en **todas las zonas de atención y en todos los lugares de preparación**.
- **Fotos obligatorias:** una por zona de atención; si la zona es la propia **Caja**, basta la foto de la caja (ver `../analisis/EVIDENCIAS_Y_CONECTIVIDAD.md`).
- La columna "Zonas de atención sugeridas" de la tabla de perfiles corresponde al canal **Mesa**.

### Red independiente con TP-Link y chip (R56, confirmada 2026-10-10)

Aplica a **cualquier rubro** si se cumple alguna de estas condiciones:

| Condición | De dónde sale |
|---|---|
| Caídas de internet | P17 = Sí |
| Sin proveedor de internet | P14 = No tiene o No sé |
| Patio de comidas de un **centro comercial grande** | Se deduce de la dirección o ubicación (P03); no se pregunta. Se reconoce por el nombre, no por "C.C.": Patio de comidas / patio gastronómico, El Refugio, Larcomar, Real Plaza, Jockey Plaza, Plaza Norte, MegaPlaza, Open Plaza, Plaza San Miguel, Mall (Mall Aventura, Mall del Sur…). Un C.C. pequeño no cuenta |

**Recomendación:** independizar la red con un TP-Link con chip y plan básico, solo para las impresoras y el equipo donde va el sistema de impresiones. **Reemplaza** al internet del local para la impresión; no es un respaldo. El chip (Entel o Claro) sale de la prueba de señal con el celular (P14b).

- **Excepción:** sin internet y sin comandas, con un Sunmi que ya trae su chip, no hace falta el TP-Link (caso I3 de `kb_decision_internet`).
- **R01** (respaldo 4G por caídas) y **R45** (router con chip sin fibra ni coaxial) quedan **inactivas**, unificadas en R56. Las minutas que las usaban ahora apuntan a R56.

### Preguntas nuevas

```jsonc
// P09: opciones nuevas (rubro + tipo de comida)
"options": [{"value":"REST_PESCADOS"}, {"value":"REST_ORIENTAL"}, {"value":"RESTAURANTE"}, {"value":"CHIFA"}, {"value":"PIZZERIA"}, {"value":"PARRILLA"}, {"value":"POLLERIA"}, {"value":"FAST_FOOD"}, {"value":"RESTOBAR"}, {"value":"DISCOTECA_BAR"}, {"value":"CAFETERIA"}, {"value":"HELADERIA"}, {"value":"HOSPEDAJE"}, {"value":"RETAIL"}, {"value":"OTRO"}]
// Patio de comidas: NO es pregunta. Dato derivado de la dirección (P03).
"derived": { "centroComercial": { "regex": ["$P03", "patio\\s+(de\\s+comidas?|gastron)|refugio|larcomar|real\\s*plaza|jockey|plaza\\s*norte|mega\\s*plaza|open\\s*plaza|plaza\\s*san\\s*miguel|\\bmall\\b"] } }   // + la IA puede reconocer otros centros comerciales grandes por el nombre
// R56: TP-Link con chip (red independiente para la impresión) si hay caídas, no hay proveedor o está en un patio de comidas grande
{ "if": {"or":[{"eq":["$P17","SI"]},{"in":["$P14",["NO_TIENE","NO_SE"]]},{"eq":["$derived.centroComercial",true]}]},
  "addAction": "Independizar la red con un TP-Link con chip {P14b.label} y plan básico, solo para las impresoras y el sistema de impresiones." }
{ "id": "P09c", "kind": "question", "block": "B", "inputType": "YES_NO",
  "text": "¿Hay mozos que toman pedidos con tablet o celular?", "field": "negocio.mozosConTablet",
  "showIf": {"in": ["$P09", ["FAST_FOOD","CAFETERIA","HELADERIA"]]} }   // si es No, se ocultan P45/P46
{ "id": "P11e", "kind": "question", "block": "B", "inputType": "CHOICE",
  "text": "Un {perfil.label} normalmente tiene: {perfil.areas}. ¿Es así?", "field": "negocio.areasConfirmadas",
  "options": [{"value":"CONFIRMAR","label":"Sí, es correcto"}, {"value":"EDITAR","label":"Agregar o quitar áreas"}],
  "effects": [{ "if": {"eq":["$P11e","CONFIRMAR"]}, "set": {"P11": "$perfil.areas"} }],
  "next": { "rules": [ { "if": {"eq":["$P11e","EDITAR"]}, "goto": "P11" } ], "default": "P12" } }
{ "id": "P12e", "kind": "question", "block": "B", "inputType": "CHOICE",
  "text": "¿La caja comparte lugar con alguna área de preparación?", "field": "negocio.cajaCompartida",
  "options": { "from": "$derived.areasPreparacion", "plus": [{"value":"NO","label":"No, la caja está aparte"}, {"value":"OTRO","label":"Otro (escribir)"}] },
  "default": "$perfil.cajaComparte" }
{ "id": "P22g", "kind": "question", "block": "D", "inputType": "YES_NO",
  "text": "Caja {i}: ¿la PC o laptop se queda en el local de forma permanente?", "showIf": {"eq":["$P22","SI"]} }   // No → R60 (Raspberry)
{ "id": "P22h", "kind": "question", "block": "D", "inputType": "MULTI_SELECT",
  "text": "Caja {i}: ¿qué periféricos tiene?", "showIf": {"eq":["$P09","RETAIL"]},
  "options": [{"value":"BALANZA"}, {"value":"ESCANER"}, {"value":"GAVETA"}, {"value":"IMPRESORA_ETIQUETAS"}, {"value":"POS_PAGO"}, {"value":"OTRO"}] }

// Motor: cada pregunta puede tener "hideFor": [rubros]. Si se oculta, se guarda "NO_APLICA_RUBRO".
// Si el técnico agrega un área fuera del perfil (ej. BARRA_2 en FAST_FOOD), las preguntas relacionadas vuelven a mostrarse.
// L_IMPRESORAS se pre-arma con una impresora por área de preparación (+ caja, o una sola si P12e ≠ NO);
// por impresora solo se pregunta P29 (marca), P30 (conexión) y E5 (autotest).
```

### Reglas nuevas

| Regla | Estado | Qué recomienda | Minutas |
|---|---|---|---|
| **R56** Red independiente con TP-Link y chip | ✅ Confirmada | TP-Link con chip y plan básico, solo para impresoras y el equipo del sistema de impresiones. Reemplaza al internet del local para la impresión (no es respaldo). Unifica R01 y R45 | Sicilia, Bros, Soma, Saltao, Maki Sam, Malala, Don Chalán |
| **R57** Raspberry junto al router | ✅ Confirmada | Junto al router o switch, cable corto, ventilado, lejos de calor y grasa | Anticuchería Susy |
| R58 Switch de 1000 Mbps | Propuesta | Cambiar el switch de 10/100 por uno gigabit | Shimaya (6), El Chito |
| R59 Ponchado norma B | Propuesta | Ponchar con T568B (no cruzado) | Shimaya Los Olivos |
| R60 PC o laptop temporal | Propuesta | Tomarlo como "sin PC": Raspberry | Mala Vida, Omakase |
| R61 Puertos del router | Propuesta | Probar los puertos y no mover la impresora de puerto | Belgofre |
| R62 Cables de cámaras | Propuesta | Lejos de la corriente y de equipos con motor | Modonese |
| R63 Retail | Propuesta | Probar periféricos (balanza, escáner, gaveta, etiquetas, POS) | Flora y Fauna |
| R64 Reestructurar cableado | Propuesta | Canaletas y rotulado; no impide el inicio | La Carpita, Shimaya Comas |

**R65–R67 (2026-10-11):** velocidad limitada a 100 Mbps (8 minutas con pruebas de 88 a 94 Mbps), saturación con carga y plan por debajo del 70 %. Detalle en `../analisis/EVIDENCIAS_Y_CONECTIVIDAD.md`.

Las minutas también refuerzan reglas que ya existían: R37 (RJ45 desgastado, 6 minutas), R06 (punto de red cerca de la impresora, 11 minutas), R16 (impresora USB con la PC de caja, todos los Shimaya con USB), R55 (segmento: Flora y Fauna, Maki Sam).

## 4d. Estado del local, pasos a seguir, observaciones y anexos (confirmado 2026-10-10)

### P08: estado del local (reemplaza al motivo de visita)

Opciones: En operación · En obra · Por inaugurar · Ya cuenta con un sistema de punto de venta anterior · Otro (escribir). Con "Otro" el técnico escribe cuál es (P08a) y se guarda como una opción más (regla general de "Otro").

- Si el local está **En obra** o **Por inaugurar**, se agrega el paso "Dar seguimiento a la instalación hasta la fecha de inicio" (S10). Lo que falta (puntos de red, energía) queda como trabajo pendiente con segunda visita.
- Las minutas manuales se usan para el análisis, no para separar visitas por motivo.

**Antecedentes automáticos.** La sección Antecedentes de la minuta se escribe sola con el estado del local (hoja `kb_texto_minuta`, clave `ANTECEDENTES`):

> La visita se realiza para la implementación del punto de venta, donde el local se encuentra en estado de "{estado}". En la visita se realizarán acciones como validar los equipos tecnológicos y de red y el estado de los mismos, y se le recomendará una mejor infraestructura si lo necesitara.

- `{estado}` es la opción elegida en P08. Si es "Otro", va el texto que escribió el técnico (P08a).
- Ejemplo con "En obra": *La visita se realiza para la implementación del punto de venta, donde el local se encuentra en estado de "En obra". En la visita se realizarán acciones como validar los equipos tecnológicos y de red y el estado de los mismos, y se le recomendará una mejor infraestructura si lo necesitara.*

### Pasos a seguir: se generan solos y el técnico los edita

Reemplaza a P52 "Acuerdos y siguientes acciones" (hoy es texto libre). Al terminar el chat, el asistente arma la lista separada en **Por parte del cliente** y **Por parte de ID** (hoja `kb_accion_siguiente`):

| Paso | Para | Texto | Cuándo se agrega | Minutas que lo traen |
|---|---|---|---|---|
| S01 | ID | Compartir la minuta de visita técnica. | Siempre | 36 |
| S02 | CLIENTE | Confirmar la recepción de la minuta. | Siempre | 36 |
| S03 | CLIENTE | Solicitar la cotización de los equipos recomendados: {lista de equipos con cantidad}. | Hay equipos recomendados (sale de las recomendaciones y su equipo_id) | 7 |
| S04 | ID | Enviar la cotización de {equipos que vende ID}. Raspberry y Sunmi se cotizan con Desarrollo de Negocios. | Se recomendó Raspberry, Sunmi u otro equipo que vende ID | 6 |
| S05 | CLIENTE | Enviar el documento con la carta (maestra de productos) y la información del negocio: serie de facturación, IGV y usuarios. | Siempre (visita para implementar IDPos) | 11 |
| S06 | CLIENTE | Implementar {trabajos pendientes: puntos de red, energía, switch, impresora} y avisar a ID cuando estén listos. | Hay trabajos pendientes del cliente | 5 |
| S07 | ID | Realizar una segunda visita técnica cuando estén implementados {equipos o trabajos pendientes}. | Hay trabajos pendientes del cliente o P50 = Sí | 3 |
| S08 | ID | Dar inicio con IDPos el {fecha de inicio}. | El técnico indica la fecha de inicio (P50b) | 4 |
| S09 | ID | Configurar el sistema de impresiones en {PC, laptop o Raspberry}. | Hay controlador de impresiones por instalar | 3 |
| S10 | ID | Dar seguimiento a la instalación hasta la fecha de inicio. | Estado del local = En obra o Por inaugurar | 4 |
| S11 | CLIENTE o ID | {Texto que escribe el técnico} | El técnico agrega una acción (Otro) | — |

- Los equipos de la cotización (S03, S04) salen de las recomendaciones, con nombre y cantidad. Ejemplo: "Solicitar la cotización de los equipos recomendados: kit Raspberry, switch de 8 puertos y TP-Link con chip".
- **P52a:** el técnico revisa la lista y puede quitar un paso, editar su texto o agregar uno (elige Cliente o ID y lo escribe).
- **P50b (opcional):** fecha de inicio acordada. Si la hay, se agrega "Dar inicio con IDPos el {fecha}".

### Anexos: video del local (apartado fijo)

No es una pregunta de sí o no: es un **apartado que siempre aparece** al cierre (P49a), con esta instrucción:

> Graba parte del local y dónde están los equipos de red (router e impresoras) y dónde se colocaría el sistema de impresiones. Sube el video a Drive o YouTube y pega aquí el enlace.

- Tiene casillas para pegar uno o varios enlaces (Drive, YouTube) y, opcional, la zona de cada uno.
- **Si el técnico no pega ningún enlace, puede continuar** con la minuta.
- **En el PDF:** si hay enlaces, sale la sección **Anexos** con "Video del local: {enlace} ({zona})". **Si no hay enlaces, la sección no aparece**: ni el título "Anexos" ni un texto como "No hay video".
- La IA no analiza el video. En el Excel se registra en `kb_minuta_evidencia` con código `VIDEO`.

### Observaciones: después del análisis, fuera del chat

Al abrir el análisis, la app muestra un aviso: **"Antes de cerrar, agrega tus observaciones"**. Se abre un apartado con **selección múltiple** (hoja `kb_observacion_opcion`):

| Opción | Tipo | Valores |
|---|---|---|
| Observaciones del técnico | Texto | — |
| Serie de facturación | Texto | — |
| ¿El cliente acepta la recomendación? | Opción | Sí · No · Otro (escribir) |
| Método de pago | Selección múltiple | Niubiz · Izipay · Culqi · Otro (escribir) |
| Otro | Texto | El técnico escribe el nombre y el contenido |

- En la minuta, **Observaciones** va **después de Recomendaciones**.
- **Método de pago** sale en la minuta así: *"El método de pago que utilizan en el local es el siguiente: Niubiz y Culqi."* Opciones: Niubiz, Izipay, Culqi y Otro; con Otro el técnico escribe cuál es.
- Si el cliente **no acepta** la recomendación, se refuerza el aviso de viabilidad.
- Orden de la minuta: Datos generales → Antecedentes (automático) → Desarrollo y hallazgos → Análisis (puntajes) → Recomendaciones → Observaciones (nuevo) → Aviso de viabilidad → Pasos a seguir: cliente / ID (editable) → Anexos: enlaces de video (nuevo).
- Las observaciones que ya traían las minutas manuales están en la hoja `kb_minuta_observacion`: Culqi (Verbena), Niubiz (Flora y Fauna), Izipay (Los Juárez) y Omakase, que no aceptó el Raspberry.

```jsonc
{ "id": "P08", "inputType": "CHOICE", "text": "¿Cuál es el estado del local?",
  "options": [{"value":"EN_OPERACION"}, {"value":"EN_OBRA"}, {"value":"POR_INAUGURAR"}, {"value":"SISTEMA_ANTERIOR"}, {"value":"OTRO"}],
  "next": { "rules": [ { "if": {"eq":["$P08","OTRO"]}, "goto": "P08a" } ], "default": "P09" } }
{ "id": "P49a", "kind": "section", "inputType": "URL_LIST", "alwaysShown": true, "required": false, "repeatable": true,
  "fields": ["enlace", "zona (opcional)"], "help": "Graba parte del local y dónde están los equipos de red (router e impresoras) y dónde se colocaría el sistema de impresiones. Sube el video a Drive o YouTube y pega aquí el enlace." }   // PDF: la sección Anexos solo se imprime si hay al menos un enlace
{ "id": "P50b", "inputType": "DATE", "optional": true, "text": "¿Hay una fecha de inicio acordada con el cliente?" }
{ "id": "P52a", "inputType": "EDITABLE_LIST", "source": "$derived.pasosASeguir",
  "itemFields": ["para (CLIENTE | ID)", "texto"], "allowAdd": true, "allowRemove": true, "allowEdit": true }
// Pantalla de análisis (app): botón "Agregar observaciones" + aviso al abrir; se guarda en minuta.observaciones[]
```

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
- La hoja kb_minuta_credencial del Excel tiene credenciales: cárgalas cifradas con pgcrypto y la llave KB_CRED_KEY desde una variable de entorno. Nunca imprimas las claves en logs ni en el reporte.

TAREAS:
1. Crea las tablas kb_* y la vista v_kb_regla_frecuencia de la sección 2 (entidades JPA en el gateway, paquete kb, o migración SQL; propón cuál según el proyecto).
2. Implementa el importador local de la sección 3 (dry-run por defecto, se niega a correr contra hosts remotos, validaciones y transacción) con su test.
3. Carga el Excel en la base local y muéstrame el reporte y SELECT * FROM v_kb_regla_frecuencia.
4. Propón (sin implementar todavía) el cambio de la sección 4 en proposal.py.
```

## 7. Supuestos y pendientes

- **Estado del local (antes, motivo de visita).** Desde 2026-10-10, P08 es el estado del local. En las minutas cargadas solo se llenó donde la minuta lo deja claro (12 minutas, marcado INFERIDO); las demás quedan en blanco. El texto del antiguo motivo pasó a la columna `antecedentes`. Las minutas sirven para el análisis, no para separar visitas por motivo (no nos enfocamos en soporte).
- **Rock & Burgers.** Se cargó con los datos del PDF manual, no con los del documento generado por la app (`rock_burgers_document.json`). Por eso no tiene velocidad contratada ni cajas.
- **Credenciales (CONFIDENCIAL, pedido del equipo 2026-10-10).** Son parte de la lectura de la evidencia, así que se cargan en la hoja `kb_minuta_credencial` (19 registros de 14 minutas): claves WiFi (incluidos los QR de Gelato Alore, leídos de la imagen), clave del TP-Link de Saltao, usuario de la laptop de Mala Vida, AnyDesk de Rock & Burgers, RKT Hacienda y Maki Sam Real Plaza, y el puerto del Raspberry de Saltao.
  - Solo van en esa hoja del Excel y en la tabla `kb_minuta_credencial` (clave cifrada, acceso solo ADMIN). No se escriben en los `.md`, el código ni los logs.
  - **El Excel no tiene contraseña** y se sube al repositorio para mantenerlo actualizado. El repositorio debe ser privado.
  - Hoy la app oculta las credenciales de la etiqueta del router (E3, regla V09). Ese comportamiento no cambia mientras el equipo no lo decida.
- **Don Oscar.** La versión del PDF que llegó el 2026-10-06 tiene el mismo contenido que MH-002; no se duplicó.
- **Proveedores fuera del catálogo** (Yachay Telecomunicaciones, WOW, Hughesnet): se cargaron como `OTRO`, con el nombre en `observaciones_carga`.
- **Tipo de conexión (P14):** cuando la minuta no lo dice, queda **en blanco**, no "No sé", porque "No sé" ahora significa sin internet.
  - Malala quedó como "No tiene".
  - Hotel La Confianza (satelital, Hughesnet) quedó en blanco: P14 no tiene opción para satelital.
- **Equipo de caja (P22):** las respuestas se pasaron a los códigos nuevos. PC pasó a "Sí", y Gelato Alore quedó como "Tablet".
- **Fotos.** `kb_minuta_evidencia` solo referencia las fotos y videos, no los sube. Si más adelante se quieren las imágenes, deben ir a un almacenamiento local de pruebas, nunca al de producción.
- **Lote del 2026-10-10 (MH-019 a MH-044):**
  - **No se cargaron:** T&C LEOMAR y T&C Puerto Máncora (son contratos; LEOMAR trae cuentas bancarias), Summary Sheet RKT, y Juicy Lucy – Larco (y su V2), que tienen el texto de Bros. Juicy Lucy queda pendiente de su minuta (hoja `archivos_no_cargados`).
  - **Credenciales:** en la hoja `kb_minuta_credencial` (ver arriba). Las cuentas bancarias del T&C LEOMAR no se cargan.
  - **Por confirmar:** si Saltao está en Larcomar (como dice la minuta) o en El Refugio, y si Soma (El Refugio) está en patio de comidas.
  - **Gourmety:** el archivo dice 11.03.22, pero la fecha de la visita es 2025.



## 4e. Ajustes del 2026-10-11 (probados en el simulador)
| Impresora USB en caja (R16) | Si la impresora de caja es USB y está conectada a la PC o laptop de caja: **"Mantener la impresora USB conectada a la PC o laptop de caja con el controlador de impresiones"**, sin cambiarla por una de red. Aprendida de la prueba retrospectiva (3 de 3 minutas). Estado: confirmada (2026-10-11). |
| Referencias (Análisis IA) | Sección oculta **Referencias**: 3 casos más parecidos con % de similitud, "visto en N minutas" por recomendación y sugerencias frecuentes en casos parecidos con **Agregar a la propuesta**. **Nunca** en la minuta ni en el PDF. Detalle en `analisis/PRUEBA_RETROSPECTIVA.md`. Estado: confirmada (2026-10-11). |
| Validación de evidencias | Cada foto se compara con el dataset de referencias del tipo pedido (hojas `kb_evidencia_tipo` y `kb_evidencia_referencia`). Si no coincide: **"La imagen no corresponde a lo solicitado ({evidencia}): se parece a {tipo detectado}"**. Detalle en `analisis/VALIDACION_EVIDENCIAS.md`. Estado: confirmada (2026-10-11). |
| Eliminar una lectura | En "Esto leí en la captura" cada lectura tiene una **X roja**: al tocarla se elimina y **no aparece** en el chat, la minuta ni el anexo (se guarda en `data._del`; las reglas la toman como dato no leído). **Restaurar** devuelve lo que leyó la IA. Si se eliminan todas, la foto queda como evidencia sin datos. Estado: confirmada (2026-10-11). |
| Vista previa / Propuesta PDF | Se muestra en **hojas A4** con los mismos cortes que el PDF (márgenes 24/15/18 mm, anexo en hoja nueva, tablas cortadas entre filas, título no queda solo al pie). **Compaginado** "‹ Página n de N ›" y deslizar a los lados. La hoja se ajusta al ancho: si no entra, se **desliza hacia abajo**. **Zoom** con dos dedos, doble toque o − / + (100 % a 400 %); con zoom se desplaza en las dos direcciones. Estado: confirmada (2026-10-11). |

| Tema | Regla |
|---|---|
| Tablet en caja | El aviso dice: "Caja {i}: **el dispositivo que usan en caja es una tablet**. Es obligatorio un kit Raspberry…" (no "la caja es una tablet"). |
| R56 en la minuta | Se escribe **"TP-Link con entrada de chip + Chip {proveedor con buena señal en P14b}"** y el plan: **plan básico** sin cámaras, **plan superior** si hay cámaras conectadas o por conectar. Con "Ambos": "Chip Entel o Claro". |
| Energía (P33 = No) | Nueva **P33a**: "Indícame qué dispositivos no tienen energía cerca para lograr conectarlos". El técnico marca los equipos (router, TP-Link, switch, PC/Raspberry de caja, IMP. …) y se arma un **texto automático editable** ("No hay tomas de energía cerca de: … Se deben instalar tomas para conectarlos."); puede borrarlo y escribir el suyo. El texto va a la minuta y los equipos marcados llevan **rayo rojo** en el mapa. |
| Cámaras | P47: "¿Hay cámaras de seguridad conectadas a la red **o se van a conectar**?" (incluye el TP-Link con chip). Si es Sí, **P47a** "¿Cuántas cámaras irán conectadas a la red?" 1 · 2 · 3 · 4 · Otro (número). R53: si van a la misma red, **subir a un plan superior** que soporte las cámaras **o independizar la red de las cámaras de la de los equipos de impresión**. Si están en el TP-Link con chip, el chip lleva plan superior. |
| Anexos (P49a) | "Pega el enlace (**Drive, OneDrive**, YouTube…)". |
| Fecha de inicio (P50b) | Se escribe o se elige con el botón **Calendario**. |
| Pasos a seguir (P52a) | Botones **Deshacer** y **Rehacer** junto a "Confirmar pasos". Al tocar un paso, su texto completo aparece en la caja de texto del chat (varias líneas) y se edita ahí. |
| Lecturas de evidencias | Además de editar los datos y "Correcto", hay **"No es correcto"**: el chat ofrece volver a tomar la foto, volver a subirla o continuar sin esa lectura (queda pendiente). |
| Selftest de impresora | Confirma **marca y modelo** (o los completa si no salieron en la etiqueta) y registra **IP, MAC y puerta de enlace**. En impresoras **USB** se pide igual, pero no hay IP ni MAC: solo marca y modelo. |
| Después del chat | Módulos: Análisis IA → Propuesta (Ver, Editar, Guardar, Generar PDF) → Vista previa → Minutas (validación del supervisor) → Notificaciones → WhatsApp y correo siempre activos (sin validar: marca de agua SIN VALIDAR). Ver el simulador. |
| Unidades | Las recomendaciones de plan se escriben **siempre en Mbps** (1000 Mbps = 1 Gbps). |
| Propuesta técnica | Ver, Editar, Guardar, WhatsApp y Email son **solo íconos** (ojo, lápiz, guardar, WhatsApp, correo). El botón de PDF lleva el ícono PDF y dice "Generar PDF" y luego "Volver a generar". |
| Historial | El ícono de filtro abre el **filtro por fecha**: Todas, Hoy, Últimos 7 días, Este mes o un rango Desde/Hasta. |
| Inicio y menú | Iguales a la APK. Se agrega el módulo **Notificaciones** (menú lateral) y un **punto rojo** en la campana cuando hay una notificación sin leer. Recientes, Historial, Minutas y Notificaciones abren la minuta al tocarla. Configuración no cambia. |
| Mapa de red | Colores y reglas de conexión de impresoras: ver `analisis/MAPA_DE_RED.md`, sección 8. |
| Anular borrador | Igual que la APK: en **Historial**, deslizar a la izquierda una minuta en **Borrador** muestra "Anular" en rojo y el aviso "¿Anular «{nombre}»? Es un borrador y se descarta junto con su chat; no se puede deshacer." (Cancelar · Anular). Completas, validadas y en análisis no se pueden anular. |
| Inicio del chat | Orden de las primeras preguntas: **1) ¿Cuál es el nombre de la razón social?** · **2) ¿Cuál es el RUC?** (11 dígitos que empiezan con 10, 15, 17 o 20; si no cumple, el chat avisa y lo vuelve a pedir) · **3) ¿Cuál es el nombre del local?** · **4) Dirección**, y luego el resto. Razón social y RUC llegan solos a "Datos generales" de la minuta y se pueden corregir en Editar › Datos del local. |
| Contacto del local | Después de la dirección: **P04 encargado del local** (nombre y cargo) → **P05 celular** (para compartir la minuta por WhatsApp; 9 dígitos que empiezan con 9; botón **Saltar**) → **P05b correo** (para enviar la minuta; botón **Saltar**) → P06 fecha de la visita (con calendario) → P07 técnico responsable. Si se saltan, en la minuta quedan "Pendiente · se completa al editar la minuta". El celular se usa en WhatsApp y el correo se llena solo en "Enviar por correo". |
| Formato de la minuta | Se mantiene la plantilla de la APK (`templates/minuta.html` del gateway): encabezado "Minuta técnica · {local} · {fecha}", título MINUTA TÉCNICA DE RED, indicadores (campos, de imágenes, del técnico, pendientes), cuadros Campo · Valor · Fuente con pendientes en ámbar, validación técnica automática, resumen ejecutivo con ESTADO FINAL y pie "Generada por el Asistente Virtual de Red · Página X de Y". Orden: 1 Datos generales · 2 Antecedentes · 3 Desarrollo y hallazgos (negocio, internet, equipos de caja, impresoras, WiFi y cámaras, energía) · 4 Topología de red · 5 Análisis + validación · 6 Recomendaciones · (7 Observaciones) · Resumen ejecutivo · Aviso de viabilidad · Pasos a seguir · **Anexo A. Evidencias**: cada foto con "Imagen de: …", su descripción y "Ubic.:"; las 3 fotos de cada impresora juntas en un bloque "IMP. {nombre} · Ubic.: {lugar}", separado del de otra impresora · Anexo B. Video (solo si hay enlace). |
| Proveedor (P13) | Movistar · Claro · Entel · WIN · Bitel · **Otro (escribir)**: lo escrito se guarda como respuesta. Solo si el local tiene internet. |
| Ubicación del router (P18) | "¿En dónde se encuentra ubicado el router?" con opciones = **las áreas que el técnico ya indicó** (Caja, lugares de preparación y zonas de atención de todos los canales) + **Otro (escribir)** para cualquier lugar (Administración 3er piso, central del patio de comidas…). |
| Energía (P33 = No) | **P33a** selección múltiple con los equipos que registró el técnico + Otro (escribir, varias veces) + **Ninguno** (ningún equipo tiene tomas cerca). Luego **P33b** texto libre mediano o largo: "¿Recomendarías un supresor de picos? ¿El cliente va a implementar tomas cerca? Dame tus apreciaciones". Se usa en el análisis (Infraestructura), la recomendación de supresor de picos, la topología (rayo rojo en los equipos sin toma, azul en el resto; con P33 = Sí todos llevan rayo azul) y la minuta. |
| Minuta (ajustes) | Sin columna **Fuente** en ningún cuadro. Arriba, en lugar de "campos/imágenes/técnico/pendientes", los **KPI del Análisis IA**: Conectividad, Infraestructura, Equipamiento, Cobertura WiFi (si aplica) y Puntuación global. **Datos generales** suma Rubro, Estado del local (sale de Desarrollo), Fecha de inicio y Validada por. **WiFi y cámaras**: la revisión por zona solo si se hizo; cámaras siempre (Sí o No). **Energía**: "No" en rojo, equipos sin toma y apreciaciones del técnico. Nuevo cuadro **Equipos varios** (router, switch, TP-Link, Raspberry, repetidores, cámaras…) después de Impresoras. Se quita la sección Análisis (queda **Validación técnica automática**). **Topología**: 50 % de la hoja A4, o 75 % con 4 o más impresoras o 2 o más pisos. Una sola fuente de letra en todo el documento. |
| Impresora USB | **Solo se conecta a una PC o laptop.** Nunca a tablet, Raspberry, Sunmi, iMin, router, repetidor ni otro equipo. Si P28 = USB, nueva pregunta **P28u** "IMP. {nombre}: ¿a qué equipo está conectada por USB?" (PC o laptop —solo si el local tiene una—, Tablet, Raspberry, Sunmi, iMin, Router o repetidor, Otro equipo, No está conectada a ningún equipo). Si no es PC o laptop: aviso en el chat, observación **USB** en el análisis y en la topología la impresora se muestra **sin flecha USB** (la flecha solo existe hacia una PC o laptop), con "USB a {equipo}: no válido" debajo del nombre. Si no está conectada, queda sin flecha con "USB sin conectar". |
| Rayo de energía | El rayo **rojo** significa solo que el equipo **no tiene toma de energía cerca**; el azul, que sí tiene. Mide el **45 %** del ícono del equipo. No se usa para marcar problemas de conexión (eso lo hacen las líneas). |
| Colores de líneas de impresoras | Si la impresora tiene buena conexión de red, su línea es **azul** aunque cuelgue de un switch o TP-Link por implementar. Plomo solo cuando la impresora o su conexión están por implementar. |
| Topología en la minuta | Gráfico vectorial propio (no captura del visor): **todo el ancho del A4** dentro de márgenes y **siempre el 50 % del alto**; íconos pequeños, leyenda incluida. Reemplaza la regla anterior de 50 % / 75 %. |
| Calor y grasa (R10) | Parrilla y pollería: router, Raspberry e impresoras lejos del calor y la grasa; **también se pueden cubrir los equipos** para evitar que se llenen de grasa o tengan sobretemperatura. |
| Cable cerca de electrodomésticos (P34c, R_ELEC) | Después del bloque de energía: "¿Algún cable de red pasa cerca o por detrás de electrodomésticos (licuadoras, congeladoras, refrigeradoras, microondas u otros)?" Sí/No. Si es Sí: aviso en el chat y recomendación de **aislar el cable de la estática y la interferencia** (canaleta o tubo, separarlo del equipo y del cable de energía, o cable apantallado FTP). Suma una pregunta en todos los rubros. |
| Anexo A de la minuta | **Evidencias utilizadas** (speedtest, frontis, lugares de preparación, zonas, caja): **2 imágenes por fila**. **Otros equipos**: empieza con el bloque **Router principal · Ubic.: …** con sus **3 fotos** (ubicación, puertos, etiqueta con marca, modelo y SN), en el mismo formato que las impresoras; luego el bloque **Otros equipos del local** (router secundario o de otros pisos, gabinete, servidor, extensión, switch, repetidor, mesh, access point, otro), **3 por fila**. **Impresoras**: un bloque de 3 por impresora. Debajo de cada imagen, una descripción corta. **Solo se muestran las evidencias subidas**: si no hay router, otros equipos o impresoras (o se saltaron sus fotos), ese apartado no aparece y no se escribe "falta evidencia". Si no hay ninguna evidencia, no hay Anexo A. |
| Fotos del router (E2u, E2, E2e) | Después de "¿En dónde se encuentra ubicado el router?": **E2u** foto de la ubicación del router · **E2** foto de los puertos (si no se leen, P18a pregunta cuántos libres) · **E2e** foto de la etiqueta: la IA lee **marca, modelo y SN** (editable). Las tres se pueden saltar. |
| Otros equipos (EVO, EVQ) | Después de las fotos obligatorias: "¿Qué otros equipos hay en el local? Por ejemplo router principal, router secundario o de otro piso, gabinete, servidor, extensión, switch, repetidor o mesh. Márcalos para subir una foto de cada uno." Opciones: **Router principal** (solo si no se subió ya la foto del router en E2), **Router secundario**, **Router del piso 2**, **Router del piso 3**, Gabinete, Servidor, Extensión (regleta), Switch, Repetidor, Mesh, Access point + Otro (escribir, p. ej. router del piso 4) + Ninguno. Si marca alguno, pide una foto de cada uno (EVQ). La foto del router (E2) va en este mismo apartado del anexo. |
| Anexo B: videos (P49a) | "Anexo B: agrega los enlaces de los videos del local y de los equipos…" El técnico puede agregar **varios enlaces** (Drive, OneDrive, YouTube u otro), cada uno con qué muestra el video: Recorrido del local, Equipos de red (router, switch, gabinete…), Sistema de impresiones, Caja y periféricos u Otro (escribir). Se pueden quitar antes de continuar, o continuar sin enlaces. En la minuta, **Anexo B. Videos del local y de los equipos** es una tabla N.° · Video · Enlace. |
| Mozos con tablet (P09c) | "¿Hay mozos que toman pedidos con tablet o celular?" se pregunta en **Pescados y mariscos, Parrilla/Anticuchería, Restaurante, Hotel, Pizzería, Chifa, Restobar, Discoteca o bar, Pollería, Comida oriental** y también en **Fast food, Cafetería y Heladería** (ahí es poco común, la respuesta sugerida es No). No se pregunta en Retail ni en Otro. Si es **No**, no se revisa el WiFi por zona (P46, revisión por zona, P46a) y no se suma el consumo de tablets de mozo. |
| Conteo de preguntas en el chat | El encabezado cuenta solo preguntas: "Pregunta N de ~T". Las evidencias y lo que se pregunta sobre ellas (fotos, lecturas, cuadro del router, estado y conexión de la impresora P28, P28b, P28c, P28u, otros equipos EVO, videos del Anexo B, resumen de evidencias) se muestran aparte como "Evidencia N de ~M" y no suman al total de preguntas. ~T y ~M se estiman completando lo que falta con las respuestas sugeridas. |
| Áreas de preparación (P11e) | Selección múltiple con las áreas **ya marcadas** por rubro (se pueden quitar o agregar con Otro): Fast food: Cocina, Barra · Cafetería: Barra · Heladería: Barra · Restaurante: Cocina, Barra, Jugos, Cafetería · Restobar y Discoteca o bar: Barra 1, Barra 2, Cocina · Comida oriental: Salteados, Ramen, Makis, Frituras, Bebidas · Pescados y mariscos: Fríos, Calientes, Bebidas · Parrilla/Anticuchería: Parrilla, Cocina, Bebidas · Hotel: Cocina, Barra · Pizzería: Bebidas, Pizzas · Chifa: Cocina, Despacho, Bebidas · Pollería: Brasas, Cocina, Bebidas, Despacho · Otro: sin marcar. **Retail y Otro**: aviso "los lugares de preparación no tienen impresora asignada" y P11r lugares de referencia (Almacén…), sin impresoras ni evidencias; en Retail viene marcado Almacén, en Otro no se marca nada. Cafetería y Heladería: Barra. |
| Canales (P11f) | Ya marcados por rubro: Fast food y Pizzería: Recojo, Delivery · Retail: Recojo, Delivery, Backoffice · Restaurante, Pescados, Chifa, Pollería, Comida oriental, Parrilla: Mesa, Recojo, Delivery · Hotel: Habitaciones, Mesa, Recojo · Restobar: Recojo, Mesa · Discoteca o bar: Recojo · Cafetería y Heladería: Recojo, Delivery · Otro: sin marcar. Canal nuevo **Habitaciones**. |
| Cajas (P12, P12e) | P12 "¿Cuántas cajas hay o va a haber?" 1 a 6 + Otro (sin 0). P12e "¿La caja comparte impresora con alguna área de preparación?" (no en Retail ni Otro). |
| Retail y Otro | No se pregunta tipo de conexión, proveedor, respaldo ni caídas: "¿De cuánto es su plan de internet? (Mbps)" → speedtest → ipconfig → escáner de IP. Sin bloques de energía, WiFi ni cámaras. Solo impresoras de caja (los lugares de preparación no tienen impresora). En el análisis no se marca evidencia ni información faltante. Fotos obligatorias: Retail 2 (frontis y caja), Otro 4. |
| Resto de rubros | Se pregunta todo. P15 cambia a "¿De cuánto es su plan de internet? (Mbps)". E1 sin "Se ignora la publicidad de la pantalla". Fotos del local: frontis, lugares de preparación y caja obligatorias; zonas de atención con **Saltar**. |
| P14 tipo de conexión | Opciones: Fibra · Coaxial · Chip · No sé · No tiene · **Va a contratar internet antes de implementar**. "No sé" sigue con el proveedor. "No tiene": Conectividad 0 %, R56 TP-Link con chip y **prueba del chip** (P14b + E1b) con la opción **Saltar prueba (queda pendiente por validar)**; luego pasa a las cajas. "Va a contratar" (típico en obra o por inaugurar): no se recomienda TP-Link ni se hace la prueba del chip; recomendación de contratar el internet antes de implementar y segunda visita para validar la conectividad; pasa a las cajas. En obra la sugerida es "Va a contratar". |
| Plan y speedtest | P15 "¿De cuánto es su plan de internet? (Mbps)": 300 · 500 · 1000 · Otro (escribir). P19a desde qué equipo: Equipo de caja conectado por cable de red directo al router · Laptop del técnico conectada directo al router · Por WiFi · Otro equipo del local conectado directo al router. Si es por WiFi, P19w "La prueba de internet debe hacerse por cable de red directo al router": He repetido la prueba con el equipo de caja por cable directo al router · He repetido con mi laptop conectada por cable directo al router · Saltar: plan sin verificar. P18 suma las opciones Piso 1, Piso 2 y Piso 3. |
| Sunmi o iMin en caja | En Retail, o cuando la comanda sale en la misma caja y no hay otro lugar de preparación que el que comparte con caja (por ejemplo, una cafetería), no se pregunta por Raspberry ni se recomienda controlador: el Sunmi o iMin imprime en la misma caja. En los demás casos se pregunta si tiene o adquirirá Raspberry. |
| ipconfig e IP scanner (E1c, E1d) | Después del speedtest. ipconfig: IP, máscara y puerta de enlace (da el segmento del router). Escáner: equipos encontrados, IP de impresoras y otros. Se usan para la topología y la minuta. |
| Cámaras DVR / NVR (CAM_DVR, CAM_NVR) | Con internet por fibra o coaxial (no chip) y cámaras, el escáner de IP dice si van por **DVR** (una sola IP, ej. Hikvision) o **NVR** (varias IP de cámaras). DVR: aviso, sin cambio de plan. NVR con plan < 500 Mbps: OBSERVADO, independizar la red de internet de las cámaras o ampliar a 1000 Mbps; NVR con 500 a 999 Mbps: recomendar ampliar a 1000 Mbps. Si no se ve en el escáner, aplica R53. Confirmada 2026-10-11. |
| Router (E2u, E2, E2e + cuadro) | Tres fotos (ubicación, puertos, etiqueta) sin lectura por foto; al final un **cuadro** con IP, proveedor, marca, modelo, SN, puertos en total, libres y ocupados, con **Correcto** y **Corregir**. P18 con opciones de las áreas del local y Otro. |
| Equipo de caja | Las preguntas dicen "equipo de caja" (no "Caja:"). P22 "¿Cuál es el equipo de caja?" PC o laptop · Tablet · Sunmi · iMin · No tiene · No sé · Otro. P22h (Retail) "¿Qué periféricos están conectados al equipo de caja?". |
| Raspberry existente (P22c, P22d, P22e) | Si el cliente ya tiene Raspberry: P22c ¿Cable de red o WiFi? → P22d "¿A qué equipo de red principal va conectado el Raspberry?" Router · Switch · Repetidor · Access point · Otro → P22e: si es por cable, "¿Hay toma de energía y punto de red disponibles para el Raspberry?" (Sí, ambos · Solo toma de energía · Solo punto de red · Ninguno); si es por WiFi, "¿Hay toma de energía disponible para el Raspberry?" (Sí/No). Lo que falte sale como observación (implementar punto de red o toma para el Raspberry); en la topología el Raspberry cuelga del equipo indicado, con línea roja si falta el punto de red y rayo rojo si falta la toma. Solo cuenta un puerto del router si va conectado al router. **Si el cliente va a adquirir el Raspberry** (P22b = Raspberry) también se pregunta P22d ("¿A qué equipo de red principal irá conectado…?") y P22e ("¿Hay toma de energía y punto de red disponibles para cuando se implemente el Raspberry?"); en la topología se muestra como por implementar (ícono con borde plomo y línea plomo) y lo que falte sale como observación. |
| AnyDesk (E7) | Si el equipo de caja es PC, laptop, tablet, Sunmi o iMin, o hay o habrá Raspberry: foto de AnyDesk abierto; la IA lee el código y el equipo desde donde se accede. No en obra. |
| Impresoras | Fotos de etiqueta, conexiones y selftest. Si el selftest tiene IP y la foto muestra cable de red → se completa **red**; si no tiene IP y se ve USB → **USB**. P28 solo si no se lee. P28b "¿Cuál es el estado de la impresora?" Nueva · Viable · Necesita mantenimiento · Con fallas · Otro. Nueva P28c "¿Conectada directamente al router, a un switch, a un repetidor, por WiFi o a un socket de pared?" + Otro (se dibuja en la topología). |
| Fotos con toma de energía | P33 se pregunta antes de las fotos. En los equipos que **sí** tienen toma cerca (router, impresoras, equipo de caja, otros equipos), la foto debe mostrar también la toma a la que está conectado. No se pide en equipos sin toma, ni en obra, Retail u Otro. |
| Estado: En operación | Flujo completo (cliente que ya opera; se evalúa que todo esté correcto). |
| Estado: Sistema anterior | Sin respaldo ni caídas. Impresora fuera del segmento: configurarla en la misma red y segmento que las otras antes de iniciar. Sunmi o iMin en caja: aviso de que si no es de Desarrollo de Negocios no se puede usar. |
| Estado: Por inaugurar | Sin caídas ni respaldo; "¿Qué proveedor tiene contratado?"; estado de impresora sugerido "Nueva"; fotos con patrón. Sin internet: observación, el proveedor ya debe estar contratado y el router instalado. |
| Estado: En obra | "¿El cliente ya tiene internet contratado?". Sin speedtest, ipconfig, escáner, puertos ni etiqueta del router, AnyDesk ni WiFi por zona. "¿Dónde irá el router?" con foto sin patrón. Por cada impresora "¿El cliente ya tiene la impresora?" (si sí, se piden sus fotos). Recomendaciones: segunda visita cuando esté habitado, puntos de red y energía, cotizar con Desarrollo de Negocios los equipos por implementar. |
