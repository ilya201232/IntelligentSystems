package org.example.decision.controller.part.impl.common;

import lombok.RequiredArgsConstructor;
import org.example.decision.controller.data.ControllerData;
import org.example.decision.controller.data.ControllerDataType;
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
import org.example.sender.action.TurnAction;
import org.example.utils.PlayerUtils;

import java.util.Map;
import java.util.Optional;

public class IdleControllerPart extends ControllerPart {

    private static final double FLAG_SEEK_ANG_DEG = 30;
    private static final double BALL_SEEK_ANG_DEG = 60;
    private static final double REF_POINT_DISTANCE_DELTA = 3;
    private static final double DASH_POWER = 60;
    private static final double BALL_DIR_DELTA_RAD = Math.toRadians(1);
    private static final double REF_DIR_DELTA_RAD = Math.toRadians(20);

    private Vector2 refPoint;
    private final boolean forceToRefOnInit;

    private boolean isInit = true;

    public IdleControllerPart(Vector2 relativeRefPoint, boolean forceToRefOnInit) {
        this.refPoint = relativeRefPoint;
        this.forceToRefOnInit = forceToRefOnInit;
    }

    @Override
    public Optional<Action> decideAction(Perception perception, Knowledge knowledge, Map<ControllerDataType, ControllerData> data) {
        if (knowledge.isGoalie() && knowledge.getTeamSide() == Side.RIGHT) {
            int pop = 0;
        }

        if (nextController == null) {
            throw new IllegalStateException("This controller part cannot be the last one!");
        }
        MoveEventListenerData moveEventListenerData = (MoveEventListenerData) data.get(ControllerDataType.MoveEventListener);

        if (moveEventListenerData.isHasMoved()) {
            // Reset state
            isInit = true;
        }

        Optional<Vector2> playerPosition = PlayerUtils.calcThisPlayerPosition(perception);
        // Try to return to ref point
        Double distanceToRef = playerPosition.isEmpty() ? null : playerPosition.get().getDistance(refPoint);

        data.put(
                ControllerDataType.IdleController,
                new IdleControllerData(
                        playerPosition.orElse(null),
                        distanceToRef)
        );

        if (!forceToRefOnInit || !isInit) {
            Optional<Action> nextControllerAction = nextController.decideAction(perception, knowledge, data);

            if (nextControllerAction.isPresent())
                return nextControllerAction;
        }

        if (playerPosition.isEmpty()) {
            // Cannot calculate position => need to rotate a bit
            return Optional.of(new TurnAction(FLAG_SEEK_ANG_DEG));
        }

        if (forceToRefOnInit) {
            Optional<Double> directionToRef = PlayerUtils.calcRotationToCoordinates(refPoint, perception);

            if (directionToRef.isEmpty()) {
                // Cannot calculate direction => need to rotate a bit
                return Optional.of(new TurnAction(FLAG_SEEK_ANG_DEG));
            }

            if (Math.abs(directionToRef.get()) > REF_DIR_DELTA_RAD) {
                return Optional.of(TurnAction.fromRadians(directionToRef.get()));
            }
        }

        if (distanceToRef > REF_POINT_DISTANCE_DELTA) {
            Optional<Double> directionToRef = PlayerUtils.calcRotationToCoordinates(refPoint, perception);

            if (directionToRef.isEmpty()) {
                // Cannot calculate direction => need to rotate a bit
                return Optional.of(new TurnAction(FLAG_SEEK_ANG_DEG));
            }

            return Optional.of(new DashAction(DASH_POWER, Math.toDegrees(directionToRef.get()), true));
        }

        isInit = false;

        // Watch the ball

        // TODO: This is done by controllers up top
        Ball ball = perception.getBallSaw();

        if (ball == null) {
            return Optional.of(new TurnAction(BALL_SEEK_ANG_DEG));
        }

        if (Math.abs(ball.getDirection()) > BALL_DIR_DELTA_RAD) {
            return Optional.of(TurnAction.fromRadians(ball.getDirection()));
        }

        return Optional.empty();
    }

    @Override
    public void baseInit(Knowledge knowledge) {
        if (knowledge.getTeamSide() == Side.RIGHT) {
            refPoint = refPoint.multiply(-1);
        }

        if (nextController != null)
            nextController.baseInit(knowledge);
    }
}
