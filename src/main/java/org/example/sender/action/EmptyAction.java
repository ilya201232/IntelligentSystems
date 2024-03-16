package org.example.sender.action;

public class EmptyAction extends Action {

    public static EmptyAction instance = new EmptyAction();

    private EmptyAction() {
        super(ActionType.NONE, true);
    }
}
