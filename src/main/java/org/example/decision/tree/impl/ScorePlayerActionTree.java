package org.example.decision.tree.impl;

import org.example.decision.tree.ActionTree;
import org.example.decision.tree.TreeNode;
import org.example.decision.tree.impl.data.ScorePlayerStateType;
import org.example.model.Knowledge;
import org.example.model.Perception;
import org.example.model.object.Ball;
import org.example.model.object.Marker;
import org.example.model.unit.*;
import org.example.receiver.dto.HearDTO;
import org.example.sender.action.*;
import org.example.utils.KnowledgeUtils;
import org.example.utils.PerceptionUtils;
import org.example.utils.PlayerUtils;

import java.util.Optional;
import java.util.Random;

public class ScorePlayerActionTree extends ActionTree {
    private static final double MARKER_SEEK_STEP_DEG = 90;
    private static final double MARKER_SEEK_ABS_RAD = Math.toRadians(10);
    private static final double MARKER_SEEK_DIST_OK = 1.5d;
    private static final double MARKER_CLOSE_DASH_POWER = 70;
    private static final double SECOND_MARKER_ROTATE_DISTANCE = 25;

    private static final double BALL_SEEK_STEP_DEG = 90;
    private static final double BALL_SEEK_ABS_RAD = Math.toRadians(20);
    private static final double BALL_SEEK_DIST_OK = 1;
    private static final double BALL_CLOSE_DASH_POWER = 80;
    private static final double BALL_CLOSE_DASH_POWER_MAX = 100;

    private static final double KICK_POWER_SMALL = 5;
    private static final double KICK_POWER_GOAL = 100;
    private static final double KICK_DIRECTION_SMALL_DEG = 90;

    private static final String MESSAGE_TEXT = "go";

    private String firstMarkerName = "";
    private String secondMarkerName = "";
    private String goalMarkerName = "";

    private Integer lastKickCycleNumber = Integer.MAX_VALUE;
    private Integer startCycleNumber = Integer.MAX_VALUE;
    private Boolean heardMessage = false;
    private Boolean hasTurned = false;

    private ScorePlayerStateType state = ScorePlayerStateType.WAITING_FOR_PLAY_ON;
    private Random random = new Random();

    public ScorePlayerActionTree(Knowledge knowledgeGlobal) {
        super(knowledgeGlobal);
    }

    @Override
    public boolean checkMinimumConditionForPassingPerception(Perception perception) {
        return perception.getSensed() == null || perception.getMarkersSaw().isEmpty();
    }

    @Override
    protected TreeNode createTreeRoot() {

        TreeNode checkGoMessage = createCheckGoMessageTree();
        createdTreeNodes.put("checkGoMessage", checkGoMessage);

        return (perception, knowledge, args) -> {

            if (firstMarkerName.isEmpty()) {
                boolean isTop = random.nextBoolean();
                firstMarkerName = "f p " + (knowledge.getTeamSide() == Side.LEFT ? "l" : "r") + " " + (isTop ? "t" : "b");
                secondMarkerName = "f p " + (knowledge.getTeamSide() == Side.LEFT ? "r" : "l") + " " + (isTop ? "t" : "b");
                goalMarkerName = "g " + (knowledge.getTeamSide() == Side.LEFT ? "r" : "l");
            }


            if (state == ScorePlayerStateType.WAITING_FOR_PLAY_ON) {
                if (knowledge.getCurrentPlayMode().getPlayModeType() == PlayModeType.PLAY_ON) {
                    startCycleNumber = perception.getCycleNumber();
                    state = ScorePlayerStateType.TRAVEL_TO_FIRST_FLAG;
                    return checkGoMessage.getResultAction(perception, knowledge);
                }

                return EmptyAction.instance;
            } else if (state == ScorePlayerStateType.WAITING_FOR_PLAY_ON_LATE) {
                Event goalEvent = knowledge.getHeardEvents().get(EventType.GOAL);

                if (goalEvent != null && goalEvent.getCycleNumber() > lastKickCycleNumber &&
                        knowledge.getCurrentPlayMode().getPlayModeType() == PlayModeType.PLAY_ON && knowledge.getCurrentPlayMode().getCreatedAt() > lastKickCycleNumber) {
                    // This is final action in the loop. All state variables must be reinitialized
                    lastKickCycleNumber = Integer.MAX_VALUE;
                    startCycleNumber = perception.getCycleNumber();
                    state = ScorePlayerStateType.TRAVEL_TO_FIRST_FLAG;
                    heardMessage = false;
                    hasTurned = false;

                    return checkGoMessage.getResultAction(perception, knowledge);
                }

                return EmptyAction.instance;
            } else {
                Event goalEvent = knowledge.getHeardEvents().get(EventType.GOAL);

                if (goalEvent != null && goalEvent.getCycleNumber() > lastKickCycleNumber) {
                    state = ScorePlayerStateType.WAITING_FOR_PLAY_ON_LATE;
                    return new MoveAction(knowledge.getStartPosition());
                }
            }

            return checkGoMessage.getResultAction(perception, knowledge);
        };
    }

