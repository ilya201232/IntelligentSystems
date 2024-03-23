package org.example.decision.controller.data.impl;

import lombok.Getter;
import lombok.Setter;
import org.example.decision.controller.data.ControllerData;
import org.example.model.unit.Vector2;

import static org.example.decision.controller.data.ControllerDataType.BallTracker;

@Getter
@Setter
public class BallTrackerData extends ControllerData {

    private Vector2 ballPosition;
    private Vector2 ballSpeedVector;
    private Vector2 lastSeenBallPosition;
    private Vector2 lastSeenBallSpeed;
    private Integer lastSeenBallCycle;

    public BallTrackerData() {
        super(BallTracker);
    }

    public BallTrackerData(Vector2 ballPosition, Vector2 ballSpeedVector, Vector2 lastSeenBallPosition, Vector2 lastSeenBallSpeed, Integer lastSeenBallCycle) {
        super(BallTracker);
        this.ballPosition = ballPosition;
        this.ballSpeedVector = ballSpeedVector;
        this.lastSeenBallPosition = lastSeenBallPosition;
        this.lastSeenBallSpeed = lastSeenBallSpeed;
        this.lastSeenBallCycle = lastSeenBallCycle;
    }
}
