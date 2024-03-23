package org.example.decision.controller.handler.impl;

import org.example.decision.controller.handler.BaseControllersHandler;
import org.example.decision.controller.part.impl.common.BallTrackerControllerPart;
import org.example.decision.controller.part.impl.common.IdleControllerPart;
import org.example.decision.controller.part.impl.common.MoveEventListenerControllerPart;
import org.example.decision.controller.part.impl.goalie.GoalieHighControllerPart;
import org.example.decision.controller.part.impl.goalie.GoalieMidControllerPart;
import org.example.model.Knowledge;
import org.example.model.unit.Vector2;

import java.util.List;

public class GoalieControllerHandler extends BaseControllersHandler {

    // -50 0
    public GoalieControllerHandler(Knowledge knowledge, Vector2 refPoint) {
        super(List.of(
                new MoveEventListenerControllerPart(),
                new BallTrackerControllerPart(),
                new IdleControllerPart(refPoint, false),
                new GoalieMidControllerPart(),
                new GoalieHighControllerPart()
        ), knowledge);
    }
}
