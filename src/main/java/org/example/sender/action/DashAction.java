package org.example.sender.action;

import lombok.Getter;
import org.example.sender.action.Action;
import org.example.sender.action.ActionType;

@Getter
public class DashAction extends Action {

    private final double power;
    private final Double direction;

    public DashAction(double power, Double direction) {
        super(ActionType.DASH);
        this.power = power;
        this.direction = direction;
    }

    public DashAction(double power) {
        super(ActionType.DASH);
        this.power = power;
        this.direction = null;
    }
}
