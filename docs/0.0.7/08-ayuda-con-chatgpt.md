# Ayuda con ChatGPT y fotos

- La **Ayuda** del Chat IA responde con ChatGPT (misma API que el Asistente IA, ver API `08-ayuda-vision.md`), con las reglas de la app como contexto. Lo que responde **no se guarda en la minuta**, salvo que el técnico confirme pasar datos.
- Barra de la Ayuda: 📷 foto, 🎤 audio, texto y enviar.
- Si el técnico pregunta «¿qué es un switch administrable?», la Ayuda lo explica, da las pistas de la etiqueta («Managed», «Smart», «Unmanaged»…) y le pide una foto.
- Con la foto, responde marca, modelo y si es **administrable** (✅), **administrable básico** (⚠️) o **no administrable** (❌), y qué hacer: usar «Ubicar puerto» o pedir la IP y la comunidad SNMP.
- Si la foto no es un switch (por ejemplo, el router del proveedor), lo dice.

## Criterios de aceptación
- Dada la foto de un D-Link DES-1024D, entonces la Ayuda responde «No es administrable» y sugiere «Ubicar puerto».
- Dada la foto de un switch Cisco CBS250, entonces responde «Administrable» y pide la IP y la comunidad SNMP de lectura.
- Dada la foto de un router Huawei EG8145X6, entonces responde que no es un switch.
