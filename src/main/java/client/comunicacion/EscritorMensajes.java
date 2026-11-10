package client.comunicacion;

import modelo.MensajeChat;
import java.io.IOException;
import java.io.ObjectOutputStream;

/** Envía mensajes al servidor. */
public class EscritorMensajes {
    private final ObjectOutputStream salida;
    private final String nombreUsuario;

    public EscritorMensajes(ObjectOutputStream salida, String nombreUsuario) {
        this.salida = salida;
        this.nombreUsuario = nombreUsuario;
    }

    /** Primer mensaje del protocolo: LOGIN con nuestro nombre. */
    public void enviarLogin() throws IOException {
        enviar(new MensajeChat(MensajeChat.Tipo.LOGIN, nombreUsuario, ""));
    }

    public void enviarPublico(String texto) throws IOException {
        enviar(new MensajeChat(MensajeChat.Tipo.PUBLICO, nombreUsuario, texto));
    }

    public void enviarPrivado(String destinatario, String texto) throws IOException {
        enviar(new MensajeChat(MensajeChat.Tipo.PRIVADO, nombreUsuario, destinatario, texto));
    }

    /** Aviso de desconexión ordenada. */
    public void enviarLogout() throws IOException {
        enviar(new MensajeChat(MensajeChat.Tipo.LOGOUT, nombreUsuario, ""));
    }

    // synchronized: el hilo de red (LOGIN) y el de la ventana (resto) no escriben a la vez
    private synchronized void enviar(MensajeChat mensaje) throws IOException {
        salida.writeObject(mensaje);
        salida.flush();
    }
}
