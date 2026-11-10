package com.example; // Ajusta esto según el paquete raíz que estés usando (ej. retopsp o com.example)

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * <h1>Clase Principal de la Interfaz (App)</h1>
 * <p>Inicializa la aplicación JavaFX y carga la ventana inicial del cliente de chat.</p>
 */
public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        // Asegúrate de que la ruta comience con "/" para buscar desde la raíz de resources,
        // o ajusta la ruta relativa según dónde guardes el archivo .fxml
        scene = new Scene(loadFXML("/com/example/vista/VentanaReto"), 640, 480);
        stage.setTitle("Chat Multiusuario - PSP");
        stage.setScene(scene);
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}