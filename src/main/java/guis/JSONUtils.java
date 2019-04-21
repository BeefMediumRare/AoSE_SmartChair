package guis;

import guis.mapBuilder.AutoExecutableQueue;
import helper.Wrapper;
import helper.WrapperNullable;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import stores.JSONFileStore;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static helper.Methods.constructSpinner;

public class JSONUtils { // TODO move

    public static @Nonnull
    JSONObject loadJSON(@Nonnull InputStream stream) throws JSONException {
        return new JSONObject(new JSONTokener(stream));
    }

    public static @Nonnull
    JSONObject loadJSON(@Nonnull String json) throws JSONException {
        return new JSONObject(new JSONTokener(json));
    }

    public static @Nonnull
    JSONObject validateJSON(@Nonnull JSONObject json, @Nonnull JSONObject jsonSchema) throws ValidationException {
        SchemaLoader.load(jsonSchema).validate(json);
        return json;
    }

    public static @Nonnull
    JSONObject validateJSON(@Nonnull JSONObject json, @Nonnull JSONFileStore.JSONFile jsonSchema) throws ValidationException, JSONException {
        return validateJSON(json, loadJSON(JSONFileStore.getAsStream(jsonSchema)));
    }

    // TODO documentation; long => cast exception if value < Integer.MAX_VALUE!
    //  Use (Number.class, n -> n.longValue(), jsonArray) instead!
    public static @Nonnull
    <T> List<T> parseJSONArray(Class<T> type, @Nonnull JSONArray jsonArray) {
        return parseJSONArray(type, obj -> obj, jsonArray);
    }

    public static @Nonnull
    <T, R> List<R> parseJSONArray(Class<T> type, @Nonnull Function<T, R> parseFunc, @Nonnull JSONArray jsonArray) {
        List<R> result = new ArrayList<>();
        jsonArray.forEach(element -> result.add(parseFunc.apply(type.cast(element))));

        return result;
    }

    public static @Nonnull
    <R> List<R> parseJSONArray(@Nonnull Function<JSONObject, R> parseFunc, @Nonnull JSONArray jsonArray) {
        List<R> result = new ArrayList<>();
        jsonArray.forEach(element -> result.add(parseFunc.apply((JSONObject) element)));

        return result;
    }

    public static @Nonnull
    <T> JSONArray tokenize(@Nonnull Function<T, JSONObject> tokenizeFunc, @Nonnull List<T> elements) {
        return new JSONArray(elements.stream().map(tokenizeFunc).collect(Collectors.toList()));
    }

    // TODO move the following somewhere else!

    /**
     * Executes the {@link Runnable} in the current {@link Thread} and calls {@link #showProgressInfoAsync(AutoExecutableQueue, Label, String, boolean) showProgressInfoAsync}.
     *
     * @see #showProgressInfoAsync(AutoExecutableQueue, Label, String, boolean)
     */
    public static void showProgressInfoAsync(@Nonnull AutoExecutableQueue queue, @Nonnull Label progressLabel, @Nonnull String message, @Nonnull Runnable runnable, boolean modal) {
        Runnable done = showProgressInfoAsync(queue, progressLabel, message, modal);
        runnable.run();
        done.run();
    }

    /**
     * Displays the passed message for at least 1000ms in a <b>modal</b> dialog that the user <b>cannot close</b>.
     * <br><b>The message will be displayed <u><i>until</i></u> the {@link Runnable#run() run} method of the returned {@link Runnable} was called!</b>
     *
     * @implNote This method and the returned {@link Runnable} must <b>not</b> be executed in the JavaFX Thread.
     */
    @Nonnull
    public static Runnable showProgressInfoAsync(@Nonnull AutoExecutableQueue queue, @Nonnull Label progressLabel, @Nonnull String message, boolean modal) {
        Wrapper<Boolean> timeOut = new Wrapper<>(false);
        int minDisplayTimeMillis = 500;
        WrapperNullable<Dialog<Void>> spinner = new WrapperNullable<>();

        BooleanProperty taskDone = new SimpleBooleanProperty(false);
        ChangeListener<Boolean> taskDoneListener = (observableValue, oldValue, newValue) -> {
            if (timeOut.get()) {
                Platform.runLater(() -> {
                    progressLabel.setText("");
                    spinner.get().ifPresent(s -> s.getDialogPane().getScene().getWindow().hide());
                });
            }
        };
        taskDone.addListener(taskDoneListener);

        queue.push(() -> {
            Platform.runLater(() -> {
                progressLabel.setText(message + " ...");
                if (modal) {
                    spinner.set(constructSpinner(progressLabel.getScene()));
                    spinner.get().get().show();
                }
            });
            Instant start = Instant.now();
            do {
                try {
                    Thread.sleep(Math.max(100, minDisplayTimeMillis / 10));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (Duration.between(start, Instant.now()).toMillis() < minDisplayTimeMillis);
            timeOut.set(true);
            if (taskDone.get()) {
                taskDoneListener.changed(taskDone, false, true);
            }
        });

        return () -> taskDone.set(true);
    }
}
