package org.example.sender.action;

import lombok.Getter;

@Getter
public class SayAction extends Action {

    private final String message;

    public SayAction(String message) {
        this(message, false);
    }

    public SayAction(String message, boolean repeatable) {
        super(ActionType.SAY, repeatable);
        this.message = message;
    }
}
