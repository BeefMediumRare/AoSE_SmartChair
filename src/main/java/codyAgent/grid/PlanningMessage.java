package codyAgent.grid;

import helper.Point;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class PlanningMessage {
    private final @Nonnull
    String agentID_;
    private final
    long timestampStarted_;
    private final @Nonnull
    Point agentPosition_;
    private final
    int agentPriority_;
    private final @Nonnull
    Map<Integer, Point> spaceTimePath_;

    public PlanningMessage(@Nonnull String agentID, long timestampStarted, @Nonnull Point agentPosition, int agentPriority, @Nonnull Map<Integer, Point> spaceTimePath) {
        agentID_ = agentID;
        timestampStarted_ = timestampStarted;
        agentPosition_ = agentPosition;
        agentPriority_ = agentPriority;
        spaceTimePath_ = spaceTimePath;
    }


    public PlanningMessage(@Nonnull JSONObject planningMessageJSON) {
        agentID_ = planningMessageJSON.getString("agentID");
        timestampStarted_ = planningMessageJSON.getLong("timestampStarted");
        agentPosition_ = new Point(planningMessageJSON.getJSONObject("agentPosition"));
        agentPriority_ = planningMessageJSON.getInt("agentPriority");
        JSONObject spaceTimePathJSON = planningMessageJSON.getJSONObject("spaceTimePath");
        spaceTimePath_ = spaceTimePathJSON.keySet().stream()
                .collect(Collectors.toMap(Integer::parseInt, key -> new Point(spaceTimePathJSON.getJSONObject(key))));
    }

    public @Nonnull
    JSONObject tokenize() {
        return new JSONObject(new HashMap<String, Object>() {{
            put("agentID", agentID_);
            put("timestampStarted", timestampStarted_);
            put("agentPosition", agentPosition_);
            put("agentPriority", agentPriority_);
            put("spaceTimePath", new JSONObject(spaceTimePath_.entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().tokenize()))));
        }});
    }

    @Nonnull
    public String getAgentID() {
        return agentID_;
    }

    public long getTimestampStarted() {
        return timestampStarted_;
    }

    @Nonnull
    public Point getAgentPosition() {
        return agentPosition_;
    }

    public int getAgentPriority() {
        return agentPriority_;
    }

    @Nonnull
    public Map<Integer, Point> getSpaceTimePath() {
        return spaceTimePath_;
    }

    @Override
    public String toString() {
        return String.format("From\t%s\n" +
                "Position\t%s\n" +
                "Path\t\t%s\n" +
                "Priority\t%d\n" +
                "tStarted\t%d\n", agentID_, agentPosition_, spaceTimePath_, agentPriority_, timestampStarted_);
    }
}
