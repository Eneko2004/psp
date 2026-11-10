package server;

import modelo.MensajeChat;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Recurso compartido por todos los hilos del servidor: usuarios conectados, historial y estadísticas.
 *
 * Todos los métodos que tocan estos datos son "synchronized" (cerrojo = el propio gestor),
 * así que dos hilos nunca los modifican a la vez ni escriben a la vez en el mismo stream.
 */
public class GestorChat {
    private final int maxClientes;
    private final Map<String, ObjectOutputStream> clientesConectados = new HashMap<>(); // usuario -> su salida
    private final List<String> historialMensajes = new ArrayList<>(); // mensajes enviados
    private String ultimoMensaje = "Ninguno";
    private final long tiempoInicio = System.currentTimeMillis();

    public GestorChat(int maxClientes) {
        this.maxClientes = maxClientes;
    }

    /** Log global con fecha. static synchronized: las líneas de distintos hilos no se mezclan. */
    public static synchronized void log(String mensaje) {
        String fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        System.out.println("[" + fecha + "] " + mensaje);
    }

    /**
     * Registra a un usuario. Devuelve null si todo va bien o el motivo del rechazo.
     * Límite y nombre se comprueban en el MISMO synchronized: si entran dos a la vez,
     * no se pueden pasar del máximo ni coger el mismo nombre.
     */
    public synchronized String registrar(String nombre, ObjectOutputStream salida) {
        if (clientesConectados.size() >= maxClientes) {
            return "Servidor lleno (máximo " + maxClientes + " clientes). Inténtalo más tarde.";
        }
        if (nombre == null || nombre.trim().isEmpty() || clientesConectados.containsKey(nombre)) {
            return "Nombre inválido o ya en uso.";
        }
        clientesConectados.put(nombre, salida);
        log("Usuario registrado con éxito: " + nombre);
        enviarATodos(new MensajeChat(MensajeChat.Tipo.SERVER, "SERVIDOR", "El usuario " + nombre + " se ha unido al chat."));
        enviarListaUsuarios();
        return null;
    }

    /** Quita a un usuario de la lista y avisa al resto. */
    public synchronized void eliminar(String nombre) {
        if (nombre != null && clientesConectados.remove(nombre) != null) {
            log("Usuario desconectado: " + nombre);
            enviarATodos(new MensajeChat(MensajeChat.Tipo.SERVER, "SERVIDOR", "El usuario " + nombre + " ha abandonado el chat."));
            enviarListaUsuarios();
        }
    }

    /** Mensaje público: se guarda y se reenvía a todos. */
    public synchronized void mensajePublico(String emisor, String texto) {
        log("Mensaje público de [" + emisor + "]: " + texto);
        registrarMensaje(emisor + " (Público): " + texto);
        enviarATodos(new MensajeChat(MensajeChat.Tipo.PUBLICO, emisor, texto));
    }

    /**
     * MensajeChat privado: al destinatario y una copia al emisor (para que lo vea en su ventana).
     * Si el destinatario no está (o es él mismo), el emisor recibe un ERROR.
     */
    public synchronized void mensajePrivado(String emisor, String destinatario, String texto) {
        ObjectOutputStream emisorOut = clientesConectados.get(emisor);
        ObjectOutputStream destinoOut = clientesConectados.get(destinatario);

        if (destinoOut == null || destinoOut == emisorOut) {
            log("MensajeChat privado NO entregado de [" + emisor + "] para [" + destinatario + "]: no disponible");
            enviar(emisor, emisorOut, new MensajeChat(MensajeChat.Tipo.ERROR, "SERVIDOR",
                    "El usuario " + destinatario + " no está disponible."));
            return;
        }
        log("MensajeChat privado de [" + emisor + "] para [" + destinatario + "]: " + texto);
        registrarMensaje(emisor + " -> " + destinatario + " (Privado)");
        MensajeChat privado = new MensajeChat(MensajeChat.Tipo.PRIVADO, emisor, destinatario, texto);
        enviar(destinatario, destinoOut, privado);
        enviar(emisor, emisorOut, privado);
    }

    /** Lo llama el hilo monitor: clientes conectados, tiempo activo y último mensaje. */
    public synchronized void mostrarEstadisticas() {
        long segundos = (System.currentTimeMillis() - tiempoInicio) / 1000;
        String tiempoActivo = String.format("%02d:%02d:%02d", segundos / 3600, (segundos % 3600) / 60, segundos % 60);

        System.out.println("\n--- [MONITORIZACIÓN / ESTADÍSTICAS DEL SERVIDOR] ---");
        System.out.println(" Clientes conectados: " + clientesConectados.size() + " / " + maxClientes);
        System.out.println(" Tiempo activo: " + tiempoActivo);
        System.out.println(" Último mensaje recibido: " + ultimoMensaje);
        System.out.println(" Total mensajes procesados: " + historialMensajes.size());
        System.out.println("----------------------------------------------------\n");
    }

    // ----- Métodos privados: solo se llaman desde métodos que ya tienen el cerrojo -----

    private void registrarMensaje(String resumen) {
        ultimoMensaje = resumen;
        historialMensajes.add(resumen);
    }

    private void enviarATodos(MensajeChat mensaje) {
        for (Map.Entry<String, ObjectOutputStream> cliente : clientesConectados.entrySet()) {
            enviar(cliente.getKey(), cliente.getValue(), mensaje);
        }
    }

    private void enviarListaUsuarios() {
        enviarATodos(new MensajeChat(MensajeChat.Tipo.USERLIST, new ArrayList<>(clientesConectados.keySet())));
    }

    private void enviar(String nombre, ObjectOutputStream out, MensajeChat mensaje) {
        if (out == null) return;
        try {
            out.reset(); // Olvida los objetos ya enviados (si no, la lista de usuarios no se actualizaría)
            out.writeObject(mensaje);
            out.flush();
        } catch (IOException e) {
            // Ese cliente se está cayendo: su propio hilo lo detectará y lo quitará de la lista
            log("No se pudo enviar el mensaje a " + nombre + " (" + e.getMessage() + ")");
        }
    }
}
