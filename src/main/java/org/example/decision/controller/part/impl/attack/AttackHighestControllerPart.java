package org.example.decision.controller.part.impl.attack;

import lombok.RequiredArgsConstructor;
import org.example.decision.controller.data.ControllerData;
import org.example.decision.controller.data.ControllerDataType;
import org.example.decision.controller.data.impl.BallTrackerData;
import org.example.decision.controller.data.impl.IdleControllerData;
import org.example.decision.controller.data.impl.MoveEventListenerData;
import org.example.decision.controller.part.ControllerPart;
import org.example.model.Knowledge;
import org.example.model.Perception;
import org.example.model.object.Ball;
import org.example.model.object.Marker;
import org.example.model.unit.Side;
import org.example.model.unit.Vector2;
import org.example.sender.action.*;
import org.example.utils.PerceptionUtils;
import org.example.utils.PlayerUtils;

import java.util.Map;
import java.util.Optional;

public class AttackHighestControllerPart extends ControllerPart {
    private static double BALL_SEEK_ANG_DEG = 60;
    private static double DASH_TO_BALL_POWER = 70;
    private static double KICK_DISTANCE = 1;


    private double turnProgress = 0d;


    @Override
    public Optional<Action> decideAction(Perception perception, Knowledge knowledge, Map<ControllerDataType, ControllerData> data) {

        MoveEventListenerData moveEventListenerData = (MoveEventListenerData) data.get(ControllerDataType.MoveEventListener);
        BallTrackerData ballTrackerData = (BallTrackerData) data.get(ControllerDataType.BallTracker);
        IdleControllerData idleControllerData = (IdleControllerData) data.get(ControllerDataType.IdleController);

        if (moveEventListenerData.isHasMoved()) {
            // Reset state
            turnProgress = 0d;
        }

        Ball ball = perception.getBallSaw();
        Vector2 ballPosition;

        if (ball == null) {
            if (turnProgress < 360) {
                turnProgress += BALL_SEEK_ANG_DEG;
                return Optional.of(new TurnAction(BALL_SEEK_ANG_DEG));
            } else {
                return Optional.empty();
            }
        } else {
            turnProgress = 0;
            Optional<Vector2> position = PlayerUtils.calcAnotherObjectPosition(perception, ball);

            if (position.isEmpty()) {
                return Optional.of(EmptyAction.instance);
            } else {
                ballPosition = position.get();
            }
        }

        if ((knowledge.getTeamSide() == Side.LEFT && ballPosition.getX() > 36) ||
                (knowledge.getTeamSide() == Side.RIGHT && ballPosition.getX() < -36)){

            if (Math.abs(ball.getDistance()) > 3 * KICK_DISTANCE) {
                return Optional.of(new DashAction(DASH_TO_BALL_POWER, Math.toDegrees(ball.getDirection()), true));
            } else if (Math.abs(ball.getDistance()) > KICK_DISTANCE) {
                return Optional.of(new DashAction(DASH_TO_BALL_POWER, Math.toDegrees(ball.getDirection())));
            }

            String enemyGoalFlag = knowledge.getTeamSide() == Side.LEFT ? "g r" : "g l";

            Optional<Marker> marker = PerceptionUtils.getMarker(perception, enemyGoalFlag);
            if (marker.isPresent()) {
                return Optional.of(new KickAction(100, Math.toDegrees(marker.get().getDirection())));
            }

            Vector2 markerPosition = knowledge.getMarkersPositions().get(enemyGoalFlag);

            Optional<Double> directionToFlag = PlayerUtils.calcRotationToCoordinates(markerPosition, perception);
            if (directionToFlag.isPresent()) {
                return Optional.of(new KickAction(100, Math.toDegrees(directionToFlag.get())));
            }

            return Optional.of(new KickAction(20, 10));
        }

        return Optional.empty();
    }

    @Override
    public void baseInit(Knowledge knowledge) {
        if (nextController != null)
            nextController.baseInit(knowledge);
    }
}
