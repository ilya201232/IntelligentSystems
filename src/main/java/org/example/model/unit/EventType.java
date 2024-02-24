package org.example.model.unit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum EventType {
    GOAL, FOUL,
    YELLOW_CARD, RED_CARD,
    GOALIE_CATCH_BALL,
    TIME_UP_WITHOUT_A_TEAM, TIME_UP, HALF_TIME, TIME_EXTENDED;

    public static EventType parseEventType(String string) {
        return switch (string) {
            case "goal" -> GOAL;
            case "foul" -> FOUL;
            case "yellow_card" -> YELLOW_CARD;
            case "red_card" -> RED_CARD;
            case "goalie_catch_ball" -> GOALIE_CATCH_BALL;
            case "time_up_without_a_team" -> TIME_UP_WITHOUT_A_TEAM;
            case "time_up" -> TIME_UP;
            case "half_time" -> HALF_TIME;
            case "time_extended" -> TIME_EXTENDED;
            default -> {
                log.warn("Failed to parse {} for EventType", string);
                yield null;
            }
        };
    }
}
