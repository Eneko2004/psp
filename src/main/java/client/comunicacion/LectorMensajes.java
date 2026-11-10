package client.comunicacion;

import modelo.MensajeChat;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Hilo receptor de comunicaciones encubierto en una tarea {@link Runnable}.
 * <p>
 * Realiza la escucha ininterrumpida y lectura síncrona de los paquetes {@link MensajeChat} 
 * transmitidos desde el servidor por el canal de entrada. Al ejecutarse en un hilo 
 * totalmente independiente al de la interfaz gráfica, evita que la pantalla de JavaFX 
 * se congele o bloquee a la espera de nuevos datos por la red.
 * </p>
 * 
 * @author Eneko.Plaza
 * @version 1.0
 */
public class LectorMensajes implements Runnable {

    /** Canal de entrada para la deserialización de objetos transmitidos por el socket TCP. */
    private final ObjectInputStream entrada;

    /** Interfaz oyente encargada de recibir las notificaciones y pasárselas a la vista. */
    private final OyenteMensajes oyente;

    /** 
     * Flag de control de ciclo de vida del hilo.
     * Declarado como {@code volatile} para garantizar la visibilidad inmediata entre hilos 
     * (modificado desde el hilo de la ventana y leído por este hilo receptor).
     */
    private volatile boolean activo = true;

    /**
     * Construye una instancia del lector enlazándolo a su stream y al oyente correspondiente.
     * 
     * @param entrada Stream de entrada de objetos {@link ObjectInputStream} asociado al socket.
     * @param oyente Implementador del contrato {@link OyenteMensajes} para delegar eventos.
     */
    public LectorMensajes(ObjectInputStream entrada, OyenteMensajes oyente) {
        this.entrada = entrada;
        this.oyente = oyente;
    }

    /**
     * Solicita la detención ordenada de la escucha de red.
     * <p>
     * Se invoca cuando el usuario cierra la conexión manualmente, evitando que la finalización 
     * habitual del socket se interprete falsamente como una desconexión accidental del servidor.
     * </p>
     */
    public void detener() {
        activo = false;
    }

    /**
     * Bucle ejecutor principal del hilo receptor.
     * <p>
     * Mantiene la lectura continua del stream de objetos mientras la bandera {@code activo} sea cierta.
     * En caso de recibir un rechazo de servidor o interrupción de la red, notifica 
     * de forma segura al oyente asignado.
     * </p>
     */
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
        } catch (ClassNotFoundException e) {
            System.err.println("Error al deserializar la clase del mensaje entrante: " + e.getMessage());
            if (activo) {
                activo = false;
                oyente.conexionPerdida();
            }
        } catch (IOException e) {
            System.err.println("Conexión interrumpida en el flujo de entrada: " + e.getMessage());
            if (activo) {
                activo = false;
                oyente.conexionPerdida();
            }
        } catch (Exception e) {
            System.err.println("Error inesperado en el hilo lector: " + e.getMessage());
            if (activo) {
                activo = false;
                oyente.conexionPerdida();
            }
        }
    }
}