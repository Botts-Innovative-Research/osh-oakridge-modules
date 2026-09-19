# Monitor de portal radiológico Rapiscan

El controlador Rapiscan se conecta a un RPM mediante TCP y publica datos gamma, de neutrones, de ocupación y otros datos relacionados.

## Configuración

- Introduzca un número de serie único y el ID del carril.
- En **Configuración de comunicación**, añada un proveedor TCP e introduzca el host y el puerto del RPM.
- Configure los tiempos de espera, los reintentos y la comprobación de accesibilidad.
- Introduzca la posición del sensor cuando no se herede de un sistema de carril.
- Use **Configuración del hardware RPM** para el intervalo gamma, la retención de ocupación y el valor umbral N sigma.
- Active **Análisis EML** solo para un carril EML VM250 e introduzca su estado de colimación y ancho.
- Active **Inicio automático** para iniciar el controlador con el nodo.
