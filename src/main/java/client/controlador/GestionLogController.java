package controlador;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;

/**
 * Controlador de la interfaz gráfica de usuario en JavaFX para la visualización
 * de logs en tiempo real.
 * <p>
 * Implementa un sistema de actualización asíncrono mediante un hilo secundario
 * independiente
 * y la llamada a {@link Platform#runLater(Runnable)}, de modo que la lectura
 * periódica del
 * fichero {@code log/app.log} no bloquee ni congele la fluidez del hilo
 * principal gráfico (*FX Application Thread*).
 * </p>
 * 
 * @author Eneko.Plaza
 * @version 1.0
 */
public class GestionLogController implements Initializable {

    /**
     * Componente de interfaz de usuario de tipo lista para mostrar las líneas del
     * log.
     */
    @FXML
    private ListView<String> listaLogsView;

    /** Colección observable vinculada dinámicamente al {@link ListView}. */
    private ObservableList<String> lineasLog = FXCollections.observableArrayList();

    /**
     * Inicializa los componentes FXML de la vista y arranca la escucha en vivo del
     * archivo de logs.
     * 
     * @param url Ubicación relativa del recurso FXML.
     * @param rb  Paquete de recursos de localización o null.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        listaLogsView.setItems(lineasLog);
        iniciarLecturaEnTiempoReal();
    }

    /**
     * Inicia un hilo secundario ejecutor para vigilar y leer el fichero
     * {@code log/app.log}.
     * <p>
     * Utiliza clases anónimas {@link Runnable} en lugar de expresiones lambda para
     * mantener
     * la compatibilidad y legibilidad con estándares clásicos.
     * </p>
     */
    private void iniciarLecturaEnTiempoReal() {
        Thread hiloLectura = new Thread(new Runnable() {
            @Override
            public void run() {
                File archivo = new File("log/app.log");
                long ultimaPosicion = 0;

                while (true) {
                    try {
                        if (archivo.exists() && archivo.length() > ultimaPosicion) {
                            try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
                                reader.skip(ultimaPosicion);
                                String linea;
                                while ((linea = reader.readLine()) != null) {
                                    final String nuevaLinea = linea;
                                    if (!nuevaLinea.trim().isEmpty()) {
                                        // Delegación síncrona en el hilo gráfico de JavaFX
                                        Platform.runLater(new Runnable() {
                                            @Override
                                            public void run() {
                                                lineasLog.add(nuevaLinea);
                                            }
                                        });
                                    }
                                }
                                ultimaPosicion = archivo.length();
                            }
                        }
                        Thread.sleep(1000); // Frecuencia de muestreo de 1 segundo
                    } catch (IOException e) {
                        System.err.println("Error al leer el archivo de log: " + e.getMessage());
                        break;
                    } catch (InterruptedException e) {
                        System.err.println("Hilo de lectura de logs interrumpido.");
                        Thread.currentThread().interrupt(); // Buena práctica para cerrar el hilo limpiamente
                        break;
                    }
                }
            }
        });

        hiloLectura.setDaemon(true); // Permite la destrucción limpia del hilo al cerrar la aplicación
        hiloLectura.start();
    }
}