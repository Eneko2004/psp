package client;

import modelo.MensajeChat;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class Cliente extends Application {

    // Componentes JavaFX
    private TextField tfIp = new TextField("127.0.0.1");
    private TextField tfPuerto = new TextField("5050");
    private TextField tfUsuario = new TextField();
    private Button btnConectar = new Button("Conectar");
    private Button btnDesconectar = new Button("Desconectar");

    private TextArea taChat = new TextArea();
    private ListView<String> lvUsuarios = new ListView<>();
    private TextField tfMensaje = new TextField();
    private Button btnEnviar = new Button("Enviar");
    private CheckBox cbPrivado = new CheckBox("Mensaje Privado");

    private ObservableList<String> usuariosObs = FXCollections.observableArrayList();

    // Sockets y Streams de Objetos
    private Socket socket;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private boolean conectado = false;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Cliente Chat Multiusuario - PSP");

        // Configuración panel superior
        HBox panelSuperior = new HBox(10);
        panelSuperior.setPadding(new Insets(10));
        btnDesconectar.setDisable(true);
        panelSuperior.getChildren().addAll(
            new Label("IP:"), tfIp,
            new Label("Puerto:"), tfPuerto,
            new Label("Usuario:"), tfUsuario,
            btnConectar, btnDesconectar
        );

        // Configuración panel central
        taChat.setEditable(false);
        taChat.setWrapText(true);
        lvUsuarios.setItems(usuariosObs);
        lvUsuarios.setPrefWidth(150);

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(taChat, lvUsuarios);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // Configuración panel inferior
        HBox panelInferior = new HBox(10);
        panelInferior.setPadding(new Insets(10));
        tfMensaje.setPromptText("Escribe tu mensaje aquí...");
        HBox.setHgrow(tfMensaje, Priority.ALWAYS);
        btnEnviar.setDisable(true);
        cbPrivado.setDisable(true);
        panelInferior.getChildren().addAll(cbPrivado, tfMensaje, btnEnviar);

        VBox root = new VBox(10);
        root.getChildren().addAll(panelSuperior, splitPane, panelInferior);

        // Eventos
        btnConectar.setOnAction(e -> conectar());
        btnDesconectar.setOnAction(e -> desconectar());
        btnEnviar.setOnAction(e -> enviarMensaje());
        tfMensaje.setOnAction(e -> enviarMensaje());

        Scene scene = new Scene(root, 700, 450);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> desconectar());
        primaryStage.show();
    }

    private void conectar() {
        String ip = tfIp.getText().trim();
        String puertoStr = tfPuerto.getText().trim();
        String usuario = tfUsuario.getText().trim();

        if (ip.isEmpty() || puertoStr.isEmpty() || usuario.isEmpty()) {
            mostrarAlerta("Por favor rellena la IP, el Puerto y tu Nombre de Usuario.");
            return;
        }

        try {
            int puerto = Integer.parseInt(puertoStr);
            socket = new Socket(ip, puerto);
            
            // IMPORTANTE: Orden de apertura de streams para evitar bloqueo de socket
            salida = new ObjectOutputStream(socket.getOutputStream());
            salida.flush();
            entrada = new ObjectInputStream(socket.getInputStream());

            // Enviar mensaje de LOGIN usando el modelo MensajeChat
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

            // Hilo receptor independiente
            Thread hiloReceptor = new Thread(new HiloReceptor());
            hiloReceptor.setDaemon(true);
            hiloReceptor.start();

        } catch (NumberFormatException e) {
            mostrarAlerta("El puerto debe ser un número entero válido.");
        } catch (IOException e) {
            mostrarAlerta("No se pudo conectar al servidor: " + e.getMessage());
        }
    }

    private void enviarMensaje() {
        String texto = tfMensaje.getText().trim();
        String usuarioActual = tfUsuario.getText().trim();
        if (texto.isEmpty() || !conectado) return;

        try {
            if (cbPrivado.isSelected()) {
                String seleccionado = lvUsuarios.getSelectionModel().getSelectedItem();
                if (seleccionado == null || seleccionado.equals(usuarioActual)) {
                    mostrarAlerta("Selecciona un usuario válido de la lista lateral para enviar un mensaje privado.");
                    return;
                }
                // Enviar mensaje privado usando MensajeChat
                salida.writeObject(new MensajeChat(MensajeChat.Tipo.PRIVADO, usuarioActual, seleccionado, texto));
                salida.flush();
                taChat.appendText("[Tú -> Privado a " + seleccionado + "]: " + texto + "\n");
            } else {
                // Enviar mensaje público usando MensajeChat
                salida.writeObject(new MensajeChat(MensajeChat.Tipo.PUBLICO, usuarioActual, texto));
                salida.flush();
            }
            tfMensaje.clear();
        } catch (IOException e) {
            mostrarAlerta("Error al enviar el mensaje: " + e.getMessage());
        }
    }

    // Hilo secundario para recibir objetos del servidor
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

    private void desconectar() {
        conectado = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        desconectarUI();
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
            taChat.appendText("[SISTEMA] Desconectado del servidor.\n");
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