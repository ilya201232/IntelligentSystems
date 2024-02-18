package org.example.receiver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.receiver.dto.enums.MessageType;

@Getter
@AllArgsConstructor
public class MessageDTO {
    private final int cycleNumber;
    private final MessageType messageType;
}
