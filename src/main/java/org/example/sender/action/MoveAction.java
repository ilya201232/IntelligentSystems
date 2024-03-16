package org.example.sender.action;

import lombok.Getter;
import org.example.model.unit.Vector2;

@Getter
public class MoveAction extends Action {

    private final Vector2 position;

    public MoveAction(Vector2 position) {
        this(position, false);
    }

    public MoveAction(Vector2 position, boolean repeatable) {
        super(ActionType.MOVE, repeatable);
        this.position = position;
    }
}
