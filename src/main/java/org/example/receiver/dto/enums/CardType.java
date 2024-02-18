package org.example.receiver.dto.enums;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum CardType {
    RED, YELLOW;

    public static CardType parseString(String string) {
        return switch (string) {
            case "red" -> RED;
            case "yellow" -> YELLOW;
            default -> {
                log.warn("Failed to parse {} for CardType", string);
                yield null;
            }
        };
    }
}
