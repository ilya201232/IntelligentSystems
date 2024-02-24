package org.example.model.unit;

import lombok.extern.slf4j.Slf4j;
import org.example.exception.FailedToParseException;

@Slf4j
public enum Side {
    LEFT, RIGHT;

    public static Side getOpposite(Side side) {
        if (side == Side.LEFT) {
            return Side.RIGHT;
        } else {
            return Side.LEFT;
        }
    }

    public static Side parseString(String string) {
        return switch (string) {
            case "l" -> LEFT;
            case "r" -> RIGHT;
            default -> {
                log.warn("Failed to parse {} for Side", string);
                yield null;
            }
        };
    }

    public static Side parseStringPossibleException(String string) {
        return switch (string) {
            case "l" -> LEFT;
            case "r" -> RIGHT;
            default -> {
                throw new FailedToParseException();
            }
        };
    }
}
