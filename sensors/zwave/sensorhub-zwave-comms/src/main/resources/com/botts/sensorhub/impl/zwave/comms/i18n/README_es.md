# Servicio de comunicación Z-Wave

Este servicio administra la conexión serie con un controlador Z-Wave y comparte los nodos detectados con los controladores de sensores.

## Configuración

- Seleccione el puerto serie USB conectado al controlador.
- Establezca la velocidad en baudios del controlador; el valor predeterminado es 115200.
- Active **Controlador principal** cuando este dispositivo sea el controlador Z-Wave principal.
- Introduzca el ID del controlador y su posición física.
- **Controladores de sensor suscritos** muestra los nodos Z-Wave asociados actualmente al servicio.
- Inicie este servicio antes de iniciar los controladores de sensores que dependan de él.
