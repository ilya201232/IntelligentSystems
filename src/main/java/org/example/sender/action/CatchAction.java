package org.example.sender.action;

import lombok.Getter;

@Getter
public class CatchAction extends Action {

    private final double direction;

    public CatchAction(double direction) {
        super(ActionType.CATCH);
        this.direction = direction;
    }
}
