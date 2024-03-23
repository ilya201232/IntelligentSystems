package org.example.decision.controller.handler.impl;

import org.example.decision.controller.handler.BaseControllersHandler;
import org.example.decision.controller.part.impl.attack.AttackHighControllerPart;
import org.example.decision.controller.part.impl.attack.AttackHighestControllerPart;
import org.example.decision.controller.part.impl.attack.AttackMidControllerPart;
import org.example.decision.controller.part.impl.common.BallTrackerControllerPart;
import org.example.decision.controller.part.impl.common.IdleControllerPart;
import org.example.decision.controller.part.impl.common.MoveEventListenerControllerPart;
import org.example.model.Knowledge;
import org.example.model.unit.Vector2;

import java.util.List;

public class AttackPlayerControllerHandler extends BaseControllersHandler {

    public AttackPlayerControllerHandler(Knowledge knowledge, Vector2 refPoint, boolean isTop) {
        super(List.of(
                new MoveEventListenerControllerPart(),
                new BallTrackerControllerPart(),
                new IdleControllerPart(refPoint, true),
                new AttackMidControllerPart(isTop),
                new AttackHighControllerPart(isTop),
                new AttackHighestControllerPart()
        ), knowledge);
    }
}
