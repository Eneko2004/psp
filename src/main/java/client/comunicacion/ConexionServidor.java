package client.comunicacion;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Gestiona el establecimiento, mantenimiento y cierre de la conexión por Socket TCP con el servidor.
 * <p>
 * Encapsula el enlace de red y la inicialización de los flujos de comunicación 
 * de objetos ({@link ObjectOutputStream} y {@link ObjectInputStream}). 
 * </p>
 * 
 * @author Eneko.Plaza
 * @version 1.0
 */
public class ConexionServidor {

    /** Tiempo máximo de espera en milisegundos (3000 ms = 3 s) para establecer la conexión con el servidor. */
    private static final int TIEMPO_MAXIMO_MS = 3000;

    /** Socket TCP utilizado para mantener el canal de comunicación con el servidor. */
    private final Socket socket;

    /** Flujo de salida para el envío de objetos serializados al servidor. */
    private final ObjectOutputStream salida;

    /** Flujo de entrada para la recepción de objetos serializados desde el servidor. */
    private final ObjectInputStream entrada;

    /**
     * Establece la conexión Socket con la dirección IP y puerto especificados.
     * <p>
     * Se debe invocar siempre desde un hilo secundario de red y jamás desde el hilo gráfico 
     * de la interfaz para evitar la congelación de la pantalla. Inicializa obligatoriamente 
     * el canal de salida primero (realizando {@code flush()}) para evitar bloqueos mutuos 
     * (deadlocks) con el servidor.
     * </p>
     * 
     * @param ip Dirección IP o nombre de host del servidor de chat.
     * @param puerto Puerto TCP en el que escucha el servidor.
     * @throws IOException Si ocurre un error de E/S o se supera el tiempo límite de conexión.
     */
    public ConexionServidor(String ip, int puerto) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(ip, puerto), TIEMPO_MAXIMO_MS);
        
        // Primero la salida (con flush) y luego la entrada para coordinar la negociación con el servidor
        salida = new ObjectOutputStream(socket.getOutputStream());
        salida.flush();
        entrada = new ObjectInputStream(socket.getInputStream());
    }

    /**
     * Obtiene el flujo de salida de objetos asociado a la conexión.
     * 
     * @return El canal {@link ObjectOutputStream} para enviar mensajes al servidor.
     */
    public ObjectOutputStream getSalida() {
        return salida;
    }

    /**
     * Obtiene el flujo de entrada de objetos asociado a la conexión.
     * 
     * @return El canal {@link ObjectInputStream} para recibir mensajes del servidor.
     */
    public ObjectInputStream getEntrada() {
        return entrada;
    }

    /**
     * Cierra de forma segura el Socket y sus flujos de datos asociados.
     * <p>
     * Al cerrar el objeto {@link Socket}, Java finaliza automáticamente 
     * los streams de entrada y salida vinculados.
     * </p>
     */
    public void cerrar() {
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar la conexión con el servidor: " + e.getMessage());
        }
    }
}