package org.example.decision.tree.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.decision.tree.TimedActionTree;
import org.example.decision.tree.TreeNode;
import org.example.decision.tree.impl.data.GoaliePlayerTimedStateType;
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

    private final static double SEEK_TURN_STEP = 60;
    private final static double ABS_DELTA_OPPOSITE_MARKER_DIRECTION_RAD = Math.toRadians(20);

    private final static double BALL_DISTANCE_FAR = 26.043665256641585; // 16.5 by x, 20 by y
    private final static double BALL_CATCHING_DISTANCE = 1.5;
    private final static double BALL_KEEP_DISTANCE = 10;
    private final static double GOAL_KEEP_DISTANCE = 7;
    private final static double DASH_TO_BALL_POWER = 100;
    private final static double DASH_TO_BALL_MID_POWER = 60;
    private final static double BALL_IN_FRONT_DIRECTION_DELTA_RAD = Math.toRadians(15);
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


    private Integer catchingProgress = 0;
    private int lastSentCatch;

    private final UUID lookoutTimerUUID;
    private final static Integer LOOKOUT_TIME_POINT = 20;
    private List<Vector2> lookoutMarkersPositions = new ArrayList<>();
    private Integer lookoutProgress = 0;

    private final UUID afterCatchTimerUUID;
    private final static Integer AFTER_CATCH_WAIT_TIME = 15;

    private Boolean needToRealign = true;


    private GoaliePlayerTimedStateType state = GoaliePlayerTimedStateType.WAITING_FOR_BALL;
    private Integer lastGoalEventTime = null;


    public GoaliePlayerTimedActionTree(Knowledge knowledgeGlobal) {
        super(knowledgeGlobal);

        lookoutTimerUUID = createTimer();
        afterCatchTimerUUID = createTimer();
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

            // Is it necessary to turn player to look at the center?
            if (needToRealign) {
                Optional<Marker> oppositeGoalMarker = PerceptionUtils.getMarker(perception, oppositeGoalFlagName);

                if (oppositeGoalMarker.isEmpty()) {
                    Optional<Double> oppositeGoalMarkerDirection = PlayerUtils.calcRotationToMarker(oppositeGoalFlagName, perception, knowledge);

                    if (oppositeGoalMarkerDirection.isEmpty()) {
                        return new TurnAction(SEEK_TURN_STEP);
                    } else {
                        if (Math.abs(oppositeGoalMarkerDirection.get()) > ABS_DELTA_OPPOSITE_MARKER_DIRECTION_RAD) {
                            return TurnAction.fromRadians(oppositeGoalMarkerDirection.get());
                        } else {
                            needToRealign = false;
                            return isBallFarTree.getResultAction(perception, knowledge);
                        }
                    }
                } else {
                    if (Math.abs(oppositeGoalMarker.get().getDirection()) > ABS_DELTA_OPPOSITE_MARKER_DIRECTION_RAD) {
                        return TurnAction.fromRadians(oppositeGoalMarker.get().getDirection());
                    } else {
                        needToRealign = false;
                        return isBallFarTree.getResultAction(perception, knowledge);
                    }
                }
            }

            return isBallFarTree.getResultAction(perception, knowledge);
        };
    }

    private TreeNode createIsBallFarTree() {
        // Check ball position

        TreeNode getPlayerAndMarkerTree = createGetPlayerAndMarkerTree();
        createdTreeNodes.put("getPlayerAndMarker", getPlayerAndMarkerTree);

        TreeNode isCatchingTree = createIsCatchingTree();

        return (perception, knowledge, args) -> {
            Ball ball = perception.getBallSaw();

            if (state == GoaliePlayerTimedStateType.WAITING_FOR_BALL) {

                if (getTimerValue(afterCatchTimerUUID).orElse(0) <= AFTER_CATCH_WAIT_TIME) {
                    // Goalie just kicked ball. Need to wait a bit
                    return EmptyAction.instance;
                }

                if (ball == null || ball.getDistance() == null) {
                    // If ball is not visible - get to default position
                    return getPlayerAndMarkerTree.getResultAction(perception, knowledge, args);
                } else {
                    // Ball is visible, so no need to lookout
                    resetTimer(lookoutTimerUUID);
                    lookoutProgress = 0;
                    needToRealign = false;

                    Optional<Vector2> ballPosition = PlayerUtils.calcAnotherObjectPosition(perception, ball);

                    if (ballPosition.isEmpty()) {
                        // Couldn't calculate position => run to it any way
                        return isCatchingTree.getResultAction(perception, knowledge);
                    }

                    Vector2 goalFlagPosition = knowledge.getMarkersPositions().get(goalFlagName);

                    if (ballPosition.get().getDistance(goalFlagPosition) > BALL_DISTANCE_FAR) {
                        // Ball is too far - go to default position, but without lookout event
                        return getPlayerAndMarkerTree.getResultAction(perception, knowledge, args);
                    } else {
                        // Ball it close enough -> need to start closing in to catch it
                        state = GoaliePlayerTimedStateType.CATCHING_BALL;
                        return isCatchingTree.getResultAction(perception, knowledge);
                    }
                }
            } else {
                // Already in state of catching the ball. Before proceeding - need to check for goal event

                Event goalEvent = knowledge.getHeardEvents().get(EventType.GOAL);

                if (goalEvent != null && (lastGoalEventTime == null || goalEvent.getCycleNumber() > lastGoalEventTime)) {
                    // Heard goal event, and it happened after last known goal -> failed to catch. Need to wait for ball again

                    state = GoaliePlayerTimedStateType.WAITING_FOR_BALL;
                    lastGoalEventTime = goalEvent.getCycleNumber();

                    resetTimer(lookoutTimerUUID);
                    lookoutProgress = 0;
                    needToRealign = true;

                    return new MoveAction(knowledge.getStartPosition());
                }

                // Must look for ball

                if (ball == null) {
                    return new TurnAction(SEEK_TURN_STEP);
                }

                if (Math.abs(ball.getDirection()) > BALL_IN_FRONT_DIRECTION_DELTA_RAD) {
                    return TurnAction.fromRadians(ball.getDirection());
                }

                // Goal hasn't happened yet -> there is still chance to catch the ball!
                return isCatchingTree.getResultAction(perception, knowledge);
            }
        };
    }

    private TreeNode createGetPlayerAndMarkerTree() {

        TreeNode lookoutTree = createLookoutTree();

        return (perception, knowledge, args) -> {
            // Ball is either too far or not visible at all. Need to return to start position.

            // Player can't always realign when moving to start position. Need to consider he's already aligned properly!

            catchingProgress = 0;
            lastSentCatch = Integer.MAX_VALUE;

            // Calculate this player position
            Optional<Vector2> playerPosition = PlayerUtils.calcThisPlayerPosition(perception);

            if (playerPosition.isEmpty()) {
                log.error("Can't proceed! Player calculation has failed!");
                return new TurnAction(SEEK_TURN_STEP);
            }

            Optional<Marker> marker = perception.getMarkersSaw().stream().findAny();

            if (marker.isEmpty()) {
                log.error("Can't proceed! Failed to get any marker to calculate rotations.");
                return new TurnAction(SEEK_TURN_STEP);
            }

            // Move player around until his position satisfy every constraint
            Vector2 goalPosition = knowledge.getMarkersPositions().get(goalFlagName);
            double goalDist = playerPosition.get().getDistance(goalPosition);

            if (goalDist > GOAL_MAX_DISTANCE) {

                // Calculate rotation angle and dash in that direction
                double direction = marker.get().getDirection() + Vector2.getAngleBetweenVectors(Vector2.createVector(playerPosition.get(), marker.get().getPosition()), Vector2.createVector(playerPosition.get(), goalPosition));

                if (direction > Math.PI) {
                    direction -= 2 * Math.PI;
                } else if (direction < -Math.PI) {
                    direction += 2 * Math.PI;
                }

                return new DashAction(DASH_TO_GOAL_POWER, Math.toDegrees(direction), true);
            }


            Vector2 fpcPosition = knowledge.getMarkersPositions().get(perimeterCenterFlagName);
            double fpcDist = playerPosition.get().getDistance(fpcPosition);

            if (fpcDist < FPC_MIN_DISTANCE) {
                // Calculate rotation angle and dash in that direction
                double direction = marker.get().getDirection() + Vector2.getAngleBetweenVectors(Vector2.createVector(playerPosition.get(), marker.get().getPosition()), Vector2.createVector(playerPosition.get(), fpcPosition));

                if (direction > Math.PI) {
                    direction -= 2 * Math.PI;
                } else if (direction < -Math.PI) {
                    direction += 2 * Math.PI;
                }

                return new DashAction(-DASH_TO_FP_MARKS_POWER, Math.toDegrees(direction), true);
            } else if (fpcDist > FPC_MAX_DISTANCE) {
                // Calculate rotation angle and dash in that direction
                double direction = marker.get().getDirection() + Vector2.getAngleBetweenVectors(Vector2.createVector(playerPosition.get(), marker.get().getPosition()), Vector2.createVector(playerPosition.get(), fpcPosition));

                if (direction > Math.PI) {
                    direction -= 2 * Math.PI;
                } else if (direction < -Math.PI) {
                    direction += 2 * Math.PI;
                }

                return new DashAction(DASH_TO_FP_MARKS_POWER, Math.toDegrees(direction), true);
            }


            Vector2 fpbPosition = knowledge.getMarkersPositions().get(perimeterBottomFlagName);
            double fpbDist = playerPosition.get().getDistance(fpbPosition);

            if (fpbDist < FP_SIDE_MIN_DISTANCE) {
                // Calculate rotation angle and dash in that direction
                double direction = marker.get().getDirection() + Vector2.getAngleBetweenVectors(Vector2.createVector(playerPosition.get(), marker.get().getPosition()), Vector2.createVector(playerPosition.get(), fpbPosition));

                if (direction > Math.PI) {
                    direction -= 2 * Math.PI;
                } else if (direction < -Math.PI) {
                    direction += 2 * Math.PI;
                }

                return new DashAction(-DASH_TO_FP_MARKS_POWER, Math.toDegrees(direction), true);
            } else if (fpbDist > FP_SIDE_MAX_DISTANCE) {
                // Calculate rotation angle and dash in that direction
                double direction = marker.get().getDirection() + Vector2.getAngleBetweenVectors(Vector2.createVector(playerPosition.get(), marker.get().getPosition()), Vector2.createVector(playerPosition.get(), fpbPosition));

                if (direction > Math.PI) {
                    direction -= 2 * Math.PI;
                } else if (direction < -Math.PI) {
                    direction += 2 * Math.PI;
                }

                return new DashAction(DASH_TO_FP_MARKS_POWER, Math.toDegrees(direction), true);
            }


            Vector2 fptPosition = knowledge.getMarkersPositions().get(perimeterTopFlagName);
            double fptDist = playerPosition.get().getDistance(fptPosition);

            if (fptDist < FP_SIDE_MIN_DISTANCE) {
                // Calculate rotation angle and dash in that direction
                double direction = marker.get().getDirection() + Vector2.getAngleBetweenVectors(Vector2.createVector(playerPosition.get(), marker.get().getPosition()), Vector2.createVector(playerPosition.get(), fptPosition));

                if (direction > Math.PI) {
                    direction -= 2 * Math.PI;
                } else if (direction < -Math.PI) {
                    direction += 2 * Math.PI;
                }

                return new DashAction(-DASH_TO_FP_MARKS_POWER, Math.toDegrees(direction), true);
            } else if (fptDist > FP_SIDE_MAX_DISTANCE) {
                // Calculate rotation angle and dash in that direction
                double direction = marker.get().getDirection() + Vector2.getAngleBetweenVectors(Vector2.createVector(playerPosition.get(), marker.get().getPosition()), Vector2.createVector(playerPosition.get(), fptPosition));

                if (direction > Math.PI) {
                    direction -= 2 * Math.PI;
                } else if (direction < -Math.PI) {
                    direction += 2 * Math.PI;
                }

                return new DashAction(DASH_TO_FP_MARKS_POWER, Math.toDegrees(direction), true);
            }

            // Player is at start position. Look around if the ball is not visible some time
            if (getTimerValue(lookoutTimerUUID).orElse(-1) > LOOKOUT_TIME_POINT) {
                return lookoutTree.getResultAction(perception, knowledge, args);
            }


            Ball ball = perception.getBallSaw();

            if (ball == null) {
                // If ball is not visible - do nothing. Lookout /\ will handle it.
                return EmptyAction.instance;
            }

            // Player is at start position. Turn to always face ball (if you already see it, of course)
            if (Math.abs(ball.getDirection()) > BALL_IN_FRONT_DIRECTION_DELTA_RAD) {
                return TurnAction.fromRadians(ball.getDirection());
            }

            // No action is required
            return EmptyAction.instance;
        };
    }

    private TreeNode createLookoutTree() {

        return (perception, knowledge, args) -> {
            // Ball is not visible for some time. Need to look for it.

            if (lookoutProgress == lookoutMarkersPositions.size()) {
                lookoutProgress = 0;
                needToRealign = true;
                resetTimer(lookoutTimerUUID);

                return createdTreeNodes.get("facingMark").getResultAction(perception, knowledge);
            }

            Vector2 markerPosition = lookoutMarkersPositions.get(lookoutProgress);
            lookoutProgress++;

            Optional<Double> rotationOptional = PlayerUtils.calcRotationToCoordinates(markerPosition, perception);

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
            // Player is in catching mode. This means that ball got through threshold, and now it's til to catch it

            /*Optional<Marker> centerMarker = perception.getMarkersSaw().stream().filter(marker1 -> marker1.getId().equals("f c")).findFirst();

            double distance;
            double direction;
            if (centerMarker.isEmpty() || centerMarker.get().getDistance() == null) {
                Optional<Vector2> playerPosition = PlayerUtils.calcThisPlayerPosition(perception, knowledge);

                if (playerPosition.isEmpty()) {
                    return new TurnAction(SEEK_TURN_STEP);
                }

                distance = playerPosition.get().getDistance(new Vector2());

                Optional<Double> directionOpt = PlayerUtils.calcRotationToCoordinates(new Vector2(), perception, knowledge);

                if (directionOpt.isEmpty()) {
                    return new TurnAction(SEEK_TURN_STEP);
                }

                direction = directionOpt.get();

            } else {
                distance = centerMarker.get().getDistance();
                direction = centerMarker.get().getDirection();
            }

            if (distance < 39) {
                List<Object> argsModified = new ArrayList<>(Arrays.stream(args).toList());
                argsModified.removeLast();

                Object[] argsNew = argsModified.toArray();
                resetTimer(lookoutTimerUUID);

                createdTreeNodes.get("getPlayerAndMarker").getResultAction(perception, knowledge, argsNew);
            }*/

            Vector2 topPoint = knowledge.getMarkersPositions().get(perimeterTopFlagName);
            Vector2 bottomPoint = knowledge.getMarkersPositions().get(perimeterBottomFlagName);
            Vector2 goalPoint = knowledge.getMarkersPositions().get(goalFlagName);

            double maxY = bottomPoint.getY();
            double minY = topPoint.getY();
            double minX = knowledge.getTeamSide() == Side.LEFT ? goalPoint.getX() : topPoint.getX();
            double maxX = knowledge.getTeamSide() == Side.LEFT ? topPoint.getX() : goalPoint.getX();

            Ball ball = perception.getBallSaw();

            if (ball == null) {
                return new TurnAction(SEEK_TURN_STEP);
            }

            Optional<Vector2> ballPositionOpt = PlayerUtils.calcAnotherObjectPosition(perception, ball);

            if (ballPositionOpt.isEmpty()) {
                // Failed to calculate ball position for some reason. Just go to it then.
                return new DashAction(DASH_TO_BALL_POWER, Math.toDegrees(ball.getDirection()), true);
            }

            Vector2 ballPosition = ballPositionOpt.get();

            if (ballPosition.getX() >= minX && ballPosition.getX() <= maxX &&
                ballPosition.getY() >= minY && ballPosition.getY() <= maxY
            ) {
                // Ball is within box around goal. Can swiftly approach it and catch
                return catchBallTree.getResultAction(perception, knowledge);
            }

            Optional<Vector2> playerPosition = PlayerUtils.calcThisPlayerPosition(perception);

            if (playerPosition.isEmpty()) {
                return new DashAction(DASH_TO_BALL_POWER, Math.toDegrees(ball.getDirection()), true);
            }

            if (playerPosition.get().getDistance(goalPoint) > GOAL_KEEP_DISTANCE) {
                return new DashAction(-20);
            }

            // Ball is not in the box yet. Approach to it, but keep distance
            if (ball.getDistance() > 2 * BALL_KEEP_DISTANCE) {
                return new DashAction(DASH_TO_BALL_MID_POWER, Math.toDegrees(ball.getDirection()), true);
            } else if (ball.getDistance() > BALL_KEEP_DISTANCE) {
                return new DashAction(DASH_TO_BALL_MID_POWER * 0.7, Math.toDegrees(ball.getDirection()), true);
            }

            // Ball is withing keeping distance. Wait
            return EmptyAction.instance;
        };
    }

    private TreeNode createCatchBallTree() {

        return (perception, knowledge, args) -> {
            // 100% to calculate player position here

            Ball ball = perception.getBallSaw();

            // Ball is 100% visible
            if (ball == null) {
                return EmptyAction.instance;
            }

            if (catchingProgress == 0) {
                // 1st step. Approach the ball and catch it

                Event lastHeardCatchEvent = knowledge.getHeardEvents().get(EventType.GOALIE_CATCH_BALL);

                if (lastHeardCatchEvent != null && lastHeardCatchEvent.getCycleNumber() > lastSentCatch) {
                    // If heard catch event, and it happened after last catch -> proceed to kick
                    catchingProgress = 1;
                } else {
                    // Still haven't caught the ball

                    if (ball.getDistance() < BALL_CATCHING_DISTANCE) {
                        // Ball is close enough to catch it
                        lastSentCatch = perception.getCycleNumber();
                        return new CatchAction(Math.toDegrees(ball.getDirection()), true);
                    } else {
                        // Ball is too far. Trying to get closer
                        if (ball.getDistance() > BALL_CATCHING_DISTANCE * 3 && lastPerception != null) {
                            // Ball is really far and there is data to get ball approximate position after some ticks

                            Ball lastSeenBall = lastPerception.getBallSaw();

                            if (lastSeenBall == null) {
                                return new DashAction(DASH_TO_BALL_POWER, Math.toDegrees(ball.getDirection()), true);
                            }

                            Optional<Vector2> lastBallPosition = PlayerUtils.calcAnotherObjectPosition(lastPerception, lastSeenBall);

                            if (lastBallPosition.isEmpty()) {
                                return new DashAction(DASH_TO_BALL_POWER, Math.toDegrees(ball.getDirection()), true);
                            }

                            Optional<Vector2> ballPosition = PlayerUtils.calcAnotherObjectPosition(perception, ball);

                            if (ballPosition.isEmpty()) {
                                return new DashAction(DASH_TO_BALL_POWER, Math.toDegrees(ball.getDirection()), true);
                            }

                            Vector2 ballMovementVector = ballPosition.get().minus(lastBallPosition.get());
                            Vector2 approximatePosition = ballPosition.get().plus(ballMovementVector.multiply(2));
                            Optional<Double> direction = PlayerUtils.calcRotationToCoordinates(approximatePosition, perception);

                            return new DashAction(DASH_TO_BALL_POWER, Math.toDegrees(direction.orElse(ball.getDirection())), true);
                        }

                        // Ball is close now. Need to be careful
                        return new DashAction(DASH_TO_BALL_POWER, Math.toDegrees(ball.getDirection()), true);
                    }
                }
            }

            if (catchingProgress == 1) {
                // Ball has been caught. Move to start position.
                catchingProgress = 2;
                return new MoveAction(knowledge.getStartPosition());
            }

            if (catchingProgress == 2) {
                // Ball has been caught, and this player has been moved to start position. Kick the ball.
                catchingProgress = 0;

                state = GoaliePlayerTimedStateType.WAITING_FOR_BALL;
                resetTimer(afterCatchTimerUUID);

                resetTimer(lookoutTimerUUID);
                lookoutProgress = 0;
                needToRealign = true;

                double directionToCenter = PlayerUtils.calcRotationToCoordinates(new Vector2(), perception).orElse(0d);
                return new KickAction(BALL_KICK_POWER, Math.toDegrees(directionToCenter), true);
            }

            return EmptyAction.instance;
        };
    }

}
