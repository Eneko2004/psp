package retopsp.cliente;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import modelo.MensajeChat;

import java.io.*;
import java.net.Socket;

public class PrimaryController {
    @FXML private TextField txtIp;
    @FXML private TextField txtPuerto;
    @FXML private TextField txtUsuario;
    @FXML private Button btnConectar;
    @FXML private Button btnDesconectar;
    @FXML private Label lblEstado; // NUEVO
    
    @FXML private TextArea txtChat;
    
    @FXML private CheckBox chkPrivado; // NUEVO
    @FXML private TextField txtDestinatario;
    @FXML private TextField txtMensaje;
    @FXML private Button btnEnviar;
    
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread hiloRecepcion;
    
    @FXML
    public void initialize() {
        txtDestinatario.setDisable(true); // Desactivado por defecto
        
        chkPrivado.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                txtDestinatario.setDisable(!chkPrivado.isSelected());
            }
        });
        
        btnConectar.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                conectar();
            }
        });
        
        btnDesconectar.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                desconectar();
            }
        });
        
        btnEnviar.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                enviarMensaje();
            }
        });
        
        btnDesconectar.setDisable(true);
    }
    
    private void conectar() {
        try {
            String ip = txtIp.getText();
            int puerto = Integer.parseInt(txtPuerto.getText());
            String usuario = txtUsuario.getText();
            
            if (usuario == null || usuario.trim().isEmpty()) {
                mostrarMensaje("Por favor, introduce un nombre de usuario.");
                return;
            }
            
            socket = new Socket(ip, puerto);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            out.writeObject(new MensajeChat(usuario, "", "", false));
            out.flush();
            
            btnConectar.setDisable(true);
            btnDesconectar.setDisable(false);
            txtUsuario.setDisable(true);
            lblEstado.setText("Conectado");
            lblEstado.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            mostrarMensaje("Conectado correctamente al servidor.");
            
            hiloRecepcion = new Thread(new Runnable() {
                @Override
                public void run() {
                    recibirMensajes();
                }
            });
            hiloRecepcion.setDaemon(true);
            hiloRecepcion.start();
            
        } catch (Exception e) {
            mostrarMensaje("Error al conectar con el servidor: " + e.getMessage());
        }
    }
    
    public void desconectar() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    btnConectar.setDisable(false);
                    btnDesconectar.setDisable(true);
                    txtUsuario.setDisable(false);
                    lblEstado.setText("No conectado");
                    lblEstado.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
                    mostrarMensaje("Desconectado del servidor.");
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void enviarMensaje() {
        try {
            String texto = txtMensaje.getText();
            String dest = txtDestinatario.getText();
            
            if (texto.trim().isEmpty()) return;
            
            boolean privado = chkPrivado.isSelected() && (dest != null && !dest.trim().isEmpty());
            MensajeChat msg = new MensajeChat(txtUsuario.getText(), texto, privado ? dest : "", privado);
            
            out.writeObject(msg);
            out.flush();
            txtMensaje.clear();
        } catch (IOException e) {
            mostrarMensaje("Error al enviar el mensaje.");
        }
    }
    
    private void recibirMensajes() {
        try {
            while (true) {
                final MensajeChat msg = (MensajeChat) in.readObject();
                
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        String prefijo;
                        if (msg.getEmisor().equals("Servidor")) {
                            prefijo = "[INFO]";
                        } else if (msg.isEsPrivado()) {
                            prefijo = "[Privado de " + msg.getEmisor() + "]";
                        } else {
                            prefijo = "[" + msg.getEmisor() + "]";
                        }
                        mostrarMensaje(prefijo + " " + msg.getMensaje());
                    }
                });
            }
        } catch (Exception e) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    desconectar();
                }
            });
        }
    }
    
    private void mostrarMensaje(final String msg) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                txtChat.appendText(msg + "\n");
            }
        });
    }
}
