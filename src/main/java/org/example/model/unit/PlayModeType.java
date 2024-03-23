package org.example.model.unit;

import org.example.exception.FailedToParseException;

public enum PlayModeType {

    BEFORE_KICK_OFF, PLAY_ON,
    TIME_OVER, KICK_OFF, KICK_IN, FREE_KICK, CORNER_KICK,
    GOAL_KICK, // Documentation had stars between "Side". It may mean smth
    DROP_BALL,
    OFFSIDE, PENALTY_KICK, FOUL_CHARGE, BACK_PASS, FREE_KICK_FAULT, INDIRECT_FREE_KICK, ILLEGAL_DEFENSE;
    public static PlayModeType parsePlayModeType(String playModeType) throws FailedToParseException {
        return switch (playModeType) {
            case "before_kick_off" -> BEFORE_KICK_OFF;
            case "play_on" -> PLAY_ON;
            case "time_over" -> TIME_OVER;
            case "kick_off" -> KICK_OFF;
            case "kick_in" -> KICK_IN;
            case "free_kick" -> FREE_KICK;
            case "corner_kick" -> CORNER_KICK;
            case "goal_kick" -> GOAL_KICK;
            case "drop_ball" -> DROP_BALL;
            case "offside" -> OFFSIDE;
            case "penalty_kick" -> PENALTY_KICK;
            case "foul_charge" -> FOUL_CHARGE;
            case "back_pass" -> BACK_PASS;
            case "free_kick_fault" -> FREE_KICK_FAULT;
            case "indirect_free_kick" -> INDIRECT_FREE_KICK;
            case "illegal_defense" -> ILLEGAL_DEFENSE;
            default -> throw new FailedToParseException();
        };
    }
}
