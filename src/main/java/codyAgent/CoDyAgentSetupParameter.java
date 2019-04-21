package codyAgent;

import guis.mapBuilder.AgentParameter;
import helper.Point;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

public class CoDyAgentSetupParameter {
    final int uiID_; // TODO no UI stuff here
    final @Nonnull
    CoDyAgentHAL coDyAgentHal_;
    final @Nonnull
    Consumer<CoDyAgent> registerAgent_;
    final @Nonnull
    Point startPos_;
    final @Nonnull
    Point target_;
    final @Nonnull
    Integer dimensions_;
    final @Nonnull
    List<Point> staticObstacles_;
    final @Nonnull
    AgentParameter agentParameter_;

    public CoDyAgentSetupParameter(int uiID, @Nonnull CoDyAgentHAL coDyAgentHal, @Nonnull Consumer<CoDyAgent> registerAgent, @Nonnull Point startPos, @Nonnull Point target, @Nonnull Integer dimensions,
                                   @Nonnull List<Point> staticObstacles, @Nonnull AgentParameter agentParameter) {
        uiID_ = uiID;
        coDyAgentHal_ = coDyAgentHal;
        registerAgent_ = registerAgent;
        startPos_ = startPos;
        target_ = target;
        dimensions_ = dimensions;
        staticObstacles_ = staticObstacles;
        agentParameter_ = agentParameter;
    }


}
