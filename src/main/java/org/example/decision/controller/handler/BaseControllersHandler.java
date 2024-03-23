package org.example.decision.controller.handler;

import org.example.decision.controller.part.ControllerPart;
import org.example.model.Knowledge;
import org.example.model.Perception;
import org.example.sender.action.Action;
import org.example.sender.action.EmptyAction;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public abstract class BaseControllersHandler {

    private final ControllerPart startPart;
    private final Knowledge knowledge;

    public BaseControllersHandler(List<ControllerPart> parts, Knowledge knowledge) {
        this.startPart = parts.getFirst();
        this.knowledge = knowledge;

        setNextForControllers(parts);
    }

    private void setNextForControllers(List<ControllerPart> parts) {
        for (int i = 0; i < parts.size() - 1; i++) {
            parts.get(i).setNextController(parts.get(i + 1));
        }
    }

    public Action decideAction(Perception perception) {
        Optional<Action> actionOptional = startPart.decideAction(perception, knowledge, new HashMap<>());

        return actionOptional.orElse(EmptyAction.instance);
    }

    public void init() {
        startPart.baseInit(knowledge);
    }
}
