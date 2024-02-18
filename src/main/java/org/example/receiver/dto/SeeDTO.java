package org.example.receiver.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.receiver.dto.enums.MessageType;
import org.example.receiver.dto.object.ObjectInfo;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SeeDTO extends MessageDTO {

    private List<ObjectInfo> objects;

    public SeeDTO(int cycleNumber) {
        super(cycleNumber, MessageType.SEE);
        objects = new ArrayList<>();
    }
}
