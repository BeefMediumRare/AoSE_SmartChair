package codyAgent;

import codyAgent.grid.DistanceMap;
import codyAgent.grid.LocalMap;
import helper.Point;
import javafx.util.Pair;

import java.util.*;

public class CoDyTestStartUp {

    public static void main(String[] args) {

        Point target = new Point(0, 0);
        List<Point> staticObstacles = Arrays.asList(
                new Point(0,1), new Point(1, 1), new Point(2, 1),
                new Point(0,2), new Point(1, 2),
                new Point(0,3),
                new Point(0,4));

        DistanceMap distanceMap = new DistanceMap(5, staticObstacles);
        distanceMap.calcDistances(target);

        LocalMap localMap = new LocalMap(1, 3, 5, new Point(2,2), "0", distanceMap);

        Map<Integer, Point> a1Path = new HashMap<Integer, Point>() {{
            put(1, new Point(3,2));
            put(2, new Point(3,3));
            put(3, new Point(2, 3));
        }};

        localMap.addPriority("1", 0);
        a1Path.forEach((t, p) -> localMap.occupy(t, "1", p));

//        localMap.completePaths();

        Pair<Boolean, Optional<Map<Integer, Point>>> calcedPath = localMap.calcPath(0, new ArrayList<>());

        System.out.println(calcedPath);
        System.out.println(localMap);
        System.out.println("##########################");
        calcedPath = localMap.calcPath(398, Arrays.asList("1"));
        System.out.println(calcedPath);
        System.out.println(localMap);


    }
}
