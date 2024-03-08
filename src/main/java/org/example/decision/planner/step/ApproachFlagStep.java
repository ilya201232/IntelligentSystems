package org.example.decision.planner.step;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Knowledge;
import org.example.model.Perception;
import org.example.model.object.Marker;
import org.example.model.unit.Vector2;
import org.example.sender.action.Action;
import org.example.sender.action.DashAction;
import org.example.sender.action.EmptyAction;
import org.example.sender.action.TurnAction;

import java.util.Optional;

@Getter
@Slf4j
public class ApproachFlagStep extends Step {

    private static final double DISTANCE_EPS = 3;
    private static final double ANGLE_EPS_RAD = Math.toRadians(20);


    private static final double MIN_SPEED = 1; // speed is usually < 1


    private static final double TURN_ANGLE_DEG = 30;
    private static final double DASH_POWER = 50;

    private final String markerName;
    private Vector2 markerPosition = null;

    public ApproachFlagStep(String markerName) {
        super(StepType.APPROACH_FLAG);
        this.markerName = markerName;
    }

    @Override
    public boolean isGoalSatisfied(Knowledge knowledge, Perception perception) {
        if (markerPosition == null) {
            markerPosition = knowledge.getMarkersPositions().get(markerName);
        }

        Optional<Marker> marker = perception.getMarkersSaw().stream().filter(m -> m.getId().equals(markerName)).findFirst();

        boolean isSatisfied = marker.filter(value -> value.getDistance() <= DISTANCE_EPS).isPresent();

        log.debug("Checking if flag {} is near: {}", markerName, isSatisfied);

        return isSatisfied;
    }

    @Override
    public Action onGoalSatisfied(Knowledge knowledge, Perception perception) {
        log.debug("Flag {} is near. Stopping...", markerName);
        return new DashAction(-DASH_POWER);
    }

    @Override
    public Action decideAction(Knowledge knowledge, Perception perception) {
        // TODO: for some reason player frequently looses flag from its sight...

        log.debug("Deciding appropriate action for getting to flag {}", markerName);

        // Step 1. Is flag visible?
        // This could be skipped by knowing player position. But algorithm is in need of adjusting, so using this for now...
        Optional<Marker> optionalMarker = perception.getMarkersSaw().stream().filter(marker -> marker.getId().equals(markerName)).findFirst();
        if (optionalMarker.isEmpty()) {
            // If flag is not visible - turn

            log.debug("Flag {} is not visible. Turning {} degrees clockwise.", markerName, TURN_ANGLE_DEG);

            return new TurnAction(TURN_ANGLE_DEG);
        }

        // Direction is always known!
        if (Math.abs(optionalMarker.get().getDirection()) <= ANGLE_EPS_RAD) {
            // If we are in the right direction

            // ... but too slow
            if (perception.getSensed().getSpeed().getAmount() < MIN_SPEED) {
                log.debug("Flag {} is visible (direction: |{}| < {}), but player speed ({}) is lower then {}. Dashing with power {}.",
                        markerName,
                        Math.toDegrees(optionalMarker.get().getDirection()),
                        Math.toDegrees(ANGLE_EPS_RAD),
                        perception.getSensed().getSpeed().getAmount(),
                        MIN_SPEED,
                        DASH_POWER
                );

                return new DashAction(DASH_POWER);
            }

            log.debug("Flag {} is visible (direction: |{}| < {}), and player is going towards it with speed {} >= {}",
                    markerName,
                    Math.toDegrees(optionalMarker.get().getDirection()),
                    Math.toDegrees(ANGLE_EPS_RAD),
                    perception.getSensed().getSpeed().getAmount(),
                    MIN_SPEED
            );

            // We don't check for distance since it's final action and is performed by onGoalSatisfied()
        } else {

            log.debug("Flag {} is visible, but player is not facing it. Turning {} (= direction to flag)",
                    markerName,
                    Math.toDegrees(optionalMarker.get().getDirection())
            );

            // If not in the right direction - turn to it)
            return new TurnAction(Math.toDegrees(optionalMarker.get().getDirection()));
        }

        log.debug("Not reachable.");

        return EmptyAction.instance;
    }
}
