package org.example.receiver.dto.object;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Speed {

    // Approximation
    private double amount;

    // Approximation
    private double direction;

}
