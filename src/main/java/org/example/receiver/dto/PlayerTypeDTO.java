package org.example.receiver.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.receiver.dto.enums.MessageType;

@Getter
@Setter
public class PlayerTypeDTO extends MessageDTO {

    private int id;
    private double playerSpeedMax;
    private double staminaIncMax;
    private double playerDecay;
    private double inertiaMoment;
    private double dashPowerRate;
    private double playerSize;
    private double kickableMargin;
    private double kickRand;
    private double extraStamina;
    private double effortMax;
    private double effortMin;

    public PlayerTypeDTO() {
        super(-1, MessageType.PLAYER_TYPE);
    }
}
