package org.example.receiver.dto.enums;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum ViewQuality {
    HIGH, LOW;

    public static ViewQuality parseString(String string) {

        return switch (string) {
            case "high" -> HIGH;
            case "low" -> LOW;
            default -> {
                log.warn("Failed to parse {} for ViewQuality", string);
                yield  null;
            }
        };
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
