package guis.mapBuilder.controller;

import guis.JSONUtils;
import guis.mapBuilder.AgentParameter;
import guis.mapBuilder.SimpleCoDyAgent;
import helper.Point;
import helper.SubController;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.stage.FileChooser;
import org.everit.json.schema.ValidationException;
import org.json.JSONException;
import org.json.JSONObject;
import stores.JSONFileStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.HashMap;
import java.util.Optional;

import static guis.JSONUtils.*;
import static helper.Methods.*;

public class MenuController extends SubController<MainController> {
    @FXML
    private MenuItem saveMap_;
    @FXML
    private RadioMenuItem showStartPositions_;
    @FXML
    private RadioMenuItem showTargets_;

    private @Nonnull
    ObjectProperty<File> openedFile_ = new SimpleObjectProperty<>();
    private final @Nonnull
    FileChooser.ExtensionFilter extensionFilter_ = new FileChooser.ExtensionFilter("AoSE Project", "*.aose");

    @FXML
    void validate(@Nullable ActionEvent event) {
        Optional.ofNullable(event).ifPresent(Event::consume);
        mainController_.validateAgents();
    }

    @FXML
    void deploy(@Nullable ActionEvent event) {
        Optional.ofNullable(event).ifPresent(Event::consume);
        // TODO implement
    }

    @FXML
    void newProject(@Nullable ActionEvent event) {
        Optional.ofNullable(event).ifPresent(Event::consume);

        boolean okPressed = true;
        if (hasUnsavedChanges()) {
            okPressed = constructAlert(mainController_.getScene(), Alert.AlertType.WARNING, "Open Project", "Unsaved changes will be deleted!", ButtonType.OK, ButtonType.CANCEL)
                    .showAndWait().orElse(null) == ButtonType.OK;
        }
        if (okPressed) {
            mainController_.newProject();
        }
    }

    @FXML
    void open(@Nullable ActionEvent event) {
        Optional.ofNullable(event).ifPresent(Event::consume);

        boolean okPressed = true;
        if (hasUnsavedChanges()) {
            okPressed = constructAlert(mainController_.getScene(), Alert.AlertType.WARNING, "New Project", "Unsaved changes will be deleted!", ButtonType.OK, ButtonType.CANCEL)
                    .showAndWait().orElse(null) == ButtonType.OK;
        }
        if (okPressed) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(extensionFilter_);
            fileChooser.setInitialDirectory(new File("."));
            Optional.ofNullable(fileChooser.showOpenDialog(null)).ifPresent(file -> {
                mainController_.newProject();
                open(file);
            });
        }
    }

    private void open(@Nonnull File fileToOpen) {
        try {
            JSONObject jsonToParse = loadJSON(new FileInputStream(fileToOpen));
            validateJSON(jsonToParse, loadJSON(JSONFileStore.getAsStream(JSONFileStore.JSONFile.PROJECT_SCHEMA)));

            mainController_.open(
                    parseJSONArray(Point::new, jsonToParse.getJSONArray("staticObstacles")),
                    jsonToParse.getInt("dimensions"),
                    parseJSONArray(SimpleCoDyAgent::new, jsonToParse.getJSONArray("agents")),
                    new AgentParameter(jsonToParse.getJSONObject("agentParameter")));

            mainController_.setTitle(fileToOpen.getName());
            openedFile_.set(fileToOpen);
        } catch (ValidationException e1) {
            constructTextAreaAlert(mainController_.getScene(), "JSON Validation", e1.toJSON().toString(JSONFileStore.getIndentFactor()));
        } catch (FileNotFoundException | JSONException e2) {
            constructExceptionAlert(mainController_.getScene(), e2);
        }
    }

    @FXML
    void save(@Nullable ActionEvent event) {
        Optional.ofNullable(event).ifPresent(Event::consume);
        if (openedFile_.get() == null) {
            saveAs(null);
        } else {
            save(openedFile_.get());
        }
    }

    @FXML
    void saveAs(@Nullable ActionEvent event) {
        Optional.ofNullable(event).ifPresent(Event::consume);
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(extensionFilter_);
        fileChooser.setInitialDirectory(new File("."));
        Optional.ofNullable(fileChooser.showSaveDialog(null)).ifPresent(this::save);
    }

    private void save(@Nonnull File fileToWrite) {
        mainController_.showProgressInfoAsync("Saving", () -> {
            try (Writer writer = new FileWriter(fileToWrite)) {
                writer.write(serializeProject().toString(JSONFileStore.getIndentFactor()));
                mainController_.setTitle(fileToWrite.getName());
                openedFile_.set(fileToWrite);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, true);
    }

    private @Nonnull
    JSONObject serializeProject() {
        return new JSONObject(new HashMap<String, Object>() {{
            put("dimensions", mainController_.getDimensions());
            put("staticObstacles", JSONUtils.tokenize(Point::tokenize, mainController_.getStaticObstacles()));
            put("agentParameter", mainController_.getAgentParameter().tokenize());
            put("agents", JSONUtils.tokenize(SimpleCoDyAgent::tokenize, mainController_.getAgents()));
        }});
    }

    boolean hasUnsavedChanges() { // TODO does not work properly
        try {
            return openedFile_.get() == null || !loadJSON(new FileInputStream(openedFile_.get())).equals(serializeProject());
        } catch (FileNotFoundException e) {
            return true;
        }
    }

    @Nonnull
    ObservableValue<Boolean> getShowStartPositions() {
        return showStartPositions_.selectedProperty();
    }

    @Nonnull
    ObservableValue<Boolean> getShowTargets() {
        return showTargets_.selectedProperty();
    }

    @Override
    public void reset() {
        mainController_.setTitle(null);
        openedFile_.set(null);
    }

    @Override
    public void forcedInitialize() {
        saveMap_.disableProperty().bind(Bindings.createBooleanBinding(() -> openedFile_.get() == null, openedFile_));
    }

    @Nonnull
    Optional<String> getProjectName() {
        return Optional.ofNullable(openedFile_.get() != null ? openedFile_.get().getName() : null);
    }
}
