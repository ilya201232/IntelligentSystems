package org.example.decision.controller.handler.impl;

import org.example.decision.controller.handler.BaseControllersHandler;
import org.example.decision.controller.part.impl.common.BallTrackerControllerPart;
import org.example.decision.controller.part.impl.common.IdleControllerPart;
import org.example.decision.controller.part.impl.common.MoveEventListenerControllerPart;
import org.example.decision.controller.part.impl.guard.GuardHighControllerPart;
import org.example.decision.controller.part.impl.guard.GuardHighestControllerPart;
import org.example.decision.controller.part.impl.guard.GuardMidControllerPart;
import org.example.model.Knowledge;
import org.example.model.unit.Vector2;

import java.util.List;

public class GuardControllerHandler extends BaseControllersHandler {

    // L)  X   Y
    // 1) -26 -13
    // 2) -26  13
    // 3) -30 -24
    // 4) -30  24

    // R)  X   Y
    // 1)  26 -13
    // 2)  26  13
    // 3)  30 -24
    // 4)  30  24
    public GuardControllerHandler(Knowledge knowledge, Vector2 refPoint, boolean isTop) {
        super(List.of(
                new MoveEventListenerControllerPart(),
                new BallTrackerControllerPart(),
                new IdleControllerPart(refPoint, false),
                new GuardMidControllerPart(isTop),
                new GuardHighControllerPart(isTop),
                new GuardHighestControllerPart()
        ), knowledge);
    }
}
