package server;

import modelo.MensajeChat;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Tarea runnable encargada de procesar las operaciones de entrada/salida de un cliente conectado.
 * <p>
 * Cada cliente aceptado por el {@link java.net.ServerSocket} se ejecuta dentro de su propio hilo 
 * de ejecución independiente. Administra el ciclo de vida de la conexión: recepción de la petición de 
 * login, lectura sostenida de mensajes de la red y liberación de sockets en la desconexión.
 * </p>
 * 
 * @author Eneko.Plaza
 * @version 1.0
 */
public class HiloCliente implements Runnable {

    /** Socket TCP/IP mediante el cual se mantiene el canal de red con el cliente. */
    private final Socket socket;

    /** Referencia al gestor global de hilos y salas de chat. */
    private final GestorChat gestor;

    /** Nombre del usuario autenticado correctamente en esta sesión de conexión. */
    private String nombreUsuario;

    /**
     * Instancia un nuevo manejador dedicado para un cliente.
     * 
     * @param socket Socket devuelto por el método {@code accept()} del servidor.
     * @param gestor Recurso compartido donde registrar acciones e intercambiar mensajes.
     */
    public HiloCliente(Socket socket, GestorChat gestor) {
        this.socket = socket;
        this.gestor = gestor;
    }

    /**
     * Método ejecutor principal del hilo que procesa el flujo de comunicación por el socket.
     */
    @Override
    public void run() {
        try {
            ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream());
            salida.flush();
            ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());

            // 1. Verificación obligatoria de inicio de sesión
            MensajeChat login = (MensajeChat) entrada.readObject();
            if (login.getTipo() != MensajeChat.Tipo.LOGIN) {
                return;
            }
            String motivoRechazo = gestor.registrar(login.getEmisor(), salida);
            if (motivoRechazo != null) {
                GestorChat.log("Login rechazado (" + login.getEmisor() + "): " + motivoRechazo);
                salida.writeObject(new MensajeChat(MensajeChat.Tipo.RECHAZADO, "SERVIDOR", motivoRechazo));
                salida.flush();
                return;
            }
            nombreUsuario = login.getEmisor();

            // 2. Escucha ininterrumpida de mensajes en el canal
            while (true) {
                MensajeChat msg = (MensajeChat) entrada.readObject();

                if (msg.getTipo() == MensajeChat.Tipo.LOGOUT) {
                    GestorChat.log("El usuario " + nombreUsuario + " ha solicitado desconectarse.");
                    break;
                } else if (msg.getTipo() == MensajeChat.Tipo.PUBLICO) {
                    gestor.mensajePublico(nombreUsuario, msg.getTexto());
                } else if (msg.getTipo() == MensajeChat.Tipo.PRIVADO) {
                    gestor.mensajePrivado(nombreUsuario, msg.getDestinatario(), msg.getTexto());
                }
            }
        } catch (IOException e) {
            GestorChat.log("Conexión perdida con el usuario: " + (nombreUsuario != null ? nombreUsuario : "Desconocido"));
        } catch (ClassNotFoundException e) {
            GestorChat.log("Conexión perdida con el usuario: " + (nombreUsuario != null ? nombreUsuario : "Desconocido"));
        } finally {
            // Liberación de recursos e informe al gestor
            gestor.eliminar(nombreUsuario);
            try {
                socket.close();
            } catch (IOException e) {
                GestorChat.log("Error al cerrar el socket: " + e.getMessage());
            }
        }
    }
}