    private TreeNode createCheckGoMessageTree() {

        TreeNode travelToFirstFlagTree = createTravelToFirstFlagTree();
        createdTreeNodes.put("travelToFirstFlag", travelToFirstFlagTree);

        TreeNode seeBallTree = createSeeBallTree();
        createdTreeNodes.put("seeBall", seeBallTree);

        return (perception, knowledge, args) -> {

            if (!heardMessage) {
                Optional<HearDTO> message = KnowledgeUtils.hadHeardMessageSince(knowledge, MESSAGE_TEXT, startCycleNumber);

                if (message.isPresent()) {
                    heardMessage = true;
                    return seeBallTree.getResultAction(perception, knowledge);
                }

                return travelToFirstFlagTree.getResultAction(perception, knowledge);
            }

            return seeBallTree.getResultAction(perception, knowledge);
        };
    }

    private TreeNode createTravelToFirstFlagTree() {

        TreeNode travelToSecondFlagTree = createTravelToSecondFlagTree();
        createdTreeNodes.put("travelToSecondFlag", travelToSecondFlagTree);

        return (perception, knowledge, args) -> {

            if (state == ScorePlayerStateType.TRAVEL_TO_FIRST_FLAG) {
                Optional<Marker> marker = PerceptionUtils.getMarker(perception, firstMarkerName);

                if (marker.isEmpty()) {
                    Optional<Double> rotToMarkerRad = PlayerUtils.calcRotationToMarker(firstMarkerName, perception, knowledge);

                    return new TurnAction(rotToMarkerRad.map(Math::toDegrees).orElse(MARKER_SEEK_STEP_DEG));
                } else {
                    if (Math.abs(marker.get().getDirection()) < MARKER_SEEK_ABS_RAD) {
                        // Facing marker. Need to get to it

                        if (marker.get().getDistance() < MARKER_SEEK_DIST_OK) {
                            state = ScorePlayerStateType.TRAVEL_TO_SECOND_FLAG;
                            return travelToSecondFlagTree.getResultAction(perception, knowledge);
                        } else {
                            if (marker.get().getDistance() > 2 * MARKER_SEEK_DIST_OK)
                                return new DashAction(MARKER_CLOSE_DASH_POWER, true);

                            return new DashAction(MARKER_CLOSE_DASH_POWER);
                        }

                    } else {
                        return new TurnAction(Math.toDegrees(marker.get().getDirection()));
                    }
                }
            }

            return travelToSecondFlagTree.getResultAction(perception, knowledge);
        };
    }

