package client.comunicacion;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/** Abre y cierra la conexión con el servidor (socket y sus dos streams). */
public class ConexionServidor {
    private static final int TIEMPO_MAXIMO_MS = 3000; // Máximo 3 s esperando al servidor

    private final Socket socket;
    private final ObjectOutputStream salida;
    private final ObjectInputStream entrada;

    /** Conecta con el servidor. Se llama desde el hilo de red, nunca desde el de la ventana. */
    public ConexionServidor(String ip, int puerto) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(ip, puerto), TIEMPO_MAXIMO_MS);
        // Primero la salida (con flush) y luego la entrada: si no, cliente y servidor se quedan esperando
        salida = new ObjectOutputStream(socket.getOutputStream());
        salida.flush();
        entrada = new ObjectInputStream(socket.getInputStream());
    }

    public ObjectOutputStream getSalida() {
        return salida;
    }

    public ObjectInputStream getEntrada() {
        return entrada;
    }

    /** Cerrar el socket cierra también sus streams. */
    public void cerrar() {
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
