package org.example.sender.action;

import lombok.Getter;
import org.example.sender.action.Action;
import org.example.sender.action.ActionType;

@Getter
public class KickAction extends Action {

    private final double power;
    private final double direction;

    public KickAction(double power, double direction) {
        super(ActionType.KICK);
        this.power = power;
        this.direction = direction;
    }
}
