package org.example.decision.controller.data.impl;

import lombok.Getter;
import org.example.decision.controller.data.ControllerData;
import org.example.decision.controller.data.ControllerDataType;

@Getter
public class MoveEventListenerData extends ControllerData {

    private final boolean hasMoved;

    public MoveEventListenerData(boolean hasMoved) {
        super(ControllerDataType.MoveEventListener);
        this.hasMoved = hasMoved;
    }
}
