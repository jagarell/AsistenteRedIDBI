# Autenticación en la app

## 1. Sin registro en la app
- Se quita el botón **Crear Cuenta Nueva** y la pantalla de registro.
- En su lugar: «¿No tienes cuenta? Solicítala a tu administrador.»
- Las cuentas las crea el administrador desde la web.

## 2. Recuperar contraseña con aprobación del administrador
1. **¿Olvidé contraseña?** → el usuario escribe su correo → **Enviar código**.
2. La app muestra un **número de 2 dígitos** grande y «Esperando la aprobación…», con **Enviar de nuevo**.
3. Al administrador le llega una notificación (push y en Notificaciones), estilo Gmail:
   «¿Apruebas el cambio de contraseña? [correo] está intentando restablecer su contraseña desde la app. Toca el número que aparece en su pantalla» con **3 números** y «No, no reconozco esta solicitud».
4. Si el administrador toca el número correcto: el usuario pasa a **Crea tu nueva contraseña** (nueva y confirmar, mínimo 8 caracteres).
5. Si toca otro número o rechaza: el usuario ve **Solicitud rechazada** y **Volver a solicitar**.
6. La app consulta el estado de la solicitud cada pocos segundos mientras espera (o por notificación).

API: ver `AsistenteRedIDBI-API-Gateway/docs/0.0.7/02-autenticacion-recuperacion.md`.

## 3. Criterios de aceptación
**CA-01 Sin registro**
- Dado el inicio de sesión, cuando se abre la app, entonces no existe ningún botón ni pantalla para crear una cuenta.

**CA-02 Aprobación correcta**
- Dada una solicitud con el número 59, cuando el administrador toca 59, entonces la app del usuario muestra «Crea tu nueva contraseña».
- Cuando el usuario guarda una contraseña de 8 o más caracteres que coincide en ambos campos, entonces vuelve al inicio de sesión con «Contraseña actualizada correctamente».

**CA-03 Rechazo**
- Cuando el administrador toca un número distinto, entonces el usuario ve «El administrador eligió otro número» y la solicitud queda cerrada.
- Cuando el administrador toca «No, no reconozco esta solicitud», entonces el usuario ve «El administrador indicó que no reconoce la solicitud».
