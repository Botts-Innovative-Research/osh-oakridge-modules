# Monitor de portal radiológico Aspect

El controlador Aspect conecta un monitor de portal radiológico (RPM) Aspect con OpenSensorHub.

## Configuración

- Asigne un nombre claro al módulo e introduzca el número de serie del sensor.
- En **Configuración de comunicación**, añada el **Controlador de comunicación Modbus TCP**.
- Introduzca el host, el puerto y el intervalo de direcciones Modbus del RPM. El intervalo habitual es de 1 a 32.
- Configure los tiempos de espera, los reintentos y la comprobación de accesibilidad según sea necesario.
- Introduzca el ID del carril y, si se conocen, la posición y orientación del sensor.
- Active **Inicio automático** para iniciar el controlador con el nodo OpenSensorHub.
