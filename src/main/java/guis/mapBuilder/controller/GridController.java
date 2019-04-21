package guis.mapBuilder.controller;

import guis.UICellFactory;
import guis.mapBuilder.SimpleCoDyAgent;
import helper.Point;
import helper.SubController;
import helper.TriConsumer;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static helper.Methods.*;


//TODO integrate dynamic agent (current) pos
public class GridController extends SubController<MainController> {
    private enum Layer {
        AGENT_TARGETS, AGENT_TARGETS_BG, AGENT_START_POSITIONS, AGENT_START_POSITIONS_BG, STATIC_OBSTACLES, GRID;

        public static boolean isBgLayer(@Nonnull Layer layer) {
            return layer.toString().toLowerCase().matches(".*bg");
        }
    }

    @FXML
    private ScrollPane grid_;
    @FXML
    private StackPane gridContainer_;

    private final @Nonnull
    HashMap<Layer, AnchorPane> panes_ = new HashMap<>();
    private @Nonnull
    BooleanProperty split_ = new SimpleBooleanProperty();
    private boolean moving_ = false;

    @Nonnull
    Node getGrid() {
        return grid_;
    }

    void removeAgent(@Nonnull Integer id) {
        Arrays.stream(Layer.values())
                .map(layer -> panes_.get(layer).getChildren())
                .forEach(list -> list.removeAll(
                        list.filtered(node -> id.equals(UICellFactory.getAgentID(node).orElse(null)))));
    }

    void focusAgent(@Nonnull Integer id) {
        moving_ = true;
        moveNodes(Layer.AGENT_START_POSITIONS, Layer.AGENT_START_POSITIONS_BG, panes_.get(Layer.AGENT_START_POSITIONS).getChildren());
        moveNodes(Layer.AGENT_TARGETS, Layer.AGENT_TARGETS_BG, panes_.get(Layer.AGENT_TARGETS).getChildren());

        TriConsumer<Layer, Layer, Integer> moveFromBgLayer = (bgLayer, layer, agentID) ->
                moveNodes(bgLayer, layer, panes_.get(bgLayer).getChildren().stream().filter(
                        node -> agentID.equals(UICellFactory.getAgentID(node).orElse(null))).collect(Collectors.toList()));

        moveFromBgLayer.consume(Layer.AGENT_START_POSITIONS_BG, Layer.AGENT_START_POSITIONS, id);
        moveFromBgLayer.consume(Layer.AGENT_TARGETS_BG, Layer.AGENT_TARGETS, id);
        moving_ = false;
    }

    private void moveNodes(@Nonnull Layer from, @Nonnull Layer to, List<Node> nodes) {
        panes_.get(to).getChildren().addAll(nodes);
        panes_.get(from).getChildren().removeAll(nodes);
    }

    void drawGrid(@Nonnull Integer dimensions) {
        Arrays.stream(Layer.values()).forEach(layer -> panes_.get(layer).getChildren().clear());

        //TODO add create ticks

        iterateOver(dimensions, dimensions, pos ->
                panes_.get(Layer.GRID).getChildren().add(
                        UICellFactory.createClickableUICell(pos, () -> cellPressed(pos), () -> cellHoveredWithShiftDown(pos), () -> cellHoveredWithAltDown(pos))));
    }

    private void cellHoveredWithShiftDown(@Nonnull Point pos) {
        Optional<Tool> optionalTool = mainController_.getCurrentTool();
        if (optionalTool.isPresent() && optionalTool.get() == Tool.SET_OBSTACLE) {
            boolean occupied = isOccupied(pos, Layer.AGENT_START_POSITIONS, Layer.AGENT_START_POSITIONS_BG, Layer.AGENT_TARGETS, Layer.AGENT_TARGETS_BG);
            if (!occupied) {
                Optional<Node> uiCell = getNode(pos, Layer.STATIC_OBSTACLES);
                if (!uiCell.isPresent()) {
                    panes_.get(Layer.STATIC_OBSTACLES).getChildren().add(createNode(pos, Layer.STATIC_OBSTACLES));
                }
            }
        }
    }

    private void cellHoveredWithAltDown(@Nonnull Point pos) {
        Optional<Tool> optionalTool = mainController_.getCurrentTool();
        if (optionalTool.isPresent() && optionalTool.get() == Tool.SET_OBSTACLE) {
            boolean occupied = isOccupied(pos, Layer.AGENT_START_POSITIONS, Layer.AGENT_START_POSITIONS_BG, Layer.AGENT_TARGETS, Layer.AGENT_TARGETS_BG);
            if (!occupied) {
                Optional<Node> uiCell = getNode(pos, Layer.STATIC_OBSTACLES);
                uiCell.ifPresent(node -> panes_.get(Layer.STATIC_OBSTACLES).getChildren().remove(node));
            }
        }
    }

