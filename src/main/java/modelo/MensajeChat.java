package modelo;

import java.io.Serializable;
import java.util.List;

public class MensajeChat implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Tipo {
        LOGIN,      
        PUBLICO,    
        PRIVADO,    
        SERVER,    
        USERLIST    
    }

    private Tipo tipo;
    private String emisor;
    private String destinatario;
    private String texto;
    private List<String> listaUsuarios;

    public MensajeChat(Tipo tipo, String emisor, String texto) {
        this.tipo = tipo;
        this.emisor = emisor;
        this.texto = texto;
    }

    public MensajeChat(Tipo tipo, String emisor, String destinatario, String texto) {
        this.tipo = tipo;
        this.emisor = emisor;
        this.destinatario = destinatario;
        this.texto = texto;
    }

    public MensajeChat(Tipo tipo, List<String> listaUsuarios) {
        this.tipo = tipo;
        this.listaUsuarios = listaUsuarios;
    }

    public Tipo getTipo() { return tipo; }
    public String getEmisor() { return emisor; }
    public String getDestinatario() { return destinatario; }
    public String getTexto() { return texto; }
    public List<String> getListaUsuarios() { return listaUsuarios; }
}