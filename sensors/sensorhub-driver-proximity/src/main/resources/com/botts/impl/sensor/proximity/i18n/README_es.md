# Interruptor de proximidad inductivo

Este controlador de Raspberry Pi supervisa un interruptor de proximidad inductivo mediante entradas GPIO.

## Cableado y configuración

- Introduzca un número de serie único para el interruptor.
- Establezca **Pin normalmente cerrado** en el GPIO conectado al contacto NC blanco cuando esté cableado.
- Establezca **Pin normalmente abierto** en el GPIO conectado al contacto NO negro cuando esté cableado.
- Seleccione **No establecido** para un contacto que no esté conectado. Puede utilizar uno de los contactos o ambos.
- Compruebe la numeración GPIO y la compatibilidad eléctrica antes de iniciar el controlador.
- Active **Inicio automático** para supervisar el interruptor al iniciar el nodo.
