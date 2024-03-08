package org.example.decision.planner;

import lombok.extern.slf4j.Slf4j;
import org.example.decision.planner.step.Step;
import org.example.model.Knowledge;
import org.example.model.Perception;
import org.example.sender.action.Action;
import org.example.sender.action.ActionType;
import org.example.sender.action.EmptyAction;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class Planner {

    private final Knowledge knowledge;

    private final List<Step> stepsToExecute;
    private int currentStepIndex = 0;

//    {
//        stepsToExecute.add(new ApproachFlagStep("f r b"));
//        stepsToExecute.add(new ApproachFlagStep("g l"));
//        stepsToExecute.add(new ApproachFlagStep("f c"));
//        stepsToExecute.add(new KickBallForGoalStep(Side.RIGHT));
//        stepsToExecute.add(new KickBallForGoalStep(Side.LEFT));
//    }

    public Planner(Knowledge knowledge, List<Step> stepsToExecute) {
        this.knowledge = knowledge;
        this.stepsToExecute = stepsToExecute;
    }

    public Action planAction(Perception perception) {

        if (stepsToExecute.isEmpty()) {
            return EmptyAction.instance;
        }

        Step currentStep = stepsToExecute.get(currentStepIndex);

        int counter = 0;
        try {
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
        } catch (IllegalStateException ignored) {}

        return EmptyAction.instance;
    }

    private void increaseStepIndex() {
        currentStepIndex++;
        if (currentStepIndex >= stepsToExecute.size()) {
            currentStepIndex = 0;
        }
    }
}
