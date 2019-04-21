package guis.mapBuilder;

import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Objects;

public class AgentParameter {
    private double robotSize_;
    private double robotSpeed_;

    private int localMapDimensions_;
    private int localMapTMax_;

    private int prioBase_;
    private int prioNoBlock_;
    private int prioBlock_;
    private int prioFullBlock_;
    private int prioMax_;
    private int prioFallbackMin_;
    private int prioFallbackMax_;

    public AgentParameter(double robotSize, double robotSpeed, int localMapDimensions, int localMapTMax, int prioBase, int prioNoBlock, int prioBlock, int prioFullBlock,
                          int prioMax, int prioFallbackMin, int prioFallbackMax, int stepForEachCalculationStep) {
        robotSize_ = robotSize;
        robotSpeed_ = robotSpeed;
        localMapDimensions_ = localMapDimensions;
        localMapTMax_ = localMapTMax;
        prioBase_ = prioBase;
        prioNoBlock_ = prioNoBlock;
        prioBlock_ = prioBlock;
        prioFullBlock_ = prioFullBlock;
        prioMax_ = prioMax;
        prioFallbackMin_ = prioFallbackMin;
        prioFallbackMax_ = prioFallbackMax;
    }

    public AgentParameter(@Nonnull AgentParameter agentParameter) {
        robotSize_ = agentParameter.robotSize_;
        robotSpeed_ = agentParameter.robotSpeed_;
        localMapDimensions_ = agentParameter.localMapDimensions_;
        localMapTMax_ = agentParameter.localMapTMax_;
        prioBase_ = agentParameter.prioBase_;
        prioNoBlock_ = agentParameter.prioNoBlock_;
        prioBlock_ = agentParameter.prioBlock_;
        prioFullBlock_ = agentParameter.prioFullBlock_;
        prioMax_ = agentParameter.prioMax_;
        prioFallbackMin_ = agentParameter.prioFallbackMin_;
        prioFallbackMax_ = agentParameter.prioFallbackMax_;
    }

    public AgentParameter(@Nonnull JSONObject agentParameterJSON) {
        JSONObject robotJSON = agentParameterJSON.getJSONObject("robot");
        robotSize_ = robotJSON.getDouble("size");
        robotSpeed_ = robotJSON.getDouble("speed");

        JSONObject localMapJSON = agentParameterJSON.getJSONObject("localMap");
        localMapDimensions_ = localMapJSON.getInt("dimensions");
        localMapTMax_ = localMapJSON.getInt("tMax");

        JSONObject priorityJSON = agentParameterJSON.getJSONObject("priority");
        prioBase_ = priorityJSON.getInt("base");
        prioNoBlock_ = priorityJSON.getInt("noBlock");
        prioBlock_ = priorityJSON.getInt("block");
        prioFullBlock_ = priorityJSON.getInt("fullBlock");
        prioMax_ = priorityJSON.getInt("max");
        prioFallbackMin_ = priorityJSON.getJSONObject("randomFallback").getInt("min");
        prioFallbackMax_ = priorityJSON.getJSONObject("randomFallback").getInt("max");

    }

    public @Nonnull
    JSONObject tokenize() {
        return new JSONObject(new HashMap<String, Object>() {{
            put("robot", new JSONObject(new HashMap<String, Object>() {{
                put("size", robotSize_);
                put("speed", robotSpeed_);
            }}));
            put("localMap", new JSONObject(new HashMap<String, Object>() {{
                put("dimensions", localMapDimensions_);
                put("tMax", localMapTMax_);
            }}));
            put("priority", new JSONObject(new HashMap<String, Object>() {{
                put("base", prioBase_);
                put("noBlock", prioNoBlock_);
                put("block", prioBlock_);
                put("fullBlock", prioFullBlock_);
                put("max", prioMax_);
                put("randomFallback", new JSONObject(new HashMap<String, Object>() {
                    {
                        put("min", prioFallbackMin_);
                        put("max", prioFallbackMax_);
                    }
                }));
            }}));
        }});
    }

    public double getRobotSize() {
        return robotSize_;
    }

    public double getRobotSpeed() {
        return robotSpeed_;
    }

    public void setRobotSpeed(double speed) {
        robotSpeed_ = speed;
    }

    public int getLocalMapDimensions() {
        return localMapDimensions_;
    }

    public int getLocalMapTMax() {
        return localMapTMax_;
    }

    public int getPrioBase() {
        return prioBase_;
    }

    public int getPrioNoBlock() {
        return prioNoBlock_;
    }

    public int getPrioBlock() {
        return prioBlock_;
    }

    public int getPrioFullBlock() {
        return prioFullBlock_;
    }

    public int getPrioMax() {
        return prioMax_;
    }

    public int getPrioFallbackMin() {
        return prioFallbackMin_;
    }

    public int getPrioFallbackMax() {
        return prioFallbackMax_;
    }

    @Override
    public String toString() {
        return "robotSize: " + String.format("%.2fm", robotSize_) +
                "\nrobotSpeed: " + String.format("%.2fm", robotSpeed_) +
                "\nlocalMapDimensions: " + localMapDimensions_ +
                "\nlocalMapTMax: " + localMapTMax_ +
                "\nprioBase: " + prioBase_ +
                "\nprioNoBlock: " + prioNoBlock_ +
                "\nprioBlock: " + prioBlock_ +
                "\nprioFullBlock: " + prioFullBlock_ +
                "\nprioMax: " + prioMax_ +
                "\nprioFallbackMin: " + prioFallbackMin_ +
                "\nprioFallbackMax: " + prioFallbackMax_;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentParameter other = (AgentParameter) o;
        return robotSize_ == other.robotSize_ &&
                robotSpeed_ == other.robotSpeed_ &&
                localMapDimensions_ == other.localMapDimensions_ &&
                localMapTMax_ == other.localMapTMax_ &&
                prioBase_ == other.prioBase_ &&
                prioNoBlock_ == other.prioNoBlock_ &&
                prioBlock_ == other.prioBlock_ &&
                prioFullBlock_ == other.prioFullBlock_ &&
                prioMax_ == other.prioMax_ &&
                prioFallbackMin_ == other.prioFallbackMin_ &&
                prioFallbackMax_ == other.prioFallbackMax_;
    }

    @Override
    public int hashCode() {
        return Objects.hash(robotSize_, robotSpeed_, localMapDimensions_, localMapTMax_, prioBase_, prioNoBlock_, prioBlock_, prioFullBlock_, prioMax_, prioFallbackMin_, prioFallbackMax_);
    }
}
