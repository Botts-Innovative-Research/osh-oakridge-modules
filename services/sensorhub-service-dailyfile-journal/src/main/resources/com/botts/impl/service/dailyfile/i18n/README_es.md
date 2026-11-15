# Servicio de diario duradero de archivos diarios OSCAR

El **Servicio de diario duradero de archivos diarios OSCAR** registra los mensajes sin procesar de los monitores de pórtico de radiación (RPM) en un diario binario recuperable ante fallos y publica archivos CSV diarios verificados (`dailyfiles/`) junto con manifiestos de integridad criptográfica (`SHA-256`).

## Términos en lenguaje claro

| Término | Significado |
| --- | --- |
| **Archivo diario (Daily File)** | Un registro CSV estándar que contiene un día natural de mensajes RPM sin procesar para un carril: fondo (`GB`, `NB`), inicio/fin de ocupación (`GS`, `GX`), conteos gamma/neutrones (`GC`, `NC`) y alarmas (`GA`, `NA`, `NS`). |
| **Segmento de diario (Journal Segment)** | Un archivo binario de solo adición que guarda cada línea RPM entrante con una suma de verificación `CRC32C` antes de publicar el CSV diario. Si se corta la energía, OSCAR reproduce el diario al iniciar para no perder mensajes. |
| **Manifiesto y libro mayor (Manifest & Ledger)** | Archivos de verificación creados al cerrar el día. Registran el número exacto de líneas, el tamaño en bytes y la huella `SHA-256` del CSV publicado. |
| **Fsync** | Volcado de los datos en memoria al disco físico para que los mensajes aceptados sobrevivan a un reinicio inesperado del servidor. |

## Referencia de configuración

- **Raíz de almacenamiento (`Storage Root`)**: Carpeta base que contiene tanto el diario binario privado como los archivos CSV diarios publicados.
- **Directorio del diario (`Journal Directory`)**: Subcarpeta dentro de la raíz que almacena los segmentos binarios activos y sellados.
- **Directorio de publicación (`Publication Directory`)**: Subcarpeta donde se publican los archivos CSV diarios y sus archivos `.manifest.json`.
- **Zona horaria del archivo diario (`Daily File Time Zone`)**: Zona horaria IANA (por ejemplo, `UTC` o `America/New_York`) utilizada para el corte de medianoche.
- **Modo de observación de archivo diario (`Daily File Observation Mode`)**:
  - `NONE` *(recomendado con `PARQUET_PRIMARY`)*: Escribe el diario binario y los archivos CSV diarios directamente sin duplicar observaciones `dailyFile` en la base de datos.
  - `LATEST` / `FULL`: Publica observaciones de compatibilidad en la base de datos y limpia automáticamente los días completados y verificados por manifiesto.
- **Intervalo de Fsync (ms)**, **Máximo de registros sin forzar** y **Máximo de bytes sin forzar**: Controlan la frecuencia con la que se vuelcan los mensajes RPM al disco físico.
- **Máximo de bytes por segmento** y **Edad máxima de segmento (segundos)**: Controlan cuándo se rota un archivo de segmento activo.
- **Máximo de bytes por registro**: Tamaño máximo permitido para un único mensaje RPM en bytes UTF-8.
- **Retención del diario sellado (días)**: Días que se conservan los segmentos binarios sellados después de cerrar y verificar el día (`0` los conserva indefinidamente).
- **Requerir copia de seguridad antes de eliminar el diario sellado**: Si está activado, exige confirmación de copia de seguridad externa antes de borrar segmentos sellados vencidos.

<details class="oscar-video-guide">
  <summary>Videoguía (#13, 19.4s): Revisar los 5 fotogramas retenidos, el archivo diario y adjudicar</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/13-event-details-adjudication-and-daily-file.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/13-event-details-adjudication-and-daily-file.vtt" srclang="en" label="English" default>
  </video>
</details>

<details class="oscar-video-guide">
  <summary>Videoguía (#03, 11.4s): Verificar archivos Parquet de nivel frío y copia de seguridad instantánea</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/03-archive-and-snapshot-backup.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/03-archive-and-snapshot-backup.vtt" srclang="en" label="English" default>
  </video>
</details>
