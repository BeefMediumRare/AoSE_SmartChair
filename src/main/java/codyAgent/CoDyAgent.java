package codyAgent;

import codyAgent.grid.DistanceMap;
import codyAgent.grid.LocalMap;
import codyAgent.grid.PlanningMessage;
import guis.JSONUtils;
import guis.mapBuilder.AgentParameter;
import helper.Direction;
import helper.Point;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import javafx.util.Pair;
import org.json.JSONException;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static helper.Methods.*;

public class CoDyAgent extends Agent {
    public static long CREATION_TIME = new Date().getTime(); // TODO make private again

    private final @Nonnull
    Map<Integer, Point> plannedMovements_ = new HashMap<>();
    private int currentEpoch_ = 0;
    private int period_;

    private @Nonnull
    CoDyAgentHAL hal_;
    private @Nonnull
    Point target_;
    private @Nonnull
    AgentParameter agentParameter_;
    private @Nonnull
    DistanceMap distanceMap_;

    private final @Nonnull
    List<PlanningMessage> planningMessages_ = new ArrayList<>();
    private final @Nonnull
    List<AID> otherAgents_ = new ArrayList<>();
    private final @Nonnull
    Map<String, Long> nearAgents_ = new HashMap<>();
    private long tStarted_;
    private boolean atTarget_;

