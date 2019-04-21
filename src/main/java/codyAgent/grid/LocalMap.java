package codyAgent.grid;

import codyAgent.CoDyAgent;
import helper.Point;
import helper.Wrapper;
import javafx.util.Pair;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static helper.Methods.getLast;
import static helper.Methods.range;

public class LocalMap {
    private int currentEpoch_;
    private final int tMax_;

    private final int dimensions_;

    private @Nonnull
    Point agentPos_;
    private @Nonnull
    String agentID_;
    private @Nonnull
    Point origin_;

    private final @Nonnull
    Map<String, Integer> priorities_ = new HashMap<>();
    private final @Nonnull
    Map<Integer, LocalMapLayer> layers_ = new HashMap<>();
    private final @Nonnull
    List<Integer> epochs_ = new ArrayList<>();

    private final @Nonnull
    Set<Point> staticObstacles_ = new HashSet<>();
    private final @Nonnull
    DistanceMap distanceMap_;


    public LocalMap(int epoch, int tMax, int dimensions, @Nonnull Point agentPos, @Nonnull String agentID, @Nonnull DistanceMap distanceMap) {
        currentEpoch_ = epoch;
        tMax_ = tMax;
        dimensions_ = (dimensions / 2) * 2 + 1; // Dimensions must be uneven
        agentPos_ = agentPos;
        agentID_ = agentID;
        origin_ = agentPos_.subtractX(dimensions_ / 2).subtractY(dimensions_ / 2);
        distanceMap_ = distanceMap;

        clearLayers();
    }

    private void clearLayers() {
        staticObstacles_.clear();
        staticObstacles_.addAll(distanceMap_.getStaticObstacles());
        staticObstacles_.addAll(new Grid(dimensions_, origin_).cellIndexStream()
                .filter(cellIndex -> !distanceMap_.inBounds(cellIndex))
                .collect(Collectors.toList()));

        layers_.clear();
        epochs_.clear();
        range(currentEpoch_, 1, tMax_).forEach(epoch -> {
            epochs_.add(epoch);
            layers_.put(epoch, new LocalMapLayer());
        });
    }

