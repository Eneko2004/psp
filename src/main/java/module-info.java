module com.example {
    requires javafx.controls;
    requires javafx.fxml;

    // Permite que JavaFX acceda a las clases del paquete client por reflexión
    opens client to javafx.graphics;
    
    // Si también usas FXML en ese paquete u otros, asegúrate de abrirlos o exportarlos según necesites
    exports client;
}