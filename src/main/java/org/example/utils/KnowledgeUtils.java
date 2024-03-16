package org.example.utils;

import org.example.model.Knowledge;
import org.example.receiver.dto.HearDTO;

import java.util.Iterator;
import java.util.Optional;

public class KnowledgeUtils {

    public static Optional<HearDTO> hadHeardMessageSince(Knowledge knowledge, String messageText, int startCycleNumber) {

        Iterator<HearDTO> messagesIterator = knowledge.getMessages().descendingIterator();

        while (messagesIterator.hasNext()) {
            HearDTO hearDTO = messagesIterator.next();

            if (hearDTO.getCycleNumber() < startCycleNumber) {
                break;
            } else {
                if (hearDTO.getMessage().equals(messageText)) {
                    return Optional.of(hearDTO);
                }
            }
        }

        return Optional.empty();
    }

}
