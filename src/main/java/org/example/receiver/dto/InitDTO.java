package org.example.receiver.dto;

import lombok.*;
import org.example.model.unit.PlayMode;
import org.example.model.unit.Side;
import org.example.receiver.dto.enums.MessageType;

@Getter
public class InitDTO extends MessageDTO {

    private Side side;
    private int uniformNumber;
    private PlayMode playMode;

    @Builder
    public InitDTO(Side side, int uniformNumber, PlayMode playMode) {
        super(-1, MessageType.INIT);
        this.side = side;
        this.uniformNumber = uniformNumber;
        this.playMode = playMode;
    }
}
