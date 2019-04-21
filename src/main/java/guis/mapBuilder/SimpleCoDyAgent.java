package guis.mapBuilder;

import helper.Point;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Optional;

public class SimpleCoDyAgent {
    private final int id_; //TODO use JADE ID

    private @Nullable
    Point startPos_;
    private @Nullable
    Point target_;

    public SimpleCoDyAgent(int id) {
        id_ = id;
    }

    public SimpleCoDyAgent(@Nonnull JSONObject simpleCoDyAgentJSON) {
        this(simpleCoDyAgentJSON.getInt("id"));

        if (simpleCoDyAgentJSON.has("target")) {
            target_ = new Point(simpleCoDyAgentJSON.getJSONObject("target"));
        }
        if (simpleCoDyAgentJSON.has("startPos")) {
            startPos_ = new Point(simpleCoDyAgentJSON.getJSONObject("startPos"));
        }
    }

    public @Nonnull
    JSONObject tokenize() {
        return new JSONObject(new HashMap<String, Object>() {{
            put("id", id_);
            if (target_ != null) {
                put("target", target_.tokenize());
            }
            if (startPos_ != null) {
                put("startPos", startPos_.tokenize());
            }
        }});
    }

    public @Nonnull
    SimpleCoDyAgent setStartPos(@Nullable Point startPos) {
        startPos_ = startPos;
        return this;
    }

    public @Nonnull
    SimpleCoDyAgent setTarget(@Nullable Point target) {
        target_ = target;
        return this;
    }

    public int getID() {
        return id_;
    }

    @Nonnull
    public Optional<Point> getStartPos() {
        return Optional.ofNullable(startPos_);
    }

    @Nonnull
    public Optional<Point> getTarget() {
        return Optional.ofNullable(target_);
    }

    @Override
    public String toString() {
        return "CoDyAgent" + id_
                + Optional.ofNullable(startPos_).map(pos -> " start " + pos).orElse("")
                + Optional.ofNullable(target_).map(pos -> " target " + pos).orElse("");
    }
}