package org.example.decision.controller.part.impl.midguard;

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
import java.util.concurrent.atomic.AtomicBoolean;

public class MidguardHighControllerPart extends ControllerPart {
    private boolean isTop;

    private static double BALL_DIR_ABD_RAD = Math.toRadians(15);
    private static double BALL_SEEK_ANG_DEG = 60;
    private static double DASH_TO_BALL_POWER = 60;
    private static double KICK_DISTANCE = 1;


    private static double X_WALL_ABS = 17;
    private static double Y_WALL = 0;

    private AtomicBoolean isInit;
    private double turnProgress = 0d;

    public MidguardHighControllerPart(boolean isTop, AtomicBoolean isInit) {
        this.isTop = isTop;
        this.isInit = isInit;
    }

    @Override
    public Optional<Action> decideAction(Perception perception, Knowledge knowledge, Map<ControllerDataType, ControllerData> data) {
        MoveEventListenerData moveEventListenerData = (MoveEventListenerData) data.get(ControllerDataType.MoveEventListener);
        BallTrackerData ballTrackerData = (BallTrackerData) data.get(ControllerDataType.BallTracker);
        IdleControllerData idleControllerData = (IdleControllerData) data.get(ControllerDataType.IdleController);

        if (moveEventListenerData.isHasMoved()) {
            // Reset state
            turnProgress = 0d;
            isInit.set(true);
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
                (ballPosition.getX() > (-X_WALL_ABS) && ballPosition.getX() < (X_WALL_ABS)) &&
                (
                        (Math.abs(ballPosition.getY()) < 0.1f) || (isTop && ballPosition.getY() < (Y_WALL)) ||
                        (!isTop && ballPosition.getY() > (Y_WALL))
                )
        ) {

            if (Math.abs(ball.getDirection()) > BALL_DIR_ABD_RAD) {
                return Optional.of(TurnAction.fromRadians(ball.getDirection()));
            }

            if (Math.abs(ball.getDistance()) > KICK_DISTANCE) {
                return Optional.of(new DashAction(DASH_TO_BALL_POWER, Math.toDegrees(ball.getDirection()), true));
            }


            String flag = "f p r t";

            if (!isTop) {
                if (knowledge.getTeamSide() == Side.LEFT) {
                    flag = "f p r b";
                } else {
                    flag = "f p l b";
                }
            } else {
                if (knowledge.getTeamSide() == Side.RIGHT) {
                    flag = "f p l t";
                }
            }

            boolean wasIsInit = isInit.get();

            // At start this should be reversed to allow attack players to advance into enemy territory!
            if (wasIsInit) {
                if (flag.contains("l"))
                    flag = flag.replace("l", "r");
                else
                    flag = flag.replace("r", "l");
            }

            Optional<Marker> centerMarker = PerceptionUtils.getMarker(perception, flag);
            if (centerMarker.isPresent()) {
                isInit.set(false);
                return Optional.of(new KickAction(wasIsInit ? 50 : 80, Math.toDegrees(centerMarker.get().getDirection())));
            }

            Optional<Double> directionToCenter = PlayerUtils.calcRotationToCoordinates(knowledge.getMarkersPositions().get(flag), perception);
            if (directionToCenter.isPresent()) {
                isInit.set(false);
                return Optional.of(new KickAction(wasIsInit ? 50 : 80, Math.toDegrees(directionToCenter.get())));
            }

            return Optional.of(new KickAction(20, 10));
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
