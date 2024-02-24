package org.example.sender.action;

import lombok.Getter;
import org.example.sender.dto.Foul;

@Getter
public class TackleAction extends Action {

    private final double power;
    private final Foul foul;

    public TackleAction(double power, Foul foul) {
        super(ActionType.TACKLE);
        this.power = power;
        this.foul = foul;
    }
}
