# Ubicación GPS al pedir la dirección

## 1. En el Chat IA
Debajo de la pregunta «¿Cuál es la dirección del local?» aparece la tarjeta **Ubicación GPS del local** con el botón **📍 Usar GPS**.

Al tocarlo:
1. Se pide el permiso de ubicación del celular (`ACCESS_FINE_LOCATION`) y se toma la posición con alta precisión.
2. Se guardan latitud, longitud, precisión (± m) y fecha y hora.
3. Se llama a la API `POST /geo/reverse` (ver API `04-geocodificacion.md`), que devuelve calle y número, distrito, provincia, departamento y si un lugar cercano **coincide con el nombre del local** ya registrado.
4. La tarjeta muestra:
   - coordenadas y precisión;
   - **Dirección** (calle y número) y **Distrito / Dpto.** (y provincia si es de provincia, por ejemplo «Chancay / Lima · provincia de Huaral»);
   - si coincide: «✓ Coincide con «[local]» en el mapa, a N m: la ubicación del local queda confirmada»;
   - botones **Usar esta dirección** (responde la pregunta de dirección) y **Ver en el mapa** (abre `https://www.google.com/maps?q=lat,lng`).
5. Si no se encuentra la dirección: se guarda igual el enlace y se pide escribir la dirección a mano.
6. Botón **Actualizar** para tomar la ubicación de nuevo.

También está en **Editar › Datos del local** y en el **Borrador de minuta** del Asistente IA.

## 2. En la minuta (Datos generales)
| Fila | Valor |
|---|---|
| Dirección | La usada (del GPS o escrita) |
| **Distrito / Departamento** (reemplaza «Ciudad») | Del GPS; si se escribió a mano, el distrito se toma de la dirección y el departamento de la tabla de distritos |
| **Ubicación GPS** | Enlace al mapa con las coordenadas, la precisión y «coincide con «[local]»» si hubo coincidencia. Sin GPS: «Pendiente · no se capturó la ubicación GPS» |

## 3. Criterios de aceptación
**CA-01 Captura**
- Dado el permiso de ubicación concedido, cuando el técnico toca «Usar GPS», entonces la tarjeta muestra coordenadas con 5 decimales, la precisión en metros y la fecha y hora.
- Dado el permiso denegado, entonces se muestra «Activa la ubicación para usar el GPS» y la dirección se puede escribir a mano.

**CA-02 Dirección y coincidencia**
- Dado un local llamado «El Chinito» y un lugar con ese nombre a menos de 100 m, cuando se consulta la ubicación, entonces se muestra «✓ Coincide con «El Chinito»».
- Cuando el técnico toca «Usar esta dirección», entonces la respuesta de dirección es la calle y el número encontrados, con el distrito.

**CA-03 Minuta**
- Dada una minuta con GPS, cuando se abre el PDF, entonces la fila «Ubicación GPS» es un enlace que abre el mapa en esas coordenadas.
- Dada la dirección «Av. Grau 450, Chancay», entonces «Distrito / Departamento» dice «Chancay / Lima».
