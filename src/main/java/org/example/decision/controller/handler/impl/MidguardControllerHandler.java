package org.example.decision.controller.handler.impl;

import org.example.decision.controller.handler.BaseControllersHandler;
import org.example.decision.controller.part.impl.common.BallTrackerControllerPart;
import org.example.decision.controller.part.impl.common.IdleControllerPart;
import org.example.decision.controller.part.impl.common.MoveEventListenerControllerPart;
import org.example.decision.controller.part.impl.midguard.MidguardHighControllerPart;
import org.example.decision.controller.part.impl.midguard.MidguardMidControllerPart;
import org.example.model.Knowledge;
import org.example.model.unit.Vector2;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MidguardControllerHandler extends BaseControllersHandler {

    // *) X  Y
    // 1) 0 -17
    // 2) 0  17
    public MidguardControllerHandler(Knowledge knowledge, Vector2 refPoint, boolean isTop, AtomicBoolean isInit) {
        super(List.of(
                new MoveEventListenerControllerPart(),
                new BallTrackerControllerPart(),
                new IdleControllerPart(refPoint, false),
                new MidguardMidControllerPart(isTop),
                new MidguardHighControllerPart(isTop, isInit)
        ), knowledge);
    }
}
