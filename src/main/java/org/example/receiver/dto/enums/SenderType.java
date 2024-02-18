package org.example.receiver.dto.enums;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum SenderType {
    ONLINE_COACH_LEFT, ONLINE_COACH_RIGHT, COACH, REFEREE, SELF, DIRECTION;

    public static SenderType parseString(String string) {

        return switch (string) {
            case "online_coach_left" -> ONLINE_COACH_LEFT;
            case "online_coach_right" -> ONLINE_COACH_RIGHT;
            case "coach" -> COACH;
            case "referee" -> REFEREE;
            case "self" -> SELF;
            case "direction" -> DIRECTION;
            default -> {
                log.warn("Failed to parse {} for SenderType", string);
                yield null;
            }
        };
    }
}
