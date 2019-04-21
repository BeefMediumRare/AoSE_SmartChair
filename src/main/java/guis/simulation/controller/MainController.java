package guis.simulation.controller;

import codyAgent.CoDyAgent;
import codyAgent.CoDyAgentHAL;
import codyAgent.CoDyAgentSetupParameter;
import codyAgent.Logger;
import guis.mapBuilder.AgentParameter;
import guis.mapBuilder.SimpleCoDyAgent;
import helper.Direction;
import helper.Point;
import helper.SubController;
import helper.Wrapper;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.util.Duration;

import javax.annotation.Nonnull;
import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static helper.Methods.*;


public class
MainController {
    @FXML
    private Label versionLabel_;
    @FXML
    private Label timeProgressLabel_;
    @FXML
    private Slider timeStepProgressSlider_;
    @FXML
    private Slider timeProgressSlider_;

    // Controllers have to be referenced here for JavaFX to load them
    @FXML
    private GridController gridController;
    @FXML
    private MenuController menuController;
    @FXML
    private SidebarController sidebarController;

    private final @Nonnull
    DoubleProperty simulationDuration_ = new SimpleDoubleProperty();
    private final @Nonnull
    IntegerProperty simulationTimePointer_ = new SimpleIntegerProperty();

    private final @Nonnull
    ObjectProperty<Integer> dimensions_ = new SimpleObjectProperty<>();
    private final @Nonnull
    List<HALAction> halActions_ = Collections.synchronizedList(new ArrayList<>());
    private @Nonnull
    Instant globalStartTime_ = Instant.now();
    private @Nonnull
    DoubleProperty speedFactorProperty_ = new SimpleDoubleProperty();
    private @Nonnull
    Wrapper<Boolean> simulationIsPlaying_ = new Wrapper<>(false);

    private @Nonnull
    Stream<SubController<MainController>> getSubControllers() {
        return Stream.of(gridController, menuController, sidebarController);
    }

    @Nonnull
    Scene getScene() {
        return versionLabel_.getScene();
    }

    void openSimulationFile(@Nonnull List<HALAction> halActions, @Nonnull List<Point> staticObstacles, int dimensions,
                            @Nonnull AgentParameter agentParameter, @Nonnull Map<SimpleCoDyAgent, SimulationCoDyHAL> hals) {
        reset();
        open(staticObstacles, dimensions, agentParameter, hals);
        initializeSimulationPlayback(halActions);
    }

    void importAoSE(@Nonnull List<Point> staticObstacles, int dimensions, @Nonnull List<SimpleCoDyAgent> simpleCoDyAgents, @Nonnull AgentParameter agentParameter) {
        List<Integer> idsOfInvalidAgents = simpleCoDyAgents.stream()
                .filter(agent -> !agent.getStartPos().isPresent() || !agent.getTarget().isPresent())
                .map(SimpleCoDyAgent::getID)
                .collect(Collectors.toList());
        if (!idsOfInvalidAgents.isEmpty()) {
            constructAlert(getScene(), Alert.AlertType.INFORMATION, "CoDyAgent Validation",
                    "All agents have to have a start position and target set. The following agents are invalid\n"
                            + idsOfInvalidAgents).showAndWait();
        } else {
            reset();

            // The multiplicator for the "simulation time"
            sidebarController.getSpeedMultiplicatorProperty().set(2);
            runInNewThread(() -> {
                try {
                    simulate(hals -> Platform.runLater(() -> open(staticObstacles, dimensions, agentParameter, hals)),
                            hals -> Platform.runLater(() -> {
                                // edit the halActions so that the time represent the "real-time"
                                long initTime = halActions_.get(0).time_;
                                halActions_.forEach(action -> action.time_ = (long) ((action.time_ - initTime) * speedFactorProperty_.get()) + initTime);
                                halActions_.add(0, new HALAction(0, () -> hals.values().forEach(SimulationCoDyHAL::reset)));
                                halActions_.get(0).action_.run();

                                initializeSimulationPlayback(new ArrayList<>(halActions_));
                                Logger.exportAsCSV(new File("logs/" + new SimpleDateFormat("yy-MM-dd_HH-mm-ss").format(new Date()) + ".cody-sim-log.csv"));
//                                Logger.exportPriosAsCSV(new File("experiments/logs/"+ new SimpleDateFormat("yy-MM-dd_HH-mm-ss").format(new Date()) + ".cody-sim-prio-log.csv"));

                            }), agentParameter, simpleCoDyAgents, staticObstacles, dimensions);
                } catch (StaleProxyException e) {
                    e.printStackTrace(); // should not occur
                }
            });
        }
    }

    private void initializeSimulationPlayback(@Nonnull List<HALAction> halActions) {
        halActions_.clear();
        halActions_.addAll(halActions);
        simulationDuration_.set(getLast(halActions_).get().time_);
        simulationTimePointer_.set(0);
        timeProgressSlider_.setValue(0.01); // to trigger the listener
        timeProgressSlider_.setValue(0.0);
        animateTimeProgress(Duration.millis(1));

        sidebarController.getSpeedMultiplicatorProperty().set(1);
    }

    private void open(@Nonnull List<Point> staticObstacles, int dimensions, @Nonnull AgentParameter agentParameter, @Nonnull Map<SimpleCoDyAgent, SimulationCoDyHAL> hals) {
        dimensions_.set(dimensions);
        sidebarController.open(dimensions_.get(), agentParameter);
        gridController.open(staticObstacles, dimensions_.get());
        hals.forEach((agent, hal) ->
                gridController.addAgent(agent.getID(), agent.getTarget().get(),
                        hal.getCurrentPositionProperty(), hal.transitionTimeProperty_));
    }

    private void simulate(@Nonnull Consumer<Map<SimpleCoDyAgent, SimulationCoDyHAL>> agentsCreated, @Nonnull Consumer<Map<SimpleCoDyAgent, SimulationCoDyHAL>> simulationDone,
                          @Nonnull AgentParameter agentParameter, @Nonnull List<SimpleCoDyAgent> simpleCoDyAgents,
                          @Nonnull List<Point> staticObstacles, int dimensions) throws StaleProxyException {
        AgentContainer agentContainer = createJADEEnvironment();

        Map<SimpleCoDyAgent, SimulationCoDyHAL> hals = new HashMap<>();
        List<CoDyAgent> agents = new ArrayList<>();

        List<SimpleCoDyAgent> shuffledAgents = new ArrayList<>(simpleCoDyAgents);
        Collections.shuffle(shuffledAgents);

        List<AgentController> agentController = new ArrayList<>();
        for (SimpleCoDyAgent simpleCoDyAgent : shuffledAgents) {
            // Modify the speed to so the agent calculates the right period for the faster sim time
            AgentParameter modifiedAgentParameter = new AgentParameter(agentParameter);
            modifiedAgentParameter.setRobotSpeed(agentParameter.getRobotSpeed() * speedFactorProperty_.doubleValue());

            SimulationCoDyHAL simulationHAL = new SimulationCoDyHAL(simpleCoDyAgent.getStartPos().get(), agentParameter);
            hals.put(simpleCoDyAgent, simulationHAL);

            agentController.add(agentContainer.createNewAgent(
                    "" + simpleCoDyAgent.getID(),
                    CoDyAgent.class.getCanonicalName(),
                    new Object[]{new CoDyAgentSetupParameter(
                            simpleCoDyAgent.getID(),
                            simulationHAL,
                            agents::add,
                            simpleCoDyAgent.getStartPos().get(),
                            simpleCoDyAgent.getTarget().get(),
                            dimensions,
                            staticObstacles,
                            modifiedAgentParameter)}));
        }

        for (AgentController controller : agentController) {
            controller.start();
        }

        // wait for all agents to be created
        while (agents.size() != simpleCoDyAgents.size()) {
            sleep(200);
        }
        agentsCreated.accept(hals);

        //wait for "simulation" to end
        while (agents.stream().anyMatch(agent -> !agent.isDone())) {
            sleep(200);
        }
        simulationDone.accept(hals);

        agentContainer.kill();
    }

    void focusAgent(@Nonnull Integer agentID) {
        gridController.focusAgent(agentID);
        sidebarController.focusAgent(agentID);
    }

    @FXML
    private void initialize() {
        getSubControllers().forEach(injectable -> injectable.injectMainController(this));

        versionLabel_.setText("1.0.0");

        simulationTimePointer_.addListener((observable, oldValue, newValue) ->
                timeStepProgressSlider_.setValue(halActions_.get(newValue.intValue()).time_));
        simulationDuration_.addListener((observable, oldValue, newValue) -> {
            timeStepProgressSlider_.setMax(newValue.doubleValue());
            timeProgressSlider_.setMax(newValue.doubleValue());
        });

        timeProgressSlider_.valueProperty().addListener((observable, oldValue, newValue) ->
                timeProgressLabel_.setText(String.format("%.2fs / %.2fs / %.2fs", newValue.doubleValue() / 1000.0, timeStepProgressSlider_.getValue() / 1000.0, timeStepProgressSlider_.getMax() / 1000.0)));

        versionLabel_.sceneProperty().addListener((observableValue, oldScene, newScene) -> {
            if (oldScene == null && newScene != null) {
                Platform.runLater(() -> {
                    getSubControllers().forEach(SubController::forcedInitialize);
                    speedFactorProperty_ = sidebarController.getSpeedMultiplicatorProperty();
                    reset();
                });
            }
        });
    }

    private @Nonnull
    AgentContainer createJADEEnvironment() {
        Runtime runtime = Runtime.instance();

        Profile profile = new ProfileImpl();
        //profile.setParameter(Profile.GUI, "true");
        return runtime.createMainContainer(profile);
    }

    private void reset() {
        halActions_.clear();
        globalStartTime_ = Instant.now();

        getSubControllers().forEach(SubController::reset);
    }

    private class HALAction implements Comparable<HALAction> { // TODO how can this be saves as a JSON?
        private long time_;
        private final @Nonnull
        Runnable action_;

        HALAction(long time, @Nonnull Runnable action) {
            time_ = time;
            action_ = action;
        }

        @Override
        public int compareTo(@Nonnull HALAction other) {
            return (int) (time_ - other.time_);
        }

        @Override
        public String toString() {
            return time_ + "ms";
        }
    }

    private @Nonnull
    Runnable addHALAction(@Nonnull Runnable action) {
        long elapsedTime = java.time.Duration.between(globalStartTime_, Instant.now()).toMillis();
        halActions_.add(new HALAction(elapsedTime, action));

        return action;
    }

    private boolean incrementSimulationTime() {
        if (simulationTimePointer_.get() < halActions_.size() - 1) {
            simulationTimePointer_.set(simulationTimePointer_.intValue() + 1);

            return true;
        } else {
            return false;
        }
    }

    private void animateTimeProgress(@Nonnull Duration duration) {
        Timeline oldTimeline = (Timeline) timeProgressSlider_.getProperties().get(Timeline.class.getSimpleName());

        runInNewThread(() -> {
            if (oldTimeline != null) {
                oldTimeline.stop();
                while (oldTimeline.getStatus() == Animation.Status.RUNNING) ;
            }

            Platform.runLater(() -> {
                Timeline timeline = new Timeline();
                timeline.getKeyFrames().add(
                        new KeyFrame(duration,
                                new KeyValue(
                                        timeProgressSlider_.valueProperty(),
                                        halActions_.get(simulationTimePointer_.get()).time_)));
                timeline.play();
                timeProgressSlider_.getProperties().put(Timeline.class.getSimpleName(), timeline);
            });
        });
    }

    void playSimulation(@Nonnull Runnable donePlayingCallback) {
        runInNewThread(() -> {
            simulationIsPlaying_.set(true);

            Supplier<Boolean> continuePlaying = () -> {
                Wrapper<Boolean> result = new Wrapper<>(true);
                syncPlatformRunLater(() -> result.set(simulationIsPlaying_.get() && incrementSimulationTime()));
                return result.get();
            };

            while (continuePlaying.get()) {
                long prevTime = halActions_.get(simulationTimePointer_.get() - 1).time_;
                long currentTime = halActions_.get(simulationTimePointer_.get()).time_;
                long duration = (long) ((currentTime - prevTime) / speedFactorProperty_.get());

                animateTimeProgress(Duration.millis(duration));

                try {
                    Thread.sleep(duration);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                halActions_.get(simulationTimePointer_.get()).action_.run();
            }
            simulationIsPlaying_.set(false);
            donePlayingCallback.run();
        });
    }

    void pauseSimulation() {
        simulationIsPlaying_.set(false);
        animateTimeProgress(Duration.millis(1));
    }

    void stepSimulationForward() {
        if (incrementSimulationTime()) {
            animateTimeProgress(Duration.millis(1));
            halActions_.get(simulationTimePointer_.get()).action_.run();
        }
    }

    void stopSimulation() {
        pauseSimulation();

        simulationTimePointer_.set(0);
        halActions_.get(simulationTimePointer_.get()).action_.run();
        animateTimeProgress(Duration.millis(1));
    }

    // TODO cleanup implementation!
    private class SimulationCoDyHAL implements CoDyAgentHAL {
        private final @Nonnull
        Point startPos_;
        private final @Nonnull
        ObjectProperty<Point> currentPositionProperty_ = new SimpleObjectProperty<>();
        private final @Nonnull
        ObjectProperty<Duration> transitionTimeProperty_ = new SimpleObjectProperty<>();

        private final @Nonnull
        Map<Direction, Function<Point, Point>> stepFunctions_ = new HashMap<Direction, Function<Point, Point>>() {{
            put(Direction.EAST, p -> p.addX(1));
            put(Direction.SOUTH, p -> p.addY(1));
            put(Direction.WEST, p -> p.addX(-1));
            put(Direction.NORTH, p -> p.addY(-1));
        }};

        SimulationCoDyHAL(@Nonnull Point startPos, @Nonnull AgentParameter agentParameter) {
            startPos_ = startPos;
            transitionTimeProperty_.bind(Bindings.createObjectBinding(() ->
                            Duration.millis((long) (agentParameter.getRobotSize() / agentParameter.getRobotSpeed() / speedFactorProperty_.doubleValue() * 1000.0)),
                    speedFactorProperty_));
//                            Duration.seconds((long) (agentParameter.getRobotSize() / agentParameter.getRobotSpeed() / speedFactorProperty_.doubleValue())),
            reset();
        }

        private void reset() {
            currentPositionProperty_.set(startPos_);
        }

        @Override
        public void move(@Nonnull Direction direction/*, @Nonnull Runnable callback*/) {
            Point nextPos = stepFunctions_.get(direction).apply(currentPositionProperty_.get());
            addHALAction(() -> currentPositionProperty_.set(nextPos)).run();
        }

        @Nonnull
        @Override
        public Point getCurrentPos() {
            return currentPositionProperty_.get();
        }

        ObservableValue<Point> getCurrentPositionProperty() {
            return currentPositionProperty_;
        }
    }
}