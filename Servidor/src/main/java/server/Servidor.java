package server;

import modelo.MensajeChat;
import java.io.*;
import java.net.*;
import java.util.*;

public class Servidor {
    private static final int PUERTO = 5000;
    private static final int MAX_CLIENTES = 10;
    private static final Map<String, ObjectOutputStream> clientes = new HashMap<>();
    private static long tiempoInicio;
    
    public static void main(String[] args) {
        tiempoInicio = System.currentTimeMillis();
        iniciarMonitor();
        
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("Servidor iniciado en el puerto " + PUERTO);
            while (true) {
                Socket socket = serverSocket.accept();
                
                synchronized (clientes) {
                    if (clientes.size() >= MAX_CLIENTES) {
                        System.out.println("Conexión rechazada: máximo de " + MAX_CLIENTES + " clientes alcanzado.");
                        socket.close();
                        continue;
                    }
                }
                
                Thread hiloCliente = new Thread(new ManejadorCliente(socket));
                hiloCliente.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void iniciarMonitor() {
        Thread monitor = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(10000); 
                        long tiempoActivo = (System.currentTimeMillis() - tiempoInicio) / 1000;
                        synchronized (clientes) {
                            System.out.println("\n=== MONITOR DEL SERVIDOR ===");
                            System.out.println("Usuarios conectados: " + clientes.size());
                            System.out.println("Tiempo activo: " + tiempoActivo + " segundos");
                            System.out.println("============================\n");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
        monitor.setDaemon(true);
        monitor.start();
    }
    
    private static class ManejadorCliente implements Runnable {
        private Socket socket;
        private String nombreUsuario;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        
        public ManejadorCliente(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                
                MensajeChat msgInicial = (MensajeChat) in.readObject();
                nombreUsuario = msgInicial.getEmisor();
                
                synchronized (clientes) {
                    if (clientes.containsKey(nombreUsuario)) {
                        MensajeChat error = new MensajeChat("Servidor", "Nombre de usuario en uso.", nombreUsuario, true);
                        out.writeObject(error);
                        socket.close();
                        return;
                    }
                    clientes.put(nombreUsuario, out);
                }
                
                System.out.println("LOG: Usuario conectado -> " + nombreUsuario);
                difundirMensaje(new MensajeChat("Servidor", nombreUsuario + " se ha unido al chat.", "", false));
                
                while (true) {
                    MensajeChat mensaje = (MensajeChat) in.readObject();
                    System.out.println("LOG: Mensaje recibido de " + mensaje.getEmisor() + (mensaje.isEsPrivado() ? " para " + mensaje.getDestinatario() : " (Público)"));
                    
                    if (mensaje.isEsPrivado()) {
                        enviarPrivado(mensaje);
                    } else {
                        difundirMensaje(mensaje);
                    }
                }
            } catch (Exception e) {
                System.out.println("LOG: Desconexión o error con usuario " + nombreUsuario);
            } finally {
                if (nombreUsuario != null) {
                    synchronized (clientes) {
                        clientes.remove(nombreUsuario);
                    }
                    System.out.println("LOG: Usuario desconectado -> " + nombreUsuario);
                    difundirMensaje(new MensajeChat("Servidor", nombreUsuario + " ha abandonado el chat.", "", false));
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        private void difundirMensaje(MensajeChat mensaje) {
            synchronized (clientes) {
                for (ObjectOutputStream clientOut : clientes.values()) {
                    try {
                        clientOut.writeObject(mensaje);
                        clientOut.flush();
                    } catch (IOException e) {
                        System.out.println("Error al enviar mensaje difuso.");
                    }
                }
            }
        }
        
        private void enviarPrivado(MensajeChat mensaje) {
            synchronized (clientes) {
                try {
                    ObjectOutputStream destOut = clientes.get(mensaje.getDestinatario());
                    if (destOut != null) {
                        destOut.writeObject(mensaje);
                        destOut.flush();
                    } else {
                        out.writeObject(new MensajeChat("Servidor", "El usuario " + mensaje.getDestinatario() + " no está conectado.", nombreUsuario, true));
                        out.flush();
                    }
                    
                    if (destOut != null && !mensaje.getEmisor().equals(mensaje.getDestinatario())) {
                        out.writeObject(mensaje);
                        out.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
