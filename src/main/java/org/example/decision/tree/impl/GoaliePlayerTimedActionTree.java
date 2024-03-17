package org.example.decision.tree.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.decision.tree.TimedActionTree;
import org.example.decision.tree.TreeNode;
import org.example.model.Knowledge;
import org.example.model.Perception;
import org.example.model.object.Ball;
import org.example.model.object.Marker;
import org.example.model.unit.Event;
import org.example.model.unit.EventType;
import org.example.model.unit.Side;
import org.example.model.unit.Vector2;
import org.example.sender.action.*;
import org.example.utils.PerceptionUtils;
import org.example.utils.PlayerUtils;

import java.util.*;

@Slf4j
public class GoaliePlayerTimedActionTree extends TimedActionTree {

    private final static double SEEK_TURN_STEP = 50;
    private final static double ABS_DELTA_OPPOSITE_MARKER_DIRECTION_RAD = Math.toRadians(20);

    private final static double BALL_DISTANCE_FAR = 20;
    private final static double BALL_CATCHING_DISTANCE = 2;
    private final static double DASH_TO_BALL_POWER = 100;
    private final static double BALL_IN_FRONT_DIRECTION_DELTA_RAD = Math.toRadians(10);
    private static final double BALL_KICK_POWER = 100;


    private final static double GOAL_MAX_DISTANCE = 5;
    private final static double DASH_TO_GOAL_POWER = 100;

    private final static double FPC_MAX_DISTANCE = 16;
    private final static double FPC_MIN_DISTANCE = 12;

    private final static double FP_SIDE_MAX_DISTANCE = 28;
    private final static double FP_SIDE_MIN_DISTANCE = 20;

    private final static double DASH_TO_FP_MARKS_POWER = 100;

    private String goalFlagName = null;
    private String oppositeGoalFlagName = null;

    private String perimeterCenterFlagName = null;
    private String perimeterBottomFlagName = null;
    private String perimeterTopFlagName = null;


    private Integer catchingProgress = -1;
    private int lastSentCatch;

    private final UUID lookoutTimerUUID;
    private final static Integer LOOKOUT_TIME_POINT = 20;
    private List<Vector2> lookoutMarkersPositions = new ArrayList<>();
    private Integer lookoutProgress = 0;
    private Boolean needToRealign = true;


    public GoaliePlayerTimedActionTree(Knowledge knowledgeGlobal) {
        super(knowledgeGlobal);

        lookoutTimerUUID = createTimer();
    }

    @Override
    public boolean checkMinimumConditionForPassingPerception(Perception perception) {
        return perception.getSensed() == null || perception.getMarkersSaw().isEmpty();
    }

    @Override
    protected TreeNode createTreeRoot() {
        TreeNode facingMarkTree = createFacingMarkTree();
        createdTreeNodes.put("facingMark", facingMarkTree);

        return (perception, knowledge, args) -> {

            // Initialising

            if (goalFlagName == null || oppositeGoalFlagName == null || perimeterCenterFlagName == null || perimeterBottomFlagName == null || perimeterTopFlagName == null) {
                goalFlagName = "g " + knowledge.getTeamSide();
                oppositeGoalFlagName = "g " + knowledge.getTeamSide().getOpposite();
                perimeterCenterFlagName = "f p " + knowledge.getTeamSide() + " c";
                perimeterTopFlagName = "f p " + knowledge.getTeamSide() + " t";
                perimeterBottomFlagName = "f p " + knowledge.getTeamSide() + " b";

                lookoutMarkersPositions.add(knowledge.getMarkersPositions().get("f " + knowledge.getTeamSide() + " b"));
                lookoutMarkersPositions.add(knowledge.getMarkersPositions().get("f p " + knowledge.getTeamSide() + " b"));
                lookoutMarkersPositions.add(knowledge.getMarkersPositions().get("f p " + knowledge.getTeamSide() + " c"));
                lookoutMarkersPositions.add(knowledge.getMarkersPositions().get("f p " + knowledge.getTeamSide() + " t"));
                lookoutMarkersPositions.add(knowledge.getMarkersPositions().get("f " + knowledge.getTeamSide() + " t"));
            }

            return facingMarkTree.getResultAction(perception, knowledge);
        };
    }

