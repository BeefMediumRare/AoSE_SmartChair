package codyAgent.grid;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Optional;

public enum CellStatus {
    FREE, STATIC_OBSTACLE, DYNAMIC_OBSTACLE;

    public static @Nonnull
    Optional<CellStatus> fromString(String representation) {
        return Arrays.stream(values()).filter(cellStatus -> representation.toLowerCase().equals(cellStatus.toString().toLowerCase())).findFirst();
    }
}
