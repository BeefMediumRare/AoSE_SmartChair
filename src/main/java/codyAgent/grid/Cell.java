package codyAgent.grid;

import helper.Direction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class Cell {
    private @Nonnull
    CellStatus cellStatus_ = CellStatus.FREE;
    private final @Nonnull
    Map<Direction, Cell> neighbours_ = new HashMap<>();
    private @Nullable
    Integer distance_;
    private @Nullable
    String occupantID_;

    @Nonnull
    Optional<String> getOccupant() {
        return Optional.ofNullable(occupantID_);
    }

    void setNeighbours(@Nullable Cell northNeighbour, @Nullable Cell eastNeighbour, @Nullable Cell southNeighbour, @Nullable Cell westNeighbour) {
        neighbours_.put(Direction.NORTH, northNeighbour);
        neighbours_.put(Direction.EAST, eastNeighbour);
        neighbours_.put(Direction.SOUTH, southNeighbour);
        neighbours_.put(Direction.WEST, westNeighbour);
    }

    @Nonnull
    List<Cell> getNeighboursAsList() {
        return new ArrayList<>(neighbours_.values()).stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    void setDistance(@Nullable Integer distance) {
        distance_ = distance;
    }

    @Nonnull
    public Optional<Integer> getDistance() {
        return Optional.ofNullable(distance_);
    }

    void setCellStatus(@Nonnull CellStatus cellStatus, @Nullable String occupantID) {
        cellStatus_ = cellStatus;
        occupantID_ = occupantID;
    }

    @Nonnull
    CellStatus getCellStatus() {
        return cellStatus_;
    }


    @Override
    public @Nonnull
    String toString() {
        String result;
        switch (cellStatus_) {
            case FREE:
                result = distance_ != null ? (distance_ > 0 ? String.format("%03d", distance_) : " T ") : "   ";
                break;
            case STATIC_OBSTACLE:
                result = "---";
                break;
            case DYNAMIC_OBSTACLE:
                result = " " + getOccupant().orElse("?") + " ";
                break;
            default:
                result = "";
        }
        return "|" + result + "|";
    }
}