    private TreeNode createFacingMarkTree() {
        TreeNode isBallFarTree = createIsBallFarTree();

        return (perception, knowledge, args) -> {
//            Marker oppositeGoalMarker = (Marker) args[0];

            if (needToRealign) {
                Optional<Marker> oppositeGoalMarker = PerceptionUtils.getMarker(perception, oppositeGoalFlagName);

                if (oppositeGoalMarker.isEmpty()) {
                    Optional<Double> oppositeGoalMarkerDirection = PlayerUtils.calcRotationToMarker(oppositeGoalFlagName, perception, knowledge);

                    if (oppositeGoalMarkerDirection.isEmpty()) {
                        return new TurnAction(SEEK_TURN_STEP);
                    } else {
                        if (Math.abs(oppositeGoalMarkerDirection.get()) > ABS_DELTA_OPPOSITE_MARKER_DIRECTION_RAD) {
                            return new TurnAction(Math.toDegrees(oppositeGoalMarkerDirection.get()));
                        } else {
                            needToRealign = false;
                            return isBallFarTree.getResultAction(perception, knowledge, args);
                        }
                    }
                } else {
                    if (Math.abs(oppositeGoalMarker.get().getDirection()) > ABS_DELTA_OPPOSITE_MARKER_DIRECTION_RAD) {
                        return new TurnAction(Math.toDegrees(oppositeGoalMarker.get().getDirection()));
                    } else {
                        needToRealign = false;
                        return isBallFarTree.getResultAction(perception, knowledge, args);
                    }
                }
            }

            return isBallFarTree.getResultAction(perception, knowledge, args);
        };
    }

    private TreeNode createIsBallFarTree() {

        TreeNode getPlayerAndMarkerTree = createGetPlayerAndMarkerTree();
        createdTreeNodes.put("getPlayerAndMarker", getPlayerAndMarkerTree);

        TreeNode isCatchingTree = createIsCatchingTree();

        return (perception, knowledge, args) -> {
            Ball ball = perception.getBallSaw();

            if (ball == null || ball.getDistance() == null) {
                return getPlayerAndMarkerTree.getResultAction(perception, knowledge, args);
            } else {
                resetTimer(lookoutTimerUUID);
                lookoutProgress = 0;
                needToRealign = false;

                // Always watch the ball
                if (Math.abs(ball.getDirection()) > BALL_IN_FRONT_DIRECTION_DELTA_RAD) {
                    return new TurnAction(Math.toDegrees(ball.getDirection()));
                }

                if (ball.getDistance() > BALL_DISTANCE_FAR) {

                    return getPlayerAndMarkerTree.getResultAction(perception, knowledge, args);
                } else {
                    List<Object> argsModified = new ArrayList<>(Arrays.stream(args).toList());
                    argsModified.add(ball);

                    Object[] argsNew = argsModified.toArray();

                    return isCatchingTree.getResultAction(perception, knowledge, argsNew);
                }
            }

        };
    }


    private TreeNode createGetPlayerAndMarkerTree() {

        TreeNode checkPositionTree = createCheckPositionTree();

        return (perception, knowledge, args) -> {
            // If ball is not visible or it's too far

            catchingProgress = -1;
            lastSentCatch = Integer.MAX_VALUE;

            // Calculate this player position
            Optional<Vector2> playerPosition = PlayerUtils.calcThisPlayerPosition(perception, knowledge);

            if (playerPosition.isEmpty()) {
                log.error("Can't proceed! Player calculation has failed!");
                return new TurnAction(SEEK_TURN_STEP);
            }

            Optional<Marker> marker = perception.getMarkersSaw().stream().findAny();

            if (marker.isEmpty()) {
                log.error("Can't proceed! Failed to get any marker to calculate rotations.");
                return new TurnAction(SEEK_TURN_STEP);
            }


            List<Object> argsModified = new ArrayList<>(Arrays.stream(args).toList());
            argsModified.add(playerPosition.get());
            argsModified.add(marker.get());

            Object[] argsNew = argsModified.toArray();
            return checkPositionTree.getResultAction(perception, knowledge, argsNew);
        };
    }

