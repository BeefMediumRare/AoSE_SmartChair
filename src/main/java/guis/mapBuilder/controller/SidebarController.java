package guis.mapBuilder.controller;

import guis.mapBuilder.AgentParameter;
import guis.mapBuilder.SimpleCoDyAgent;
import helper.SubController;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableIntegerValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import org.everit.json.schema.ValidationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import stores.JSONFileStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static helper.Methods.*;

public class SidebarController extends SubController<MainController> {
    @FXML
    private TextField dimensionsInput_;
    @FXML
    private TextArea configInput_;

    @FXML
    private ListView<Integer> agentIDListView_;

    private final @Nonnull
    ObservableList<Integer> agentIDs_ = FXCollections.observableArrayList();

    @FXML
    void addNewAgent(@Nullable ActionEvent event) {
        Optional.ofNullable(event).ifPresent(Event::consume);
        addNewAgent();
    }

    @FXML
    void apply(@Nullable ActionEvent event) {
        Optional.ofNullable(event).ifPresent(Event::consume);

        try {
            JSONObject configInput = new JSONObject(new JSONTokener(configInput_.getText()));
            mainController_.setAgentParameter(new AgentParameter(configInput));
            //To trigger the listener
            configInput_.setText(configInput.toString(JSONFileStore.getIndentFactor() + 1));
            //Styling
            configInput_.setText(configInput.toString(JSONFileStore.getIndentFactor()));
        } catch (ValidationException e) {
            constructTextAreaAlert(mainController_.getScene(), "JSON Schema Exception", e.toJSON().toString(JSONFileStore.getIndentFactor()));
        } catch (JSONException e) {
            constructExceptionAlert(mainController_.getScene(), e);
        }
    }

    @FXML
    void setDefault(@Nullable ActionEvent event) {
        Optional.ofNullable(event).ifPresent(Event::consume);

        constructAlert(mainController_.getScene(), Alert.AlertType.CONFIRMATION, "Reset CoDyAgent Parameter", "Resetting to default will override the current parameters", ButtonType.OK, ButtonType.CANCEL)
                .showAndWait()
                .filter(buttonType -> buttonType == ButtonType.OK)
                .ifPresent(buttonType -> setDefault());
    }

    private void setDefault() {
        try {
            String fileContent = Files.lines(JSONFileStore.getAsPath(JSONFileStore.JSONFile.DEFAULT_AGENT_PARAMETER))
                    .collect(Collectors.joining("\n"));
            JSONObject defaultConfig = new JSONObject(new JSONTokener(fileContent));
            AgentParameter newParameter = new AgentParameter(defaultConfig);
            mainController_.setAgentParameter(newParameter);
            configInput_.setText(defaultConfig.toString(JSONFileStore.getIndentFactor()));
        } catch (ValidationException e) {
            constructTextAreaAlert(mainController_.getScene(), "JSON Schema Exception", e.toJSON().toString(JSONFileStore.getIndentFactor()));
        } catch (JSONException | URISyntaxException | IOException e) {
            constructExceptionAlert(mainController_.getScene(), e);
        }
    }

    private void addNewAgent() {
        Integer id = nextAgentID();
        agentIDs_.add(id);
        agentIDs_.sort(Integer::compareTo);
        agentIDListView_.getSelectionModel().select(id);
        mainController_.addAgent(id);
    }

    private Integer nextAgentID() {
        Integer result = 0;

        if (!agentIDs_.isEmpty()) {
            List<Integer> prefIDs = range(agentIDs_.size()).boxed().collect(Collectors.toList());

            AtomicInteger highestID = new AtomicInteger(-1);
            agentIDs_.forEach(agentID -> {
                highestID.set(Math.max(highestID.get(), agentID));
                if (agentID < agentIDs_.size()) {
                    prefIDs.remove(agentID);
                }
            });

            result = prefIDs.isEmpty() ? highestID.get() + 1 : prefIDs.stream().min(Integer::compareTo).get();
        }
        return result;
    }

    @FXML
    void deleteSelectedAgent(@Nullable ActionEvent event) {
        Optional.ofNullable(event).ifPresent(Event::consume);
        getSelectedAgentID().ifPresent(selectedAgentID -> {
            agentIDs_.remove(selectedAgentID);
            mainController_.removeAgent(selectedAgentID);
            if (agentIDs_.isEmpty()) {
                addNewAgent();
            }
        });
    }

    void focusAgent(@Nonnull Integer id) {
        agentIDListView_.getSelectionModel().select(id);
    }

    @Nonnull
    Optional<Integer> getSelectedAgentID() {
        return Optional.ofNullable(agentIDListView_.getSelectionModel().getSelectedItem());
    }

    @Override
    public void reset() {
        dimensionsInput_.setText("" + mainController_.getDimensions());

        clearAgentList();
        setDefault();
    }

