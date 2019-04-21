package guis.simulation.controller;

import guis.UICellFactory;
import helper.Point;
import helper.SubController;
import helper.TriConsumer;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static helper.Methods.iterateOver;

public class GridController extends SubController<MainController> {
    private enum Layer {
        STATIC_OBSTACLES, AGENT_TARGETS, AGENT_TARGETS_BG, AGENT_POSITION, AGENT_POSITION_BG, GRID;
    }

    @FXML
    private ScrollPane grid_;
    @FXML
    private StackPane gridContainer_;

    private final @Nonnull
    HashMap<Layer, AnchorPane> panes_ = new HashMap<>();

    @Override
    public void reset() {
        clearAllPanes();
    }

    @Override
    public void forcedInitialize() {
        Arrays.stream(Layer.values()).forEach(layer -> {
            AnchorPane pane = newTransparentAnchorPane();
            panes_.put(layer, pane);
            gridContainer_.getChildren().add(pane);
        });

        panes_.get(Layer.AGENT_TARGETS).setOpacity(0.8);
        panes_.get(Layer.AGENT_TARGETS_BG).setOpacity(0.45);
        panes_.get(Layer.AGENT_POSITION_BG).setOpacity(0.6);

        gridContainer_.translateXProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(0.0, (grid_.getWidth() - gridContainer_.getWidth()) / 2.0),
                grid_.widthProperty(), gridContainer_.widthProperty()));
        gridContainer_.translateYProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(0.0, (grid_.getHeight() - gridContainer_.getHeight()) / 2.0),
                grid_.heightProperty(), gridContainer_.heightProperty()));
    }

    void open(@Nonnull List<Point> staticObstacles, @Nonnull Integer dimensions) {
        drawGrid(dimensions);
        setStaticObstacles(staticObstacles);
    }

    void focusAgent(@Nonnull Integer id) { //TODO
        moveNodes(Layer.AGENT_POSITION, Layer.AGENT_POSITION_BG, panes_.get(Layer.AGENT_POSITION).getChildren());
        moveNodes(Layer.AGENT_TARGETS, Layer.AGENT_TARGETS_BG, panes_.get(Layer.AGENT_TARGETS).getChildren());

        TriConsumer<Layer, Layer, Integer> moveFromBgLayer = (bgLayer, layer, agentID) ->
                moveNodes(bgLayer, layer, panes_.get(bgLayer).getChildren().stream().filter(
                        node -> agentID.equals(UICellFactory.getAgentID(node).orElse(null))).collect(Collectors.toList()));

        moveFromBgLayer.consume(Layer.AGENT_POSITION_BG, Layer.AGENT_POSITION, id);
        moveFromBgLayer.consume(Layer.AGENT_TARGETS_BG, Layer.AGENT_TARGETS, id);
    }

    void addAgent(@Nonnull Integer id, @Nonnull Point target, @Nonnull ObservableValue<Point> currentPosition, @Nonnull ObservableValue<Duration> transitionSpeedProperty) {
        panes_.get(Layer.AGENT_POSITION_BG).getChildren().add(
                UICellFactory.createAgentPosUICell(id, currentPosition, transitionSpeedProperty));

        panes_.get(Layer.AGENT_TARGETS_BG).getChildren().add(
                UICellFactory.createTargetUICell(id, target));

        focusAgent(id);
    }

    private void moveNodes(@Nonnull Layer from, @Nonnull Layer to, List<Node> nodes) {
        panes_.get(to).getChildren().addAll(nodes);
        panes_.get(from).getChildren().removeAll(nodes);
    }

    private void clearAllPanes() {
        Arrays.stream(Layer.values()).forEach(layer -> panes_.get(layer).getChildren().clear());
    }

    private void drawGrid(@Nonnull Integer dimensions) {
        clearAllPanes();

        //TODO add create ticks

        iterateOver(dimensions, dimensions,
                pos -> panes_.get(Layer.GRID).getChildren().add(
                        UICellFactory.createClickableUICell(pos, () -> cellPressed(pos))));
    }

    private void cellPressed(@Nonnull Point pos) {
        Stream.of(Layer.AGENT_POSITION_BG)
                .map(panes_::get).flatMap(pane -> pane.getChildren().stream()
                .filter(node -> pos.equals(UICellFactory.getPosition(node).orElse(null)) && UICellFactory.getAgentID(node).isPresent()))
                .findFirst().ifPresent(node -> mainController_.focusAgent(UICellFactory.getAgentID(node).get()));
    }

    private void setStaticObstacles(@Nonnull List<Point> staticObstacles) {
        panes_.get(Layer.STATIC_OBSTACLES).getChildren().addAll(
                staticObstacles.stream()
                        .map(UICellFactory::createStaticObstacleCell).collect(Collectors.toList()));
    }

    private @Nonnull
    AnchorPane newTransparentAnchorPane() {
        AnchorPane anchorPane = new AnchorPane();
        anchorPane.setStyle("-fx-background-color: transparent;");
        return anchorPane;
    }
}
