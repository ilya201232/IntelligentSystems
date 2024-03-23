package org.example.decision.tree.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.decision.tree.ActionTree;
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
public class GoaliePlayerActionTree extends ActionTree {

    private final static double SEEK_TURN_STEP = 50;
    private final static double ABS_DELTA_OPPOSITE_MARKER_DIRECTION_RAD = Math.toRadians(20);

    private final static double BALL_DISTANCE_FAR = 20;
    private final static double BALL_CATCHING_DISTANCE = 2;
    private final static int BALL_CATCHING_CHANCE_NUMBER = 5; // Resulting chance is (n-1)/n * 100%
    private final static double DASH_TO_BALL_POWER = 100;
    private final static double BALL_IN_FRONT_DIRECTION_DELTA_RAD = Math.toRadians(10);
    private static final double BALL_KICK_DISTANCE_EPS = 1;
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

    private Boolean isCatching = null;
    private Integer catchingProgress = -1;
    private int lastSentCatch;

    private final Random random = new Random();

    public GoaliePlayerActionTree(Knowledge knowledgeGlobal) {
        super(knowledgeGlobal);
    }

    @Override
    public boolean checkMinimumConditionForPassingPerception(Perception perception) {
        return perception.getSensed() == null || perception.getMarkersSaw().isEmpty();
    }

    @Override
    protected TreeNode createTreeRoot() {
        TreeNode seeMarkTree = createSeeMarkTree();
//        createdTreeNodes.put("stillRotating", stillRotatingTree);

        return (perception, knowledge, args) -> {

            // Initialising

            if (goalFlagName == null || oppositeGoalFlagName == null || perimeterCenterFlagName == null || perimeterBottomFlagName == null || perimeterTopFlagName == null) {
                goalFlagName = "g " + (knowledge.getTeamSide() == Side.LEFT ? "l" : "r");
                oppositeGoalFlagName = "g " + (knowledge.getTeamSide() == Side.LEFT ? "r" : "l");
                perimeterCenterFlagName = "f p " + (knowledge.getTeamSide() == Side.LEFT ? "l" : "r") + " c";
                perimeterTopFlagName = "f p " + (knowledge.getTeamSide() == Side.LEFT ? "l" : "r") + " t";
                perimeterBottomFlagName = "f p " + (knowledge.getTeamSide() == Side.LEFT ? "l" : "r") + " b";
            }

            return seeMarkTree.getResultAction(perception, knowledge);
        };
    }

    private TreeNode createSeeMarkTree() {
        TreeNode facingMarkTree = createFacingMarkTree();

        return (perception, knowledge, args) -> {
            // Always face opponent's goal marker. dir(f<l/r>) ~ (-60, 60)

            Optional<Marker> oppositeGoalMarker = PerceptionUtils.getMarker(perception, oppositeGoalFlagName);

            if (oppositeGoalMarker.isEmpty()) {
                // If opposite goal marker is not visible - turn until it is
                return new TurnAction(SEEK_TURN_STEP);
            } else {
                return facingMarkTree.getResultAction(perception, knowledge, oppositeGoalMarker.get());
            }
        };
    }

    private TreeNode createFacingMarkTree() {
        TreeNode isBallFarTree = createIsBallFarTree();

        return (perception, knowledge, args) -> {
            Marker oppositeGoalMarker = (Marker) args[0];

            if (Math.abs(oppositeGoalMarker.getDirection()) > ABS_DELTA_OPPOSITE_MARKER_DIRECTION_RAD) {
                return new TurnAction(Math.toDegrees(oppositeGoalMarker.getDirection()));
            } else {
                return isBallFarTree.getResultAction(perception, knowledge, args);
            }
        };
    }

    private TreeNode createIsBallFarTree() {

        TreeNode getPlayerAndMarkerTree = createGetPlayerAndMarkerTree();
        createdTreeNodes.put("getPlayerAndMarker", getPlayerAndMarkerTree);

        TreeNode isCatchingTree = createIsCatchingTree();

        return (perception, knowledge, args) -> {
            Ball ball = perception.getBallSaw();

            if (ball == null || ball.getDistance() == null || ball.getDistance() > BALL_DISTANCE_FAR) {
                return getPlayerAndMarkerTree.getResultAction(perception, knowledge, args);
            } else {
                List<Object> argsModified = new ArrayList<>(Arrays.stream(args).toList());
                argsModified.add(ball);

                Object[] argsNew = argsModified.toArray();

                return isCatchingTree.getResultAction(perception, knowledge, argsNew);
            }
        };
    }


