package client.comunicacion;

import modelo.MensajeChat;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Encargada del envío serializado de mensajes hacia el servidor mediante el flujo de salida Socket.
 * <p>
 * Proporciona métodos específicos para cada tipo de mensaje definido en el protocolo 
 * (login, público, privado y logout). El método interno de envío se encuentra declarado 
 * como {@code synchronized} para impedir que el hilo principal de la ventana y los hilos de red 
 * escriban datos simultáneamente en el mismo flujo.
 * </p>
 * 
 * @author Eneko.Plaza
 * @version 1.0
 */
public class EscritorMensajes {

    /** Flujo de salida para transmitir objetos serializados hacia el servidor. */
    private final ObjectOutputStream salida;

    /** Nombre de usuario del cliente que remite las comunicaciones. */
    private final String nombreUsuario;

    /**
     * Construye una instancia de la clase asociándola a un flujo de salida y a un usuario.
     * 
     * @param salida Canal {@link ObjectOutputStream} abierto hacia el servidor.
     * @param nombreUsuario Identificador del cliente que enviará los mensajes.
     */
    public EscritorMensajes(ObjectOutputStream salida, String nombreUsuario) {
        this.salida = salida;
        this.nombreUsuario = nombreUsuario;
    }

    /**
     * Emite el mensaje inicial de autenticación en la red (LOGIN) utilizando el nombre asignado.
     * 
     * @throws IOException Si ocurre un error durante el proceso de escritura en el socket.
     */
    public void enviarLogin() throws IOException {
        enviar(new MensajeChat(MensajeChat.Tipo.LOGIN, nombreUsuario, ""));
    }

    /**
     * Envía un mensaje de difusión pública dirigido a todos los usuarios de la sala.
     * 
     * @param texto Contenido textual del mensaje.
     * @throws IOException Si falla el envío por el canal de red.
     */
    public void enviarPublico(String texto) throws IOException {
        enviar(new MensajeChat(MensajeChat.Tipo.PUBLICO, nombreUsuario, texto));
    }

    /**
     * Envía un mensaje de carácter confidencial hacia un receptor en concreto.
     * 
     * @param destinatario Nombre del usuario receptor.
     * @param texto Contenido del mensaje privado.
     * @throws IOException Si ocurre una anomalía en el flujo de transmisión.
     */
    public void enviarPrivado(String destinatario, String texto) throws IOException {
        enviar(new MensajeChat(MensajeChat.Tipo.PRIVADO, nombreUsuario, destinatario, texto));
    }

    /**
     * Envía la señal de desconexión ordenada (LOGOUT) para notificar el cierre al servidor.
     * 
     * @throws IOException Si la conexión falla durante la emisión de la señal.
     */
    public void enviarLogout() throws IOException {
        enviar(new MensajeChat(MensajeChat.Tipo.LOGOUT, nombreUsuario, ""));
    }

    /**
     * Escribe y vacía de forma síncrona el mensaje en el canal de salida de objetos.
     * <p>
     * Al llevar el modificador {@code synchronized}, se garantiza exclusión mutua 
     * para evitar que peticiones concurrentes desde la interfaz gráfica o desde 
     * hilos secundarios corrompan la cabecera o el cuerpo de los datos transmitidos.
     * </p>
     * 
     * @param mensaje Instancia serializable del paquete {@link MensajeChat}.
     * @throws IOException Si no se logra completar la operación física de E/S.
     */
    private synchronized void enviar(MensajeChat mensaje) throws IOException {
        salida.writeObject(mensaje);
        salida.flush();
    }
}