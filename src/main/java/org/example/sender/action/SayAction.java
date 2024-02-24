package org.example.sender.action;

import lombok.Getter;

@Getter
public class SayAction extends Action {

    private final String message;

    public SayAction(String message) {
        super(ActionType.SAY);
        this.message = message;
    }
}
