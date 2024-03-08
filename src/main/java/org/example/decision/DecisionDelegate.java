package org.example.decision;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.decision.tree.ActionTree;
import org.example.model.Knowledge;
import org.example.model.Perception;
import org.example.sender.action.Action;
import org.example.sender.action.ActionType;
import org.example.sender.action.EmptyAction;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class DecisionDelegate {

    private final ActionTree actionTree;
    private LocalDateTime lastUsedPerceptionMoment = LocalDateTime.MIN;

    public Action planAction(Perception perception) {

        if (perception.getSensed() == null || perception.getMarkersSaw().isEmpty()) {
            log.debug("Received perception doesn't have all information needed for action planning.");
            return EmptyAction.instance;
        }

        if (lastUsedPerceptionMoment.equals(LocalDateTime.MIN)) {
            lastUsedPerceptionMoment = LocalDateTime.now();
        } else {
            if (lastUsedPerceptionMoment.isBefore(perception.getCreationDatetime())) {
                lastUsedPerceptionMoment = LocalDateTime.now();
            } else {
                log.debug("Received perception that was created before last action.");
                return EmptyAction.instance;
            }
        }

        return actionTree.decideAction(perception);
    }

}
