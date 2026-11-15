# Monitor de portal radiológico Rapiscan

El controlador Rapiscan se conecta a un RPM mediante TCP y publica datos gamma, de neutrones, de ocupación y otros datos relacionados.

## Configuración

- Introduzca un número de serie único y el ID del carril.
- En **Configuración de comunicación**, añada un proveedor TCP e introduzca el host y el puerto del RPM.
- Configure los tiempos de espera, los reintentos y la comprobación de accesibilidad.
- Introduzca la posición del sensor cuando no se herede de un sistema de carril.
- Use **Configuración gamma del hardware RPM** y **Configuración de neutrones del hardware RPM** para los parámetros de hardware gamma y de neutrones; cuando el RPM publica sus mensajes diarios de configuración (`SG1`–`SG3` y `SN1`–`SN2`) en el flujo de archivos diarios, el controlador actualiza automáticamente estos parámetros en memoria y agrupa cualquier cambio en una actualización asíncrona de `config.json` sin bloquear el flujo en tiempo real.
- Active **Análisis EML** solo para un carril EML VM250 tras cargar el complemento EML externo en **Servicios > Módulo de servicio OSCAR > Ruta del complemento EML**, e introduzca su estado de colimación y ancho. Las alarmas sin amenaza EML (`RELEASE`) se omiten del panel y se adjudican automáticamente como **Sin amenaza EML** (código `9999`) con metadatos consultables.
- Active **Inicio automático** para iniciar el controlador con el nodo.

<details class="oscar-video-guide">
  <summary>Videoguía (#07, 11.8s): Cargar complemento externo EML VM-250 y activar EML por carril</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/07-admin-eml-plugin-setup.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/07-admin-eml-plugin-setup.vtt" srclang="en" label="English" default>
  </video>
</details>
