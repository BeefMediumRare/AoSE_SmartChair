package guis.customControl;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;

//TODO make a proper FX Class, that has its own css selectors and stuff.
//This should move then to a lib that this project can use as a dependency
// http://www.guigarage.com/2012/11/custom-ui-controls-with-javafx-part-1/

public class ProgressSlider extends StackPane {
    private DoubleProperty blockIncrement = new SimpleDoubleProperty();
    private DoubleProperty min = new SimpleDoubleProperty();
    private DoubleProperty max = new SimpleDoubleProperty();
    private DoubleProperty value = new SimpleDoubleProperty();

    public ProgressSlider() {
        this(10, 0, 100, 0);
    }

    public ProgressSlider(double min, double max, double value, double blockIncrement) {
        if (!(min < max && value >= min && value <= max)) {
            throw new IllegalArgumentException("min(" + min + ") must be smaller than max(" + max + ") and value(" + value + ") must be between min and max.");
        }
        setBlockIncrement(blockIncrement);
        setMin(min);
        setMax(max);
        setValue(value);

        Slider slider = new Slider(min, max, value);
        slider.valueProperty().bindBidirectional(valueProperty());

        slider.minProperty().bind(minProperty());
        slider.maxProperty().bind(maxProperty());
        slider.blockIncrementProperty().bind(blockIncrementProperty());
        slider.majorTickUnitProperty().bind(blockIncrementProperty());
        slider.setMinorTickCount(0);
        slider.setSnapToTicks(true);

        ProgressBar progressBar = new ProgressBar();
        progressBar.progressProperty().bind(Bindings.createDoubleBinding(() -> {
            double valueInPercent = (getValue() - getMin()) / (getMax() - getMin());
            return valueInPercent + (valueInPercent < 0.2 ? 0.015 : (valueInPercent < 0.4 ? 0.01 : (valueInPercent < 0.6 ? 0.005 : (valueInPercent < 0.99 ? -0.005 : 0.0))));
        }, valueProperty()));

        slider.minWidthProperty().bind(widthProperty());
        slider.prefWidthProperty().bind(widthProperty());
        slider.maxWidthProperty().bind(widthProperty());

        progressBar.minWidthProperty().bind(widthProperty());
        progressBar.prefWidthProperty().bind(widthProperty());
        progressBar.maxWidthProperty().bind(widthProperty());

        progressBar.setStyle("-fx-background-color: transparent;");

        new Thread(() -> {
            int reps = 0;
            while (reps++ < 5) {
                try {
                    Platform.runLater(() -> {
                        progressBar.lookup(".bar").setStyle("-fx-background-insets: 0;-fx-padding: 2;");
                        slider.lookup(".track").setStyle("-fx-background-color: transparent;-fx-padding: 2;");
                    });
                } catch (NullPointerException ignored1) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored2) {
                    }
                }
            }
        }).start();

        setStyle("-fx-background-color: transparent");
        setAlignment(Pos.CENTER);

        getChildren().addAll(progressBar, slider);
    }

    public final void setBlockIncrement(double value) {
        blockIncrement.set(value);
    }

    public final double getBlockIncrement() {
        return blockIncrement.get();
    }

    public final DoubleProperty blockIncrementProperty() {
        return blockIncrement;
    }

    public final void setMin(double value) {
        min.set(value);
    }

    public final double getMin() {
        return min.get();
    }

    public final DoubleProperty minProperty() {
        return min;
    }

    public final void setMax(double value) {
        max.set(value);
    }

    public final double getMax() {
        return max.get();
    }

    public final DoubleProperty maxProperty() {
        return max;
    }

    public final void setValue(double value) {
        this.value.set(value);
    }

    public final double getValue() {
        return value.get();
    }

    public final DoubleProperty valueProperty() {
        return value;
    }
}
