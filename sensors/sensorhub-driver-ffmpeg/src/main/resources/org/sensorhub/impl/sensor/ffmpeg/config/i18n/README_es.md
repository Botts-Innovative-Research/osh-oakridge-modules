# Controlador de vídeo FFmpeg

Este controlador publica vídeo desde un archivo o un flujo de red compatible con FFmpeg.

## Conexión

- Establezca **Ruta del archivo** para un vídeo grabado o **Cadena de conexión** para un flujo en directo.
- Para reproducir un archivo, configure la frecuencia de fotogramas, el tamaño del búfer y la repetición opcional.
- Para un flujo de red, seleccione TCP cuando sea necesario; de lo contrario, el controlador usa UDP.
- Active **Ignorar marcas de tiempo** cuando la fuente no contenga marcas de tiempo utilizables.

## Salidas

- Active **HLS** para publicar una salida de ruta de archivo HLS.
- Active **Fotogramas de vídeo** para publicar fotogramas binarios.
- Configure el ID del flujo, la posición de la fuente, el tiempo de espera y la reconexión según sea necesario.