    private TreeNode createCheckPositionTree() {

        TreeNode lookoutTree = createLookoutTree();

        return (perception, knowledge, args) -> {

            Vector2 playerPosition = (Vector2) args[1];
            Marker marker = (Marker) args[2];

            Vector2 goalPosition = knowledge.getMarkersPositions().get(goalFlagName);
            double goalDist = playerPosition.getDistance(goalPosition);

            if (goalDist > GOAL_MAX_DISTANCE) {

                // Calculate rotation angle and dash in that direction
                double direction = marker.getDirection() + Vector2.getAngleBetweenVectors(Vector2.createVector(playerPosition, marker.getPosition()), Vector2.createVector(playerPosition, goalPosition));

                if (direction > Math.PI) {
                    direction -= 2 * Math.PI;
                } else if (direction < -Math.PI) {
                    direction += 2 * Math.PI;
                }

                return new DashAction(DASH_TO_GOAL_POWER, Math.toDegrees(direction), true);
            }


            Vector2 fpcPosition = knowledge.getMarkersPositions().get(perimeterCenterFlagName);
            double fpcDist = playerPosition.getDistance(fpcPosition);

            if (fpcDist < FPC_MIN_DISTANCE) {
                // Calculate rotation angle and dash in that direction
                double direction = marker.getDirection() + Vector2.getAngleBetweenVectors(Vector2.createVector(playerPosition, marker.getPosition()), Vector2.createVector(playerPosition, fpcPosition));

                if (direction > Math.PI) {
                    direction -= 2 * Math.PI;
                } else if (direction < -Math.PI) {
                    direction += 2 * Math.PI;
                }

                return new DashAction(-DASH_TO_FP_MARKS_POWER, Math.toDegrees(direction), true);
            } else if (fpcDist > FPC_MAX_DISTANCE) {
                // Calculate rotation angle and dash in that direction
                double direction = marker.getDirection() + Vector2.getAngleBetweenVectors(Vector2.createVector(playerPosition, marker.getPosition()), Vector2.createVector(playerPosition, fpcPosition));

                if (direction > Math.PI) {
                    direction -= 2 * Math.PI;
                } else if (direction < -Math.PI) {
                    direction += 2 * Math.PI;
                }

                return new DashAction(DASH_TO_FP_MARKS_POWER, Math.toDegrees(direction), true);
            }


            Vector2 fpbPosition = knowledge.getMarkersPositions().get(perimeterBottomFlagName);
            double fpbDist = playerPosition.getDistance(fpbPosition);

            if (fpbDist < FP_SIDE_MIN_DISTANCE) {
                // Calculate rotation angle and dash in that direction
                double direction = marker.getDirection() + Vector2.getAngleBetweenVectors(Vector2.createVector(playerPosition, marker.getPosition()), Vector2.createVector(playerPosition, fpbPosition));

                if (direction > Math.PI) {
                    direction -= 2 * Math.PI;
                } else if (direction < -Math.PI) {
                    direction += 2 * Math.PI;
                }

                return new DashAction(-DASH_TO_FP_MARKS_POWER, Math.toDegrees(direction), true);
            } else if (fpbDist > FP_SIDE_MAX_DISTANCE) {
                // Calculate rotation angle and dash in that direction
                double direction = marker.getDirection() + Vector2.getAngleBetweenVectors(Vector2.createVector(playerPosition, marker.getPosition()), Vector2.createVector(playerPosition, fpbPosition));

                if (direction > Math.PI) {
                    direction -= 2 * Math.PI;
                } else if (direction < -Math.PI) {
                    direction += 2 * Math.PI;
                }

                return new DashAction(DASH_TO_FP_MARKS_POWER, Math.toDegrees(direction), true);
            }


            Vector2 fptPosition = knowledge.getMarkersPositions().get(perimeterTopFlagName);
            double fptDist = playerPosition.getDistance(fptPosition);

            if (fptDist < FP_SIDE_MIN_DISTANCE) {
                // Calculate rotation angle and dash in that direction
                double direction = marker.getDirection() + Vector2.getAngleBetweenVectors(Vector2.createVector(playerPosition, marker.getPosition()), Vector2.createVector(playerPosition, fptPosition));

                if (direction > Math.PI) {
                    direction -= 2 * Math.PI;
                } else if (direction < -Math.PI) {
                    direction += 2 * Math.PI;
                }

                return new DashAction(-DASH_TO_FP_MARKS_POWER, Math.toDegrees(direction), true);
            } else if (fptDist > FP_SIDE_MAX_DISTANCE) {
                // Calculate rotation angle and dash in that direction
                double direction = marker.getDirection() + Vector2.getAngleBetweenVectors(Vector2.createVector(playerPosition, marker.getPosition()), Vector2.createVector(playerPosition, fptPosition));

                if (direction > Math.PI) {
                    direction -= 2 * Math.PI;
                } else if (direction < -Math.PI) {
                    direction += 2 * Math.PI;
                }

                return new DashAction(DASH_TO_FP_MARKS_POWER, Math.toDegrees(direction), true);
            }

            if (getTimerValue(lookoutTimerUUID).orElse(-1) > LOOKOUT_TIME_POINT) {
                return lookoutTree.getResultAction(perception, knowledge, args);
            }

            return EmptyAction.instance;
        };
    }

