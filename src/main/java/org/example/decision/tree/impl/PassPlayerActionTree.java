package org.example.decision.tree.impl;

import org.example.decision.tree.ActionTree;
import org.example.decision.tree.TreeNode;
import org.example.decision.tree.impl.data.PassPlayerStateType;
import org.example.model.Knowledge;
import org.example.model.Perception;
import org.example.model.object.Ball;
import org.example.model.object.Marker;
import org.example.model.object.Player;
import org.example.model.unit.*;
import org.example.sender.action.*;
import org.example.utils.PerceptionUtils;
import org.example.utils.PlayerUtils;

import java.util.Optional;

public class PassPlayerActionTree extends ActionTree {

    private static final double MARKER_SEEK_STEP_DEG = 90;
    private static final double MARKER_SEEK_ABS_RAD = Math.toRadians(10);
    private static final double MARKER_SEEK_DIST_OK = 1.5;
    private static final double MARKER_CLOSE_DASH_POWER = 80;

    private static final double BALL_SEEK_STEP_DEG = 90;
    private static final double BALL_SEEK_ABS_RAD = Math.toRadians(10);
    private static final double BALL_SEEK_DIST_OK = 1;
    private static final double BALL_CLOSE_DASH_POWER = 70;

    private static final double KICK_POWER_SMALL = 5;
    private static final double KICK_DIRECTION_SMALL_DEG = 90;

    private static final double PLAYER_SEEK_ABS_RAD = Math.toRadians(20);
    private static final double KICK_BALL_TO_PLAYER_POWER = 50;


    private String markerName = "";

    private PassPlayerStateType state = PassPlayerStateType.WAITING_FOR_PLAY_ON;
    private Boolean hasKicked = false;
    private Integer kickBallCycle = Integer.MAX_VALUE;
    private Boolean isScorePlayerTop = false;

    public PassPlayerActionTree(Knowledge knowledgeGlobal) {
        super(knowledgeGlobal);
    }

    @Override
    public boolean checkMinimumConditionForPassingPerception(Perception perception) {
        return perception.getSensed() == null || perception.getMarkersSaw().isEmpty();
    }

    @Override
    protected TreeNode createTreeRoot() {
        TreeNode travelToFlagTree = createTravelToFlagTree();
        createdTreeNodes.put("travelToFlag", travelToFlagTree);

        return (perception, knowledge, args) -> {

            if (markerName.isEmpty()) {
                markerName = "f p " + (knowledge.getTeamSide() == Side.LEFT ? "l" : "r") + " c";
            }

            if (state == PassPlayerStateType.WAITING_FOR_PLAY_ON) {

                if (knowledge.getCurrentPlayMode().getPlayModeType() == PlayModeType.PLAY_ON) {
                    state = PassPlayerStateType.TRAVELLING_TO_FLAG;
                    return travelToFlagTree.getResultAction(perception, knowledge);
                }

                return EmptyAction.instance;
            } else if (state == PassPlayerStateType.WAITING_FOR_PLAY_ON_LATE) {

                Event goalEvent = knowledge.getHeardEvents().get(EventType.GOAL);

                if (
                        goalEvent != null && goalEvent.getCycleNumber() > kickBallCycle &&
                                knowledge.getCurrentPlayMode().getPlayModeType() == PlayModeType.PLAY_ON && knowledge.getCurrentPlayMode().getCreatedAt() > kickBallCycle
                ) {
                    state = PassPlayerStateType.TRAVELLING_TO_FLAG;
                    return travelToFlagTree.getResultAction(perception, knowledge);
                }

                return EmptyAction.instance;
            } else {
                return travelToFlagTree.getResultAction(perception, knowledge);
            }
        };
    }

    private TreeNode createTravelToFlagTree() {

        TreeNode travelToBallTree = createTravelToBallTree();
        createdTreeNodes.put("travelToBall", travelToBallTree);

        return (perception, knowledge, args) -> {


            if (state == PassPlayerStateType.TRAVELLING_TO_FLAG || state == PassPlayerStateType.TRAVELLING_TO_FLAG_LATE) {

                Optional<Marker> marker = PerceptionUtils.getMarker(perception, markerName);

                if (state == PassPlayerStateType.TRAVELLING_TO_FLAG_LATE) {
                    Event goalEvent = knowledge.getHeardEvents().get(EventType.GOAL);

                    if (goalEvent != null && goalEvent.getCycleNumber() > kickBallCycle && knowledge.getCurrentPlayMode().getPlayModeType() == PlayModeType.KICK_OFF) {
                        state = PassPlayerStateType.WAITING_FOR_PLAY_ON_LATE;
                        return new MoveAction(knowledge.getMarkersPositions().get(markerName));
                    }

                } else {
                    // If new action cycle - reset values
                    hasKicked = false;
                    kickBallCycle = Integer.MAX_VALUE;
                }

                if (marker.isEmpty()) {
                    Optional<Double> rotToMarkerRad = PlayerUtils.calcRotationToMarker(markerName, perception, knowledge);

                    return new TurnAction(rotToMarkerRad.map(Math::toDegrees).orElse(MARKER_SEEK_STEP_DEG));
                } else {
                    if (Math.abs(marker.get().getDirection()) < MARKER_SEEK_ABS_RAD) {
                        // Facing marker. Need to get to it

                        if (marker.get().getDistance() < MARKER_SEEK_DIST_OK) {
                            if (state == PassPlayerStateType.TRAVELLING_TO_FLAG) {
                                state = PassPlayerStateType.TRAVELLING_TO_BALL;
                                return travelToBallTree.getResultAction(perception, knowledge);
                            } else {
                                state = PassPlayerStateType.WAITING_FOR_PLAY_ON_LATE;
                                return treeRoot.getResultAction(perception, knowledge);
                            }
                        } else {
                            if (marker.get().getDistance() > 2 * MARKER_SEEK_DIST_OK) {
                                return new DashAction(MARKER_CLOSE_DASH_POWER, true);
                            }
                            return new DashAction(MARKER_CLOSE_DASH_POWER, true);
                        }

                    } else {
                        return new TurnAction(Math.toDegrees(marker.get().getDirection()));
                    }
                }
            } else {
                return travelToBallTree.getResultAction(perception, knowledge);
            }
        };
    }