    private TreeNode createGetPlayerAndMarkerTree() {

        TreeNode checkPositionTree = createCheckPositionTree();

        return (perception, knowledge, args) -> {
            // If ball is not visible or it's too far

            isCatching = null;
            catchingProgress = -1;
            lastSentCatch = Integer.MAX_VALUE;

            // Calculate this player position
            Optional<Vector2> playerPosition = PlayerUtils.calcThisPlayerPosition(perception);

            if (playerPosition.isEmpty()) {
                log.error("Can't proceed! Player calculation has failed!");
                return EmptyAction.instance; // TODO: May be dash a little to the side
            }

            Optional<Marker> marker = perception.getMarkersSaw().stream().findAny();

            if (marker.isEmpty()) {
                log.error("Can't proceed! Failed to get any marker to calculate rotations.");
                return EmptyAction.instance; // TODO: May be dash a little to the side
            }


            List<Object> argsModified = new ArrayList<>(Arrays.stream(args).toList());
            argsModified.add(playerPosition.get());
            argsModified.add(marker.get());

            Object[] argsNew = argsModified.toArray();
            return checkPositionTree.getResultAction(perception, knowledge, argsNew);
        };
    }

    private TreeNode createCheckPositionTree() {

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

                return new DashAction(DASH_TO_GOAL_POWER, Math.toDegrees(direction));
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

                return new DashAction(-DASH_TO_FP_MARKS_POWER, Math.toDegrees(direction));
            } else if (fpcDist > FPC_MAX_DISTANCE) {
                // Calculate rotation angle and dash in that direction
                double direction = marker.getDirection() + Vector2.getAngleBetweenVectors(Vector2.createVector(playerPosition, marker.getPosition()), Vector2.createVector(playerPosition, fpcPosition));

                if (direction > Math.PI) {
                    direction -= 2 * Math.PI;
                } else if (direction < -Math.PI) {
                    direction += 2 * Math.PI;
                }

                return new DashAction(DASH_TO_FP_MARKS_POWER, Math.toDegrees(direction));
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

                return new DashAction(-DASH_TO_FP_MARKS_POWER, Math.toDegrees(direction));
            } else if (fpbDist > FP_SIDE_MAX_DISTANCE) {
                // Calculate rotation angle and dash in that direction
                double direction = marker.getDirection() + Vector2.getAngleBetweenVectors(Vector2.createVector(playerPosition, marker.getPosition()), Vector2.createVector(playerPosition, fpbPosition));

                if (direction > Math.PI) {
                    direction -= 2 * Math.PI;
                } else if (direction < -Math.PI) {
                    direction += 2 * Math.PI;
                }

                return new DashAction(DASH_TO_FP_MARKS_POWER, Math.toDegrees(direction));
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

                return new DashAction(-DASH_TO_FP_MARKS_POWER, Math.toDegrees(direction));
            } else if (fptDist > FP_SIDE_MAX_DISTANCE) {
                // Calculate rotation angle and dash in that direction
                double direction = marker.getDirection() + Vector2.getAngleBetweenVectors(Vector2.createVector(playerPosition, marker.getPosition()), Vector2.createVector(playerPosition, fptPosition));

                if (direction > Math.PI) {
                    direction -= 2 * Math.PI;
                } else if (direction < -Math.PI) {
                    direction += 2 * Math.PI;
                }

                return new DashAction(DASH_TO_FP_MARKS_POWER, Math.toDegrees(direction));
            }

