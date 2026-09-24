# Manual de administración y operación de OSCAR 4.0.0

Este manual cubre el primer uso y la operación completa: certificado, acceso, plano georreferenciado, importación/creación de carriles, adjudicación y evidencias, eventos, estadísticas nacionales, informes y federación de nodos.

La interfaz de administración selecciona automáticamente la versión del manual que corresponde al idioma activo:

- [English](README.md)
- [Français](README_fr.md)
- [Ελληνικά](README_el.md)

> **Alcance y seguridad.** Los valores de las capturas son ejemplos. Utilice el nombre de host, las coordenadas, direcciones, puertos, usuarios, contraseñas, política de retención y base de datos del sitio real. Los cambios requieren una cuenta administradora de OSCAR. Verifique la huella del certificado con el administrador del despliegue antes de confiar en él. No importe certificados sin verificar ni reutilice las credenciales de ejemplo.

## Contenido

1. [Antes de comenzar](#1-antes-de-comenzar)
2. [Confiar en el certificado de OSCAR](#2-confiar-en-el-certificado-de-oscar)
3. [Iniciar sesión y comprender el guardado](#3-iniciar-sesión-y-comprender-el-guardado)
4. [Configurar el módulo de servicio OSCAR](#4-configurar-el-módulo-de-servicio-oscar)
5. [Crear y cargar un plano georreferenciado](#5-crear-y-cargar-un-plano-georreferenciado)
6. [Importar o exportar carriles con `config.csv`](#6-importar-o-exportar-carriles-con-configcsv)
7. [Crear manualmente un Sistema de carril](#7-crear-manualmente-un-sistema-de-carril)
8. [Operar OSCAR Viewer](#8-operar-oscar-viewer)
9. [Estadísticas nacionales](#9-estadísticas-nacionales)
10. [Generación de informes](#10-generación-de-informes)
11. [Federación de nodos](#11-federación-de-nodos)
12. [Retención de datos y almacenamiento](#12-retención-de-datos-y-almacenamiento)
13. [Lista de validación](#13-lista-de-validación)
14. [Solución de problemas](#14-solución-de-problemas)
15. [Documentación relacionada](#15-documentación-relacionada)

## 1. Antes de comenzar

Necesita:

- un despliegue OSCAR 4.0.0 instalado y en ejecución;
- la URL de OSCAR, normalmente `https://oscar.local/` salvo que se haya elegido otro host;
- una cuenta administradora para `/sensorhub/admin`;
- la imagen aprobada del sitio y la latitud/longitud de sus esquinas inferior izquierda y superior derecha;
- la dirección IP o nombre DNS y el puerto de cada RPM;
- la dirección IP o DNS, credenciales, fabricante y datos de transmisión de cada cámara; y
- un navegador compatible en un equipo que pueda resolver y alcanzar el host OSCAR.

Consulte la [Guía de inicio rápido](https://github.com/Botts-Innovative-Research/osh-oakridge-buildnode/blob/main/dist/release/QUICKSTART.md) para la instalación. La [Guía de despliegue](https://github.com/Botts-Innovative-Research/osh-oakridge-buildnode/blob/main/dist/release/DEPLOYMENT.md) describe la inicialización, DNS, certificados, comandos de ciclo de vida, controles de seguridad y actualizaciones. OSCAR no incluye una contraseña administradora predeterminada; se define durante `oscar init`.

### Convenciones de coordenadas y red

- La latitud y longitud son grados decimales WGS 84. La latitud es positiva al norte y negativa al sur; la longitud es positiva al este y negativa al oeste.
- Un rectángulo válido exige `latitud inferior izquierda < latitud superior derecha` y `longitud inferior izquierda < longitud superior derecha`. La latitud debe estar entre -90 y 90, y la longitud entre -180 y 180.
- Para un RPM, el host y el puerto se introducen por separado. La cámara no tiene un campo de puerto separado; agregue un puerto no estándar al host, por ejemplo `192.0.2.25:8554`.
- No añada `rtsp://` al host de una cámara de carril. El Sistema de carril genera la URL RTSP.

## 2. Confiar en el certificado de OSCAR

OSCAR usa HTTPS. Un despliegue con certificado autofirmado muestra una advertencia de privacidad hasta que el equipo confíe en él. Un certificado emitido por una autoridad pública o corporativa ya confiable no necesita este procedimiento.

![Advertencia de certificado en Chrome](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/01-certificate-warning.png)

### 2.1 Verificar antes de confiar

1. Confirme que la barra de direcciones contiene exactamente el host OSCAR facilitado por el administrador.
2. Solicite al administrador la huella SHA-256 del certificado de OSCAR o de su autoridad emisora.
3. Abra los detalles del certificado en el navegador y compare la huella. Deténgase si no coincide.

Las capturas usan Chrome en Windows; los textos pueden variar según la versión.

### 2.2 Exportar desde Chrome en Windows

1. Abra `https://<host-oscar>/sensorhub/admin`.
2. En la advertencia, use **Avanzado** solo después de confirmar que es el servidor esperado.
3. Abra el control de información del sitio junto a la dirección y seleccione **Detalles del certificado**.

![Abrir los detalles del certificado](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/02-certificate-details.png)

4. En el visor, seleccione el certificado que corresponda, abra **Detalles** y elija **Exportar**.

![Exportar el certificado](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/03-certificate-export.png)

5. Guárdelo como certificado X.509 codificado en Base-64, preferiblemente con extensión `.cer` o `.crt`. La extensión `.download` del ejemplo puede importarse si Windows la reconoce, pero `.cer` es más clara.

![Guardar el certificado](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/04-certificate-save.png)

Si el despliegue usa una autoridad privada, importe el **certificado de CA** proporcionado por el administrador, no un certificado de servidor obtenido de una página sin verificar.

### 2.3 Importar al almacén de confianza de Windows

1. Abra el certificado y seleccione **Instalar certificado**, o ejecute `certmgr.msc`, abra **Entidades de certificación raíz de confianza > Certificados** y seleccione **Importar**.
2. Elija **Usuario actual** para confiar solo con la sesión actual. Use **Equipo local** únicamente si la política exige confianza para todo el equipo y tiene autorización administrativa.
3. Seleccione **Colocar todos los certificados en el siguiente almacén** y **Entidades de certificación raíz de confianza**.
4. Seleccione el archivo exportado y finalice el asistente.

![Seleccionar el certificado para importar](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/05-certificate-import.png)

5. Cierre todas las ventanas del navegador, ábralo de nuevo y cargue OSCAR. La advertencia debe desaparecer y el certificado debe ser válido para el host seleccionado.

En equipos gestionados, distribuya la confianza mediante el procedimiento corporativo. En macOS, importe el certificado o la CA verificada en Acceso a Llaveros. En Linux, use el almacén del navegador o del sistema indicado por la distribución. La Guía de despliegue es la referencia para crear o sustituir el certificado del servidor.

## 3. Iniciar sesión y comprender el guardado

Abra `https://<host-oscar>/sensorhub/admin` e introduzca las credenciales creadas durante el despliegue.

![Inicio de sesión de administración](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/06-login.png)

### 3.1 Idioma

Use el selector de idioma de la parte superior de la administración o de OSCAR Viewer. Se admiten inglés, español, francés y griego. La preferencia se comparte entre ambas interfaces y se aplica al cargar o recargar la otra interfaz. Esta pestaña README abre automáticamente el manual localizado.

### 3.2 Tres acciones distintas

- **Cargar** envía inmediatamente el archivo; en el caso del CSV, inicia la importación de carriles.
- **Aplicar cambios** confirma el formulario del módulo mostrado y actualiza su configuración en ejecución.
- **Guardar** en el encabezado escribe la configuración completa para conservarla después de reiniciar.

Después de configurar, use **Aplicar cambios** cuando esté disponible y luego **Guardar**. El mensaje de carga correcta demuestra que el archivo fue aceptado, no que todos los dispositivos se conectaron ni que todo quedó persistido.

## 4. Configurar el módulo de servicio OSCAR

En la navegación izquierda, abra **Servicios > Módulo de servicio OSCAR**.

![Configuración general del servicio OSCAR](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/14-service-general.png)

### 4.1 Pestaña General

| Campo | Significado y orientación |
| --- | --- |
| Clase del módulo | Implementación Java seleccionada. Trátela como gestionada por el sistema. |
| Nombre del módulo | Nombre legible en el árbol de administración. |
| ID del módulo | ID local único. Conserve el generado salvo migración controlada. |
| Descripción | Descripción administrativa opcional. |
| Ruta de configuración de hoja de cálculo | Carga inmediatamente un CSV de carriles. **Descargar** exporta los carriles cargados como `config.csv`; no está disponible si no hay carriles. |
| ID de nodo | Identificador obligatorio y único de este nodo OSCAR. No lo reutilice en otro nodo. |
| ID de base de datos | Base de observaciones usada por OSCAR. En blanco, las estadísticas usan la base federada; los trabajos de purga/exportación específicos solo arrancan con una base elegida explícitamente. |
| Raíz de API WebID | URL base de Sandia Full Spectrum Web ID. Predeterminado: `https://full-spectrum.sandia.gov/api/v1`. Use un servicio local aprobado para despliegues aislados o déjelo vacío para no crear el cliente Web ID. |
| Frecuencia de estadísticas (min) | Intervalo de publicación; predeterminado 60 minutos. |
| Inicio automático | Inicia el servicio cuando se carga el nodo. Normalmente debe estar activo en producción. |

El servicio necesita un Servicio de almacenamiento en buckets iniciado. Si no lo encuentra, inicie y verifique **Servicios > Servicio de almacenamiento en buckets**, y reinicialice o reinicie el servicio OSCAR.

### 4.2 Pestaña Configuración del plano del sitio

Selecciona una imagen PNG/JPG/JPEG y asigna los límites inferior izquierdo y superior derecho. La sección 5 contiene el procedimiento completo.

### 4.3 Pestaña Retención de vídeo

| Campo | Predeterminado | Comportamiento |
| --- | ---: | --- |
| Tiempo hasta retención/eliminación de fotogramas clave (días) | 7 | Edad a la que se procesa un vídeo de ocupación. |
| Periodo de consulta de vídeo (minutos) | 1 | Intervalo entre búsquedas de vídeos elegibles. Un periodo mayor agrupa más registros por consulta. |
| Activar retención de fotogramas | activado | Activado: reduce clips antiguos al número indicado. Desactivado: elimina los clips elegibles. |
| Número de fotogramas clave retenidos | 5 | Fotogramas que se conservan; no tiene efecto si la retención está desactivada. |

Estas opciones afectan al vídeo histórico de ocupación, no al flujo en directo. Defina valores compatibles con la política de evidencias del sitio.

### 4.4 Pestaña Retención por presión de almacenamiento

| Campo | Predeterminado | Comportamiento |
| --- | ---: | --- |
| Uso de activación (%) | 85 | La limpieza comienza al alcanzar este uso. Debe ser mayor que el objetivo y no superar 100. |
| Uso objetivo (%) | 80 | La limpieza continúa hasta alcanzarlo o agotar archivos elegibles. |
| Periodo de comprobación (minutos) | 1 | Frecuencia de comprobación; debe ser mayor que cero. |
| Edad mínima del objeto (minutos) | 10 | Protege archivos más nuevos; no puede ser negativa. |
| Ruta de almacenamiento | `files` | Sistema de archivos cuyo uso controla la limpieza. Debe apuntar al volumen real de archivos OSCAR. |

La limpieza por presión solo elimina objetos elegibles del bucket `videos`, priorizando los candidatos antiguos; los CSV diarios están protegidos. Mantenga además supervisión del sistema operativo: este control de emergencia no sustituye las copias de seguridad.

## 5. Crear y cargar un plano georreferenciado

El plano es una imagen rectangular orientada al norte que se interpola linealmente entre dos esquinas geográficas. OSCAR muestra OSM de forma predeterminada cuando está disponible y no hay plano. Con un plano válido, Viewer ajusta la extensión a sus límites, lo dibuja como capa ráster superior y conserva los marcadores de carril por encima.

### 5.1 Abrir el formulario

1. Abra **Servicios > Módulo de servicio OSCAR > Configuración del plano del sitio**.
2. Si la configuración opcional no existe, seleccione **Añadir**.

![Configuración vacía del plano](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/07-site-diagram-empty.png)

El formulario contiene la carga y dos pares de coordenadas.

![Campos del plano](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/08-site-diagram-fields.png)

### 5.2 Preparar la imagen

1. Abra una fuente cartográfica o un plano aprobado. Si usa Google Maps, respete sus condiciones y la política de la organización.
2. Use una vista orientada al norte, sin inclinación ni rotación; OSCAR usa un rectángulo alineado con los ejes.
3. Encuadre el rectángulo más pequeño que incluya el área operativa y los carriles. Un recorte ajustado mejora el zoom inicial.
4. Capture solo el rectángulo y guárdelo como `.png`, `.jpg` o `.jpeg`.

![Mapa fuente encuadrado](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/09-source-map.png)

![Plano recortado](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/10-cropped-site-diagram.png)

Evite menús, cursor, controles del navegador y márgenes grandes. Las etiquetas o chinchetas que ya estén en la captura son píxeles de la imagen, no marcadores OSCAR.

### 5.3 Obtener los límites

Registre las coordenadas de las esquinas exactas de la imagen:

- **Límite inferior izquierdo**: esquina suroeste (`latitud`, `longitud`).
- **Límite superior derecho**: esquina noreste (`latitud`, `longitud`).

En Google Maps, haga clic derecho para mostrar y copiar la coordenada decimal. Repita en las dos esquinas opuestas del recorte.

![Copiar una coordenada de esquina](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/11-corner-coordinate.png)

Compruebe que:

- la latitud superior derecha sea mayor que la inferior izquierda;
- la longitud superior derecha sea mayor (más oriental); en el ejemplo de EE. UU. es la cifra menos negativa;
- ninguna pareja sea `0, 0`; y
- el rectángulo no sea tan grande que la instalación ocupe una parte mínima.

### 5.4 Cargar y persistir

1. Elija la imagen con **Elegir archivo**.
2. Introduzca los cuatro límites antes de pulsar **Cargar**.

![Límites introducidos](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/12-site-bounds-entered.png)

3. Pulse **Cargar** y espere el mensaje verde.

![Carga correcta del plano](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/13-site-upload-success.png)

4. Pulse **Aplicar cambios** y después **Guardar** global.
5. Abra o recargue Viewer y compruebe que el plano se alinee y ocupe la extensión inicial.

Otra carga válida sustituye la referencia activa, pero no cambia las coordenadas de los carriles. Si los límites son inválidos, corrija el orden y rango antes de reintentar.

## 6. Importar o exportar carriles con `config.csv`

La importación masiva es preferible para muchos carriles. En un sitio existente, use primero **Descargar** para obtener una plantilla exacta. Trate el archivo como sensible: los usuarios y contraseñas de cámara se exportan en texto claro.

### 6.1 Cargar el CSV

1. Abra **Servicios > Módulo de servicio OSCAR > General**.
2. En **Ruta de configuración de hoja de cálculo**, seleccione un `.csv`.

![Seleccionar CSV](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/15-csv-selected.png)

3. Pulse **Cargar** y espere la confirmación.

![CSV cargado](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/16-csv-upload-success.png)

4. Abra **Sensores** e inspeccione cada carril y sus submódulos RPM/cámara. La carga es asíncrona; espere a que aparezcan todas las filas.
5. Corrija valores por carril, aplique cambios y use **Guardar** tras validar.

![Carriles importados](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/17-sensors-overview.png)

Se omite una fila cuyo `UniqueID` ya pertenezca a un Sistema de carril cargado; no se sobrescriben carriles existentes. `UniqueID` también debe ser único dentro del propio archivo. El CSV se analiza antes de cargar módulos, pero la inicialización de hijos es asíncrona: un dispositivo que falle puede afectar a ese carril sin revertir otros ya aceptados.

### 6.2 Esquema exacto

El esquema actual tiene 14 encabezados principales obligatorios, sensibles a mayúsculas y en este orden exacto. `OperationalViews` es la sexta columna:

```csv
Name,UniqueID,AutoStart,Latitude,Longitude,OperationalViews,RPMConfigType,RPMHost,RPMPort,AspectAddressStart,AspectAddressEnd,EMLEnabled,EMLCollimated,LaneWidth
```

La importación sigue aceptando el esquema heredado de 13 encabezados sin `OperationalViews`; esos carriles no se asignan a ninguna vista operativa. Las exportaciones nuevas siempre usan 14 encabezados. Con el encabezado actual, cada fila debe incluir la celda `OperationalViews`, aunque puede estar vacía.

Después, incluya al menos un grupo de seis columnas de cámara. Use un grupo `CameraType0` vacío cuando el carril no tenga cámara; los índices adicionales deben ser consecutivos:

```csv
CameraType0,CameraHost0,CameraPath0,Codec0,Username0,Password0
CameraType1,CameraHost1,CameraPath1,Codec1,Username1,Password1
```

El código no fija un máximo de grupos secuenciales, pero el despliegue debe soportar el número total de flujos. Cada fila debe tener el mismo número de celdas que el encabezado. El analizador usa una separación simple por comas: no incluya comas ni saltos de línea dentro de valores, aunque estén entre comillas. Use celdas vacías sin comillas.

### 6.3 Columnas de carril y RPM

| Columna | Obligatoria | Valor y efecto |
| --- | --- | --- |
| `Name` | sí | Nombre visible; no puede superar 12 caracteres. |
| `UniqueID` | sí | Identificador estable y no reutilizable. Un sufijo simple se convierte en URN de carril. |
| `AutoStart` | sí | `true` o `false`; solo `true` sin distinguir mayúsculas se interpreta como verdadero. |
| `Latitude` / `Longitude` | juntas | Grados WGS 84. Deje ambas vacías para omitir ubicación fija. |
| `OperationalViews` | no | Claves de vistas de estación separadas por punto y coma, por ejemplo `north-gate;secondary`. Cada clave debe tener entre 1 y 63 letras ASCII minúsculas (`a`–`z`), números o guiones, sin guion inicial ni final. Déjela vacía para no asignar el carril a vistas delimitadas. |
| `RPMConfigType` | no | Vacío, `Aspect`, `Rapiscan` o `RS350` (sin distinguir mayúsculas). |
| `RPMHost` | con RPM | IP o DNS del RPM. |
| `RPMPort` | con RPM | Puerto TCP entero. |
| `AspectAddressStart` / `AspectAddressEnd` | solo Aspect | Rango Modbus inclusivo; ambos enteros. Valores predeterminados del formulario: 1 a 32. |
| `EMLEnabled` | solo Rapiscan | `true` solo para carril VM250/EML; de lo contrario `false`. |
| `EMLCollimated` | solo Rapiscan | Estado de colimación `true` o `false`. |
| `LaneWidth` | solo Rapiscan | Ancho en metros. Proporcione un número aun con EML desactivado; predeterminado 4.82. |

### 6.4 Columnas de cámara

| Columna | Valor y efecto |
| --- | --- |
| `CameraTypeN` | Vacío, `Sony`, `Axis` o `Custom` (sin distinguir mayúsculas). |
| `CameraHostN` | IP/DNS no vacío. Añada `:puerto` si no es el RTSP estándar. No incluya `rtsp://`. |
| `CameraPathN` | Solo Custom. Debe comenzar por `/`, por ejemplo `/stream1`. Sony y Axis generan la ruta. |
| `CodecN` | Solo Axis. `H264`/`H.264` selecciona H.264; `MJPEG`/`JPEG`, Motion JPEG. |
| `UsernameN` / `PasswordN` | Credenciales cuando sean necesarias. Proteja el archivo y elimine copias inseguras. |

El esquema no incluye **Longitud del búfer de vídeo**; la importación usa `0`. Ajuste el valor después en el hijo de cámara si se necesita.

### 6.5 Ejemplos mínimos

Un carril Rapiscan con una cámara Axis:

```csv
Name,UniqueID,AutoStart,Latitude,Longitude,OperationalViews,RPMConfigType,RPMHost,RPMPort,AspectAddressStart,AspectAddressEnd,EMLEnabled,EMLCollimated,LaneWidth,CameraType0,CameraHost0,CameraPath0,Codec0,Username0,Password0
Lane01,lane01,true,35.8858,-84.2121,north-gate,Rapiscan,192.0.2.10,1601,,,false,false,4.82,Axis,192.0.2.20,,H264,operator,replace-me
```

Un carril Aspect con dos cámaras:

```csv
Name,UniqueID,AutoStart,Latitude,Longitude,OperationalViews,RPMConfigType,RPMHost,RPMPort,AspectAddressStart,AspectAddressEnd,EMLEnabled,EMLCollimated,LaneWidth,CameraType0,CameraHost0,CameraPath0,Codec0,Username0,Password0,CameraType1,CameraHost1,CameraPath1,Codec1,Username1,Password1
Lane02,lane02,true,35.8859,-84.2119,north-gate;secondary,Aspect,192.0.2.11,502,1,32,,,,Sony,192.0.2.21,,,operator,replace-me,Custom,192.0.2.22:8554,/stream1,,operator,replace-me
```

Sustituya todas las direcciones y credenciales. Evite que la hoja de cálculo reformatee IDs, booleanos o coordenadas.

## 7. Crear manualmente un Sistema de carril

La creación manual es útil para un solo carril, hardware inusual o diagnóstico. Para sitios grandes, importe CSV y revise después cada carril.

### 7.1 Elegir la acción correcta

Haga clic derecho en **Sensores** o use su acción de alta:

- **Añadir nuevo módulo** crea un módulo de nivel superior. Úselo para un **Sistema de carril** nuevo.
- **Añadir submódulo** crea un hijo del sistema seleccionado. Úselo para adjuntar manualmente un controlador a un carril existente. No anide un Sistema de carril dentro de otro salvo que sea intencionado.
- **Reiniciar**, **Detener** y **Forzar inicialización** controlan el módulo seleccionado.
- **Eliminar módulo** borra su configuración. Si **Eliminar datos al eliminar el carril** está activo, también se borran observaciones, flujos y registros del carril.
- **Seleccionar/Deseleccionar todo** solo cambia la selección del árbol.

![Menú contextual del sensor](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/20-lane-context-menu.png)

Seleccione **Sistema de carril**.

![Selector de tipo de módulo](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/21-module-picker.png)

El selector muestra todos los tipos compatibles instalados. La compilación ilustrada contiene:

| Tipo | Uso |
| --- | --- |
| Sistema de carril | Padre de un RPM y una o más cámaras; opción normal para carriles OSCAR. |
| Controlador Aspect | Controlador RPM directo, normalmente generado dentro de un carril. |
| Controlador Rapiscan | Controlador RPM directo, normalmente generado dentro de un carril. |
| Controlador RS-350 | Controlador directo; el carril crea además el proceso de ocupación. |
| Controlador de vídeo FFmpeg | Cámara compatible con FFmpeg, normalmente generada dentro de un carril. |
| Cámara RTSP/RTP | Integración genérica, distinta de las plantillas de fabricante del carril. |
| Kromek D3S / D5 | Integraciones directas de detectores Kromek. |
| Sensor virtual SWE | Sensor genérico a partir de datos SWE/OSH existentes. |
| Sistema de sensores | Padre genérico sin el comportamiento de carril OSCAR. |

La lista varía con los paquetes instalados. Para un carril normal, seleccione **Sistema de carril** y configure sus hijos iniciales.

### 7.2 Pestaña General

![Campos generales del carril](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/22-lane-general.png)

| Campo | Orientación |
| --- | --- |
| Clase del módulo | Implementación gestionada por el sistema. |
| Nombre del módulo | Obligatorio, máximo 12 caracteres; use una convención estable como `Lane01`. |
| ID del módulo | ID local generado; no copie el de otro módulo. |
| Descripción | Descripción opcional. |
| URL de SensorML | URL opcional de una descripción SensorML base. Déjela vacía si el sitio no mantiene una. |
| ID único | ID estable obligatorio. `lane01` se convierte en `urn:osh:system:lane:lane01`; también se admite una URN completa. Evite espacios y reutilización. |
| Última actualización | Hora de actualización de SensorML; normalmente vacía/gestionada por el sistema. |
| Inicio automático | Inicia el carril al cargar la configuración. Actívelo tras verificar. |
| Claves de vista operativa | Lista opcional de claves de estación autorizadas para mostrar este carril. Añada cada clave por separado; use entre 1 y 63 letras ASCII minúsculas (`a`–`z`), números o guiones, sin guion inicial ni final. Déjela vacía si el carril no debe pertenecer a una vista delimitada. |
| Eliminar datos al eliminar el carril | Activo de forma predeterminada. Al eliminar el carril borra sus registros de base de datos. Desactívelo si debe conservar el historial. |
| Información de fuente de datos | Metadatos heredados opcionales; úselos solo si el modelo SensorML del sitio los exige. |

### 7.3 Pestaña Ubicación fija

Introduzca latitud, longitud y altitud opcional (altura sobre el elipsoide WGS 84, en metros).

Si existe un plano válido, se muestra en el formulario. Haga clic en la ubicación precisa y OSCAR calcula latitud y longitud a partir de los límites. Revise los valores antes de guardar. También puede escribir las coordenadas.

![Seleccionar ubicación en el plano](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/23-lane-location.png)

### 7.4 Pestaña Orientación fija

Usa el marco local Norte-Este-Abajo:

- **Rumbo** (guiñada): rotación sobre Z, en grados;
- **Cabeceo**: rotación sobre Y; y
- **Alabeo**: rotación sobre X.

Déjela sin definir salvo que el sitio la utilice. No sustituye la ubicación ni georreferencia el plano.

### 7.5 Pestaña Opciones del carril

Pulse **Añadir** en **Configuración RPM inicial** y el botón más/**Añadir** en **Configuración inicial de cámara**.

![Opciones iniciales vacías](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/24-lane-options-empty.png)

Las opciones iniciales generan módulos hijos al inicializar el carril. No son una segunda copia en vivo de todos los ajustes. Después, inspeccione y mantenga los hijos RPM/cámara en el árbol Sensores.

#### Tipos de RPM

Todos requieren **Host remoto** y **Puerto remoto**. Opciones adicionales:

| Tipo | Campos adicionales |
| --- | --- |
| Aspect | **Buscar dispositivo en rango — Desde/Hasta**. Rango Modbus inclusivo obligatorio; predeterminado 1–32. |
| Rapiscan | **Activar análisis EML** solo para VM250/EML; **Está colimado**; **Ancho del carril (m)**, predeterminado 4.82. |
| RS350 | Sin campos iniciales adicionales. Al arrancar se crean el controlador y el proceso de ocupación. |

No adivine puertos ni rango Aspect. Un fallo de RPM no debe detener carriles independientes, pero este carril no tendrá ocupaciones normales hasta corregirlo.

#### Tipos de cámara

Todos muestran **Host remoto**, **Usuario**, **Contraseña** y **Longitud del búfer de vídeo** (predeterminado `0`). Deje el búfer en cero salvo pruebas justificadas: aumentarlo puede consumir memoria y añadir latencia.

| Tipo | Comportamiento de flujo |
| --- | --- |
| Sony | Genera `rtsp://[credenciales@]<host>:554/media/video1`. Introduzca el host sin esquema ni puerto duplicado. |
| Axis | **H264** (predeterminado, 640×480 con intervalo de fotograma clave 15) o **MJPEG** (640×480 JPEG). |
| Personalizada | Introduzca una **Ruta de flujo** que empiece por `/`. Genera `rtsp://[credenciales@]<host><ruta>`; incluya el puerto personalizado en el host. Una ruta vacía cae en la ruta Axis H.264 y no debe usarse como configuración intencional. |

Los hijos FFmpeg generados usan TCP, solicitan 24 fps, activan HLS, desactivan fotogramas individuales, usan tiempo de conexión de 5 segundos y reintentos. La capacidad real depende de la cámara y la red.

![RPM y cámaras configurados](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/25-lane-options-filled.png)

### 7.6 Finalizar y verificar

1. Revise todas las pestañas.
2. Añada el módulo o aplique cambios si ya existe.
3. Espere la inicialización del carril y sus hijos.
4. Expanda el carril: confirme un RPM, todas las cámaras y, para RS350, el proceso de ocupación.
5. Corrija hijos si es necesario y confirme estado **Iniciado**.
6. Pulse **Guardar** global.

Consulte la documentación de [Rapiscan](https://github.com/Botts-Innovative-Research/osh-oakridge-modules/tree/main/sensors/sensorhub-driver-rapiscan), [Aspect](https://github.com/Botts-Innovative-Research/osh-oakridge-modules/tree/main/sensors/sensorhub-driver-aspect), [RS-350](https://github.com/Botts-Innovative-Research/osh-oakridge-modules/tree/main/sensors/sensorhub-driver-rs350) y [FFmpeg](https://github.com/Botts-Innovative-Research/osh-oakridge-modules/tree/main/sensors/sensorhub-driver-ffmpeg).

## 8. Operar OSCAR Viewer

Abra `https://<host-oscar>/`. El panel debe mostrar estados, tabla de eventos y mapa. Con un plano configurado, este es la imagen superior y la extensión inicial coincide con sus límites. Los marcadores OSCAR siguen siendo interactivos por encima; una chincheta dibujada en la captura solo es parte de la imagen.

![Panel OSCAR Viewer](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/18-viewer-dashboard.png)

### 8.1 Panel y cola de alarmas

1. Confirme todos los carriles en **Estado del carril**.
2. Compruebe que el marcador esté en el punto esperado.
3. Use el control de capas para comparar el plano con OSM o Esri.
4. Verifique que nuevas ocupaciones lleguen a la tabla y que tiempos, gamma, neutrones y estado sean plausibles.
5. La tabla de alarmas contiene ocupaciones con alarma aún no adjudicadas. Seleccione una fila para abrir/cerrar su vista previa; el estado distingue Gamma, Neutrón o Gamma y Neutrón por color.
6. Revise las pestañas CPS/NSIGMA y cada vídeo disponible. Las flechas cambian de elemento multimedia.
7. Use expandir o haga doble clic en la fila para abrir **Detalles del evento**.

### 8.2 Adjudicación rápida en el panel

![Adjudicación desde el panel](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/26-dashboard-adjudication.png)

Úsela cuando los gráficos y el vídeo basten y no se necesiten evidencias ni WebID:

1. Introduzca **ID del vehículo** si se conoce.
2. Elija un código de **Adjudicar** de la lista agrupada de 8.5.
3. Defina **Inspección secundaria** como **Ninguna**, **Solicitada** o **Completada**.
4. Añada **Notas** que justifiquen la decisión.
5. Pulse **Enviar**. El éxito identifica la ocupación y la alarma sale de la cola. **Restablecer** limpia el formulario local sin enviar.

El envío añade un registro de adjudicación; no modifica la observación del detector. Para evidencias, isótopos, QR, WebID o historial, abra Detalles del evento.

### 8.3 Página Eventos

![Lista de eventos](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/27-event-list.png)

**Eventos** es el historial de todos los nodos locales y federados configurados, no solo alarmas pendientes. Muestra carril/nodo, ID de ocupación, inicio/fin, máximos gamma/neutrón, estado y si existe adjudicación. Los filtros, recuentos, páginas y la selección masiva se aplican al conjunto completo de resultados coincidentes en el servidor, no solo a la página visible.

- **Columnas** muestra u oculta campos; **Filtros** abre filtros del servidor; **Densidad** cambia el espaciado.
- Cree grupos con **Todas las condiciones (Y)** o **Cualquier condición (O)** y anide grupos cuando necesite ambas lógicas. Se admiten hasta 20 reglas y tres niveles de grupos. Las condiciones de nodo y carril eligen los flujos; las de ID de ocupación, tiempo, máximos gamma/neutrón, estado y adjudicación se envían al servidor para cada carril aplicable. Al aplicar, se vuelve a la primera página.
- Use los operadores ofrecidos para cada campo. Las horas admiten antes, después y entre; los números, límites inclusivos, entre e igualdad; el estado, Ninguno, Gamma, Neutrón o Gamma y Neutrón; y la adjudicación, Sí/No y vacío/no vacío.
- Marque alarmas no adjudicadas por separado o pulse **Seleccionar todas las alarmas filtradas** para fijar el conjunto elegible completo en el momento de la carga, incluidas otras páginas. Después puede desmarcar filas concretas. La adjudicación masiva aplica a la selección un código, estado de inspección secundaria, ID de vehículo opcional y notas, con un máximo de seis eventos simultáneos. No adjunta evidencias ni selecciones de isótopos; use Detalles del evento cuando sean necesarias.
- Revise el recuento y la advertencia antes de enviar. Los eventos correctos salen de la cola o aparecen como adjudicados. Los fallidos permanecen visibles y seleccionados; **Reintentar fallidos** procesa solo esos fallos. Si OSCAR no puede enumerar todo el resultado filtrado, no selecciona nada en vez de adjudicar silenciosamente un subconjunto.
- Los resultados se ordenan del más reciente y se paginan de 15 en 15.
- Seleccione una fila para vista previa; doble clic o la acción **Detalles** abre la página completa.
- Si un nodo federado no responde, sus filas o conteos pueden estar incompletos; no interprete cero como ausencia sin comprobar conectividad.

### 8.4 Detalles del evento

![Resumen, gráficos y vídeo del evento](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/28-event-details-summary.png)

La página puede incluir:

- carril, ocupación, horas, alarma, velocidad y otros campos;
- gráficos gamma, umbral, neutrones y RS-350 compatibles;
- vídeo grabado durante el evento;
- datos varios y adjudicaciones anteriores;
- evidencias cargadas o escaneadas por QR;
- análisis Web ID e isótopos opcionales;
- ID de vehículo, notas, inspección secundaria y código; y
- **Exportar como PDF**.

**Atrás** vuelve a la lista. **CPS/NSIGMA** cambia la presentación gamma compatible; las flechas recorren vídeos. **Exportar como PDF** abre la impresión del navegador para la página renderizada; es distinto de los informes del servidor de la sección 10.

La disponibilidad depende del detector, cámara, retención, antigüedad, permisos y conectividad WebID. Un panel vacío no demuestra ausencia de datos: compruebe carril, flujo, cámara y retención.

#### Códigos de adjudicación

| Grupo | Códigos |
| --- | --- |
| Alarma real | 1 Contrabando encontrado; 2 Otro |
| Alarma inocente | 3 Isótopo médico; 4 NORM; 5 Envío declarado de material radiactivo |
| Falsa alarma | 6 Inspección física negativa; 7 RIID/ASP indica solo fondo; 8 Otro |
| Prueba/Mantenimiento | 9 Actividad autorizada de prueba, mantenimiento o formación |
| Manipulación/Fallo | 10 Actividad no autorizada |
| Otro | 11 Otro |

Elija según el procedimiento del sitio, no por conveniencia.

### 8.5 Adjudicación completa y evidencias

![Evidencias y adjudicación completa](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/29-event-details-adjudication.png)

- **Resultados de análisis WebID** muestra fecha, nombre/tipo de isótopo, confianza, tasa, texto y número de isótopos, avisos, chi cuadrado, DRF, error y dosis estimada; **Leer más** abre texto largo.
- **Adjudicaciones registradas** muestra ocupación, fecha, usuario, código, comentarios, isótopos, rutas de archivos, inspección secundaria e ID del vehículo. Una nueva adjudicación añade una fila; no reescribe las anteriores.
- **Recopilación de evidencias** y el formulario crean la decisión nueva.

#### Evidencias

1. **Subir archivos** añade uno o varios. No hay filtro de tipo en el navegador: respete la política del sitio.
2. Para analizar un espectro, active **WebID**, elija una DRF recibida del servicio Sandia Full Spectrum, **Primer plano** o **Fondo** y, solo para primer plano, **Sintetizar fondo**. Un par primer plano/fondo se envía junto.
3. **Escáner QR** usa la cámara del dispositivo para texto espectroscópico. Conceda permiso, escanee/revise/elimine códigos, configure WebID/DRF/tipo y pulse **Terminar**. Los códigos se convierten en evidencia de texto.
4. **Subir a WebID** procesa evidencias WebID aún no enviadas; permanece desactivado si no hay ninguna. Requiere acceso al bucket y al servicio Full Spectrum.
5. Seleccione resultados en **Evidencia WebID** y pulse **Usar resultado seleccionado** para aplicar sus isótopos. La decisión final sigue siendo responsabilidad del operador.

Eliminar antes del envío solo quita el elemento pendiente. Tras subirlo, use la ruta registrada y el procedimiento de retención; no suponga que quitarlo del formulario borra el objeto del servidor.

#### Formulario

1. Introduzca **ID de vehículo** si se conoce y exactamente un código.
2. Elija cero o más isótopos. **Desconocido** excluye los nombrados: Neptunio, Plutonio, Uranio-233/235/238, Americio, Bario, Bismuto, Californio, Cesio-134/137, Cobalto-57/60, Europio-152, Iridio, Manganeso, Selenio, Sodio, Estroncio, Flúor, Galio, Yodo-123/131, Indio, Paladio, Tecnecio, Xenón, Potasio, Radio y Torio.
3. Añada notas y seleccione inspección **Ninguna**, **Solicitada** o **Completada**.
4. Pulse **Enviar**, revise la confirmación completa y use **Confirmar y enviar**. Compruebe el mensaje de éxito y la fila nueva. Ante fallo, conserve el formulario y corrija nodo/flujo de control/carga antes de reintentar.

### 8.6 Transferir una alarma mediante código QR en un entorno aislado

OSCAR puede incluir un resumen compacto de la alarma en un solo código QR sin usar un servicio de Internet. La exportación contiene nodo y carril de origen, identidad y horas del evento, estado y máximos, metadatos compactos de adjudicación si existen, y datos reducidos de los gráficos gamma, neutrón y umbral. **No** contiene vídeo, archivos de evidencia, espectros ni todas las muestras originales.

Para exportar:

1. Abra la vista previa en el panel o **Detalles del evento** y pulse **Exportar alarma como QR**.
2. Espere mientras OSCAR consulta las observaciones del intervalo. El diálogo indica los puntos gamma y neutrón exportados y originales.
3. Permita que el receptor escanee el código, use **Descargar imagen QR** para obtener un PNG, **Descargar archivo de alarma** para guardar `.oscar-alarm.json`, o **Compartir alarma**. Si el navegador no puede compartir archivos, OSCAR descarga el archivo.

El muestreo conserva extremos inicial/final, mínimos/máximos globales, los puntos gamma a ambos lados de cada cruce del umbral y mínimos/máximos locales. Los valores se redondean a tres decimales. El paquete guarda un resumen SHA-256 de las series completas para compararlas posteriormente con el origen, pero las muestras omitidas no pueden reconstruirse desde el QR.

Para recibir:

1. Abra **Transferencia de alarmas** en la navegación del Viewer.
2. Use **Iniciar escaneo con cámara**, **Escanear imagen QR**, **Importar archivo de alarma** o pegue el texto `OSCAR-ALARM:1:`. La cámara exige permiso y HTTPS seguro; la importación por imagen/archivo funciona cuando la cámara está prohibida.
3. OSCAR descomprime, aplica límites de tamaño y datos, verifica el resumen SHA-256 de transferencia y vuelve a dibujar los gráficos reducidos.
4. Confirme nodo, carril, ocupación, horas, estado y gráficos; después descargue o comparta el archivo solo si está autorizado.

> **Límite de seguridad.** El paquete está comprimido y comprueba integridad, pero no está cifrado ni firmado digitalmente. El resumen detecta daños; no identifica al remitente, pues quien cambie el contenido puede calcular otro. Trátelo como vista previa portátil, confirme independientemente su origen antes del uso operativo y transfiéralo solo mediante soportes y dispositivos autorizados.

### 8.7 Supervisar el Estado de salud

Abra **Estado de salud** mediante el icono de monitor cardíaco de la navegación o vaya directamente a `/health`. La página muestra una fila por cada carril visible en el ámbito actual; `/health?view=<clave>` la limita a la vista operativa asignada.

- **Conexiones** indica el RPM y cada cámara configurada como **En línea**, **Sin conexión** o **Esperando**. Esperando significa que todavía no llegó un valor de conexión utilizable y no se considera saludable.
- **Estado de fallas** muestra gamma alta, gamma baja, neutrón alto, manipulación y ocupación prolongada como **Fallo**, **Normal** o **Esperando**. La telemetría ausente o fallida permanece en Esperando; nunca se considera saludable ni se muestra como normal.
- **Ocupación actual** usa el flujo canónico `occupancyStatus` del Sistema de carril para mostrar un tiempo estable con RPM Rapiscan, Aspect y RS350.
- **Última actualización** usa la hora local del navegador para la actualización más reciente de conexión o falla.

La ocupación prolongada usa un minuto de forma predeterminada. Introduzca de 1 a 1440 minutos en **Umbral de ocupación prolongada**. El valor se guarda en ese navegador y reevalúa de inmediato una ocupación en curso; no modifica observaciones ni informes del servidor.

Después de actualizar, confirme que cada Sistema de carril publica `occupancyStatus` y que cada RPM y cámara FFmpeg publica su conexión. Detenga y reinicie una cámara de prueba autorizada y compruebe Sin conexión y luego En línea. Confirme que la telemetría de fallas no disponible permanece en Esperando; pruebe gamma, neutrón, manipulación y ocupación larga solo conforme al procedimiento aprobado del sitio.

### 8.8 Usar una estación con vista operativa

Abra `https://<host-oscar>/?view=<clave>` o `https://<host-oscar>/view/<clave>`. OSCAR conserva la clave al navegar y limita carriles, eventos, mapas, notificaciones, informes, transferencia de alarmas y Estado de salud a los carriles asignados. La URL raíz sin ámbito carga todos los carriles. Las claves no válidas y las vistas sin carriles fallan de forma cerrada y no cargan datos.

Las vistas operativas separan la presentación de estaciones; no son un límite de autorización. Use autenticación OSCAR y controles de red cuando deba restringirse el acceso.

## 9. Estadísticas nacionales

![Estadísticas nacionales](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/30-national-statistics.png)

Una fila por nodo muestra **ID de nodo**, alarmas Gamma, Neutrón y combinadas, ocupaciones, manipulación, fallos Gamma, fallos Neutrón y fallos totales. Columnas/Filtros/Densidad ajustan la tabla.

1. Elija **Todo el tiempo**, **Últimos 30 días**, **Últimos 7 días**, **Últimas 24 horas** o **Rango personalizado**.
2. Para personalizado, introduzca inicio y fin; el fin no puede preceder al inicio.
3. Pulse **Actualizar estadísticas**. OSCAR ordena a cada flujo de control del Servicio OSCAR generar el rango. Las fechas explícitas solo se envían para Personalizado.
4. Espere y revise cada nodo. Un cero no demuestra por sí solo que el nodo respondió; investigue errores de control/conexión.

Los rangos predefinidos se almacenan en caché para cambio rápido y se recargan tras actualizar. El resultado depende del calendario estadístico, datos retenidos, relojes y red de cada nodo.

## 10. Generación de informes

![Generador de informes](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/31-report-generator.png)

El generador crea un PDF en el bucket `reports` del nodo y lo abre a la derecha.

1. Seleccione **Nodo** y tipo:
   - **RDS Site**: alarmas/tasas, EML suprimidas/tasa, fallos y capacidad libre/utilizable/total.
   - **Lane**: estadísticas de alarmas y fallos de uno o varios carriles.
   - **Adjudication**: distribuciones, porcentajes, isótopos y detalles de adjudicación por carril.
   - **Event**: seleccione **Alarmas y ocupaciones**, **Alarmas** o **Estado de salud**. Este último incluye gamma alto/bajo, neutrón alto y manipulación.
2. Para Lane/Adjudication, elija uno o varios carriles. **Seleccionar todos** alterna entre todos y ninguno.
3. Elija **Últimas 24 horas**, **7 días**, **30 días**, **Este mes** o **Rango personalizado** con ambas fechas válidas.
4. Pulse **Generar informe** y mantenga la página abierta durante los estados aceptado/pendiente. Revise el PDF y use sus controles para zoom, búsqueda, descarga o impresión. El formulario se restablece, pero el PDF queda visible.

El servidor reintenta hasta tres veces. El nombre contiene nodo/tipo/inicio/fin; una solicitud idéntica puede reutilizar el archivo existente. Solo se informan datos conservados.

## 11. Federación de nodos

![Federación de nodos](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/32-node-federation.png)

**Nodos** permite consultar varios servidores. El nodo local derivado del navegador es predeterminado y no se puede eliminar; los remotos se añaden, editan o eliminan.

1. Introduzca **Nombre** único y **Dirección** sin esquema.
2. Indique **Puerto** (inicial `8282`) y **Endpoint de Connected Systems API** (predeterminado `/api`, añadido a `/sensorhub`).
3. Escriba usuario/contraseña de lectura.
4. Active **Conexión segura** para HTTPS/WSS/MQTTS y certificado/nombre válidos.
5. Deje **Nodo solo Basic** desactivado para modo de sesión: las credenciales crean una cookie HttpOnly opaca y la contraseña se descarta. Actívelo solo sin endpoint de sesión: las credenciales viven únicamente en memoria hasta recargar/cerrar.
6. Pulse **Añadir nodo** o **Guardar**. Primero se prueba el endpoint completo; un nodo inaccesible/no autorizado no se guarda. **Cancelar** abandona cambios.

Se guardan nombre, red, TLS/modo y mapa, nunca usuario ni contraseña; entradas antiguas también se limpian al cargarse. Nombres duplicados y pares dirección/puerto duplicados se rechazan. **Eliminar** solo quita el remoto de este navegador, no borra su servidor ni datos.

Verifique carriles/eventos del remoto, su fila Nacional, un informe y un evento con multimedia. Esto prueba API, control, bucket y medios. Tras recargar, confirme la sesión; vuelva a introducir credenciales Basic. El remoto debe permitir origen, CORS, rutas, WebSocket/MQTT, permisos y confianza TLS necesarios.

## 12. Retención de datos y almacenamiento

Con un **ID de base de datos** explícito, el servicio inicia:

- cada hora, eliminación de estados de conexión de más de una hora y mediciones de alto volumen fuera de ventanas de ocupación amortiguadas; y
- a medianoche UTC, exportación del `dailyFile` del día anterior por carril al bucket `dailyfiles` y eliminación de esas observaciones ya exportadas.

Si no existen ventanas de ocupación, se omite la purga fuera de ocupación para evitar pérdida accidental. Se conserva un margen reciente y se aplica un búfer de cinco segundos alrededor de cada ocupación.

El Servicio de buckets maneja planos, hojas, vídeos, informes y exportaciones. Los registros de base de datos y los archivos necesitan copias de seguridad separadas. La retención por edad y la de presión son independientes: la primera reduce/elimina vídeos de ocupación; la segunda reacciona al uso del disco y actualmente elimina solo objetos elegibles de `videos`.

Antes de cambiar retención, base de datos, ruta o **Eliminar datos al eliminar el carril**, confirme las obligaciones de evidencias y copias.

## 13. Lista de validación

### Certificado y acceso

- [ ] El host correcto abre sin advertencia y la huella coincide.
- [ ] Funciona la cuenta administradora sin contraseña compartida/predeterminada.
- [ ] El idioma abre controles y ayuda correspondientes.

### Servicio OSCAR

- [ ] ID de nodo único; base de datos, Web ID, estadísticas y Auto Start revisados.
- [ ] Retención de vídeo y presión revisadas antes de activar limpieza.
- [ ] Servicios de buckets y OSCAR están iniciados.

### Plano

- [ ] Imagen orientada al norte, recortada y PNG/JPG/JPEG.
- [ ] Límites válidos, ordenados y no cero.
- [ ] Carga correcta, Aplicar cambios y Guardar realizados.
- [ ] Viewer usa la extensión del plano y los marcadores están alineados.

### Carriles y dispositivos

- [ ] Nombres de hasta 12 caracteres e IDs únicos.
- [ ] Tipo/host/puerto/opciones del RPM correctos.
- [ ] Tipo, host/puerto, credenciales, ruta/códec y búfer de cámaras correctos.
- [ ] Hijos esperados en estado Iniciado; un fallo de hardware no bloquea la validación de otros carriles.
- [ ] Guardado global tras importación o alta manual.

### Viewer

- [ ] Estados y marcadores se actualizan.
- [ ] Ocupación de prueba con valores plausibles.
- [ ] Vídeo directo y grabado funcionan incluso tras recargar el navegador.
- [ ] Detalles abre sin excepción de cliente.
- [ ] Una adjudicación controlada puede revisarse y enviarse.
- [ ] Se probaron filtros anidados Y/O de Eventos, selección de todo el resultado filtrado, una adjudicación masiva controlada, actualización Nacional e informes requeridos.
- [ ] Un QR de alarma controlada se exporta, escanea/importa, supera la integridad y vuelve a dibujar curvas gamma/neutrón plausibles.
- [ ] **Estado de salud** enumera todos los carriles previstos; RPM y cámaras muestran correctamente En línea/Sin conexión/Esperando, y pruebas aprobadas de gamma, neutrón, manipulación y ocupación prolongada de un minuto producen el estado previsto.
- [ ] La URL sin ámbito muestra todos los carriles, y cada URL aprobada `?view=<clave>` o `/view/<clave>` muestra solo sus carriles asignados en Panel, Eventos, informes, Transferencia de alarmas y Estado de salud.
- [ ] Se revisaron nodos federados tras recargar y no hay credenciales en almacenamiento del navegador.

## 14. Solución de problemas

| Síntoma | Comprobaciones y acción |
| --- | --- |
| Sigue la advertencia TLS | Compruebe host/SAN, fechas, almacén y ámbito usuario/equipo; reinicie completamente. Nunca ignore una huella distinta. |
| Límites inválidos | Añada ambos objetos, cuatro números finitos y compruebe rangos y orden inferior/superior. |
| Carga correcta pero sin plano | Aplique y guarde, confirme servicios iniciados, recargue Viewer y compruebe el objeto en el bucket. |
| Plano demasiado pequeño | Recorte más ajustado e introduzca las coordenadas exactas de las esquinas. |
| Sin marcador | Confirme Ubicación fija guardada y dentro de límites. Una chincheta de la captura no es un marcador OSCAR. |
| OSM 403/bloqueado | No use directamente servidores voluntarios incumpliendo su política. Configure proveedor/proxy OSM aprobado o use Esri mientras se corrige. |
| CSV rechazado | Revise encabezado/orden, grupos de seis, índices secuenciales, número de celdas, numéricos, vacíos sin comillas y ausencia de comas internas. |
| Falta un carril tras CSV | Busque UniqueID ya cargado, nombre mayor de 12, errores de hijos y espere la carga asíncrona. |
| Una vista operativa está vacía o rechaza su clave | Confirme que **Claves de vista operativa** del carril contiene exactamente la clave en minúsculas, aplique y guarde globalmente, reinicie el carril si procede y use `?view=<clave>` o `/view/<clave>`. Una asignación vacía nunca coincide con una vista delimitada. |
| RPM no inicia | Pruebe alcance del host, puerto, cortafuegos, rango Aspect y disponibilidad; revise el error del hijo. |
| Cámara no inicia | Pruebe RTSP/credenciales, quite `rtsp://` del host, no duplique puerto, confirme códec Axis o ruta Custom y pruebe desde el host OSCAR. |
| Vídeo falla al recargar | Confirme cámara e HLS iniciados y revise logs. Recargar no debe exigir recrear el carril. |
| Evento sin medios | Confirme carril disponible, flujos en el intervalo, retención y permisos. |
| Falta un carril/dispositivo en Estado de salud, o aparece Esperando/Sin conexión | Confirme que el carril pertenece a la vista actual, que el carril y el dispositivo hijo están iniciados y que el RPM o la cámara FFmpeg publica el estado de conexión. Esperando indica que aún no llegó un estado utilizable; pruebe conectividad y revise los logs del hijo. |
| No aparece la ocupación prolongada | Confirme que el Sistema de carril publica `occupancyStatus`, verifique que la entrada de archivo diario/estado del RPM informa inicio y fin, configure entre 1 y 1440 minutos en este navegador y espere a que el tiempo estable supere el umbral. |
| La adjudicación masiva falla parcialmente | Mantenga seleccionadas las filas fallidas, revise la conectividad del carril/nodo y el flujo de control de adjudicación, y pulse **Reintentar fallidos**. Las filas correctas no se vuelven a enviar. |
| Falla la adjudicación | Seleccione código; compruebe nodo/carril, flujo de control y carga de evidencias. No repita hasta conocer el resultado original. |
| WebID sin DRF/resultados | Compruebe Full Spectrum, DRF, primer plano/fondo y columnas de aviso/error. La decisión sigue siendo humana. |
| Nacional vacío/cero | Actualice el rango y compruebe control estadístico, datos retenidos y autenticación del nodo. |
| Informe no generado | Complete nodo/tipo/rango y carril/tipo de evento; compruebe fechas, control y bucket `reports`. Una solicitud idéntica puede reutilizar archivo. |
| Nodo remoto falla tras recargar | La sesión necesita cookie válida; Basic requiere reintroducir secretos. Compruebe TLS, CORS, rutas, puerto y permisos. |
| No se genera el QR de alarma | Confirme carril disponible y observaciones gamma/neutrón retenidas en el intervalo. Si no cabe en un solo QR, use el archivo de alarma descargable. |
| El QR no se escanea/importa | Aumente brillo o use el PNG a tamaño original; como alternativa importe `.oscar-alarm.json`. Un fallo de integridad exige una nueva exportación, no omitir la validación. |
| Cambios desaparecen al reiniciar | Aplique el formulario y después pulse Guardar global. |

Para soporte, registre versión OSCAR, navegador, carril/ocupación, hora y zona, estados y logs saneados. Elimine contraseñas, tokens, claves privadas y evidencias sensibles.

## 15. Documentación relacionada

- [Inicio rápido de OSCAR](https://github.com/Botts-Innovative-Research/osh-oakridge-buildnode/blob/main/dist/release/QUICKSTART.md)
- [Guía de despliegue](https://github.com/Botts-Innovative-Research/osh-oakridge-buildnode/blob/main/dist/release/DEPLOYMENT.md)
- [Sistema de traducción](https://github.com/Botts-Innovative-Research/osh-oakridge-buildnode/blob/main/docs/TRANSLATION_SYSTEM.md)
- [Módulo Sistema de carril](https://github.com/Botts-Innovative-Research/osh-oakridge-modules/tree/main/sensors/sensorhub-system-lane)
- [Módulo de servicio OSCAR](https://github.com/Botts-Innovative-Research/osh-oakridge-modules/tree/main/services/sensorhub-service-oscar)

---

Base del documento: comportamiento del código OSCAR 4.0.0, revisado el 2026-09-24. Si una versión posterior cambia campos o flujos, actualice conjuntamente el manual canónico en inglés y las tres traducciones.
