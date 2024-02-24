package org.example.model.unit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.example.exception.FailedToParseException;

@Getter
public class Event {

    @Setter
    private int cycleNumber;
    private EventType type;
    private Side side; // If type has side
    private Integer number; // If type has some int number in it

    public Event(EventType type, Side side, Integer number) {
        this.type = type;
        this.side = side;
        this.number = number;
    }

    public static Event parseEvent(String message) {

        String[] split = message.split("_");

        Integer number = null;
        Side side = null;

        boolean hasNumber = false;

        try {
            number = Integer.parseInt(split[split.length - 1]);
            side = Side.parseString(split[split.length - 2]);

            message = message.substring(0, message.length() - (split[split.length - 1].length() + split[split.length - 2].length() + 2));

            hasNumber = true;
        } catch (NumberFormatException ignored) {}

        if (!hasNumber) {
            try {
                side = Side.parseStringPossibleException(split[split.length - 1]);
                message = message.substring(0, message.length() - (split[split.length - 1].length() + 1));
            } catch (FailedToParseException ignored) {}
        }

        return new Event(
                EventType.parseEventType(message),
                side,
                number
        );
    }
}
