package org.example.sender.action;

import lombok.Getter;
import org.example.sender.dto.Foul;

@Getter
public class TackleAction extends Action {

    private final double power;
    private final Foul foul;

    public TackleAction(double power, Foul foul) {
        this(power, foul, false);
    }

    public TackleAction(double power, Foul foul, boolean repeatable) {
        super(ActionType.TACKLE, repeatable);
        this.power = power;
        this.foul = foul;
    }
}
