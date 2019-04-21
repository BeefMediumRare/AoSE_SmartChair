package helper;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class Methods {

    public static void syncPlatformRunLater(@Nonnull Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            throw new IllegalStateException("Calling this method from the FXApplicationThread would cause a deadlock!");
        }
        Semaphore semaphore = new Semaphore(1);
        try {
            semaphore.acquire();

            Platform.runLater(() -> {
                runnable.run();
                semaphore.release();
            });
            semaphore.acquire();
            semaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static <T> void addListenerAndTrigger(@Nonnull ObservableValue<T> value, @Nonnull ChangeListener<T> listener, @Nullable T initialValue) {
        value.addListener(listener);
        listener.changed(value, value.getValue(), initialValue);
    }

    public static <T> T get(Supplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (NullPointerException e) {
            return defaultValue;
        }
    }

    public static void runInNewThread(@Nonnull Runnable runnable) {
        new Thread(runnable).start();
    }

    public static void iterateOver(int endXExclusive, int endYExclusive,
                                   @Nonnull Consumer<Point> consumer) {
        range(endXExclusive).forEach((x) ->
                range(endYExclusive).forEach((y) -> consumer.accept(new Point(x, y))));
    }

    public static @Nonnull
    <T> Optional<T> get(@Nonnull List<T> list, @Nullable T value) {
        int index = value != null ? list.indexOf(value) : -1;
        return Optional.ofNullable(index < 0 ? null : list.get(index));
    }

    @SafeVarargs
    public static <T> boolean or(@Nonnull Function<T, Boolean> orFunc, @Nonnull T... orFuncParams) {
        return Arrays.stream(orFuncParams).anyMatch(orFunc::apply);
    }

    @SafeVarargs
    public static <T> boolean and(@Nonnull Function<T, Boolean> andFunc, @Nonnull T... andFuncParams) {
        return Arrays.stream(andFuncParams).allMatch(andFunc::apply);
    }

    public static @Nonnull
    IntStream range(int startInclusive, int endExclusive) {
        return IntStream.range(startInclusive, endExclusive);
    }

    public static @Nonnull
    IntStream range(int start, int step, int elementCount) {
        return IntStream.iterate(start, n -> n + step).limit(elementCount);
    }

    public static @Nonnull
    LongStream range(long start, long step, long elementCount) {
        return LongStream.iterate(start, n -> n + step).limit(elementCount);
    }

    public static @Nonnull
    IntStream range(int endExclusive) {
        return range(0, endExclusive);
    }

    public static void constructExceptionAlert(@Nonnull Scene origScene, @Nonnull Exception exception) {
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        constructTextAreaAlert(origScene, "Exception", sw.toString());
    }

    public static void constructTextAreaAlert(@Nonnull Scene origScene, @Nonnull String title, @Nonnull String text) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.getDialogPane().getStylesheets().addAll(origScene.getStylesheets());
        alert.getDialogPane().getStyleClass().add("alert-dialog-pane");
        alert.setTitle(title);

        TextArea textArea = new TextArea(text);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        DialogPane dialog = alert.getDialogPane();
        alert.setResizable(true);
        dialog.setExpanded(true);
        dialog.setExpandableContent(textArea);
        dialog.setPrefWidth(600);
        dialog.setPrefHeight(400);
        alert.showAndWait();
    }

    public static @Nonnull
    Alert constructAlert(@Nonnull Scene origScene, @Nonnull Alert.AlertType alertType, @Nonnull String title, @Nonnull String header, @Nonnull ButtonType... buttons) {
        Alert alert = new Alert(alertType, "", buttons);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.getDialogPane().getStylesheets().addAll(origScene.getStylesheets());
        alert.getDialogPane().getStyleClass().add("alert-dialog-pane");
        return alert;
    }

    public static @Nonnull
    Dialog<Void> constructSpinner(@Nonnull Scene origScene) {
        return constructSpinner(origScene, "");
    }

    public static @Nonnull
    Dialog<Void> constructSpinner(@Nonnull Scene origScene, @Nonnull String message) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.getDialogPane().getScene().getStylesheets().addAll(origScene.getStylesheets());

        dialog.getDialogPane().setPadding(new Insets(5));
        dialog.getDialogPane().setStyle("-fx-background-color: white, -grey; -fx-background-insets: 1");

        ProgressIndicator progressIndicator = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressIndicator.setMaxHeight(30);
        progressIndicator.setMaxWidth(progressIndicator.getMaxHeight());

        VBox vBox = new VBox(progressIndicator, new Label(message));
        vBox.setSpacing(10);
        vBox.setAlignment(Pos.TOP_CENTER);
        vBox.setStyle("-fx-background-color: transparent");

        dialog.getDialogPane().setContent(vBox);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        return dialog;
    }

    public static <T> Optional<T> getLast(@Nonnull Collection<T> collection) {
        return collection.stream().reduce((v1, v2) -> v2);
    }

    public static <T> Optional<T> getLast(@Nonnull List<T> list) {
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() - 1));
    }

    public static <T extends Comparable<T>> T min(T... values) {
        return Collections.min(Arrays.asList(values));
    }
}
