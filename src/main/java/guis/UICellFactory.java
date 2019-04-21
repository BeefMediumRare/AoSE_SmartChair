package guis;

import helper.Point;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class UICellFactory {
    private static final int gridCellSize_ = 35;

    private enum extraFields {
        POSITION, AGENT_ID
    }

    public static @Nonnull
    Node createClickableUICell(@Nonnull Point pos, @Nonnull Runnable clickCallback) {
        Node cell = createBase(pos);
        cell.getStyleClass().add("grid-cell-transparent");
        cell.setOnMousePressed(e -> clickCallback.run());

        return cell;
    }


    public static @Nonnull
    Node createClickableUICell(@Nonnull Point pos, @Nonnull Runnable clickCallback, @Nonnull Runnable hoverShiftCallback, @Nonnull Runnable hoverAltCallback) {
        Node cell = createClickableUICell(pos, clickCallback);
        cell.setOnMouseEntered(e -> {
            if (e.isShiftDown()) {
                hoverShiftCallback.run();
            } else if (e.isAltDown()) {
                hoverAltCallback.run();
            }
        });

        return cell;
    }

    public static @Nonnull
    Node createAgentPosUICell(@Nonnull Integer agentID, @Nonnull ObservableValue<Point> position, @Nonnull ObservableValue<Duration> cellChangeDuration) {
        Circle circle = new Circle(gridCellSize_ / 2.0 * 0.8);
        circle.setStyle("-fx-fill: -agent-start;");
        Pane root = new StackPane(circle, new Label(agentID.toString()));
        root.getProperties().put(extraFields.AGENT_ID, agentID);

        TranslateTransition transition = new TranslateTransition(Duration.millis(1), root);
        cellChangeDuration.addListener((observable, oldValue, newValue) -> transition.setDuration(newValue));

        ChangeListener<Point> changeListener = (observable, oldValue, newValue) -> {
            transition.setToX(newValue.getX() * gridCellSize_);
            transition.setToY(newValue.getY() * gridCellSize_);
            transition.playFromStart();

            root.getProperties().put(extraFields.POSITION, newValue);
        };
        changeListener.changed(position, null, position.getValue());
        position.addListener(changeListener);

        transition.setDuration(cellChangeDuration.getValue());

        return root;
    }

//    public static @Nonnull
//    Node createTickUICell(@Nonnull Point pos, @Nonnull String text) {
//        StackPane pane = createBase(pos);
//        pane.getStyleClass().add("grid-cell-transparent");
//        ((AnchorPane) pane.getChildren().get(0)).getChildren().add(createLabel(text, "-grey", null, Pos.CENTER));
//        return pane;
//    }

    public static @Nonnull
    Node createTargetUICell(@Nonnull Integer agentID, @Nonnull Point pos) {
        return createStaticAgentPosCell(pos, agentID, new SimpleBooleanProperty(false), true);
    }

    public static @Nonnull
    Node createTargetUICell(@Nonnull Point pos, @Nonnull Integer agentID, @Nonnull ObservableValue<Boolean> split) {
        return createStaticAgentPosCell(pos, agentID, split, true);
    }

    public static @Nonnull
    Node createAgentStartPositionUICell(@Nonnull Point pos, @Nonnull Integer agentID, @Nonnull ObservableValue<Boolean> split) {
        return createStaticAgentPosCell(pos, agentID, split, false);
    }

    //TODO naming
    private static @Nonnull
    Node createStaticAgentPosCell(@Nonnull Point pos, @Nonnull Integer agentID, @Nonnull ObservableValue<Boolean> split, boolean isTarget) {
        StackPane pane = createBase(pos);
        AnchorPane root = (AnchorPane) pane.getChildren().get(0);

        root.styleProperty().bind(Bindings.createStringBinding(() -> {
            String style = "-fx-background-color: ";
            String color = "-agent-" + (isTarget ? "target" : "start");

            if (split.getValue()) {
                style += "linear-gradient(from 0% 0% to 100% 100%, "
                        + (isTarget ? "transparent 50%, " : "") + color + " 50% "
                        + (!isTarget ? ", transparent 50%" : "") + ");";
            } else {
                style += color + ";";
            }

            return style;
        }, split));

        pane.getProperties().put(extraFields.AGENT_ID, agentID);

        root.getChildren().add(createLabel(agentID.toString(), "white", 10, isTarget ? Pos.BOTTOM_RIGHT : Pos.TOP_LEFT));
        return pane;
    }

    private static @Nonnull
    Label createLabel(@Nonnull String text, @Nonnull String color, @Nullable Integer fontsize, @Nonnull Pos alignment) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + color + ";" + (fontsize != null ? "-fx-font-size: " + fontsize + "px" : ""));
        label.setAlignment(alignment);
        label.setPadding(new Insets(1, 1, -1, 3));

        AnchorPane.setTopAnchor(label, 0.0);
        AnchorPane.setRightAnchor(label, 0.0);
        AnchorPane.setBottomAnchor(label, 0.0);
        AnchorPane.setLeftAnchor(label, 0.0);

        return label;
    }

    public static @Nonnull
    Node createStaticObstacleCell(@Nonnull Point pos) {
        Node cell = createBase(pos);
        cell.getStyleClass().add("grid-cell-dark-grey");
        return cell;
    }

    private static @Nonnull
    StackPane createBase(@Nonnull Point position) {
        StackPane base = new StackPane();
        base.setLayoutX(position.getX() * gridCellSize_);
        base.setLayoutY(position.getY() * gridCellSize_);

        AnchorPane root = new AnchorPane();
        root.setStyle("-fx-background-color: transparent");
        root.setPrefWidth(gridCellSize_);
        root.setPrefHeight(gridCellSize_);

        base.getChildren().add(root);
        base.getProperties().put(extraFields.POSITION, position);

        return base;
    }

    public static @Nonnull
    Optional<Integer> getAgentID(@Nonnull Node node) {
        return Optional.ofNullable(Integer.class.cast(node.getProperties().get(extraFields.AGENT_ID)));
    }

    public static @Nonnull
    Optional<Point> getPosition(@Nonnull Node node) {
        return Optional.ofNullable((Point) node.getProperties().get(extraFields.POSITION));
    }
}
