package org.example.sender.action;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public abstract class Action {
    private final ActionType type;
    private final boolean repeatable;
}
