package org.example.decision.controller.data.impl;

import lombok.Getter;
import lombok.Setter;
import org.example.decision.controller.data.ControllerData;
import org.example.decision.controller.data.ControllerDataType;
import org.example.model.unit.Vector2;

@Setter
@Getter
public class IdleControllerData extends ControllerData {

    private Vector2 playerPosition;
    private Double distanceToRef;

    public IdleControllerData(Vector2 playerPosition, Double distanceToRef) {
        super(ControllerDataType.IdleController);
        this.playerPosition = playerPosition;
        this.distanceToRef = distanceToRef;
    }
}
