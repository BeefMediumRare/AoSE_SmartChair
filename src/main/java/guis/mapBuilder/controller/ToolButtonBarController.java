package guis.mapBuilder.controller;

import helper.SubController;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.text.Text;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.BiConsumer;

public class ToolButtonBarController extends SubController<MainController> {
    @FXML
    private ToggleGroup tools;
    @FXML
    private ToggleButton blockToggle_;
    @FXML
    private ToggleButton targetToggle_;
    @FXML
    private ToggleButton agentToggle_;
    @FXML
    private Text targetVisibilityIndicator_;
    @FXML
    private Text startPosVisibilityIndicator_;

    private final @Nonnull
    ObjectProperty<Tool> currentTool_ = new SimpleObjectProperty<>();

    @Nonnull
    Optional<Tool> getCurrentTool() {
        return Optional.ofNullable(currentTool_.get());
    }

    @Nonnull
    ObjectProperty<Tool> getObservableCurrentTool() {
        return currentTool_;
    }

    void setTargetVisibilityIndicator(boolean visibility){
        targetVisibilityIndicator_.setOpacity(visibility ? 1.0 : 0.33);
    }

    void setStartPosVisibilityIndicator(boolean visibility){
        startPosVisibilityIndicator_.setOpacity(visibility ? 1.0 : 0.33);
    }

    @Override
    public void forcedInitialize() {
        BiConsumer<Boolean, Tool> currentToolSetter = (selected, tool) -> currentTool_.set(selected ? tool : null);
        blockToggle_.selectedProperty().addListener(
                (observable, oldValue, newValue) -> currentToolSetter.accept(newValue, Tool.SET_OBSTACLE));
        targetToggle_.selectedProperty().addListener(
                (observable, oldValue, newValue) -> currentToolSetter.accept(newValue, Tool.AGENT_TARGET));
        agentToggle_.selectedProperty().addListener(
                (observable, oldValue, newValue) -> currentToolSetter.accept(newValue, Tool.AGENT_START));
    }
}