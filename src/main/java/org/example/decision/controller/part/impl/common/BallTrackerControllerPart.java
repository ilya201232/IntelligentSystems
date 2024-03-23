package org.example.decision.controller.part.impl.common;

import org.example.decision.controller.data.ControllerData;
import org.example.decision.controller.data.ControllerDataType;
import org.example.decision.controller.data.impl.BallTrackerData;
import org.example.decision.controller.part.ControllerPart;
import org.example.model.Knowledge;
import org.example.model.Perception;
import org.example.model.object.Ball;
import org.example.model.unit.Side;
import org.example.model.unit.Vector2;
import org.example.sender.action.Action;
import org.example.utils.PlayerUtils;

import java.util.Map;
import java.util.Optional;

public class BallTrackerControllerPart extends ControllerPart {

    private Perception lastPerception;
    private Vector2 lastSeenBallPosition;
    private Vector2 lastSeenBallSpeed;
    private Integer lastSeenBallCycle = null;

    @Override
    public Optional<Action> decideAction(Perception perception, Knowledge knowledge, Map<ControllerDataType, ControllerData> data) {
        if (nextController == null) {
            throw new IllegalStateException("This controller part cannot be the last one!");
        }

        Ball ball = perception.getBallSaw();
        Optional<Vector2> ballPosition = PlayerUtils.calcAnotherObjectPosition(perception, ball);

        Vector2 ballSpeed = null;

        if (lastPerception != null) {
            Ball lastBall = lastPerception.getBallSaw();
            Optional<Vector2> lastBallPosition = PlayerUtils.calcAnotherObjectPosition(lastPerception, lastBall);

            if (ballPosition.isPresent() && lastBallPosition.isPresent()) {
                ballSpeed = lastBallPosition.get().minus(ballPosition.get()).divide(perception.getCycleNumber() - lastPerception.getCycleNumber());

                lastSeenBallPosition = ballPosition.get();
                lastSeenBallSpeed = ballSpeed;
                lastSeenBallCycle = perception.getCycleNumber();
            }
        }


        data.put(
                ControllerDataType.BallTracker,
                new BallTrackerData(
                        ballPosition.orElse(null),
                        ballSpeed,
                        lastSeenBallPosition,
                        lastSeenBallSpeed,
                        lastSeenBallCycle)
        );

        lastPerception = perception;

        return nextController.decideAction(perception, knowledge, data);
    }

    @Override
    public void baseInit(Knowledge knowledge) {
        if (nextController != null)
            nextController.baseInit(knowledge);
    }
}
