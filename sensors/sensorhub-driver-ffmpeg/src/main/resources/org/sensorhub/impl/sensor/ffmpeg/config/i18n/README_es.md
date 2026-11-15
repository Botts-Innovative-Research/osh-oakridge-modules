# Controlador de vídeo FFmpeg

El **Controlador de vídeo FFmpeg** se conecta a una transmisión de cámara IP (RTSP/HTTP) o a un archivo de vídeo, sirve vídeo en vivo de baja latencia al Visor OSCAR y extrae fotogramas JPEG de evidencia retenidos (`frame-01.jpg` a `frame-05.jpg`) cuando ocurre una alarma de ocupación.

## Términos en lenguaje claro

| Término | Significado |
| --- | --- |
| **Fotograma clave / Intervalo GOP (`1–2 s`)** | Un fotograma de vídeo completo de referencia (`I-frame`). Se recomienda configurar la cámara con un fotograma clave cada `1–2 segundos` (`GOP 30–60` a `30 fps`) para que el vídeo en vivo inicie de inmediato y las capturas de alarma coincidan con el paso del vehículo. |
| **Medios retenidos de 5 fotogramas** | En cada ocupación con alarma, OSCAR extrae 5 fotogramas JPEG distribuidos uniformemente. Tras 30 días (`retainedSecondaryFramesDays = 30`), se depuran los fotogramas secundarios (`01`, `02`, `04`, `05`) y se conserva el fotograma central (`frame-03.jpg`). |
| **Proxy RTSP en bucle local (`mediamtx`)** | Cuando el modo de transmisión en vivo en el Servicio OSCAR es `RTSP_PROXY` o `HYBRID`, OSCAR envía los fotogramas H.264 al servidor local `mediamtx` (`127.0.0.1:8554`) para que varios operadores vean vídeo sin abrir conexiones duplicadas hacia la cámara física. |

## Conexión

- **Cadena de conexión (`Connection String`)** / **Ruta de archivo (`File Path`)**: Defina la cadena de conexión para una cámara RTSP/HTTP en vivo o la ruta de archivo para vídeo grabado.
- **Usar TCP (`Use TCP`)**: Actívelo para cámaras RTSP cuando la red requiera transporte TCP fiable.
- **Ignorar marcas de tiempo de datos (`Ignore Data Timestamps`)**: Actívelo cuando el reloj interno de la cámara no sea fiable para que OSCAR asigne la hora del servidor.

## Salidas

- **Fotogramas de vídeo (`Video Frames`)**: Publica fotogramas H.264 para visualización en vivo y extracción de los 5 fotogramas de evidencia en alarmas.
- **HLS**: Publica una salida HLS cuando se requiere reproducción basada en segmentos.
- Configure el ID de flujo, el tiempo de espera de conexión y el comportamiento de reconexión según sea necesario.

<details class="oscar-video-guide">
  <summary>Videoguía (#11, 12.0s): Inspeccionar vídeo en vivo del carril y curvas gamma/neutrones</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/11-live-lane-video-and-charts.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/11-live-lane-video-and-charts.vtt" srclang="en" label="English" default>
  </video>
</details>

<details class="oscar-video-guide">
  <summary>Videoguía (#13, 19.4s): Revisar los 5 fotogramas retenidos, el archivo diario y adjudicar</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/13-event-details-adjudication-and-daily-file.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/13-event-details-adjudication-and-daily-file.vtt" srclang="en" label="English" default>
  </video>
</details>
