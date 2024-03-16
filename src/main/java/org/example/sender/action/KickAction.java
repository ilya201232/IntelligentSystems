package org.example.sender.action;

import lombok.Getter;

@Getter
public class KickAction extends Action {

    private final double power;
    private final double direction;

    public KickAction(double power, double direction) {
        this(power, direction, false);
    }

    public KickAction(double power, double direction, boolean repeatable) {
        super(ActionType.KICK, repeatable);
        this.power = power;
        this.direction = direction;
    }
}
