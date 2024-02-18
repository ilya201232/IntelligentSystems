package org.example.receiver.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.receiver.dto.enums.CollisionType;
import org.example.receiver.dto.enums.MessageType;
import org.example.receiver.dto.object.*;

import java.util.List;

@Getter
@Setter
public class SenseBodyDTO extends MessageDTO {

    private ViewMode viewMode;
    private Stamina stamina;
    private Speed speed;
    private double headAngle;
    private int kick;
    private int dash;
    private int turn;
    private int say;
    private int turnNeck;
    private int catchCount;
    private int move;
    private int changeView;
    private int changeFocus;
//    private Arm arm;
//    private Focus focus;
//    private Tackle tackle;
//    private List<CollisionType> collision;
//    private Foul foul;



    public SenseBodyDTO(int cycle) {
        super(cycle, MessageType.SENSE_BODY);
    }

}