    private TreeNode createTravelToSecondFlagTree() {

        return (perception, knowledge, args) -> {

            if (state == ScorePlayerStateType.TRAVEL_TO_SECOND_FLAG) {
                Optional<Marker> marker = PerceptionUtils.getMarker(perception, secondMarkerName);

                if (marker.isEmpty()) {

                    if (hasTurned) {
                        Vector2 markerPosition = knowledge.getMarkersPositions().get(secondMarkerName);
                        Optional<Double> baseRotation = PlayerUtils.calcRotationToMarker(secondMarkerName, perception, knowledge);

                        if (baseRotation.isEmpty()) {
                            return new TurnAction(10);
                        }

                        // No check for presence since this is performed in calcRotationToMarker(...)
                        //noinspection OptionalGetWithoutIsPresent
                        Vector2 playerPosition = PlayerUtils.calcThisPlayerPosition(perception, knowledge).get();

                        double realRot = baseRotation.get();
                        if (realRot > 0) {
                            realRot -= Math.PI;
                        } else {
                            realRot += Math.PI;
                        }

                        if (Math.abs(realRot) < MARKER_SEEK_ABS_RAD) {
                            // Facing marker. Need to get to it

                            if (markerPosition.getDistance(playerPosition) < MARKER_SEEK_DIST_OK) {
                                state = ScorePlayerStateType.FINISHED_TRAVELING;
                            } else {
                                return new DashAction(MARKER_CLOSE_DASH_POWER, 180d, true);
                            }

                        } else {
                            return new TurnAction(Math.toDegrees(realRot));
                        }
                    }

                    Optional<Double> rotToMarkerRad = PlayerUtils.calcRotationToMarker(secondMarkerName, perception, knowledge);

                    return new TurnAction(rotToMarkerRad.map(Math::toDegrees).orElse(MARKER_SEEK_STEP_DEG));
                } else {
                    if (Math.abs(marker.get().getDirection()) < MARKER_SEEK_ABS_RAD) {
                        // Facing marker. Need to get to it

                        if (marker.get().getDistance() < MARKER_SEEK_DIST_OK) {
                            state = ScorePlayerStateType.FINISHED_TRAVELING;
                        } else {
                            if (marker.get().getDistance() < SECOND_MARKER_ROTATE_DISTANCE) {
                                hasTurned = true;
                                return new TurnAction(180);
                            }
                            if (marker.get().getDistance() > 2 * MARKER_SEEK_DIST_OK)
                                return new DashAction(MARKER_CLOSE_DASH_POWER, true);
                            return new DashAction(MARKER_CLOSE_DASH_POWER);
                        }

                    } else {
                        return new TurnAction(Math.toDegrees(marker.get().getDirection()));
                    }
                }
            }

            // Finished traveling => seek ball (only keep it in sight)
            Ball ball = perception.getBallSaw();

            if (ball == null) {
                return new TurnAction(BALL_SEEK_STEP_DEG);
            } else {
                if (Math.abs(ball.getDirection()) < BALL_SEEK_ABS_RAD) {
                    return EmptyAction.instance;
                } else {
                    return new TurnAction(ball.getDirection());
                }
            }

        };
    }

    private TreeNode createSeeBallTree() {

        TreeNode seekGoalTree = createSeekGoalTree();
        createdTreeNodes.put("seekGoal", seekGoalTree);

        return (perception, knowledge, args) -> {
            Ball ball = perception.getBallSaw();

            if (ball == null) {
                return createdTreeNodes.get("travelToFirstFlag").getResultAction(perception, knowledge);
            } else {
                state = ScorePlayerStateType.FINISHED_TRAVELING;

                if (Math.abs(ball.getDirection()) < BALL_SEEK_ABS_RAD) {
                    if (ball.getDistance() < BALL_SEEK_DIST_OK) {
                        return seekGoalTree.getResultAction(perception, knowledge);
                    } else {
                        if (ball.getDistance() > 5 * BALL_SEEK_DIST_OK)
                            return new DashAction(BALL_CLOSE_DASH_POWER_MAX, true);

                        if (ball.getDistance() > 2 * BALL_SEEK_DIST_OK)
                            return new DashAction(BALL_CLOSE_DASH_POWER, true);

                        return new DashAction(BALL_CLOSE_DASH_POWER);
                    }
                } else {
                    return new TurnAction(Math.toDegrees(ball.getDirection()));
                }
            }
        };
    }

    private TreeNode createSeekGoalTree() {
        return (perception, knowledge, args) -> {

            Optional<Marker> goalMarker = PerceptionUtils.getMarker(perception, goalMarkerName);

            if (goalMarker.isEmpty()) {
                // Try to calculate
                Optional<Double> rotation = PlayerUtils.calcRotationToMarker(goalMarkerName, perception, knowledge);

                if (rotation.isEmpty()) {
                    // Failed to calculate. Kick ball a little to the side.
                    return new KickAction(KICK_POWER_SMALL, KICK_DIRECTION_SMALL_DEG, true);
                }

                lastKickCycleNumber = perception.getCycleNumber();
                return new KickAction(KICK_POWER_GOAL, Math.toDegrees(rotation.get()), true);
            }

            lastKickCycleNumber = perception.getCycleNumber();
            return new KickAction(KICK_POWER_GOAL, Math.toDegrees(goalMarker.get().getDirection()), true);
        };
    }
}
