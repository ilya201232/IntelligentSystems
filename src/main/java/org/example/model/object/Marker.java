package org.example.model.object;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.model.base.FieldObject;
import org.example.model.unit.Vector2;

@Getter
@AllArgsConstructor
public class Marker extends FieldObject {

    private final String id;

    public Marker(String id, Vector2 position) {
        super(position);
        this.id = id;
    }

}
