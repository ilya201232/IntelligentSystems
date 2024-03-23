package org.example.receiver.dto;

import org.example.receiver.dto.enums.MessageType;

public class OkDTO extends MessageDTO {

    public OkDTO() {
        super(-1, MessageType.OK);
    }
}
