package org.example.model.base;

import lombok.Getter;
import lombok.Setter;
import org.example.model.unit.Vector2;

@Getter
@Setter
public abstract class MobileObject extends FieldObject {

    protected Double distanceChange;
    protected Double directionChange;

}
