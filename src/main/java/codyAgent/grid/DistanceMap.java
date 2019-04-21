package codyAgent.grid;

import helper.Point;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

public class DistanceMap extends Grid {
    private final @Nonnull
    List<Point> staticObstacles_;

    public DistanceMap(@Nonnull Integer dimensions) {
        this(dimensions, new ArrayList<>());
    }

    public DistanceMap(@Nonnull Integer dimensions, @Nonnull List<Point> staticObstacles) {
        super(dimensions);
        staticObstacles_ = new ArrayList<>(staticObstacles);
        staticObstacles_.forEach(obstacle -> getCell(obstacle).setCellStatus(CellStatus.STATIC_OBSTACLE, null));
    }

    public void clearDistances() {
        cellStream().forEach(cell -> cell.setDistance(null));
    }

    public void calcDistances(@Nonnull Point target) {
        if (getCell(target).getCellStatus() != CellStatus.FREE) {
            throw new IllegalArgumentException("The target " + target + " cells status must be FREE but is " + getCell(target).getCellStatus());
        }
        clearDistances();

        getCell(target).setDistance(0);
        Stack<Cell> toVisit = new Stack<>();
        toVisit.push(getCell(target));

        while (!toVisit.isEmpty()) {
            //the distance of closestCell is present
            toVisit.sort(Comparator.comparing(cell -> cell.getDistance().get(), Comparator.reverseOrder()));
            Cell closestCell = toVisit.pop();

            closestCell.getNeighboursAsList().stream().filter(
                    neighbour -> neighbour != null && neighbour.getCellStatus() == CellStatus.FREE && !neighbour.getDistance().isPresent())
                    .forEach((neighbour) -> {
                        //the distance of closestCell is present
                        neighbour.setDistance(closestCell.getDistance().get() + 1);
                        toVisit.push(neighbour);
                    });
        }
    }

    public @Nonnull
    List<Point> getStaticObstacles() {
        return staticObstacles_;
    }
}
