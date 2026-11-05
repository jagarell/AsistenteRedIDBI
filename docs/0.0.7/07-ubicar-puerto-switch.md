# Ubicar el puerto del switch de cada impresora

**Requisito:** el celular del técnico conectado al WiFi del local (misma red que las impresoras). La app no se conecta al switch con cables.

> **Paso opcional:** la app pregunta al técnico si quiere ubicar los puertos; puede saltar el paso completo o una impresora.

## 1. Opción B (principal, cualquier switch): «Ubicar puerto»
1. En «Switch», el técnico indica cuántos puertos tiene el switch y marca el **puerto de subida** (el que va al router), para descartarlo.
2. Elige una impresora y toca **Ubicar puerto**.
3. La app envía pings a la IP de la impresora con un ritmo de 2 s sí / 2 s no durante 20 s. La luz del puerto de esa impresora parpadea al mismo ritmo.
4. El técnico toca el número del puerto en el dibujo del switch. Si ninguna luz sigue el ritmo: «No está en este switch» (por ejemplo, va a un repetidor).
5. Se repite con cada impresora. La tabla **Impresora → puerto** se llena sola y se sugiere el rotulado.
6. **Fase 2:** grabar un video corto del switch; la IA detecta qué luz sigue el ritmo y asigna el puerto sin que el técnico lo marque.

Solo se usan pings (ICMP). Nunca se envían datos al puerto 9100 de la impresora (imprimiría basura).

## 2. Opción A (switch administrable)
Si la Ayuda o la foto indican que el switch es administrable, la app pide la IP del switch y la comunidad SNMP de lectura (o el usuario). La API lee la tabla de MAC por puerto y la cruza con la MAC de cada impresora (del selftest o del escáner). No hay que mirar luces.

## 3. Calidad del punto de red
Con los mismos pings se mide latencia y pérdida por impresora. Si la pérdida supera el 2 % o la latencia media supera 50 ms dentro de la red local, se crea la observación: «Punto de red de IMP. [nombre] (puerto N) con N % de pérdida: revisar el cableado del punto y su extremo en el switch». El puerto queda **en observación** en la tabla del switch y en el rotulado.

## 4. Rotulado y nombres de zonas
- Pregunta nueva: «¿Se rotularon los puntos en el switch?» (Sí / No / Parcial). La respuesta y los puertos en observación van a la minuta.
- Los nombres de las zonas (por ejemplo «Tomador de pedidos 1») se definen una vez y se usan igual en la topología, la tabla del switch y el anexo.

## 5. Criterios de aceptación
- Dado el puerto de subida marcado como 24, cuando se ubica IMP. Cocina y el técnico toca el 12, entonces la tabla dice «IMP. Cocina → puerto 12».
- Dado que el técnico toca «Ninguna luz sigue el ritmo», entonces la impresora queda como «no conectada a este switch».
- Dada una impresora con 12 % de pérdida, entonces se crea la observación del punto de red y el puerto queda «en observación».
- Dado un switch administrable con SNMP, cuando la API lee la tabla, entonces cada impresora con MAC conocida queda asignada a su puerto sin acción del técnico.