    void open(@Nonnull List<SimpleCoDyAgent> agents, @Nonnull Integer dimensions, @Nonnull AgentParameter agentParameter) {
        agentIDs_.clear();
        agentIDs_.addAll(agents.stream().map(SimpleCoDyAgent::getID).collect(Collectors.toList()));
        setAgentParameter(agentParameter);
        dimensionsInput_.setText("" + dimensions);
    }

    private void setAgentParameter(@Nonnull AgentParameter agentParameter) {
        JSONObject agentParameterJSON = agentParameter.tokenize();

        //To trigger the listener
        configInput_.setText(agentParameterJSON.toString(JSONFileStore.getIndentFactor() + 1));
        //Styling
        configInput_.setText(agentParameterJSON.toString(JSONFileStore.getIndentFactor()));
    }

    // opening the alert will trigger the focused changed listener; that's the quickest solution I came up with
    private boolean changingDimension_ = false;

    private void dimensionsChanged() {
        Integer newDimensions = Math.max(1, new Integer(dimensionsInput_.getText()));

        if (!changingDimension_ && !newDimensions.equals(mainController_.getDimensions())) {
            changingDimension_ = true;

            constructAlert(mainController_.getScene(), Alert.AlertType.WARNING, "Set Dimensions",
                    "This will reset all cells!\nConstruct new agent.grid with dimensions: " + newDimensions + "?",
                    ButtonType.OK, ButtonType.CANCEL).showAndWait()
                    .filter(buttonType -> buttonType == ButtonType.OK)
                    .ifPresent(buttonType ->
                            mainController_.setDimensions(newDimensions));

            dimensionsInput_.setText("");//To trigger the listeners
            dimensionsInput_.setText("" + mainController_.getDimensions());
            dimensionsInput_.positionCaret(dimensionsInput_.getText().length());

            changingDimension_ = false;
        }
    }

    @Override
    public void forcedInitialize() {
        Platform.runLater(() -> {
            initializeWhenSceneIsPresent(dimensionsInput_, () ->
                    dimensionsInput_.getScene().setOnKeyPressed(e -> {
                        if (e.getCode().equals(KeyCode.NUMPAD2)) {
                            agentIDListView_.getSelectionModel().selectNext();
                        } else if (e.getCode().equals(KeyCode.NUMPAD8)) {
                            agentIDListView_.getSelectionModel().selectPrevious();
                        }
                    }));

            dimensionsInput_.focusedProperty().addListener(
                    (observable, oldValue, newValue) -> {
                        if (!newValue) {
                            dimensionsChanged();
                        }
                    });
            dimensionsInput_.setOnKeyPressed(e -> {
                e.consume();
                if (e.getCode().equals(KeyCode.ENTER)) {
                    dimensionsChanged();
                }
            });
            dimensionsInput_.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d*") ? c : null));
            setColorBehaviourForDimensionField();

            setColorBehaviourForConfigInput();

            enforceResizing(agentIDListView_, 100, 220);
            enforceResizing(configInput_, 100, 352);

            agentIDListView_.setItems(agentIDs_);
            agentIDListView_.setCellFactory(param -> new ListCell<Integer>() {
                @Override
                protected void updateItem(Integer agentID, boolean empty) {
                    super.updateItem(agentID, empty);
                    if (agentID != null) {
                        setGraphic(new Label("CoDyAgent " + agentID));
                    } else {
                        setGraphic(null);
                    }
                }
            });
            agentIDListView_.getSelectionModel().selectedItemProperty().addListener(
                    (observable, oldValue, newValue) -> Optional.ofNullable(newValue).ifPresent(mainController_::focusAgent));
        });
    }

    private void enforceResizing(@Nonnull Control node, int minSize, int bottomInset) {
        Scene scene = mainController_.getScene();

        ObservableIntegerValue height = Bindings.createIntegerBinding(
                () -> Math.max(minSize, (int) scene.getHeight() - bottomInset), scene.heightProperty());

        node.minHeightProperty().bind(height);
        node.maxHeightProperty().bind(height);
    }

    private void setColorBehaviourForDimensionField() {
        Function<String, Integer> getValue = text ->
                new Integer(text != null && !text.isEmpty() ? text : "0");

        dimensionsInput_.textProperty().addListener((observable, oldValue, newValue) ->
                dimensionsInput_.setStyle("-fx-text-fill:" +
                        (mainController_.getDimensions().equals(getValue.apply(newValue)) ? "white" : "-accent" + ";")));
    }

    private void setColorBehaviourForConfigInput() {
        configInput_.textProperty().addListener((observable, oldValue, newValue) -> {
            String newColor;
            try {
                JSONObject configInput = new JSONObject(new JSONTokener(newValue));
                AgentParameter agentParameter = new AgentParameter(configInput);
                AgentParameter master = mainController_.getAgentParameter();
                newColor = agentParameter.equals(master) ? "white" : "-accent";
            } catch (Exception e) {
                newColor = "-red";
            }
            configInput_.setStyle("-fx-text-fill:" + newColor + ";");
        });
    }

    void clearAgentList() {
        mainController_.removeAgents(agentIDs_);
        agentIDs_.clear();
        addNewAgent();
    }
}
