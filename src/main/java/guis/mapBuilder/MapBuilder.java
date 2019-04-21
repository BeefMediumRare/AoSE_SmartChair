package guis.mapBuilder;

import guis.mapBuilder.controller.MainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.Optional;

public class MapBuilder extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            Locale.setDefault(Locale.ENGLISH);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MainGUI.fxml"));
            Parent root = loader.load();
            MainController controller = loader.getController();

            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

            primaryStage.setOnCloseRequest(e -> {
                Optional.ofNullable(e).ifPresent(Event::consume);
                controller.onCloseRequest();
                Platform.exit();
            });
            primaryStage.setScene(scene);
            primaryStage.minWidthProperty().set(500);
            primaryStage.minHeightProperty().set(470);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //TODO prompt save before closing
    }


    public static void main(String[] args) {
        launch(args);
    }
}