    private void cellPressed(@Nonnull Point pos) {
        Optional<Tool> optionalTool = mainController_.getCurrentTool();
        if (optionalTool.isPresent()) {
            Tool currentTool = optionalTool.get();
            Layer targetLayer = getLayer(currentTool);

            boolean occupied = (currentTool == Tool.AGENT_START && isOccupied(pos, Layer.AGENT_START_POSITIONS_BG, Layer.STATIC_OBSTACLES)) ||
                    (currentTool == Tool.AGENT_TARGET && isOccupied(pos, Layer.AGENT_TARGETS_BG, Layer.STATIC_OBSTACLES)) ||
                    (currentTool == Tool.SET_OBSTACLE && isOccupied(pos, Layer.AGENT_START_POSITIONS, Layer.AGENT_START_POSITIONS_BG, Layer.AGENT_TARGETS, Layer.AGENT_TARGETS_BG));

            if (!occupied) {
                toggle(pos, targetLayer);
            } else {
                constructAlert(mainController_.getScene(), Alert.AlertType.ERROR, "", "This cell is already occupied").showAndWait();
            }
        } else {
            Stream.of(Layer.AGENT_START_POSITIONS, Layer.AGENT_START_POSITIONS_BG, Layer.AGENT_TARGETS, Layer.AGENT_TARGETS_BG)
                    .map(panes_::get).filter(AnchorPane::isVisible).flatMap(pane -> pane.getChildren().stream()
                    .filter(node -> pos.equals(UICellFactory.getPosition(node).orElse(null)) && UICellFactory.getAgentID(node).isPresent()))
                    .findFirst().ifPresent(node -> mainController_.focusAgent(UICellFactory.getAgentID(node).get()));
        }
    }

    private boolean isOccupied(@Nonnull Point pos, @Nonnull Layer... layers) {
        return Stream.of(layers).anyMatch(layer -> getNode(pos, layer).isPresent());
    }

    private @Nonnull
    Optional<Node> getNode(@Nonnull Point pos, @Nonnull Layer layer) {
        return panes_.get(layer).getChildren().stream().filter(
                uiCell -> UICellFactory.getPosition(uiCell).isPresent() &&
                        UICellFactory.getPosition(uiCell).get().equals(pos)).findFirst();
    }

    private @Nonnull
    Node createNode(@Nonnull Point pos, @Nonnull Layer layer) {
        switch (layer) {
            case AGENT_START_POSITIONS:
                return UICellFactory.createAgentStartPositionUICell(pos, mainController_.getSelectedAgentID().get(), split_);
            case AGENT_TARGETS:
                return UICellFactory.createTargetUICell(pos, mainController_.getSelectedAgentID().get(), split_);
            case STATIC_OBSTACLES:
                return UICellFactory.createStaticObstacleCell(pos);
            default:
                throw new IllegalArgumentException("No uiCell to return for " + layer);
        }
    }

    private void toggle(@Nonnull Point pos, @Nonnull Layer targetLayer) {
        Optional<Node> uiCell = getNode(pos, targetLayer);
        if (uiCell.isPresent()) {
            panes_.get(targetLayer).getChildren().remove(uiCell.get());
        } else {
            if (or(layer -> targetLayer == layer, Layer.AGENT_START_POSITIONS, Layer.AGENT_TARGETS)) {
                panes_.get(targetLayer).getChildren().clear();
            }
            panes_.get(targetLayer).getChildren().add(createNode(pos, targetLayer));
        }
    }

    private @Nonnull
    Layer getLayer(@Nonnull Tool currentTool) {
        switch (currentTool) {
            case SET_OBSTACLE:
                return Layer.STATIC_OBSTACLES;
            case AGENT_START:
                return Layer.AGENT_START_POSITIONS;
            case AGENT_TARGET:
                return Layer.AGENT_TARGETS;
            default:
                throw new IllegalArgumentException("No layer to return for " + currentTool);
        }
    }

    void setStaticObstacles(@Nonnull List<Point> staticObstacles) {
        panes_.get(Layer.STATIC_OBSTACLES).getChildren().clear();
        panes_.get(Layer.STATIC_OBSTACLES).getChildren().addAll(
                staticObstacles.stream()
                        .map(UICellFactory::createStaticObstacleCell).collect(Collectors.toList()));
    }

