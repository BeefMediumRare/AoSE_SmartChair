package guis.simulation.controller;

import guis.customControl.ProgressSlider;
import guis.mapBuilder.AgentParameter;
import helper.SubController;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import stores.JSONFileStore;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class SidebarController extends SubController<MainController> {
    private enum SimulationReplayStatus {
        PLAYING, PAUSED, STOPPED
    }

    @FXML
    private TextField dimensions_;
    @FXML
    private TextArea agentParameter_;
    @FXML
    private TextArea dynamicData_;
    @FXML
    private Label speedLabel_;
    @FXML
    private Button playButton;
    @FXML
    private Button stopButton;
    @FXML
    private Button stepForwardButton;
    @FXML
    private Button pauseButton;

    private @Nonnull
    ProgressSlider speedSlider_;
    private @Nonnull
    ObjectProperty<SimulationReplayStatus> simulationReplayStatus_ = new SimpleObjectProperty<>();

    @Override
    public void reset() {
        simulationReplayStatus_.set(SimulationReplayStatus.STOPPED);
        speedSlider_.setValue(1);
    }

    @Override
    public void forcedInitialize() {
        playButton.setOnMousePressed(e -> play());
        stopButton.setOnMousePressed(e -> stop());
        stepForwardButton.setOnMousePressed(e -> stepForwards());
        pauseButton.setOnMousePressed(e -> pause());

        simulationReplayStatus_.addListener((observable, oldValue, newValue) -> setButtonEnableStatus(newValue));

        // "hacky" adding, because ProgressSlider is not a proper FXClass yet.
        speedSlider_ = new ProgressSlider(0.5, 10, 1, 0.1);
        speedSlider_.setMinWidth(120);
        speedSlider_.setMaxWidth(120);
        ((HBox) speedLabel_.getParent()).getChildren().add(2, speedSlider_);

        speedLabel_.textProperty().bind(Bindings.createStringBinding(
                () -> String.format("%.1f", speedSlider_.getValue()).replace(",", "."),
                speedSlider_.valueProperty()));
    }

    @Nonnull
    DoubleProperty getSpeedMultiplicatorProperty() {
        return speedSlider_.valueProperty();
    }

    void focusAgent(@Nonnull Integer agentID) {
        dynamicData_.setText("CoDyAgent: " + agentID);
    }

    void open(@Nonnull Integer dimensions, @Nonnull AgentParameter agentParameter) {
        dimensions_.getParent().getParent().setDisable(false);
        dimensions_.setText(dimensions.toString());
        agentParameter_.setText(agentParameter.tokenize().toString(JSONFileStore.getIndentFactor()));
    }

    private void setButtonEnableStatus(@Nonnull SimulationReplayStatus status) {
        Stream.of(playButton, stepForwardButton, pauseButton, stopButton, speedSlider_)
                .forEach(node -> node.setDisable(false));

        Stream<Node> nodeToDisable;
        if (status == SimulationReplayStatus.STOPPED) {
            nodeToDisable = Stream.of(pauseButton, stopButton);
        } else if (status == SimulationReplayStatus.PLAYING) {
            nodeToDisable = Stream.of(playButton, stepForwardButton, speedSlider_);
        } else {// if (status == SimulationReplayStatus.PAUSED) {
            nodeToDisable = Stream.of(pauseButton);
        }
        nodeToDisable.forEach(node -> node.setDisable(true));
    }

    private void play() {
        simulationReplayStatus_.set(SimulationReplayStatus.PLAYING);
        mainController_.playSimulation(() -> simulationReplayStatus_.set(SimulationReplayStatus.PAUSED));
    }

    private void pause() {
        simulationReplayStatus_.set(SimulationReplayStatus.PAUSED);
        mainController_.pauseSimulation();
    }

    private void stepForwards() {
        simulationReplayStatus_.set(SimulationReplayStatus.PAUSED);
        mainController_.stepSimulationForward();
    }

    private void stop() {
        simulationReplayStatus_.set(SimulationReplayStatus.STOPPED);
        mainController_.stopSimulation();
    }
}
