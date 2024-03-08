package org.example.decision.planner.step;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.example.model.Knowledge;
import org.example.model.Perception;
import org.example.sender.action.Action;

@Getter
@ToString // Children must remember to callSuper=true
@AllArgsConstructor
public abstract class Step {
    private final StepType type;

    public abstract boolean isGoalSatisfied(Knowledge knowledge, Perception perception);
    public abstract Action onGoalSatisfied(Knowledge knowledge, Perception perception);
    public abstract Action decideAction(Knowledge knowledge, Perception perception);

}
