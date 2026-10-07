---
name: gateway-port-conflict-voya
description: "Port 8080 on this Mac can be squatted by an unrelated project (Voya), making the IDBI gateway look broken with false 403s"
metadata: 
  node_type: memory
  type: project
---

The user has another, unrelated Android Studio project called **Voya**
(`~/AndroidStudioProjects/Voya/backend`) whose Spring Boot backend also
defaults to port 8080 (`pe.voya.backend.VoyaBackendApplication`). If it was
left running from an earlier session, it silently occupies port 8080 instead
of `idbi-api-gateway`. The Android app then talks to Voya without any
connection error — but every endpoint, even public ones like
`/api/auth/login` or `/api/auth/register`, returns `403 Forbidden` with no
body, because Voya has none of IDBI's routes.

**Why this matters:** this looks exactly like a Spring Security
misconfiguration (permitAll not working) and cost significant debugging time
on 2026-09-08 before `lsof -i :8080 -sTCP:LISTEN` revealed the real process.
The actual `idbi-api-gateway` `SecurityConfig.java` was correct the whole
time.

**How to apply:** before debugging any "no permissions" / unexpected 403 /
"login doesn't work" report on this project, run
`lsof -i :8080 -sTCP:LISTEN` first and confirm the process listed is
`IdbiApiGatewayApplication` (path containing `idbi-api-gateway`), not
`VoyaBackendApplication`. If it's the wrong one, `kill` it and restart the
real gateway with `cd ~/IdeaProjects/idbi-api-gateway && set -a; source .env;
set +a; ./mvnw spring-boot:run` (per `CONTEXTO_PROYECTO.md` in the app repo root).

**Confirmed recurring** (2026-09-14): hit the exact same conflict again on a
routine server restart mid-session, ~1 week after first discovering it —
this isn't a one-off, it happens whenever Voya was run more recently than
the gateway. Worth checking `lsof -i :8080` proactively before *every*
gateway restart in this project, not just when something looks broken.
