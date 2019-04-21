package guis.simulation;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Locale;

public class Simulation extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            Locale.setDefault(Locale.ENGLISH);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MainGUI.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

            primaryStage.setScene(scene);
            primaryStage.minWidthProperty().set(500);
            primaryStage.minHeightProperty().set(470);
            primaryStage.show();
        } catch (
                Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
