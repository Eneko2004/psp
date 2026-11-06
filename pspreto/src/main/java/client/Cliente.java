package client;

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

public class Cliente extends Application {

    // Componentes JavaFX
    private TextField tfIp = new TextField("127.0.0.1");
    private TextField tfPuerto = new TextField("5000");
    private TextField tfUsuario = new TextField();
    private Button btnConectar = new Button("Conectar");
    private Button btnDesconectar = new Button("Desconectar");

    private TextArea taChat = new TextArea();
    private ListView<String> lvUsuarios = new ListView<>();
    private TextField tfMensaje = new TextField();
    private Button btnEnviar = new Button("Enviar");
    private CheckBox cbPrivado = new CheckBox("Mensaje Privado");

    private ObservableList<String> usuariosObs = FXCollections.observableArrayList();

    // Sockets y Hilos
    private Socket socket;
    private PrintWriter salida;
    private BufferedReader entrada;
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
            salida = new PrintWriter(socket.getOutputStream(), true);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Enviar el nombre como primer mensaje de autenticación
            salida.println(usuario);

            conectado = true;
            btnConectar.setDisable(true);
            tfIp.setDisable(true);
            tfPuerto.setDisable(true);
            tfUsuario.setDisable(true);
            btnDesconectar.setDisable(false);
            btnEnviar.setDisable(false);
            cbPrivado.setDisable(false);

            taChat.appendText("[SISTEMA] Conectado exitosamente al servidor como: " + usuario + "\n");

            // Hilo receptor independiente para que la interfaz nunca se bloquee
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
        if (texto.isEmpty() || !conectado) return;

        if (cbPrivado.isSelected()) {
            String seleccionado = lvUsuarios.getSelectionModel().getSelectedItem();
            if (seleccionado == null || seleccionado.equals(tfUsuario.getText().trim())) {
                mostrarAlerta("Selecciona un usuario válido de la lista lateral para enviar un mensaje privado.");
                return;
            }
            salida.println("PRIVATE:" + seleccionado + ":" + texto);
            taChat.appendText("[Tú -> Privado a " + seleccionado + "]: " + texto + "\n");
        } else {
            salida.println(texto);
        }
        tfMensaje.clear();
    }

    // Hilo secundario para recibir mensajes en segundo plano
    private class HiloReceptor implements Runnable {
        @Override
        public void run() {
            try {
                String linea;
                while (conectado && (linea = entrada.readLine()) != null) {
                    final String msg = linea;

                    // Actualización segura de JavaFX desde un hilo secundario
                    Platform.runLater(() -> {
                        if (msg.startsWith("USERLIST:")) {
                            String[] users = msg.substring(9).split(",");
                            usuariosObs.clear();
                            for (String u : users) {
                                usuariosObs.add(u);
                            }
                        } else {
                            taChat.appendText(msg + "\n");
                        }
                    });
                }
            } catch (IOException e) {
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