package retopsp.cliente;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class ClienteChat extends Application {
    private PrimaryController controller;
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/retopsp/Ventana.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        
        primaryStage.setTitle("Chat Multiusuario PSP");
        primaryStage.setScene(new Scene(root, 750, 450));
        
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                controller.desconectar();
            }
        });
        
        
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
