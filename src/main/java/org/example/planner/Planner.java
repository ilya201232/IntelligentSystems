package org.example.planner;

import lombok.extern.slf4j.Slf4j;
import org.example.model.Knowledge;
import org.example.model.Perception;
import org.example.model.unit.Side;
import org.example.planner.step.ApproachFlagStep;
import org.example.planner.step.KickBallForGoalStep;
import org.example.planner.step.Step;
import org.example.sender.action.Action;
import org.example.sender.action.ActionType;
import org.example.sender.action.EmptyAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class Planner {

    private final Knowledge knowledge;

    List<Step> stepsToExecute = new ArrayList<>();
    private int currentStepIndex = 0;

    private LocalDateTime lastUsedPerceptionMoment = LocalDateTime.MIN;

    {
//        stepsToExecute.add(new ApproachFlagStep("f r b"));
//        stepsToExecute.add(new ApproachFlagStep("g l"));
//        stepsToExecute.add(new ApproachFlagStep("f c"));
        stepsToExecute.add(new KickBallForGoalStep(Side.RIGHT));
        stepsToExecute.add(new KickBallForGoalStep(Side.LEFT));
    }

    public Planner(Knowledge knowledge) {
        this.knowledge = knowledge;
    }

    public Action planAction(Perception perception) {

        if (stepsToExecute.isEmpty()) {
            return EmptyAction.instance;
        }

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

        Step currentStep = stepsToExecute.get(currentStepIndex);

        int counter = 0;
        while (currentStep.isGoalSatisfied(knowledge, perception)) {
            Action action = currentStep.onGoalSatisfied(knowledge, perception);

            increaseStepIndex();

            if (action.getType() != ActionType.NONE) {
                return action;
            }

            currentStep = stepsToExecute.get(currentStepIndex);
            counter++;

            if (counter >= stepsToExecute.size()) {
                log.warn("Planner stuck inside infinite loop. Initial step index {}, Steps: {}", currentStepIndex, Arrays.toString(stepsToExecute.toArray()));
                return EmptyAction.instance;
            }
        }

        return currentStep.decideAction(knowledge, perception);
    }

    private void increaseStepIndex() {
        currentStepIndex++;
        if (currentStepIndex >= stepsToExecute.size()) {
            currentStepIndex = 0;
        }
    }
}
