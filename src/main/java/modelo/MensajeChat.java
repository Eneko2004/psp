package modelo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Mensaje que viaja por el socket entre cliente y servidor (ObjectOutputStream / ObjectInputStream).
 * Está en el paquete "modelo" porque lo usan los dos: el cliente y el servidor.
 */
public class MensajeChat implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Tipo {
        LOGIN,     // Cliente -> Servidor: primer mensaje, con el nombre de usuario
        LOGOUT,    // Cliente -> Servidor: el cliente se desconecta de forma ordenada
        PUBLICO,   // Mensaje para todos
        PRIVADO,   // Mensaje para un único destinatario
        SERVER,    // Servidor -> Cliente: avisos (alguien entra o sale)
        ERROR,     // Servidor -> Cliente: error que no te echa (destinatario no disponible)
        RECHAZADO, // Servidor -> Cliente: no te deja entrar (lleno o nombre en uso) y cierra
        USERLIST   // Servidor -> Cliente: lista de usuarios conectados
    }

    private final Tipo tipo;
    private final String emisor;
    private final String destinatario;
    private final String texto;
    private final ArrayList<String> usuarios;

    // Mensajes públicos, LOGIN, LOGOUT, SERVER, ERROR y RECHAZADO
    public MensajeChat(Tipo tipo, String emisor, String texto) {
        this(tipo, emisor, null, texto);
    }

    // Mensajes privados
    public MensajeChat(Tipo tipo, String emisor, String destinatario, String texto) {
        this.tipo = tipo;
        this.emisor = emisor;
        this.destinatario = destinatario;
        this.texto = texto;
        this.usuarios = null;
    }

    // Lista de usuarios conectados (USERLIST)
    public MensajeChat(Tipo tipo, List<String> usuarios) {
        this.tipo = tipo;
        this.emisor = "SERVIDOR";
        this.destinatario = null;
        this.texto = null;
        this.usuarios = new ArrayList<>(usuarios); // Copia: ArrayList es Serializable
    }

    public Tipo getTipo() {
        return tipo;
    }

    public String getEmisor() {
        return emisor;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public String getTexto() {
        return texto;
    }

    public List<String> getUsuarios() {
        return usuarios;
    }
}
