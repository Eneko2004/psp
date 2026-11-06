package server;

import modelo.MensajeChat;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Servidor {
    private static final int PUERTO = 5000;
    private static final int MAX_CLIENTES = 5;

    private static final Map<String, ObjectOutputStream> clientesConectados = new HashMap<>();

    private static long tiempoInicio;
    private static String ultimoMensaje = "Ninguno";
    private static int totalMensajes = 0;

    public static void main(String[] args) {
        tiempoInicio = System.currentTimeMillis();

        Thread hiloMonitor = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000);
                    mostrarEstadisticas();
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        hiloMonitor.setDaemon(true);
        hiloMonitor.start();

        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            log("Servidor encendido en el puerto " + PUERTO + " (Máx. clientes: " + MAX_CLIENTES + ")");

            while (true) {
                Socket socket = servidor.accept();

                synchronized (clientesConectados) {
                    if (clientesConectados.size() >= MAX_CLIENTES) {
                        log("Conexión rechazada: Se alcanzó el límite máximo de clientes.");
                        ObjectOutputStream tempOut = new ObjectOutputStream(socket.getOutputStream());
                        tempOut.writeObject(new MensajeChat(MensajeChat.Tipo.SERVER, "SERVIDOR", "Servidor lleno. Inténtalo más tarde."));
                        tempOut.flush();
                        socket.close();
                        continue;
                    }
                }

                log("Nueva conexión aceptada desde: " + socket.getInetAddress());
                new Thread(() -> manejarCliente(socket)).start();
            }
        } catch (IOException e) {
            log("Error en el servidor: " + e.getMessage());
        }
    }

    public static synchronized void log(String mensaje) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        System.out.println("[" + timestamp + "] " + mensaje);
    }

    private static synchronized void mostrarEstadisticas() {
        long segundosActivo = (System.currentTimeMillis() - tiempoInicio) / 1000;
        long horas = segundosActivo / 3600;
        long minutos = (segundosActivo % 3600) / 60;
        long segundos = segundosActivo % 60;
        String tiempoActivoStr = String.format("%02d:%02d:%02d", horas, minutos, segundos);

        System.out.println("\n--- [MONITORIZACIÓN / ESTADÍSTICAS DEL SERVIDOR] ---");
        System.out.println(" Clientes conectados: " + clientesConectados.size() + " / " + MAX_CLIENTES);
        System.out.println(" Tiempo activo: " + tiempoActivoStr);
        System.out.println(" Último mensaje recibido: " + ultimoMensaje);
        System.out.println(" Total mensajes procesados: " + totalMensajes);
        System.out.println("----------------------------------------------------\n");
    }

    private static synchronized void registrarMensaje(String msg) {
        ultimoMensaje = msg;
        totalMensajes++;
    }

    private static void manejarCliente(Socket socket) {
        String nombreUsuario = null;
        ObjectOutputStream salida = null;
        ObjectInputStream entrada = null;

        try {
            salida = new ObjectOutputStream(socket.getOutputStream());
            salida.flush();
            entrada = new ObjectInputStream(socket.getInputStream());

            MensajeChat mensajeLogin = (MensajeChat) entrada.readObject();
            if (mensajeLogin != null && mensajeLogin.getTipo() == MensajeChat.Tipo.LOGIN) {
                nombreUsuario = mensajeLogin.getEmisor();

                synchronized (clientesConectados) {
                    if (nombreUsuario == null || nombreUsuario.trim().isEmpty() || clientesConectados.containsKey(nombreUsuario)) {
                        salida.writeObject(new MensajeChat(MensajeChat.Tipo.SERVER, "SERVIDOR", "Nombre inválido o ya en uso."));
                        salida.flush();
                        socket.close();
                        return;
                    }
                    clientesConectados.put(nombreUsuario, salida);
                }

                log("Usuario registrado con éxito: " + nombreUsuario);
                broadcast(new MensajeChat(MensajeChat.Tipo.SERVER, "SERVIDOR", "El usuario " + nombreUsuario + " se ha unido al chat."));
                enviarListaUsuarios();
            } else {
                socket.close();
                return;
            }

            MensajeChat msgEntrante;
            while ((msgEntrante = (MensajeChat) entrada.readObject()) != null) {
                if (msgEntrante.getTipo() == MensajeChat.Tipo.PRIVADO) {
                    log("Mensaje privado de [" + nombreUsuario + "] para [" + msgEntrante.getDestinatario() + "]: " + msgEntrante.getTexto());
                    registrarMensaje(nombreUsuario + " -> " + msgEntrante.getDestinatario() + " (Privado)");
                    enviarPrivado(nombreUsuario, msgEntrante.getDestinatario(), msgEntrante.getTexto());
                } else if (msgEntrante.getTipo() == MensajeChat.Tipo.PUBLICO) {
                    log("Mensaje público de [" + nombreUsuario + "]: " + msgEntrante.getTexto());
                    registrarMensaje(nombreUsuario + " (Público): " + msgEntrante.getTexto());
                    broadcast(new MensajeChat(MensajeChat.Tipo.PUBLICO, nombreUsuario, msgEntrante.getTexto()));
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            log("Conexión perdida con el usuario: " + (nombreUsuario != null ? nombreUsuario : "Desconocido"));
        } finally {
            if (nombreUsuario != null) {
                synchronized (clientesConectados) {
                    clientesConectados.remove(nombreUsuario);
                }
                log("Usuario desconectado: " + nombreUsuario);
                broadcast(new MensajeChat(MensajeChat.Tipo.SERVER, "SERVIDOR", "El usuario " + nombreUsuario + " ha abandonado el chat."));
                enviarListaUsuarios();
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void broadcast(MensajeChat mensaje) {
        synchronized (clientesConectados) {
            for (ObjectOutputStream out : clientesConectados.values()) {
                try {
                    out.writeObject(mensaje);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void enviarPrivado(String emisor, String destinatario, String texto) {
        synchronized (clientesConectados) {
            ObjectOutputStream destinoOut = clientesConectados.get(destinatario);
            if (destinoOut != null) {
                try {
                    destinoOut.writeObject(new MensajeChat(MensajeChat.Tipo.PRIVADO, emisor, destinatario, texto));
                    destinoOut.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                ObjectOutputStream emisorOut = clientesConectados.get(emisor);
                if (emisorOut != null) {
                    try {
                        emisorOut.writeObject(new MensajeChat(MensajeChat.Tipo.SERVER, "SERVIDOR", "El usuario " + destinatario + " no está disponible."));
                        emisorOut.flush();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    private static void enviarListaUsuarios() {
        synchronized (clientesConectados) {
            List<String> lista = new ArrayList<>(clientesConectados.keySet());
            MensajeChat msgLista = new MensajeChat(MensajeChat.Tipo.USERLIST, lista);
            for (ObjectOutputStream out : clientesConectados.values()) {
                try {
                    out.writeObject(msgLista);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}