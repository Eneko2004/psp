package controlador;

import modelo.MensajeChat;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class VentanaRetoController {

    @FXML private TextField tfIp;
    @FXML private TextField tfPuerto;
    @FXML private TextField tfUsuario;
    @FXML private Button btnConectar;
    @FXML private Button btnDesconectar;
    
    @FXML private TextArea taChat;
    @FXML private ListView<String> lvUsuarios;
    @FXML private TextField tfMensaje;
    @FXML private Button btnEnviar;
    @FXML private CheckBox cbPrivado;

    private ObservableList<String> usuariosObs = FXCollections.observableArrayList();

    private Socket socket;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private boolean conectado = false;

    @FXML
    public void initialize() {
        lvUsuarios.setItems(usuariosObs);
        btnDesconectar.setDisable(true);
        btnEnviar.setDisable(true);
        cbPrivado.setDisable(true);
    }

    @FXML
    private void conectar() {
        String ip = tfIp.getText().trim();
        String puertoStr = tfPuerto.getText().trim();
        String usuario = tfUsuario.getText().trim();

        if (ip.isEmpty() || puertoStr.isEmpty() || usuario.isEmpty()) {
            mostrarAlerta("Rellena la IP, el Puerto y tu Nombre de Usuario.");
            return;
        }

        try {
            int puerto = Integer.parseInt(puertoStr);
            socket = new Socket(ip, puerto);
            
            salida = new ObjectOutputStream(socket.getOutputStream());
            salida.flush();
            entrada = new ObjectInputStream(socket.getInputStream());

            salida.writeObject(new MensajeChat(MensajeChat.Tipo.LOGIN, usuario, ""));
            salida.flush();

            conectado = true;
            btnConectar.setDisable(true);
            tfIp.setDisable(true);
            tfPuerto.setDisable(true);
            tfUsuario.setDisable(true);
            btnDesconectar.setDisable(false);
            btnEnviar.setDisable(false);
            cbPrivado.setDisable(false);

            taChat.appendText("[SISTEMA] Conectado exitosamente al servidor como: " + usuario + "\n");

            Thread hiloReceptor = new Thread(new HiloReceptor());
            hiloReceptor.setDaemon(true);
            hiloReceptor.start();

        } catch (NumberFormatException e) {
            mostrarAlerta("El puerto debe ser un número válido.");
        } catch (IOException e) {
            mostrarAlerta("No se pudo conectar al servidor: " + e.getMessage());
        }
    }

    @FXML
    private void enviarMensaje() {
        String texto = tfMensaje.getText().trim();
        String usuarioActual = tfUsuario.getText().trim();
        if (texto.isEmpty() || !conectado) return;

        try {
            if (cbPrivado.isSelected()) {
                String seleccionado = lvUsuarios.getSelectionModel().getSelectedItem();
                if (seleccionado == null || seleccionado.equals(usuarioActual)) {
                    mostrarAlerta("Selecciona un usuario válido de la lista lateral para el mensaje privado.");
                    return;
                }
                salida.writeObject(new MensajeChat(MensajeChat.Tipo.PRIVADO, usuarioActual, seleccionado, texto));
                salida.flush();
                taChat.appendText("[Tú -> Privado a " + seleccionado + "]: " + texto + "\n");
            } else {
                salida.writeObject(new MensajeChat(MensajeChat.Tipo.PUBLICO, usuarioActual, texto));
                salida.flush();
            }
            tfMensaje.clear();
        } catch (IOException e) {
            mostrarAlerta("Error al enviar el mensaje: " + e.getMessage());
        }
    }

    @FXML
    private void desconectar() {
        conectado = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        desconectarUI();
    }

    private class HiloReceptor implements Runnable {
        @Override
        public void run() {
            try {
                Object obj;
                while (conectado && (obj = entrada.readObject()) != null) {
                    if (obj instanceof MensajeChat) {
                        MensajeChat msg = (MensajeChat) obj;
                        
                        Platform.runLater(() -> {
                            if (msg.getTipo() == MensajeChat.Tipo.USERLIST) {
                                List<String> users = msg.getListaUsuarios();
                                usuariosObs.clear();
                                if (users != null) {
                                    usuariosObs.addAll(users);
                                }
                            } else if (msg.getTipo() == MensajeChat.Tipo.PRIVADO) {
                                taChat.appendText("[PRIVADO de " + msg.getEmisor() + "]: " + msg.getTexto() + "\n");
                            } else if (msg.getTipo() == MensajeChat.Tipo.PUBLICO) {
                                taChat.appendText(msg.getEmisor() + ": " + msg.getTexto() + "\n");
                            } else if (msg.getTipo() == MensajeChat.Tipo.SERVER) {
                                taChat.appendText("[SERVIDOR] " + msg.getTexto() + "\n");
                            }
                        });
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                if (conectado) {
                    Platform.runLater(() -> {
                        taChat.appendText("[SISTEMA] Conexión perdida con el servidor.\n");
                        desconectarUI();
                    });
                }
            }
        }
    }

    private void desconectarUI() {
        Platform.runLater(() -> {
            btnConectar.setDisable(false);
            tfIp.setDisable(false);
            tfPuerto.setDisable(false);
            tfUsuario.setDisable(false);
            btnDesconectar.setDisable(true);
            btnEnviar.setDisable(true);
            cbPrivado.setDisable(true);
            cbPrivado.setSelected(false);
            usuariosObs.clear();
            taChat.appendText("[SISTEMA] Desconectado.\n");
        });
    }

    private void mostrarAlerta(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Aviso");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}