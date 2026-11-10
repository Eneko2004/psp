module retopsp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.base;

    opens retopsp.cliente to javafx.fxml;
    exports retopsp.cliente;
}