    void setAgentStartPosVisibility(boolean visibility) {
        Stream.of(Layer.AGENT_START_POSITIONS, Layer.AGENT_START_POSITIONS_BG).forEach(
                layer -> panes_.get(layer).setVisible(visibility)
        );
    }

    void setAgentTargetsVisibility(boolean visibility) {
        Stream.of(Layer.AGENT_TARGETS, Layer.AGENT_TARGETS_BG).forEach(
                layer -> panes_.get(layer).setVisible(visibility)
        );
    }

    @Override
    public void reset() {
        drawGrid(mainController_.getDimensions());
    }

    void open(@Nonnull List<SimpleCoDyAgent> agents, @Nonnull List<Point> staticObstacles, @Nonnull Integer dimensions) {
        drawGrid(dimensions);
        setStaticObstacles(staticObstacles);
        setAgents(agents);
    }

    private void setAgents(@Nonnull List<SimpleCoDyAgent> agents) {
        panes_.get(Layer.AGENT_TARGETS).getChildren().clear();
        panes_.get(Layer.AGENT_START_POSITIONS).getChildren().clear();

        agents.forEach(agent -> {
            agent.getTarget().ifPresent
                    (target -> panes_.get(Layer.AGENT_TARGETS).getChildren().add(
                            UICellFactory.createTargetUICell(target, agent.getID(), split_)));

            agent.getStartPos().ifPresent(startPos ->
                    panes_.get(Layer.AGENT_START_POSITIONS).getChildren().add(
                            UICellFactory.createAgentStartPositionUICell(startPos, agent.getID(), split_)));
        });
    }

    @Override
    public void forcedInitialize() {
        Arrays.stream(Layer.values()).forEach(layer -> {
            AnchorPane pane = newTransparentAnchorPane();
            pane.setOpacity(Layer.isBgLayer(layer) ? 1.0 : 1.0);
            panes_.put(layer, pane);
            gridContainer_.getChildren().add(pane);
        });

        panes_.get(Layer.AGENT_START_POSITIONS).getChildren().addListener((ListChangeListener.Change<? extends Node> change) -> {
            if (change.next() && !moving_) {
                Stream.concat(change.getAddedSubList().stream(), change.getRemoved().stream()).forEach(
                        node -> UICellFactory.getPosition(node).ifPresent(
                                pos -> UICellFactory.getAgentID(node).ifPresent(
                                        id -> mainController_.setAgentStartPos(id, change.wasAdded() ? pos : null))));
            }
        });

        panes_.get(Layer.AGENT_TARGETS).getChildren().addListener((ListChangeListener.Change<? extends Node> change) -> {
            if (change.next() && !moving_) {
                Stream.concat(change.getAddedSubList().stream(), change.getRemoved().stream()).forEach(
                        node -> UICellFactory.getPosition(node).ifPresent(
                                pos -> UICellFactory.getAgentID(node).ifPresent(
                                        id -> mainController_.setAgentTarget(id, change.wasAdded() ? pos : null))));
            }
        });

        panes_.get(Layer.STATIC_OBSTACLES).getChildren().addListener((ListChangeListener.Change<? extends Node> change) -> {
            if (change.next()) {
                Stream.concat(change.getAddedSubList().stream(), change.getRemoved().stream()).forEach(
                        node -> UICellFactory.getPosition(node).ifPresent(
                                pos -> {
                                    if (change.wasAdded()) {
                                        mainController_.addStaticObstacle(pos);
                                    } else {
                                        mainController_.removeStaticObstacle(pos);
                                    }
                                }));
            }
        });


        split_.bind(Bindings.createBooleanBinding(
                () -> panes_.get(Layer.AGENT_START_POSITIONS).isVisible() && panes_.get(Layer.AGENT_TARGETS).isVisible(),
                panes_.get(Layer.AGENT_START_POSITIONS).visibleProperty(), panes_.get(Layer.AGENT_TARGETS).visibleProperty()));

        gridContainer_.translateXProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(0.0, (grid_.getWidth() - gridContainer_.getWidth()) / 2.0),
                grid_.widthProperty(), gridContainer_.widthProperty()));
        gridContainer_.translateYProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(0.0, (grid_.getHeight() - gridContainer_.getHeight()) / 2.0),
                grid_.heightProperty(), gridContainer_.heightProperty()));
    }

    private @Nonnull
    AnchorPane newTransparentAnchorPane() {
        AnchorPane anchorPane = new AnchorPane();
        anchorPane.setStyle("-fx-background-color: transparent;");
        return anchorPane;
    }
}
