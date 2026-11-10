package server;

import modelo.MensajeChat;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestor del estado compartido del servidor de chat.
 * <p>
 * Esta clase actúa como un recurso crítico compartido entre múltiples hilos de ejecución 
 * (uno por cada cliente conectado). Al tener todos sus métodos de modificación declarados 
 * como {@code synchronized}, garantiza la exclusión mutua para evitar condiciones de carrera, 
 * corrupción de streams y acceso concurrente indebido a la estructura de usuarios.
 * </p>
 * 
 * @author Eneko.Plaza
 * @version 1.0
 */
public class GestorChat {

    /** Número máximo de clientes simultáneos permitidos en el servidor. */
    private final int maxClientes;

    /** Mapa concurrente que asocia el nombre de usuario con su correspondiente flujo de salida de objetos. */
    private final Map<String, ObjectOutputStream> clientesConectados = new HashMap<String, ObjectOutputStream>();

    /** Lista con el historial de mensajes procesados por el servidor. */
    private final List<String> historialMensajes = new ArrayList<String>();

    /** Registro de resumen del último mensaje procesado para monitorización. */
    private String ultimoMensaje = "Ninguno";

    /** Timestamp del momento exacto en el que inició la ejecución del servidor. */
    private final long tiempoInicio = System.currentTimeMillis();

    /**
     * Construye un gestor de chat indicando el límite máximo de conexiones.
     * 
     * @param maxClientes Número máximo de usuarios que pueden estar conectados al mismo tiempo.
     */
    public GestorChat(int maxClientes) {
        this.maxClientes = maxClientes;
    }

    /**
     * Escribe una traza de log tanto en la consola de comandos como de forma persistente en el archivo {@code log/app.log}.
     * <p>
     * Al estar declarado como {@code static synchronized}, se asegura de que si varios hilos 
     * intentan registrar un evento simultáneamente, no corrompan ni solapen el contenido del fichero físico.
     * </p>
     * 
     * @param mensaje Texto explicativo del evento a registrar.
     */
    public static synchronized void log(String mensaje) {
        String fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String textoLog = "[" + fecha + "] " + mensaje;

        // 1. Salida en la consola del sistema
        System.out.println(textoLog);

        // 2. Persistencia en fichero físico log/app.log
        try {
            File carpeta = new File("log");
            if (!carpeta.exists()) {
                carpeta.mkdirs();
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("log/app.log", true))) {
                writer.write(textoLog);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error al guardar log: " + e.getMessage());
        }
    }

    /**
     * Intenta registrar e iniciar sesión de un nuevo usuario en la plataforma.
     * <p>
     * Comprueba de manera atómica que el servidor no haya alcanzado el límite máximo 
     * de capacidad y que el nombre deseado no esté repetido o vacío.
     * </p>
     * 
     * @param nombre Nombre del usuario que intenta identificarse.
     * @param salida Flujo {@link ObjectOutputStream} asociado al socket del cliente para enviarle datos.
     * @return {@code null} si el registro fue exitoso; o un texto con la causa de rechazo en caso de fallo.
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

    /**
     * Elimina a un usuario de la lista activa de conexiones y notifica a los demás clientes.
     * 
     * @param nombre Nombre del usuario que se desconecta.
     */
    public synchronized void eliminar(String nombre) {
        if (nombre != null && clientesConectados.remove(nombre) != null) {
            log("Usuario desconectado: " + nombre);
            enviarATodos(new MensajeChat(MensajeChat.Tipo.SERVER, "SERVIDOR", "El usuario " + nombre + " ha abandonado el chat."));
            enviarListaUsuarios();
        }
    }

    /**
     * Procesamiento de un mensaje público difundido hacia la totalidad de los miembros activos.
     * 
     * @param emisor Nombre del cliente que redactó el mensaje.
     * @param texto Contenido textual del mensaje enviado.
     */
    public synchronized void mensajePublico(String emisor, String texto) {
        log("Mensaje público de [" + emisor + "]: " + texto);
        registrarMensaje(emisor + " (Público): " + texto);
        enviarATodos(new MensajeChat(MensajeChat.Tipo.PUBLICO, emisor, texto));
    }

    /**
     * Envía un mensaje en privado de un emisor concreto hacia un receptor específico.
     * 
     * @param emisor Nombre del remitente.
     * @param destinatario Nombre del receptor proyectado.
     * @param texto Contenido del mensaje confidencial.
     */
    public synchronized void mensajePrivado(String emisor, String destinatario, String texto) {
        ObjectOutputStream emisorOut = clientesConectados.get(emisor);
        ObjectOutputStream destinoOut = clientesConectados.get(destinatario);

        if (destinoOut == null || destinoOut == emisorOut) {
            log("Mensaje privado NO entregado de [" + emisor + "] para [" + destinatario + "]: no disponible");
            enviar(emisor, emisorOut, new MensajeChat(MensajeChat.Tipo.ERROR, "SERVIDOR",
                    "El usuario " + destinatario + " no está disponible."));
            return;
        }
        log("Mensaje privado de [" + emisor + "] para [" + destinatario + "]: " + texto);
        registrarMensaje(emisor + " -> " + destinatario + " (Privado)");
        MensajeChat privado = new MensajeChat(MensajeChat.Tipo.PRIVADO, emisor, destinatario, texto);
        enviar(destinatario, destinoOut, privado);
        enviar(emisor, emisorOut, privado);
    }

    /**
     * Muestra por consola las métricas de rendimiento y estado del servidor de socket.
     * <p>
     * Este método es ejecutado periódicamente por el hilo monitor independiente.
     * </p>
     */
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

    /**
     * Guarda el resumen del mensaje en la memoria interna de historial.
     * 
     * @param resumen Resumen descriptivo de la acción enviada.
     */
    private void registrarMensaje(String resumen) {
        ultimoMensaje = resumen;
        historialMensajes.add(resumen);
    }

    /**
     * Reenvía un paquete de datos {@link MensajeChat} a cada uno de los clientes conectados.
     * 
     * @param mensaje Objeto serializable del mensaje a distribuir.
     */
    private void enviarATodos(MensajeChat mensaje) {
        for (Map.Entry<String, ObjectOutputStream> cliente : clientesConectados.entrySet()) {
            enviar(cliente.getKey(), cliente.getValue(), mensaje);
        }
    }

    /**
     * Notifica el listado actualizado de usuarios activos a todos los miembros de la sala.
     */
    private void enviarListaUsuarios() {
        enviarATodos(new MensajeChat(MensajeChat.Tipo.USERLIST, new ArrayList<String>(clientesConectados.keySet())));
    }

    /**
     * Emite un mensaje de chat a través de un canal de datos hacia un cliente concreto.
     * 
     * @param nombre Nombre descriptivo del cliente destino.
     * @param out Stream de salida del canal TCP/IP.
     * @param mensaje Objeto serializado a enviar.
     */
    private void enviar(String nombre, ObjectOutputStream out, MensajeChat mensaje) {
        if (out == null) return;
        try {
            out.reset();
            out.writeObject(mensaje);
            out.flush();
        } catch (IOException e) {
            log("No se pudo enviar el mensaje a " + nombre + " (" + e.getMessage() + ")");
        }
    }
}