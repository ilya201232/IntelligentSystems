package org.example.sender.action;

import lombok.Getter;

@Getter
public class PointToAction extends Action {

    private final double distance;
    private final double direction;

    public PointToAction(double distance, double direction) {
        this(distance, direction, false);
    }

    public PointToAction(double distance, double direction, boolean repeatable) {
        super(ActionType.POINT_TO, repeatable);
        this.distance = distance;
        this.direction = direction;
    }
}
