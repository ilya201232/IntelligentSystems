package org.example.sender.action;

import lombok.Getter;
import org.example.model.unit.Vector2;

@Getter
public class MoveAction extends Action {

    private final Vector2 position;

    public MoveAction(Vector2 position) {
        super(ActionType.MOVE);
        this.position = position;
    }
}
