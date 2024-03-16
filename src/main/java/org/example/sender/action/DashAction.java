package org.example.sender.action;

import lombok.Getter;

@Getter
public class DashAction extends Action {

    private final double power;
    private final Double direction;

    public DashAction(double power) {
        this(power, false);
    }

    public DashAction(double power, boolean repeatable) {
        this(power, null, repeatable);
    }

    public DashAction(double power, Double direction) {
        this(power, direction, false);
    }

    public DashAction(double power, Double direction, boolean repeatable) {
        super(ActionType.DASH, repeatable);
        this.power = power;
        this.direction = direction;
    }


}
