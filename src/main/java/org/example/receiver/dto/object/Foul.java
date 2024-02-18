package org.example.receiver.dto.object;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.receiver.dto.enums.CardType;

@Getter
@Setter
@NoArgsConstructor
public class Foul {
    private int chargedCycles;
    private CardType cardType;
}
