package org.example.decision.controller.part.impl.goalie;

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
import org.example.model.unit.Event;
import org.example.model.unit.EventType;
import org.example.model.unit.Side;
import org.example.model.unit.Vector2;
import org.example.sender.action.*;
import org.example.utils.PerceptionUtils;
import org.example.utils.PlayerUtils;

import java.util.Map;
import java.util.Optional;

public class GoalieHighControllerPart extends ControllerPart {
    private static double BALL_DIR_ABD_RAD = Math.toRadians(15);
    private static double BALL_SEEK_ANG_DEG = 60;
    private static double DASH_TO_BALL_POWER = 80;
    private static double CATCH_DISTANCE = 1.5;

    private double turnProgress = 0d;

    private boolean hasMoved = false;
    private Integer lastCatchAttempt = null;


    @Override
    public Optional<Action> decideAction(Perception perception, Knowledge knowledge, Map<ControllerDataType, ControllerData> data) {
        MoveEventListenerData moveEventListenerData = (MoveEventListenerData) data.get(ControllerDataType.MoveEventListener);
        BallTrackerData ballTrackerData = (BallTrackerData) data.get(ControllerDataType.BallTracker);
        IdleControllerData idleControllerData = (IdleControllerData) data.get(ControllerDataType.IdleController);

        if (moveEventListenerData.isHasMoved()) {
            // Reset state
            hasMoved = false;
            lastCatchAttempt = null;

            turnProgress = 0d;
        }

        if (hasMoved) {
            hasMoved = false;
            lastCatchAttempt = null;

            Optional<Marker> centerMarker = PerceptionUtils.getMarker(perception, "f c");
            if (centerMarker.isPresent()) {
                return Optional.of(new KickAction(100, Math.toDegrees(centerMarker.get().getDirection())));
            }

            Optional<Double> directionToCenter = PlayerUtils.calcRotationToCoordinates(new Vector2(), perception);
            if (directionToCenter.isPresent()) {
                return Optional.of(new KickAction(100, Math.toDegrees(directionToCenter.get())));
            }

            return Optional.of(new KickAction(50, 0));
        }

        Event event = knowledge.getHeardEvents().get(EventType.GOALIE_CATCH_BALL);
        if (lastCatchAttempt != null && event != null && event.getCycleNumber() > lastCatchAttempt) {
            hasMoved = true;
            return Optional.of(new MoveAction(knowledge.getStartPosition()));
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

        if (
                (knowledge.getTeamSide() == Side.LEFT && ballPosition.getX() < -36) ||
                        (knowledge.getTeamSide() == Side.RIGHT && ballPosition.getX() > 36)
        ) {

            if (Math.abs(ball.getDirection()) > BALL_DIR_ABD_RAD) {
                return Optional.of(TurnAction.fromRadians(ball.getDirection()));
            }

            if (Math.abs(ball.getDistance()) > CATCH_DISTANCE) {
                return Optional.of(new DashAction(DASH_TO_BALL_POWER, Math.toDegrees(ball.getDirection()), true));
            }

            lastCatchAttempt = perception.getCycleNumber();
            return Optional.of(new CatchAction(Math.toDegrees(ball.getDirection()), true));
        }

        return Optional.empty();
    }

    @Override
    public void baseInit(Knowledge knowledge) {
        if (nextController != null)
            nextController.baseInit(knowledge);
    }
}
