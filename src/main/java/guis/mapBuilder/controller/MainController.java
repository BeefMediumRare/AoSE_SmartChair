package guis.mapBuilder.controller;

import guis.JSONUtils;
import guis.mapBuilder.AgentParameter;
import guis.mapBuilder.AutoExecutableQueue;
import guis.mapBuilder.MapBuilder;
import guis.mapBuilder.SimpleCoDyAgent;
import helper.Point;
import helper.SubController;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static helper.Methods.constructAlert;
import static helper.Methods.or;

public class
MainController {
    @FXML
    private Label versionLabel_;
    @FXML
    private Label progressLabel_;

    // Controllers have to be referenced here for JavaFX to load them
    @FXML
    private GridController gridController;
    @FXML
    private MenuController menuController;
    @FXML
    private SidebarController sidebarController;
    @FXML
    private ToolButtonBarController toolButtonBarController;

    private final @Nonnull
    AutoExecutableQueue autoExecutableQueue_ = new AutoExecutableQueue();

    private final @Nonnull
    List<SimpleCoDyAgent> agents_ = new ArrayList<>();
    private final @Nonnull
    List<Point> staticObstacles_ = new ArrayList<>();
    private final @Nonnull
    ObjectProperty<Integer> dimensions_ = new SimpleObjectProperty<>();
    private @Nonnull
    ObjectProperty<AgentParameter> agentParameter_ = new SimpleObjectProperty<>();

    //Field because else the GarbageCollector would clean these up.
    private final @Nonnull
    BooleanProperty agentStartPosVisible_ = new SimpleBooleanProperty();
    private final @Nonnull
    BooleanProperty agentTargetsVisible_ = new SimpleBooleanProperty();

    @FXML
    private void initialize() {
        getSubControllers().forEach(injectable -> injectable.injectMainController(this));

        versionLabel_.setText("1.0.0");
        dimensions_.addListener((observable, oldValue, newValue) -> sidebarController.clearAgentList());

        versionLabel_.sceneProperty().addListener((observableValue, oldScene, newScene) -> {
            if (oldScene == null && newScene != null) {
                Platform.runLater(() -> {
                    getSubControllers().forEach(SubController::forcedInitialize);

                    agentStartPosVisible_.bind(Bindings.createBooleanBinding(
                            () -> menuController.getShowStartPositions().getValue() ||
                                    or(tool -> toolButtonBarController.getCurrentTool().orElse(null) == tool, Tool.AGENT_START, Tool.SET_OBSTACLE),
                            menuController.getShowStartPositions(), toolButtonBarController.getObservableCurrentTool()));
                    agentStartPosVisible_.addListener((observable, oldValue, newValue) -> {
                        gridController.setAgentStartPosVisibility(newValue);
                        toolButtonBarController.setStartPosVisibilityIndicator(newValue);
                    });

                    agentTargetsVisible_.bind(Bindings.createBooleanBinding(
                            () -> menuController.getShowTargets().getValue() ||
                                    or(tool -> toolButtonBarController.getCurrentTool().orElse(null) == tool, Tool.AGENT_TARGET, Tool.SET_OBSTACLE),
                            menuController.getShowTargets(), toolButtonBarController.getObservableCurrentTool()));
                    agentTargetsVisible_.addListener((observable, oldValue, newValue) -> {
                        gridController.setAgentTargetsVisibility(newValue);
                        toolButtonBarController.setTargetVisibilityIndicator(newValue);
                    });

                    newProject();
                });
            }
        });
    }

    private @Nonnull
    Stream<SubController<MainController>> getSubControllers() {
        return Stream.of(gridController, menuController, sidebarController, toolButtonBarController);
    }

    void open(@Nonnull List<Point> staticObstacles, @Nonnull Integer dimensions, @Nonnull List<SimpleCoDyAgent> agents, @Nonnull AgentParameter agentParameter) {
        newProject(); //TODO check if needed

        dimensions_.set(dimensions);
        staticObstacles_.clear();
        staticObstacles_.addAll(staticObstacles);
        agentParameter_.set(new AgentParameter(agentParameter));

        agents_.clear();
        agents_.addAll(agents);

        sidebarController.open(agents, dimensions_.get(), agentParameter);
        gridController.open(agents, staticObstacles_, dimensions_.get());

        focusAgent(agents.get(0).getID());
    }

    @Nonnull
    Optional<Integer> getSelectedAgentID() {
        return sidebarController.getSelectedAgentID();
    }

    @Nonnull
    Integer getDimensions() {
        return dimensions_.get();
    }

    @Nonnull
    Optional<Tool> getCurrentTool() {
        return toolButtonBarController.getCurrentTool();
    }

    @Nonnull
    Node getGrid() {
        return gridController.getGrid();
    }

    void addStaticObstacle(@Nonnull Point staticObstacle) {
        staticObstacles_.add(staticObstacle);
    }

    void removeStaticObstacle(@Nonnull Point staticObstacle) {
        staticObstacles_.remove(staticObstacle);
    }

    void newProject() {
        Integer defaultDimensions = 32;
        dimensions_.set(defaultDimensions + 1); // To trigger the listeners
        dimensions_.set(defaultDimensions);

        getSubControllers().forEach(SubController::reset);
    }

    /**
     * Sets the dimensions of the agent.grid. <b>This will reset the grid and remove all agents!</b>
     *
     * @param dimensions the new dimensions. <b><u><i>Must be a positive</i></u></b> {@link Integer}.
     */
    void setDimensions(@Nonnull Integer dimensions) {
        if (dimensions < 1) {
            throw new IllegalArgumentException("Dimensions must be positive a Integer but is " + dimensions);
        }
        dimensions_.set(dimensions);
        gridController.drawGrid(dimensions);
    }

    void setAgentStartPos(@Nonnull Integer id, @Nullable Point pos) {
        getAgent(id).ifPresent(agent -> agent.setStartPos(pos));
    }

    void setAgentTarget(@Nonnull Integer id, @Nullable Point pos) {
        getAgent(id).ifPresent(agent -> agent.setTarget(pos));
    }

    void setAgentParameter(@Nonnull AgentParameter agentParameter) {
        agentParameter_.set(agentParameter);
    }

    @Nonnull
    AgentParameter getAgentParameter() {
        return agentParameter_.get();
    }

    @Nonnull
    List<SimpleCoDyAgent> getAgents() {
        return agents_;
    }

    @Nonnull
    List<Point> getStaticObstacles() {
        return staticObstacles_;
    }

    void removeAgents(List<Integer> agentIDs) {
        agents_.removeAll(agentIDs.stream().map(id -> getAgent(id).orElse(null)).filter(Objects::nonNull).collect(Collectors.toList()));
    }

    @Nonnull
    Scene getScene() {
        return versionLabel_.getScene();
    }

    private @Nonnull
    Optional<SimpleCoDyAgent> getAgent(@Nonnull Integer id) {
        return agents_.stream().filter(agent -> agent.getID() == id).findFirst();
    }

    void addAgent(@Nonnull Integer id) {
        if (agents_.stream().map(SimpleCoDyAgent::getID).anyMatch(agentID -> agentID.equals(id))) {
            throw new IllegalArgumentException("An AgentID must be unique. " + id + " already exists.");
        }
        agents_.add(new SimpleCoDyAgent(id));
    }

    void removeAgent(@Nonnull Integer id) {
        getAgent(id).ifPresent(agents_::remove);
        gridController.removeAgent(id);
    }

    void focusAgent(@Nonnull Integer id) {
        gridController.focusAgent(id);
        sidebarController.focusAgent(id);
    }

    void setTitle(@Nullable String title) {
//        ((Stage) getScene().getWindow()).setTitle("AoSE" + (title == null || title.isEmpty() ? "" : " - " + title));
    }

    @Nonnull
    String getProjectName() {
        return menuController.getProjectName().orElse("Untitled Project");
    }

    void validateAgents() {
        Runnable validatingDone = showProgressInfoAsync("Validating Project", true);
        List<Integer> idsOfInvalidAgents = agents_.stream()
                .filter(agent -> !agent.getStartPos().isPresent() || !agent.getTarget().isPresent())
                .map(SimpleCoDyAgent::getID)
                .collect(Collectors.toList());
        validatingDone.run();

        constructAlert(getScene(), Alert.AlertType.INFORMATION, "CoDyAgent Validation",
                idsOfInvalidAgents.isEmpty()
                        ? "Project is valid and can be deployed/simulated!"
                        : "All agents have to have a start position and target set. The following agents are invalid\n"
                        + idsOfInvalidAgents).showAndWait();
    }

    /**
     * @see JSONUtils#showProgressInfoAsync(AutoExecutableQueue, Label, String, boolean)
     */
    void showProgressInfoAsync(@Nonnull String message, @Nonnull Runnable runnable, boolean modal) {
        JSONUtils.showProgressInfoAsync(autoExecutableQueue_, progressLabel_, message, runnable, modal);
    }

    /**
     * @see JSONUtils#showProgressInfoAsync(AutoExecutableQueue, Label, String, boolean)
     */
    @Nonnull
    Runnable showProgressInfoAsync(@Nonnull String message, boolean modal) {
        return JSONUtils.showProgressInfoAsync(autoExecutableQueue_, progressLabel_, message, modal);
    }

    public void onCloseRequest() {
        if (menuController.hasUnsavedChanges()) {
            if (constructAlert(getScene(), Alert.AlertType.WARNING, "Exit",
                    "Unsaved changes will be deleted!\nWould you like to save before exiting?", ButtonType.YES, ButtonType.NO)
                    .showAndWait().orElse(null) == ButtonType.YES) {
                menuController.save(null);
            }
        }
    }
}