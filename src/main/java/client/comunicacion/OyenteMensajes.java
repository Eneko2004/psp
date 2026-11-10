package client.comunicacion;

import modelo.MensajeChat;

/**
 * Lo que el LectorMensajes avisa a quien lo usa (el controlador de la ventana).
 * Así la parte de comunicación no depende de JavaFX.
 */
public interface OyenteMensajes {

    /** Ha llegado un mensaje normal (público, privado, aviso, error o lista de usuarios). */
    void mensajeRecibido(MensajeChat mensaje);

    /** El servidor no nos deja entrar (lleno o nombre en uso). */
    void conexionRechazada(String motivo);

    /** El servidor ha cortado la conexión sin que nosotros nos desconectáramos. */
    void conexionPerdida();
}
