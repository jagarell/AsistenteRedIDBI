# Guía de instalación y ejecución — Asistente Red IDBI

Esta guía explica cómo levantar los 3 repos del proyecto desde cero en una
máquina nueva, para poder correr la app completa y validar los cambios de
la branch `feature/roles-minutas-topologia`. Para entender **qué** hace cada
pieza y el estado real de cada feature, lee también
[`CONTEXTO_PROYECTO.md`](CONTEXTO_PROYECTO.md) en este mismo repo.

## 0. Arquitectura (3 repos)

| Repo | Qué es | Puerto |
|---|---|---|
| `AsistenteRedIDBI` (este repo) | App Android nativa (Kotlin/MVVM/Hilt) | — |
| `idbi-api-gateway` | Backend Spring Boot, única fuente de verdad de negocio (auth, evaluaciones, minutas, PDF, perfil) | `8080` |
| `idbi-fastapi` | Microservicio Python: motor de chat técnico (23 preguntas) y análisis de fotos por visión | `8000` |

El Android le habla siempre al **gateway** (nunca directo a FastAPI). El
gateway es el único que le habla a FastAPI. Necesitas los 3 repos
clonados y corriendo en paralelo.

## 1. Prerrequisitos

- **Git**, acceso a los 3 repositorios remotos.
- **JDK 17** (para el gateway, Spring Boot / Maven).
- **Android Studio** reciente, con un emulador o dispositivo físico
  (`compileSdk 35`, `minSdk 24`; el proyecto compila con JDK 11 target vía
  el propio Gradle/Android Studio, no hace falta instalarlo aparte).
