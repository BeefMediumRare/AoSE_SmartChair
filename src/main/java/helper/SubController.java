package helper;

import guis.mapBuilder.controller.MainController;
import javafx.application.Platform;
import javafx.scene.Node;

import javax.annotation.Nonnull;

/**
 * <b>Subclasses are not allowed to implement any constructor!</b>
 * <br>This would cause the JavaFX Thread to crash while doing magic reflection things to hook the JavaFX elements and Controllers up.
 */
public abstract class SubController<T> {
    protected T mainController_;

    public void injectMainController(@Nonnull T mainController) {
        mainController_ = mainController;
    }

    public void forcedInitialize(){}

    public void initializeWhenSceneIsPresent(@Nonnull Node node, @Nonnull Runnable initialize) {
        node.sceneProperty().addListener((observableValue, oldScene, newScene) -> {
            if(oldScene == null && newScene != null){
                Platform.runLater(initialize);
            }});
    }

    public void reset() {
    }
}
