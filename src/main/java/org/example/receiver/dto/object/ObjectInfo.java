package org.example.receiver.dto.object;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ObjectInfo {
    private int parametersReceived;

    private String objectName;
    private Double distance;

    private Double direction;

    private Double distanceChange;
    private Double directionChange;

    private Double BodyFacingDirection;
    private Double HeadFacingDirection;

    // PointDir and t/k is ignored - no docs on what it is
}
