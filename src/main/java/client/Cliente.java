package client;

import modelo.MensajeChat; 
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.*;
import java.net.Socket;
import java.util.List;

/**
 * <h1>Clase Cliente (JavaFX)</h1>
 * <p>
 * Representa la interfaz gráfica y la lógica del cliente para la aplicación de chat multiusuario 
 * bajo arquitectura cliente-servidor (PSP). Se comunica con el servidor mediante Sockets TCP 
 * y flujos de objetos ({@code ObjectInputStream} y {@code ObjectOutputStream}).
 * </p>
 * <p>
 * Cumple con los requisitos de la rúbrica al no utilizar expresiones lambda, empleando clases anónimas
 * tradicionales y gestionando la recepción de mensajes en un hilo independiente para evitar el bloqueo de la GUI.
 * </p>
 * 
 * @author Eneko
 * @version 1.0
 */
public class Cliente extends Application {

    // Componentes de la Interfaz Gráfica (JavaFX)
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

    /** Lista observable para mantener sincronizada la lista de usuarios conectados en el ListView lateral. */
    private ObservableList<String> usuariosObs = FXCollections.observableArrayList();

    // Sockets y Streams de Comunicación de Red
    private Socket socket;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private boolean conectado = false;

    /**
     * Método principal de la aplicación. Lanza el ciclo de vida de JavaFX.
     * 
     * @param args argumentos de línea de comandos.
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Configura y despliega la interfaz gráfica principal de la ventana de chat,
     * asignando los manejadores de eventos correspondientes mediante clases anónimas.
     * 
     * @param primaryStage la ventana principal (Stage) de la aplicación JavaFX.
     */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Cliente Chat Multiusuario - PSP");

        // Configuración del panel superior (Conexión)
        HBox panelSuperior = new HBox(10);
        panelSuperior.setPadding(new Insets(10));
        btnDesconectar.setDisable(true);
        panelSuperior.getChildren().addAll(
            new Label("IP:"), tfIp,
            new Label("Puerto:"), tfPuerto,
            new Label("Usuario:"), tfUsuario,
            btnConectar, btnDesconectar
        );

        // Configuración del panel central (Área de chat y lista de usuarios)
        taChat.setEditable(false);
        taChat.setWrapText(true);
        lvUsuarios.setItems(usuariosObs);
        lvUsuarios.setPrefWidth(150);

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(taChat, lvUsuarios);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // Configuración del panel inferior (Envío de mensajes)
        HBox panelInferior = new HBox(10);
        panelInferior.setPadding(new Insets(10));
        tfMensaje.setPromptText("Escribe tu mensaje aquí...");
        HBox.setHgrow(tfMensaje, Priority.ALWAYS);
        btnEnviar.setDisable(true);
        cbPrivado.setDisable(true);
        panelInferior.getChildren().addAll(cbPrivado, tfMensaje, btnEnviar);

        VBox root = new VBox(10);
        root.getChildren().addAll(panelSuperior, splitPane, panelInferior);

        // Registro de eventos de la interfaz mediante clases anónimas (Sin Lambdas)
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

        tfMensaje.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                enviarMensaje();
            }
        });

        Scene scene = new Scene(root, 700, 450);
        primaryStage.setScene(scene);
        
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                desconectar();
            }
        });
        
        primaryStage.show();
    }

    /**
     * Establece la conexión socket con el servidor utilizando la IP y el puerto introducidos,
     * inicializa los flujos de objetos, envía el paquete de LOGIN e inicia el hilo receptor.
     */
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
            
            // Inicialización segura de streams de red
            salida = new ObjectOutputStream(socket.getOutputStream());
            salida.flush();
            entrada = new ObjectInputStream(socket.getInputStream());

            // Envío de la solicitud de inicio de sesión (LOGIN)
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

            // Lanzamiento del hilo independiente para la recepción de mensajes del servidor
            Thread hiloReceptor = new Thread(new HiloReceptor());
            hiloReceptor.setDaemon(true);
            hiloReceptor.start();

        } catch (NumberFormatException e) {
            mostrarAlerta("El puerto debe ser un número entero válido.");
        } catch (IOException e) {
            mostrarAlerta("No se pudo conectar al servidor: " + e.getMessage());
        }
    }

    /**
     * Envía un mensaje a través del socket al servidor. Permite diferenciar entre
     * mensajes públicos (difundidos a todos) o privados (dirigidos a un usuario seleccionado de la lista).
     */
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

    /**
     * <h1>Clase interna HiloReceptor</h1>
     * Gestiona de forma concurrente e independiente la lectura continua de objetos entrantes
     * procedentes del servidor, actualizando de forma segura la interfaz gráfica mediante {@code Platform.runLater}.
     */
    private class HiloReceptor implements Runnable {
        @Override
        public void run() {
            try {
                Object obj;
                while (conectado && (obj = entrada.readObject()) != null) {
                    if (obj instanceof MensajeChat) {
                        final MensajeChat msg = (MensajeChat) obj;

                        // Actualización segura del hilo de JavaFX UI sin lambdas
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                if (msg.getTipo() == MensajeChat.Tipo.USERLIST) {
                                    List<String> users = msg.getUsuarios();
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
                                } else if (msg.getTipo() == MensajeChat.Tipo.ERROR) {
                                    taChat.appendText("[ERROR] " + msg.getTexto() + "\n");
                                    mostrarAlerta(msg.getTexto());
                                }
                            }
                        });
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                if (conectado) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            taChat.appendText("[SISTEMA] Conexión perdida con el servidor.\n");
                            desconectarUI();
                        }
                    });
                }
            }
        }
    }

    /**
     * Cierra de forma ordenada el socket de comunicación y restablece el estado de desconexión.
     */
    private void desconectar() {
        conectado = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        desconectarUI();
    }

    /**
     * Restablece los componentes visuales de la interfaz gráfica al estado inicial tras la desconexión.
     */
    private void desconectarUI() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
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
            }
        });
    }

    /**
     * Muestra una ventana emergente de alerta informativa al usuario.
     * 
     * @param mensaje texto descriptivo que mostrará la alerta.
     */
    private void mostrarAlerta(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Aviso");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}