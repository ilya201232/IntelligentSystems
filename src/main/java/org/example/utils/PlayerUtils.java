package org.example.utils;

import lombok.extern.slf4j.Slf4j;
import org.example.exception.FailedToCalculateException;
import org.example.model.Knowledge;
import org.example.model.Perception;
import org.example.model.base.GameObject;
import org.example.model.object.Marker;
import org.example.model.object.Player;
import org.example.model.unit.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class PlayerUtils {

    public static Optional<Vector2> calcThisPlayerPosition(Perception perception, Knowledge knowledge) {

        if (!perception.isHasGotSeeInfo() || !perception.isHasGotSenseBodyInfo()) {
            log.debug("No enough data for position calculation...");
            return Optional.empty();
        }

        // Only markers with distance needed
        List<Marker> markersInView = new ArrayList<>(perception.getMarkersSaw().stream().filter(marker -> marker.getDistance() != null).toList());

        if (markersInView.size() < 3) {
            log.debug("Not enough markers in sight to calculate position.");
            return Optional.empty();
        }

        Marker marker1 = markersInView.getFirst();
        markersInView.removeFirst();

        Marker marker2 = markersInView.getFirst();
        markersInView.removeFirst();

        Optional<Marker> marker3 = markersInView.stream()
                .filter(marker -> {
                    double C1A = marker.getPosition().getDistance(marker1.getPosition());
                    double C1B = marker.getPosition().getDistance(marker2.getPosition());

                    double AB = marker1.getPosition().getDistance(marker2.getPosition());

                    return !((C1A + AB - C1B < 0.00001d) || (C1A + C1B - AB < 0.00001d) || (AB + C1B - C1A < 0.00001d));
                })
                .findAny();

        if (marker3.isEmpty()) {
            log.debug("There are enough markers in sight to calculate position, but they are in the straight line!.");
            return Optional.empty();
        }

        try {
            return Optional.of(calcPositionBy2MarkersAndOne(
                    marker1,
                    marker2,
                    marker3.get()
            ));
        } catch (FailedToCalculateException e) {
            log.error("Position calculation has failed...");
            return Optional.empty();
        }

    }

    public static Optional<Vector2> calcAnotherObjectPosition(Perception perception, Knowledge knowledge, GameObject object) {

        if (!perception.isHasGotSeeInfo() || !perception.isHasGotSenseBodyInfo()) {
            log.debug("No enough data for position calculation...");
            return Optional.empty();
        }

        Optional<Vector2> playerPosition = calcThisPlayerPosition(perception, knowledge);

        if (playerPosition.isEmpty()) {
            log.debug("Failed to calculate player position => can't calculate object position.");
            return Optional.empty();
        }

        // No need to check markers if player position has been successfully calculated
        List<Marker> markersInView = perception.getMarkersSaw().stream().filter(marker -> marker.getDistance() != null).toList();

        try {
            return Optional.of(calcPositionForAnotherObject(
                    markersInView.get(0),
                    markersInView.get(1),
                    playerPosition.get(),
                    object
            ));
        } catch (FailedToCalculateException e) {
            log.error("Position calculation has failed...");
            return Optional.empty();
        }
    }

    private static Vector2 calcPositionBy2MarkersAndOne(Marker marker1, Marker marker2, Marker marker3) throws FailedToCalculateException {

        int fieldX = 54;
        int fieldY = 32;

        Vector2 p1 = marker1.getPosition();
        Vector2 p2 = marker2.getPosition();

        double d1 = marker1.getDistance();
        double d2 = marker2.getDistance();

        // Check for equality of X or Y coordinates
        if (Math.abs(p1.getX() - p2.getX()) < 0.00001) {
            double y = (
                    Math.pow(p2.getY(), 2) - Math.pow(p1.getY(), 2) + Math.pow(d1, 2) - Math.pow(d2, 2)
            ) / (2 * (p2.getY() - p1.getY()));

            double sqrt = Math.sqrt(Math.abs(Math.pow(d1, 2) - Math.pow(y - p1.getY(), 2)));

            Vector2 position1 = new Vector2(0, y);
            Vector2 position2 = new Vector2(0, y);

            double x1 = p1.getX() - sqrt;
            double x2 = p1.getX() + sqrt;

            position1.setX(x1);
            position2.setX(x2);

            if (x1 < -fieldX || x1 > fieldX) {
                return position2;
            } else if (x2 < -fieldX || x2 > fieldX) {
                return position1;
            }

            Vector2 finalPos = null;
            double min = Double.MAX_VALUE;

            double newMin = calculateMistake(position1, marker3.getPosition(), marker3.getDistance());
            if (newMin < min && confirmAllDistances(position1, List.of(marker1, marker2, marker3))) {
                finalPos = position1;
                min = newMin;
            }

            newMin = calculateMistake(position2, marker3.getPosition(), marker3.getDistance());
            if (newMin < min && confirmAllDistances(position2, List.of(marker1, marker2, marker3))) {
                finalPos = position2;
            }

            if (finalPos == null) {
                throw new FailedToCalculateException();
            }

            return finalPos;
        } else if (Math.abs(p1.getY() - p2.getY()) < 0.00001) {
            double beta = (
                    Math.pow(p2.getY(), 2) - Math.pow(p1.getY(), 2) +
                            Math.pow(p2.getX(), 2) - Math.pow(p1.getX(), 2) +
                            Math.pow(d1, 2) - Math.pow(d2, 2)
            ) / (2 * (p2.getX() - p1.getX()));

            double c = Math.pow(p1.getX() - beta, 2) + Math.pow(p1.getY(), 2) - Math.pow(d1, 2);

            double x = (
                    Math.pow(p2.getX(), 2) - Math.pow(p1.getX(), 2) + Math.pow(d1, 2) - Math.pow(d2, 2)
            ) / (2 * (p2.getX() - p1.getX()));

            double sqrt = Math.sqrt(Math.abs(Math.pow(p1.getY(), 2) - c));
//            double sqrt1 = Math.sqrt(Math.pow(p1.getY(), 2) - c);

            Vector2 position1 = new Vector2(x, 0);
            Vector2 position2 = new Vector2(x, 0);

            double y1 = p1.getY() - sqrt;
            double y2 = p1.getY() + sqrt;

            position1.setY(y1);
            position2.setY(y2);

            if (y1 < -fieldY || y1 > fieldY) {
                return position2;
            } else if (y2 < -fieldY || y2 > fieldY) {
                return position1;
            }

            Vector2 finalPos = null;
            double min = Double.MAX_VALUE;

            double newMin = calculateMistake(position1, marker3.getPosition(), marker3.getDistance());
            if (newMin < min && confirmAllDistances(position1, List.of(marker1, marker2, marker3))) {
                finalPos = position1;
                min = newMin;
            }

            newMin = calculateMistake(position2, marker3.getPosition(), marker3.getDistance());
            if (newMin < min && confirmAllDistances(position2, List.of(marker1, marker2, marker3))) {
                finalPos = position2;
            }

            if (finalPos == null) {
                throw new FailedToCalculateException();
            }

            return finalPos;
        }

        double alpha = (p1.getY() - p2.getY()) / (p2.getX() - p1.getX());
        double beta = (
                Math.pow(p2.getY(), 2) - Math.pow(p1.getY(), 2) +
                        Math.pow(p2.getX(), 2) - Math.pow(p1.getX(), 2) +
                        Math.pow(d1, 2) - Math.pow(d2, 2)
        ) / (2 * (p2.getX() - p1.getX()));

        double a = Math.pow(alpha, 2) + 1;
        double b = -2 * (alpha * (p1.getX() - beta) + p1.getY());
        double c = Math.pow(p1.getX() - beta, 2) + Math.pow(p1.getY(), 2) - Math.pow(d1, 2);

        double sqrt = Math.sqrt(Math.abs(b * b - 4 * a * c));

        Vector2 position1 = new Vector2();
        Vector2 position2 = new Vector2();
        Vector2 position3 = new Vector2();
        Vector2 position4 = new Vector2();

        double y1 = (-b - sqrt) / (2 * a);
        double y2 = (-b + sqrt) / (2 * a);
        position1.setY(y1);
        position2.setY(y1);
        position3.setY(y2);
        position4.setY(y2);
        if (y1 < -fieldY || y1 > fieldY) {
            position1 = null;
            position2 = null;
        } else if (y2 < -fieldY || y2 > fieldY) {
            position3 = null;
            position4 = null;
        }

        sqrt = Math.sqrt(Math.abs(Math.pow(d1, 2) - Math.pow(y1 - p1.getY(), 2)));
        double x1 = p1.getX() - sqrt;
        double x2 = p1.getX() + sqrt;

        if (position1 != null) position1.setX(x1);
        if (position2 != null) position2.setX(x2);
        if (position3 != null) position3.setX(x1);
        if (position4 != null) position4.setX(x2);

        if (x1 < -fieldX || x1 > fieldX) {
            position1 = null;
            position3 = null;
        } else if (x2 < -fieldX || x2 > fieldX) {
            position2 = null;
            position4 = null;
        }

        Vector2 finalPos = null;
        double min = Double.MAX_VALUE;

        if (position1 != null) {
            double newMin = calculateMistake(position1, marker3.getPosition(), marker3.getDistance());
            if (newMin < min && confirmAllDistances(position1, List.of(marker1, marker2, marker3))) {
                finalPos = position1;
                min = newMin;
            }
        }

        if (position2 != null) {
            double newMin = calculateMistake(position2, marker3.getPosition(), marker3.getDistance());
            if (newMin < min && confirmAllDistances(position2, List.of(marker1, marker2, marker3))) {
                finalPos = position2;
                min = newMin;
            }
        }

        if (position3 != null) {
            double newMin = calculateMistake(position3, marker3.getPosition(), marker3.getDistance());
            if (newMin < min && confirmAllDistances(position3, List.of(marker1, marker2, marker3))) {
                finalPos = position3;
                min = newMin;
            }
        }

        if (position4 != null) {
            double newMin = calculateMistake(position4, marker3.getPosition(), marker3.getDistance());
            if (newMin < min && confirmAllDistances(position4, List.of(marker1, marker2, marker3))) {
                finalPos = position4;
            }
        }

        if (finalPos == null) {
            throw new FailedToCalculateException();
        }

        return finalPos;
    }

    private static double calculateMistake(Vector2 calcPosition, Vector2 markerPos, double distToMarker) {
        return Math.abs(Math.pow(calcPosition.getX() - markerPos.getX(), 2) + Math.pow(calcPosition.getY() - markerPos.getY(), 2) - Math.pow(distToMarker, 2));
    }

    private static boolean confirmDistance(Vector2 calcPosition, Vector2 objPos, double expectedDist) {
        double distError = 10;

        return Math.abs(calcPosition.getDistance(objPos) - expectedDist) <= distError;
    }

    private static boolean confirmAllDistances(Vector2 calcPosition, List<Marker> markers) {
        for (Marker marker : markers) {
            if (!confirmDistance(calcPosition, marker.getPosition(), marker.getDistance())) {
                return false;
            }
        }

        return true;
    }

    private static Vector2 calcPositionForAnotherObject(Marker marker1, Marker marker2, Vector2 playerPosition, GameObject object) throws FailedToCalculateException {

        double distObjToMarker1 = Math.sqrt(Math.pow(marker1.getDistance(), 2) + Math.pow(object.getDistance(), 2) -
                2 * marker1.getDistance() * object.getDistance() * Math.cos(Math.abs(marker1.getDirection() - object.getDirection())));

        // Теорема косинусов не работает на отрезках на одной прямой
        /*if (Math.abs(marker1.getDirection() - object.getDirection()) < 0.000001f) {
            distObjToMarker1 = marker1.getDistance() + object.getDistance();
        }*/

        Marker marker1FromObjectPerspective = new Marker(marker1.getId(), marker1.getPosition());
        marker1FromObjectPerspective.setDistance(distObjToMarker1);

        double distObjToMarker2 = Math.sqrt(Math.pow(marker2.getDistance(), 2) + Math.pow(object.getDistance(), 2) -
                2 * marker2.getDistance() * object.getDistance() * Math.cos(Math.abs(marker2.getDirection() - object.getDirection())));

        // Теорема косинусов не работает на отрезках на одной прямой
        /*if (Math.abs(marker2.getDirection() - object.getDirection()) < 0.000001f) {
            distObjToMarker2 = marker2.getDistance() + object.getDistance();
        }*/

        Marker marker2FromObjectPerspective = new Marker(marker2.getId(), marker2.getPosition());
        marker2FromObjectPerspective.setDistance(distObjToMarker2);

        Marker playerAsMarker = new Marker("player", playerPosition);
        playerAsMarker.setDistance(object.getDistance());

        return calcPositionBy2MarkersAndOne(marker1FromObjectPerspective, playerAsMarker, marker2FromObjectPerspective);
    }

}
