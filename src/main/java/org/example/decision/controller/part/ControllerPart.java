package org.example.decision.controller.part;

import lombok.Setter;
import org.example.decision.controller.data.ControllerData;
import org.example.decision.controller.data.ControllerDataType;
import org.example.model.Knowledge;
import org.example.model.Perception;
import org.example.sender.action.Action;
import org.example.sender.action.EmptyAction;

import java.util.Map;
import java.util.Optional;

@Setter
public abstract class ControllerPart {

    protected ControllerPart nextController;

    public static ControllerPart empty() {
        return new ControllerPart() {
            @Override
            public Optional<Action> decideAction(Perception perception, Knowledge knowledge, Map<ControllerDataType, ControllerData> data) {
                return Optional.ofNullable(EmptyAction.instance);
            }

            @Override
            public void baseInit(Knowledge knowledge) {

            }
        };
    }

    public abstract Optional<Action> decideAction(Perception perception, Knowledge knowledge, Map<ControllerDataType, ControllerData> data);

    public abstract void baseInit(Knowledge knowledge);
}
