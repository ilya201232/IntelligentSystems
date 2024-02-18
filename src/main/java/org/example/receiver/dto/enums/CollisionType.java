package org.example.receiver.dto.enums;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum CollisionType {
    BALL, PLAYER, GOAL_POST;

    public static CollisionType parseString(String string) {
        return switch (string) {
            case "ball" -> BALL;
            case "player" -> PLAYER;
            case "post" -> GOAL_POST;
            default -> {
                log.warn("Failed to parse {} for CollisionType", string);
                yield null;
            }
        };
    }
}