- **Python 3.13+** con `venv`.
- **PostgreSQL** corriendo localmente (puerto `5432`). En Mac, lo más simple
  es [Postgres.app](https://postgresapp.com/).

## 2. Clonar los 3 repos

```bash
git clone <url-AsistenteRedIDBI> ~/AndroidStudioProjects/AsistenteRedIDBI
git clone <url-idbi-api-gateway> ~/IdeaProjects/idbi-api-gateway
git clone <url-idbi-fastapi> ~/IdeaProjects/idbi-fastapi
```

Las rutas exactas no importan (todo se comunica por `localhost`, no por
rutas de archivo), pero usar estas mismas carpetas evita tener que ajustar
nada más abajo. En los 3 repos, ubícate en la branch
`feature/roles-minutas-topologia` (o la que corresponda validar):

```bash
cd ~/AndroidStudioProjects/AsistenteRedIDBI && git checkout feature/roles-minutas-topologia
cd ~/IdeaProjects/idbi-api-gateway        && git checkout feature/roles-minutas-topologia
cd ~/IdeaProjects/idbi-fastapi            && git checkout feature/roles-minutas-topologia
```

## 3. Base de datos (PostgreSQL)

Crea la base y el usuario que espera el gateway (nombres fijos, ver
`idbi-api-gateway/.env.example`):

```bash
psql postgres -c "CREATE USER idbi_user WITH PASSWORD 'tu-password-local';"
psql postgres -c "CREATE DATABASE asistente_red_idbi OWNER idbi_user;"
```

No hay scripts de migración — el gateway crea/actualiza todas las tablas
solo (`spring.jpa.hibernate.ddl-auto: update`) la primera vez que arranca.

> **Gotcha conocido**: si en algún momento las tablas quedan de otro dueño
> (por ejemplo si se crearon a mano con tu usuario del sistema en vez de
> `idbi_user`), el gateway falla con `must be owner of table`. Fix:
> `REASSIGN OWNED BY "<tu_usuario>" TO idbi_user;` conectado a la base
> `asistente_red_idbi`.

## 4. Backend — `idbi-api-gateway` (puerto 8080)

```bash
cd ~/IdeaProjects/idbi-api-gateway
cp .env.example .env
```

Edita `.env` (nunca se sube a git — está en `.gitignore`):

- `DB_USERNAME=idbi_user`, `DB_PASSWORD=<la que pusiste arriba>`
- `JWT_SECRET`: genera uno real, ej. `openssl rand -base64 48` (mínimo 32 bytes)
- `FASTAPI_BASE_URL=http://localhost:8000` (ya viene así por defecto)
- Deja `MAIL_ENABLED=false` y `RUC_VALIDATION_ENABLED=false` salvo que
  tengas credenciales reales de SMTP / apis.net.pe — apagados, el sistema
  falla explícito en vez de simular éxito, así que la app sigue funcionando
  igual sin ellos (solo no se manda correo real ni se valida RUC contra SUNAT).
- `FIREBASE_CREDENTIALS_PATH` puede quedar con el valor de ejemplo si no
  vas a probar notificaciones push — el gateway simplemente no las envía.

Arrancar:

```bash
set -a; source .env; set +a
./mvnw spring-boot:run
```

Primer arranque exitoso: verás logs de Hibernate creando tablas y
`Tomcat started on port 8080`.

## 5. Microservicio de IA — `idbi-fastapi` (puerto 8000)

```bash
cd ~/IdeaProjects/idbi-fastapi
python3 -m venv venv
./venv/bin/pip install -r requirements.txt
cp .env.example .env
```

Edita `.env`:

- Deja `CHAT_PROPOSAL_ENGINE=builtin` (motor de reglas propio, **no**
  requiere OpenAI — así es como corre hoy en producción).
- `OPENAI_API_KEY` solo hace falta si quieres probar el análisis de fotos
  por visión (identificación de marca/modelo de equipos en Evidencias). Sin
  ella, esa feature específica responde "no configurado" pero el resto de
  la app (chat, propuesta AS-IS/TO-BE, checklist dinámico) funciona igual.

Arrancar:

```bash
./venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Verificación rápida: `curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8000/docs` debe dar `200`.

## 6. App Android

```bash
cd ~/AndroidStudioProjects/AsistenteRedIDBI
```

Abre la carpeta en Android Studio y deja que sincronice Gradle. No necesitas
tocar nada de configuración: la app apunta a `http://10.0.2.2:8080/`
(`AuthModule.kt`), que es el alias estándar del emulador Android hacia el
`localhost` de la máquina donde corre el gateway — funciona solo si usas el
**emulador**. Si vas a probar en un **dispositivo físico**, cambia
`BASE_URL` en `app/src/main/java/com/upc/asistenteredidbi/domain/di/AuthModule.kt`
a la IP de tu máquina en la red local (ej. `http://192.168.x.x:8080/`).

`google-services.json` (Firebase/push) es **opcional** — si no existe, el
plugin de Google Services simplemente no se aplica (`app/build.gradle.kts`
lo detecta solo) y el resto de la app compila y corre normal, solo sin
notificaciones push.

Ejecuta desde Android Studio (▶) o por línea de comandos:

```bash
./gradlew installDebug
adb shell am start -n com.upc.asistenteredidbi/.MainActivity
```

## 7. Orden de arranque (siempre así)

1. PostgreSQL (si no corre como servicio, levántalo primero).
2. Gateway (`idbi-api-gateway`) — puerto 8080.
3. FastAPI (`idbi-fastapi`) — puerto 8000. Puede arrancar antes o después
   del gateway sin problema, pero debe estar arriba **antes** de usar el
   chat técnico o análisis de fotos en la app.
4. App Android.

**Antes de arrancar el gateway**, verifica que el puerto 8080 esté libre:

```bash
lsof -i :8080 -sTCP:LISTEN
```

Si aparece un proceso que no es `IdbiApiGatewayApplication` (por ejemplo
otro proyecto Spring Boot local), mátalo primero — si no, la app recibirá
`403 Forbidden` en *todos* los endpoints, incluso los públicos como login,
y parecerá un bug de seguridad que en realidad no existe.

## 8. Checklist de validación (qué probar en la app)

Los siguientes son los cambios de la última sesión de trabajo que
**todavía no se caminaron a mano en el emulador** — verificados solo por
API/base de datos directa. Ideal que tu compañero confirme esto
visualmente:

- [ ] **Registro + login** funcionan de punta a punta.
- [ ] **Chat técnico (23 preguntas)**: los botones de Sí/No/opción muestran
      feedback visual claro al tocarlos (antes desaparecían sin avisar), y
      el campo de texto libre no arrastra un valor viejo de la pregunta
      anterior si se responde con un botón.
- [ ] Al completar el chat, aparece la propuesta con **dos secciones
      separadas**: diagnóstico AS-IS y propuesta recomendada TO-BE.
- [ ] **Pantalla de Evidencias**: al entrar, el checklist de áreas/equipos
      se arma solo según lo que se respondió en el chat (no es una lista
      fija de 7 categorías genéricas). Se puede agregar/quitar áreas y
      equipos a mano (Fase A) antes de "Confirmar selección".
- [ ] Tras bloquear la selección (Fase B), cada ítem pide al menos una
      foto; el botón "Analizar con IA" solo se habilita cuando todos los
      ítems tienen foto.
- [ ] La pantalla principal de propuesta ("Recomendaciones IA") muestra el
      mismo AS-IS/TO-BE que generó el chat — antes eran dos motores
      independientes con resultados distintos, ahora es uno solo.
- [ ] (Opcional, requiere `OPENAI_API_KEY`) Subir una foto de un equipo
      (router/switch/etc.) y confirmar que se detecta marca/modelo.

## 9. Problemas comunes

| Síntoma | Causa probable | Fix |
|---|---|---|
| `403 Forbidden` en todo, incluso login/registro | Otro proceso ocupa el puerto 8080, no es el gateway real | `lsof -i :8080 -sTCP:LISTEN`, matar el proceso incorrecto, reiniciar el gateway |
| App que antes funcionaba empieza a dar 403 en todo | Se regeneró `JWT_SECRET` en `.env` y quedaron tokens viejos guardados en la app | Cerrar sesión y volver a entrar |
| Gateway falla con `must be owner of table` | Las tablas de Postgres no son propiedad de `idbi_user` | `REASSIGN OWNED BY "<tu_usuario>" TO idbi_user;` en la base `asistente_red_idbi` |
| Análisis de fotos dice "no configurado" | Falta `OPENAI_API_KEY` en `idbi-fastapi/.env` | Esperado sin credencial — el resto de la app funciona igual |
| Envío de correo (reset de contraseña, propuesta por email) falla | `MAIL_ENABLED=false` (default) | Esperado sin credenciales SMTP reales — falla explícito, no simula envío |
