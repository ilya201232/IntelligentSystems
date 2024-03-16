package org.example.sender.action;

import lombok.Getter;

@Getter
public class TurnAction extends Action {

    private final double moment;

    public TurnAction(double moment) {
        this(moment, false);
    }

    public TurnAction(double moment, boolean repeatable) {
        super(ActionType.TURN, repeatable);
        this.moment = moment;
    }
}
