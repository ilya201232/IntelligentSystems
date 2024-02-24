package org.example.sender.action;

import lombok.Getter;

@Getter
public class TurnAction extends Action {

    private final double moment;

    public TurnAction(double moment) {
        super(ActionType.TURN);
        this.moment = moment;
    }
}