    private enum ServiceDescriptionType {
        CODY_AGENT, PLANNING
    }

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args.length != 1 || !(args[0] instanceof CoDyAgentSetupParameter)) {
            throw new IllegalArgumentException("Expected 1 argument of type " + CoDyAgentSetupParameter.class.getSimpleName());
        }
        construct((CoDyAgentSetupParameter) args[0]);
        registerToYellowPages();

        addBehaviour(new CoDyAgentDiscoveryBehaviour(this));
        addBehaviour(new PlanningBehaviour(this));
        addBehaviour(new MessageReceiveBehaviour(this));
        addBehaviour(new MovementBehaviour(this, period_));
    }

    public boolean isDone() {
        return atTarget_;
    }

    private void registerToYellowPages() {
        DFAgentDescription dfDescription = new DFAgentDescription();
        dfDescription.setName(getAID());

        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType(ServiceDescriptionType.CODY_AGENT.toString());
        serviceDescription.setName(getLocalName() + "-" + serviceDescription.getType());
        dfDescription.addServices(serviceDescription);

        try {
            DFService.register(this, dfDescription);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private void construct(@Nonnull CoDyAgentSetupParameter parameter) {
        hal_ = parameter.coDyAgentHal_;
        target_ = parameter.target_;
        distanceMap_ = new DistanceMap(parameter.dimensions_, parameter.staticObstacles_);
        distanceMap_.calcDistances(target_);
        agentParameter_ = parameter.agentParameter_;
        parameter.registerAgent_.accept(this);

        period_ = (int) (agentParameter_.getRobotSize() / agentParameter_.getRobotSpeed() * 1000);
    }

    private long getCurrentTime() {
        return new Date().getTime() - CREATION_TIME;
    }

    private class MovementBehaviour extends TickerBehaviour {
        MovementBehaviour(@Nonnull Agent agent, long periodInMs) {
            super(agent, periodInMs);
        }

        @Override
        protected void onTick() {
            Point from = plannedMovements_.get(currentEpoch_);
            Point to = plannedMovements_.get(++currentEpoch_);
            if (to == null) {
                throw new IllegalStateException("No planned move to execute " + (currentEpoch_ + 1) + " " + getCurrentTime());
            }

            if (!from.equals(to)) {
                atTarget_ = to.equals(target_);

                if (or(diff -> Math.abs(diff) > 1, from.getX() - to.getX(), from.getY() - to.getY())) {
                    throw new IllegalArgumentException("Can only make one step at a time! " + from + " " + to);
                }

                Direction direction = from.subtractY(1).equals(to) ? Direction.NORTH
                        : from.addX(1).equals(to) ? Direction.EAST
                        : from.addY(1).equals(to) ? Direction.SOUTH
                        : Direction.WEST;
                hal_.move(direction);

                Logger.increment(getLocalName(), LoggableValue.MOVED);
                if (atTarget_) {
                    Logger.set(getLocalName(), LoggableValue.EPOCHS, currentEpoch_);
                }
            }
        }
    }

    private class CoDyAgentDiscoveryBehaviour extends OneShotBehaviour {

        CoDyAgentDiscoveryBehaviour(@Nonnull Agent agent) {
            super(agent);
        }

        @Override
        public void action() {
            DFAgentDescription agentDescription = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(ServiceDescriptionType.CODY_AGENT.toString());
            agentDescription.addServices(sd);

            try {
                // It's assumed that every CoDyAgent has all "Services" that's why we only look for ServiceDescriptionType.CODY_AGENT
                DFAgentDescription[] result = DFService.search(myAgent, agentDescription);
                otherAgents_.clear();
                otherAgents_.addAll(Arrays.stream(result)
                        .filter(dfDescription -> !dfDescription.getName().equals(getAID()))
                        .map(DFAgentDescription::getName).collect(Collectors.toList()));
            } catch (FIPAException e) {
                e.printStackTrace();
            }
        }
    }

    private class MessageReceiveBehaviour extends CyclicBehaviour {
        private final @Nonnull
        MessageTemplate messageTemplate_;

        MessageReceiveBehaviour(@Nonnull Agent agent) {
            super(agent);
            messageTemplate_ = MessageTemplate.MatchConversationId(ServiceDescriptionType.PLANNING.toString());
        }

        @Override
        public void action() {
            ACLMessage aclMessage = receive(messageTemplate_);
            while (aclMessage != null) {
                try { // TODO remove try catch after fixing bug
                    PlanningMessage receivedMessage =
                            new PlanningMessage(JSONUtils.loadJSON(aclMessage.getContent()));
                    String agentID = receivedMessage.getAgentID();

                    PlanningMessage cachedMessage = planningMessages_.stream()
                            .filter(message -> message.getAgentID().equals(agentID))
                            .findFirst().orElse(null);

                    if (cachedMessage == null || cachedMessage.getTimestampStarted() < receivedMessage.getTimestampStarted()) {
                        if (cachedMessage != null) {
                            nearAgents_.remove(agentID);
                            planningMessages_.remove(cachedMessage);
                        }
                        Map<Integer, Point> otherSpaceTimePath = receivedMessage.getSpaceTimePath();
                        int refEpoch = otherSpaceTimePath.containsKey(currentEpoch_) ? currentEpoch_ : currentEpoch_ + 1;

                        if (overlap(plannedMovements_.get(refEpoch), otherSpaceTimePath.get(refEpoch), agentParameter_.getLocalMapDimensions())) {
                            nearAgents_.put(receivedMessage.getAgentID(), receivedMessage.getTimestampStarted());
                            planningMessages_.add(receivedMessage);
                        }
                    }
                    updateAgentsToWaitFor();
                } catch (JSONException e1) {
                    // TODO do smth about it
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Invalid JSON\n" + aclMessage);
                }

                aclMessage = receive(messageTemplate_);
            }
            block();
        }
    }

    private @Nonnull
    List<String> agentsToWaitFor_ = new ArrayList<>();

    private void updateAgentsToWaitFor() {
        agentsToWaitFor_.clear();
        nearAgents_.forEach((otherID, otherTStarted) -> {
            if (otherTStarted < tStarted_ || (otherTStarted == tStarted_) && Integer.parseInt(otherID) < Integer.parseInt(getLocalName())) {
                agentsToWaitFor_.add(otherID);
            }
        });
    }

    private boolean overlap(@Nonnull Point center0, @Nonnull Point center1, int dimensions) {
        Point diff = center0.subtract(center1);
        return and(coordinate -> Math.abs(coordinate) <= dimensions + 1, diff.getX(), diff.getY());
    }

    private class PlanningBehaviour extends CyclicBehaviour { // TODO rename
        private @Nonnull
        LocalMap localMap_;
        private @Nonnull
        Point prevBlockPos_;
        private int lastEpoch_;
        private int priority_ = agentParameter_.getPrioBase();
        private int step_ = 0;

        PlanningBehaviour(@Nonnull Agent agent) {
            super(agent);
        }

        @Override
        public void onStart() {
            tStarted_ = getCurrentTime();
            Point currentPos = hal_.getCurrentPos();
            int tMax = agentParameter_.getLocalMapTMax();
            int dimensions = agentParameter_.getLocalMapDimensions();

            localMap_ = new LocalMap(currentEpoch_, tMax, dimensions, currentPos, getLocalName(), distanceMap_);
            plannedMovements_.clear();
            plannedMovements_.putAll(range(currentEpoch_, 1, tMax).boxed().collect(Collectors.toMap(epoch -> epoch, epoch -> currentPos)));

            broadcast();
            Logger.set(getLocalName(), LoggableValue.DIST_START_TARGET, distanceMap_.getCell(currentPos).getDistance().get());
        }

        private void broadcast() {
            PlanningMessage planningMessage = new PlanningMessage(getLocalName(), tStarted_, plannedMovements_.get(currentEpoch_), priority_, plannedMovements_);
            ACLMessage message = new ACLMessage(ACLMessage.INFORM);
            message.setConversationId(ServiceDescriptionType.PLANNING.toString());
            message.setContent(planningMessage.tokenize().toString(2));
            otherAgents_.forEach(message::addReceiver);

            Logger.increment(getLocalName(), LoggableValue.MESSAGES_SEND);
            send(message);
        }

        @Override
        public void action() {
            switch (step_) {
                case 0:
                    if (currentEpoch_ > lastEpoch_ && agentsToWaitFor_.isEmpty()) { // TODO timeout
                        lastEpoch_ = currentEpoch_;
                        step_++;
                    }

                    break;
                case 1:
                    tStarted_ = getCurrentTime();

                    updateAgentsToWaitFor();
                    Point currentPos = plannedMovements_.get(currentEpoch_);

                    // 2# Center the LocalMap to the new position and set the time line new
                    localMap_.recenter(currentPos, currentEpoch_);

                    // 3# Update SpaceTimePath reservations
                    List<String> waitingAgents = new ArrayList<>();
                    planningMessages_.forEach(message -> {
                        String agentID = message.getAgentID();
                        localMap_.remove(agentID);
                        localMap_.addPriority(agentID, message.getAgentPriority());
                        if (message.getSpaceTimePath().containsKey(currentEpoch_ - 1)) {
                            waitingAgents.add(agentID);
                        }
                    });

                    planningMessages_.forEach(message ->
                            message.getSpaceTimePath().forEach((epoch, pos) -> localMap_.occupy(epoch, message.getAgentID(), pos)));

                    // 4# Complete incomplete paths
                    // Causes Problem? Not really necessary because we always wait for all agents and always receive all messages
                    // In the simulation at least
//                    localMap_.completePaths();

                    // 5# Build new own SpaceTimePath
                    addToPriority(-agentParameter_.getPrioNoBlock());
                    Pair<Boolean, Optional<Map<Integer, Point>>> result = localMap_.calcPath(priority_, waitingAgents);
                    boolean isBlocked = result.getKey();
                    Optional<Map<Integer, Point>> spaceTimePath = result.getValue();
                    LoggableValue blockType = null;

                    if (isBlocked) {
                        if (!spaceTimePath.isPresent()) {
                            // Full Block
                            blockType = LoggableValue.FULL_BLOCK;
                            Logger.increment(getLocalName(), LoggableValue.FULL_BLOCK);
                            addToPriority(agentParameter_.getPrioFullBlock());
                            spaceTimePath = localMap_.calcPath(priority_, waitingAgents).getValue();
                        } else {
                            List<Integer> plannedEpochs = spaceTimePath.get().keySet().stream().sorted().collect(Collectors.toList());
                            Point blockPos = spaceTimePath.get().get(plannedEpochs.get(plannedEpochs.size() - 1));

                            if (!blockPos.equals(target_) && blockPos.equals(prevBlockPos_)) {
                                // Block
                                blockType = LoggableValue.BLOCK;
                                Logger.increment(getLocalName(), LoggableValue.BLOCK);
                                addToPriority(agentParameter_.getPrioBlock());
                                spaceTimePath = localMap_.calcPath(priority_, waitingAgents).getValue();
                            } else if (!blockPos.equals(target_) && priority_ < agentParameter_.getPrioMax() - 1) {
                                // Block Suspicion
                                addToPriority(agentParameter_.getPrioNoBlock());
                                spaceTimePath = localMap_.calcPath(priority_, waitingAgents).getValue();
                            }
                            prevBlockPos_ = blockPos;
                        }
                    }

                    if (!spaceTimePath.isPresent()) {
                        blockType = LoggableValue.FULL_BLOCK_EMERGENCY;
                        priority_ = (int) Math.min(agentParameter_.getPrioMax(),
                                planningMessages_.stream().map(PlanningMessage::getAgentPriority)
                                        .max(Integer::compareTo).get() + (1.5 * agentParameter_.getPrioFullBlock()));

                        spaceTimePath = Optional.of(range(currentEpoch_, 1, agentParameter_.getLocalMapTMax()).boxed()
                                .collect(Collectors.toMap(epoch -> epoch, epoch -> currentPos)));
                    }

                    if (blockType != null) {
                        Logger.increment(getLocalName(), blockType);
                    }

                    plannedMovements_.clear();
                    plannedMovements_.putAll(spaceTimePath.get());

                    // 6# Let everybody know about the new "plan"
                    broadcast();

                    step_ = 0;
                    break;
            }
        }

        private void addToPriority(int addend) {
            priority_ += addend;
            if (priority_ > agentParameter_.getPrioMax()) {
                Logger.increment(getLocalName(), LoggableValue.PRIO_OVERFLOW);
                priority_ = ThreadLocalRandom.current().nextInt(
                        agentParameter_.getPrioFallbackMin(),
                        agentParameter_.getPrioFallbackMax());
            } else if (priority_ < agentParameter_.getPrioBase()) {
                priority_ = agentParameter_.getPrioBase();
            }
            Logger.logPriority(getLocalName(), currentEpoch_, priority_);
        }
    }

    @Override
    public String toString() {
        return "CoDyAgent" + getLocalName() + " currentPos " + plannedMovements_.get(currentEpoch_) + " target " + target_;
    }
}