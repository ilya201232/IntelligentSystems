package org.example.controller;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.exception.FailedToCalculateException;
import org.example.model.Knowledge;
import org.example.model.base.GameObject;
import org.example.model.object.Marker;
import org.example.model.unit.Side;
import org.example.model.unit.Vector2;
import org.example.receiver.PerceptionFormer;
import org.example.receiver.Receiver;
import org.example.model.Perception;
import org.example.model.object.Player;
import org.example.sender.Sender;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
public class PlayerController implements Runnable {

    private final Knowledge knowledge;
    private PerceptionFormer perceptionFormer;

    private Receiver receiver;
    private Sender sender;


    @Setter
    private double turnMoment;
    private final Vector2 startPos;
    private boolean isInitialized = false;

    public PlayerController(String teamName, boolean isGoalie, Vector2 startPos) {
        knowledge = new Knowledge(teamName, isGoalie);
        this.startPos = startPos;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            perceptionFormer = new PerceptionFormer(knowledge, socket);
            sender = new Sender(socket, knowledge);

            try (ExecutorService executorService = Executors.newSingleThreadExecutor()) {
                Future<?> future = executorService.submit(perceptionFormer);

                try {
                    sender.sendInit(knowledge.getTeamName(), "7", knowledge.isGoalie());
                    while (!future.isDone()) {
                        if (knowledge.isServerReady()) {
                            if (!isInitialized) {
                                sender.sendMove(startPos);
                                isInitialized = true;
                            }
                        } else {
                            continue;
                        }
                        executeActions();
                    }
                    sender.sendBye();
                } catch (Exception e) {
                    log.error("Unexpected exception", e);
                }


            } catch (Exception e) {
                log.error("Unexpected exception", e);
                throw e;
            }
        } catch (SocketException e) {
            log.error("Failed to initialise socket connection for player in team {}", e.getMessage());
        } catch (UnknownHostException e) {
            log.error("Failed to get localhost address.");
        } catch (Exception e) {
            log.error("Unexpected exception", e);
            throw e;
        }
    }

    // Main function!
    private void executeActions() {
        calcPosition();
        sender.sendTurn(turnMoment);
    }

    private void calcPosition() {
        Perception perception = perceptionFormer.getLastPerception();

        if (perception == null) {
            log.debug("No enough data for position calculation...");
            return;
        }

        List<Marker> markersInView = perception.getMarkersSaw();

        /*if (markersInView.size() >= 3) {
            knowledge.getThisPlayer().setPosition(calcPositionBy3Markers(
                markersInView.get(0),
                markersInView.get(1),
                markersInView.get(2)
            ));

            log.info("Calculated this player's position: {}", knowledge.getThisPlayer().getPosition());
        } else*/
        if (markersInView.size() >= 3) {
            try {
                knowledge.getThisPlayer().setPosition(calcPositionBy2MarkersAndOne(
                        markersInView.get(0),
                        markersInView.get(1),
                        markersInView.get(2)
                ));
                log.info("Calculated this player's position: {}", knowledge.getThisPlayer().getPosition());
            } catch (FailedToCalculateException e) {
                log.error("Position calculation has failed...");
            }
        } else {
            log.debug("Not enough markers in sight to calculate position.");
        }

        List<Player> players = perception.getUnknownPlayersSaw();
        players.addAll(perception.getTeammatesSaw());
        players.addAll(perception.getOpponentsSaw());

        if (knowledge.getThisPlayer().getPosition() != null && !players.isEmpty() && markersInView.size() >= 2) {
            Player player = players.getFirst();

            Vector2 position;
            try {
                position = calcPositionForAnotherObject(markersInView.get(0), markersInView.get(1), knowledge.getThisPlayer().getPosition(), player);
                player.setPosition(position);
                log.info("Calculated another player's position: {}", position);
            } catch (FailedToCalculateException e) {
                log.error("Position calculation of another player has failed...");
            }
        }
    }

    private Vector2 calcPositionBy2MarkersAndOne(Marker marker1, Marker marker2, Marker marker3) throws FailedToCalculateException {

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

            Vector2 finalPos = position2;
            double min = calculateMistake(position2, marker3.getPosition(), marker3.getDistance());

            double newMin = calculateMistake(position1, marker3.getPosition(), marker3.getDistance());
            if (newMin < min) {
                finalPos = position1;
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

            Vector2 finalPos = position2;
            double min = calculateMistake(position2, marker3.getPosition(), marker3.getDistance());

            double newMin = calculateMistake(position1, marker3.getPosition(), marker3.getDistance());
            if (newMin < min) {
                finalPos = position1;
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

    private double calculateMistake(Vector2 calcPosition, Vector2 markerPos, double distToMarker) {
        return Math.abs(Math.pow(calcPosition.getX() - markerPos.getX(), 2) + Math.pow(calcPosition.getY() - markerPos.getY(), 2) - Math.pow(distToMarker, 2));
    }

    private boolean confirmDistance(Vector2 calcPosition, Vector2 objPos, double expectedDist) {
        double distError = 3;

        return Math.abs(calcPosition.getDistance(objPos) - expectedDist) <= distError;
    }

    private boolean confirmAllDistances(Vector2 calcPosition, List<Marker> markers) {
        for (Marker marker : markers) {
            if (!confirmDistance(calcPosition, marker.getPosition(), marker.getDistance())) {
                return false;
            }
        }

        return true;
    }

    private Vector2 calcPositionForAnotherObject(Marker marker1, Marker marker2, Vector2 playerPosition, GameObject object) throws FailedToCalculateException {

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

        if (knowledge.getTeamSide() == Side.LEFT) {
            int pop = 0;
        }

        return calcPositionBy2MarkersAndOne(marker1FromObjectPerspective, playerAsMarker, marker2FromObjectPerspective);
    }

}
