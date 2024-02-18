package org.example.model.object;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.model.unit.Side;
import org.example.model.unit.Vector2;
import org.example.model.base.MobileObject;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Player extends MobileObject {

    /**
     * Name of the team this player is on
     */
    private String teamName;

    /**
     * Side this player is on
     */
    private Side side;

    /**
     * Player's number
     */
    private Integer uniformNumber;

    /**
     * Is this player a goalie
     */
    private boolean goalie;

    private Double bodyDirection;
    private Double headDirection;

    public Player(String teamName, boolean goalie, Side side, Integer uniformNumber) {
        this.teamName = teamName;
        this.side = side;
        this.uniformNumber = uniformNumber;
        this.goalie = goalie;
    }

    public Player(String teamName, boolean goalie) {
        this.teamName = teamName;
        this.goalie = goalie;
    }

}
