package org.example.receiver.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.model.unit.Event;
import org.example.receiver.dto.enums.MessageType;
import org.example.receiver.dto.enums.SenderType;

@Getter
public class HearDTO extends MessageDTO {

    private final SenderType senderType;
    private final Double senderDirection; // In case SenderType.DIRECTION

    private final String message;


    @Builder
    public HearDTO(int cycleNumber, SenderType senderType, Double senderDirection, String message) {
        super(cycleNumber, MessageType.HEAR);
        this.senderType = senderType;
        this.senderDirection = senderDirection;
        this.message = message;
    }
}
