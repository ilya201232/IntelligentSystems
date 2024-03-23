package org.example.decision.controller.handler.impl;

import org.example.decision.controller.handler.BaseControllersHandler;
import org.example.decision.controller.part.ControllerPart;
import org.example.model.Knowledge;
import org.example.model.Perception;
import org.example.sender.action.Action;

import java.util.List;

public class EmptyControllerHandler extends BaseControllersHandler {

    public EmptyControllerHandler(Knowledge knowledge) {
        super(List.of(
                ControllerPart.empty()
        ), knowledge);
    }

}