    private TreeNode createLookoutTree() {

        return (perception, knowledge, args) -> {
            if (lookoutProgress == lookoutMarkersPositions.size()) {
                lookoutProgress = 0;
                needToRealign = true;
                resetTimer(lookoutTimerUUID);

                return createdTreeNodes.get("facingMark").getResultAction(perception, knowledge);
            }

            Vector2 markerPosition = lookoutMarkersPositions.get(lookoutProgress);
            lookoutProgress++;

            Optional<Double> rotationOptional = PlayerUtils.calcRotationToCoordinates(markerPosition, perception, knowledge);

            if (rotationOptional.isEmpty()) {
                needToRealign = true;
                return EmptyAction.instance;
            }

            return new TurnAction(Math.toDegrees(rotationOptional.get()));
        };
    }

    private TreeNode createIsCatchingTree() {
        TreeNode catchBallTree = createCatchBallTree();

        return (perception, knowledge, args) -> {

            Optional<Marker> centerMarker = perception.getMarkersSaw().stream().filter(marker1 -> marker1.getId().equals("f c")).findFirst();

            double distance;
            if (centerMarker.isEmpty() || centerMarker.get().getDistance() == null) {
                Optional<Vector2> playerPosition = PlayerUtils.calcThisPlayerPosition(perception, knowledge);

                if (playerPosition.isEmpty()) {
                    return catchBallTree.getResultAction(perception, knowledge, args);
                }

                distance = playerPosition.get().getDistance(new Vector2());
            } else {
                distance = centerMarker.get().getDistance();
            }

            if (distance < 39) {
                List<Object> argsModified = new ArrayList<>(Arrays.stream(args).toList());
                argsModified.removeLast();

                Object[] argsNew = argsModified.toArray();
                resetTimer(lookoutTimerUUID);

                createdTreeNodes.get("getPlayerAndMarker").getResultAction(perception, knowledge, argsNew);
            }

            // The goal is to catch the ball
            return catchBallTree.getResultAction(perception, knowledge, args);
        };
    }

    private TreeNode createCatchBallTree() {

        return (perception, knowledge, args) -> {
            Marker oppositeGoalMarker = (Marker) args[0];
            Ball ball = (Ball) args[1];

            if (catchingProgress == -1) {
                catchingProgress = 0;
            }

            if (catchingProgress == 0) {
                // Need to catch the ball first

                Event lastHeardCatchEvent = knowledge.getHeardEvents().get(EventType.GOALIE_CATCH_BALL);

                if (lastHeardCatchEvent != null && lastHeardCatchEvent.getCycleNumber() > lastSentCatch) {
                    catchingProgress = 1;
                } else {
                    if (ball.getDistance() < BALL_CATCHING_DISTANCE) {
                        lastSentCatch = perception.getCycleNumber();
                        return new CatchAction(Math.toDegrees(ball.getDirection()), true);
                    } else {
                        if (ball.getDistance() > 5 && lastPerception != null) {

                            Ball lastSeenBall = lastPerception.getBallSaw();

                            if (lastSeenBall == null) {
                                return new DashAction(DASH_TO_BALL_POWER * 0.8, Math.toDegrees(ball.getDirection()), true);
                            }

                            Optional<Vector2> lastBallPosition = PlayerUtils.calcAnotherObjectPosition(lastPerception, knowledge, lastSeenBall);

                            if (lastBallPosition.isEmpty()) {
                                return new DashAction(DASH_TO_BALL_POWER * 0.8, Math.toDegrees(ball.getDirection()), true);
                            }

                            Optional<Vector2> ballPosition = PlayerUtils.calcAnotherObjectPosition(perception, knowledge, ball);

                            if (ballPosition.isEmpty()) {
                                return new DashAction(DASH_TO_BALL_POWER * 0.8, Math.toDegrees(ball.getDirection()), true);
                            }

                            Vector2 ballMovementVector = ballPosition.get().minus(lastBallPosition.get());

                            Vector2 approximatePosition = ballPosition.get().plus(ballMovementVector.multiply(2));

                            Optional<Double> direction = PlayerUtils.calcRotationToCoordinates(approximatePosition, perception, knowledge);

                            return new DashAction(DASH_TO_BALL_POWER * 0.8, Math.toDegrees(direction.orElse(ball.getDirection())), true);
                        }

                        return new DashAction(DASH_TO_BALL_POWER * 0.8, Math.toDegrees(ball.getDirection()), true);
                    }
                }
            }

            if (catchingProgress == 1) {
                catchingProgress = 2;
                return new MoveAction(knowledge.getStartPosition(), true);
            }

            if (catchingProgress == 2) {
                catchingProgress = -1;
                return new KickAction(BALL_KICK_POWER, Math.toDegrees(oppositeGoalMarker.getDirection()), true);
            }

            return EmptyAction.instance;
        };
    }

}
