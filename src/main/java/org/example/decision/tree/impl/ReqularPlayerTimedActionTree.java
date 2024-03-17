package org.example.decision.tree.impl;

import org.example.decision.tree.TimedActionTree;
import org.example.decision.tree.TreeNode;
import org.example.decision.tree.impl.data.RegularPlayerTimedStateType;
import org.example.model.Knowledge;
import org.example.model.Perception;
import org.example.model.object.Ball;
import org.example.model.object.Marker;
import org.example.model.unit.Event;
import org.example.model.unit.EventType;
import org.example.model.unit.PlayModeType;
import org.example.model.unit.Vector2;
import org.example.sender.action.DashAction;
import org.example.sender.action.KickAction;
import org.example.sender.action.MoveAction;
import org.example.sender.action.TurnAction;
import org.example.utils.PerceptionUtils;
import org.example.utils.PlayerUtils;

import java.util.Optional;
import java.util.UUID;

public class ReqularPlayerTimedActionTree extends TimedActionTree {

    private static final double MARKER_SEEK_STEP_DEG = 90;
    private static final double MARKER_SEEK_ABS_RAD = Math.toRadians(10);
    private static final double MARKER_SEEK_DIST_OK = 1.5;
    private static final double MARKER_CLOSE_DASH_POWER = 80;

    private static final double BALL_SEEK_STEP_DEG = 90;
    private static final double BALL_SEEK_ABS_RAD = Math.toRadians(10);
    private static final double BALL_SEEK_DIST_OK = 1;
    private static final double BALL_CLOSE_DASH_POWER = 70;

    private static final double KICK_POWER_BIG = 100;
    private static final double KICK_POWER_MID = 70;
    private static final double KICK_BALL_SIDE = 40;
    private static final double KICK_POWER_SMALL = 5;
    private static final double KICK_DIRECTION_SMALL_DEG = 90;


    private String flagName = "";
    private String goalFlagName = "";

    private RegularPlayerTimedStateType state = RegularPlayerTimedStateType.WAITING_FOR_PLAY_ON;
    private Integer lastKickMoment = Integer.MAX_VALUE;

    private final UUID globalTimerUUID;
    private final UUID sideTimerUUID;

    private static final Integer GLOBAL_TIMER_POINT = 150;
    private static final Integer SIDE_TIMER_POINT = 50;

    public ReqularPlayerTimedActionTree(Knowledge knowledgeGlobal) {
        super(knowledgeGlobal);

        globalTimerUUID = createTimer();
        sideTimerUUID = createTimer();
    }

    @Override
    public boolean checkMinimumConditionForPassingPerception(Perception perception) {
        return perception.getSensed() == null || perception.getMarkersSaw().isEmpty();
    }

    @Override
    protected TreeNode createTreeRoot() {
        TreeNode goingToFlagTree = createGoingToFlagTree();

        return (perception, knowledge, args) -> {

            if (flagName.isEmpty()) {
                flagName = "f p " + knowledge.getTeamSide() + " c";
                goalFlagName = "g " + knowledge.getTeamSide().getOpposite();
            }


            Event goalEvent = knowledge.getHeardEvents().get(EventType.GOAL);

            if (goalEvent != null && goalEvent.getCycleNumber() > lastKickMoment) {
                resetAllTimers();

                return new MoveAction(knowledge.getStartPosition());
            }

            if (state == RegularPlayerTimedStateType.WAITING_FOR_PLAY_ON) {
                resetAllTimers();

                if (knowledge.getCurrentPlayMode().getPlayModeType() == PlayModeType.PLAY_ON) {
                    state = RegularPlayerTimedStateType.GOING_TO_FLAG;
                    return goingToFlagTree.getResultAction(perception, knowledge);
                }
            }

            return goingToFlagTree.getResultAction(perception, knowledge);
        };
    }

    private TreeNode createGoingToFlagTree() {
        TreeNode goingToBallTree = createGoingToBallTree();

        return (perception, knowledge, args) -> {

            if (state == RegularPlayerTimedStateType.GOING_TO_BALL) {
                return goingToBallTree.getResultAction(perception, knowledge);
            }

            Optional<Marker> marker = PerceptionUtils.getMarker(perception, flagName);

            if (marker.isEmpty()) {
                Optional<Double> rotation = PlayerUtils.calcRotationToMarker(flagName, perception, knowledge);

                if (rotation.isEmpty()) {
                    return new TurnAction(MARKER_SEEK_STEP_DEG);
                }

                return TurnAction.fromRadians(rotation.get());
            }

            if (Math.abs(marker.get().getDirection()) > MARKER_SEEK_ABS_RAD) {
                return TurnAction.fromRadians(marker.get().getDirection());
            }

            if (marker.get().getDistance() > MARKER_SEEK_DIST_OK) {
                return new DashAction(MARKER_CLOSE_DASH_POWER, true);
            }

            state = RegularPlayerTimedStateType.GOING_TO_BALL;
            return goingToBallTree.getResultAction(perception, knowledge);
        };
    }

    private TreeNode createGoingToBallTree() {
        TreeNode kickTree = createKickTree();

        return (perception, knowledge, args) -> {

            if (state == RegularPlayerTimedStateType.KICKING_TO_GOAL) {
                return kickTree.getResultAction(perception, knowledge);
            }

            Ball ball = perception.getBallSaw();

            if (ball == null) {
                return new TurnAction(BALL_SEEK_STEP_DEG);
            }

            if (Math.abs(ball.getDirection()) > BALL_SEEK_ABS_RAD) {
                return TurnAction.fromRadians(ball.getDirection());
            }

            if (ball.getDistance() > BALL_SEEK_DIST_OK) {
                return new DashAction(BALL_CLOSE_DASH_POWER, true);
            }

            state = RegularPlayerTimedStateType.KICKING_TO_GOAL;
            resetTimer(sideTimerUUID);
            return kickTree.getResultAction(perception, knowledge);
        };
    }

    private TreeNode createKickTree() {
        return (perception, knowledge, args) -> {

            double direction = 0;

            Optional<Marker> goalMarker = PerceptionUtils.getMarker(perception, goalFlagName);

            if (goalMarker.isPresent()) {
                direction = goalMarker.get().getDirection();
            } else {
                Optional<Double> directionToGoal = PlayerUtils.calcRotationToMarker(goalFlagName, perception, knowledge);

                if (directionToGoal.isEmpty()) {
                    return new KickAction(KICK_POWER_SMALL, KICK_DIRECTION_SMALL_DEG);
                }

                direction = directionToGoal.get();
            }


            if (getTimerValue(sideTimerUUID).orElse(0) > SIDE_TIMER_POINT) {
                // Kick to the side
                resetTimer(sideTimerUUID);

                Optional<Vector2> playerPosition = PlayerUtils.calcThisPlayerPosition(perception, knowledge);

                if (playerPosition.isPresent()) {

                    if (playerPosition.get().getY() < 0) {
                        direction += Math.PI;
                    } else {
                        direction -= Math.PI;
                    }

                    if (direction > Math.PI) {
                        direction -= 2 * Math.PI;
                    } else if (direction < -Math.PI) {
                        direction += 2 * Math.PI;
                    }

                }
            }

            double power = KICK_POWER_BIG;

            if (getTimerValue(globalTimerUUID).orElse(0) <= GLOBAL_TIMER_POINT) {
                power = KICK_POWER_MID;
            }

            if (getTimerValue(sideTimerUUID).orElse(0) > SIDE_TIMER_POINT) {
                power = KICK_BALL_SIDE;
            }

            return new KickAction(power, direction);
        };
    }





}
