package org.example.decision.controller.part.impl.common;

import org.example.decision.controller.data.ControllerData;
import org.example.decision.controller.data.ControllerDataType;
import org.example.decision.controller.data.impl.MoveEventListenerData;
import org.example.decision.controller.part.ControllerPart;
import org.example.model.Knowledge;
import org.example.model.Perception;
import org.example.model.unit.Event;
import org.example.model.unit.EventType;
import org.example.model.unit.PlayMode;
import org.example.model.unit.PlayModeType;
import org.example.sender.action.Action;
import org.example.sender.action.MoveAction;
import org.example.utils.KnowledgeUtils;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class MoveEventListenerControllerPart extends ControllerPart {

    private boolean hasMoved = true;
    private int lastGoalCycle = -1;

    @Override
    public Optional<Action> decideAction(Perception perception, Knowledge knowledge, Map<ControllerDataType, ControllerData> data) {

        if (nextController == null) {
            throw new IllegalStateException("This controller part cannot be the last one!");
        }

        if (knowledge.getCurrentPlayMode().getPlayModeType() == PlayModeType.BEFORE_KICK_OFF) {
            hasMoved = true;
            return Optional.of(new MoveAction(knowledge.getStartPosition()));
        }

        Event goalEvent = knowledge.getHeardEvents().get(EventType.GOAL);

        if (goalEvent != null && goalEvent.getCycleNumber() > lastGoalCycle) {
            // Save last goal cycle number to ignore it afterward
            lastGoalCycle = goalEvent.getCycleNumber();
            hasMoved = true;
            return Optional.of(new MoveAction(knowledge.getStartPosition()));
        }

        data.put(ControllerDataType.MoveEventListener, new MoveEventListenerData(hasMoved));
        hasMoved = false;

        return nextController.decideAction(perception, knowledge, data);
    }

    @Override
    public void baseInit(Knowledge knowledge) {
        if (nextController != null)
            nextController.baseInit(knowledge);
    }
}