    private @Nonnull
    Map<Integer, Map<String, Point>> getSpaceTimePaths() {
        return layers_.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> getSpaceTimePaths(entry.getKey())
        ));
    }

    private @Nonnull
    Map<String, Point> getSpaceTimePaths(Integer epoch) {
        return layers_.get(epoch).getAgentIDs().stream().collect(Collectors.toMap(
                agentID -> agentID,
                agentID -> getSpaceTimePath(agentID).get(epoch)
        ));
    }

    public void remove(@Nonnull String agentID) {
        layers_.values().forEach(layer -> layer.remove(agentID));
        priorities_.remove(agentID);
    }

    public void occupy(int epoch, @Nonnull String agentID, @Nonnull Point pos) {
        if (agentID_.equals(agentID)) {
            throw new IllegalArgumentException("Can not add agent " + agentID + " to its own LocalMap.");
        }
        LocalMapLayer layer = layers_.get(epoch);
        if (layer != null) {
            layer.occupy(agentID, pos);
        }
    }


    /**
     * @return the priority of the cell if occupied by some agent.
     * <br>Integer.MAX_VALUE if the cell is a static obstacle.
     * <br>-1 if the cell is free.
     */
    private int getPriority(int epoch, @Nonnull Point pos) {
        return staticObstacles_.contains(pos) || !layers_.get(epoch).inBounds(pos)
                ? Integer.MAX_VALUE
                : layers_.get(epoch).getCell(pos).getOccupant().map(priorities_::get).orElse(-1);
    }

    private @Nonnull
    Map<Integer, Point> getSpaceTimePath(@Nonnull String agentID) {
        return layers_.entrySet().stream()
                .filter(entry -> entry.getValue().getOccupiedCellIndex(agentID).isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getOccupiedCellIndex(agentID).get()));
    }

    public void recenter(@Nonnull Point agentPos, int currentEpoch) {
        agentPos_ = agentPos;
        currentEpoch_ = currentEpoch;
        origin_ = agentPos_.subtractX(dimensions_ / 2).subtractY(dimensions_ / 2);

        Map<Integer, Map<String, Point>> oldPaths = getSpaceTimePaths();

        clearLayers();

        oldPaths.forEach((epoch, spacePaths) ->
                spacePaths.forEach((agentID, positions) -> occupy(epoch, agentID, positions)));
    }

    public Pair<Boolean, Optional<Map<Integer, Point>>> calcPath(int priority, @Nonnull List<String> agentsWaiting) {
        ReachabilityMap reachabilityMap = new ReachabilityMap();
        reachabilityMap.calcSteps(priority, agentsWaiting);
        Optional<Map<Integer, Point>> path = reachabilityMap.chooseOptimalPosition().map(reachabilityMap::choosePath);

        boolean isBlocked = !path.isPresent();
        if (!isBlocked) {
            int epoch0 = epochs_.get(epochs_.size() - 2);
            int epoch1 = epochs_.get(epochs_.size() - 3);

            int setp0 = reachabilityMap.getLayer(epoch0)
                    .getStep(path.get().get(epoch0));
            int step1 = reachabilityMap.getLayer(epoch1)
                    .getStep(path.get().get(epoch0));

            isBlocked = step1 > 0 && setp0 > 0;
        }

        return new Pair<>(isBlocked, path);
    }

    private boolean isComplete(Map<Integer, Point> path) {
        List<Integer> pathEpochs = path.keySet().stream().sorted().collect(Collectors.toList());

        Function<Point, Boolean> endsAtEdge = pos -> pos.getX() == origin_.getX()
                || pos.getY() == origin_.getY()
                || pos.getX() == origin_.addX(dimensions_).getX()
                || pos.getY() == origin_.addY(dimensions_).getY();

        return pathEpochs.equals(epochs_) || endsAtEdge.apply(path.get(getLast(pathEpochs).get()));
    }

    private void completePath(@Nonnull Map<Integer, Point> incompletePath) {
        int lastValid = incompletePath.keySet().stream().max(Integer::compareTo).get();

        epochs_.stream()
                .filter(epoch -> epoch > lastValid)
                .forEach(epoch -> incompletePath.put(epoch, incompletePath.get(lastValid)));
    }

    /**
     * TODO fix
     * Does not work fully.
     * When a path from some agent is interrupted by some other with higher priority
     * the is Complete returns true quite early and so after the first few episodes
     * every agent stands still.
     * This causes problems when this localMap calculates its path and the agent send the data
     * to other agents. It can, and often will, happen that then the calculated path overrides
     * cells that he shouldn't be able to (priority wise), causing an Cell not free error for the
     * next agent trying to occupy the calculated path
     */
    public void completePaths() {
        Map<Integer, Map<String, Point>> completedPaths = new HashMap<>();
        epochs_.forEach(epoch -> completedPaths.put(epoch, new HashMap<>()));

        List<String> agentsWithoutPath = new ArrayList<>();
        priorities_.keySet().forEach(agentID -> {
            Map<Integer, Point> path = getSpaceTimePath(agentID);
            if (path.isEmpty()) {
                agentsWithoutPath.add(agentID);
            } else {
                if (!isComplete(path)) {
                    completePath(path);
                }
                path.forEach((epoch, pos) -> completedPaths.get(epoch).put(agentID, pos));
            }
        });
        agentsWithoutPath.forEach(priorities_::remove);

        clearLayers();
        Map<String, Point> fallbackPaths = null;

        for (int epoch = currentEpoch_; epoch < currentEpoch_ + tMax_; epoch++) {
            LocalMapLayer layer = layers_.get(epoch);

            if (fallbackPaths != null) {
                fallbackPaths.forEach(layer::occupy);
            } else {
                for (Map.Entry<String, Point> entry : completedPaths.get(epoch).entrySet()) {
                    try {
                        layer.occupy(entry.getKey(), entry.getValue());
                    } catch (IllegalStateException e) {
                        layer.getAgentIDs().forEach(layer::remove);
                        fallbackPaths = completedPaths.get(--epoch);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        epochs_.forEach(
                epoch -> result.append("\n\n").append(epoch).append(":\n").append(layers_.get(epoch)));

        return result.toString();
    }

    public void addPriority(@Nonnull String otherID, int senderPriority) {
        priorities_.put(otherID, senderPriority);
    }

    private class LocalMapLayer extends Grid {
        LocalMapLayer() {
            super(dimensions_, origin_);

            staticObstacles_.stream().filter(this::inBounds).forEach(obstacle -> {
                Cell cell = getCell(obstacle);
                if (cell.getCellStatus() != CellStatus.FREE) {
                    throw new IllegalStateException("Cannot set " + obstacle + " as static obstacle because its not free.\n" + cell.getCellStatus());
                }
                cell.setCellStatus(CellStatus.STATIC_OBSTACLE, null);
            });
        }

        void remove(@Nonnull String agentID) {
            cellStream()
                    .filter(cell -> agentID.equals(cell.getOccupant().orElse(null)))
                    .forEach(cell -> cell.setCellStatus(CellStatus.FREE, null));
        }

        void occupy(@Nonnull String agentID, @Nonnull Point pos) {
            if (inBounds(pos)) {
                Cell cell = getCell(pos);
                int cellPrio = cell.getOccupant().map(priorities_::get).orElse(Integer.MAX_VALUE);
                int agentPrio = priorities_.get(agentID);

                if (cell.getCellStatus() == CellStatus.STATIC_OBSTACLE || cellPrio == agentPrio) {
                    throw new IllegalStateException(new Date().getTime() - CoDyAgent.CREATION_TIME
                            + " Cannot occupy " + pos + " for agent " + agentID + " because its already occupied by agent " + cell.getOccupant().get());
                } else if (cell.getCellStatus() == CellStatus.FREE || agentPrio > cellPrio) {
                    cell.setCellStatus(CellStatus.DYNAMIC_OBSTACLE, agentID);
                }
            }
        }

        @Nonnull
        Optional<Point> getOccupiedCellIndex(@Nonnull String agentID) {
            return cellIndexStream()
                    .filter(index -> agentID.equals(getCell(index).getOccupant().orElse(null)))
                    .findFirst();
        }

        @Nonnull
        List<String> getAgentIDs() {
            return cellStream()
                    .map(cell -> cell.getOccupant().orElse(null))
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        }
    }

    private class ReachabilityMap {
        @Nonnull
        Map<Integer, ReachabilityMapLayer> reachabilityMapLayers_ = new HashMap<>();

        ReachabilityMap() {
            layers_.keySet().forEach(
                    epoch -> reachabilityMapLayers_.put(epoch, new ReachabilityMapLayer()));
        }

        @Nonnull
        ReachabilityMapLayer getLayer(int epoch) {
            return reachabilityMapLayers_.get(epoch);
        }

        @Nonnull
        Optional<Point> chooseOptimalPosition() {
            Point result = null;
            ReachabilityMapLayer tMaxLayer = getLayer(getLast(epochs_).get());
            List<Point> minDistPos = new ArrayList<>();

            Optional<Integer> minDist = tMaxLayer.cellIndexStream().filter(pos -> tMaxLayer.getStep(pos) > 0)
                    .map(pos -> distanceMap_.getCell(pos).getDistance().get()).min(Integer::compareTo);
            minDist.ifPresent(min ->
                    minDistPos.addAll(tMaxLayer.cellIndexStream()
                            .filter(pos -> tMaxLayer.getStep(pos) > 0 && distanceMap_.getCell(pos).getDistance().get().equals(min))
                            .collect(Collectors.toList())));
            if (!minDistPos.isEmpty()) { // TODO how to handle multiple possibilities?
                result = minDistPos.get(0);
            }

            return Optional.ofNullable(result);
        }

        @Nonnull
        Map<Integer, Point> choosePath(@Nonnull Point targetPos) {
            Map<Integer, Point> result = new HashMap<>();

            Wrapper<Point> prevPos = new Wrapper<>(targetPos);
            result.put(getLast(epochs_).get(), targetPos);

            epochs_.stream().sorted(Collections.reverseOrder())
                    .filter(epoch -> epoch < getLast(epochs_).get())
                    .forEach(epoch -> {
                        ReachabilityMapLayer layer = getLayer(epoch);
                        if (layer.getStep(prevPos.get()) > 0) {
                            // 3.8.3.1 Prefer waiting at the end
                            result.put(epoch, prevPos.get());
                        } else {
                            // Sorted "clockwise"
                            Point[] neighbours = new Point[]{prevPos.get().subtractY(1), prevPos.get().addX(1), prevPos.get().addY(1), prevPos.get().subtractX(1)};
                            // The neighbour with the smallest euclidean distance to the target is the "front"
                            int frontIndex = range(neighbours.length).boxed().min((i, i2) -> (int) (neighbours[i].distance(targetPos) - neighbours[i2].distance(targetPos))).get();

                            Map<String, Point> possibleDirections = new HashMap<String, Point>() {{
                                BiConsumer<String, Point> putConditionally = (key, pos) -> {
                                    if (layer.getStep(pos) > 0) {
                                        put(key, pos);
                                    }
                                };
                                putConditionally.accept("front", neighbours[frontIndex % neighbours.length]);
                                putConditionally.accept("right", neighbours[(frontIndex + 1) % neighbours.length]);
                                putConditionally.accept("back", neighbours[(frontIndex + 2) % neighbours.length]);
                                putConditionally.accept("left", neighbours[(frontIndex + 3) % neighbours.length]);
                            }};

                            if (!possibleDirections.isEmpty()) {
                                // 3.8.3.3 Prefer free cells
                                int minStep = possibleDirections.values().stream().map(layer::getStep).min(Integer::compareTo).get();
                                possibleDirections = possibleDirections.entrySet().stream().filter(entry -> layer.getStep(entry.getValue()) == minStep)
                                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                                int minPrio = possibleDirections.values().stream().map(pos -> getPriority(epoch, pos)).min(Integer::compareTo).get();
                                Set<String> minPrioPositions = possibleDirections.entrySet().stream()
                                        .filter(entry -> getPriority(epoch, entry.getValue()) == minPrio)
                                        .map(Map.Entry::getKey).collect(Collectors.toSet());

                                // 3.8.3.2 Right before left
                                Point next;
                                if (minPrioPositions.contains("right")) {
                                    next = possibleDirections.get("right");
                                } else if (minPrioPositions.contains("front")) {
                                    next = possibleDirections.get("front");
                                } else if (minPrioPositions.contains("left")) {
                                    next = possibleDirections.get("left");
                                } else {
                                    next = possibleDirections.get("back");
                                }

                                prevPos.set(next);
                                result.put(epoch, next);
                            }
                        }
                    });

            if (!result.get(currentEpoch_).equals(agentPos_)) {
                throw new IllegalStateException("Path finding algorithm failed. Start position " + result.get(currentEpoch_) + " doesn't match the agents position " + agentPos_);
            }

            return result;
        }

        void calcSteps(int priority, @Nonnull List<String> agentsWaiting) {
            reachabilityMapLayers_.get(currentEpoch_).setStep(agentPos_, 1);

            BiFunction<Integer, Point, Integer> getPriorityWithSafeZone = (epoch, pos) -> {
                int prio = getPriority(epoch, pos);
                String occupant = layers_.get(epoch).getCell(pos).getOccupant().orElse("");
                int safeZone = agentsWaiting.contains(occupant) ? 1 : 2;

                return prio == -1 ? prio : (epoch <= currentEpoch_ + safeZone ? Integer.MAX_VALUE : prio);
            };

            epochs_.stream().filter(epoch -> epoch > currentEpoch_).forEach(epoch -> {
                ReachabilityMapLayer rLayer = reachabilityMapLayers_.get(epoch);
                ReachabilityMapLayer prevRLayer = reachabilityMapLayers_.get(epoch - 1);

                BiConsumer<Point, Point> setSteps = (pos, pos2) -> {
                    if (rLayer.inBounds(pos2) && getPriorityWithSafeZone.apply(epoch, pos) < priority
                            && getPriorityWithSafeZone.apply(epoch, pos2) < priority && prevRLayer.getStep(pos2) > 0) {
                        setStep(rLayer, pos, rLayer.getStep(pos), prevRLayer.getStep(pos2) + 1);
                        setStep(rLayer, pos2, rLayer.getStep(pos2), prevRLayer.getStep(pos2));
                    }
                };

                rLayer.cellIndexStream().forEach(pos -> {
                    if (getPriorityWithSafeZone.apply(epoch, pos) < priority && prevRLayer.getStep(pos) > 0) {
                        setStep(rLayer, pos, rLayer.getStep(pos), prevRLayer.getStep(pos));
                    }
                    if (getPriorityWithSafeZone.apply(epoch - 1, pos) < priority) {
                        setSteps.accept(pos, pos.subtractX(1));
                        setSteps.accept(pos, pos.addX(1));
                        setSteps.accept(pos, pos.subtractY(1));
                        setSteps.accept(pos, pos.addY(1));
                    }
                });
            });
        }

        private void setStep(@Nonnull ReachabilityMapLayer layer, @Nonnull Point pos, int... steps) {
            int minStep = Arrays.stream(steps).filter(step -> step > 0).min().getAsInt();
            layer.setStep(pos, minStep);
        }

        private class ReachabilityMapLayer extends Grid {
            ReachabilityMapLayer() {
                super(dimensions_, origin_);
            }

            int getStep(@Nonnull Point pos) {
                return inBounds(pos) ? getCell(pos).getDistance().orElse(0) : 0;
            }

            void setStep(@Nonnull Point pos, int step) {
                getCell(pos).setDistance(step);
            }
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            epochs_.forEach(
                    epoch -> result.append("\n\n").append(epoch).append(":\n").append(reachabilityMapLayers_.get(epoch)));

            return result.toString();
        }
    }
}
