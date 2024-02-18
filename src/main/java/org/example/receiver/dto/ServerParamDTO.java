package org.example.receiver.dto;

import lombok.Getter;
import org.example.receiver.dto.enums.MessageType;

import java.util.List;
import java.util.Map;

@Getter
public class ServerParamDTO extends MessageDTO {

//    private final Map<String, String> params;
    private final List<String> params;

    public ServerParamDTO(List<String> params) {
        super(-1, MessageType.SERVER_PARAM);
        this.params = params;
    }
}
