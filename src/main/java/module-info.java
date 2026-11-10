module com.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    // Permite que JavaFX acceda a la clase App y al paquete client/controlador
    exports com.example to javafx.graphics;
    
    opens client to javafx.graphics, javafx.fxml;
    opens controlador to javafx.graphics, javafx.fxml;

    exports client;
    exports controlador;
}