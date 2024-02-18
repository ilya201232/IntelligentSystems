package org.example.receiver.dto.object;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.model.unit.Side;

@Getter
@Setter
@Builder
public class Target {
    private Side side;
    private int uniformNumber;
}
