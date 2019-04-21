package codyAgent.grid;

import helper.Point;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.stream.Stream;

import static helper.Methods.range;

class Grid {
    private final @Nonnull
    Point origin_;
    private final @Nonnull
    Integer dimensions_;
    private final @Nonnull
    Cell[][] cells_;


    Grid(@Nonnull Integer dimensions) {
        this(dimensions, new Point(0, 0));
    }

    Grid(@Nonnull Integer dimensions, @Nonnull Point origin) {
        dimensions_ = dimensions;
        origin_ = origin;
        cells_ = new Cell[dimensions_][dimensions_];
        cellIndexStream().forEach(point -> setCell(point, new Cell()));
        setCellNeighbours();
    }

    private void setCellNeighbours() {
        cellIndexStream().forEach(point -> {
            Point northP = new Point(point.getX(), point.getY() - 1);
            Point eastP = new Point(point.getX() + 1, point.getY());
            Point southP = new Point(point.getX(), point.getY() + 1);
            Point westP = new Point(point.getX() - 1, point.getY());

            getCell(point).setNeighbours(
                    inBounds(northP) ? getCell(northP) : null,
                    inBounds(eastP) ? getCell(eastP) : null,
                    inBounds(southP) ? getCell(southP) : null,
                    inBounds(westP) ? getCell(westP) : null);
        });
    }

    private @Nonnull
    Stream<Integer> dimensionStream() {
        return range(dimensions_).boxed();
    }

    @Nonnull
    Stream<Cell> cellStream() {
        return Arrays.stream(cells_).flatMap(Arrays::stream);
    }

    @Nonnull
    Stream<Point> cellIndexStream() {
        return dimensionStream().flatMap(x -> dimensionStream().map(y -> new Point(x, y).add(origin_)));
    }

    boolean inBounds(@Nonnull Point point) {
        return point.getX() >= origin_.getX() && point.getY() >= origin_.getY()
                && point.getX() < dimensions_ + origin_.getX() && point.getY() < dimensions_ + origin_.getY();
    }

    @Nonnull
    public Cell getCell(@Nonnull Point point) {
        Point index = point.subtract(origin_);

        return cells_[index.getX()][index.getY()];
    }

    void setCell(@Nonnull Point point, @Nonnull Cell cell) {
        Point index = point.subtract(origin_);
        cells_[index.getX()][index.getY()] = cell;
    }

    public @Nonnull
    Integer getDimensions() {
        return dimensions_;
    }

    public @Nonnull
    Point getOrigin() {
        return origin_;
    }

    @Override
    public @Nonnull
    String toString() {
        StringBuilder result = new StringBuilder("    ");
        dimensionStream().forEach((x) -> result.append(String.format(" x%02d ", x + origin_.getX())));

        dimensionStream().forEach((y) -> {
            result.append(String.format("\ny%02d ", y + origin_.getY()));
            dimensionStream().forEach((x) -> result.append(getCell(new Point(x, y).add(origin_))));
        });

        return result.toString();
    }
}