    private TreeNode createTravelToBallTree() {

        TreeNode searchPlayerTree = createSearchPlayerTree();
        createdTreeNodes.put("searchPlayer", searchPlayerTree);

        return (perception, knowledge, args) -> {

            if (state == PassPlayerStateType.TRAVELLING_TO_BALL) {
                Ball ball = perception.getBallSaw();

                if (ball == null) {
                    // TODO: maybe not enough
                    return new TurnAction(BALL_SEEK_STEP_DEG);
                } else {
                    if (Math.abs(ball.getDirection()) < BALL_SEEK_ABS_RAD) {
                        if (ball.getDistance() < BALL_SEEK_DIST_OK) {
                            state = PassPlayerStateType.SEARCHING_PLAYER;
                            return searchPlayerTree.getResultAction(perception, knowledge);
                        } else {
                            if (ball.getDistance() > 2 * BALL_SEEK_DIST_OK) {
                                return new DashAction(BALL_CLOSE_DASH_POWER, true);
                            }
                            return new DashAction(BALL_CLOSE_DASH_POWER, true);
                        }
                    } else {
                        return new TurnAction(Math.toDegrees(ball.getDirection()));
                    }
                }
            } else {
                return searchPlayerTree.getResultAction(perception, knowledge);
            }
        };
    }

    private TreeNode createSearchPlayerTree() {

        // TODO: how to distinguish receiving player...
        //  for now just get any player
        return (perception, knowledge, args) -> {

            Optional<Player> player = perception.getTeammatesSaw().stream().findAny();

            if (player.isEmpty()) {
                state = PassPlayerStateType.TRAVELLING_TO_BALL;
                return new KickAction(KICK_POWER_SMALL, KICK_DIRECTION_SMALL_DEG * (isScorePlayerTop ? 1 : -1) * (knowledge.getTeamSide() == Side.LEFT ? -1 : 1), true);
//                return new TurnAction(MARKER_SEEK_STEP_DEG);
            } else {
                if (Math.abs(player.get().getDirection()) < PLAYER_SEEK_ABS_RAD) {
                    // Need to first kick, then shout or else the ball gets too far to kick it
                    if (hasKicked) {
                        state = PassPlayerStateType.TRAVELLING_TO_FLAG_LATE;
                        return new SayAction("go", true);
                    } else {
                        hasKicked = true;
                        kickBallCycle = perception.getCycleNumber();
                        // TODO: Maybe adjust power by distance to player

                        Optional<Vector2> playerPosition = PlayerUtils.calcAnotherObjectPosition(perception, knowledge, player.get());

                        if (playerPosition.isEmpty())
                            return new KickAction(KICK_BALL_TO_PLAYER_POWER, Math.toDegrees(player.get().getDirection()), true);

                        isScorePlayerTop = playerPosition.get().getY() < 0;

                        Vector2 markerPosition = knowledge.getMarkersPositions().get(
                                "f p " + (knowledge.getTeamSide() == Side.LEFT ? "r" : "l") + " " + (isScorePlayerTop ? "t" : "b")
                        );

                        Optional<Double> targetAngle = PlayerUtils.calcRotationToCoordinates(Vector2.getPositionInMiddle(playerPosition.get(), markerPosition), perception, knowledge);

                        if (targetAngle.isEmpty()) {
                            return new KickAction(KICK_BALL_TO_PLAYER_POWER, Math.toDegrees(player.get().getDirection()), true);
                        }

                        return new KickAction(KICK_BALL_TO_PLAYER_POWER, Math.toDegrees(targetAngle.get()), true);
                    }
                } else {
                    return new TurnAction(Math.toDegrees(player.get().getDirection()));
                }
            }
        };
    }
}