            return EmptyAction.instance;
        };
    }


    private TreeNode createIsCatchingTree() {

        TreeNode kickBallTree = createKickBallTree();
        TreeNode catchBallTree = createCatchBallTree();

        return (perception, knowledge, args) -> {
            if (isCatching == null) {
//                isCatching = random.nextInt(BALL_CATCHING_CHANCE_NUMBER) != 0; // 80% chance for catching
                // TODO: debug
                isCatching = true;
            }

            Optional<Marker> centerMarker = perception.getMarkersSaw().stream().filter(marker1 -> marker1.getId().equals("f c")).findFirst();
            if (isCatching) {

                if (centerMarker.isEmpty() || centerMarker.get().getDistance() == null || centerMarker.get().getDistance() < 39) {
                    List<Object> argsModified = new ArrayList<>(Arrays.stream(args).toList());
                    argsModified.removeLast();

                    Object[] argsNew = argsModified.toArray();

                    createdTreeNodes.get("getPlayerAndMarker").getResultAction(perception, knowledge, argsNew);
                }

                // The goal is to catch the ball
                return catchBallTree.getResultAction(perception, knowledge, args);
            } else {
                if (centerMarker.isEmpty() || centerMarker.get().getDistance() == null || centerMarker.get().getDistance() < 34) {
                    List<Object> argsModified = new ArrayList<>(Arrays.stream(args).toList());
                    argsModified.removeLast();

                    Object[] argsNew = argsModified.toArray();

                    createdTreeNodes.get("getPlayerAndMarker").getResultAction(perception, knowledge, argsNew);
                }
                // The goal is to kick the ball
                return kickBallTree.getResultAction(perception, knowledge, args);
            }
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
                        return new CatchAction(Math.toDegrees(ball.getDirection()));
                    } else {
                        if (ball.getDistance() > 5 && Math.abs(ball.getDirection()) > BALL_IN_FRONT_DIRECTION_DELTA_RAD) {
                            // Dash to the side until the ball is in front of goalie

                            Optional<Vector2> playerPosition = PlayerUtils.calcThisPlayerPosition(perception);

                            if (playerPosition.isEmpty()) {
                                return new DashAction(DASH_TO_BALL_POWER, Math.toDegrees(ball.getDirection()));
                            }

                            Optional<Marker> marker = perception.getMarkersSaw().stream().findAny();

                            if (marker.isEmpty()) {
                                return new DashAction(DASH_TO_BALL_POWER, Math.toDegrees(ball.getDirection()));
                            }

                            int sign = (int) Math.signum(ball.getDirection());

                            Vector2 vector = new Vector2(
                                    0,
                                    sign * (knowledge.getTeamSide() == Side.LEFT ? 1 : -1)
                            );

                            double direction = marker.get().getDirection() + Vector2.getAngleBetweenVectors(Vector2.createVector(playerPosition.get(), marker.get().getPosition()), vector);

                            if (direction > Math.PI) {
                                direction -= 2 * Math.PI;
                            } else if (direction < -Math.PI) {
                                direction += 2 * Math.PI;
                            }

                            return new DashAction(DASH_TO_BALL_POWER * 0.8, Math.toDegrees(direction));
                        } else {
                            return new DashAction(DASH_TO_BALL_POWER * 0.8, Math.toDegrees(ball.getDirection()));
                        }
                    }
                }
            }

            if (catchingProgress == 1) {
                catchingProgress = 2;
                return new MoveAction(knowledge.getStartPosition());
            }

            if (catchingProgress == 2) {
                catchingProgress = -1;
                return new KickAction(BALL_KICK_POWER, Math.toDegrees(oppositeGoalMarker.getDirection()));
            }

            return EmptyAction.instance;
        };
    }

    private TreeNode createKickBallTree() {

        return (perception, knowledge, args) -> {

            Marker oppositeGoalMarker = (Marker) args[0];
            Ball ball = (Ball) args[1];


            if (ball.getDistance() < BALL_KICK_DISTANCE_EPS) {
                return new KickAction(BALL_KICK_POWER, Math.toDegrees(oppositeGoalMarker.getDirection()));
            } else {
                if (ball.getDistance() > 7 && Math.abs(ball.getDirection()) > BALL_IN_FRONT_DIRECTION_DELTA_RAD) {
                    // Dash to the side until the ball is in front of goalie

                    Optional<Vector2> playerPosition = PlayerUtils.calcThisPlayerPosition(perception);

                    if (playerPosition.isEmpty()) {
                        return new DashAction(DASH_TO_BALL_POWER, Math.toDegrees(ball.getDirection()));
                    }

                    Optional<Marker> marker = perception.getMarkersSaw().stream().findAny();

                    if (marker.isEmpty()) {
                        return new DashAction(DASH_TO_BALL_POWER, Math.toDegrees(ball.getDirection()));
                    }

                    int sign = (int) Math.signum(ball.getDirection());

                    Vector2 vector = new Vector2(
                            0,
                            sign * (knowledge.getTeamSide() == Side.LEFT ? 1 : -1)
                    );

                    double direction = marker.get().getDirection() + Vector2.getAngleBetweenVectors(Vector2.createVector(playerPosition.get(), marker.get().getPosition()), vector);

                    if (direction > Math.PI) {
                        direction -= 2 * Math.PI;
                    } else if (direction < -Math.PI) {
                        direction += 2 * Math.PI;
                    }

                    return new DashAction(DASH_TO_BALL_POWER * 0.8, Math.toDegrees(direction));
                } else {
                    return new DashAction(DASH_TO_BALL_POWER * 0.8, Math.toDegrees(ball.getDirection()));
                }
            }
        };
    }
}
