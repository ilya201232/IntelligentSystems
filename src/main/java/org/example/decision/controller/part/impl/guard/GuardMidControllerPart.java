package org.example.decision.controller.part.impl.guard;

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
import org.example.model.unit.Side;
import org.example.model.unit.Vector2;
import org.example.sender.action.Action;
import org.example.sender.action.DashAction;
import org.example.sender.action.EmptyAction;
import org.example.sender.action.TurnAction;
import org.example.utils.PlayerUtils;

import java.util.Map;
import java.util.Optional;

public class GuardMidControllerPart extends ControllerPart {
    private boolean isTop;

    private static final double DELTA = 5;
    private static final double MAX_DISTANCE_TO_REF = 10;
    private static double BALL_SEEK_ANG_DEG = 60;
    private static double DASH_TO_BALL_POWER = 30;

    private static double X_WALL_ABS = 12;
    private static double Y_WALL = 0;

    private double turnProgress = 0d;

    public GuardMidControllerPart(boolean isTop) {
        this.isTop = isTop;
    }

    @Override
    public Optional<Action> decideAction(Perception perception, Knowledge knowledge, Map<ControllerDataType, ControllerData> data) {

        if (nextController == null) {
            throw new IllegalStateException("This controller part cannot be the last one!");
        }

        MoveEventListenerData moveEventListenerData = (MoveEventListenerData) data.get(ControllerDataType.MoveEventListener);
        BallTrackerData ballTrackerData = (BallTrackerData) data.get(ControllerDataType.BallTracker);
        IdleControllerData idleControllerData = (IdleControllerData) data.get(ControllerDataType.IdleController);

        if (moveEventListenerData.isHasMoved()) {
            // Reset state
            turnProgress = 0d;
        }

        Optional<Action> nextControllerAction = nextController.decideAction(perception, knowledge, data);

        if (nextControllerAction.isPresent())
            return nextControllerAction;

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

        if ((
                (knowledge.getTeamSide() == Side.LEFT && ballPosition.getX() < (-X_WALL_ABS + DELTA)) ||
                        (knowledge.getTeamSide() == Side.RIGHT && ballPosition.getX() > (X_WALL_ABS - DELTA))
        ) && (
                (Math.abs(ballPosition.getY()) < 0.1f) || (isTop && ballPosition.getY() < (Y_WALL + DELTA)) ||
                        (!isTop && ballPosition.getY() > (Y_WALL - DELTA))
        )) {
            Double distanceToRef = idleControllerData.getDistanceToRef();

            if (distanceToRef == null || distanceToRef > MAX_DISTANCE_TO_REF) {
                return Optional.of(new DashAction(10, -180d, true));
            }

            return Optional.of(new DashAction(DASH_TO_BALL_POWER, Math.toDegrees(ball.getDirection()), true));
        }

        return Optional.empty();
    }

    @Override
    public void baseInit(Knowledge knowledge) {
        if (knowledge.getTeamSide() == Side.RIGHT) {
            isTop = !isTop;
        }

        if (nextController != null)
            nextController.baseInit(knowledge);
    }
}
