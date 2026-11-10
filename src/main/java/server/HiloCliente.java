package server;

import modelo.MensajeChat;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/** Hilo que atiende a UN cliente: login, bucle de mensajes y limpieza al salir. */
public class HiloCliente implements Runnable {
    private final Socket socket;
    private final GestorChat gestor;
    private String nombreUsuario; // Solo se rellena cuando el registro sale bien

    public HiloCliente(Socket socket, GestorChat gestor) {
        this.socket = socket;
        this.gestor = gestor;
    }

    @Override
    public void run() {
        try {
            // Primero la salida (con flush) y luego la entrada: si no, cliente y servidor se quedan esperando
            ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream());
            salida.flush();
            ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());

            // 1) El primer mensaje tiene que ser LOGIN con el nombre de usuario
            MensajeChat login = (MensajeChat) entrada.readObject();
            if (login.getTipo() != MensajeChat.Tipo.LOGIN) {
                return; // Protocolo incorrecto: el socket se cierra en el finally
            }
            String motivoRechazo = gestor.registrar(login.getEmisor(), salida);
            if (motivoRechazo != null) {
                GestorChat.log("Login rechazado (" + login.getEmisor() + "): " + motivoRechazo);
                salida.writeObject(new MensajeChat(MensajeChat.Tipo.RECHAZADO, "SERVIDOR", motivoRechazo));
                salida.flush();
                return; // nombreUsuario sigue null: no se borra a nadie de la lista
            }
            nombreUsuario = login.getEmisor();

            // 2) Bucle: leer mensajes hasta que el cliente se vaya
            while (true) {
                MensajeChat msg = (MensajeChat) entrada.readObject();

                if (msg.getTipo() == MensajeChat.Tipo.LOGOUT) {
                    GestorChat.log("El usuario " + nombreUsuario + " ha solicitado desconectarse.");
                    break; // Desconexión ordenada
                } else if (msg.getTipo() == MensajeChat.Tipo.PUBLICO) {
                    // El emisor lo pone el servidor: nadie puede hacerse pasar por otro
                    gestor.mensajePublico(nombreUsuario, msg.getTexto());
                } else if (msg.getTipo() == MensajeChat.Tipo.PRIVADO) {
                    gestor.mensajePrivado(nombreUsuario, msg.getDestinatario(), msg.getTexto());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // El cliente cerró de golpe (se fue la red, cerró el programa...)
            GestorChat.log("Conexión perdida con el usuario: " + (nombreUsuario != null ? nombreUsuario : "Desconocido"));
        } finally {
            // Pase lo que pase: quitarlo de la lista (si llegó a entrar) y cerrar el socket
            gestor.eliminar(nombreUsuario);
            try {
                socket.close(); // Cierra también sus streams
            } catch (IOException e) {
                GestorChat.log("Error al cerrar el socket: " + e.getMessage());
            }
        }
    }
}
