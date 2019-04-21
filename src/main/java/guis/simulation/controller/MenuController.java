package guis.simulation.controller;

import guis.mapBuilder.AgentParameter;
import guis.mapBuilder.SimpleCoDyAgent;
import helper.Point;
import helper.SubController;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
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
    private @Nonnull
    MenuItem saveMap_; //TODO disable if openfile = null

    private @Nonnull
    ObjectProperty<File> openedFile_ = new SimpleObjectProperty<>();
    private @Nonnull
    ObjectProperty<JSONObject> importedProject_ = new SimpleObjectProperty<>();
    private final @Nonnull
    FileChooser.ExtensionFilter aoseExtensionFilter_ = new FileChooser.ExtensionFilter("AoSE Project", "*.aose");
    private final @Nonnull
    FileChooser.ExtensionFilter simExtensionFilter_ = new FileChooser.ExtensionFilter("Simulation Result", "*.simres");

    @Override
    public void forcedInitialize() {
        saveMap_.disableProperty().bind(Bindings.createBooleanBinding(() -> openedFile_.get() == null, openedFile_));
    }

    @FXML
    void restartSimulation(@Nullable ActionEvent event) {
        Optional.ofNullable(event).ifPresent(Event::consume);
        importAoSE(importedProject_.get());
    }

    @FXML
    void importAoSE(@Nullable ActionEvent event) {
        Optional.ofNullable(event).ifPresent(Event::consume);
        boolean okPressed = constructAlert(mainController_.getScene(), Alert.AlertType.WARNING, "Importing AoSE Project", "Unsaved changes will be deleted!", ButtonType.OK, ButtonType.CANCEL)
                .showAndWait().orElse(null) == ButtonType.OK;
        if (okPressed) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(aoseExtensionFilter_);
            fileChooser.setInitialDirectory(new File("."));
            //TODO maincontroller_.newProject();
            Optional.ofNullable(fileChooser.showOpenDialog(null)).ifPresent(this::importAoSE);
        }
        openedFile_.set(null);
    }

    @FXML
    void open(@Nullable ActionEvent event) {
        Optional.ofNullable(event).ifPresent(Event::consume);
        boolean okPressed = constructAlert(mainController_.getScene(), Alert.AlertType.WARNING, "New Project", "Unsaved changes will be deleted!", ButtonType.OK, ButtonType.CANCEL)
                .showAndWait().orElse(null) == ButtonType.OK;
        if (okPressed) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(simExtensionFilter_);
            fileChooser.setInitialDirectory(new File("."));
            //TODO maincontroller_.newProject();
            Optional.ofNullable(fileChooser.showOpenDialog(null)).ifPresent(this::open);
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
        fileChooser.getExtensionFilters().add(simExtensionFilter_);
        fileChooser.setInitialDirectory(new File("."));
        Optional.ofNullable(fileChooser.showSaveDialog(null)).ifPresent(this::save);
    }

    /* TODO private*/ void importAoSE(@Nonnull File file) {
        try {
            importAoSE(loadJSON(new FileInputStream(file)));
        } catch (FileNotFoundException e) {
            constructExceptionAlert(mainController_.getScene(), e);
        }
    }

    private void importAoSE(@Nonnull JSONObject jsonToParse) {
        try {
            validateJSON(jsonToParse, JSONFileStore.JSONFile.PROJECT_SCHEMA);

            mainController_.importAoSE(
                    parseJSONArray(Point::new, jsonToParse.getJSONArray("staticObstacles")),
                    jsonToParse.getInt("dimensions"),
                    parseJSONArray(SimpleCoDyAgent::new, jsonToParse.getJSONArray("agents")),
                    new AgentParameter(jsonToParse.getJSONObject("agentParameter")));

            importedProject_.setValue(jsonToParse);
        } catch (ValidationException e1) {
            constructTextAreaAlert(mainController_.getScene(), "JSON Validation", e1.toJSON().toString(JSONFileStore.getIndentFactor()));
        } catch (JSONException e2) {
            constructExceptionAlert(mainController_.getScene(), e2);
        }
    }

    private void open(@Nonnull File file) {
        try {
            JSONObject jsonToParse = loadJSON(new FileInputStream(file));
            validateJSON(jsonToParse, loadJSON(JSONFileStore.getAsStream(JSONFileStore.JSONFile.SIMULATION_SCHEMA)));

//            mainController_.openSimulationFile(); // TODO implement

            openedFile_.set(file);
        } catch (ValidationException e1) {
            constructTextAreaAlert(mainController_.getScene(), "JSON Validation", e1.toJSON().toString(JSONFileStore.getIndentFactor()));
        } catch (FileNotFoundException | JSONException e2) {
            constructExceptionAlert(mainController_.getScene(), e2);
        }

    }

    private void save(@Nonnull File file) {
        try (Writer writer = new FileWriter(file)) {
            writer.write(serializeProject().toString(JSONFileStore.getIndentFactor()));
            openedFile_.set(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private @Nonnull
    JSONObject serializeProject() {
        return new JSONObject(new HashMap<String, Object>() {{
            put("aoseProject", importedProject_.get());
            // TODO add the missing stuff
        }});
    }
}
