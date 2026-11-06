# Empresa y configuración

## 1. Perfil y configuración › Empresa
Sección nueva **EMPRESA**, antes de Apariencia:
- **Logo** (vista previa) y botón **Cambiar logo** (imagen de hasta 2 MB).
- **Nombre de la empresa** (por defecto: IDBI).
- **Restablecer IDBI**.
- Solo el **administrador** puede editar; los demás roles lo ven bloqueado con el texto «Solo el administrador puede cambiar la empresa».

El logo y el nombre salen en la cabecera de la minuta, en el PDF y en el pie («IDBI · Minuta técnica de red»).

## 2. Preguntas y reglas › Sistema de impresiones (administrador)
- El **nombre del sistema de impresiones** (Wrapper en IDBI) **no** va en Perfil y configuración.
- Va en una tarjeta arriba de **Preguntas y reglas** (pestañas Preguntas, Mapa de nodos y Reglas), con un ejemplo: «¿En el equipo de caja está el sistema de impresiones (Wrapper)?».
- Se usa en las preguntas del Chat IA, en las reglas del estado final, en las respuestas del Asistente IA y en la minuta.

## 3. Criterios de aceptación
**CA-01 Editar empresa**
- Dado un administrador, cuando cambia el nombre a «Acme» y sube un logo PNG, entonces la próxima vista de la minuta muestra ese logo y «Acme» en la cabecera y el pie.
- Dado un técnico, cuando abre Perfil y configuración, entonces los campos de empresa están deshabilitados.

**CA-02 Sistema de impresiones**
- Dado el nombre «PrintHub», cuando el Chat IA pregunta por el equipo de caja por WiFi, entonces la pregunta dice «(PrintHub)» y la observación dice «PC con PrintHub por WiFi».
- Cuando el técnico abre Perfil y configuración, entonces no aparece el nombre del sistema de impresiones.
