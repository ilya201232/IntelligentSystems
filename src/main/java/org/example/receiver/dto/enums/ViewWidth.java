package org.example.receiver.dto.enums;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum ViewWidth {
    NARROW, NORMAL, WIDE;

    public static ViewWidth parseString(String string) {
        return switch (string) {
            case "narrow" -> NARROW;
            case "normal" -> NORMAL;
            case "wide" -> WIDE;
            default -> {
                log.warn("Failed to parse {} for ViewWidth", string);
                yield null;
            }
        };
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
