# Sistema de sensores de carril

El Sistema de carril crea un sistema principal para un monitor de portal radiológico (RPM) y una o varias cámaras de carril.

## Configuración general

- Asigne al carril un nombre de módulo y un ID únicos.
- Introduzca su latitud y longitud fijas cuando se conozcan.
- Active **Eliminar datos al quitar el carril** solo si al quitar un carril también deben eliminarse sus registros almacenados.

## Opciones del carril

- En **Configuración inicial del RPM**, seleccione Aspect, Rapiscan o RS-350 e introduzca el host y el puerto del dispositivo. Aspect también necesita un intervalo de direcciones Modbus.
- En **Configuración inicial de cámaras**, añada cada cámara Sony, Axis o personalizada e introduzca su host, nombre de usuario y contraseña cuando sean necesarios.
- Para cámaras Axis, seleccione el códec del flujo. Para cámaras personalizadas, introduzca la ruta posterior al host y puerto de la cámara.
- Revise todos los módulos secundarios generados antes de guardar el carril.
