package helper;

import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Objects;

public class Point {
    private int x_;
    private int y_;

    public Point(@Nonnull Point point) {
        this(point.getX(), point.getY());
    }

    public Point(int x, int y) {
        x_ = x;
        y_ = y;
    }

    public Point(@Nonnull JSONObject pointJSON) {
        // TODO validate
        x_ = pointJSON.getInt("x");
        y_ = pointJSON.getInt("y");
    }

    public @Nonnull
    JSONObject tokenize() {
        return new JSONObject(new HashMap<String, Object>() {{
            put("x", x_);
            put("y", y_);
        }});
    }

    public @Nonnull
    Point addX(int addend) {
        return new Point(x_ + addend, y_);
    }

    public @Nonnull
    Point addY(int addend) {
        return new Point(x_, y_ + addend);
    }

    public @Nonnull
    Point add(@Nonnull Point other) {
        return new Point(x_ + other.getX(), y_ + other.getY());
    }

    public @Nonnull
    Point subtractX(int subtrahend) {
        return new Point(x_ - subtrahend, y_);
    }

    public @Nonnull
    Point subtractY(int subtrahend) {
        return new Point(x_, y_ - subtrahend);
    }

    public @Nonnull
    Point subtract(@Nonnull Point other) {
        return new Point(x_ - other.getX(), y_ - other.getY());
    }

    public int getX() {
        return x_;
    }

    public int getY() {
        return y_;
    }

    public void set(int x, int y) {
        x_ = x;
        y_ = y;
    }

    public double distance(@Nonnull Point other){
        return Math.sqrt(Math.pow(x_ - other.x_, 2) + Math.pow(y_ - other.y_, 2));
    }

    @Override
    public String toString() {
        return "(" + x_ + ", " + y_ + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return x_ == point.x_ &&
                y_ == point.y_;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x_, y_);
    }
}
