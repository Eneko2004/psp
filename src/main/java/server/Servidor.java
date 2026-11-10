package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Punto de entrada y ejecutable principal del Servidor de sockets de la aplicación.
 * <p>
 * Inicia el socket de escucha del servidor en un puerto predefinido y arranca un hilo monitor 
 * en segundo plano encargado de emitir métricas. Por cada nueva conexión entrante de cliente, 
 * delega la petición en un hilo {@link HiloCliente} independiente.
 * </p>
 * 
 * @author Eneko.Plaza
 * @version 1.0
 */
public class Servidor {

    /** Puerto TCP donde se abre la escucha del socket del servidor. */
    private static final int PUERTO = 5050;

    /** Límite de clientes concurrentes autorizados en la plataforma. */
    private static final int MAX_CLIENTES = 5;

    /** Intervalo de refresco en segundos para el hilo de monitorización de métricas. */
    private static final int SEGUNDOS_MONITOR = 10;

    /**
     * Método {@code main} de inicio del servidor.
     * 
     * @param args Argumentos de la línea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        final GestorChat gestor = new GestorChat(MAX_CLIENTES);

        // Hilo de monitorización periódico (PSP - Hilo Monitor)
        Thread hiloMonitor = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(SEGUNDOS_MONITOR * 1000L);
                    } catch (InterruptedException e) {
                        return;
                    }
                    gestor.mostrarEstadisticas();
                }
            }
        });
        hiloMonitor.setDaemon(true);
        hiloMonitor.start();

        // Bucle de conexión por socket TCP
        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            GestorChat.log("Servidor encendido en el puerto " + PUERTO + " (Máx. clientes: " + MAX_CLIENTES + ")");

            while (true) {
                Socket socket = servidor.accept();
                GestorChat.log("Nueva conexión aceptada desde: " + socket.getInetAddress());
                new Thread(new HiloCliente(socket, gestor)).start();
            }
        } catch (IOException e) {
            GestorChat.log("Error en el servidor: " + e.getMessage());
        }
    }
}