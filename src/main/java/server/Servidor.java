package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Punto de entrada del servidor (aplicación de consola).
 * - El hilo principal solo acepta conexiones y crea un HiloCliente por cada una.
 * - Un hilo monitor muestra las estadísticas cada SEGUNDOS_MONITOR segundos.
 */
public class Servidor {
    private static final int PUERTO = 5050;
    private static final int MAX_CLIENTES = 5;
    private static final int SEGUNDOS_MONITOR = 10;

    public static void main(String[] args) {
        // Un único gestor compartido por todos los hilos
        final GestorChat gestor = new GestorChat(MAX_CLIENTES);

        // Hilo monitor: estadísticas periódicas
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
        hiloMonitor.setDaemon(true); // Se cierra solo cuando termina el servidor
        hiloMonitor.start();

        // try-with-resources: el ServerSocket se cierra solo al terminar
        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            GestorChat.log("Servidor encendido en el puerto " + PUERTO + " (Máx. clientes: " + MAX_CLIENTES + ")");

            while (true) {
                Socket socket = servidor.accept(); // Espera a que se conecte un cliente
                GestorChat.log("Nueva conexión aceptada desde: " + socket.getInetAddress());
                // Un hilo independiente para cada cliente.
                // El límite de clientes se comprueba en el LOGIN (GestorChat.registrar)
                new Thread(new HiloCliente(socket, gestor)).start();
            }
        } catch (IOException e) {
            GestorChat.log("Error en el servidor: " + e.getMessage());
        }
    }
}
