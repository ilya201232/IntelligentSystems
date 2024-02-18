package org.example.receiver.dto.object;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Arm {

    private int movableCycles;
    private int expiresCycles;
    private int count;

}
