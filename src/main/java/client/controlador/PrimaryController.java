package client.controlador;

import client.comunicacion.ConexionServidor;
import client.comunicacion.EscritorMensajes;
import client.comunicacion.LectorMensajes;
import client.comunicacion.OyenteMensajes;
import modelo.MensajeChat;
import java.io.IOException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Controlador de la ventana (VentanaReto.fxml).
 * Se encarga solo de la interfaz; la red está en el paquete "comunicacion".
 */
public class PrimaryController implements OyenteMensajes {

    // Nombres corregidos para que coincidan exactamente con los fx:id del FXML
    @FXML private TextField tfIp;
    @FXML private TextField tfPuerto;
    @FXML private TextField tfUsuario;
    @FXML private Button btnConectar;
    @FXML private Button btnDesconectar;
    @FXML private Label lblEstado; // Opcional (protegido contra nulos si no está en el FXML)

    @FXML private TextArea taChat;
    @FXML private ListView<String> lvUsuarios; 

    @FXML private CheckBox cbPrivado;
    @FXML private TextField tfMensaje;
    @FXML private Button btnEnviar;

    private ConexionServidor conexion;
    private EscritorMensajes escritor;
    private LectorMensajes lector;
    private String nombreUsuario;
    private volatile boolean conectado = false;

    @FXML
    public void initialize() {
        // Los eventos onAction ya están enlazados desde el FXML, 
        // por lo que no es necesario reasignarlos aquí con setOnAction.
        actualizarEstado(false);
    }

    @FXML
    public void conectar() {
        final String ip = tfIp.getText().trim();
        final String usuario = tfUsuario.getText().trim();
        final int puerto;
        try {
            puerto = Integer.parseInt(tfPuerto.getText().trim());
        } catch (NumberFormatException e) {
            mostrarMensaje("[ERROR] El puerto debe ser un número.");
            return;
        }
        if (usuario.isEmpty()) {
            mostrarMensaje("[AVISO] Por favor, introduce un nombre de usuario.");
            return;
        }

        nombreUsuario = usuario;
        btnConectar.setDisable(true);
        
        if (lblEstado != null) {
            lblEstado.setText("Conectando...");
            lblEstado.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
        }

        Thread hiloRed = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    conexion = new ConexionServidor(ip, puerto);
                    escritor = new EscritorMensajes(conexion.getSalida(), nombreUsuario);
                    escritor.enviarLogin();
                } catch (IOException e) {
                    if (conexion != null) conexion.cerrar();
                    mostrarMensaje("[ERROR] No se pudo conectar con el servidor: " + e.getMessage());
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            actualizarEstado(false);
                        }
                    });
                    return;
                }
                lector = new LectorMensajes(conexion.getEntrada(), PrimaryController.this);
                conectado = true;
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        actualizarEstado(true);
                    }
                });
                lector.run();
            }
        });
        hiloRed.setDaemon(true);
        hiloRed.start();
    }

    @FXML
    public void desconectar() {
        if (!conectado) return;
        conectado = false;
        if (lector != null) lector.detener();
        try {
            if (escritor != null) escritor.enviarLogout();
        } catch (IOException ignored) {}
        if (conexion != null) conexion.cerrar();
        mostrarMensaje("Desconectado del servidor.");
        actualizarEstado(false);
    }

    @FXML
    public void enviarMensaje() {
        String texto = tfMensaje.getText().trim();
        if (texto.isEmpty() || !conectado) return;

        try {
            if (cbPrivado.isSelected()) {
                String destinatario = lvUsuarios.getSelectionModel().getSelectedItem();
                if (destinatario == null || destinatario.equals(nombreUsuario)) {
                    mostrarMensaje("[AVISO] Para un privado, selecciona otro usuario en la lista de la derecha.");
                    return;
                }
                escritor.enviarPrivado(destinatario, texto);
            } else {
                escritor.enviarPublico(texto);
            }
            tfMensaje.clear();
        } catch (IOException e) {
            mostrarMensaje("[ERROR] No se pudo enviar el mensaje.");
        }
    }

    @Override
    public void mensajeRecibido(final MensajeChat msg) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                procesarMensaje(msg);
            }
        });
    }

    @Override
    public void conexionRechazada(String motivo) {
        conectado = false;
        if (conexion != null) conexion.cerrar();
        mostrarMensaje("[RECHAZADO] " + motivo);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                actualizarEstado(false);
            }
        });
    }

    @Override
    public void conexionPerdida() {
        conectado = false;
        if (conexion != null) conexion.cerrar();
        mostrarMensaje("[SISTEMA] Se ha perdido la conexión con el servidor.");
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                actualizarEstado(false);
            }
        });
    }

    private void procesarMensaje(MensajeChat msg) {
        switch (msg.getTipo()) {
            case PUBLICO:
                mostrarMensaje("[PÚBLICO] " + msg.getEmisor() + ": " + msg.getTexto());
                break;
            case PRIVADO:
                if (msg.getEmisor().equals(nombreUsuario)) {
                    mostrarMensaje("[PRIVADO para " + msg.getDestinatario() + "]: " + msg.getTexto());
                } else {
                    mostrarMensaje("[PRIVADO de " + msg.getEmisor() + "]: " + msg.getTexto());
                }
                break;
            case SERVER:
                mostrarMensaje("[INFO] " + msg.getTexto());
                break;
            case ERROR:
                mostrarMensaje("[ERROR] " + msg.getTexto());
                break;
            case USERLIST:
                lvUsuarios.getItems().setAll(msg.getUsuarios());
                break;
            default:
                break;
        }
    }

    private void actualizarEstado(boolean enLinea) {
        btnConectar.setDisable(enLinea);
        btnDesconectar.setDisable(!enLinea);
        btnEnviar.setDisable(!enLinea);
        cbPrivado.setDisable(!enLinea);
        tfIp.setDisable(enLinea);
        tfPuerto.setDisable(enLinea);
        tfUsuario.setDisable(enLinea);
        
        if (lblEstado != null) {
            if (enLinea) {
                lblEstado.setText("Conectado como " + nombreUsuario);
                lblEstado.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            } else {
                lblEstado.setText("No conectado");
                lblEstado.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                cbPrivado.setSelected(false);
                lvUsuarios.getItems().clear();
            }
        }
    }

    private void mostrarMensaje(final String texto) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                taChat.appendText(texto + "\n");
            }
        });
    }
}