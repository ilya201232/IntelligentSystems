package org.example.decision.planner.step;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Knowledge;
import org.example.model.Perception;
import org.example.model.object.Ball;
import org.example.model.object.Marker;
import org.example.model.unit.Event;
import org.example.model.unit.EventType;
import org.example.model.unit.Side;
import org.example.model.unit.Vector2;
import org.example.sender.action.*;

import java.util.Optional;

@Getter
@Slf4j
public class KickBallForGoalStep extends Step {

    private static final double KICK_DISTANCE_EPS = 1;
    private static final double ANGLE_EPS_RAD = Math.toRadians(20);
    private static final double MIN_SPEED = 1; // speed is usually < 1


    private static final double TURN_ANGLE_DEG = 30;
    private static final double DASH_POWER = 50;
    private static final double KICK_POWER = 100;

    private static final double KICK_POWER_SMALL = 10;
    private static final double KICK_DIRECTION_SMALL_DEG = 45;

    private Side goalSide;
    private Vector2 markerPosition = null;

    // State
    private int stepStartCycleNumber = -1;

    public KickBallForGoalStep() {
        super(StepType.KICK_BALL_FOR_GOAL);
    }

    @Override
    public boolean isGoalSatisfied(Knowledge knowledge, Perception perception) {
        // Initialising marker position
        if (markerPosition == null) {
            markerPosition = knowledge.getMarkersPositions().get("g " + (goalSide == Side.LEFT ? "l" : "r"));
        }

        if (goalSide == null) {
            if (knowledge.getTeamSide() == null) {
                throw new IllegalStateException("Don't know player team to start planning");
            } else {
                goalSide = Side.getOpposite(knowledge.getTeamSide());
            }
        }

        if (stepStartCycleNumber == -1) {
            stepStartCycleNumber = perception.getCycleNumber();
        }

        Event goalEvent = knowledge.getHeardEvents().get(EventType.GOAL);

        if (goalEvent == null) {
            log.debug("GOAL event has not been found. Step is not done.");
            return false;
        }

        if (goalEvent.getCycleNumber() <= stepStartCycleNumber) {
            log.debug("GOAL event has been found, but happened before this step started executing ({} <= {})", goalEvent.getCycleNumber(), stepStartCycleNumber);
            return false;
        }

        /*
        // Commented since goal indication is enough without cycle number
        if (goalEvent.get().getValue() >= perception.getCycleNumber()) {
            log.debug("Received perception ");
            return false;
        }*/



        if (goalEvent.getSide() != Side.getOpposite(goalSide)) {
            log.debug("Received goal, but on the wrong side {}", goalEvent.getSide());
            return false;
        }

        log.debug("Received goal on {} side", goalEvent.getSide());
        return true;
    }

    @Override
    public Action onGoalSatisfied(Knowledge knowledge, Perception perception) {
        // Clean state
        stepStartCycleNumber = -1;

        log.debug("Goal is satisfied. Moving player to start position.");
        return new MoveAction(knowledge.getStartPosition());
    }

    @Override
    public Action decideAction(Knowledge knowledge, Perception perception) {

        Ball ball = perception.getBallSaw();
        if (ball == null) {
            log.debug("Ball is not visible. Turning {} degrees clockwise.", TURN_ANGLE_DEG);
            // If ball is not visible - turn
            return new TurnAction(TURN_ANGLE_DEG);
        }

        // Check if player is within "kickable" distance from ball
        if (ball.getDistance() == null || ball.getDistance() > KICK_DISTANCE_EPS) {
            // Direction is always known!
            if (Math.abs(ball.getDirection()) <= ANGLE_EPS_RAD) {
                // If we are in the right direction

                // ... but too slow
                if (perception.getSensed().getSpeed().getAmount() < MIN_SPEED) {
                    // TODO - maybe adjust power to distance?

                    log.debug("Ball is visible and too far (distance: {} (can be null)), and direction is right ({}). Speed is less than normal ({} < {}) so dashing with power {}.",
                            ball.getDistance(),
                            Math.toDegrees(ball.getDirection()),
                            perception.getSensed().getSpeed().getAmount(),
                            MIN_SPEED,
                            DASH_POWER
                    );
                    return new DashAction(DASH_POWER);
                }

                log.debug("Ball is visible and too far (distance: {} (can be null)), and direction is right ({}). Speed is normal ({}) so doing no action for now.",
                        ball.getDistance(),
                        Math.toDegrees(ball.getDirection()),
                        perception.getSensed().getSpeed().getAmount()
                );
                return EmptyAction.instance;
            }

            log.debug("Ball is visible and too far (distance: {} (can be null)), but direction is wrong (|{}| > {}). Turning to face it.",
                    ball.getDistance(),
                    Math.toDegrees(ball.getDirection()),
                    Math.toDegrees(ANGLE_EPS_RAD)
            );
            // If not in the right direction - turn to it)
            return new TurnAction(Math.toDegrees(ball.getDirection()));
        }

        // Slow down...
        if (perception.getSensed().getSpeed().getAmount() > MIN_SPEED) {
            log.debug("Ball is near, but player is too fast, Slowing down by dashing with power {}.",
                    -DASH_POWER
            );
            // TODO - maybe adjust power to current speed?
            return new DashAction(-DASH_POWER);
        }

        String goalMarkerName = "g " + (goalSide == Side.LEFT ? "l" : "r");
        Optional<Marker> optionalMarker = perception.getMarkersSaw().stream().filter(marker -> marker.getId().equals(goalMarkerName)).findFirst();

        //noinspection OptionalIsPresent
        if (optionalMarker.isEmpty()) {

            log.debug("Ball is kickable, but goal is not visible. Kicking ball by power {} and angle {} deg.",
                    KICK_POWER_SMALL,
                    KICK_DIRECTION_SMALL_DEG
            );

            // If goal flag is not visible - kick ball slightly
            return new KickAction(KICK_POWER_SMALL, KICK_DIRECTION_SMALL_DEG);
        }

        log.debug("Ball is kickable, and goal is visible. Kicking ball by power {} and direction {} deg.",
                KICK_POWER,
                Math.toDegrees(optionalMarker.get().getDirection())
        );
        return new KickAction(KICK_POWER, Math.toDegrees(optionalMarker.get().getDirection()));
    }
}
