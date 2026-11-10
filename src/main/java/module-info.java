module com.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.base;

    // Permite que JavaFX acceda a la app, controladores y vistas
    opens com.example to javafx.graphics, javafx.fxml;
    opens client to javafx.graphics, javafx.fxml;
    opens client.controlador to javafx.graphics, javafx.fxml;

    // Exporta los paquetes para que sean accesibles entre ellos y por JavaFX
    exports com.example;
    exports client;
    exports client.controlador;
    exports modelo;
    exports server;
}