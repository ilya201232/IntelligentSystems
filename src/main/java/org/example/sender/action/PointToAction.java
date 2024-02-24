package org.example.sender.action;

import lombok.Getter;

@Getter
public class PointToAction extends Action {

    private final double distance;
    private final double direction;

    public PointToAction(double distance, double direction) {
        super(ActionType.POINT_TO);
        this.distance = distance;
        this.direction = direction;
    }
}
