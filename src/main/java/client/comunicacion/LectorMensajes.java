package client.comunicacion;

import modelo.MensajeChat;
import java.io.ObjectInputStream;

/**
 * Hilo receptor: lee continuamente los mensajes del servidor y se los pasa al oyente.
 * Va en un hilo distinto al de la ventana, así la interfaz nunca se bloquea esperando mensajes.
 */
public class LectorMensajes implements Runnable {
    private final ObjectInputStream entrada;
    private final OyenteMensajes oyente;
    // volatile: lo cambia el hilo de la ventana (al desconectar) y lo lee este hilo
    private volatile boolean activo = true;

    public LectorMensajes(ObjectInputStream entrada, OyenteMensajes oyente) {
        this.entrada = entrada;
        this.oyente = oyente;
    }

    /** Se llama al desconectar nosotros: así el cierre del socket no se toma como "conexión perdida". */
    public void detener() {
        activo = false;
    }

    @Override
    public void run() {
        try {
            while (activo) {
                MensajeChat msg = (MensajeChat) entrada.readObject();
                if (msg.getTipo() == MensajeChat.Tipo.RECHAZADO) {
                    activo = false;
                    oyente.conexionRechazada(msg.getTexto());
                    return;
                }
                oyente.mensajeRecibido(msg);
            }
        } catch (Exception e) {
            // Si seguíamos activos, es que el servidor ha cortado la conexión
            if (activo) {
                activo = false;
                oyente.conexionPerdida();
            }
        }
    }
}
