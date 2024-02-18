package org.example.model.base;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.model.unit.Vector2;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class FieldObject extends GameObject {

    protected Vector2 position;

}
