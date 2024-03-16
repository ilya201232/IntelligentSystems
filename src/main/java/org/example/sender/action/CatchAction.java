package org.example.sender.action;

import lombok.Getter;

@Getter
public class CatchAction extends Action {

    private final double direction;

    public CatchAction(double direction) {
        this(direction, false);
    }

    public CatchAction(double direction, boolean repeatable) {
        super(ActionType.CATCH, repeatable);
        this.direction = direction;
    }
}
