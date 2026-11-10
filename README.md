# Reto PSP: Chat Multiusuario Cliente-Servidor con Logging Persistente (JavaFX & Sockets)

Aplicación de chat red multiusuario en Java desarrollada con arquitectura cliente-servidor para el módulo de **Programación de Servicios y Procesos (PSP)** de **DAM**. 

El sistema cuenta con un servidor multihilo que gestiona conexiones simultáneas de forma síncrona/hilo-segura y registra la actividad en un fichero de logs persistente (`log/app.log`), junto con un cliente gráfico desarrollado en **JavaFX**.

---

##  Estructura del Proyecto

```text
psp/
├── log/
│   └── app.log                  # Registro persistente de eventos (generado en tiempo de ejecución)
├── src/
│   └── main/
│       ├── java/
│       │   ├── client/
│       │   │   └── comunicacion/
│       │   │       ├── ConexionServidor.java   # Gestión del Socket TCP cliente
│       │   │       ├── EscritorMensajes.java   # Envío síncrono de paquetes (synchronized)
│       │   │       ├── LectorMensajes.java     # Hilo receptor continuo (volatile)
│       │   │       └── OyenteMensajes.java     # Interfaz callback desacoplada
│       │   ├── controlador/
│       │   │   ├── GestionLogController.java # Lectura no bloqueante del log en JavaFX
│       │   │   └── PrimaryController.java   # Controlador principal de la ventana
│       │   ├── modelo/
│       │   │   └── MensajeChat.java            # Objeto DTO serializable (mensajes, login, logout)
│       │   └── server/
│       │       ├── GestorChat.java             # Monitor síncrono y persistencia en log/app.log
│       │       ├── HiloCliente.java            # Atención concurrente por usuario
│       │       └── Servidor.java               # Punto de entrada y listener de sockets
│       └── resources/
│           └── com/example/vista/
│               └── VentanaReto.fxml            # Interfaz gráfica de usuario en FXML
└── pom.xml                                     # Configuración de dependencias Maven


Reto PSP: Chat Multiusuario Cliente-Servidor (JavaFX & Sockets)

Aplicación de chat multiusuario en Java desarrollada con arquitectura cliente-servidor para la asignatura de Programación de Servicios y Procesos (PSP). El servidor es una aplicación de consola con gestión de hilos y sincronización de conexiones, mientras que el cliente dispone de una interfaz gráfica construida en JavaFX.

- Requisitos Previos

Java JDK: 17 o superior (desarrollado y probado sobre JDK 25).

Maven: 3.6 o superior.

VS Code con el paquete Extension Pack for Java.

- Cómo Ejecutar el Servidor

Abre una terminal en la raíz del proyecto.

Compila el proyecto ejecutando:

mvn clean compile


Ejecuta la clase principal del servidor:

Desde la terminal:

mvn exec:java -Dexec.mainClass="server.Servidor"


Desde VS Code: Abre el archivo src/main/java/server/Servidor.java y pulsa sobre la opción Run.

(El servidor se iniciará en el puerto 5050 y mostrará estadísticas en consola cada 10 segundos).

- Cómo Ejecutar el Cliente (JavaFX)

Con el servidor ya en ejecución, abre una segunda terminal.

Lanza el cliente de JavaFX:

mvn javafx:run


(También puedes ejecutar la clase src/main/java/client/Cliente.java desde VS Code).

- Instrucciones de Uso

Conexión al Servidor:

Revisa la dirección IP (127.0.0.1 por defecto).

Revisa el puerto (5050 por defecto).

Introduce tu nombre de usuario en el campo Usuario.

Haz clic en el botón Conectar.

Envío de Mensajes Públicos:

Asegúrate de que la casilla Mensaje Privado no esté marcada.

Escribe el texto en la parte inferior y pulsa Enviar o la tecla Enter.

Envío de Mensajes Privados:

Selecciona a un usuario de la lista lateral de Usuarios Conectados.

Marca la casilla Mensaje Privado.

Escribe tu mensaje y pulsa Enviar.

Desconexión:

Haz clic en Desconectar o cierra la ventana de la aplicación.

- Características Técnicas

Concurrencia: Hilos independientes por cada cliente conectado en el servidor (ManejadorCliente).

Sincronización de Recursos: Uso de la palabra clave synchronized en las colecciones de sockets y registros compartidos.

Monitorización: Hilo secundario daemon en el servidor que imprime estadísticas periódicas (usuarios conectados, tiempo activo y último mensaje).

Interfaz No Bloqueante: Actualización segura de JavaFX mediante Platform.runLater desde el hilo receptor del cliente.

Control de Aforo: Límite fijado en un máximo de 5 clientes simultáneos con rechazo automático de excedentes.