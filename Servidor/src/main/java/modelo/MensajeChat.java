package modelo;

import java.io.Serializable;

public class MensajeChat implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String emisor;
    private String mensaje;
    private String destinatario;
    private boolean esPrivado;

    public MensajeChat(String emisor, String mensaje, String destinatario, boolean esPrivado) {
        this.emisor = emisor;
        this.mensaje = mensaje;
        this.destinatario = destinatario;
        this.esPrivado = esPrivado;
    }

    public String getEmisor() { return emisor; }
    public String getMensaje() { return mensaje; }
    public String getDestinatario() { return destinatario; }
    public boolean isEsPrivado() { return esPrivado; }
}
