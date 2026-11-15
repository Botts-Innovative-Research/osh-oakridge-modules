# Sistema de sensores de carril

El **Sistema de sensores de carril** crea un contenedor principal para un monitor de pórtico de radiación (RPM), su proceso de ocupación y hasta cuatro cámaras de carril.

## Términos en lenguaje claro

| Término | Significado |
| --- | --- |
| **Clave de vista operativa (`Operational View Key`)** | Etiqueta opcional (por ejemplo, `north-gate` o `cargo-inbound`) que agrupa este carril en una vista de trabajo para operadores en el Visor OSCAR. |
| **Aprendizaje automático mejorado (`EML`, Enhanced Machine Learning)** | Algoritmo opcional para Rapiscan que compara conteos gamma de baja, media y alta energía para distinguir radiación natural de carga (**NORM**, auto-adjudicada como `Code 9999: EML Non-Threat`) frente a fuentes de amenaza. |
| **OCR de vehículos (`ocrConfig`)** | Reconocimiento óptico opcional por carril (`Activar OCR de vehículos = false` de forma predeterminada) que lee de forma asíncrona números de contenedor ISO 6346 (con dígito de control) y matrículas según lo permitan los recursos de CPU y memoria, publicando sugerencias `vehicleOcr` (`PARQUET_PRIMARY`), recortes particionados por fecha (`files/ocr/crops/<YYYY-MM-DD>/`, alineados con `frame-03.jpg` y `ocrCropRetentionDays = 365`) y autocompletado de **ID del vehículo** al adjudicar. Si se desmarca `Solo alarmas`, los clips sin alarma se analizan de forma transitoria y se eliminan tras el OCR manteniendo `occupancy.videoPaths` vacío (`[]`). |
| **Rango de direcciones Modbus** | Rango de registros (`Desde` y `Hasta`) utilizado por los controladores Aspect sobre TCP/IP. |

## Configuración general

- **Nombre del módulo** e **ID único de carril**: Asigne al carril un nombre visible único y un identificador único.
- **Clave de vista operativa**: Clave opcional para asignar el carril a una vista operativa definida en el Servicio OSCAR.
- **OCR de vehículos**: Ajuste opcional por carril (`Activar OCR de vehículos`, `Solo alarmas`, `Directorio de modelos`, `Leer números de contenedor`, `Leer matrículas`, `UID de cámaras`, `Fotogramas por clip`, `Confianza mínima de contenedor`, `Confianza mínima de matrícula`, `Fracción superior ignorada`, `Fracción inferior ignorada`). Desactivado de forma predeterminada sin sobrecarga cuando está apagado.
- **Ubicación**: Introduzca la latitud y longitud fijas del carril (o coloque el marcador en el plano del sitio).
- **Eliminar datos al quitar el carril**: Actívelo **solo** cuando eliminar el carril también deba borrar sus registros históricos almacenados.

## Opciones del carril

- **Configuración inicial de RPM**:
  - Seleccione **Rapiscan**, **Aspect** o **RS-350** e introduzca el host/IP y el puerto TCP.
  - Para **Aspect**, indique también el rango de direcciones Modbus.
  - Para **Rapiscan**, configure **EML** si el nodo tiene cargado un complemento `.jar` de EML.
- **Configuración inicial de cámaras**:
  - Añada hasta **4** cámaras **Sony**, **Axis** o **Custom** por carril.
  - Para cámaras **Axis**, seleccione el códec de transmisión (`H.264`). Para cámaras **Custom**, introduzca la ruta RTSP/HTTP posterior al host y puerto.
- Haga clic en **Aplicar** y **Guardar** en la barra superior para crear e iniciar los módulos hijos.

<details class="oscar-video-guide">
  <summary>Videoguía (#06, 14.9s): Configurar un sistema de carril y plano georreferenciado del sitio</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/06-admin-manual-lane-and-site-diagram.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/06-admin-manual-lane-and-site-diagram.vtt" srclang="en" label="English" default>
  </video>
</details>

<details class="oscar-video-guide">
  <summary>Videoguía (#07, 11.8s): Cargar complemento externo EML VM-250 y activar EML por carril</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/07-admin-eml-plugin-setup.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/07-admin-eml-plugin-setup.vtt" srclang="en" label="English" default>
  </video>
</details>

<details class="oscar-video-guide">
  <summary>Videoguía (#17, 32.4s): Configurar OCR de vehículos, comparar múltiples pasadas y adjudicar</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/17-vehicle-ocr-and-adjudication.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/17-vehicle-ocr-and-adjudication.vtt" srclang="en" label="English" default>
  </video>
</details>
